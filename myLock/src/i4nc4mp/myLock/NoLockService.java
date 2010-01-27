package i4nc4mp.myLock;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Handler;
import android.provider.Settings.SettingNotFoundException;
import android.util.Log;


public class NoLockService extends MediatorService {
	
	public boolean persistent = false;
	
	public boolean shouldLock = true;
	//when true screen off will start the lockscreen.
	//we will ensure that it is false until user exits lockscreen or finishes a call
	
	public boolean Lockaftercall = false;
	//just a flag we set when calls wake the device.
	
	public int patternsetting = 0;
	//we'll see if the user has pattern enabled when we startup
	//so we can disable it and then restore when we finish
	
	Handler serviceHandler;
	Task myTask = new Task();
	
	@Override
	public void onDestroy() {
		super.onDestroy();
		
		if (patternsetting == 1) {
			android.provider.Settings.System.putInt(getContentResolver(), 
    			android.provider.Settings.System.LOCK_PATTERN_ENABLED, 1);
    	//re-enable pattern lock if applicable
			
			serviceHandler.removeCallbacks(myTask);
		    serviceHandler = null;
	}
}

	@Override
	public void onRestartCommand() {
//restart is triggered if user changes the persistent mode setting
		
		SharedPreferences settings = getSharedPreferences("myLock", 0);
		boolean fgpref = settings.getBoolean("FG", true);
		boolean wake = settings.getBoolean("StayAwake", false);
		
			if (persistent != fgpref) {//user changed pref
				if (persistent) {
					stopForeground(true);//kills the ongoing notif
					persistent = false;
				}
				else doFGstart(wake);//so FG mode is started again
			}
			else {
				//the key event or slider open in unLockScreen sends a start back to us. this means exit the keyguard
				shouldLock = true;//this flag helps call logic know that user is in the middle of an active wakeup
				
				//ManageWakeLock.acquirePartial(getApplicationContext());//ensure that we can get the next lockscreen created
				//it doesn't work. in certain apps, anytime we sleep we get the broadcast and try to start but it fails to start
				//the start actually happens at next screen wakeup, as if CPU sleep is still happening right before our onCreate
				
				
				StartDismiss(getApplicationContext());
			}
	}
	
	@Override
	public void onFirstStart() {
		SharedPreferences settings = getSharedPreferences("myLock", 0);
		persistent = settings.getBoolean("FG", true);
		boolean wake = settings.getBoolean("StayAwake", false);
		//boolean bootstart = settings.getBoolean("boot", false);
		
		
		if(wake) ManageWakeLock.acquireFull(getApplicationContext());
        //probably not a common case
        //if user happens to leave stay awake on all the time
        //initialize it here, otherwise always done at time of toggle in settings
		
		if (persistent) doFGstart(wake);
		
		//we have to toggle pattern lock off to use a custom lockscreen
		try {
			patternsetting = android.provider.Settings.System.getInt(getContentResolver(), android.provider.Settings.System.LOCK_PATTERN_ENABLED);
		} catch (SettingNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		if (patternsetting == 1) {    	
    	android.provider.Settings.System.putInt(getContentResolver(), 
    			android.provider.Settings.System.LOCK_PATTERN_ENABLED, 0); 
		}
		
		/*when we got started at boot we need to exit the initial lockscreen*/
		//not critical, this an end user experience improver. disable for now
	    		/*
				ManageKeyguard.initialize(getApplicationContext());
	    		
	    		ManageKeyguard.disableKeyguard(getApplicationContext());
	    		serviceHandler.postDelayed(myTask, 50L);//unlock will be set by this callback
	    		*/
		serviceHandler = new Handler();
	}
	
	class Task implements Runnable {
    	public void run() {
    		Context mCon = getApplicationContext();
    		Log.v("lock_delay","task executing");
    		if (!shouldLock) {    			
    		//first run is after half second of screen off. see if any keyguard exists
    			ManageKeyguard.initialize(mCon);
    			if (ManageKeyguard.inKeyguardRestrictedInputMode()) StartLock(mCon);//take over the lock
    			else {
    				shouldLock = true;
    				serviceHandler.postDelayed(myTask, 4500L);
    				//wait 4 more seconds, and lock
    				}
    		}
    		else {
    			shouldLock = false;
            	StartLock(mCon);
    		}
    	}
	}
	
	@Override
	public void onScreenSleep() {
		//when sleep after an unlock, start the lockscreen again
		
		if (receivingcall || placingcall || !shouldLock) return;
		//don't handle during calls at all
		//the should flag is for extra safety in case we ever find another exception case
		
		shouldLock = false;
		serviceHandler.postDelayed(myTask, 500L);//checks if user requested the sleep
		//this way we can leave the grace period for timeout sleeps
		
		return;//prevents unresponsive broadcast error
	}
	
	
	public void StartDismiss(Context context) {
    	/*
    	//ManageKeyguard.initialize(context);
    	PowerManager pm = (PowerManager) getSystemService (Context.POWER_SERVICE); 
    	pm.userActivity(SystemClock.uptimeMillis(), false);
    	//ensure it will be awake
    	
    	ManageKeyguard.disableKeyguard(getApplicationContext());
    	//advantage here is we don't have to do a task delay
    	//because we're already showing on top of keyguard this gets the job done
    	
    	
    	ManageKeyguard.exitKeyguardSecurely(new LaunchOnKeyguardExit() {
            public void LaunchOnKeyguardExitSuccess() {
               Log.v("start", "This is the exit callback");
               }});
    	*/
    	
    	Class w = DismissKeyguardActivity.class; 
	    	      
		Intent dismiss = new Intent(context, w);
		dismiss.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK//For some reason it requires this even though we're already an activity
				| Intent.FLAG_ACTIVITY_NO_USER_ACTION//Just helps avoid conflicting with other important notifications
		        | Intent.FLAG_ACTIVITY_NO_HISTORY//Ensures the activity WILL be finished after the one time use
		        | Intent.FLAG_ACTIVITY_NO_ANIMATION);
		        
		context.startActivity(dismiss);
    	
    }
	
	private void StartLock(Context context) {

		Intent closeDialogs = new Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS);
		        context.sendBroadcast(closeDialogs);
		        
		       Class w = ShowWhenLockedActivity.class; 
		       
		       

		/* launch UI, explicitly stating that this is not due to user action
		         * so that the current app's notification management is not disturbed */
		        Intent lockscreen = new Intent(context, w);
		        
		      //new task required for our service activity start to succeed. exception otherwise
		        lockscreen.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		                //| Intent.FLAG_ACTIVITY_NO_USER_ACTION);
		        //not sure if no user action is necessary, the alarm alert used but appears just be for retaining notifications
		
		        context.startActivity(lockscreen);
		}
	
	@Override
	public void onCallStart() {
		
		if (!shouldLock) {
			//when should was false and a call started, it means the call woke device
			Lockaftercall = true;
		}
		else shouldLock = false;
		//flag so that lockscreen won't try to happen if screen off goes off in the middle of a call
		//it doesn't seem like we even get the broadcasts- phone app is forcing the screen component off
		
		//extra redundancy in this mode where we never wake the lockscreen
	}
	
	@Override
	public void onCallEnd() {
		//Account for the case that a call ends while screen is asleep
		
		if (Lockaftercall) {
			Lockaftercall = false;
			StartLock(getApplicationContext());
		}
		//the phone app is actually keeping the CPU state wakelocked
		//then forcing screen dark like our regular lockscreen would.
		else shouldLock = true;//awake and unlocked state => ensure next screen off does StartLock
		//redundancy flag telling our screen off that we want to lock
		
		
		//the only case we still can't detect here pre-2.1 is if the screen was off at call end
		//if so, not even a check for keyguard mode via the manage keyguard class will return correctly due to the 5 sec grace period
		//however it is possible the grace period doesn't happen in that scenario
	}
	
	@Override
	public void onCallMiss() {
		if (!IsAwake()) {
			//We actually need to start lock since screen is off and lockscreened
			StartLock(getApplicationContext());//lock again since user didn't react to this wake
		}
		else shouldLock = true;
	}
	
	//this logic appears to be working and places the lockscreen again when it should
	
	void doFGstart(boolean wakepref) {
		//putting ongoing notif together for start foreground
		
		//String ns = Context.NOTIFICATION_SERVICE;
		//NotificationManager mNotificationManager = (NotificationManager) getSystemService(ns);
		//No need to get the mgr, since we aren't manually sending this for FG mode.
		
		int icon = R.drawable.icon;
		CharSequence tickerText = "lockscreen disabled mode";
		
		if (wakepref) tickerText = tickerText + " (Staying Awake)";
		
		long when = System.currentTimeMillis();

		Notification notification = new Notification(icon, tickerText, when);
		
		Context context = getApplicationContext();
		CharSequence contentTitle = "myLock - click to open settings";
		CharSequence contentText = "lockscreen disabled";

		Intent notificationIntent = new Intent(this, SettingsActivity.class);
		PendingIntent contentIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);

		notification.setLatestEventInfo(context, contentTitle, contentText, contentIntent);
		
		final int SVC_ID = 1;
		
		//don't need to pass notif because startForeground will do it
		//mNotificationManager.notify(SVC_ID, notification);
		persistent = true;
		
		startForeground(SVC_ID, notification);
	}
}
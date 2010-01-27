package i4nc4mp.myLock;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Handler;
import android.provider.Settings.SettingNotFoundException;
import android.util.Log;


//custom lockscreen mode
public class CustomLockService extends MediatorService {
	
	public boolean persistent = false;
	
	public boolean shouldLock = true;
	//when true screen off will start the lockscreen.
	//we will ensure that it is false until user exits lockscreen or finishes a call
	
	public boolean Lockaftercall = false;
	//just a flag we set when calls wake the device.
	
	public boolean PendingLock = false;
	//flag we can set if user cancels a sleep by rewaking before lock 5 sec timer runs out
	
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
			else {//so if the pref isn't being changed, it means this restart is from lockscreen finish
		
		//unlock in the lockscreen activity causes start command to come back
		//react by prepping the next StartLock
				
		//if (shouldLock) Log unexpected restart occurred.
				
		if (!receivingcall && !placingcall) shouldLock = true;//queue new lockscreen if no calls active
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
		//ManageWakeLock.acquirePartial(getApplicationContext());
	}
	
	class Task implements Runnable {
    	public void run() {
    		Context mCon = getApplicationContext();
    		//Log.v("lock_delay","task executing");
    		if (!PendingLock) return;//ensures break the attempt cycle if user has aborted the lock
    		
    		//see if any keyguard exists
    			ManageKeyguard.initialize(mCon);
    			if (ManageKeyguard.inKeyguardRestrictedInputMode()) {
    				shouldLock = false;
    				PendingLock = false;
    				StartLock(mCon);//take over the lock
    			}
    			else serviceHandler.postDelayed(myTask, 500L);
    			//keep trying every half sec. essentially starts lock once keyguard is up
    				
    		}
    	
    	
    		
    	}
	
	@Override
	public void onScreenWakeup() {
		if (PendingLock) PendingLock = false;
			//user wakes screen during pending lock, so cancel it
		return;
	}
	
	@Override
	public void onScreenSleep() {
		//when sleep after an unlock, start the lockscreen again
		
		if (receivingcall || placingcall) return;//don't handle during calls at all
		
		if (shouldLock) {
        	PendingLock = true;
        	serviceHandler.postDelayed(myTask, 500L);
        	//StartLock(getApplicationContext());
		}
		
		return;//prevents unresponsive broadcast error
	}
	
	private void StartLock(Context context) {

		Intent closeDialogs = new Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS);
		        context.sendBroadcast(closeDialogs);
		        

		        Class w = Lockscreen.class;
		        //Class w = ShowWhenLockedActivity.class; no button customize, uses secure exit to unlock
		       

		/* launch UI, explicitly stating that this is not due to user action
		         * so that the current app's notification management is not disturbed */
		        Intent lockscreen = new Intent(context, w);
		        
		        //When a notification with persistent LED or vibration/sound was waiting
		        //At times it seems to wake but then sleep again when it shouldn't despite our userActivity PM calls
		        //Other times it just won't wake when notifications are waiting
		        
		      //new task required for our service activity start to succeed. exception otherwise
		        lockscreen.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
		                | Intent.FLAG_ACTIVITY_NO_USER_ACTION);
		        //not sure if no user action is necessary, the alarm alert used but appears just be for retaining notifications
		        
		                //| Intent.FLAG_ACTIVITY_NO_HISTORY
		                //this flag will tell OS to always finish the activity when user leaves it
		                //when this was on, it was exiting every time it got created. interesting unexpected behavior
		                //might be able to be utilized as an all button instant unlock mode
		                //need to investigate the focus loss to figure this out
		                //| Intent.FLAG_ACTIVITY_NO_ANIMATION)
		                //because we don't need to animate... O_o doesn't really seem to be for this
		        
		        context.startActivity(lockscreen);
		}
	
	
	//essentially anytime a call ends it seems our next screen off is not creating the lockscreen again
	//we have a few cases
	//1- call ends while screen was off. user turns on screen to find the regular lockscreen -- resolved
	//2- call ends while screen is on, user sees the lockscreen. if sleep, next wakeup still has lockscreen -- resolved
	//3- incoming call is missed or ignored results in seeing the lockscreen also
	
	@Override
	public void onCallStart() {
		//Account for the case that a call starts while screen is asleep
		
		if (!shouldLock) {
			//when should was false and a call started, it means the call woke device
			Lockaftercall = true;
		}
		else shouldLock = false;
		//flag so that lockscreen won't try to happen if screen off goes off in the middle of a call
		//it doesn't seem like we even get the broadcasts- phone app is forcing the screen component off
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
		CharSequence tickerText = "custom lockscreen mode";
		
		if (wakepref) tickerText = tickerText + " (Staying Awake)";
		
		long when = System.currentTimeMillis();

		Notification notification = new Notification(icon, tickerText, when);
		
		Context context = getApplicationContext();
		CharSequence contentTitle = "myLock - click to open settings";
		CharSequence contentText = "custom lockscreen active";

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
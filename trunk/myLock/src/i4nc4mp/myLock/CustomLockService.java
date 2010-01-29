package i4nc4mp.myLock;

import i4nc4mp.myLock.ManageKeyguard.LaunchOnKeyguardExit;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.PowerManager;
import android.os.SystemClock;
import android.provider.Settings.SettingNotFoundException;
import android.util.Log;


//custom lockscreen mode
public class CustomLockService extends MediatorService {
	
	public boolean persistent = false;
	
	public int patternsetting = 0;
	//we'll see if the user has pattern enabled when we startup
	//so we can disable it and then restore when we finish
	
/* Life-Cycle Flags */
	public boolean shouldLock = true;
	//Flagged true upon Lock Activity exit callback, remains true until StartLock intent is fired.
		
	public boolean PendingLock = false;
	//Flagged true upon sleep, remains true until StartLock sends first callback indicating Create success.
	
	public boolean Lockaftercall = false;
	//just a flag we set when calls wake the device.
	

	
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
		else {//so if the pref isn't being changed, it means this is a callback from Lock Activity
		
			if (PendingLock) {
			//This is the start success callback from Lock Activity onStart
				PendingLock = false;
				Log.v("Received Callback","Lock Activity is primed");
			//ensures that we know to handle screen on ourselves in case Lock Activity failed to complete start during off
			}
			else {
			//This is the finish callback when activity exits.
				if (!receivingcall && !placingcall) shouldLock = true;
				//queue new lockscreen on next screen off if no calls active
				//when calls are active the CallEnd reaction will take care of this
				
				Log.v("Received Callback","Lock Activity is finished");
				}
			}
				
		
	}
	
	@Override
	public void onFirstStart() {
		SharedPreferences settings = getSharedPreferences("myLock", 0);
		persistent = settings.getBoolean("FG", true);
		boolean wake = settings.getBoolean("StayAwake", false);
		//boolean bootstart = settings.getBoolean("boot", false);
		
		
		//if(wake) ManageWakeLock.acquireFull(getApplicationContext());
        //probably not a common case
        //if user happens to leave stay awake on all the time
        //initialize it here, otherwise always done at time of toggle in settings
	//FIXME change this so the boot handler starts the StayAwake Service instead.
		
		if (persistent) doFGstart(wake);
		//else send a toast telling user what mode is starting and whether stay awake is active
		//perhaps do that in the boot handler service
		
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
    		//Log.v("lock_delay","task executing");
    		if (!PendingLock) return;//ensures break the attempt cycle if user has aborted the lock
    		//user can abort Power key lock by another power key, or timeout sleep by any key wakeup
    		
    		//otherwise, see if any keyguard exists yet
    			ManageKeyguard.initialize(mCon);
    			if (ManageKeyguard.inKeyguardRestrictedInputMode()) {
    				
    				//the keyguard exists here on first try if this isn't a timeout lock
    				shouldLock = false;
    				StartLock(mCon);//take over the lock
    			}
    			else serviceHandler.postDelayed(myTask, 250L);
    			//otherwise, we need to start lock once the keyguard kicks in
    			//so keep trying until we succeed or user aborts
    				
    		}    		
    	}
	
	@Override
	public void onScreenWakeup() {
		if (!PendingLock) return;
		//we don't have to do anything at on unless we get it before the StartLock success callback
		
		Context mCon = getApplicationContext();
		
		//next, get ourselves a one time wakelock using the on after release flag.
		//user activity call on its own seems to still get ignored at times when user is aborting a power key sleep
		PowerManager pm = (PowerManager)mCon.getSystemService(Context.POWER_SERVICE);
		PowerManager.WakeLock wl = pm.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP | PowerManager.ON_AFTER_RELEASE,
            "mediatorWake");
		wl.acquire();
		
		
		
		
		//Two cases exist where we have to force the dismiss activity, in both the lockscreen would be active
	
			//1--- user aborts a Power key sleep before the 4 second mark where we startLock
			//- force dismiss activity since no Lock activity exists yet
						
			//2--- the start of LockActivity was delayed to screen on (known bug case in CPU intensive apps)
			//- force dismiss activity which will finish the newly created & primed lock activity.
			//this avoids user need to relock and rewake - there's a chance we won't detect the keyguard if the Lock activity is creating
			
			//the goal of this life cycle is to allow a big enough delay to always avoid the CPU lag bug
			//then handle the side effect of user aborting an intentional power key sleep
			
			PendingLock = false;
			//--- user aborts an auto-sleep by re-waking with any key
			//- flag off Pending to abort task loop.
			ManageKeyguard.initialize(mCon);
			if (!shouldLock || ManageKeyguard.inKeyguardRestrictedInputMode()) StartDismiss(getApplicationContext());
				//should goes false when we fire the Start Lock intent.
				//Pending is still true in absence of success callback from Lock Activity
				//force the dismiss activity since dismiss logic won't run in Lock Activity immediately after wakeup create
			//TODO i haven't seen this in action yet. the longer wait to start Lock seems to always avoid the delayed create bug
			
			//should is still true because we haven't fired Start Lock yet.
			//we will only be guarded if it was a Power Key sleep that user is aborting
			//need to fire the one time exit activity
			
			wl.release();
			//hopefully this stops the error where you can't abort the power key lock within a few seconds because it tries to sleep
			
		return;
	}
	
	
	@Override
	public void onScreenSleep() {
		//when sleep after an unlock, start the lockscreen again
		
		if (receivingcall || placingcall) return;//don't handle during calls at all
		
		if (shouldLock) {
        	PendingLock = true;
        	serviceHandler.postDelayed(myTask, 4000L);
        	//The error seems to still duplicate even at half to quarter second intervals
        	//the best fix seems to be just to let this first delay be 4 seconds
        	//if no KG on first try task retries every 500MS
        	
        	//operate on the assumption users won't normally be forcing sleep
        	//with the intention of waking again in less then 2 to 5 seconds
        	       	
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
		        
		        
		      //new task required for our service activity start to succeed. exception otherwise
		        lockscreen.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
		                | Intent.FLAG_ACTIVITY_NO_USER_ACTION);
		        //without this flag my alarm clock only buzzes once.
		        
		                //| Intent.FLAG_ACTIVITY_NO_HISTORY
		                //this flag will tell OS to always finish the activity when user leaves it
		                //when this was on, it was exiting every time it got created. interesting unexpected behavior
		                //might be able to be utilized as an all button instant unlock mode
		                //need to investigate the focus loss to figure this out
		                //| Intent.FLAG_ACTIVITY_NO_ANIMATION)
		                //because we don't need to animate... O_o doesn't really seem to be for this
		        
		        context.startActivity(lockscreen);
		}
	
	public void DoExit(Context context) {//try the alpha keyguard manager secure exit
    	
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
    }
	
	public void StartDismiss(Context context) {
    	
    	Class w = DismissKeyguardActivity.class;
    	
    		    	      
		Intent dismiss = new Intent(context, w);
		dismiss.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK//For some reason it requires this even though we're already an activity
				| Intent.FLAG_ACTIVITY_NO_USER_ACTION//Just helps avoid conflicting with other important notifications
		        | Intent.FLAG_ACTIVITY_NO_HISTORY//Ensures the activity WILL be finished after the one time use
		        | Intent.FLAG_ACTIVITY_NO_ANIMATION);
		        
		context.startActivity(dismiss);
    	
    }
	
	
	//essentially anytime a call ends it seems our next screen off is not creating the lockscreen again
	//we have a few cases
	//1- call ends while screen was off. user turns on screen to find the regular lockscreen
	//2- call ends while screen is on, user sees the lockscreen. if sleep, next wakeup still has lockscreen
	//3- incoming call is missed or ignored results in seeing the lockscreen also
	//TODO use 2.1 pm check for screen on to make these more effective
	
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
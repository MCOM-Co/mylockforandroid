package i4nc4mp.myLock;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.provider.Settings.SettingNotFoundException;


public class NoLockService extends MediatorService {
	
	public boolean persistent = false;
	
	public boolean shouldLock = true;
	//when true screen off will start the lockscreen.
	//we will ensure that it is false until user exits lockscreen or finishes a call
	
	public int patternsetting = 0;
	//we'll see if the user has pattern enabled when we startup
	//so we can disable it and then restore when we finish
	
	@Override
	public void onDestroy() {
		super.onDestroy();
		
		if (patternsetting == 1) {
			android.provider.Settings.System.putInt(getContentResolver(), 
    			android.provider.Settings.System.LOCK_PATTERN_ENABLED, 1);
    	//re-enable pattern lock if applicable
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
	}
	
	@Override
	public void onScreenSleep() {
		//when sleep after an unlock, start the lockscreen again
		
		if (receivingcall || placingcall || !shouldLock) return;
		//don't handle during calls at all
		//the should flag is for extra safety in case we ever find another exception case
		
		//TODO implement a 5 second delay which would play nice with the re-lock grace period
        //as things stand, it doesn't really work if you try to interrupt a timeout sleep with more input immediately
        StartLock(getApplicationContext());
		
		
		return;//prevents unresponsive broadcast error
	}
	
	private void StartLock(Context context) {

		Intent closeDialogs = new Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS);
		        context.sendBroadcast(closeDialogs);
		        
		       Class w = ShowWhenLockedActivity.class; 
		       
		       

		/* launch UI, explicitly stating that this is not due to user action
		         * so that the current app's notification management is not disturbed */
		        Intent lockscreen = new Intent(context, w);
		        
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
		
		//if (shouldLock)
		//the case that user placed a call or got one while actively doing something else on device
		//doesn't demand special handling at the moment
		//could fire a toast message like lite mode
		
		shouldLock = false;
		//flag so that lockscreen won't try to happen if screen off goes off in the middle of a call
		//extra redundancy in this mode where we never wake the lockscreen
	}
	
	@Override
	public void onCallEnd() {
		//Account for the case that a call ends while screen is asleep
		
		if (!IsAwake()) {
			//We actually need to start lock since screen is off and lockscreened
			StartLock(getApplicationContext());
		}
		
		shouldLock = true;//awake and unlocked state => ensure next screen off does StartLock
		//redundancy flag telling our screen off that we want to lock
	}
	
	@Override
	public void onCallMiss() {
		if (!IsAwake()) {
			//We actually need to start lock since screen is off and lockscreened
			StartLock(getApplicationContext());//lock again since user didn't react to this wake
		}
		
		shouldLock = true;
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
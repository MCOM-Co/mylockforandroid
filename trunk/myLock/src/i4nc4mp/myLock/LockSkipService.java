package i4nc4mp.myLock;


import i4nc4mp.myLock.ManageKeyguard.LaunchOnKeyguardExit;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Handler;
import android.provider.Settings.SettingNotFoundException;
import android.util.Log;

//alpha 2c method utilizing the force delayed secure exit
//really no better than just putting the self destroying dismiss activity at wakeup

public class LockSkipService extends MediatorService {
	private Handler serviceHandler;
	private Task myTask = new Task();
	//we use a handler and a task thread to cleanly get final keyguard exit on every wakeup
	
	public boolean persistent = false;
	
	public boolean unlocked = false;
	//true anytime we successfully disable lockscreen
	//and anytime a call begins (calls already prevent lockscreen)
	
	public int patternsetting = 0;
	//we'll see if the user has pattern enabled when we startup
	//so we can disable it and then restore when we finish
	
	@Override
	public void onDestroy() {
		super.onDestroy();
		serviceHandler.removeCallbacks(myTask);
		serviceHandler = null;
		//destroy the handler
		
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
		
		serviceHandler = new Handler();
		
		if(wake) ManageWakeLock.acquireFull(getApplicationContext());
        //probably not a common case
        //if user happens to leave stay awake on all the time
        //initialize it here, otherwise always done at time of toggle in settings
        
        if (persistent) doFGstart(wake);
        
    /*===pattern mode disabler===*/
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
	
	class Task implements Runnable {
		public void run() {
			ManageKeyguard.exitKeyguardSecurely(new LaunchOnKeyguardExit() {
	            public void LaunchOnKeyguardExitSuccess() {
	               Log.v("start", "This is the exit callback");
	               unlocked = true;
	                }});
			}
		}
	
	@Override
	public void onScreenWakeup() {
		if(!unlocked) {
			//only attempt when we know re-lock has occurred
			
			ManageKeyguard.disableKeyguard(getApplicationContext());
			serviceHandler.postDelayed(myTask, 50L);
			//call a secure exit to tell the OS we want to stay out of KG
    		//the delay seems to allow us to properly read that the kg has been disabled
		}
	}
	
	@Override
	public void onScreenSleep() {
		if (unlocked) {
        	if (!receivingcall && !placingcall) unlocked = false;
        	//only reset the unlocked flag when no calls are active
        	
        	//with this complete mode the lockscreen automatically restores after secure exit
        	//so no deliberate restore call is needed
		}
	}
	
	@Override
	public void onCallStart() {
		//Account for the case that a call starts while screen is asleep
		
		//if (unlocked) {
		//the case that user placed a call or got one while doing something else on device
		//} else
		unlocked = true;
	}
	
	@Override
	public void onCallEnd() {
		//Account for the case that a call ends while screen is asleep
		
		//if (IsAwake()) {
			//the case that screen is on when the call ends
			//TODO exit the lockscreen once we narrow down the cases it comes back
		//	}
		unlocked = false;
		//ensures that next screen on properly exits lockscreen
	}
	
	//all the state logic appears to be working for this mediator in the emulator
	
	void doFGstart(boolean wakepref) {
		//putting ongoing notif together for start foreground
		
		//String ns = Context.NOTIFICATION_SERVICE;
		//NotificationManager mNotificationManager = (NotificationManager) getSystemService(ns);
		//No need to get the mgr, since we aren't manually sending this for FG mode.
		
		int icon = R.drawable.icon;
		CharSequence tickerText = "disable lockscreen mode";
		
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
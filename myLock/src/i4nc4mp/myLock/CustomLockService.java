package i4nc4mp.myLock;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.provider.Settings.SettingNotFoundException;


//custom lockscreen mode
public class CustomLockService extends MediatorService {
	
	public boolean persistent = false;
	
	public boolean unlocked = true;
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
			else {//so if the pref isn't being changed, it means this restart is from lockscreen finish
		
		//unlock in the lockscreen activity causes start command to come back
		//react by prepping the next StartLock
				
		//if (unlocked) Log unexpected restart occurred.
		unlocked = true;
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
		
		if (receivingcall || placingcall) return;//don't handle during calls at all
		
		if (unlocked) {
        	unlocked = false;
        	StartLock(getApplicationContext());
		}
		
		return;//prevents unresponsive broadcast error
	}
	
	private void StartLock(Context context) {

		Intent closeDialogs = new Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS);
		        context.sendBroadcast(closeDialogs);
		        

		        Class w = Lockscreen.class;
		       

		/* launch UI, explicitly stating that this is not due to user action
		         * so that the current app's notification management is not disturbed */
		        Intent lockscreen = new Intent(context, w);
		        
		        lockscreen.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
		                | Intent.FLAG_ACTIVITY_NO_USER_ACTION
		                | Intent.FLAG_ACTIVITY_NO_ANIMATION);
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
		
		if (unlocked) {
		//the case that user placed a call or got one while actively doing something else on device
		}
		unlocked = false;
	}
	
	@Override
	public void onCallEnd() {
		//Account for the case that a call ends while screen is asleep
		
		if (!IsAwake()) {
			//We actually need to start lock since screen is off and lockscreened
			StartLock(getApplicationContext());
		}
		else unlocked = true;//awake and unlocked state to ensure next screen off does StartLock
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
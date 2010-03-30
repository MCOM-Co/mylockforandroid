package i4nc4mp.myLockGuarded;

//We need a separate mediator subclass here because we will use a show_when_locked version of the lockscreen
//this way users can have a lockscreen to control music even during pattern lockdown.
//this method is more secure and convenient than disabling secure mode if all they need is basic control /info viewing while in lockdown

//we will not complete the lifecycle till we have a user present broadcast meaning they did actually unlock the device

import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Handler;
import android.util.Log;


//custom lockscreen mode
public class SecureLockService extends MediatorService {
	
	public boolean persistent = false;
	public boolean stayawake = false;
	
/* Life-Cycle Flags */
		
	public boolean PendingLock = false;
	//Flagged true upon sleep, remains true until StartLock happens or user aborts the sleep within the 5 sec grace period
	
	
	Handler serviceHandler;
	Task myTask = new Task();
	
	@Override
	public void onDestroy() {
		super.onDestroy();
		
		if (stayawake) ManageWakeLock.releaseFull();
	
		
			serviceHandler.removeCallbacks(myTask);
		    serviceHandler = null;
		    
		    ManageWakeLock.releasePartial();
		    
}

	@Override
	public void onRestartCommand() {
		
		SharedPreferences settings = getSharedPreferences("myLock", 0);
		boolean fgpref = settings.getBoolean("FG", true);
		boolean wake = settings.getBoolean("StayAwake", false);
/*========Settings change re-start commands that come from settings activity*/
		if (stayawake != wake) {
			//this start is coming from user toggle of stay awake
			//react by getting or releasing the wakelock as this can only come while screen is on
			if (wake) ManageWakeLock.acquireFull(getApplicationContext());
			else ManageWakeLock.releaseFull();
			stayawake = wake;
		}//else is used to ensure neither setting change happened before proceeding to treat as a Lock Activity callback
		else if (persistent != fgpref) {//user changed pref
			if (persistent) {
					stopForeground(true);//kills the ongoing notif
					persistent = false;
			}
			else doFGstart(stayawake);//so FG mode is started again
		}
		else {
/*========Safety start that ensures the settings activity toggle button can work, first press to start, 2nd press to stop*/
				Log.v("toggle request","user first press of toggle after a startup at boot");
			}		
		}
	//this mediator never needs callbacks from the show when locked activity.
	//it simply exits on back press, then user has the pattern screen
	//so this mediator is safe to always start the activity at screen off once kg mode is detected.
	//we no longer need a startlock flag, the pending lock takes care of doing and cancelling the sleep timeout checker
	
	@Override
	public void onFirstStart() {
		SharedPreferences settings = getSharedPreferences("myLock", 0);
		persistent = settings.getBoolean("FG", true);
		stayawake = settings.getBoolean("StayAwake", false);
		
		if(stayawake) ManageWakeLock.acquireFull(getApplicationContext());
		if (persistent) doFGstart(stayawake);
		//else send a toast telling user what mode is starting and whether stay awake is active
		//perhaps do that in the boot handler service
		
		//we only work effectively if pattern mode is on but really if it isn't user will get regular slide unlock at pressing back
		
		serviceHandler = new Handler();
		ManageWakeLock.acquirePartial(getApplicationContext());
		//if not always holding partial we would only acquire at Lock activity exit callback
		//we found we always need it to ensure key events will not occasionally drop on the floor from idle state wakeup
		
	}
	
	
	class Task implements Runnable {
    	public void run() {
    		Context mCon = getApplicationContext();
    		Log.v("startLock task","executing, PendingLock is " + PendingLock);
    		if (!PendingLock) return;//ensures break the attempt cycle if user has aborted the lock
    		//user can abort Power key lock by another power key, or timeout sleep by any key wakeup
    		
    		//see if any keyguard exists yet
    			ManageKeyguard.initialize(mCon);
    			if (ManageKeyguard.inKeyguardRestrictedInputMode()) {
    				
    				//the keyguard exists here on first try if this isn't a timeout lock
    				StartLock(mCon);//take over the lock
    			}
    			else serviceHandler.postDelayed(myTask, 500L);
    			
    				
    		}    		
    	}
	
	@Override
	public void onScreenWakeup() {
		if (!PendingLock) return;
		//we only handle this when we get a screen on that's happening while we are waiting for a lockscreen start callback
		
		//Context mCon = getApplicationContext();		
		
		
		
		
		//This case comes in two scenarios
		//Known bug (seems to be fixed)--- the start of LockActivity was delayed to screen on due to CPU load
		//User aborting a timeout sleep by any key input before 5 second limit
												
			PendingLock = false;
			//flag swap to cancel the KG check task
			

			
		return;
	}
	
	
	@Override
	public void onScreenSleep() {
		//when sleep after an unlock, start the lockscreen again
		
		if (receivingcall || placingcall) {
			Log.v("mediator screen off","call flag in progress, aborting handling");
			return;//don't handle during calls at all
		}
		
		    PendingLock = true;
        	
        
        	
        	
        	Log.v("mediator screen off","sleep - starting check for keyguard");

        	serviceHandler.postDelayed(myTask, 500L);
        	
        
		
		return;//prevents unresponsive broadcast error
	}
	
	private void StartLock(Context context) {
		PendingLock = false;
		//now release wake lock if in stay awake mode
		if (stayawake) ManageWakeLock.releaseFull();
		//this is because we want the lock activity to do a 5 second timeout, that's the best handling for locked down key events
		
		Intent closeDialogs = new Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS);
		        context.sendBroadcast(closeDialogs);
		
		        
		

		        Class w = ShowWhenLockedActivity.class;
		       

		/* launch UI, explicitly stating that this is not due to user action
		         * so that the current app's notification management is not disturbed */
		        Intent lockscreen = new Intent(context, w);
		        
		        
		      //new task required for our service activity start to succeed. exception otherwise
		        lockscreen.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
		                | Intent.FLAG_ACTIVITY_NO_USER_ACTION);
		               
		        
		                //| Intent.FLAG_ACTIVITY_NO_HISTORY
		                //this flag will tell OS to always finish the activity when user leaves it
		                //when this was on, it was exiting every time it got created. interesting unexpected behavior
		                //even happening when i wait 4 seconds to create it.
		                //| Intent.FLAG_ACTIVITY_NO_ANIMATION)
		                //because we don't need to animate... O_o doesn't really seem to be for this
		        
		        context.startActivity(lockscreen);
		}
	
	
	//TODO call start and end may need to attempt to release/gain wakelock while in stayawake mode.
	//might not matter so i will add it if i find unexpected behavior for calls
	
	//FIXME in this mode since we never care about cancelling the lockscreen ourselves
	//we don't need to handle any specific call event.
	
	void doFGstart(boolean wakepref) {
		//putting ongoing notif together for start foreground
		
		//String ns = Context.NOTIFICATION_SERVICE;
		//NotificationManager mNotificationManager = (NotificationManager) getSystemService(ns);
		//No need to get the mgr, since we aren't manually sending this for FG mode.
		
		int icon = R.drawable.icon;
		CharSequence tickerText = "myLock is starting up";
		
		if (wakepref) tickerText = tickerText + " (Staying Awake)";
		
		long when = System.currentTimeMillis();

		Notification notification = new Notification(icon, tickerText, when);
		
		Context context = getApplicationContext();
		CharSequence contentTitle = "myLock - click to open settings";
		CharSequence contentText = "secure lockscreen active";

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
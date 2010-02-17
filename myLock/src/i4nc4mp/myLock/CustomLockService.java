package i4nc4mp.myLock;

import i4nc4mp.myLock.ManageKeyguard.LaunchOnKeyguardExit;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.PowerManager;
import android.os.SystemClock;
import android.provider.Settings.SettingNotFoundException;
import android.util.Log;


//custom lockscreen mode
public class CustomLockService extends MediatorService {
	
	public boolean persistent = false;
	public boolean stayawake = false;
	public boolean timeoutenabled = false;
	
	public int timeoutpref = 15;
	
	public int patternsetting = 0;
	//we'll see if the user has pattern enabled when we startup
	//so we can disable it and then restore when we finish
	
/* Life-Cycle Flags */
	public boolean shouldLock = true;
	//Flagged true upon Lock Activity exit callback, remains true until StartLock intent is fired.
		
	public boolean PendingLock = false;
	//Flagged true upon sleep, remains true until StartLock sends first callback indicating Create success.
	
	public boolean HandlingCallEnd = false;
	//the task will intercept if lockscreen comes back at the end of call
	public int count = 4;//only try for 2 seconds at end of call, then assume it's not locked
	
	public boolean idle = false;
	//when the idle alarm intent comes in we set this true to properly start closing down
	
	Handler serviceHandler;
	Task myTask = new Task();
	
	@Override
	public void onDestroy() {
		super.onDestroy();
		
		if (stayawake) ManageWakeLock.releaseFull();
		
		if (patternsetting == 1) {
			android.provider.Settings.System.putInt(getContentResolver(), 
    			android.provider.Settings.System.LOCK_PATTERN_ENABLED, 1);
    	//re-enable pattern lock if applicable
		}
			serviceHandler.removeCallbacks(myTask);
		    serviceHandler = null;
		    
		    unregisterReceiver(idleExit);
		    
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
/*========LockActivity restart calls that happen when it finishes starting or stopping*/
			if (PendingLock) {
			//start success, let's toggle pending off and set timeout setting
				PendingLock = false;
				Log.v("Received Callback","Lock Activity is primed");
				
				//brought in the timeout settings changer -- this way all types of exits will restore the correct timeout
				try {
		            timeoutpref = android.provider.Settings.System.getInt(getContentResolver(), android.provider.Settings.System.SCREEN_OFF_TIMEOUT);
		    } catch (SettingNotFoundException e) {
		            // TODO Auto-generated catch block
		            e.printStackTrace();
		    }//this setting will be restored at finish
		    
		    //Next, change the setting to 0 seconds
		    android.provider.Settings.System.putInt(getContentResolver(), 
		            android.provider.Settings.System.SCREEN_OFF_TIMEOUT, 0);
		    
		    //========right here I would release partial if only holding during the creation of lockscreen (acquired at screen off)
		    
		    if (timeoutenabled) IdleTimer.start(getApplicationContext());
		    
		    
		    //if we don't get user unlock callback within user-set idle timeout
		    //this alarm kills off the lock activity and this service, restores KG, & starts the user present service
		    
		    //TODO we're going to want to start at stop it within the activity wakeup as well
		    //example: stop when user deliberately wakes lockscreen to use it
		    //start again if sleeping again from that screenwake
			}
			else if (!shouldLock) {
					//this is the actual case that we just exited lockscreen
				shouldLock = true;
				
				
				Log.v("Received Callback","Lock Activity is finished");
				
				
				//ManageWakeLock.acquirePartial(getApplicationContext());
				//ensure the task can run, we'll release it on the startup callback
				//as of now, always holding partial just to get key events correctly.
				//we run no load on the cpu while device is asleep, so causes no power use or battery life reduction
								
				
				if (!idle) {
				if (timeoutenabled) IdleTimer.cancel(getApplicationContext());
					
				android.provider.Settings.System.putInt(getContentResolver(), android.provider.Settings.System.SCREEN_OFF_TIMEOUT, timeoutpref);
				
				PowerManager pm = (PowerManager) getSystemService (Context.POWER_SERVICE); 
		    	pm.userActivity(SystemClock.uptimeMillis(), false);
		    	
		    	if (stayawake) ManageWakeLock.acquireFull(getApplicationContext());
				}
				else {				
					ManageKeyguard.reenableKeyguard();
					//funny - you will see the regular lockscreen after this call because it is restoring it from time that pattern was off
					//if you slide that, you land at the security pattern screen ;]
					//otherwise if it sleeps like that, next wakeup places us at pattern screen
					
					Intent u = new Intent();
			    	u.setClassName("i4nc4mp.myLock", "i4nc4mp.myLock.UserPresentService");
			    	//service that reacts to the completion of the keyguard to start this mediator again
			    	startService(u);
					stopSelf();
				}
				}
			else {
/*========Safety start that ensures the settings activity toggle button can work, first press to start, 2nd press to stop*/
				Log.v("toggle request","user first press of toggle after a startup at boot");
			}		
		}
	}
	
	@Override
	public void onFirstStart() {
		SharedPreferences settings = getSharedPreferences("myLock", 0);
		persistent = settings.getBoolean("FG", true);
		stayawake = settings.getBoolean("StayAwake", false);
		timeoutenabled = settings.getBoolean("timeout", false);
		
		if(stayawake) ManageWakeLock.acquireFull(getApplicationContext());
		if (persistent) doFGstart(stayawake);
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
		
				
		serviceHandler = new Handler();
		ManageWakeLock.acquirePartial(getApplicationContext());
		//if not always holding partial we would only acquire at Lock activity exit callback
		//we found we always need it to ensure key events will not occasionally drop on the floor from idle state wakeup
		
		IntentFilter idleFinish = new IntentFilter ("i4nc4mp.myLock.intent.action.IDLE_TIMEOUT");
		registerReceiver(idleExit, idleFinish);
	}
	
	BroadcastReceiver idleExit = new BroadcastReceiver() {
		@Override
	    public void onReceive(Context context, Intent intent) {
		if (!intent.getAction().equals("i4nc4mp.myLock.intent.action.IDLE_TIMEOUT")) return;
		//we don't respond to this if in call because that means we sent it ourselves to kill the lockactivity.
		//also provides a safety check since the alarm might not be cancelled if a call starts
		//we don't want to shut ourselves down if user answered a call.
		if (!receivingcall && !placingcall) {
			idle = true;
		
		PowerManager myPM = (PowerManager) getApplicationContext().getSystemService(Context.POWER_SERVICE);
	  	myPM.userActivity(SystemClock.uptimeMillis(), true);
	  	
	  	Log.v("mediator idle reaction","preparing to restore KG. timeout pref is " + timeoutpref);
	  	
	  	android.provider.Settings.System.putInt(getContentResolver(),
				android.provider.Settings.System.SCREEN_OFF_TIMEOUT, timeoutpref);
	  	
	  	//the idle flag will cause proper handling on receipt of the exit callback from lockscreen
	  	//we basically instant unlock as if user requested, but then force KG back on.
		}
	}};
	
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
    				shouldLock = false;
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
			if (!shouldLock) {
				//this is the case that the lockscreen still hasn't sent us a start callback at time of this screen on
				shouldLock = true;
				//the activity itself will catch this case
				//possible we might need to implement a special intent that we could send at this occurrence
				//to notify the existing instance of the lockscreen
			}
				
			//the failure to start at off bug appears to be fully eliminated.
			//TODO might be smart to have logic in the activity lifecycle which can handle the starting at screen on case
			//FIXME we still don't have any good reaction to a user aborting a Power key sleep by shortly doing a 2nd power key press
			
		return;
	}
	
	
	@Override
	public void onScreenSleep() {
		//when sleep after an unlock, start the lockscreen again
		
		if (receivingcall || placingcall) {
			Log.v("mediator screen off","call flag in progress, aborting handling");
			return;//don't handle during calls at all
		}
		
		if (shouldLock) {
        	PendingLock = true;
        	//means trying to start lock (waiting for start callback from activity)
        
        	
        	//FIXME stay awake mode is probably preventing the logic in the quiet wake that flags that off
        	//---need to move more logic into the timeout task we have running as a screen off will never come in to flag cpuwake off
        	
        	Log.v("mediator screen off","sleep - starting check for keyguard");

        	serviceHandler.postDelayed(myTask, 500L);
        	}
        
		
		return;//prevents unresponsive broadcast error
	}
	
	private void StartLock(Context context) {

		//now release wake lock if in stay awake mode
		if (stayawake) ManageWakeLock.releaseFull();
		//this is because we want the lock activity to do a 5 second timeout, that's the best handling for locked down key events
		
		Intent closeDialogs = new Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS);
		        context.sendBroadcast(closeDialogs);
		
		        
		if (timeoutenabled) ManageKeyguard.disableKeyguard(getApplicationContext());
		//this just calls a temporary KG pause. doing this allows us to be recognized when we later want to re-enable
		//we only need this when the timeout mode is active

		        Class w = Lockscreen.class;
		        //Class w = ShowWhenLockedActivity.class; no button customize, uses secure exit to unlock
		       

		/* launch UI, explicitly stating that this is not due to user action
		         * so that the current app's notification management is not disturbed */
		        Intent lockscreen = new Intent(context, w);
		        
		        
		      //new task required for our service activity start to succeed. exception otherwise
		        lockscreen.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
		                | Intent.FLAG_ACTIVITY_NO_USER_ACTION);
		       //without this flag my alarm clock only buzzes once. but with it wakeup doesn't want to happen (also impacts handcent notifs)
		               
		        
		                //| Intent.FLAG_ACTIVITY_NO_HISTORY
		                //this flag will tell OS to always finish the activity when user leaves it
		                //when this was on, it was exiting every time it got created. interesting unexpected behavior
		                //even happening when i wait 4 seconds to create it.
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
	
//============Phone call case handling
	
	//we have many cases where the phone reloads the lockscreen even while screen is awake at call end
	//my testing shows it actually comes back after any timeout sleep plus 5 sec grace period
	//then phone is doing a KM disable command at re-wake. and restoring at call end
	

	//TODO call start and end may need to attempt to release/gain wakelock while in stayawake mode.
	//might not matter so i will add it if i find unexpected behavior for calls
	
	@Override
	public void onCallStart() {
		
		shouldLock = false;
		//send the idle exit intent to the lock activity
		//this way we don't exit it till call is answered, allowing it to be restored if call was ignored or missed
		
		Intent intent = new Intent("i4nc4mp.myLock.intent.action.IDLE_TIMEOUT");
		getApplicationContext().sendBroadcast(intent);
		
	}
	
	@Override
	public void onCallEnd() {
		//TODO 2.1 lets us check whether the screen is on
		
		//all timeout sleep causes KG to visibly restore after the 5 sec grace period
		//the phone appears to be doing a KM disable to pause it should user wake up again, and then re-enables at call end
		
		//if call ends while asleep and not in the KG-restored mode (watching for prox wake)
		//then KG is still restored, and we can't catch it due to timing
		
		//therefore, all calls ending while screen is off result in restart lockactivity
		//if screen is awake we check for KG, exit if needed, and reset shouldLock to true
		
		Context mCon = getApplicationContext();
		
		/*ManageKeyguard.initialize(mCon);
		 
		if (ManageKeyguard.inKeyguardRestrictedInputMode()) {
			Log.v("call end","lockscreen was restored due to screen timeout during the call, trying to exit");
			if (IsAwake()) {
				shouldLock = true;
				DoExit(mCon);
			}
			else {
				PendingLock = true;
				StartLock(mCon);
			}
		}
		else if(IsAwake()) shouldLock = true;
		else {
			PendingLock = true;
			StartLock(mCon);
		}*/
		
		if (IsAwake()) {
			Log.v("call end, screen awake","checking if we need to exit KG");
			shouldLock = true;
			ManageKeyguard.initialize(mCon);
			if (ManageKeyguard.inKeyguardRestrictedInputMode()) DoExit(mCon);
		}
		else {
			Log.v("call end, screen asleep","restarting lock activity.");
			PendingLock = true;
			StartLock(mCon);
		}
	}
	
	@Override
	public void onCallMiss() {
		//implemented finish intent sent to lockscreen when call is actually answered.
		//here, the lockscreen will be back on only able to be exited by home key (since it will not be in screenwake state)
		//for now i am not going to change this
		//shouldLock = true;
	}
	
//============================
	
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
		CharSequence contentText = "lockscreen is disabled";

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
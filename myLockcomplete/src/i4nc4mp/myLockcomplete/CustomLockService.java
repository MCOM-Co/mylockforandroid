package i4nc4mp.myLockcomplete;

import i4nc4mp.myLockcomplete.ManageKeyguard.LaunchOnKeyguardExit;
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


//advanced mode- lockscreen wakes up with any key
//supports advanced power save for setting locked-down buttons (screen stays off)
//or for reducing the timeout while in lockscreen mode
public class CustomLockService extends MediatorService {
	
	public boolean persistent = false;
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
	
	
	public boolean idle = false;
	//when the idle alarm intent comes in we set this true to properly start closing down
	
	Handler serviceHandler;
	Task myTask = new Task();
	
	@Override
	public void onDestroy() {
		super.onDestroy();
		
		if (patternsetting == 1) {
			android.provider.Settings.System.putInt(getContentResolver(), 
    			android.provider.Settings.System.LOCK_PATTERN_ENABLED, 1);
    	//re-enable pattern lock if applicable
		}
			serviceHandler.removeCallbacks(myTask);
		    serviceHandler = null;
		    
		    unregisterReceiver(idleExit);
		    unregisterReceiver(lockStarted);
		    unregisterReceiver(lockStopped);
		    //unregisterReceiver(homeUnlock);
		    
		    
		    ManageWakeLock.releasePartial();
		    
}

	@Override
	public void onRestartCommand() {
		
		SharedPreferences settings = getSharedPreferences("myLock", 0);
		boolean fgpref = settings.getBoolean("FG", true);
		
		//int newtimeout = 0;
		
		//FIXME - implementing implicit intent for lifecycle callbacks.
		//we can use plain broadcast receivers like we do for the idle event.
		
/*========Settings change re-start commands that come from settings activity*/
	
		if (persistent != fgpref) {//user changed pref
			if (persistent) {
					stopForeground(true);//kills the ongoing notif
					persistent = false;
			}
			else doFGstart();//so FG mode is started again
		}	
		else {
/*========Safety start that ensures the settings activity toggle button can work, first press to start, 2nd press to stop*/
				Log.v("toggle request","user first press of toggle after a startup at boot");
			}		
	}
	
	@Override
	public void onFirstStart() {
		SharedPreferences settings = getSharedPreferences("myLock", 0);
		persistent = settings.getBoolean("FG", true);
		
		timeoutenabled = settings.getBoolean("timeout", false);
		
		
		if (persistent) doFGstart();
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
		
		IntentFilter idleFinish = new IntentFilter ("i4nc4mp.myLockcomplete.lifecycle.IDLE_TIMEOUT");
		registerReceiver(idleExit, idleFinish);
		
		IntentFilter lockStart = new IntentFilter ("i4nc4mp.myLockcomplete.lifecycle.LOCKSCREEN_PRIMED");
		registerReceiver(lockStarted, lockStart);
		
		IntentFilter lockStop = new IntentFilter ("i4nc4mp.myLockcomplete.lifecycle.LOCKSCREEN_EXITED");
		registerReceiver(lockStopped, lockStop);
		
		//IntentFilter home = new IntentFilter ("i4nc4mp.myLockcomplete.lifecycle.HOMEKEY_UNLOCK");
		//registerReceiver(homeUnlock, home);
	}
	
	BroadcastReceiver lockStarted = new BroadcastReceiver() {
		@Override
	    public void onReceive(Context context, Intent intent) {
		int newtimeout = 0;
			
		if (!intent.getAction().equals("i4nc4mp.myLockcomplete.lifecycle.LOCKSCREEN_PRIMED")) return;

		if (!PendingLock) Log.v("lock start callback","did not expect this call");
		else PendingLock = false;
		
		Log.v("lock start callback","Lock Activity is primed");
		
	//=====Advanced power save timeout reduction----
		//compare our stored user-pref to the currently held system entry
		//update the stored value only if it has changed, and is at least 15
		//FIXME need to store the value in our prefs file
		
			try {
		            newtimeout = android.provider.Settings.System.getInt(getContentResolver(), android.provider.Settings.System.SCREEN_OFF_TIMEOUT);
		    } catch (SettingNotFoundException e) {
		            // TODO Auto-generated catch block
		            e.printStackTrace();
		    }
		    
		    if (newtimeout != 1) timeoutpref = newtimeout;
		    else Log.v("lock start callback","the system timeout is already 1, our stored pref is " + timeoutpref);
		    //for now this code protects us from improperly overwriting our stored timeout pref with the reduced pref
		    
		    
		    //====Advanced power save timeout reduction
		    //-----always set the timeout to 1 at lockscreen start success
		    android.provider.Settings.System.putInt(getContentResolver(), 
		            android.provider.Settings.System.SCREEN_OFF_TIMEOUT, 1);
		    
		   		    
		    if (timeoutenabled) IdleTimer.start(getApplicationContext());
		    		    
		    //if we don't get user unlock callback within user-set idle timeout
		    //this alarm kills off the lock activity and this service, restores KG, & starts the user present service
		    
		    //TODO we're going to want to start at stop it within the activity wakeup as well
		    //example: stop when user deliberately wakes lockscreen to use it
		    //start again if sleeping again from that screenwake
					
	}};
	
	BroadcastReceiver lockStopped = new BroadcastReceiver() {
		@Override
	    public void onReceive(Context context, Intent intent) {
		if (!intent.getAction().equals("i4nc4mp.myLockcomplete.lifecycle.LOCKSCREEN_EXITED")) return;

		if (shouldLock) Log.v("lock exit callback","did not expect this call"); 
		else shouldLock = true;
				
				
		Log.v("lock exit callback","Lock Activity is finished");
										
			
		if (!idle) {
			if (timeoutenabled) IdleTimer.cancel(getApplicationContext());
			
			//FIXME
			//this should restore screen off to our known user-pref that is stored in prefs
			//that pref will be set at Lock start, when we detect a change that isn't to 0 or 1
			//(0 used by screebl, 1 is our advanced power save setting)
			android.provider.Settings.System.putInt(getContentResolver(), android.provider.Settings.System.SCREEN_OFF_TIMEOUT, timeoutpref);
				
			PowerManager pm = (PowerManager) getSystemService (Context.POWER_SERVICE); 
		   	pm.userActivity(SystemClock.uptimeMillis(), false);
		   	
		   	}
		else {				
				ManageKeyguard.reenableKeyguard();
				//funny - you will see the regular lockscreen after this call because it is restoring it from time that pattern was off
				//if you slide that, you land at the security pattern screen ;]
				//otherwise if it sleeps like that, next wakeup places us at pattern screen
					
				Intent u = new Intent();
		    	u.setClassName("i4nc4mp.myLockcomplete", "i4nc4mp.myLockcomplete.UserPresentService");
		    	//service that reacts to the completion of the keyguard to start this mediator again
		    	startService(u);
				stopSelf();
			}			
	}};
	
	/*BroadcastReceiver homeUnlock = new BroadcastReceiver() {
		@Override
	    public void onReceive(Context context, Intent intent) {
		if (!intent.getAction().equals("i4nc4mp.myLockcomplete.lifecycle.HOMEKEY_UNLOCK")) return;
		
		ManageKeyguard.disableKeyguard(context);
		StartDismiss(context);
		return;
		
		}};*/
	
	BroadcastReceiver idleExit = new BroadcastReceiver() {
		@Override
	    public void onReceive(Context context, Intent intent) {
		if (!intent.getAction().equals("i4nc4mp.myLockcomplete.lifecycle.IDLE_TIMEOUT")) return;
				
		idle = true;
		
		PowerManager myPM = (PowerManager) getApplicationContext().getSystemService(Context.POWER_SERVICE);
	  	myPM.userActivity(SystemClock.uptimeMillis(), true);
	  	
	  	Log.v("mediator idle reaction","preparing to restore KG. timeout pref is " + timeoutpref);
	  	
	  	android.provider.Settings.System.putInt(getContentResolver(),
				android.provider.Settings.System.SCREEN_OFF_TIMEOUT, timeoutpref);
	  	
	  	//the idle flag will cause proper handling on receipt of the exit callback from lockscreen
	  	//we basically unlock as if user requested, but then force KG back on in the callback reaction
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
			
				
		//This case comes in two scenarios
		//Known bug (seems to be fixed)--- the start of LockActivity was delayed to screen on due to CPU load
		//User aborting a timeout sleep by any key input before 5 second limit
												
			PendingLock = false;
			if (!shouldLock) {
				//this is the case that the lockscreen still hasn't sent us a start callback at time of this screen on
				shouldLock = true;
			}
				
			
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
		
		Intent closeDialogs = new Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS);
		        context.sendBroadcast(closeDialogs);
		
		        
		if (timeoutenabled) ManageKeyguard.disableKeyguard(getApplicationContext());
		//this just calls a temporary KG pause. doing this allows us to be recognized when we later want to re-enable
		//we only need this when the timeout mode is active

		        //Class w = Lockscreen.class;
		        Class w = GuardActivity.class;
		       

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
	
	public void StartDismiss(Context context) {
                
        Class w = DismissActivity.class; 
                      
        Intent dismiss = new Intent(context, w);
        dismiss.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK//For some reason it requires this even though we're already an activity
                        | Intent.FLAG_ACTIVITY_NO_USER_ACTION//Just helps avoid conflicting with other important notifications
                        | Intent.FLAG_ACTIVITY_NO_HISTORY//Ensures the activity WILL be finished after the one time use
                        | Intent.FLAG_ACTIVITY_NO_ANIMATION);
                        
        context.startActivity(dismiss);
    }
	
//============Phone call case handling
	
	//we have many cases where the phone reloads the lockscreen even while screen is awake at call end
	//my testing shows it actually comes back after any timeout sleep plus 5 sec grace period
	//then phone is doing a KM disable command at re-wake. and restoring at call end
	

	
	@Override
	public void onCallStart() {
		
		shouldLock = false;
		
		Intent intent = new Intent("i4nc4mp.myLockcomplete.lifecycle.CALL_START");
		getApplicationContext().sendBroadcast(intent);
		//activity closes when receiving this - FIXME the advanced mode Lockscreen is expecting idle intent
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
		
		if (IsAwake()) {
			Log.v("call end, screen awake","checking if we need to exit KG");
			shouldLock = true;
			ManageKeyguard.initialize(mCon);
			if (ManageKeyguard.inKeyguardRestrictedInputMode()) DoExit(mCon);
			//TODO change this to the dismiss activity now that we have timing bugs fixed
		}
		else {
			Log.v("call end, screen asleep","restarting lock activity.");
			PendingLock = true;
			StartLock(mCon);
		}
	}
	
	@Override
	public void onCallMiss() {
			
		Intent intent = new Intent("i4nc4mp.myLockcomplete.lifecycle.CALL_ABORT");
		getApplicationContext().sendBroadcast(intent);
		//lets the activity know it is regaining focus because call was aborted
	}
	
//============================
	
	void doFGstart() {
		//putting ongoing notif together for start foreground
		
		//String ns = Context.NOTIFICATION_SERVICE;
		//NotificationManager mNotificationManager = (NotificationManager) getSystemService(ns);
		//No need to get the mgr, since we aren't manually sending this for FG mode.
		
		int icon = R.drawable.icon;
		CharSequence tickerText = "myLock is starting up";
		
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
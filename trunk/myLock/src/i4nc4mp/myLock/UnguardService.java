package i4nc4mp.myLock;

import android.app.Activity;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;
import android.os.SystemClock;
import android.util.Log;
import android.view.WindowManager;

//simple dismiss activity that will exit self at wakeup
//this mode is for users who wish for all buttons to wake and unlock
//they want no guarding... probably those belt clip holster touting jerks

public class UnguardService extends MediatorService {
	private boolean persistent = false;
    private boolean timeoutenabled = false;
    
    private boolean security = false;
    
/* Life-Cycle Flags */
    private boolean shouldLock = true;
    //Flagged true upon Lock Activity exit callback, remains true until StartLock intent is fired.
            
    private boolean PendingLock = false;
    //Flagged true upon sleep, remains true until StartLock sends first callback indicating Create success.
    
    
    private static Handler myHandler;
    private int waited = 0;
    
    //The mediator service INSTANCE sets up the shared handler
    protected void initHandler() {
    	myHandler = new Handler() {
			public void handleMessage(Message msg) {
				super.handleMessage(msg);
				
				switch (msg.what) {
					case 0:
						handleLockEvent(true);
						break;
					case 1:
						handleLockEvent(false);
						break;
					default:
						break;
					}
				}
		};
    }
    
    @Override
    public void onDestroy() {
    	super.onDestroy();
            
        SharedPreferences settings = getSharedPreferences("myLock", 0);
            
        //toggle security back on
        	if (security) {
        		android.provider.Settings.System.putInt(getContentResolver(), 
        				android.provider.Settings.System.LOCK_PATTERN_ENABLED, 1);
        	}
                myHandler = null;
                
               
                
                settings.unregisterOnSharedPreferenceChangeListener(prefslisten);
                
                ManageWakeLock.releasePartial();
                
                
              //when we get closed, it might be due to locale or idle lock
            	if (!shouldLock) {
                    //if our lock activity is alive, send broadcast to close it
                    
                    Intent intent = new Intent("i4nc4mp.myLock.lifecycle.CALL_START");
                    getApplicationContext().sendBroadcast(intent);
                    }
}
    
    @Override
    public void onFirstStart() {
            SharedPreferences settings = getSharedPreferences("myLock", 0);
            
            persistent = settings.getBoolean("FG", false);
            security = settings.getBoolean("security", false);
                        
            if (persistent) doFGstart();
            
            timeoutenabled = (settings.getInt("idletime", 0) != 0);
            
            //register a listener to update this if pref is changed to 0
            settings.registerOnSharedPreferenceChangeListener(prefslisten);
            
            
            //toggle out of security
            if (security) {
            	android.provider.Settings.System.putInt(getContentResolver(), 
                    android.provider.Settings.System.LOCK_PATTERN_ENABLED, 0);
            }
            
                            
           initHandler();
            
            
            ManageWakeLock.acquirePartial(getApplicationContext());
            //if not always holding partial we would only acquire at Lock activity exit callback
            //we found we always need it to ensure key events will not occasionally drop on the floor from idle state wakeup        
    }
    
    @Override
    public void onRestartCommand() {
    	timeoutenabled = (getSharedPreferences("myLock", 0).getInt("idletime", 0) != 0);
    }
    
    SharedPreferences.OnSharedPreferenceChangeListener prefslisten = new OnSharedPreferenceChangeListener () {
    	@Override
    	public void onSharedPreferenceChanged (SharedPreferences sharedPreference, String key) {
    		Log.v("pref change","the changed key is " + key);
    		
      		if ("FG".equals(key)) {
    			boolean fgpref = sharedPreference.getBoolean(key, false);
    			if(!fgpref && persistent) {
    				stopForeground(true);//kills the ongoing notif
    			    persistent = false;
    			}
    			else if (fgpref && !persistent) doFGstart();//so FG mode is started again
      		}
    		}
    	};
    
    private void handleLockEvent(boolean newstate) {
    	if (newstate) {
    		if (!PendingLock) Log.v("lock start callback","did not expect this call");
    		else PendingLock = false;
        
    		Log.v("lock start callback","Lock Activity is primed");                
                            
    		if (timeoutenabled) {
    			Log.v("idle lock","starting timer");
    			IdleTimer.start(getApplicationContext());
        		}
    		}
    	else {
    		if (shouldLock) Log.v("lock exit callback","did not expect this call"); 
            else shouldLock = true;
                            
                            
            Log.v("lock exit callback","Lock Activity is finished");
                                                                            
            if (timeoutenabled) IdleTimer.cancel(getApplicationContext());
    	}
    }
    
    protected void tryLock() {
       	
    	//our thread will essentially wait for the start of lock activity
    	//there is a chance the KG is never detected due to sleep within dock app
    	//so we wait 10 seconds before assuming that is the case
    	new Thread() {

    	public void run() {
    		Context mCon = getApplicationContext();
    		do {	
    		try {
        			Thread.sleep(500);} catch (InterruptedException e) {
        			}
        			if (waited == 0) Log.v("tryLock thread","beginning KG check cycle");
                    if (!PendingLock) {
                    	Log.v("startLock user abort","detected wakeup before lock started");
                    	waited = 0;
                    	return;
                    //ensures break the attempt cycle if user has aborted the lock
                    //on incredible there is no grace period on timeout sleep, this case doesn't occur
                    }
                    
                    if (waited == 20) {
                    	Log.v("startLock abort","system or app seems to be suppressing lockdown");
                    	waited = 0;
                    	PendingLock = false;
                    	return;
                    }
                                        
                    //see if any keyguard exists yet
                            ManageKeyguard.initialize(mCon);
                            if (ManageKeyguard.inKeyguardRestrictedInputMode()) {                            	
                                    shouldLock = false;//set the state of waiting for lock start success
                                    waited = 0;
                                    StartLock(mCon);//take over the lock
                            }
                            else waited++;
                            	
                            
        		//myHandler.sendMessage(Message.obtain(myHandler, 2));
    		} while (shouldLock && PendingLock);
    	}
    	       }.start();
    	    }
    
    @Override
    public void onScreenWakeup() {
    	if (!shouldLock) {
        	//lock activity is active so let's populate the screen wake awareness
        	SharedPreferences settings = getSharedPreferences("myLock", 0);
            SharedPreferences.Editor editor = settings.edit();
            editor.putBoolean("screen", true);
            editor.commit();
        }    
    	
    	
    	if (!PendingLock) return;
            //we only handle this when we get a screen on that's happening while we are waiting for a lockscreen start callback
                    
                            
            //This case comes in two scenarios
            //Known bug (seems to be fixed)--- the start of LockActivity was delayed to screen on due to CPU load
            //This was fixed by always holding a partial wake lock. cpu decides to sleep sometimes, causing havoc
            
            //other possible case is just User-Abort of timeout sleep by any key during 5 sec unguarded interim
                                                                                            
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
    	SharedPreferences settings = getSharedPreferences("myLock", 0);
        SharedPreferences.Editor editor = settings.edit();
        editor.putBoolean("screen", false);
        editor.commit();
        //always populate our screen state ref for lock activity to check
        //we only switch the flag on during the lock activity life cycle    
    	
    	
            if (receivingcall || placingcall) {
                    Log.v("mediator screen off","call flag in progress, aborting handling");
                    return;//don't handle during calls at all
            }
            
            if (shouldLock) {
            PendingLock = true;
            //means trying to start lock (waiting for start callback from activity)
    
                                   
            Log.v("mediator screen off","sleep - starting check for keyguard");

            //serviceHandler.postDelayed(myTask, 500L);
            tryLock();
            }
    
            
            return;//prevents unresponsive broadcast error
    }
    
    private void StartLock(Context context) {
            
            Intent closeDialogs = new Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS);
                    context.sendBroadcast(closeDialogs);
            
                    
            //if (timeoutenabled) ManageKeyguard.disableKeyguard(getApplicationContext());
 
                   

            /* launch UI, explicitly stating that this is not due to user action
                     * so that the current app's notification management is not disturbed */
                    Intent lockscreen = new Intent(context, UnguardActivity.class);
                    
                    
                  //new task required for our service activity start to succeed. exception otherwise
                    lockscreen.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                            | Intent.FLAG_ACTIVITY_NO_USER_ACTION);
                   //without this flag my alarm clock only buzzes once.
                           
                    
                            //| Intent.FLAG_ACTIVITY_NO_HISTORY
                            //this flag will tell OS to always finish the activity when user leaves it
                            //when this was on, it was exiting every time it got created. interesting unexpected behavior
                            //even happening when i wait 4 seconds to create it.
                            //| Intent.FLAG_ACTIVITY_NO_ANIMATION)
                            //because we don't need to animate... O_o doesn't really seem to be for this
                    
                    context.startActivity(lockscreen);
            }
    
    public void StartDismiss(Context context) {
            
    	PowerManager myPM = (PowerManager) getSystemService(Context.POWER_SERVICE);
        myPM.userActivity(SystemClock.uptimeMillis(), false);
    	
    Class w = DismissActivity.class; 
                  
    Intent dismiss = new Intent(context, w);
    dismiss.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK//required for a service to launch activity
                    | Intent.FLAG_ACTIVITY_NO_USER_ACTION//Just helps avoid conflicting with other important notifications
                    | Intent.FLAG_ACTIVITY_NO_HISTORY//Ensures the activity WILL be finished after the one time use
                    | Intent.FLAG_ACTIVITY_NO_ANIMATION);
                    
    context.startActivity(dismiss);
}
    
//============Phone call case handling
    
    //we have many cases where the phone reloads the lockscreen even while screen is awake at call end
    //my testing shows it actually comes back after any timeout sleep plus 5 sec grace period
    //then phone is doing a KM disable command at re-wake. and restoring at call end
    //that restore is what we intercept in these events as well as certain treatment based on lock activity lifecycle
    
    @Override
    public void onCallStart() {
            
            if (!shouldLock) {
            //if our lock activity is alive, send broadcast to close it
            
            //this case we will also flag to restart lock at call end
            //callWake = true;
            
            Intent intent = new Intent("i4nc4mp.myLock.lifecycle.CALL_START");
            getApplicationContext().sendBroadcast(intent);
            //FIXME is there a way to do this with a class method now with inner class activity?
            }
            else shouldLock = false;
    }
    
    @Override
    public void onCallEnd() {
            //all timeout sleep causes KG to visibly restore after the 5 sec grace period
            //the phone appears to be doing a KM disable to pause it should user wake up again, and then re-enables at call end
            
            //if call ends while asleep and not in the KG-restored mode (watching for prox wake)
            //then KG is still restored, and we can't catch it due to timing
                        
            Context mCon = getApplicationContext();
            
            Log.v("call end","checking if we need to exit KG");
            
            ManageKeyguard.initialize(mCon);
            
            boolean KG = ManageKeyguard.inKeyguardRestrictedInputMode();
            //this will tell us if the phone ever restored the keyguard
            //phone occasionally brings it back to life but suppresses it
            
            boolean screen = isScreenOn();
            
            if (!screen) {
            	//asleep case, only detected on 2.1+
            	
            	Log.v("asleep call end","restarting lock activity.");
                PendingLock = true;
                StartLock(mCon);
            }
            else {
            	//awake or pre-2.1 (causing wakeup if asleep & locked)
            	shouldLock = true;
            	if (KG) StartDismiss(mCon);
            }
    }
    
    @Override
    public void onCallRing() {  	
    	Intent intent = new Intent("i4nc4mp.myLock.lifecycle.CALL_PENDING");
        getApplicationContext().sendBroadcast(intent);
        //lets the activity know it should not treat focus loss as a navigation exit
        //this will keep activity alive, only stopping it at call start
    }
    
    public boolean isScreenOn() {
    	//Allows us to tap into the 2.1 screen check if available
    	
    	if(Integer.parseInt(Build.VERSION.SDK) < 7) { 
    		
    		return true;
    		//our own isAwake doesn't work sometimes when prox sensor shut screen off
    		//better to treat as awake then to think we are awake when actually asleep
    		
    	}
    	else {
    		PowerManager myPM = (PowerManager) getApplicationContext().getSystemService(Context.POWER_SERVICE);
    		return myPM.isScreenOn();
    	}
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
    
    //The activity is wrapped by the mediator so we can interact with a static handler instance
    //The activity can be dismissed via back button
    //User configures what keys if any will fully auto unlock
    public static class UnguardActivity extends Activity {

    	protected void onCreate(Bundle icicle) {
            super.onCreate(icicle);
            
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);
                    
            setContentView(R.layout.lockdownlayout);
            //cool translucent overlay that shows what's behind
            
           
            
        
            }
    	
    	//first focus gain after first onStart is the official point of being initialized
    	//that's when we callback the mediator service
    	
    	//resume after that if have focus still means user wake
    	//loss of focus or getting stopped after that means events waking phone
    	
    	@Override
        public void onDestroy() {
            super.onDestroy();
        	
            Log.v("destroyWelcome","Destroying");
        }
    	
    }
}

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

//mediator subclass which places the basic guard for instant unlock via 2.0 optimized activity process
//this mediator only exists to mediate slider open vs regular wakes
//a slide open wake will place the config change first, then the resume with focus, then the screen on
//otherwise, just resume then screen on
//we don't have any good way to know if screen is on its way to being on or not during that resume
//we will need to implement the 2.1 is screen on PM method.. we just have to do without it right now
//impact is that occasionally a resume is happening when no user wake request had started
//this causes wakeup when the useractivity at the various points happens


//this class is now deprecated as the only thing it accomplishes is showing the wallpaper background
//we can't seem to do that without the show when locked function window
//sometimes it doesnt show but most times we see it while waiting for unlock
//some users seem to want this cosmetic function.... 
//thge slider guard is now implemented in auto dismiss mediator
//so reintegrating this is a todo that isn't in priority since i will need to revert the code 
//to the screen on event handling and starting dismiss from here instead of in the mediator

public class BasicGuardService extends MediatorService {
	
	public boolean persistent = false;
    public boolean timeoutenabled = false;
    
    public int patternsetting = 0;
    //we'll see if the user has pattern enabled when we startup
    //so we can disable it and then restore when we finish
    
/* Life-Cycle Flags */
    public boolean shouldLock = true;
    //Flagged true upon Lock Activity exit callback, remains true until StartLock intent is fired.
            
    public boolean PendingLock = false;
    //Flagged true upon sleep, remains true until StartLock sends first callback indicating Create success.
    
    public boolean callWake = false;
    //set this flag to handle restart guard activity after call that woke device ends
    
    public boolean idle = false;
    //when the idle alarm intent comes in we set this true to properly start closing down
    
    Handler serviceHandler;
    Task myTask = new Task();
    
    @Override
    public void onDestroy() {
    	super.onDestroy();
            
        SharedPreferences settings = getSharedPreferences("myLock", 0);
        SharedPreferences.Editor editor = settings.edit();
            
            if (patternsetting == 1) {
            	            	
                android.provider.Settings.System.putInt(getContentResolver(), 
                    android.provider.Settings.System.LOCK_PATTERN_ENABLED, 1);
                    
                
        		editor.putBoolean("securepaused", false);
        		
        		// Don't forget to commit your edits!!!
        		//editor.commit();
            }
                serviceHandler.removeCallbacks(myTask);
                serviceHandler = null;
                
                unregisterReceiver(idleExit);
                unregisterReceiver(lockStarted);
                unregisterReceiver(lockStopped);
                
                
                editor.putBoolean("serviceactive", false);
                editor.commit();
                
                ManageWakeLock.releasePartial();
                
}

    @Override
    public void onRestartCommand() {
            
            SharedPreferences settings = getSharedPreferences("myLock", 0);
            boolean fgpref = settings.getBoolean("FG", true);
                                 
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
            SharedPreferences.Editor editor = settings.edit();
            
            persistent = settings.getBoolean("FG", true);
            timeoutenabled = settings.getBoolean("timeout", false);
                        
            if (persistent) doFGstart();
                        
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
    
    			
    			editor.putBoolean("securepaused", true);
    			//will be flagged off on successful exit w/ restore of pattern requirement
    			//otherwise, it is caught by the boot handler... if myLock gets force closed/uninstalled
    			//there's no clean resolution to this pause.

    			// Don't forget to commit your edits!!!
    			//editor.commit();
            }
            
                            
            serviceHandler = new Handler();
            
            
            ManageWakeLock.acquirePartial(getApplicationContext());
            //if not always holding partial we would only acquire at Lock activity exit callback
            //we found we always need it to ensure key events will not occasionally drop on the floor from idle state wakeup
            
            IntentFilter idleFinish = new IntentFilter ("i4nc4mp.myLock.lifecycle.IDLE_TIMEOUT");
            registerReceiver(idleExit, idleFinish);
            
            IntentFilter lockStart = new IntentFilter ("i4nc4mp.myLock.lifecycle.LOCKSCREEN_PRIMED");
            registerReceiver(lockStarted, lockStart);
            
            IntentFilter lockStop = new IntentFilter ("i4nc4mp.myLock.lifecycle.LOCKSCREEN_EXITED");
            registerReceiver(lockStopped, lockStop);            
            
            editor.putBoolean("serviceactive", true);
            editor.commit();
    }
    
    BroadcastReceiver lockStarted = new BroadcastReceiver() {
            @Override
        public void onReceive(Context context, Intent intent) {
                    
            if (!intent.getAction().equals("i4nc4mp.myLock.lifecycle.LOCKSCREEN_PRIMED")) return;

            if (!PendingLock) Log.v("lock start callback","did not expect this call");
            else PendingLock = false;
            
            Log.v("lock start callback","Lock Activity is primed");                
                                
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
            if (!intent.getAction().equals("i4nc4mp.myLock.lifecycle.LOCKSCREEN_EXITED")) return;

            if (shouldLock) Log.v("lock exit callback","did not expect this call"); 
            else shouldLock = true;
                            
                            
            Log.v("lock exit callback","Lock Activity is finished");
                                                                            
                    
            if (!idle) {
                    if (timeoutenabled) IdleTimer.cancel(getApplicationContext());
                                        
        		   
                    }
            else {                          
                    ManageKeyguard.reenableKeyguard();//not necessary here in show when locked mode
                                                              
                    Intent u = new Intent();
                    u.setClassName("i4nc4mp.myLock", "i4nc4mp.myLock.UserPresentService");
                    //service that reacts to the completion of the keyguard to start this mediator again
                    startService(u);
                    stopSelf();
                    }                       
    }};
   
   BroadcastReceiver idleExit = new BroadcastReceiver() {
            @Override
        public void onReceive(Context context, Intent intent) {
            if (!intent.getAction().equals("i4nc4mp.myLock.lifecycle.IDLE_TIMEOUT")) return;
                            
            idle = true;
            
            PowerManager myPM = (PowerManager) getApplicationContext().getSystemService(Context.POWER_SERVICE);
            myPM.userActivity(SystemClock.uptimeMillis(), true);
            
            Log.v("mediator idle reaction","preparing to restore KG.");
                        
            //the idle flag will cause proper handling on receipt of the exit callback from lockscreen
            //we basically unlock as if user requested, but then force KG back on in the callback reaction
    }};
    
    class Task implements Runnable {
    public void run() {
            Context mCon = getApplicationContext();
            Log.v("startLock task","executing, PendingLock is " + PendingLock);
            if (!PendingLock) return;//ensures break the attempt cycle if user has aborted the lock
            //user can abort timeout sleep by any key wakeup
            
            
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
            
            if (receivingcall || placingcall) {
                    Log.v("mediator screen off","call flag in progress, aborting handling");
                    return;//don't handle during calls at all
            }
            
            if (shouldLock) {
            PendingLock = true;
            //means trying to start lock (waiting for start callback from activity)
    
                                   
            Log.v("mediator screen off","sleep - starting check for keyguard");

            serviceHandler.postDelayed(myTask, 500L);
            }
    
            
            return;//prevents unresponsive broadcast error
    }
    
    private void StartLock(Context context) {
            
            Intent closeDialogs = new Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS);
                    context.sendBroadcast(closeDialogs);
            
                    
            //if (timeoutenabled) ManageKeyguard.disableKeyguard(getApplicationContext());
            //not necessary in guarded mode since we dont cancel kg till user wakes device

                    Class w = BasicGuardActivity.class;
                    
                    //for user pref whether to use wallpaper, need to have a subclass
                    //since i can only define the wallpaper vs Theme standard in the activity def
                    //inside the manifest.
                   

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
            }
            else shouldLock = false;
    }
    
    @Override
    public void onCallEnd() {
            //all timeout sleep causes KG to visibly restore after the 5 sec grace period
            //the phone appears to be doing a KM disable to pause it should user wake up again, and then re-enables at call end
            
            //if call ends while asleep and not in the KG-restored mode (watching for prox wake)
            //then KG is still restored, and we can't catch it due to timing
            
            //right now we can't reliably check the screen state
    		//instead we will restart the guard if call came in waking up device
    		//otherwise we will just do nothing besides dismiss any restored kg
            
            Context mCon = getApplicationContext();
            
            Log.v("call end","checking if we need to exit KG");
            
            ManageKeyguard.initialize(mCon);
            
            boolean KG = ManageKeyguard.inKeyguardRestrictedInputMode();
            //this will tell us if the phone ever restored the keyguard
            //phone occasionally brings it back to life but suppresses it
            
            //2.1 isScreenOn will allow us the logic:
            
            //restart lock if it is asleep and relocked
            //dismiss lock if it is awake and relocked
            //do nothing if it is awake and not re-locked
            //wake up if it is asleep and not re-locked (not an expected case)
            
            //right now we will always dismiss as user expects that ONLY the slide open will be guarded
            //when awake and not slider opened it will need to be dismissed.
            //this basically means those times when phone wants to be asleep and locked
            //we are actually causing a wakeup/unlock,
            //but we can't respect that case till we have 2.1
            
            
            /*
            if (callWake) {
                    Log.v("wakeup call end","restarting lock activity.");
                    callWake = false;
                    PendingLock = true;
                    StartLock(mCon);
                    //when we restart here, the guard activity is getting screen on event
                    //and calling its own dismiss as if it was a user initiated wakeup
                    //TODO but this logic will be needed for guarded custom lockscreen version
            }
            else {
            	//KG may or may not be about to come back and screen may or may not be awake
            	//these factors depend on what the user did during call
            	//all we will do is dismiss any keyguard that exists, which will cause wake if it is asleep
            	//if (IsAwake()) {}
                    Log.v("call end","checking if we need to exit KG");
                    shouldLock = true;
                    if (KG) StartDismiss(mCon);
            }*/
            shouldLock = true;
            if (KG) StartDismiss(mCon);
            
    }
    
    @Override
    public void onCallRing() {  	
    	Intent intent = new Intent("i4nc4mp.myLock.lifecycle.CALL_PENDING");
        getApplicationContext().sendBroadcast(intent);
        //lets the activity know it should not treat focus loss as a navigation exit
        //this will keep activity alive, only stopping it at call start
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

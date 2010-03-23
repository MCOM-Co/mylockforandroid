package i4nc4mp.myLock;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.PowerManager;
import android.os.SystemClock;
import android.provider.Settings.SettingNotFoundException;
import android.util.Log;
//This one is the dismiss activity method but runs no guard activity
//it just dismisses at wakeup and call end if the lockscreen is detected

public class AutoDismiss extends MediatorService {
	public boolean persistent = false;
    //public boolean timeoutenabled = false;
    
    public int patternsetting = 0;
    //we'll see if the user has pattern enabled when we startup
    //so we can disable it and then restore when we finish
    //FIXME store this in the prefs file instead of local var, so boot handler can pick it up and restore when necessary
    
   
    //public boolean idle = false;
    //when the idle alarm intent comes in we set this true to properly start closing down
    
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

                
                //unregisterReceiver(idleExit);
            	unregisterReceiver(lockStopped);
                               
                
                editor.putBoolean("serviceactive", false);
                editor.commit();
                
                //ManageWakeLock.releasePartial();
                
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
            //timeoutenabled = settings.getBoolean("timeout", false);
                        
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
            
            //ManageWakeLock.acquirePartial(getApplicationContext());
            //if not always holding partial we would only acquire at Lock activity exit callback
            //we found we always need it to ensure key events will not occasionally drop on the floor from idle state wakeup
            
            /*
            IntentFilter idleFinish = new IntentFilter ("i4nc4mp.myLock.lifecycle.IDLE_TIMEOUT");
            registerReceiver(idleExit, idleFinish);
            
             * 
             */
            
            IntentFilter lockStop = new IntentFilter ("i4nc4mp.myLock.lifecycle.LOCKSCREEN_EXITED");
            registerReceiver(lockStopped, lockStop);
            
            editor.putBoolean("serviceactive", true);
            editor.commit();
    }
    
    /*
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
    }};*/
    
    BroadcastReceiver lockStopped = new BroadcastReceiver() {
        @Override
    public void onReceive(Context context, Intent intent) {
        if (!intent.getAction().equals("i4nc4mp.myLock.lifecycle.LOCKSCREEN_EXITED")) return;
        
        //couldn't get any other method to avoid the KG from shutting screen back off
        //when dismiss activity sent itself to back
        //it would ignore all user activity pokes and log "ignoring user activity while turning off screen"
        
        ManageWakeLock.releaseFull();
        return;
        }};
    
    @Override
    public void onScreenWakeup() {
    	ManageKeyguard.initialize(getApplicationContext());
        
        boolean KG = ManageKeyguard.inKeyguardRestrictedInputMode();    
    	if (KG) StartDismiss(getApplicationContext());                
        
    	return;
    }
    
    public void StartDismiss(Context context) {
            
    	//PowerManager myPM = (PowerManager) getApplicationContext().getSystemService(Context.POWER_SERVICE);
        //myPM.userActivity(SystemClock.uptimeMillis(), true);
    	ManageWakeLock.acquireFull(getApplicationContext());
    	
    Class w = AutoDismissActivity.class; 
                  
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
            
            Log.v("mediator - call start","call was placed or answered.");
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
            
            //right now we will always dismiss
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
            
            //shouldLock = true;
            if (KG) StartDismiss(mCon);
            
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
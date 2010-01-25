package i4nc4mp.myLock;

import i4nc4mp.myLock.ManageKeyguard.LaunchOnKeyguardExit;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.PowerManager;
import android.os.SystemClock;
import android.provider.Settings.SettingNotFoundException;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

//this is the no lockscreen mode.
//we just can't handle only keys we want. we either have to break out of lockscreen
//then workaround every key event
//or handle nothing except power which is the only key that the lockscreen allows through
//that's what we'll do is set key event to call finish. Anything we do get means we woke up and should exit
public class ShowWhenLockedActivity extends Activity {
        
        //private Handler serviceHandler;
        //private Task myTask = new Task();
        
        
        private int timeoutpref = 15;        
        
        
        //very very complicated business.
        @Override
    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        
        requestWindowFeature(android.view.Window.FEATURE_NO_TITLE);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);
        
        /*WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
         * this flag pushes the activity up and gets it ready at screen off but lockscreen rules stay in effect
         * that means this flag is good for showing a custom lockscreen to user (LIKE AN SMS POPUP)
         * it would have to be paired with the current full exit method after that (disable, pause, securely exit)
         * because after finish the lockscreen is still there
         * this is ideal for the custom lockscreen when user doesn't want a special unlock method
         * 
         * 
         * I can't even force key events by calling TakeKeyEvents with this mode... >_<
         */
        
        /*
         * WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
         * with dismiss keyguard flag, every key wakes phone after that (no lockscreen rules)
         * this example has vol and camera focus locked down
         * the power key will always instant unlock
         * and a camera full press will display the custom lockscreen!
         * back can be pressed while the screen is showing to unlock
         * 
         * how we accomplish this is by setting the window's brightness to 0.0
         * this is screen off, so it stays off even if the CPU is actually waking when vol is pressed
        */
        
        updateLayout();
        
        //setBright((float) 0.0);
        
        takeKeyEvents(true);
        //can we force ourselves to get key events even while created at screen off and showing when locked?
        getWindow().takeKeyEvents(true);//see if forcing the window also helps consistency
    
        }
        
        //TODO add a handler which waits 5 seconds then switches on a flag to tell the key event logic to treat as full locked
        //the flag would allow any key event to wake it back like the stock lockdown grace period
        
    protected View inflateView(LayoutInflater inflater) {
        return inflater.inflate(R.layout.lockactivity, null);
    }

    private void updateLayout() {
        LayoutInflater inflater = LayoutInflater.from(this);

        setContentView(inflateView(inflater));
    }
        
        
       
    
    /*
    BroadcastReceiver screenoff = new BroadcastReceiver() {
        //we have to use screen off to set bright back to 0.0
    	
    	//the OS is still going to call this as it is only our activity specifying the screen is off
    	//the OS still runs the flags that would make it be on for all other activities.
    	
        public static final String Screenoff = "android.intent.action.SCREEN_OFF";

        @Override
        public void onReceive(Context context, Intent intent) {
                if (!intent.getAction().equals(Screenoff)) return;
        //if a wakeup key had turned screen on let's tell the window to keep it off now
        if (screenwake) {
        	screenwake = false;
        	setBright((float) 0.0);
        }
        else if (cpuwake) {
        	//just turn the flag back off, for now
        	cpuwake = false;
        }
        return;//avoid unresponsive receiver error outcome
             
}};*/
    
    
    
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
    	super.onConfigurationChanged(newConfig);
     	if (newConfig.hardKeyboardHidden == Configuration.HARDKEYBOARDHIDDEN_NO) {
     		//this means that a config change happened and the keyboard is open
     		//wakeup();
     	
     		//PowerManager myPM = (PowerManager) getApplicationContext().getSystemService(Context.POWER_SERVICE);
      	  	//myPM.userActivity(SystemClock.uptimeMillis(), false);
        	finish();
      	  	//let's instant unlock when slide open
        	//TODO give user option to keep it locked on slide open for that paranoid slider-bumper
     	}
     	else {
     		//we will just do nothing if a config change comes and the hard keyboard is hidden
     		//we'll let sleep handle itself
     	}
    	/*A flag indicating whether the hard keyboard has been hidden.
    	This will be set on a device with a mechanism to hide the keyboard from the user, when that mechanism is closed.
    	One of: HARDKEYBOARDHIDDEN_NO, HARDKEYBOARDHIDDEN_YES.
    	*/

//we could do something in response to a rotation but we have declared portrait only in manifest
//we receive the orientation config change only to ensure we aren't destroyed and recreated at time of change
    	
    }
        
    @Override
    protected void onStop() {
        super.onStop();
        // Don't hang around.
        finish();
    }
    
    @Override
    public void onDestroy() {
        super.onDestroy();
        
		//then send a new userActivity call to the power manager
		PowerManager pm = (PowerManager) getSystemService (Context.POWER_SERVICE); 
    	pm.userActivity(SystemClock.uptimeMillis(), false);
        
    	/*
    	ManageKeyguard.exitKeyguardSecurely(new LaunchOnKeyguardExit() {
            public void LaunchOnKeyguardExitSuccess() {
               Log.v("start", "This is the exit callback");
                }});
                //not sure if it will work or if we do need an explicit disablekeyguard call
                 * 
                 */
    	
      
       
		//TODO need to create that subclass to replace UnLock service (NolockService)
    	//No call back to start is needed since we will never be in wakeup

        Log.v("destroyWelcome","Destroying");
    }
    

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {

       //if (event.getKeyCode() == KeyEvent.KEYCODE_POWER) {
    	//getWindow().clearFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);
    	//getWindow().addFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);
    	//doesn't work. call securely exit instead
    	ManageKeyguard.disableKeyguard(getApplicationContext());
    	ManageKeyguard.exitKeyguardSecurely(new LaunchOnKeyguardExit() {
            public void LaunchOnKeyguardExitSuccess() {
               Log.v("start", "This is the exit callback");
               //strange, I'm getting this and no error about verifyUnlock
               finish();
                }});
    	//then kill it
        finish();
        return true;

        }
        //else return false;
    	//we won't get anything the lockscreen doesn't let us get
    //the forced TAKE KEY EVENTS still doesn't put us in front of the lockscreen mediator
}
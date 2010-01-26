package i4nc4mp.myLock;

import i4nc4mp.myLock.ManageKeyguard.LaunchOnKeyguardExit;
import android.app.Activity;

import android.content.Context;
import android.content.Intent;

import android.content.res.Configuration;
import android.os.Bundle;

import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;

import android.view.WindowManager;

//this is the no lockscreen mode.
//we just can't handle only keys we want. we either have to break out of lockscreen
//then workaround every key event
//or handle nothing except power which is the only key that the lockscreen allows through
//that's what we'll do is set key event to call finish. Anything we do get means we woke up and should exit
public class ShowWhenLockedActivity extends Activity {
                
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
         * All I want is for it to do what it says it will do.. let my activity override any lockscreen.
         * I should be able to take key events, where returning false passes them on to lockscreen whereas true will ensure handling
         */
        
        /*
         * WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
         * with dismiss keyguard flag, every key wakes phone after that (no lockscreen rules)
         * I have to hack severely to do anything useful with that (prevent user perception of all-key wakeup)
        */
        
        updateLayout();
        
        
        takeKeyEvents(true);
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
        
           
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
    	super.onConfigurationChanged(newConfig);
     	if (newConfig.hardKeyboardHidden == Configuration.HARDKEYBOARDHIDDEN_NO) {
     		//this means that a config change happened and the keyboard is open
     		     		
     		StartDismiss(getApplicationContext());
      	  	//let's instant unlock when slide open
        	//TODO give user option to keep it locked on slide open for that paranoid slider-bumper
     	}
     	
     		//we will just do nothing if a config change comes and the hard keyboard is hidden
     		//we'll let sleep handle itself
     	
    	/*A flag indicating whether the hard keyboard has been hidden.
    	This will be set on a device with a mechanism to hide the keyboard from the user, when that mechanism is closed.
    	One of: HARDKEYBOARDHIDDEN_NO, HARDKEYBOARDHIDDEN_YES.
    	*/

//we could do something in response to a rotation but we have declared portrait only in manifest
//we receive the orientation config change only to ensure we aren't destroyed and recreated at time of change
    	
    }
    
    
    public void StartDismiss(Context context) {
    	
    	//ManageKeyguard.initialize(context);
    	ManageKeyguard.disableKeyguard(getApplicationContext());
    	//advantage here is we don't have to do a task delay
    	//because we're already showing on top of keyguard this gets the job done
    	
    	
    	ManageKeyguard.exitKeyguardSecurely(new LaunchOnKeyguardExit() {
            public void LaunchOnKeyguardExitSuccess() {
               Log.v("start", "This is the exit callback");
               finish();
                }});
    	
    	/*
    	Class w = DismissKeyguardActivity.class; 
	    	      
		Intent dismiss = new Intent(context, w);
		dismiss.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK//For some reason it requires this even though we're already an activity
				| Intent.FLAG_ACTIVITY_NO_USER_ACTION//Just helps avoid conflicting with other important notifications
		        | Intent.FLAG_ACTIVITY_NO_HISTORY//Ensures the activity WILL be finished after the one time use
		        | Intent.FLAG_ACTIVITY_NO_ANIMATION);
		        
		context.startActivity(dismiss);*/
    	
    }
    
    @Override
    public void onWindowFocusChanged (boolean hasFocus) {
    	if (hasFocus) {
    		//do nothing
    		Log.v("focus change","we have gained focus");
    	}
    	else {//loses focus
    		Log.v("focus change","we have lost focus");
    		finish();
    		//this implementation ensures the dismiss can start and we don't finish till it is on-screen
    		//when NoLock calls it it just ensures it finishes after the keyguard exit
    	}
    	
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {

       //if (event.getKeyCode() == KeyEvent.KEYCODE_POWER) {
    	
    	StartDismiss(getApplicationContext());
        return true;

        }
        //else return false;
    	//we won't get anything the lockscreen doesn't let us get
    //the forced TAKE KEY EVENTS still doesn't put us in front of the lockscreen mediator
}
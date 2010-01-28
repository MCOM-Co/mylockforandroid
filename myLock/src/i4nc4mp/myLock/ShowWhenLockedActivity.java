package i4nc4mp.myLock;

import i4nc4mp.myLock.ManageKeyguard.LaunchOnKeyguardExit;
import android.app.Activity;

import android.content.Context;
import android.content.Intent;

import android.content.res.Configuration;
import android.os.Bundle;
import android.os.PowerManager;
import android.os.SystemClock;

import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;

import android.view.WindowManager;


//we just can't handle only keys we want
//we either have to break out of lockscreen, then workaround every key event
//or be completely dead to key events unless the screen is fully awake

//the only way this would be good is if the screen on reaction did the dismiss & finish
//we could make the slider wake suppressed via set bright 0.0 and set a flag that will tell screen on to abort dismiss & finish
//another possibly useful thing would be engage stay awake on slider open
public class ShowWhenLockedActivity extends Activity {
                
        //very very complicated business.
        @Override
    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        //Log.v("create nolock","about to request window params");
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
        
        //setVisible(false);
        //Tells the OS that we don't want to display any UI
        //we never have a UI for interaction
      //TODO might mess up the functionality, might enhance it, we shall see
        //Log.v("create nolock","about to update layout");
        updateLayout();
        
        //Log.v("create nolock","about to request key events");
        takeKeyEvents(true);
        getWindow().takeKeyEvents(true);//see if forcing the window also helps consistency
    
        }
        
        /*public void onStart() {
        	super.onStart();
        	Log.v("start nolock","onStart");
        	//ManageWakeLock.releasePartial();//drop the lock we had to get to ensure we could get this far
        	
        }*/
        
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
     		//with this event, a screen wakeup is happening at the OS level, we can't suppress
     		//except via the Lockscreen activity method of forcing window brightness to 0.0 (silent wake)
     		     
     		PowerManager pm = (PowerManager) getSystemService (Context.POWER_SERVICE); 
        	pm.userActivity(SystemClock.uptimeMillis(), false);
     		  	
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
    
    //TODO because we cannot really handle the key event here or prevent wakeup from slider without doing the screen bright hack
    //we could convert this method to a screen on broadcast reaction
    //i think the customization of the slider suppression is best suited only the the already customizable customlock setup
    //Do we even need this activity when we want to behave with slider, power, and any keyboard button as unlock?
    //I don't think so. Just have the mediator fire DoExit or StartDismiss at screen on
    //it's only cosmetic but I still think it actually creates more lag
    //let users just deal with the lag we occasionally get when breaking out of lockscreen at screen on.
    
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
               finish();
                }});    	
    }
    
    @Override
    public void onWindowFocusChanged (boolean hasFocus) {
    	if (hasFocus) {
    		//do nothing
    		Log.v("focus gain","startup is done");
    	}
    	else {//loses focus
    		Log.v("focus lost","finishing...");
    		StartMediator();
    		finish();
    		//moveTaskToBack(true);
    		
    		//reset variables also
    		
    		//this implementation ensures the dismiss can start and we don't finish till it is on-screen
    		//when NoLock calls it it just ensures it finishes after the keyguard exit
    	}
    	
    }
    
    /*
    @Override
    public void onPause() {
    	super.onPause();
    	takeKeyEvents(true);
    }
    
    @Override
    public void onResume() {
    	super.onResume();
    	takeKeyEvents(true);
    }
    
    @Override
    public void onDestroy() {
        super.onDestroy();
        ManageWakeLock.releaseFull();
    }*/
    
    public void StartDismiss(Context context) {
    	    	
    	Class w = DismissKeyguardActivity.class; 
	    	      
		Intent dismiss = new Intent(context, w);
		dismiss.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK//For some reason it requires this even though we're already an activity
				| Intent.FLAG_ACTIVITY_NO_USER_ACTION//Just helps avoid conflicting with other important notifications
		        | Intent.FLAG_ACTIVITY_NO_HISTORY//Ensures the activity WILL be finished after the one time use
		        | Intent.FLAG_ACTIVITY_NO_ANIMATION);
		        
		context.startActivity(dismiss);
    	
    }
    
    public void StartMediator() {
    Intent i = new Intent();
	i.setClassName("i4nc4mp.myLock", "i4nc4mp.myLock.NoLockService");
	startService(i);
    }//TODO try changing this back to the literal direct start dismiss as a child activity. then we could call finish on it

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
    	boolean up = event.getAction() == KeyEvent.ACTION_UP;
       
    	//if (up) StartMediator();
    	if (up) StartDismiss(getApplicationContext());
    	//seems we don't get the down event... if I force !up it won't be handled if happening simultaneously with wakeup
    	//so conclusion is show when locked can't get ANYTHING while asleep
    	//whereas dismiss keyguard activity can get that first down that wakes
    	
    	
        return true;
    	//always claim we handled the key
        //but we only launch dismiss on up
    }
    
    //we won't get anything the lockscreen doesn't let us get
    //the forced TAKE KEY EVENTS still doesn't put us in front of the lockscreen mediator
}
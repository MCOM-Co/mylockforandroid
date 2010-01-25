package i4nc4mp.myLock;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Handler;
import android.os.PowerManager;
import android.os.SystemClock;
import android.provider.Settings.SettingNotFoundException;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

//starting point is the alarmalert prompt.
//this window itself dismisses keyguard.

//we are going to call it up when the screen goes OFF
//then attempt to mediate which keys will cause a result at that point

//android color = #ffa4c639
//just thought you should know that


//lockdown and regular lockscreen are closely related
//during lockdown, we want to ensure to bring ourselves back if activity goes to background
//otherwise, we want to FINISH when we go to background in any fashion
//need to implement droid-fu to accomplish this
public class Lockscreen extends Activity {
        
        private Handler serviceHandler;
        private Task myTask = new Task();
        
        //private ShakeListener mShaker;
		//for now shake is too inconsistent to really use. 
        
        private int timeoutpref = 15;        
        
        //private int bright = 10; deprecated at the moment
        private int timeleft = 0;
        
        private boolean shouldFinish = false;
        //flag it to true if user hits power to wake up but quiet wake was already active
        //this lets our task wait a half second, then actually wake up and finish
        
        private boolean screenwake = false;//set true when a wakeup key turns screen on
        private boolean cpuwake = false;//set true when a locked key wakes CPU but not screen
        
        //very very complicated business.
        @Override
    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        
        requestWindowFeature(android.view.Window.FEATURE_NO_TITLE);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);
        
        /*    | WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);
         * this flag pushes the activity up and gets it ready at screen off but lockscreen rules stay in effect
         * that means this flag is good for showing a custom lockscreen to user (LIKE AN SMS POPUP)
         * it would have to be paired with the current full exit method after that (disable, pause, securely exit)
         * because after finish the lockscreen is still there
         * we also can't mediate key events with this one because lockscreen rules are still in control
         */
        
        /*
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
        
        setBright((float) 0.0);
        //ensures that the window will keep screen off
        //we will deliberately turn it on when a wakeup key occurs
        //it's a bit buggy at the moment as sleep timeouts seem to cause us to lose focus
        //causing first button press to not be handle but regain focus for next attempt
        //droid-fu can help avoid this?
        
        takeKeyEvents(true);
        //FORCE OUR STUPID KEY EVENTS TO GET HANDLED EVEN IF NO FOCUS!!
        //ANDRO-A.D.D.
        getWindow().takeKeyEvents(true);//see if forcing the window also helps consistency
        
        IntentFilter offfilter = new IntentFilter (Intent.ACTION_SCREEN_OFF);
		registerReceiver(screenoff, offfilter);
        
        //serviceHandler = new Handler();
        
      //retrieve the user's normal timeout setting - SCREEN_OFF_TIMEOUT
    	try {
            timeoutpref = android.provider.Settings.System.getInt(getContentResolver(), android.provider.Settings.System.SCREEN_OFF_TIMEOUT);
    } catch (SettingNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
    }//this setting will be restored at finish
    
    //Next, change the setting to 0 seconds
    android.provider.Settings.System.putInt(getContentResolver(), 
            android.provider.Settings.System.SCREEN_OFF_TIMEOUT, 0);
    //the device behavior ends up as just over 5 seconds when we do this.
    //when we set 1 here, it comes out 6.5 to 7 seconds between timeouts.
    
    /*registering the shake listener
    mShaker = new ShakeListener(this);
    mShaker.setOnShakeListener(new ShakeListener.OnShakeListener () {
      public void onShake()
      {
        wakeup();//try waking up in response to the shake
      }
    });*/
    //try acquiring the minimum partial wakelock to see if it allows us to catch shakes
    //ManageWakeLock.acquirePartial(getApplicationContext());
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
    public void onBackPressed() {
        	if (screenwake) finish();
        	//2.1 has a PM function is screen on
        	
        	//if screen is on we allow back to call finish. otherwise it does nothing
        	//a user can press back after cpu wake from a locked key, but nothing happens
        	
        	
        	//the only other inconsistency is after a locked key cpu wake, if press power
        	//it is actually sleeping the cpu again so unlock doesn't happen
        	//we've started setting flag CPU wake when a locked down key causes the forced-screenoff-wake
        	       	
        	//we could solve this by the following power key event reaction
        	//set a handler for a half sec to wait for the "sleep" we can't prevent
        	//then call a new useractivity and still do the unlock. ingenious.
        
        	//TODO need to do tests to see if our TAKEKEYEVENTS true actually allows custom sleep
        	//through calling of the set bright 0.0. we'd also have to bring ourself to front... hmm
        return;
    }
    
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
        	//Real sleep is happening after a quiet wake
        	//either by 5 sec timeout or user is pressing power expecting wake
        	//if (timeleft!=0)
        	shouldFinish=true;
        	//if the countdown is in progress that means user pressed power
        	//therefore the device is going to sleep
        	//but we actually need to wait a half second then call wakeup and finish
        	}
        
        
        return;//avoid unresponsive receiver error outcome
             
}};
    
    public void setBright(float value) {
    	Window mywindow = getWindow();
    	
    	WindowManager.LayoutParams lp = mywindow.getAttributes();

		lp.screenBrightness = value;

		mywindow.setAttributes(lp);
    }
    
    //call this task to turn off the screen in a fadeout.
    //i don't use it now, only used to test this method before coding the rest.
    //currently i just set our bright to 0 at oncreate instead.

    /*
    class Task implements Runnable {
    	public void run() {                
    		if (bright != 0) {
    			setBright(bright/100); //start at 10% bright and go to 0 (screen off)

    			bright--;
    			serviceHandler.postDelayed(myTask, 100L);
                }
    		else {
    			setBright((float) 0.0); 
    			
    			bright = 10;//put bright back
    		}
    	}
    }
    */
    
    class Task implements Runnable {
    	public void run() {                
    		//the task will turn off the quiet wake 5 seconds after the button press happened
    		//essentially if a 2nd press happens before the 5 seconds is up we need to restart
    		//since that's what the real timeout does
    		
    		//best way is to actually use an int that does decrement every half sec
    		//timeleft is equal to 10 half-second ticks. when it gets to 0 then the flag is cleared
    		//when repeat calls happen we just put the int back at 5 sec worth (10 ticks)
    		if (shouldFinish) {
    			finish();
    		}
    		else if (timeleft!=0) {
    			timeleft--;
    			serviceHandler.postDelayed(myTask,500L);//just decrement every half second
    		}
    		else {
    			cpuwake = false;
    		}
    	}
    }
    
    public void wakeup() {
    	screenwake = true;
    	timeleft = 0;//this way the task doesn't keep going
    	    	
    	//poke user activity just to be safe that it won't flicker back off
    	PowerManager myPM = (PowerManager) getApplicationContext().getSystemService(Context.POWER_SERVICE);
  	  	myPM.userActivity(SystemClock.uptimeMillis(), false);
  	  	
  	  	setBright((float) 0.1);//tell screen to go on with 10% brightness
    }
    
    
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
        
      //restore the users preference for timeout so that the screen will sleep as they expect
		android.provider.Settings.System.putInt(getContentResolver(),
				android.provider.Settings.System.SCREEN_OFF_TIMEOUT, timeoutpref);
		//then send a new userActivity call to the power manager
		PowerManager pm = (PowerManager) getSystemService (Context.POWER_SERVICE); 
    	pm.userActivity(SystemClock.uptimeMillis(), false);
        
       //serviceHandler.removeCallbacks(myTask);
       //serviceHandler = null;
       
       unregisterReceiver(screenoff);
      
       Intent i = new Intent();
		i.setClassName("i4nc4mp.myLock", "i4nc4mp.myLock.CustomLockService");
		startService(i);//tells the mediator that user has unlocked
       
		//release the CPU hold we obtained to let us listen for shakes
		//ManageWakeLock.releasePartial();
    	
        Log.v("destroyWelcome","Destroying");
    }
    //public void onWindowFocusChanged (boolean hasFocus)
    //find out if we lost focus somehow
    //might be helpful for the lockdown mode
    //hasWindowFocus ()
    
    
    //public void takeKeyEvents (boolean get)
    //Request that key events come to this activity.
    //Use this if your activity has no views with focus
    //but still want a chance to process key events.
    
    @Override
    public void onWindowFocusChanged (boolean hasFocus) {
    	if (hasFocus) {
    		//do nothing
    		Log.v("focus change","we have gained focus");
    	}
    	else if (screenwake) {
    		finish();//we aren't visible... need to unlock
    		Log.v("focus change","need to finish because user pressed home");
    		//FIXME this flags the mediator to do a new lockscreen when it shouldn't
    		//to accomplish, let mediator see if any call is in progress before responding to restart
    		//otherwise seems to work
    	}
    	
    	//for now it assumes that user did home if this happened and screen was awake
    }
    
    
    //here's where most of the magic happens
    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        // Do this on key down
        boolean up = event.getAction() == KeyEvent.ACTION_UP;
        //flags to true if the event we are getting is the up (release)
        //most cases let down get CPU wake always and let up get reaction wake or unlock
        switch (event.getKeyCode()) {
            //case KeyEvent.KEYCODE_VOLUME_UP:
            //case KeyEvent.KEYCODE_VOLUME_DOWN:
            case KeyEvent.KEYCODE_FOCUS:
                if (up) {
                    break;
                	//break without return means pass on to other processes
                    //doesn't consume the press

                    //only set timer at down we don't need to do anything at up

                    //break is actually allowing the return super to happen (see end of block)
                    //returning false passes it on. we could allow vol changes while locked
                    //for example
                }
                   
                
                Log.v("key event","locked key");
                //presses need to flag the quiet wake
                //the screen off needs to know if the re-off is happening faster than the timeout
                //because that means user forced sleep via power key
                if (screenwake) break;//don't set off the timer if we're already in a wakeup
                if (!cpuwake) {//set up the 5 second cpu wake flag
                	cpuwake = true;
                	timeleft=10;//10 half sec ticks for the task to count off
                	serviceHandler.postDelayed(myTask,500L);
                }
                else {
                	//log the fact that it is a repeat key
                	timeleft=10;//reset count
                	//task will continue running on its own if already started & timeleft is not 0
                	//so don't call it to start
                }
                return true;
                //returning true means we handled the event so don't pass it to other processes
            case KeyEvent.KEYCODE_VOLUME_UP:
            case KeyEvent.KEYCODE_VOLUME_DOWN:
            case KeyEvent.KEYCODE_CAMERA:
            	if (up) {
                   
               	Log.v("key event","wake key");
            	wakeup();
            	}
                return true;
            	
            	
               	
                //allow any keys we haven't locked down or defined as wake to instant unlock
                
                //if we catch the up from power it could mean user pressed power to sleep
                
                //back key is automatically handled, we process it in the onBackPressed
               
               
                
            default:
            	if (up) {
                             
            	Log.v("key event","unlock key");
            	//do a user activity poke to be safe that it won't go back to sleep again
            	//this doesn't seem to help when a cpuwake is active from a locked key
            	//it still goes to sleep despite our user activity
            	//seems the power key root power handling overrides user activities always.
            	//PowerManager myPM = (PowerManager) getApplicationContext().getSystemService(Context.POWER_SERVICE);
          	  	//myPM.userActivity(SystemClock.uptimeMillis(), false);
            	finish();
            	//the only case we don't get this event is during a silent wake
            	//timeleft & the task handle this case
            	}
            	return true;
        }
        return super.dispatchKeyEvent(event);
    }
}
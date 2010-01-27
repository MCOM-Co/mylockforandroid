package i4nc4mp.myLock;

import android.app.Activity;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.SystemClock;
import android.provider.Settings.SettingNotFoundException;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

//Alpha 3 release, no customization, just the custom lockscreen you never manually wake
//TODO need to create the mediator subclass for it
//TODO need to bring in updates from the other lock activities 
//This version might not be needed if we can resolve the inconsistency in the other 2 versions
public class UnLockScreen extends Activity {
        
        //private ShakeListener mShaker;
		//for now shake is too inconsistent to really use. 
        
		Handler serviceHandler;
		Task myTask = new Task();
	
        private int timeoutpref = 15;        
        
        public int timeleft = 0;
        
        public boolean shouldFinish = false;
        //flag it to true if user hits power to wake up but quiet wake was already active
        //this lets our task wait a half second, then actually wake up and finish
        
        public boolean screenwake = false;//set true when a wakeup key turns screen on
        public boolean cpuwake = false;//set true when a locked key wakes CPU but not screen
        
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
        
        takeKeyEvents(true);
        //FORCE OUR STUPID KEY EVENTS TO GET HANDLED EVEN IF NO FOCUS!!
        //ANDRO-A.D.D.
        getWindow().takeKeyEvents(true);//see if forcing the window also helps consistency
        //this still has inconsistency. when locked on top of certain apps with input windows, we sometimes fail to react to first key event
        
        IntentFilter offfilter = new IntentFilter (Intent.ACTION_SCREEN_OFF);
		registerReceiver(screenoff, offfilter);
        
        serviceHandler = new Handler();
        
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
        	//by the time 5 seconds since last quiet wake pass we flag this false again in the task

        	shouldFinish=true;
        	
        	//but we actually need to wait a half second then call wakeup and finish.
        	//that's done by the task when this should flag is true;
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
        			/*call the dismiss activity up
        			Class w = DismissKeyguardActivity.class; 
  	    	      
        			Intent dismiss = new Intent(getApplicationContext(), w);
        			dismiss.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK//For some reason it requires this even though we're already an activity
        					| Intent.FLAG_ACTIVITY_NO_USER_ACTION//Just helps avoid conflicting with other important notifications
        			        | Intent.FLAG_ACTIVITY_NO_HISTORY//Ensures the activity WILL be finished after the one time use
        			        | Intent.FLAG_ACTIVITY_NO_ANIMATION);
        			        
        			getApplicationContext().startActivity(dismiss);*/
        			//starting this steals focus from the lockscreen, causing it to finish itself
        			//and the dismiss also finishes self automatically after a half sec
        			wakeup();
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
        	//this is working for the focus key but never works for the vol key. no idea why
        	//this workaround is only relevant to power key which we can't prevent from causing the go to sleep if any wake exists
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
        
       serviceHandler.removeCallbacks(myTask);
       serviceHandler = null;
       
       unregisterReceiver(screenoff);
      
       Intent i = new Intent();
		i.setClassName("i4nc4mp.myLock", "i4nc4mp.myLock.CustomLockService");
		startService(i);//tells the mediator that user has unlocked
       
		//release the CPU hold we obtained to let us listen for shakes
		//ManageWakeLock.releasePartial();
    	
        Log.v("destroyWelcome","Destroying");
    }
        
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
            case KeyEvent.KEYCODE_VOLUME_UP:
            case KeyEvent.KEYCODE_VOLUME_DOWN:
            case KeyEvent.KEYCODE_FOCUS:
            case KeyEvent.KEYCODE_CAMERA:
                if (up) {
                    break;
                	//break without return means pass on to other processes
                    //doesn't consume the press

                    //break is actually allowing the return super to happen (see end of block)
                    //returning false passes it on.
                    //for example we could allow vol changes while locked
                }
                   
                
                Log.v("key event","locked key");

                if (screenwake) break;//we can ignore the key if already in a wakeup (user viewing lockscreen)
                timeleft=10;//10 half sec ticks for the task to count off
                if (!cpuwake) {
                	cpuwake = true;
                	serviceHandler.postDelayed(myTask, 500L);
                }
                else {
                	//log the fact that lock key was repeated while already quiet waking
                	//task will continue running on its own if already started & timeleft is not 0
                	//so we don't need to call a start
                }
                return true;
                //returning true means we handled the event so don't pass it to other processes
                
                //With the focus press the timer works and wakes and unlocks after the forced sleep
                //Volume key would never work despite the fact that the logic is being processed exactly the same for both events
                
            //TODO give an option to use a long press to wake to the blank lockscreen replacement for purpose notification panel use
            //This implementation is aimed at the alpha 3 release without customization
            //because can't get the show when locked activity to be consistent
                
            /*case KeyEvent.KEYCODE_CAMERA:
            	if (up) {
                   
               	Log.v("key event","wake key");
            	wakeup();
            	}
                return true;*/
            	
            	
               	
                //allow any keys we haven't locked down or defined as wake to instant unlock
                
                //if we catch the up from power it could mean user pressed power to sleep
                
                //back key is automatically handled, we process it in the onBackPressed
               
               
                
            default:
            	if (up) {
                             
            	Log.v("key event","unlock key");
            	
            	finish();
            	//the only case we don't get this event is during a silent wake
            	//when user presses power
            	//timeleft & the task handle this case
            	}
            	return true;
        }
        return super.dispatchKeyEvent(event);
    }
}
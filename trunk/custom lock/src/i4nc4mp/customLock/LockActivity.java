package i4nc4mp.customLock;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
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
public class LockActivity extends Activity {
        
        private Handler serviceHandler;
        private Task myTask = new Task();
        
        private int timeoutpref = 15;        
        
        private int bright = 10;
        
        private boolean awake = false;
        
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
        }
        
        //has something to do with whether we see what was behind or see a fullscreen with wallpaper BG
        protected View inflateView(LayoutInflater inflater) {
        return inflater.inflate(R.layout.lockactivity, null);
    }

    private void updateLayout() {
        LayoutInflater inflater = LayoutInflater.from(this);

        setContentView(inflateView(inflater));
    }
        
        
        @Override
    public void onBackPressed() {
        	if (awake) finish();
        	//2.1 has a PM function is screen on
        	
        	//if screen is on we allow back to call finish. otherwise it does nothing
        	//a user can press back after cpu wake from a locked key, but nothing happens
        	
        	
        	//the only other inconsistency is after a locked key cpu wake, if press power
        	//it is actually sleeping the cpu again so unlock doesn't happen
        	//we can probably pass a userActivity for that case by making a lock key set a flag
        	//that power key will check for
        return;
    }
    
    protected void onPostCreate(Bundle savedInstanceState) {
                super.onPostCreate(savedInstanceState);
        }
    
    BroadcastReceiver screenoff = new BroadcastReceiver() {
        //we have to use screen off to set bright back to 0.0
    	
    	//the OS is still going to call this as it is only our activity specifying the screen is off
    	//the OS still runs the flags that would make it be on for all other activities.
    	
        public static final String Screenoff = "android.intent.action.SCREEN_OFF";

        @Override
        public void onReceive(Context context, Intent intent) {
                if (!intent.getAction().equals(Screenoff)) return;
                //if a wakeup key had turned screen on let's tell the window to keep it off
        if (awake) {
        	awake = false;
        	setBright((float) 0.0);
        }
             
}};
    
    public void setBright(float value) {
    	Window mywindow = getWindow();
    	
    	WindowManager.LayoutParams lp = mywindow.getAttributes();

		lp.screenBrightness = value;

		mywindow.setAttributes(lp);
    }
    
    //call this task to turn off the screen in a fadeout.
    //i don't use it now, only used to test this method before coding the rest.
    
    //it will actually stay off till we finish the activity
    //wakeup keys will also tell it brighten, screen off event will then set it back to 0
    //non wakeup keys do wake up the OS but it keeps screen off to mitigate battery impact
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
    
  
    @Override
    protected void onPause() {
        super.onPause();
        // Don't hang around.
        //finish();
    }
    //not reliably happening when activity goes to background
    //seems also to be causing it to finish no matter what
        
    @Override
    protected void onStop() {
        super.onStop();
        // Don't hang around.
        finish();
    }
    
    @Override
    public void onDestroy() {
        super.onDestroy();
        
       serviceHandler.removeCallbacks(myTask);
       serviceHandler = null;
       
       unregisterReceiver(screenoff);
      
       Intent i = new Intent();
		i.setClassName("i4nc4mp.customLock", "i4nc4mp.customLock.CustomLockMediator");
		startService(i);//tells the mediator that user has unlocked
       
       //restore the users preference for timeout so that the screen will sleep as they expect
		android.provider.Settings.System.putInt(getContentResolver(),
				android.provider.Settings.System.SCREEN_OFF_TIMEOUT, timeoutpref);
		//then send a new userActivity call to the power manager
		PowerManager pm = (PowerManager) getSystemService (Context.POWER_SERVICE); 
    	pm.userActivity(SystemClock.uptimeMillis(), false);
    	
        Log.v("destroyWelcome","Destroying");
    }
    
    //here's where most of the magic happens
    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        // Do this on key down
        boolean up = event.getAction() == KeyEvent.ACTION_UP;
        //flags to true if the event we are getting is the up (release)
        switch (event.getKeyCode()) {
            case KeyEvent.KEYCODE_VOLUME_UP:
            case KeyEvent.KEYCODE_VOLUME_DOWN:
            case KeyEvent.KEYCODE_FOCUS:
                if (up) {
                    break;//break without return means pass on to other processes
                    //doesn't consume the press
                    }
                
                Log.v("key event","locked key");
       
                return true;
                //returning true means we handled the event so don't pass it to other processes
                            
            case KeyEvent.KEYCODE_CAMERA:
            	Log.v("key event","wake key");
            	awake = true;
            	setBright((float) 0.1);//tell screen to go on with 10% brightness
                return true;
            	
            case KeyEvent.KEYCODE_POWER:
            	Log.v("key event","unlock key");
            	finish();
            	return true;
                
            default:
                	                	
                break;
        }
        return super.dispatchKeyEvent(event);
    }
}
package i4nc4mp.myLockcomplete;

import java.util.GregorianCalendar;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Handler;
import android.os.PowerManager;
import android.os.SystemClock;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;

//starting point is the alarmalert prompt.
//this window itself dismisses keyguard.

//we are going to call it up when the screen goes OFF
//then attempt to mediate which keys will cause a result at that point

//android color = #ffa4c639
//just thought you should know that


//lockdown and regular lockscreen are closely related
//during lockdown, we want to ensure to bring ourselves back if activity goes to background
//otherwise, we want to FINISH when we go to background in any fashion

//the focus change method handles this fine. droid-fu lets you distinguish if it's you or other apps taking focus away



//our own lifecycle duties are to detect focus changes and user key events
//also to react to phone state broadcasts that are sent from the mediator service
//this is the most complex part of smooth operation

public class Lockscreen extends Activity {
        
		Handler serviceHandler;
		Task myTask = new Task();     
        
        public int timeleft = 0;
        
        
/* Lifecycle flags */
        public boolean starting = true;//flag off after we successfully gain focus. flag on when we send task to back
        public boolean waking = false;//any time quiet or active wake are up
        public boolean finishing = false;//flag on when an event causes unlock, back off when onStart comes in again (relocked)
        
        public boolean paused = false;
        
        public boolean shouldFinish = false;
        //flag it to true if user hits power to wake up but quiet wake was already active
        //this lets our task wait a half second, then actually wake up and finish
        
        public boolean screenwake = false;//set true when a wakeup key or external event turns screen on
        
        public boolean idle = false;
        
        
        private Button mrewindIcon;
        private Button mplayIcon;
        private Button mpauseIcon;
        private Button mforwardIcon;
        
        public TextView curhour;
        public TextView curmin;
        
        public TextView batt;

        //very very complicated business.
        @Override
    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        
        requestWindowFeature(android.view.Window.FEATURE_NO_TITLE);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);
        		//| WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
		
        /*    | WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);
         * this flag pushes the activity up and gets it ready at screen off but lockscreen rules stay in effect
         * that means this flag is good for showing a custom lockscreen to user (LIKE AN SMS POPUP)
         * it would have to be paired with the current full exit method after that (disable, pause, securely exit)
         * because after finish the lockscreen is still there
         * we also can't mediate key events with this one because lockscreen rules are still in control
         */
        
        /*
         * with dismiss keyguard flag, every key wakes phone after that (no lockscreen rules)
         * 
         * we can't get key down, but we usually get the up events
         * the down is what wakes the device. this activity keeps the screen off
         *
         * 
         * how we accomplish this is by setting the window's brightness to 0.0
         * this is screen off, so it stays off even if the CPU is actually waking when vol is pressed
        */
               
        
        updateLayout();
        
        setBright((float) 0.0);
        
        
        curhour = (TextView) findViewById(R.id.hourText);
        
        curmin = (TextView) findViewById(R.id.minText);
        
        batt = (TextView) findViewById(R.id.batt);
        
       updateClock();
        
        mrewindIcon = (Button) findViewById(R.id.PrevButton); 
        
        mrewindIcon.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
             Intent intent;
             intent = new Intent("com.android.music.musicservicecommand.previous");
             getApplicationContext().sendBroadcast(intent);
             }
          });
 
        mplayIcon = (Button) findViewById(R.id.PlayToggle); 
 
        mplayIcon.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
             Intent intent;
             intent = new Intent("com.android.music.musicservicecommand.togglepause");
             getApplicationContext().sendBroadcast(intent);
             /*if (!am.isMusicActive()) {
                 mpauseIcon.setVisibility(View.VISIBLE);
                 mplayIcon.setVisibility(View.GONE);
                 }*/
             }
          });
 
        /*mpauseIcon = (ImageButton) findViewById(R.id.pauseIcon); 
 
        mpauseIcon.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
             Intent intent;
             intent = new Intent("com.android.music.musicservicecommand.togglepause");
             getBaseContext().sendBroadcast(intent);
             if (am.isMusicActive()) {
                 mplayIcon.setVisibility(View.VISIBLE);
                 mpauseIcon.setVisibility(View.GONE);
                 }
             }
          });*/
 
        mforwardIcon = (Button) findViewById(R.id.NextButton); 
 
        mforwardIcon.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
             Intent intent;
             intent = new Intent("com.android.music.musicservicecommand.next");
             getApplicationContext().sendBroadcast(intent);
             }
          });
        
        IntentFilter offfilter = new IntentFilter (Intent.ACTION_SCREEN_OFF);
		registerReceiver(screenoff, offfilter);
		
		IntentFilter callbegin = new IntentFilter ("i4nc4mp.myLockcomplete.lifecycle.CALL_START");
        registerReceiver(callStarted, callbegin);  
        
        IntentFilter callabort = new IntentFilter ("i4nc4mp.myLockcomplete.lifecycle.CALL_ABORT");
        registerReceiver(callAborted, callabort);
        
		IntentFilter idleFinish = new IntentFilter ("i4nc4mp.myLockcomplete.lifecycle.IDLE_TIMEOUT");
		registerReceiver(idleExit, idleFinish);
		
        serviceHandler = new Handler();
    
        }
        
        public void updateClock() {
        	GregorianCalendar Calendar = new GregorianCalendar();         
            
        	int mHour = Calendar.get(GregorianCalendar.HOUR_OF_DAY);
        	int mMin = Calendar.get(GregorianCalendar.MINUTE);
        	
        	String hour = new String("");
        	String min = new String("");
        	
            if (mHour <10) hour = hour + "0";
            hour = hour + mHour;
            
            if (mMin <10) min = min + "0";
            min = min + mMin;
            
            curhour.setText(hour);
            curmin.setText(min);
            
            
            //update battery as it is also a form of time passing
            
            SharedPreferences settings = getSharedPreferences("myLock", 0);
            int battlevel = settings.getInt("BattLevel", 0);
            
            batt.setText(battlevel + "%");
            
            
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
        	if (screenwake) {
        		finishing = true;
        		//setBright((float) 0.1);
        		moveTaskToBack(true);//finish();
        	}
        	//2.1 has a PM function is screen on
        	
        	//if screen is on we allow back to call finish. otherwise it does nothing
        	//a user can press back after cpu wake from a locked key, but nothing happens
        return;
    }
        
    
    BroadcastReceiver screenoff = new BroadcastReceiver() {
        //we have to use screen off to set bright back to 0.0 so that true screen turn on is avoided for locked input
    	//the OS is still going to call this as it is only our activity specifying the screen is off
    	//the OS still runs the flags that would make it be on for all other activities.
    	//if the cpu is awake for too long it seems to be overriding and causing the screen to wake
    	
        public static final String Screenoff = "android.intent.action.SCREEN_OFF";

        @Override
        public void onReceive(Context context, Intent intent) {
                if (!intent.getAction().equals(Screenoff)) return;
                
                
        //if anything had turned screen on, undo
        if (screenwake && hasWindowFocus()) {
        	//check for focus because another activity may still be waiting to return focus to us
        	//like a handcent popup ---- this works awesome to allow the popup to work
        	screenwake = false;
        	setBright((float) 0.0);
        	}
        //discovered a bug with this - we don't have focus if user exited, so I now have onStop reset the screenwake flag also.
        else if (waking) {
        	//no screen wake exists but waking was set by the silent wake handling (or unhandled wakeup where we still had focus)
        	//this case should only happen if user is pressing power to unlock but they have bumped a locked key on the way

        	shouldFinish=true;
        	
			
        	//but we actually need to wait a half second then call wakeup and finish.
        	//that's done by the task when this should flag is true;
        	}
        //takeKeyEvents(true);
        //getWindow().takeKeyEvents(true);
        waking = false; //reset lifecycle
        
        return;//avoid unresponsive receiver error outcome
             
}};

BroadcastReceiver callStarted = new BroadcastReceiver() {
	@Override
    public void onReceive(Context context, Intent intent) {
	if (!intent.getAction().equals("i4nc4mp.myLockcomplete.lifecycle.CALL_START")) return;
	
	//we are going to be dormant (sitting in background) while this happens, so we need to force finish
	finishing = true;
	finish();
	return;
	
	}};
   	
BroadcastReceiver callAborted = new BroadcastReceiver() {
   	@Override
       public void onReceive(Context context, Intent intent) {
  	if (!intent.getAction().equals("i4nc4mp.myLockcomplete.lifecycle.CALL_ABORT")) return;
   		//do anything special we need to do right after the call abort
   		//we might not even need this broadcast in this mode
  	return;
  	}};

BroadcastReceiver idleExit = new BroadcastReceiver() {
	@Override
    public void onReceive(Context context, Intent intent) {
	if (!intent.getAction().equals("i4nc4mp.myLockcomplete.lifecycle.IDLE_TIMEOUT")) return;
	
	finishing = true;
	idle = true;
	
	Log.v("exit intent received","calling finish");
	 //PowerManager myPM = (PowerManager) getApplicationContext().getSystemService(Context.POWER_SERVICE);
	  	//myPM.userActivity(SystemClock.uptimeMillis(), true);
	  	//cause a quiet wake in the short timeout, so that when it ends the system will restore keyguard
	
	finish();
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
        		
        		
        		
        		/*if (starting) {
                	//I am calling this with a 5 sec delay when we stop
        			//this just seems like a good practice to destroy the activity
        			//this makes so user can not land back in it with back key presses
                	finish();
        		}
        		else*/ 
        			if (shouldFinish) {
        			finishing=true;
        			
        			PowerManager myPM = (PowerManager) getApplicationContext().getSystemService(Context.POWER_SERVICE);
         	  	  	myPM.userActivity(SystemClock.uptimeMillis(), false);
         	  	  	Log.v("silent wake task","power sleep occurred, forcing wake and exit");
        			setBright((float) 0.1);
        			moveTaskToBack(true);
        		}
        		else if (timeleft!=0) {
        			timeleft--;
        			serviceHandler.postDelayed(myTask,500L);//just decrement every half second
        		}
        		else if (!screenwake) {
        			waking = false;//no more wake flags unless the screen wake has cancelled the silent wake
        		}
        	}
        	//this workaround is only relevant to power key which we can't prevent from causing the go to sleep if any wake exists
        	//this is the case during the 5 seconds after a locked key press
        	//FIXME we only need this if the user is keeping the default suggestion of PWR = instant unlock
    	}
    
    public void wakeup() {
    	setBright((float) 0.1);//tell screen to go on with 10% brightness
    	//poke user activity just to be safe that it won't flicker back off
    	PowerManager myPM = (PowerManager) getApplicationContext().getSystemService(Context.POWER_SERVICE);
  	  	myPM.userActivity(SystemClock.uptimeMillis(), false);
  	  	//doesn't seem necessary
  	  	
    	screenwake = true;
    	timeleft = 0;//this way the task doesn't keep going     	  	
    }
    
    
    
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
    	super.onConfigurationChanged(newConfig);
     	if (newConfig.hardKeyboardHidden == Configuration.HARDKEYBOARDHIDDEN_NO) {
     		//this means that a config change happened and the keyboard is open. we only 
     		if (starting) {
     			Log.v("slide-open lock","aborting handling, slide was opened before this lock");
     		}
     		else {
     		finishing = true;
     		
     		setBright((float) 0.1);
        	moveTaskToBack(true);//finish();
     		}
     	}
     	else if (newConfig.hardKeyboardHidden == Configuration.HARDKEYBOARDHIDDEN_YES)
     		Log.v("slide closed","lockscreen activity got the config change from background");
     	//This comes in if we had slide unlocked, then user closes it, we get this first thing as we re-lock
     	//so this doesn't help us disengage stay awake immediately at close
     	
     	//FIXME a quiet wake happens when user closes slide while asleep. we need to handle it as such
     	//we get this immediately at that time since we are active
     	//unlike the delayed receipt of the changes user does while lockscreen is in the background
     	
    	/*A flag indicating whether the hard keyboard has been hidden.
    	This will be set on a device with a mechanism to hide the keyboard from the user, when that mechanism is closed.
    	One of: HARDKEYBOARDHIDDEN_NO, HARDKEYBOARDHIDDEN_YES.
    	*/    	
    }
        
    @Override
    protected void onStop() {
        super.onStop();
                
        Log.v("lockscreen stop","checking if user left");
        if (finishing) {
        	Log.v("lock stop","onStop is telling mediator we have been unlocked by one touch unlock");
        }
        else if (screenwake && paused && !hasWindowFocus()) {
        	//we were awake, we got paused, and lost focus
        	//this only happens if user is navigating out via notif, popup, or home key shortcuts
        	Log.v("lock stop","onStop is telling mediator we have been unlocked by user navigation");
        	
        }
        else return;//I can't think of a stop that wouldn't be one of these two
        
        
        
        starting = true;//this way if we get brought back we'll be aware of it

        screenwake = false;
        
        //set task which sees if starting flag is true. if so, it actually destroys the activity
        //serviceHandler.postDelayed(myTask, 5000L);
        //doesn't seem necessary right now
        //the only benefit to doing this is to avoid user accidental recovery of lockscreen window by back key (history stack)
        
        //also, this could eliminate the recovery of the screen after a received-from-sleep call is over
        
        StopCallback();
    }
    
    @Override
    protected void onPause() {
    	super.onPause();
    	//appears we always pause when leaving the lockscreen but it also happens at times in sleep and wakeup
    	
    	Log.v("lock paused","setting pause flag");
    	
    	    	
    	//since pauses also occur while it is asleep but focus is not lost, we will only send "exited" callback
    	//when paused and !hasWindowFocus()
    	//this is handled by onStop which occurs in that scenario and we detect it by this combo of lifecycle flags
    	paused = true;

    }
    
    @Override
    protected void onResume() {
    	super.onResume();
    	Log.v("lock resume","setting pause flag");
    	paused = false;
    	
    	updateClock();
    }
    
/* Key event bug ---- device wakes but we aren't getting the event so screen stays at 0.0 bright
02-02 13:32:12.725: INFO/power(1019): *** set_screen_state 1
02-02 13:32:12.741: DEBUG/Sensors(1019): using sensors (name=sensors)
02-02 13:32:12.772: WARN/WindowManager(1019): No focus window, dropping: KeyEvent{action=1 code=26 repeat=0 meta=0 scancode=107 mFlags=8}
02-02 13:32:12.772: VERBOSE/lock resume(5731): setting pause flag
02-02 13:32:12.788: WARN/UsageStats(1019): Something wrong here, didn't expect i4nc4mp.myLockcomplete to be resumed
02-02 13:32:13.188: DEBUG/SurfaceFlinger(1019): Screen about to return, flinger = 0x114758
02-02 13:32:13.264: VERBOSE/screenon(5731): Screen just went ON!
 */
    
    @Override
    public void onDestroy() {
        super.onDestroy();
        
        
       serviceHandler.removeCallbacks(myTask);
       serviceHandler = null;
       
       unregisterReceiver(screenoff);
       unregisterReceiver(idleExit);
       
       
    	
       Log.v("destroyWelcome","Destroying");
    }
    
    @Override
    public void onWindowFocusChanged (boolean hasFocus) {
    	if (hasFocus) {
    		Log.v("focus change","we have gained focus");
    		//Catch first focus gain after onStart here.
    		//this way if something stops us from immediate focus gain, we still call back appropriately
    		if (starting) {
    			starting = false;
    			//set our own lifecycle reference now that we know we started and got focus properly
    			
    			//tell mediator it is no longer waiting for us to start up
    			StartCallback();
    		}
    	}
    	else {    		    		   		
    		//if (!hasWindowFocus()) //seems to return same thing as this event reaction method
    			Log.v("focus loss","lost focus, checking if we should wake to yield to other events");
    			if (!waking && !finishing && paused) {
    				//not aware of any deliberate action- we're paused, not awake, and not about to finish
    				//this focus loss means something else needs us to wake up the screen (like a ringing alarm)
    				waking=true;
    				wakeup();
    				//this passes a wakeup we don't cancel unless we see that we have focus again
    			}    	
    	}
    }
    
    protected void onStart() {
    	super.onStart();
    	Log.v("lockscreen start success","setting flags");
    	
    	if (finishing) {
    		finishing = false;//since we are sometimes being brought back, safe to ensure flags are like at creation
    		shouldFinish = false;
    		waking = false;
    		screenwake = false;
    		setBright((float) 0.0);
    	}
    }
    
    public void StartCallback() {
    	Intent i = new Intent("i4nc4mp.myLockcomplete.lifecycle.LOCKSCREEN_PRIMED");
        getApplicationContext().sendBroadcast(i);
    }
    
    public void StopCallback() {
    	Intent i = new Intent("i4nc4mp.myLockcomplete.lifecycle.LOCKSCREEN_EXITED");
        getApplicationContext().sendBroadcast(i);
    }
    
    //here's where most of the magic happens
    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {

        boolean up = event.getAction() == KeyEvent.ACTION_UP;
        //flags to true if the event we are getting is the up (release)
        //when we are coming from sleep, the pwr down gets taken by power manager to cause wakeup
        //if we are awake already the power up might also get taken.
        
        //however even from sleep we get a down and an up for focus & cam keys with a full press
        
        int code = event.getKeyCode();
        Log.v("dispatching a key event","Is this the up? -" + up);
        
       //TODO implement pref-checking method to see if any advanced power saved keys are set
        
        int reaction = 1;//wakeup, the preferred behavior in advanced mode
                
        if (code == KeyEvent.KEYCODE_BACK) reaction = 3;//check for wake, if yes, exit
        else if (code == KeyEvent.KEYCODE_POWER) reaction = 2;//unlock
        else if (code == KeyEvent.KEYCODE_FOCUS) reaction = 0;//locked (advanced power save)
       
        	switch (reaction) {
        	case 3:
        		onBackPressed();
        		return true;
        	case 2:
    	   if (up && !finishing) {
    		   Log.v("unlock key","power key UP, unlocking");
    		   finishing = true;
    	  	  	
    		   setBright((float) 0.1);
    		       		       		  
    		   moveTaskToBack(true);
    		  
    	   }
                   return true;
       
        	case 1:
    	   if (up && !screenwake) {
                   waking = true;
                  	Log.v("key event","wake key");
               	wakeup();
    	   }
    	   return true;
       
        case 0:    	   
    	   if (!screenwake && up) {
         	   timeleft=10;
         	//so that countdown is refreshed
            //countdown won't be running in screenwakes
         	if (!waking) {
            //start up the quiet wake timer    
             Log.v("key event","locked key timer starting");

             	waking = true;
             	serviceHandler.postDelayed(myTask, 500L);
             		}
            }
             
             
    	   return true;
       }
        	return false;
    }
      
        
    
}
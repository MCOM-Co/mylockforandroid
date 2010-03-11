package i4nc4mp.myLockcomplete;

import java.util.GregorianCalendar;

import i4nc4mp.myLockcomplete.ManageKeyguard.LaunchOnKeyguardExit;
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
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;


//Guarded mode for use only without pattern mode
//which does the secure lockscreen exit
//that was used by the original alpha 2c

public class GuardActivity extends Activity {
    
	Handler serviceHandler;
	Task myTask = new Task();     
    
    public int timeleft = 0;
    
    
/* Lifecycle flags */
    public boolean starting = true;//flag off after we successfully gain focus. flag on when we send task to back
    public boolean finishing = false;//flag on when an event causes unlock, back off when onStart comes in again (relocked)
    
    public boolean paused = false;
    
    public boolean shouldFinish = false;
    //flag it to true if user hits power to wake up but quiet wake was already active
    //this lets our task wait a half second, then actually wake up and finish
    
    public boolean resumedwithfocus = false;
    //can use this to run a timer that checks if any key input results come within a few seconds
    //if nothing comes in we then know that we need to do something about the unhandled wake
    //we will also come into this state where a wake or unlock key is being done
    //but those states then set themselves within the first second.
    
    public boolean idle = false;
    
    public boolean slideWakeup = false;
    //we will set this when we detect slideopen
    
    
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
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);
        
        updateLayout();
        
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
        
        IntentFilter offfilter = new IntentFilter (Intent.ACTION_SCREEN_ON);
		registerReceiver(screenon, offfilter);
        
		IntentFilter idleFinish = new IntentFilter ("i4nc4mp.myLockcomplete.intent.action.IDLE_TIMEOUT");
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
        
    //TODO we might be able to offer customization of advanced power save for bumped slider
    //need to try this, low priority
    
    
    BroadcastReceiver screenon = new BroadcastReceiver() {
         	
        public static final String Screenon = "android.intent.action.SCREEN_ON";

        @Override
        public void onReceive(Context context, Intent intent) {
                if (!intent.getAction().equals(Screenon)) return;
                
        DoExit(getApplicationContext());
                
        return;//avoid unresponsive receiver error outcome
             
}};

	BroadcastReceiver idleExit = new BroadcastReceiver() {
	@Override
    public void onReceive(Context context, Intent intent) {
	if (!intent.getAction().equals("i4nc4mp.myLockcomplete.intent.action.IDLE_TIMEOUT")) return;
	
	finishing = true;
	idle = true;
	
	Log.v("exit intent received","calling finish");
	
	finish();
}};

	class Task implements Runnable {
	public void run() {
		//in case we ever need to use a delay to securely exit consistently
		ManageKeyguard.exitKeyguardSecurely(new LaunchOnKeyguardExit() {
            public void LaunchOnKeyguardExitSuccess() {
               Log.v("doExit", "This is the exit callback");
               finishing = true;
               //finish();
               moveTaskToBack(true);
                }});
	}}
	
	@Override
    public void onConfigurationChanged(Configuration newConfig) {
    	super.onConfigurationChanged(newConfig);
     	if (newConfig.hardKeyboardHidden == Configuration.HARDKEYBOARDHIDDEN_NO) {
     		//this means that a config change happened and the keyboard is open.
     		if (starting) {
     			Log.v("slide-open lock","aborting handling, slide was opened before this lock");
     		}
     		else {
     		ManageKeyguard.disableKeyguard(getApplicationContext());
     		     		
        	slideWakeup = true;
     		}
     	}
     	else if (newConfig.hardKeyboardHidden == Configuration.HARDKEYBOARDHIDDEN_YES)
     		Log.v("slide closed","lockscreen activity got the config change from background");    	
    }
    
	@Override
    protected void onStop() {
        super.onStop();
                
        Log.v("lockscreen stop","checking if user left");
        if (finishing) {
        	Log.v("lock stop","onStop is telling mediator we have been unlocked by one touch unlock");
        }
        else if (paused && !hasWindowFocus()) {
        	//we got paused, and lost focus
        	//this only happens if user is navigating out via notif, popup, or home key shortcuts
        	Log.v("lock stop","onStop is telling mediator we have been unlocked by user navigation");
        	
        }
        else return;//I can't think of a stop that wouldn't be one of these two
        
        
        
        starting = true;//this way if we get brought back we'll be aware of it
        resumedwithfocus = false;
        
        //set task which sees if starting flag is still true 5 sec from now
        //if so, it actually destroys the activity, to prevent users from stack history navigating back in
        //serviceHandler.postDelayed(myTask, 5000L);
        
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
    	resumedwithfocus = false;
    }
    
    @Override
    protected void onResume() {
    	super.onResume();
    	Log.v("lock resume","setting pause flag");
    	paused = false;
    	
    	if (hasWindowFocus()) {
    		resumedwithfocus = true;
    		//serviceHandler.postDelayed(myTask, 1000L);
    	}
    	updateClock();
    }
    
    @Override
    public void onDestroy() {
        super.onDestroy();
        
        
       serviceHandler.removeCallbacks(myTask);
       serviceHandler = null;
       
       unregisterReceiver(screenon);
       unregisterReceiver(idleExit);
       
       
    	
        Log.v("destroyWelcome","Destroying");
    }
    
    @Override
    public void onWindowFocusChanged (boolean hasFocus) {
    	if (hasFocus) {
    		Log.v("focus change","we have gained focus");
    		//Catch first focus gain after onStart here.
    		//this allows us to know if we actually got as far as having focus (expected but bug sometimes prevents
    		if (starting) {
    			starting = false;//set our own lifecycle reference now that we know we started and got focus properly
    			
    			//tell mediator it is no longer waiting for us to start up
    			StartCallback();
    		}
    	}
    	else {    		    		   		
    			if (!finishing && paused) {
    				//not aware of any deliberate action- we're paused, and not about to finish
    				//this focus loss means something else needs us to wake up the screen (like a ringing alarm)
    				Log.v("focus lost to external event","null");
    			}
    		
    	
    	}
    }
    
    protected void onStart() {
    	super.onStart();
    	Log.v("lockscreen start success","setting flags");
    	
    	if (finishing) {
    		finishing = false;
    		//since we are sometimes being brought back, safe to ensure flags are like at creation
    		shouldFinish = false;
    	}
    }
    
    public void CallbackMediator() {
        Intent i = new Intent();
    	i.setClassName("i4nc4mp.myLockcomplete", "i4nc4mp.myLockcomplete.CustomLockService");
    	startService(i);
        }
    
    public void StartCallback() {
    	Intent i = new Intent("i4nc4mp.myLockcomplete.lifecycle.LOCKSCREEN_PRIMED");
        getApplicationContext().sendBroadcast(i);
    }
    
    public void StopCallback() {
    	Intent i = new Intent("i4nc4mp.myLockcomplete.lifecycle.LOCKSCREEN_EXITED");
        getApplicationContext().sendBroadcast(i);
    }
    
    public void DoExit(Context context) {//try the alpha keyguard manager secure exit
        
        //ManageKeyguard.initialize(context);
        //PowerManager pm = (PowerManager) getSystemService (Context.POWER_SERVICE); 
        //pm.userActivity(SystemClock.uptimeMillis(), false);
        //ensure it will be awake
        
        if (!slideWakeup) ManageKeyguard.disableKeyguard(getApplicationContext());
        else {
        	slideWakeup = false;
        	Log.v("completing slider open wake","about to try secure exit");
        }
        
        serviceHandler.postDelayed(myTask, 50);
        
        /*
        ManageKeyguard.exitKeyguardSecurely(new LaunchOnKeyguardExit() {
            public void LaunchOnKeyguardExitSuccess() {
               Log.v("doExit", "This is the exit callback");
               finishing = true;
               //finish();
               moveTaskToBack(true);
                }});*/            
    }
    
    
}
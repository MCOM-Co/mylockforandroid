package i4nc4mp.myLock;

import i4nc4mp.myLock.ManageKeyguard.LaunchOnKeyguardExit;
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
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;


//Guarded mode - Lockscreen replacement that's designed to be used without pattern mode on
//the mediator for this mode does the pattern suppress with option for idle timeout
//it simply replaces the lockscreen, does not touch any wakeup rules

//the instant unlocking functionality will be released as standalone myLock basic
//automatically doing the secure lockscreen exit that was used by the original alpha 2c
//FIXME when it instant unlocks/dismisses at wakeup it causes big bugs in incoming or ignored calls.
//got unique odd behavior out of those two cases. FIX for the basic ver update.

//When it is time to exit (by back key or slidingtab) we start a one shot dismiss activity.
//The dismiss activity will load, wait 50 ms, then finish
//Here, we finish in the background immediately after requesting the dismiss activity

//For this lifecycle, we go dormant for any outside event
//such as incoming call ringing, alarm, handcent popup, etc.
//we detect going dormant by losing focus while already paused.
//if focus loss occurs while not paused, it means the user is actively navigating out of the woken lockscreen

//for the exits that occur from navigation, we're forced to use the pre-2.0 exit method
//due to bugs in the overall implementation of the new flags.. there's no way to really allow the navigation exit
//when only show_when_locked is active. i can't seem to make it cooperate with dismissActivity
//because the KG comes back and blocks it
//however, for instant exit, the dismissActivity code is flawless

//==========================
//SHOW WHEN LOCKED activity which takes over mediation of actual lockscreen dismissal
//It will present a blank guard screen on slider open because there is a large lag when intentionally opening slider for lockscreen skip
//user can press back or home to exit from there
//TODO - slide to unlock or a lifecycle to dismiss this screen and show stock lock slider when slide is opened

public class BasicGuardActivity extends Activity {
	//import from the guard activity from Complete revision
	Handler serviceHandler;
    Task myTask = new Task();
    //OtherTask dismissthread = new OtherTask(); 


/* Lifecycle flags */
public boolean starting = true;//flag off after we successfully gain focus. flag on when we send task to back
public boolean finishing = false;//flag on when an event causes unlock, back off when onStart comes in again (relocked)

public boolean paused = false;

public boolean idle = false;

public boolean dormant = false;
//special lifecycle phase- we are waiting in the background for outside event to return focus to us
//an example of this is while a call is ringing. we have to force the state
//because the call prompt acts like a user notification panel nav

public boolean pendingExit = false;
//special lifecycle phase- when we lose focus and aren't paused, we launch a KG pause
//two outcomes, either we securely exit if a pause comes in meaning user is navigating out
//or else we are going to get focus back and re-enable keyguard

public boolean slideWakeup = false;
//we will set this when we detect slideopen, only used with instant unlock (replacement for 2c ver)

public boolean pendingDismiss = false;
//will be set true when we launch the dismiss window for auto and user requested exits
//this ensures focus changes and pause/resume will be ignored to allow dismiss activity to finish

public boolean resurrected = false;
//just to handle return from dormant, avoid treating it same as a user initiated wake

public boolean isScreenOn = false;
//temporary solution for pre 2.1 screen checking
//we will simply set it in the screen on event

//====Items in the default custom lockscreen
/*
private Button mrewindIcon;
private Button mplayIcon;
private Button mpauseIcon;
private Button mforwardIcon;

public TextView curhour;
public TextView curmin;

public TextView batt;
*/
    
    
    //very very complicated business.
    @Override
protected void onCreate(Bundle icicle) {
    super.onCreate(icicle);

    requestWindowFeature(android.view.Window.FEATURE_NO_TITLE);
    getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);
    		//| WindowManager.LayoutParams.FLAG_FULLSCREEN);
    
    updateLayout();
    
    /* this is the custom lockscreen stuff
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
         
         }
      });
    
    mforwardIcon = (Button) findViewById(R.id.NextButton); 

    mforwardIcon.setOnClickListener(new View.OnClickListener() {
        public void onClick(View v) {
         Intent intent;
         intent = new Intent("com.android.music.musicservicecommand.next");
         getApplicationContext().sendBroadcast(intent);
         }
      });*/
    
    IntentFilter onfilter = new IntentFilter (Intent.ACTION_SCREEN_ON);
            registerReceiver(screenon, onfilter);
    
    IntentFilter callbegin = new IntentFilter ("i4nc4mp.myLock.lifecycle.CALL_START");
    registerReceiver(callStarted, callbegin);  
    
    IntentFilter callpend = new IntentFilter ("i4nc4mp.myLock.lifecycle.CALL_PENDING");
    registerReceiver(callPending, callpend);
    
    IntentFilter idleFinish = new IntentFilter ("i4nc4mp.myLock.lifecycle.IDLE_TIMEOUT");
    registerReceiver(idleExit, idleFinish);
            
    serviceHandler = new Handler();
}
    
    /*public void updateClock() {
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
        
        
    }*/
    
    
           
protected View inflateView(LayoutInflater inflater) {
    return inflater.inflate(R.layout.guardlayout, null);
}

private void updateLayout() {
    LayoutInflater inflater = LayoutInflater.from(this);

    setContentView(inflateView(inflater));
}
    
@Override
public void onBackPressed() {
    //Back will cause unlock
    
    StartDismiss(getApplicationContext());
    finishing=true;
}


BroadcastReceiver screenon = new BroadcastReceiver() {
            
    public static final String Screenon = "android.intent.action.SCREEN_ON";

    @Override
    public void onReceive(Context context, Intent intent) {
            if (!intent.getAction().equals(Screenon)) return;
    //FIXME we can change this to an isScreenOn in the onResume in 2.1
    //TODO testing moving auto dismiss logic into resume on condition already have focus
    //TODO restore if it doesn't work
            
    return;//avoid unresponsive receiver error outcome
         
}};

BroadcastReceiver callStarted = new BroadcastReceiver() {
    @Override
    public void onReceive(Context context, Intent intent) {
    if (!intent.getAction().equals("i4nc4mp.myLock.lifecycle.CALL_START")) return;
    
    //we are going to be dormant while this happens, therefore we need to force finish
    Log.v("guard received broadcast","completing callback and finish");
    
    //StopCallback();
    finish();
    
    return;
    }};
    
BroadcastReceiver callPending = new BroadcastReceiver() {
    @Override
       public void onReceive(Context context, Intent intent) {
    if (!intent.getAction().equals("i4nc4mp.myLock.lifecycle.CALL_PENDING")) return;
            //incoming call does not steal focus till user grabs a tab
            //lifecycle treats this like a home key exit
            //forcing dormant state here will allow us to only exit if call is answered
            dormant = true;
            return;                 
    }};


    BroadcastReceiver idleExit = new BroadcastReceiver() {
    @Override
public void onReceive(Context context, Intent intent) {
    if (!intent.getAction().equals("i4nc4mp.myLock.lifecycle.IDLE_TIMEOUT")) return;
    
    finishing = true;
    idle = true;
    
    Log.v("exit intent received","calling finish");
    finish();//we will still have focus because this comes from the mediator as a wake event
    return;
    }};

    class Task implements Runnable {
    public void run() {
            
            ManageKeyguard.exitKeyguardSecurely(new LaunchOnKeyguardExit() {
        public void LaunchOnKeyguardExitSuccess() {
           Log.v("doExit", "This is the exit callback");
           StopCallback();
           finish();
            }});
    }}
    
    /*class OtherTask implements Runnable {
    public void run() {
    		onBackPressed();
    	}
    }*/
    
    
    @Override
public void onConfigurationChanged(Configuration newConfig) {
    super.onConfigurationChanged(newConfig);
    if (newConfig.hardKeyboardHidden == Configuration.HARDKEYBOARDHIDDEN_NO) {
            //this means that a config change happened and the keyboard is open.
            if (starting) Log.v("slide-open lock","aborting handling, slide was opened before this lock");
            else {
            	Log.v("slide-open wake","setting state flag");
            	slideWakeup = true;
            }           
    }
    else if (newConfig.hardKeyboardHidden == Configuration.HARDKEYBOARDHIDDEN_YES) {
    	Log.v("slide closed","lockscreen activity got the config change from background");
    }
                  
}

    @Override
protected void onStop() {
    super.onStop();
    
    if (pendingDismiss) return;
    
    if (finishing) {
            Log.v("lock stop","we have been unlocked by a user exit request");
    }
    else if (paused) {
            if (hasWindowFocus()) {
    
            //stop is called, we were already paused, and still have focus
            //this means something is about to take focus, we should go dormant
            dormant = true;
            Log.v("lock stop","detected external event about to take focus, setting dormant");
            }
        else if (!hasWindowFocus()) {
            //we got paused, lost focus, then finally stopped
            //this only happens if user is navigating out via notif, popup, or home key shortcuts
            Log.v("lock stop","onStop is telling mediator we have been unlocked by user navigation");
        }
    }
    else Log.v("unexpected onStop","lockscreen was stopped for unknown reason");
    
    if (finishing) {
            StopCallback();
            finish();
    }
    
}

@Override
protected void onPause() {
    super.onPause();
    
    paused = true;
    
    if (!starting && !hasWindowFocus() && !pendingDismiss) {
            //case: we yielded focus to something but didn't pause. Example: notif panel
            //pause in this instance means something else is launching, that is about to try to stop us
            //so we need to exit now, as it is a user nav, not a dormancy event
            Log.v("navigation exit","got paused without focus, starting dismiss sequence");
            
            //anytime we lose focus before pause, we are calling disable
            //this will exit properly as we navigate out
            ManageKeyguard.exitKeyguardSecurely(new LaunchOnKeyguardExit() {
            public void LaunchOnKeyguardExitSuccess() {
               Log.v("doExit", "This is the exit callback");
               StopCallback();
               finish();
                }});
            
    }
    else {
    	Log.v("lock paused","normal pause - we still have focus");
    	if (slideWakeup) {
    		Log.v("returning to sleep","toggling slide wakeup false");
    		slideWakeup = false;
    	}
    	if (resurrected) {
    		Log.v("returning to sleep","toggling resurrected false");
    		resurrected = false;
    		//sometimes the invalid screen on doesn't happen
    		//in that case we just turn off the flag at next pause
    	}
    }
}

@Override
protected void onResume() {
    super.onResume();
    Log.v("lock resume","resuming, focus is " + hasWindowFocus());
    /*if (pendingDismiss) {
    	StopCallback();
    	finish();
    }*/

    paused = false;
    
    //TODO ===============
    //Resume might be the best place to start launching dismiss... screen on events can be delayed
    //Resume with focus would reliably mean a wakeup
    //as the resume never happens when outside events are taking control.
    //we lose focus direct from the paused state in that scenario
    //FIXME testing it here:
    
    /*if (resurrected) {
    	//ignore this wake as we do not actually want instant exit
    	resurrected = false;
    	Log.v("guard resurrected","ignoring invalid screen on");
    }*/
    if (hasWindowFocus() && !slideWakeup && !resurrected) {
    	
       	//StartDismiss(getApplicationContext());
       	//now tell mediator we need to exit & set pending exit flag
    	pendingDismiss = true;
    	
  	  Intent intent = new Intent("i4nc4mp.myLock.lifecycle.EXIT_REQUEST");
      getApplicationContext().sendBroadcast(intent);
      
      
    }
    
    
    //updateClock();
}

@Override
public void onDestroy() {
    super.onDestroy();
            
   serviceHandler.removeCallbacks(myTask);

   serviceHandler = null;
   
   unregisterReceiver(screenon);
   unregisterReceiver(callStarted);
   unregisterReceiver(callPending);
   unregisterReceiver(idleExit);
   
   StopCallback();
    
   Log.v("destroy Guard","Destroying");
}

@Override
public void onWindowFocusChanged (boolean hasFocus) {
    if (hasFocus) {
            Log.v("focus change","we have gained focus");
            //Catch first focus gain after onStart here.
            //this allows us to know if we actually got as far as having focus (expected but bug sometimes prevents
            if (starting) {
                    starting = false;
                    //set our own lifecycle reference now that we know we started and got focus properly
                    
                    //tell mediator it is no longer waiting for us to start up
                    StartCallback();
            }
            else if (dormant) {
                    Log.v("regained","we are no longer dormant");
                    dormant = false;
                    resurrected = true;
            }
            else if (pendingExit) {
                    Log.v("regained","we are no longer pending nav exit");
                    pendingExit = false;
                    ManageKeyguard.reenableKeyguard();
            }
    }
    else if (!pendingDismiss) {                                                  
            if (!finishing && paused) {
                            if (!dormant) {
                                    Log.v("home key exit","launching full secure exit");
                                                                                    
                                    ManageKeyguard.disableKeyguard(getApplicationContext());
                                    serviceHandler.postDelayed(myTask, 50);
                                            
                                    
                            }
                            else Log.v("dormant handoff complete","the external event now has focus");
            }
            else if (!paused) {
                    //not paused, losing focus, we are going to manually disable KG
                    Log.v("focus yielded while active","about to exit through notif nav");
                    pendingExit = true;
                    ManageKeyguard.disableKeyguard(getApplicationContext());
            }
    }
    
}

protected void onStart() {
    super.onStart();
    Log.v("lockscreen start success","setting flags");
    
    if (finishing) {
            finishing = false;
            Log.v("re-start","we got restarted while in Finishing phase, wtf");
            //since we are sometimes being brought back, safe to ensure flags are like at creation
    }
}

public void StartCallback() {
    Intent i = new Intent("i4nc4mp.myLock.lifecycle.LOCKSCREEN_PRIMED");
    getApplicationContext().sendBroadcast(i);
}

public void StopCallback() {
    Intent i = new Intent("i4nc4mp.myLock.lifecycle.LOCKSCREEN_EXITED");
    getApplicationContext().sendBroadcast(i);
}

public void StartDismiss(Context context) {
    
	PowerManager myPM = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
    myPM.userActivity(SystemClock.uptimeMillis(), false);
    //the KeyguardViewMediator poke doesn't have enough time to register before our handoff sometimes (rare)
    //this might impact the nexus more than droid. need to test further
    //result is the screen is off (as the code is successful)
    //but no keyguard, have to hit any key to wake it back up
	
    Class w = DismissActivity.class;
                  
    Intent dismiss = new Intent(context, w);
    dismiss.setFlags(//Intent.FLAG_ACTIVITY_NEW_TASK//For some reason it requires this even though we're already an activity
                    Intent.FLAG_ACTIVITY_NO_USER_ACTION//Just helps avoid conflicting with other important notifications
                    | Intent.FLAG_ACTIVITY_NO_HISTORY//Ensures the activity WILL be finished after the one time use
                    | Intent.FLAG_ACTIVITY_NO_ANIMATION);
                    
    pendingDismiss = true;
    startActivity(dismiss);//ForResult(dismiss, 1);
    
    //finish();
    //instead, we will get a call started CB from the dismiss activity as soon as it has focus
    //there might be an advantage to calling move task to back, then finish
}
}
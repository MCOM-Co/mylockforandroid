package i4nc4mp.myLockxoom;

import android.app.Activity;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.os.Handler;
import android.os.PowerManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.Toast;
import i4nc4mp.myLockxoom.ManageKeyguard.LaunchOnKeyguardExit;


//we mediate wakeup & call end, to fire dismiss activity if the lockscreen is detected

public class AutoDismiss extends MediatorService {
	private boolean persistent = false;
    
	private boolean oldmode = false;


    
    private boolean dismissed = false;
    //will just toggle true after dismiss callback - used to help ensure airtight lifecycle
    
    Handler serviceHandler;
    Task myTask = new Task();
    
    
    
    @Override
    public void onDestroy() {
    	super.onDestroy();
            
        SharedPreferences settings = getSharedPreferences("myLock", 0);       

            
            	unregisterReceiver(lockStopped);
                            	
            	settings.unregisterOnSharedPreferenceChangeListener(prefslisten);
                               
                serviceHandler.removeCallbacks(myTask);
                serviceHandler = null;
                
                
             
                
}
    
    @Override
    public void onCreate() {
    	super.onCreate();
    	if (!getSharedPreferences("myLock",0).getBoolean("startingUp", false)) {
    		Log.v("system restart","apparent low mem system restart, toggling back on");
    		ManageMediator.updateEnablePref(true, getApplicationContext());
			ManageMediator.startService(getApplicationContext());
    		Toast.makeText(AutoDismiss.this, "myLock restarted after system low mem shutdown", Toast.LENGTH_SHORT).show();	
    	}
    	else Log.v("normal oncreate","commencing first start call");
    	
    }
    
    @Override
    public void onFirstStart() {
    	
    	//first acquire the prefs that need to be initialized
            SharedPreferences settings = getSharedPreferences("myLock", 0);
            SharedPreferences.Editor editor = settings.edit();
            
            persistent = settings.getBoolean("FG", false);
                       
            oldmode = settings.getBoolean("oldmode", false);
            
            if (persistent) doFGstart();
            
            
            //register a listener to update this if pref is changed to 0
            settings.registerOnSharedPreferenceChangeListener(prefslisten);
            	
            serviceHandler = new Handler();
            
            IntentFilter lockStop = new IntentFilter ("i4nc4mp.myLockxoom.lifecycle.LOCKSCREEN_EXITED");
            registerReceiver(lockStopped, lockStop);
            
            editor.putBoolean("startingUp",false);
            editor.commit();
    }
    
    
    SharedPreferences.OnSharedPreferenceChangeListener prefslisten = new OnSharedPreferenceChangeListener () {
    	
    	public void onSharedPreferenceChanged (SharedPreferences sharedPreference, String key) {
    		Log.v("pref change","the changed key is " + key);
    		
      		if ("FG".equals(key)) {
    			boolean fgpref = sharedPreference.getBoolean(key, false);
    			if(!fgpref && persistent) {
    				stopForeground(true);//kills the ongoing notif
    			    persistent = false;
    			}
    			else if (fgpref && !persistent) doFGstart();//so FG mode is started again
      		}
      		
    		if ("oldmode".equals(key)) oldmode = sharedPreference.getBoolean(key, false);
    		}
    	};
    
    BroadcastReceiver lockStopped = new BroadcastReceiver() {
        @Override
    public void onReceive(Context context, Intent intent) {
        if (!intent.getAction().equals("i4nc4mp.myLockxoom.lifecycle.LOCKSCREEN_EXITED")) return;
        
        //couldn't get any other method to avoid the KG from shutting screen back off
        //when dismiss activity sent itself to back
        //it would ignore all user activity pokes and log "ignoring user activity while turning off screen"
        
        
        	dismissed = true;
        	//gingerbread test, we are not using wake lock
        	//if (Integer.parseInt(Build.VERSION.SDK) < 9)
        	ManageWakeLock.releaseFull();
        
        return;
        }};
        
        class Task implements Runnable {
            public void run() {
            	
            	if (oldmode) {
            		//finalize the kg exit
            		ManageKeyguard.exitKeyguardSecurely(new LaunchOnKeyguardExit() {
                        public void LaunchOnKeyguardExitSuccess() {
                           Log.v("start", "This is the exit callback");
                            }});
                            }
            	}
                           
        }
    
        public boolean isScreenOn() {
        	//Allows us to tap into the 2.1 screen check if available
        	
        	
        		PowerManager myPM = (PowerManager) getApplicationContext().getSystemService(Context.POWER_SERVICE);
        		return myPM.isScreenOn();
        }
        
    @Override
    public void onScreenWakeup() {    	
    	
    	//now let's see if the KG is even up
    	ManageKeyguard.initialize(getApplicationContext());
    	boolean KG = ManageKeyguard.inKeyguardRestrictedInputMode();
    	
    	if (KG) {
    		if (!oldmode) StartDismiss(getApplicationContext());
    		else {
    			ManageKeyguard.disableKeyguard(getApplicationContext());
                serviceHandler.postDelayed(myTask, 50L);
    		}
    	}
        
    	return;
    }
    
    @Override
    public void onScreenSleep() {
        
        dismissed = false;//flag will allow us to know we are coming into a slide wakeup
    }
    
    public void StartDismiss(Context context) {
            
    	//PowerManager myPM = (PowerManager) getApplicationContext().getSystemService(Context.POWER_SERVICE);
        //myPM.userActivity(SystemClock.uptimeMillis(), true);
    	
    	//try not using wakelock on gingerbread to see if issue with the lock handoff is fixed
    	//if (Integer.parseInt(Build.VERSION.SDK) < 9) {
    	ManageWakeLock.acquireFull(getApplicationContext());
    	
    	
    //}
    	
    	//what we should do here is launch a 5 sec wait that releases it also
    	//sometimes dismiss doesn't stop/destroy right away if no user action (ie pocket wake)
    	//so release it after 5 seconds
    	
    Class w = AutoDismissActivity.class; 
                  
    Intent dismiss = new Intent(context, w);
    dismiss.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK//required for a service to launch activity
                    | Intent.FLAG_ACTIVITY_NO_USER_ACTION//Just helps avoid conflicting with other important notifications
                    | Intent.FLAG_ACTIVITY_NO_HISTORY//Ensures the activity WILL be finished after the one time use
                    | Intent.FLAG_ACTIVITY_NO_ANIMATION);
                    
    context.startActivity(dismiss);
}
    
    
   
    
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
            CharSequence contentTitle = "quick unlock mode active";
            CharSequence contentText = "click to open settings";

            Intent notificationIntent = new Intent(this, MainPreferenceActivity.class);
            PendingIntent contentIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);

            notification.setLatestEventInfo(context, contentTitle, contentText, contentIntent);
            
            final int SVC_ID = 1;
            
            //don't need to pass notif because startForeground will do it
            //mNotificationManager.notify(SVC_ID, notification);
            persistent = true;
            
            startForeground(SVC_ID, notification);
    }
    
    public static class AutoDismissActivity extends Activity {
    	public boolean done = false;
        
        protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        
        requestWindowFeature(android.view.Window.FEATURE_NO_TITLE);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
      		  //| WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
      		  | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
      		  | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        
        Log.v("dismiss","creating dismiss window");
        
        //when using the show when locked the dismiss doesn't actually happen. lol.    
        updateLayout();
        
        //register for user present so we don't have to manually check kg with the keyguard manager
        IntentFilter userunlock = new IntentFilter (Intent.ACTION_USER_PRESENT);
        registerReceiver(unlockdone, userunlock);

    }      
        protected View inflateView(LayoutInflater inflater) {
        return inflater.inflate(R.layout.dismisslayout, null);
    }

    private void updateLayout() {
        LayoutInflater inflater = LayoutInflater.from(this);

        setContentView(inflateView(inflater));
    }

    BroadcastReceiver unlockdone = new BroadcastReceiver() {
    	    
    	    public static final String present = "android.intent.action.USER_PRESENT";

    	    @Override
    	    public void onReceive(Context context, Intent intent) {
    	    	if (!intent.getAction().equals(present)) return;
    	    	if (!done) {
    	    		Log.v("dismiss user present","sending to back");
    	    		done = true;
    	    		//callback mediator for final handling of the stupid wake lock
    	            Intent i = new Intent("i4nc4mp.myLockxoom.lifecycle.LOCKSCREEN_EXITED");
    	            getApplicationContext().sendBroadcast(i);
    	    	   	moveTaskToBack(true);
    	    	   	finish();
    	    	   	overridePendingTransition(0, 0);//supposed to avoid trying to animate
    	    	}
    	    }
    };

    @Override
    public void onDestroy() {
        super.onDestroy();      
       
        unregisterReceiver(unlockdone);
        Log.v("destroy_dismiss","Destroying");

        }
    }
    
    
}
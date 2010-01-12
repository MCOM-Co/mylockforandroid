package i4nc4mp.myLocklite;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.IBinder;
import android.provider.Settings.SettingNotFoundException;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.widget.Toast;

/* 
 * The lockscreen is a gatekeeper with rules we have to play by. 
 * We can think of "disable" more like "pause/hide"
 * 
 * The first rule of Lockscreen is disablekeyguard must be followed up with re-enable.
 * The second rule of Lockscreen is disablekeyguard must be followed up with re-enable.
 * 
 * If we fail to follow these rules, all future disables will get blocked.
 * The gatekeeper "learns" of our breaking the rules when user presses home key.
 * At which point it will force the lockscreen back on at next screen off.
 * 
 * Luckily, securely exit can also be called as the alternative to re-enabling.
 * Lite mode does not do this, in favor of faster performance.
 * So most of this code just mediates between lockscreen and user
 * to ensure we follow the rules correctly in all scenarios
 */

public class LockMediatorService extends Service {
	
	
/*Service Status Flags*/
	public boolean initialized = false;
	//true after the first start command executes, turning on the phone & screen listeners
		
	public boolean active = false;
	//set by screen listen initializers to block redundant calls that can cause force close
	
	public boolean unlocked = false;
	//set true when screen on initiates a lockscreen disable, true means user has awakened the device
	
	
/*Phone Status Flags*/
	public int lastphonestate = 100;
	//because we receive a state change to 0 when listener starts
	
	public boolean receivingcall = false;
	//true when state 1 to 2 occurs
	public boolean placingcall = false;
	//true when state 0 to 2 occurs
	
	public boolean callwakeup = false;
	//true when incoming call occurs while device is locked
	
	
/*Settings Flags*/	
	public int patternsetting = 0;
	//we'll see if the user has pattern enabled when we startup
	//so we can disable it and then restore when we finish
	
	@Override
	public IBinder onBind(Intent arg0) {
		Log.d(getClass().getSimpleName(), "onBind()");
		return null;//we don't bind
	}


	
	@Override
	public void onCreate() {
		super.onCreate();
		//Log.d(getClass().getSimpleName(),"onCreate()");
	}
	
	@Override
	public void onDestroy() {
		super.onDestroy();
			
		//Log.d(getClass().getSimpleName(),"onDestroy()");
		
		pause();
		
		if (patternsetting == 1) {
			android.provider.Settings.System.putInt(getContentResolver(), 
    			android.provider.Settings.System.LOCK_PATTERN_ENABLED, 1);
    	//re-enable pattern lock if applicable
		}
			
			if (unlocked) {
				//TODO post a handler delay of 1 second here
				//maybe even move this logic to the toggler
				ManageKeyguard.reenableKeyguard();
				//restart the lockscreen, this follows the rules of the lockscreen gatekeeper
				unlocked = false;
			}
	}
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		super.onStartCommand(intent, flags, startId);
		
		if (initialized) {
			//any repeat start commands will be processed here.
			
			//currently, I have a toggler service attached to the widget button
			//which does create and destroy the mediator
			
			Log.v("re-start", "start command received");

			return 1;
		}
		
		//need to do first inits
		Log.v("first-start", "init service");
		//find out if the user has lock pattern on
		try {
			patternsetting = android.provider.Settings.System.getInt(getContentResolver(), android.provider.Settings.System.LOCK_PATTERN_ENABLED);
		} catch (SettingNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		if (patternsetting == 1) {    	
    	android.provider.Settings.System.putInt(getContentResolver(), 
    			android.provider.Settings.System.LOCK_PATTERN_ENABLED, 0); 
    	//tries turning off the lock pattern
		}
		
		activate();
		
		initPhoneListen();
   		
    	initialized = true;//protect it from retrying any of the init commands    	
    	
    	return 1;
	}
	
	void initPhoneListen() {
		//register with the telephony mgr to make sure we can read call state
		//First get the ref to existing TM. We don't need the service to globally keep this reference.
    	final TelephonyManager tm = (TelephonyManager)getSystemService(Context.TELEPHONY_SERVICE); 
        assert(tm != null); //gets the ref to existing TM. We don't need the service to globally keep it
        tm.listen(Detector, PhoneStateListener.LISTEN_CALL_STATE);
	}
	
	void stopPhoneListen() {//not currently ever used
		
		//First get the ref to existing TM. We don't need the service to globally keep this reference.
		final TelephonyManager tm = (TelephonyManager)getSystemService(Context.TELEPHONY_SERVICE); 
        assert(tm != null);
        tm.listen(Detector, PhoneStateListener.LISTEN_NONE);//just pass none to our spawned listener
        lastphonestate = 100;//set the state so the listener will know it has been paused
	}
	
	PhoneStateListener Detector = new PhoneStateListener() {
      	
		/*
		CALL_STATE_IDLE is 0 - this state comes back when calls end
		CALL_STATE_RINGING is 1 - a call is incoming, waiting for user to answer.
		CALL_STATE_OFFHOOK is 2 - call is actually in progress
		 */
		
		
    	@Override
    	public void onCallStateChanged(int state, String incomingNumber) 
        {
    		//0 to 1 when a call starts ringing
            //0 to 2 when user places a call
            
            //1 to 2 when user answers a call
            //1 to 0 when a call is ignored/missed
            
            //2 to 0 when a call ends
            
    		if (state == 2) {
    			if (lastphonestate==1) {
    				if (!unlocked) callwakeup = true;
    				//flag this so end of call can be handled for lockscreen restore
    				
    				//it seems if user presses hangup the phone takes them a visible lockscreen
    				//as if they had pressed power
    				
    				//if other party hangs up it forces screen to sleep
    				//and our disable doesn't wake it up but still disables lockscreen
    				
    				receivingcall = true;
    			}
    			
    			if (lastphonestate==0) {
    				//you can only place calls after unlocking
                	placingcall = true;
                }
            
            pause();
    		}
    		else if (state==1){
//state 1, waiting call is ringing. users have requested a way to skip the incoming call lockscreen
//we could spawn a dialog that allows the ringing call to be answered via key event
//that's a TODO as I don't know how currently
        	}
    		else {//return to 0, call is ending
    			
    			if (lastphonestate == 100) Log.v("ListenInit","first phone listener init");
    			else {//Change to 0 happens at first init as well as end of calls
    			
    		
    			if (lastphonestate==2) {//state 1 to 0 is user ignored call, no pause occurs
    				activate();
    			    			   			
    				if (callwakeup) {
    			//incoming call woke device, so disable lockscreen for user
    					ManageKeyguard.disableKeyguard(getApplicationContext());
    					callwakeup = false;
    					}
    				
    				unlocked = true;//queue next screen off to restore lockscreen
    				
    				//cancel call flags now that call is over
    				receivingcall = false;
    				placingcall = false;
    				}
    			}
    		}
    		
    		//all state changes store themselves so changes can be interpreted
            lastphonestate = state;
        }
    };
    
    /*
    OnSharedPreferenceChangeListener PrefDetector = new OnSharedPreferenceChangeListener() {
    	@Override
    	void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        	
        }
    };*/
    
	
	BroadcastReceiver screenwakeup = new BroadcastReceiver() {
		
		public static final String TAG = "screen wakeup";
		public static final String Screen = "android.intent.action.SCREEN_ON";
		
		@Override
		public void onReceive(Context context, Intent intent) {
			if (!intent.getAction().equals(Screen)) return;
			
			Log.v(TAG, "Screen just went ON!");
		
			if (!unlocked) {
				//pause the lockscreen and set flag so next screen off will re-enable
				ManageKeyguard.disableKeyguard(context);
				unlocked = true;
			}
			else Log.v(TAG,"Something wrong, redundant unlock request.");
}};
	
	BroadcastReceiver screensleep = new BroadcastReceiver() {
        
        public static final String TAG = "screen sleep";
        public static final String Screenoff = "android.intent.action.SCREEN_OFF";

        @Override
        public void onReceive(Context context, Intent intent) {
                if (!intent.getAction().equals(Screenoff)) return;
                
                Log.v(TAG, "Screen just went OFF");
                if (unlocked) {
                	//ensures this broadcast will always unpause a paused lockscreen
                	//at times we need to force unlocked to true to follow lockscreen rules
                	ManageKeyguard.reenableKeyguard();
                	unlocked = false;
                }
                else Log.v(TAG,"Something wrong, redundant lock request.");
}};

void activate() {
	if (active) return;
	
	//register the receivers
	IntentFilter onfilter = new IntentFilter (Intent.ACTION_SCREEN_ON);
	IntentFilter offfilter = new IntentFilter (Intent.ACTION_SCREEN_OFF);
	registerReceiver(screenwakeup, onfilter);
	registerReceiver(screensleep, offfilter);
	
	active = true;
	doNotify(false);//pass notification of change
}

void pause() {
	if (!active) return;
	
	//destroy the receivers
	unregisterReceiver(screenwakeup);
    unregisterReceiver(screensleep);
	
	active = false;
	doNotify(true);//pass notification of change
}

void doNotify(boolean stopping) {//to support the Lite vision, no notifications only Toast
	
	//String ns = Context.NOTIFICATION_SERVICE;
	//NotificationManager mNotificationManager = (NotificationManager) getSystemService(ns);
	
	//int icon = R.drawable.icon;
	
	//CharSequence contentTitle = "myLock";
	//CharSequence tickerText = "myLock lite is starting";
	//CharSequence contentText = "lockscreen has been disabled";
	
	String update = "Lockscreen has been disabled";
	if (patternsetting == 1) update = "Pattern lockdown disabled";
	if (receivingcall || placingcall) update = "myLock will now resume";
	
	if (stopping) {
		//tickerText = "myLock lite is stopping";
		//contentText = "lockscreen has been re-enabled";
		update = "Lockscreen has been re-enabled";
		if (patternsetting == 1) update = "Pattern lockdown restored";
		if (receivingcall || placingcall) {
			//tickerText = "myLock must pause for call";
			//contentText = "call in progress";
			update = "myLock is pausing due to call";
		}
		
	}
	Toast.makeText(LockMediatorService.this, update, Toast.LENGTH_LONG).show();	
	
	//long when = System.currentTimeMillis();

	//Notification notification = new Notification(icon, tickerText, when);
	
	//Context context = getApplicationContext();
	
	
	//Intent i = new Intent();
	//i.setClassName("i4nc4mp.myLocklite", "i4nc4mp.myLocklite.Toggler");
	//PendingIntent contentIntent = PendingIntent.getService(context, 0, i, 0);
	
	
	//notification.setLatestEventInfo(context, contentTitle, contentText, contentIntent);
	
	//final int SVC_ID = 1;
	
	//mNotificationManager.notify(SVC_ID, notification);
}

}
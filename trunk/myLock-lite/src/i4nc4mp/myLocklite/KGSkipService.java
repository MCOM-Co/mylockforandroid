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
import android.os.RemoteException;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;

public class KGSkipService extends Service {
	
	public boolean started = false;
	//flag to keep track of whether it is a first start command
	//helps account for actions needed at boot like phone listener init
	//as well as the first lockscreen exit in myLock complete
		
	public boolean skipactive = false;
	//to stop redundant receiver registration calls when a user answers an incoming call
	//also allows us to pause or restart skipping from the started command
	
	public boolean skipinprogress = false;
	//this will be set true when screen on pauses lockscreen
	//this ensures screen off plays nice with user & phone pause requests
	
	public int lastphonestate = 100;
	//because we receive a state change to 0 when listener starts
	//that function will do nothing when the state was previously 100
	
	public boolean receivingcall = false;
	//we set it true when state 1 to 2 change occurs
	
	
	@Override
	public IBinder onBind(Intent arg0) {
		Log.d(getClass().getSimpleName(), "onBind()");
		return myRemoteServiceStub;
	}

	private IMyRemoteService.Stub myRemoteServiceStub = new IMyRemoteService.Stub() {
		public void ToggleFG() throws RemoteException {
			return;
		}
	};
	
	@Override
	public void onCreate() {
		super.onCreate();
		//Log.d(getClass().getSimpleName(),"onCreate()");
	}
	
	@Override
	public void onDestroy() {
		super.onDestroy();
			
		//Log.d(getClass().getSimpleName(),"onDestroy()");
		
		cancelSkip();
		unregisterReceiver(guard);//also stop listening to screen off for a clean destroy
		
		//if this destroy came in while screen was on and lockscreen was paused
		if (skipinprogress) { 
			ManageKeyguard.reenableKeyguard();
			//restart the lockscreen, this follows the rules of the lockscreen gatekeeper
			skipinprogress = false;
		}
				
		//it appears that once the phone takes control our process is not going to get penalized
		//the penalty only happens if screen goes off and we fail to restore the paused lockscreen
		
		//this is only applicable to an outgoing call which starts during the pause
		//incoming calls happen while lockscreen is already enabled (not paused)
	}
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		super.onStartCommand(intent, flags, startId);
		
		if (started) {
			//any repeat start commands will be processed here.
			
			//currently, I have a toggler service that can create and destroy this service
			//that seems to work pretty well but we might be able to do without it
			//just by calling a pause or restart here
			
			Log.v("re-start", "start command received, trying toggle");

			return 1;
		}
		
		//now when all above returns fail it means that this is the first service startup
		//so need to do first inits
		Log.v("first-start", "init service");
		
		initSkip();//registers to receive the screen on broadcasts
		
		IntentFilter offfilter = new IntentFilter (Intent.ACTION_SCREEN_OFF);
		registerReceiver(guard, offfilter);
		//only register for screen off when we are first starting
		//that receiver stays on the rest of the time
		//ensuring that paused lockscreen will always be unpaused correctly
		
		initPhoneListen();//start phone call state listener which pauses automatically during calls
		//the reason for that is the phone already interacts with pausing lockscreen
   		
    	started = true;//protect it from retrying any of the init commands
    	
    	return 1;
	}
	
	void initPhoneListen() {
		//register with the telephony mgr to make sure we can read call state
		//First get the ref to existing TM. We don't need the service to globally keep this reference.
    	final TelephonyManager tm = (TelephonyManager)getSystemService(Context.TELEPHONY_SERVICE); 
        assert(tm != null); //gets the ref to existing TM. We don't need the service to globally keep it
        tm.listen(Detector, PhoneStateListener.LISTEN_CALL_STATE);
	}
	
	void stopPhoneListen() {
		//LISTEN_NONE	Stop listening for updates.
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
    		//Log.v("statechange",state + " " + incomingNumber);
    		//this revealed that incoming number is blank when not an incoming call
    		//who would have thought?
    		
            if(state!=0) {
    		//0 to 1 when a call starts ringing
            //0 to 2 when user places a call
            
            //1 to 2 when user answers a call
            //1 to 0 when a call is ignored/missed
            
            //2 to 0 when a call ends
            
            if (lastphonestate==1 && state==2) receivingcall = true;
            //so here we catch that user answered an incoming call
            
            
            cancelSkip();
            //unregisters from broadcasts and sets a safety flag
            //the flag stops the redundant try on changes from ringing to active
            
            if (lastphonestate==0 && state==2) skipinprogress=false;
            //this line reacts to outgoing call
            //that case always occurrs during a paused lockscreen
            //so we need to stop the screen off from unpausing
            //since control is being passed to the phone app
            
            }
    		else {
    			if (lastphonestate == 100) Log.v("ListenInit","first phone listener init");
    			else {
    			//Change to 0 means call ended when it isn't from the first init
    			
    			initSkip();
    			//register again for screen broadcasts
    			
    			if (receivingcall) {//an incoming call is ending
    				ManageKeyguard.disableKeyguard(getApplicationContext());
    				receivingcall = false;
    				}
    			}
    		}
            lastphonestate = state;
        }
    };
    
    /*
    OnSharedPreferenceChangeListener PrefDetector = new OnSharedPreferenceChangeListener() {
    	@Override
    	void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        	
        }
    };*/
    
	
	BroadcastReceiver skip = new BroadcastReceiver() {
		
		public static final String TAG = "skip";
		public static final String Screen = "android.intent.action.SCREEN_ON";
		
		@Override
		public void onReceive(Context context, Intent intent) {
			if (!intent.getAction().equals(Screen)) return;
			
			Log.v(TAG, "Screen just went ON!");
		
			if (!skipinprogress) {
				//pause the lockscreen and set flag so next screen off will re-enable
				ManageKeyguard.disableKeyguard(context);
				skipinprogress = true;
			}
}};
	
	BroadcastReceiver guard = new BroadcastReceiver() {
        
        public static final String TAG = "guard";
        public static final String Screenoff = "android.intent.action.SCREEN_OFF";

        @Override
        public void onReceive(Context context, Intent intent) {
                if (!intent.getAction().equals(Screenoff)) return;
                
                Log.v(TAG, "Screen just went OFF");
                if (skipinprogress) {
                	//ensures this broadcast will always unpause a paused lockscreen
                	ManageKeyguard.reenableKeyguard();
                	skipinprogress = false;
                }
}};

void initSkip() {
	if (skipactive) return;//protect from bad redundant calls
	
	//register the receivers
	IntentFilter onfilter = new IntentFilter (Intent.ACTION_SCREEN_ON);
	//IntentFilter offfilter = new IntentFilter (Intent.ACTION_SCREEN_OFF);
	registerReceiver(skip, onfilter);
	//registerReceiver(guard, offfilter);
	skipactive = true;
	doNotify(false);
}

void cancelSkip() {
	if (!skipactive) return;//protect from bad redundant calls
	
	//destroy the receivers
	unregisterReceiver(skip);
    //unregisterReceiver(guard);
	//we leave guard registered because it needs to unpause the lockscreen
	
	skipactive = false;
	doNotify(true);
}

void doNotify(boolean stopping) {
	//instead of toast use notification to tell user that service has started or stopped
	
	String ns = Context.NOTIFICATION_SERVICE;
	NotificationManager mNotificationManager = (NotificationManager) getSystemService(ns);
	
	int icon = R.drawable.icon;
	
	CharSequence contentTitle = "myLock";
	CharSequence tickerText = "myLock lite is starting";
	CharSequence contentText = "lockscreen has been disabled";
		
	if (stopping) {
		tickerText = "myLock lite is stopping";
		contentText = "lockscreen has been re-enabled";
	}
		
	long when = System.currentTimeMillis();

	Notification notification = new Notification(icon, tickerText, when);
	
	Context context = getApplicationContext();
	
	
	//Intent clickIntent = new Intent("i4nc4mp.myLock.intent.action.TOGGLE_LOCKSCREEN");
	//PendingIntent contentIntent = PendingIntent.getBroadcast(context, 0, clickIntent, 0);
	//TODO sometime it seems intent would be better for this
	//for now we are just going to use an intent for Toggler service start command.
	
	Intent i = new Intent();
	i.setClassName("i4nc4mp.myLocklite", "i4nc4mp.myLocklite.Toggler");
	PendingIntent contentIntent = PendingIntent.getService(context, 0, i, 0);
	
	
	notification.setLatestEventInfo(context, contentTitle, contentText, contentIntent);
	//notification.flags |= Notification.FLAG_ONGOING_EVENT;
	//notification.flags |= Notification.FLAG_NO_CLEAR;
	
	final int SVC_ID = 1;
	
	mNotificationManager.notify(SVC_ID, notification);
}

}
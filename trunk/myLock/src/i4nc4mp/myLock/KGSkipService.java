package i4nc4mp.myLock;

import i4nc4mp.myLock.ManageKeyguard.LaunchOnKeyguardExit;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;

public class KGSkipService extends Service {
	
	//private boolean wakeupscreen = false;
	//whether wakeup homescreen will be brought up
	//default is quick mode that just does the skip
	
	//for now we will just check this in prefs every time
	
	private Handler serviceHandler;
	private Task myTask = new Task();
	//we use a handler and a task thread to cleanly get final keyguard exit on every wakeup
	
	public boolean started = false;
	//flag to keep track of whether it is a first start command
	//this is because we normally start at device boot but user starts after first install with toggle btn
	//start gets called to flip the FG mode also, and also when the settings toggler goes to stop the service
	
	
	public boolean skipactive = false;
	//to stop redundant receiver registration calls when a user answers an incoming call
	
	public boolean isFG = false;
	//flag to keep track of whether FG mode is running.
	
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
		Log.d(getClass().getSimpleName(),"onCreate()");
	}
	
	@Override
	public void onDestroy() {
		super.onDestroy();
		serviceHandler.removeCallbacks(myTask);
		serviceHandler = null;
		//destroy the handler
		
		Log.d(getClass().getSimpleName(),"onDestroy()");
		
		cancelSkip();//disengage from screen broadcasts
	}
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		super.onStartCommand(intent, flags, startId);
		
		//Log.d(getClass().getSimpleName(), "onStart()");
		
		SharedPreferences settings = getSharedPreferences("myLock", 0);
		boolean fgpref = settings.getBoolean("FG", true);
		boolean wake = settings.getBoolean("StayAwake", false);
		boolean welcome = settings.getBoolean("welcome", false);
		
		if (started) {
			Log.v("re-start", "start command received, checking FG for change");
					
			if (isFG == fgpref) return 1;//don't do anything if the pref is still our same mode
			//that would only happen when toggle is trying a start to get oriented
			
			
			//now if a change has happened code proceeds and toggles the fg mode
			if (isFG) {
				Log.v("re-start", "changing to quiet mode");
				stopForeground(true);
				isFG = false;
			}
			else {
				Log.v("re-start", "changing to foreground mode");
				doFGstart(wake, welcome);
			}
			
			return 1;
		}
		
		//now when all above returns fail it means that this is the first service startup
		//so need to do first inits
		Log.v("first-start", "boot handler & rcvrs");
		
		serviceHandler = new Handler();
		//handler takes care of delaying our secure KG exit to satisfy security settings in the OS
			
		initSkip();//registers to receive the screen broadcasts
		
		  	
    	//register with the telephony mgr to make sure we can read call state
    	final TelephonyManager tm = (TelephonyManager)getSystemService(Context.TELEPHONY_SERVICE); 
        assert(tm != null); 
        tm.listen(Detector, PhoneStateListener.LISTEN_CALL_STATE);
        
        if(wake) ManageWakeLock.acquireFull(getApplicationContext());
        //probably not a common case but if user happens to leave stay awake on all the time
        //this initializes it
        //the rest of the time it will always be initialized by the settings activity
        
        //stay awake persists through use of the phone correctly
        
        if (fgpref) doFGstart(wake, welcome);
		else {
			Log.v("first-start", "starting without fg mode due to user pref");
		}
		        
        /*the screen on broadcast doesn't get received at boot because we just registered too late*/
    	
    	if(!welcome) {//when we are in quick mode only    		
    		Log.v("first-start", "Quick mode first KG exit");
    		
    		ManageKeyguard.initialize(getApplicationContext());
    		
    		ManageKeyguard.disableKeyguard(getApplicationContext());
    		serviceHandler.postDelayed(myTask, 50L);
    		}
    		//else StartWelcome(getApplicationContext());
    		//don't start the welcome home at first start, doesn't make sense like quick skip does
    	
    		//If it were possible to get key input from a service we'd initialize it here
    		//initialize accelerometer watcher here
        
    	started = true;//protect it from retrying any of the init commands
    	
    	return 1;
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
    		Log.v("statechange",state + " " + incomingNumber);
    		//this revealed that incoming number is blank when not an incoming call
    		//who would have thought?
    		
            if(state!=0) {
    		//0 to 1 when a call starts ringing
            //1 to 2 when user answers a call
            //0 to 2 when user places a call
            //2 to 0 when a call ends
            //1 to 0 when a call is ignored/missed
            	
            cancelSkip();
            //unregisters from broadcasts and sets a safety flag
            //the flag stops the redundant try on changes from ringing to active
    		}
    		else {
    			//Change to 0 means call ended
    			initSkip();
    			//register again for screen broadcasts
    		}
        }
    };
    
    /*
    OnSharedPreferenceChangeListener PrefDetector = new OnSharedPreferenceChangeListener() {
    	@Override
    	void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        	
        }
    };*/
    
	
	class Task implements Runnable {
		public void run() {
			ManageKeyguard.exitKeyguardSecurely(new LaunchOnKeyguardExit() {
	            public void LaunchOnKeyguardExitSuccess() {
	               Log.v("start", "This is the exit callback");
	                }});
			}
		}
	
	BroadcastReceiver skip = new BroadcastReceiver() {
		
		public static final String TAG = "skipKeyguard";
		public static final String Screen = "android.intent.action.SCREEN_ON";
		
		

		@Override
		public void onReceive(Context context, Intent intent) {
			if (!intent.getAction().equals(Screen)) return;
			
			Log.v(TAG, "Screen just went ON!");
			//marks that we received the broadcast
			
			/*here, we want to tell the system to go back to sleep in 5 seconds
			 * system automatically extends to full timeout if user does something
			 * sadly, the wakelock command doesn't seem to help
			 * secure exit of the keyguard causes the longer timeout
			 * so the short timeout will only work in the welcome screen
            */
			
           /*
           SharedPreferences settings = getSharedPreferences("myLock", 0);
           boolean wake = settings.getBoolean("StayAwake", false);
      		
      		if (!wake) {
      			//Step 1: Take control of the timeout
      		    ManageWakeLock.acquireFull(getApplicationContext());
      		    //Step 2: Set plan to cancel it back in 5 sec
      		    ManageWakeLock.DoCancel(getApplicationContext(),50);
      		}*/
			
			SharedPreferences settings = getSharedPreferences("myLock", 0);
			boolean welcome = settings.getBoolean("welcome", false);
			
			if (!welcome) {
				ManageKeyguard.disableKeyguard(context);
	    		serviceHandler.postDelayed(myTask, 50L);
	    		//call a secure exit to tell the OS we want to stay out of KG
	    		//the delay seems to allow us to properly read that the kg has been disabled
	    		//this seems to be exactly the same thing that the windowmanager based bypass does
			}
			else StartWelcome(context);	
			//welcome screen mode
}};
	
	/*
	BroadcastReceiver guard = new BroadcastReceiver() {
        
        public static final String TAG = "reguard";
        public static final String Screenoff = "android.intent.action.SCREEN_OFF";

        @Override
        public void onReceive(Context context, Intent intent) {
                if (!intent.getAction().equals(Screenoff)) return;
                
                Log.v(TAG, "Screen just went OFF");
                //alpha functionality is to simply restore KG when screen is turned off
}};*/

void initSkip() {
	if (skipactive) return;//protect from bad redundant calls
	
	//register the receivers
	IntentFilter onfilter = new IntentFilter (Intent.ACTION_SCREEN_ON);
	//IntentFilter offfilter = new IntentFilter (Intent.ACTION_SCREEN_OFF);
	registerReceiver(skip, onfilter);
	//registerReceiver(guard, offfilter);
	skipactive = true;
}

void cancelSkip() {
	if (!skipactive) return;//protect from bad redundant calls
	
	//destroy the receivers
	unregisterReceiver(skip);
    //unregisterReceiver(guard);
	skipactive = false;
}

void doFGstart(boolean wakepref, boolean welcomepref) {
	//putting ongoing notif together for start foreground
	
	//String ns = Context.NOTIFICATION_SERVICE;
	//NotificationManager mNotificationManager = (NotificationManager) getSystemService(ns);
	//No need to get the mgr, since we aren't manually sending this for FG mode.
	
	int icon = R.drawable.icon;
	CharSequence tickerText = "myLock";
	
	if (welcomepref) tickerText= tickerText + "welcome mode";
	if (wakepref) tickerText = tickerText + " (Staying Awake)";
	
	long when = System.currentTimeMillis();

	Notification notification = new Notification(icon, tickerText, when);
	
	Context context = getApplicationContext();
	CharSequence contentTitle = "myLock";
	CharSequence contentText = "lockscreen will be skipped";
	Intent notificationIntent = new Intent(this, SettingsActivity.class);
	PendingIntent contentIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);

	notification.setLatestEventInfo(context, contentTitle, contentText, contentIntent);
	notification.flags |= Notification.FLAG_ONGOING_EVENT;
	notification.flags |= Notification.FLAG_NO_CLEAR;
	
	final int SVC_ID = 1;
	
	//don't need to pass notif because startForeground will do it
	//mNotificationManager.notify(SVC_ID, notification);
	isFG = true;
	
	startForeground(SVC_ID, notification);
}

private void StartWelcome(Context context) {
	
	//TODO this needs to respect stay awake setting
	SharedPreferences settings = getSharedPreferences("myLock", 0);
    boolean wake = settings.getBoolean("StayAwake", false);
    
	ManageWakeLock.acquireFull(context);
	if (!wake) ManageWakeLock.DoCancel(context,20);
	
	Intent closeDialogs = new Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS);
	        context.sendBroadcast(closeDialogs);


	Class w = WelcomeActivity.class;
	       

	/* launch UI, explicitly stating that this is not due to user action
	         * so that the current app's notification management is not disturbed */
	        Intent welcome = new Intent(context, w);
	        
	        welcome.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
	                | Intent.FLAG_ACTIVITY_NO_USER_ACTION);
	        context.startActivity(welcome);
	}
}
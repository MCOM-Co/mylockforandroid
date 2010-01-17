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
import android.provider.Settings.SettingNotFoundException;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;

public class LockMediatorService extends Service {
	
	private Handler serviceHandler;
	private Task myTask = new Task();
	//we use a handler and a task thread to cleanly get final keyguard exit on every wakeup
	
	public boolean initialized = false;
	//flag to keep track of whether it is a first start command
	//this is because we normally start at device boot but user starts after first install with toggle btn
	
	
	public boolean active = false;
	//whether receivers are active- safety flag in the registration methods
	
	public boolean unlocked = false;
	//in custom lockscreen mode, set when user finishes the lockscreen activity
	//screen off will then reset lockscreen if this is true
	//otherwise, regular wakeups will set it true at secure exit success
	//this flag allows phone listener to flag incoming call waking the phone
	
	
	public boolean persistent = false;
	//flag to keep track of whether the no-clear notification is active
	//service always runs in foreground mode but allows users to set persistent notification pref
	
	public boolean customLock = false;
	//flag to keep track of whether the users pref for the custom lock screen is on & initialized
	
	//private Notification myNotif;
	
/*Phone Status Flags*/
	public int lastphonestate = 100;
	//because we receive a state change to 0 when listener starts
	
	public boolean receivingcall = false;
	//true when state 1 to 2 occurs
	public boolean placingcall = false;
	//true when state 0 to 2 occurs

	public boolean callwakeup = false;
	//set to true when user gets a call while they were already using device
	//this one is impact on quick mode, never affects custom lock mode
	
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
		Log.d(getClass().getSimpleName(),"onCreate()");
	}
	
	@Override
	public void onDestroy() {
		super.onDestroy();
		serviceHandler.removeCallbacks(myTask);
		serviceHandler = null;
		//destroy the handler
		
		Log.d(getClass().getSimpleName(),"onDestroy()");
		
		pause();//disengage from screen broadcasts
		
		if (patternsetting == 1) {
			android.provider.Settings.System.putInt(getContentResolver(), 
    			android.provider.Settings.System.LOCK_PATTERN_ENABLED, 1);
    	//re-enable pattern lock if applicable
		}
	}
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		super.onStartCommand(intent, flags, startId);
		
		//Log.d(getClass().getSimpleName(), "onStart()");
		
		SharedPreferences settings = getSharedPreferences("myLock", 0);
		boolean fgpref = settings.getBoolean("FG", true);
		boolean wake = settings.getBoolean("StayAwake", false);
		boolean welcome = settings.getBoolean("welcome", false);
		
		if (initialized) {
			Log.v("re-start", "running repeat start logic");
			if (persistent != fgpref) {//user changed persistent pref
			if (persistent) {
				stopForeground(true);
				persistent = false;
			}
			else doFGstart(wake, welcome);//so FG mode is started again
		}
			
			if (welcome == customLock) {//this is the case that the custom lockscreen is finishing
				unlocked = true;//flag next screen off to StartLock
			}
			else {//otherwise user is turning custom lockscreen on or off
				customLock = !customLock;
				//just flip the flag
				//screen off always handles flip to unlocked = false
			}
			
			return 1;//don't proceed - the rest of the start command is initialization code
		}
		//end of re-start logic block
	
		/* this method didn't work, broke lockscren rules
		 * what we need to do instead is place a pref item for which mode has started
		 * the pref will be the only way to have the two mediators avoid tangling each other
		 */
		//Intent i = new Intent();
		//i.setClassName("i4nc4mp.myLock", "i4nc4mp.myLock.LiteLockMediator");
		//stopService(i);
		
		Log.v("first-start", "boot handler & rcvrs");

		serviceHandler = new Handler();
		//handler takes care of delaying our secure KG exit to satisfy security settings in the OS
			
		activate();//registers to receive the screen broadcasts
		
		  	
    	//register with the telephony mgr to make sure we can read call state
    	final TelephonyManager tm = (TelephonyManager)getSystemService(Context.TELEPHONY_SERVICE); 
        assert(tm != null); 
        tm.listen(Detector, PhoneStateListener.LISTEN_CALL_STATE);
        
        if(wake) ManageWakeLock.acquireFull(getApplicationContext());
        //probably not a common case
        //if user happens to leave stay awake on all the time
        //initialize it here, otherwise always done at time of toggle in settings
        
        doFGstart(wake, welcome);
        
    /*===pattern mode disabler===*/
        try {
			patternsetting = android.provider.Settings.System.getInt(getContentResolver(), android.provider.Settings.System.LOCK_PATTERN_ENABLED);
		} catch (SettingNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		if (patternsetting == 1) {    	
    	android.provider.Settings.System.putInt(getContentResolver(), 
    			android.provider.Settings.System.LOCK_PATTERN_ENABLED, 0); 
		}
		        
    /*last, do a first init of lockscreen disable*/
    	
    	//whether or not custom lock is on we need to exit the initial lockscreen 		
    		Log.v("first-start", "Quick mode first KG exit");
    		
    		ManageKeyguard.initialize(getApplicationContext());
    		
    		ManageKeyguard.disableKeyguard(getApplicationContext());
    		serviceHandler.postDelayed(myTask, 50L);//unlock will be set by this callback
    		
    		if (welcome) customLock = true;//so screen off will place the lockscreen from now on
        
    	initialized = true;//protect it from retrying any of the init commands
    	
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
    		//0 to 1 when a call starts ringing
            //0 to 2 when user places a call
            
            //1 to 2 when user answers a call
            //1 to 0 when a call is ignored/missed
            
            //2 to 0 when a call ends
            
    		if (state == 2) {
    			if (lastphonestate==1) {
    				if (!unlocked) callwakeup = true;
    						/* looks like lock activity finishes itself in cases where screen
    						 * was awake when call ended.
    						 * we get a problem where normal lockscreen is there on next wakeup
    						 * if screen was off and other party terms the call
    						 * or if we had placed a call
    						 * doesn't matter whether we were awake before the call came--
    						 * catch this scenario when call end happens
    						*/
    				
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
    			
    			
    			if (callwakeup) {//incoming call woke device, so handle disable lockscreen for user
    				ManageKeyguard.disableKeyguard(getApplicationContext());
    				//We have to use the regular exit since the lockscreen is placed by end-call
    				serviceHandler.postDelayed(myTask, 50L);

    				callwakeup = false;
    			}
    			
    			   			
    				receivingcall = false;
    				placingcall = false;
    				
    				//FIXME bug when I place a call it is not setting unlocked to true properly
    				//next screen off, wakeup resulted in seeing regular lockscreen instead of custom
    				//this probably means we didn't receive the screen off broadcast in time
    				//unlocked was most definitely already true, i had placed a call
    				//this is the same case as when a call ends with screen asleep- next wakeup is default lockscreen
    				}
    			
    			}
    			//end of the Call Ending logic block
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
    
	
	class Task implements Runnable {
		public void run() {
			ManageKeyguard.exitKeyguardSecurely(new LaunchOnKeyguardExit() {
	            public void LaunchOnKeyguardExitSuccess() {
	               Log.v("start", "This is the exit callback");
	               unlocked = true;//set the flag so incoming calls will be handled correctly
	                }});
			}
		}
	
	BroadcastReceiver screenon = new BroadcastReceiver() {
		
		public static final String TAG = "screenon";
		public static final String Screen = "android.intent.action.SCREEN_ON";
		
		

		@Override
		public void onReceive(Context context, Intent intent) {
			if (!intent.getAction().equals(Screen)) return;
			
			Log.v(TAG, "Screen just went ON!");
			
			if (customLock) return;//screen on is handled by the custom lockscreen, do nothing here
			if(!unlocked) {
					ManageKeyguard.disableKeyguard(context);
					serviceHandler.postDelayed(myTask, 50L);//when success, unlocked set true
					//call a secure exit to tell the OS we want to stay out of KG
		    		//the delay seems to allow us to properly read that the kg has been disabled
				}
			else return;
			//this case is not expected but seems to happen at boot with custom lock on
			//the return statement averts an "unresponsive receiver error"
	    		
}};
	
	BroadcastReceiver screenoff = new BroadcastReceiver() {
        
        public static final String TAG = "screenoff";
        public static final String Screenoff = "android.intent.action.SCREEN_OFF";

        @Override
        public void onReceive(Context context, Intent intent) {
        	if (!intent.getAction().equals(Screenoff)) return;
                
                Log.v(TAG, "Screen just went OFF");
                if (unlocked) {
                	//screen off broadcast only cares if we were unlocked when it happened
                	unlocked = false;
                if (!customLock) return;//lockscreen auto-enables when not in custom lockscreen mode
                else StartLock(context);//custom lock mode- need to startup the lockscreen
                }
}};

void activate() {
	if (active) return;//protect from bad redundant calls
	
	//register the receivers
	IntentFilter onfilter = new IntentFilter (Intent.ACTION_SCREEN_ON);
	IntentFilter offfilter = new IntentFilter (Intent.ACTION_SCREEN_OFF);
	registerReceiver(screenon, onfilter);
	registerReceiver (screenoff, offfilter);
	active = true;
}

void pause() {
	if (!active) return;//protect from bad redundant calls
	
	//destroy the receivers
	unregisterReceiver(screenon);
    unregisterReceiver(screenoff);
	active = false;
}

void doFGstart(boolean wakepref, boolean welcomepref) {
	//putting ongoing notif together for start foreground
	
	//String ns = Context.NOTIFICATION_SERVICE;
	//NotificationManager mNotificationManager = (NotificationManager) getSystemService(ns);
	//No need to get the mgr, since we aren't manually sending this for FG mode.
	
	int icon = R.drawable.icon;
	CharSequence tickerText = "myLock";
	
	if (welcomepref) tickerText= tickerText + " - custom lockscreen";
	else tickerText = tickerText + " - quick unlock";
	if (wakepref) tickerText = tickerText + " (Staying Awake)";
	
	long when = System.currentTimeMillis();

	Notification notification = new Notification(icon, tickerText, when);
	
	Context context = getApplicationContext();
	CharSequence contentTitle = "myLock";
	CharSequence contentText = "lockscreen is disabled";

	Intent notificationIntent = new Intent(this, SettingsActivity.class);
	PendingIntent contentIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);

	notification.setLatestEventInfo(context, contentTitle, contentText, contentIntent);
	
	final int SVC_ID = 1;
	
	//don't need to pass notif because startForeground will do it
	//mNotificationManager.notify(SVC_ID, notification);
	persistent = true;
	startForeground(SVC_ID, notification);
}



private void StartLock(Context context) {

	Intent closeDialogs = new Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS);
	        context.sendBroadcast(closeDialogs);
	        

	        Class w = LockActivity.class;
	       

	/* launch UI, explicitly stating that this is not due to user action
	         * so that the current app's notification management is not disturbed */
	        Intent welcome = new Intent(context, w);
	        
	        welcome.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
	                //| Intent.FLAG_ACTIVITY_NO_USER_ACTION
	                | Intent.FLAG_ACTIVITY_NO_ANIMATION);
	        context.startActivity(welcome);
	}
}
package i4nc4mp.customLock;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.IBinder;
import android.util.Log;


//this service loads at boot. All it does is load a dummy custom lockscreen
//the lockscreen allows wakeup with any key because the stupid OS won't allow us a way to stop that
//the beauty of this is we can show it at the time the screen locks, so there is now no lag to wakeup
public class CustomLockMediator extends Service {
	
	public boolean unlocked = true;
	
	public boolean started = false;
	//just allow us to do init commands once and let repeat start commands do other things
	
	@Override
	public IBinder onBind(Intent arg0) {
		//Log.d(getClass().getSimpleName(), "onBind()");
		return null;//no binding
	}
	
	@Override
	public void onCreate() {
		super.onCreate();
		Log.d(getClass().getSimpleName(),"onCreate()");
	}
	
	@Override
	public void onDestroy() {
		super.onDestroy();
			
		Log.d(getClass().getSimpleName(),"onDestroy()");
		
		unregisterReceiver(screenon);
	    unregisterReceiver(screenoff);
		//disengage from screen broadcasts
	}
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		super.onStartCommand(intent, flags, startId);
		
		if (!started) {
		IntentFilter onfilter = new IntentFilter (Intent.ACTION_SCREEN_ON);
		IntentFilter offfilter = new IntentFilter (Intent.ACTION_SCREEN_OFF);
		registerReceiver(screenon, onfilter);
		registerReceiver(screenoff, offfilter);
		started = true;
		}
		else if (!unlocked) {
			unlocked = true;//flag ensures the receiver won't restart the Lock Activity
			
		}//when finish happens in the Lock Activity, it will send a start to Mediator
			
		
		return 1;
	}
	
	BroadcastReceiver screenon = new BroadcastReceiver() {
		
		public static final String TAG = "LockMediator";
		public static final String Screen = "android.intent.action.SCREEN_ON";
		
		

		@Override
		public void onReceive(Context context, Intent intent) {
			if (!intent.getAction().equals(Screen)) return;
			Log.v(TAG,"screen on happened");
			
}};
	
	
	BroadcastReceiver screenoff = new BroadcastReceiver() {
       
        public static final String TAG = "LockMediator";
        public static final String Screenoff = "android.intent.action.SCREEN_OFF";

        @Override
        public void onReceive(Context context, Intent intent) {
                if (!intent.getAction().equals(Screenoff)) return;
              Log.v(TAG,"screen off happened");
              //only repeat lock activity if we were unlocked at time of screen off
              //unlocked gets set by the finish of the Lock Activity
              if (unlocked) {
            	  unlocked = false;
            	  StartLock(context);
              }
              //right now would be a good time to re-set the user's preferred timeout
              //which we have to override for lock activity to function well
}};

private void StartLock(Context context) {

	Intent closeDialogs = new Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS);
	        context.sendBroadcast(closeDialogs);
	        
	Class w = LockActivity.class;
	       

	/* launch UI, explicitly stating that this is not due to user action
	         * so that the current app's notification management is not disturbed */
	        Intent welcome = new Intent(context, w);
	        
	        welcome.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
	                | Intent.FLAG_ACTIVITY_NO_USER_ACTION
	                | Intent.FLAG_ACTIVITY_NO_ANIMATION);
	        context.startActivity(welcome);
	}
}

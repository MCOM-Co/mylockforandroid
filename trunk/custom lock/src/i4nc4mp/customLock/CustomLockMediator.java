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
		
		IntentFilter onfilter = new IntentFilter (Intent.ACTION_SCREEN_ON);
		IntentFilter offfilter = new IntentFilter (Intent.ACTION_SCREEN_OFF);
		registerReceiver(screenon, onfilter);
		registerReceiver(screenoff, offfilter);
		
		return 1;
	}
	
	BroadcastReceiver screenon = new BroadcastReceiver() {
		
		public static final String TAG = "screenon";
		public static final String Screen = "android.intent.action.SCREEN_ON";
		
		

		@Override
		public void onReceive(Context context, Intent intent) {
			if (!intent.getAction().equals(Screen)) return;
			Log.v(TAG,"screen on happened");
			//SharedPreferences settings = getSharedPreferences("myLock", 0);
			//boolean welcome = settings.getBoolean("welcome", false);
			
			//StartLock(context);	
}};
	
	
	BroadcastReceiver screenoff = new BroadcastReceiver() {
        
        public static final String TAG = "screenoff";
        public static final String Screenoff = "android.intent.action.SCREEN_OFF";

        @Override
        public void onReceive(Context context, Intent intent) {
                if (!intent.getAction().equals(Screenoff)) return;
              Log.v(TAG,"screen off happened");  
              StartLock(context);
}};

private void StartLock(Context context) {
	
	ManageWakeLock.acquireFull(context);
	//won't cause wakeup when acquire causes wakeup not set
	//the documentation for power manager indicates that the device is supposed to sleep
	//when any screen affecting wakelock is released
	
	//this one calls a screen dime wakeup that does not wake on acquire
	//Lock activity sets to release it in 1 second when a locked key is pressed

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

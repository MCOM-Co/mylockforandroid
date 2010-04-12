package i4nc4mp.myLock;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

public class UserPresentService extends Service {
	//this will be started when myLock is shut off due to idle timeout, or at bootup
	//it starts everything up once we receive user present broadcast (meaning the lockscreen was completed)
	//just bridges the gap between an idle timeout or first startup and the user authentication of their pattern
	
	//it is only used when the user has turned on security mode
	
	public boolean secured = false;
	
	Handler serviceHandler;
    Task myTask = new Task();
	
	@Override
	public IBinder onBind(Intent arg0) {
		Log.d(getClass().getSimpleName(), "onBind()");
		return null;//we don't care
	}
	
	@Override
	public void onDestroy() {
		super.onDestroy();
		
		Log.d(getClass().getSimpleName(),"onDestroy()");
		
		unregisterReceiver(unlockdone);
		
		serviceHandler.removeCallbacks(myTask);

		serviceHandler = null;
				
		}
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		super.onStartCommand(intent, flags, startId);
		
		IntentFilter userunlock = new IntentFilter (Intent.ACTION_USER_PRESENT);
		
		registerReceiver (unlockdone, userunlock);
				
		serviceHandler = new Handler();
		
		ManageKeyguard.disableKeyguard(getApplicationContext());
		serviceHandler.postDelayed(myTask, 50L);
		
		return START_NOT_STICKY;
		//we would never get killed while sitting idle waiting for a user to come back and unlock
	}
	

	BroadcastReceiver unlockdone = new BroadcastReceiver() {
	    
	    public static final String present = "android.intent.action.USER_PRESENT";

	    @Override
	    public void onReceive(Context context, Intent intent) {
	    	if (!intent.getAction().equals(present)) return;
	    	Log.v("user unlocking","Keyguard was completed by user");
	    	//send myLock start intent
	    	Intent i = new Intent();
	    	
	    	SharedPreferences settings = getSharedPreferences("myLock", 0);
	    	boolean guard = settings.getBoolean("wallpaper",false);
			if (guard) i.setClassName("i4nc4mp.myLock", "i4nc4mp.myLock.BasicGuardService");
			else i.setClassName("i4nc4mp.myLock", "i4nc4mp.myLock.AutoDismiss");
			startService(i);
	    	//call stopSelf
			stopSelf();
	    	return;
	    
	}};
	
	class Task implements Runnable {
        public void run() {
        	/*ManageKeyguard.exitKeyguardSecurely(new LaunchOnKeyguardExit() {
                public void LaunchOnKeyguardExitSuccess() {
                   Log.v("doExit", "This is the exit callback");
                   //the callback isn't really necessary, we already get user present
                    }});*/
        	ManageKeyguard.reenableKeyguard();
        	secured = true;
        }
	}

}
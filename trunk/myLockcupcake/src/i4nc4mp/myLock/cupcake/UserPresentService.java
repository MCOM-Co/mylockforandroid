package i4nc4mp.myLock.cupcake;

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
	
	//there is a special lockdown activity we use if no KG is detected on startup
	
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
	public void onStart(Intent intent, int startId) {
		super.onStart(intent, startId);
		
		IntentFilter userunlock = new IntentFilter (Intent.ACTION_USER_PRESENT);
		
		registerReceiver (unlockdone, userunlock);
				
		serviceHandler = new Handler();
		
		ManageKeyguard.initialize(getApplicationContext());
		if (ManageKeyguard.inKeyguardRestrictedInputMode()) {
			//case - user has not unlocked the non-secure KG. Screen will probably be asleep
			//clear the standard keyguard. the delay waits 50 ms then forces the secure keyguard on
			ManageKeyguard.disableKeyguard(getApplicationContext());
			serviceHandler.postDelayed(myTask, 50L);
		}
		else {
			//case - user has unlocked immediately on startup
			//they are going about the phone like a boss
			//TIME TO SLAP THEM WITH A LOCKDOWN
			
						
			Intent slap = new Intent("i4nc4mp.myLock.FORCE_LOCK");
		    getApplicationContext().sendBroadcast(slap);
	    	secured = true;
		}
		
		return;
	}
	

	BroadcastReceiver unlockdone = new BroadcastReceiver() {
	    
	    public static final String present = "android.intent.action.USER_PRESENT";

	    @Override
	    public void onReceive(Context context, Intent intent) {
	    	if (!intent.getAction().equals(present)) return;
	    	if (!secured) return;
	    	//just in case it sends us a user present from the non-secure kg
	    	//if user is actively unlocking it immediately on boot
	    	
	    	Log.v("user unlocking","Keyguard was completed by user");
	    	
	    	//send myLock start intent
	    	Intent i = new Intent();
	    
			i.setClassName("i4nc4mp.myLock.cupcake", "i4nc4mp.myLock.cupcake.Toggler");
			i.putExtra("i4nc4mp.myLock.TargetState", true);
			startService(i);
			
			stopSelf();
	    	return;
	    
	}};
	
	class Task implements Runnable {
        public void run() {
        	ManageKeyguard.reenableKeyguard();
        	secured = true;
        }
	}

}
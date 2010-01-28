package i4nc4mp.myLock;

import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.IBinder;
import android.util.Log;

//starts up the correct mediator at boot if user's pref for start at boot is true
public class BootHandler extends Service {
	
	@Override
	public IBinder onBind(Intent intent) {
		// we don't bind it, just call start from the widget
		return null;
	}
	
	@Override
	public void onCreate() {
		super.onCreate();
		Log.d(getClass().getSimpleName(),"BootHandler onCreate");
	}
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		SharedPreferences settings = getSharedPreferences("myLock", 0);
		boolean boot = settings.getBoolean("boot", false);//retrieve user's start at boot pref
		boolean wake = settings.getBoolean("StayAwake", true);//retrieve user's stay awake pref
		
		//always start stay awake at boot
		if (wake) {
			Intent w = new Intent();
			w.setClassName("i4nc4mp.myLock","i4nc4mp.myLock.StayAwakeService");
			startService(w);
			//send a toast here or perhaps from the service itself
		}
		
		
		if (!boot) {
			stopSelf();//destroy the process because user doesn't have start at boot enabled
			return 1;
		}
		
		//boolean custom = settings.getBoolean("welcome", false);//retrieve user's mode pref
		
		//TODO we can put a pref that is called "booting"
		//the mediator will check if that is true, then respond appropriately and then set it false
		
		Intent i = new Intent();
		
		//if (!custom) i.setClassName("i4nc4mp.myLock", "i4nc4mp.myLock.NoLockService");
		//else i.setClassName("i4nc4mp.myLock", "i4nc4mp.myLock.CustomLockService");
		
		i.setClassName("i4nc4mp.myLock", "i4nc4mp.myLock.CustomLockService");
		
		startService(i);
		
		stopSelf();
		
		return 1;
	}
}
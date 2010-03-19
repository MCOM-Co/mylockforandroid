package i4nc4mp.myLockAdvanced;

import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.IBinder;
import android.util.Log;
//if user has the start at boot pref, we queue the user present detection service
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
		boolean secure = settings.getBoolean("secure", false);
		
		
		
		if (!boot) {
			stopSelf();//destroy the process because user doesn't have start at boot enabled
			return 1;
		}
		
		//FIXME next we should actually check our prefs file for the flag that pattern was suppressed
		//then restore pattern in real system prefs, for security.
		//SO will need to implement this setting at service start where the pattern suppression occurs.
		
		//boolean custom = settings.getBoolean("welcome", false);//retrieve user's mode pref
		
		Intent i = new Intent();
		
		//if (!custom) i.setClassName("i4nc4mp.myLockAdvanced", "i4nc4mp.myLockAdvanced.NoLockService");
		//else i.setClassName("i4nc4mp.myLockAdvanced", "i4nc4mp.myLockAdvanced.CustomLockService");
		
		if (!secure) i.setClassName("i4nc4mp.myLockAdvanced", "i4nc4mp.myLockAdvanced.UserPresentService");
		//the service will wait for user to complete the first lockscreen - this protects phone from a restart security circumvention
		else i.setClassName("i4nc4mp.myLockAdvanced","i4nc4mp.myLockAdvanced.SecureLockService");
			
		startService(i);
		
		stopSelf();
		
		return 1;
	}
}
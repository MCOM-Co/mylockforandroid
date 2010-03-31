package i4nc4mp.myLockGuarded;

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
		SharedPreferences.Editor editor = settings.edit();
		
		boolean boot = settings.getBoolean("boot", false);
		boolean securemode = settings.getBoolean("secure", false);
		
		boolean secure = settings.getBoolean("securepaused", false);
		//this will be true if user had pattern on when they turned on myLock
		//we turn it back off when we turn pattern mode back on from next user disable myLock
		//if it is true here, it means the phone rebooted or service died while pattern was suppressed
		
		boolean active = settings.getBoolean("serviceactive", false);

					
		
		//we will handle it by forcing pattern back on in the real system settings
		if (secure) {
			android.provider.Settings.System.putInt(getContentResolver(), 
                    android.provider.Settings.System.LOCK_PATTERN_ENABLED, 1);
			
			//set the flag in prefs back to false
			
			editor.putBoolean("securepaused", false);	
		}
		if (active) {
			editor.putBoolean("serviceactive", false);
		}
		//fixes this so the widget won't be stuck and settings won't think we're active
		
		if (secure || active) {
			Log.v("restart recovery","corrected pref flags");
			editor.commit();
		}
		
				
		if (!boot) {
			stopSelf();//destroy the process because user doesn't have start at boot enabled
			return START_NOT_STICKY;//ensure it won't be restarted
		}
		
		Intent i = new Intent();
				
		if (!securemode) i.setClassName("i4nc4mp.myLockGuarded", "i4nc4mp.myLockGuarded.UserPresentService");
		//the service will wait for user to complete the first lockscreen - this protects phone from a restart security circumvention
		else i.setClassName("i4nc4mp.myLockGuarded","i4nc4mp.myLockGuarded.SecureLockService");
		//FIXME need to make the secure lock service put the service active pref.
		
		
		startService(i);
		
		stopSelf();
		
		return START_NOT_STICKY;//ensure it won't be restarted
	}
}
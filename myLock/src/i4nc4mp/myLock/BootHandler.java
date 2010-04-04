package i4nc4mp.myLock;

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
		
		boolean boot = settings.getBoolean("boot", false);//retrieve user's start at boot pref
		
		boolean secure = settings.getBoolean("securepaused", false);
		//this will be true if user had pattern on when they turned on myLock
		//we turn it back off when we turn pattern mode back on from next user disable myLock
		//if it is true here, it means the phone rebooted and the pattern was still suppressed
		
		boolean active = settings.getBoolean("serviceactive", false);
		//this should be false.
		//if it is true, that means that at some point the service was FC, uninstalled,
		//or this boot is a result of a battery pull or OS crash
		
		
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
		
		// Don't forget to commit your edits!!!
		if (secure || active) {
			Log.v("restart recovery","corrected pref flags");
			editor.commit();
		}
		
				
		if (!boot) {
			stopSelf();//destroy the process because user doesn't have start at boot enabled
			return START_NOT_STICKY;//ensure it won't be restarted
		}
		
		Intent i = new Intent();
				
		i.setClassName("i4nc4mp.myLock", "i4nc4mp.myLock.UserPresentService");		
		startService(i);
		
		stopSelf();
		
		return START_NOT_STICKY;//ensure it won't be restarted
	}
}
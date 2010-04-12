package i4nc4mp.myLock;

import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.IBinder;
import android.util.Log;

public class BootHandler extends Service {
	
	@Override
	public IBinder onBind(Intent intent) {
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
		
		boolean secure = settings.getBoolean("security", false);

		boolean active = settings.getBoolean("serviceactive", false);
		
		if (active) {
			editor.putBoolean("serviceactive", false);
		
			Log.v("restart recovery","corrected pref flags");
			editor.commit();
		}
		
		//security mode requires that we force security on at boot and launch user present
		//we basically treat boot like an idle timeout
		if (secure) {
			//ManageKeyguard.disableKeyguard(getApplicationContext());
			
			android.provider.Settings.System.putInt(getContentResolver(), 
                    android.provider.Settings.System.LOCK_PATTERN_ENABLED, 1);

			Intent i = new Intent();
			
			i.setClassName("i4nc4mp.myLock", "i4nc4mp.myLock.UserPresentService");		
			startService(i);
		}
		
		//I know I'm asking for complaints from non-security users who used start at boot. oh well
			
				
		stopSelf();
		
		return START_NOT_STICKY;//ensure it won't be restarted
	}
}
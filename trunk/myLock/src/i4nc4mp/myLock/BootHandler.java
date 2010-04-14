package i4nc4mp.myLock;

import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.IBinder;
import android.provider.Settings.SettingNotFoundException;
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
		
		boolean waitforuser = true;
		
		if (active) {
			editor.putBoolean("serviceactive", false);
		
			Log.v("restart recovery","corrected active flag");
			editor.commit();
		}
		
		Intent u = new Intent();
		u.setClassName("i4nc4mp.myLock", "i4nc4mp.myLock.UserPresentService");
		
		Intent i = new Intent();
		i.setClassName("i4nc4mp.myLock", "i4nc4mp.myLock.Toggler");
		i.putExtra("i4nc4mp.myLock.TargetState", true);
		
		//security mode requires that we force security on at boot and launch user present
		//so, only when system's security is off, we launch user present
		//it will handle if user unlocked the non-secure KG immediately at startup
		
		if (secure) {
			
			int patternsetting = 0;

	        try {
	                patternsetting = android.provider.Settings.System.getInt(getContentResolver(), android.provider.Settings.System.LOCK_PATTERN_ENABLED);
	        } catch (SettingNotFoundException e) {
	                // TODO Auto-generated catch block
	                e.printStackTrace();
	        }
	        
	        boolean s = (patternsetting == 1);
			
			if (!s) {
				android.provider.Settings.System.putInt(getContentResolver(), 
						android.provider.Settings.System.LOCK_PATTERN_ENABLED, 1);
			}
			else {
				ManageKeyguard.initialize(getApplicationContext());
				if (!ManageKeyguard.inKeyguardRestrictedInputMode()) waitforuser = false;
					
				//security was already on and phone has been unlocked
				//so no need to wait
				}
		}
		else waitforuser = false;
		
		//Don't wait if not aware of security or we already started system secure and got are already unlocked
		
		if (waitforuser) startService(u);//start user present
		else startService(i);//start toggler
						
		stopSelf();
		
		return START_NOT_STICKY;//ensure it won't be restarted
	}
}
package i4nc4mp.myLock.cupcake;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.IBinder;
import android.provider.Settings.SettingNotFoundException;
import android.util.Log;


//Forces itself into foreground mode, as a low mem death is likely during boot
//many processes are all doing init and demanding resources.

public class BootHandler extends Service {
	
	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}
	
	@Override
	public void onCreate() {
		super.onCreate();
		Log.d(getClass().getSimpleName(),"BootHandler - setting foreground");
		           
            setForeground(true);
            //not sure if this works anymore in the cupcake/donut.. 
            //it was made no-op in 2.0 requiring notification
            //but should work here to ensure we can bootstart
    
	}
	
	@Override
	public void onStart(Intent intent, int startId) {
		SharedPreferences settings = getSharedPreferences("myLock", 0);
		SharedPreferences.Editor editor = settings.edit();
		
		boolean secure = settings.getBoolean("security", false);

		boolean active = settings.getBoolean("enabled", false);
		
		boolean waitforuser = true;
		
		Intent u = new Intent();
		u.setClassName("i4nc4mp.myLock.cupcake", "i4nc4mp.myLock.cupcake.UserPresentService");
		
		Intent i = new Intent();
		i.setClassName("i4nc4mp.myLock.cupcake", "i4nc4mp.myLock.cupcake.Toggler");
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
				Log.v("secure boot","re-enabling pattern");
				android.provider.Settings.System.putInt(getContentResolver(), 
						android.provider.Settings.System.LOCK_PATTERN_ENABLED, 1);
			}
			else {
				Log.v("secure boot","system pattern already active");
				ManageKeyguard.initialize(getApplicationContext());
				if (!ManageKeyguard.inKeyguardRestrictedInputMode()) waitforuser = false;
					
				//security was already on and phone has been unlocked
				//so no need to wait
				}
		}
		else waitforuser = false;
		
		//Don't wait if not aware of security or we already started system secure and already unlocked
		
		Log.v("Startup result","wait for user flag is " + waitforuser);
		
		
		if (waitforuser) startService(u);//start user present
		else if (secure || active) startService(i);//start toggler
		//condition causes start at boot to only be considered when security was on
		//or we were enabled before the reboot
		setForeground(false);
		stopSelf();
		
		return;
	}
}
package i4nc4mp.myLock;

import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.IBinder;
import android.util.Log;
//functionality for clickable widget, migrated in from the settings activity toggle button
//run as service since widget UI wants to call this independent of an activity view
import android.widget.Toast;

public class Toggler extends Service {
	
	public boolean active = false;
	public boolean guard = false;
	
	@Override
	public IBinder onBind(Intent intent) {
		// we don't bind it, just call start from the widget
		return null;
	}
	
	@Override
	public void onCreate() {
		super.onCreate();
		Log.d(getClass().getSimpleName(),"Toggler created");
	}
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		Log.v("Toggler","start command running");
		
		getPrefs();
		
		//TryToggle();
		
		if (!active) {
			startService();
    		Toast.makeText(Toggler.this, "myLock is now enabled", Toast.LENGTH_SHORT).show();
		}
		else {
			stopService();
  			Toast.makeText(Toggler.this, "myLock is now disabled", Toast.LENGTH_SHORT).show();
		}
		//added to prevent android "restarting" this after it dies/is purged causing unexpected toggle
		stopSelf();//close so it won't be sitting idle in the running services window
		return START_NOT_STICKY;//ensure it won't be restarted by the OS, we only want explicit starts
	}
	
public void getPrefs() {
	SharedPreferences settings = getSharedPreferences("myLock", 0);
	active = settings.getBoolean("serviceactive", false);
	//guard = settings.getBoolean("slideGuard", false);
}
	
private void startService(){
		Intent i = new Intent();
		//if (guard) i.setClassName("i4nc4mp.myLock", "i4nc4mp.myLock.BasicGuardService"); else
		i.setClassName("i4nc4mp.myLock", "i4nc4mp.myLock.AutoDismiss");
		startService(i);
		Log.d( getClass().getSimpleName(), "startService()" );
}

private void stopService() {
		Intent i = new Intent();
		//if (guard) i.setClassName("i4nc4mp.myLock", "i4nc4mp.myLock.BasicGuardService"); else
		i.setClassName("i4nc4mp.myLock", "i4nc4mp.myLock.AutoDismiss");
		stopService(i);
		Log.d( getClass().getSimpleName(), "stopService()" );
}

}
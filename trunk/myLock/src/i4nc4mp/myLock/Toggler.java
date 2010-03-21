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
		
		//TryToggle();
		SharedPreferences settings = getSharedPreferences("myLock", 0);
		
		boolean active = settings.getBoolean("serviceactive", false);
		
		if (!active) {
			startService();
    		Toast.makeText(Toggler.this, "myLock is now enabled", Toast.LENGTH_SHORT).show();
		}
		else {
			stopService();
  			Toast.makeText(Toggler.this, "myLock is now disabled", Toast.LENGTH_SHORT).show();
		}
		
		return 1;
	}
	
private void startService(){
		Intent i = new Intent();
		i.setClassName("i4nc4mp.myLock", "i4nc4mp.myLock.BasicGuardService");
		startService(i);
		Log.d( getClass().getSimpleName(), "startService()" );
}

private void stopService() {
		Intent i = new Intent();
		i.setClassName("i4nc4mp.myLock", "i4nc4mp.myLock.BasicGuardService");
		stopService(i);
		Log.d( getClass().getSimpleName(), "stopService()" );
}

}
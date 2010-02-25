package i4nc4mp.myLockcomplete;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;
//functionality for clickable widget, migrated in from the settings activity toggle button
//run as service since widget UI wants to call this independent of an activity view

//TODO - interface with prefs to toggle the user's preferred mode
//right now it still interfaces with lite mode only
public class Toggler extends Service {
	
	public boolean triedstart = false;
	
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
		
		TryToggle();
		
		return 1;
	}
	
	
	
	
private void startService(){
		Intent i = new Intent();
		i.setClassName("i4nc4mp.myLockcomplete", "i4nc4mp.myLockcomplete.LiteLockMediator");
		startService(i);
		Log.d( getClass().getSimpleName(), "startService()" );
}

private void stopService() {
		Intent i = new Intent();
		i.setClassName("i4nc4mp.myLockcomplete", "i4nc4mp.myLockcomplete.LiteLockMediator");
		stopService(i);
		Log.d( getClass().getSimpleName(), "stopService()" );
}

private void TryToggle() {
	if (!triedstart) {
		startService();
		triedstart = true;
	}
	else {
		triedstart = false;
		stopService();
	}
}

}
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
		
		boolean target = intent.getBooleanExtra("i4nc4mp.myLock.TargetState", !active);
		//button doesn't supply, so default is opposite of known state
		//toggle which fixes wrong active state by stopping first
						
		if (target) {
			startService();
    		if (!active) Toast.makeText(Toggler.this, "myLock is now enabled", Toast.LENGTH_SHORT).show();
    		//the pref was obtained before start command. so if it was not active before, now is, send toast
    		//prevents Toast if it was already running and something called start to it again
		}
		else {
			if (!active) {
				stopSelf();
				return START_NOT_STICKY;
				//we do nothing if already showing not active
			}
			else {
			//this means a real stop attempt or incorrect active flag but no mediator running
			//either way the result is stopped
			stopService();
			/*
			getPrefs();
			
			if (active) {
				//failed due to false-active needs to correct pref
				SharedPreferences set = getSharedPreferences("myLock", 0);
				SharedPreferences.Editor editor = set.edit();
	            editor.putBoolean("serviceactive", false);

	            // Don't forget to commit your edits!!!
	            editor.commit();
			}
			else 
			//this fails because the setting we are trying to check hasnt been updated quite yet
			*/
			Toast.makeText(Toggler.this, "myLock is now disabled", Toast.LENGTH_SHORT).show();
			//will always be sent even if was already stopped regardless of active flag
			//fix is to register a listener but the timing is more trouble than it is worth
			}
			
		}
		
		//added to prevent android "restarting" this after it dies/is purged causing unexpected toggle
		stopSelf();//close so it won't be sitting idle in the running services window
		return START_NOT_STICKY;//ensure it won't be restarted by the OS, we only want explicit starts
	}
	
public void getPrefs() {
	SharedPreferences settings = getSharedPreferences("myLock", 0);
	active = settings.getBoolean("serviceactive", false);
	guard = settings.getBoolean("wallpaper", false);
}
	
private void startService(){
		Intent i = new Intent();
		if (guard) i.setClassName("i4nc4mp.myLock", "i4nc4mp.myLock.BasicGuardService"); else
		i.setClassName("i4nc4mp.myLock", "i4nc4mp.myLock.AutoDismiss");
		startService(i);
		Log.d( getClass().getSimpleName(), "startService()" );
}

private void stopService() {
		Intent i = new Intent();
		if (guard) i.setClassName("i4nc4mp.myLock", "i4nc4mp.myLock.BasicGuardService"); else
		i.setClassName("i4nc4mp.myLock", "i4nc4mp.myLock.AutoDismiss");
		stopService(i);
		Log.d( getClass().getSimpleName(), "stopService()" );
}

}
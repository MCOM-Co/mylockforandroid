package i4nc4mp.myLock;


import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

public class Toggler extends Service {
	
	private boolean target;
	private boolean active;
	
	private boolean guard = false;
	
	@Override
	public IBinder onBind(Intent intent) {
		// we don't bind it, just call start from the widget
		return null;
	}
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
Log.v("Toggler","Starting");
		
		getPrefs();
		
		active = ManageMediator.bind(getApplicationContext());
		
		target = intent.getBooleanExtra("i4nc4mp.myLock.TargetState", !active);
		
		Log.v("toggling","target is " + target + " and current state is " + active);
		
		//start if we've been told to start and did not already exist				
		if (target && !active) {
			startService();
			updateEnablePref(true);
    		Toast.makeText(Toggler.this, "myLock is now enabled", Toast.LENGTH_SHORT).show();
    		
		}//stop if we've been told to stop and did already exist
		else if (active && !target) {
				
				stopService();
				Toast.makeText(Toggler.this, "myLock is now disabled", Toast.LENGTH_SHORT).show();
				
				updateEnablePref(false);
		}//log the request - locale condition may send a desired state that already exists
		else Log.v("toggler","unhandled outcome - target was not a change");
		
		//added to prevent android "restarting" this after it dies/is purged causing unexpected toggle
		stopSelf();//close so it won't be sitting idle in the running services window
		return START_NOT_STICKY;//ensure it won't be restarted by the OS, we only want explicit starts
	}
	
public void getPrefs() {
	SharedPreferences settings = getSharedPreferences("myLock", 0);
	guard = settings.getBoolean("wallpaper", false);
}

private void updateEnablePref(boolean on) {
	SharedPreferences set = getSharedPreferences("myLock", 0);
	SharedPreferences.Editor editor = set.edit();
    editor.putBoolean("enabled", on);

    // Don't forget to commit your edits!!!
    editor.commit();
}
	
private void startService(){
		Intent i = new Intent();
		if (guard) i.setClassName("i4nc4mp.myLock", "i4nc4mp.myLock.BasicGuardService");
		else i.setClassName("i4nc4mp.myLock", "i4nc4mp.myLock.AutoDismiss");
		startService(i);
		ManageMediator.bind(getApplicationContext());//we always hold the binding while officially active
		Log.d( getClass().getSimpleName(), "startService()" );
}

private void stopService() {
		ManageMediator.release(getApplicationContext());
		Intent i = new Intent();
		if (guard) i.setClassName("i4nc4mp.myLock", "i4nc4mp.myLock.BasicGuardService");
		else i.setClassName("i4nc4mp.myLock", "i4nc4mp.myLock.AutoDismiss");
		stopService(i);
		Log.d( getClass().getSimpleName(), "stopService()" );
}

}
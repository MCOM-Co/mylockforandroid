package i4nc4mp.myLock.cupcake;

import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;
import android.widget.Toast;

//Toggler works with user's enabled pref and the ManageMediator to correctly get and set service
//When we are running, we will have a binding in the static reference
//when service dies, it kills the connection
//when we close the service on purpose, we just kill the link to the service but keep "Connection"
//it is a very complicated interchange.. binding is obtuse

//the best rule to remember is - if you start the service, bind to it
//when you stop it, kill the binding first.
//this way we can reliably know that when we can't find the binding, the service doesn't exist

//now we can have the widget be aware of whether the service is running
//we could even make the disconnect event in binding force a restart if user's enabled pref is on


public class Toggler extends Service {
	
	private boolean target;
	private boolean active;
	
	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}
	
	@Override
	public void onStart(Intent intent, int startId) {
		Log.v("Toggler","Starting");
		
		//ManageMediator m = new ManageMediator();
		//active = m.bind(getApplicationContext());
		//m = null;
		
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
		return;
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
		
		i.setClassName("i4nc4mp.myLock.cupcake", "i4nc4mp.myLock.cupcake.AutoDismiss");
		startService(i);
		ManageMediator.bind(getApplicationContext());
}

private void stopService() {
		ManageMediator.release(getApplicationContext());
		Intent i = new Intent();
		i.setClassName("i4nc4mp.myLock.cupcake", "i4nc4mp.myLock.cupcake.AutoDismiss");
		stopService(i);
}

}
package i4nc4mp.myLock;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

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
		i.setClassName("i4nc4mp.myLock", "i4nc4mp.myLock.LiteLockMediator");
		startService(i);
		Log.d( getClass().getSimpleName(), "startService()" );
}

private void stopService() {
		Intent i = new Intent();
		i.setClassName("i4nc4mp.myLock", "i4nc4mp.myLock.LiteLockMediator");
		stopService(i);
		Log.d( getClass().getSimpleName(), "stopService()" );
}

private void TryToggle() {
	if (!triedstart) {
		startService();
		triedstart = true;
	}
	//first attempt to start. the service does nothing if already initialized
	else {
		triedstart = false;
		stopService();
	}//the user has clicked a 2nd time. confirming they do want the lockscreen re-enabled
}

}
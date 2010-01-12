package i4nc4mp.myLocklite;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

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
			
			//IntentFilter togglefilter = new IntentFilter ("i4nc4mp.myLock.intent.action.TOGGLE_LOCKSCREEN");
			//registerReceiver(trytoggle, togglefilter);
		}
		
		@Override
		public int onStartCommand(Intent intent, int flags, int startId) {
			Log.v("Toggler","start command running");
			
			TryToggle();
			
			return 1;
		}
		
		
		
		
	private void startService(){
			Intent i = new Intent();
			i.setClassName("i4nc4mp.myLocklite", "i4nc4mp.myLocklite.LockMediatorService");
			startService(i);
			Log.d( getClass().getSimpleName(), "startService()" );
	}

	private void stopService() {
			Intent i = new Intent();
			i.setClassName("i4nc4mp.myLocklite", "i4nc4mp.myLocklite.LockMediatorService");
			stopService(i);
			Log.d( getClass().getSimpleName(), "stopService()" );
	}

	private void TryToggle() {
		if (!triedstart) {
			startService();
			//Toast.makeText(Toggler.this, "intializing myLock lite", Toast.LENGTH_SHORT).show();
			//all toasting moved to Mediator
			triedstart = true;
		}
		//first attempt to start. the service does nothing if already initialized
		else {
			triedstart = false;
			stopService();
		}//the user has clicked a 2nd time. confirming they do want the lockscreen re-enabled
	}
	
	}
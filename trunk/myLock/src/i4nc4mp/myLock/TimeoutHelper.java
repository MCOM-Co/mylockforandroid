package i4nc4mp.myLock;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

//timeout helper is a service which updates the myLock settings
//it will be started by the IdleSetup activity from the toggle addon
//TODO add declaration to manifest
public class TimeoutHelper extends Service {

	@Override
	public IBinder onBind(Intent arg0) {
		// TODO Auto-generated method stub
		return null;
	}
	
}
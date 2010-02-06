package i4nc4mp.myLock;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.os.PowerManager;
import android.util.Log;
//deprecating this class and using managewakelock commands in the main mediator
//this will ensure the wakelock starts at Lock Activity exit, and stops when the user initiated sleep causes lock activity to come back
public class StayAwakeService extends Service {
	//users might want to have stay awake running but not a lock mediator
	//The settings activity wakelocks don't work unless a mediator service is running
	
	//Pulled the code straight from the full lock commands done in the managewakelock class
	//it's not complicated at all to get wakelocks, this service ensures we can actually keep one at all times
	
	//parts of other processes such as the lockactivities sometimes need wakelocks to avoid bugs
	//so we need our real stay awake to run independently
	
	
	private static PowerManager.WakeLock myWakeLock = null;
	
	@Override
	public IBinder onBind(Intent arg0) {
		// TODO Auto-generated method stub
		return null;
	}
	
	//service has no reason to run except to keep this wakelock
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		super.onStartCommand(intent, flags, startId);
		
		if (myWakeLock == null) acquireFull(getApplicationContext());
		else {
			releaseFull();
			stopSelf();
		}
		return 1;
	}
	
	
	public static synchronized void acquireFull(Context context) {
	    // setPM(context);
	    PowerManager myPM = (PowerManager) context.getSystemService(Context.POWER_SERVICE);

	    if (myWakeLock != null) {
	      
	      Log.v("acquire","**Wakelock already held");
	      return;
	    }
	    int flags;

	    flags = PowerManager.SCREEN_DIM_WAKE_LOCK;
	    

	    myWakeLock = myPM.newWakeLock(flags, "acquire");
	    Log.v("acquire","**Wakelock acquired");
	    myWakeLock.setReferenceCounted(false);
	    myWakeLock.acquire();

	    
	  }
	
	public static synchronized void releaseFull() {
	    if (myWakeLock != null) {
	      Log.v("release","**Wakelock released");
	      myWakeLock.release();
	      myWakeLock = null;
	    }
	  }
	
}
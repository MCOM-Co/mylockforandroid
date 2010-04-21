package i4nc4mp.myLock;


import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;
import android.widget.TextView;

//static helper - all we need to do is try bind with mediator
//used by toggler & main settings to see if mediator is running
//this works better than trying to keep track of serviceactive pref flag

public class ManageMediator {
	private static RemoteServiceConnection conn = null;
	private static IsActive mediator; 
	
	static class RemoteServiceConnection implements ServiceConnection {
        public synchronized void onServiceConnected(ComponentName className, 
			IBinder boundService ) {
          mediator = IsActive.Stub.asInterface((IBinder)boundService);
          Log.v("service connected","bind to existent service");
          
        }

        public synchronized void onServiceDisconnected(ComponentName className) {
          mediator = null;
          Log.v("service disconnected","service death");
        }
    };
	
	public static synchronized boolean bind(Context mCon) {
		boolean exists;
		
		if(conn == null) {
			Log.v("bind attempt","initializing connection");
			conn = new RemoteServiceConnection();
		}
		//the connection object continues to exist
		//service death means that the mediator will be nulled out
		if (mediator == null) {
			//try to find the mediator
			SharedPreferences settings = mCon.getSharedPreferences("myLock", 0);
			boolean guard = settings.getBoolean("wallpaper", false);
			
			Intent i = new Intent();
			
			if (guard) i.setClassName("i4nc4mp.myLock", "i4nc4mp.myLock.BasicGuardService");
			else i.setClassName("i4nc4mp.myLock", "i4nc4mp.myLock.AutoDismiss");

			
			mCon.bindService(i, conn, 0);
		}
		
		exists = (mediator !=null); 
		//the bind is forced by toggler immediately after starting service
		//we will never gain the bind to an active mediator other than at that point
		
		//however, this will still be null if no mediator exists at all.
		
			/*try {
			exists = mediator.Exists();
		} catch (RemoteException re) {
			Log.e("failed to check existence" , "RemoteException" );
			exists = false;
			}*/
			//don't try to check the method, having the reference is sufficient
		
		Log.v("bind result","exists: " + exists);
		return exists;
	}

	//called when we deliberately stop the service
	public static synchronized void release(Context mCon) {
		if(conn != null) {
			mCon.unbindService(conn);
			conn = null;
			mediator = null;
		} 
	}
	
	
}
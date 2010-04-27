package i4nc4mp.myLock;



import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;
import android.widget.Toast;


//static helper to give us a reliable notificaton if service dies and let us keep track of state
//also houses method to call through to toggler service to handle a state change request
//also can be used to call specific state change, that is used by the pref screen

public class ManageMediator {
	private static RemoteServiceConnection conn = null;
	private static IsActive mediator; 
	private static Context c;
	
	static class RemoteServiceConnection implements ServiceConnection {
        public void onServiceConnected(ComponentName className, 
			IBinder boundService ) {
          mediator = IsActive.Stub.asInterface((IBinder)boundService);
          Log.v("service connected","bind to existent service");
          //this one happens when we start the bind
          //if (c!=null) ToggleWidget.makeView(c, true);
        }

        public void onServiceDisconnected(ComponentName className) {
          mediator = null;
          Log.v("service disconnected","service death");
          
          if (c==null) return;
          
          Toast.makeText(c, "unexpected myLock stop", Toast.LENGTH_LONG).show();
          
          //updateEnablePref(false, c);
          
          ToggleWidget.makeView(c, false);
          //this one only comes through when something force kills the service but not entire process
        }
    };
	
	public static synchronized boolean bind(Context mCon) {
		boolean success;
		
		//calls for the bind
		//will just return true if we already had the bind
		
		if (c==null) c=mCon;//store our context ref so we can use it if service dies
		
		if(conn == null) {
			Log.v("bind attempt","initializing connection object");
			conn = new RemoteServiceConnection();
		}
		
		if (mediator == null) {
			//try to find the mediator
			SharedPreferences settings = mCon.getSharedPreferences("myLock", 0);
			boolean guard = settings.getBoolean("wallpaper", false);
			
			Intent i = new Intent();
			
			if (guard) i.setClassName("i4nc4mp.myLock", "i4nc4mp.myLock.BasicGuardService");
			else i.setClassName("i4nc4mp.myLock", "i4nc4mp.myLock.AutoDismiss");

			
			success = mCon.bindService(i, conn, 0);
		}
		else {
			Log.v("bind result","binding already held, returning true");
			return true;
		}
		
		//Log.v("bind attempt","attempted to get mediator, and the request returned " + success);
		//if (success) return serviceActive(mCon);
		//if we created the bind, double check by calling through to the mediator
		//success is always true, even when we didn't make a bind. I don't know why
		
		//on first create, we always get false here
		//around 50 MS later the bind connect goes off
		
		
		//Because of this, only return true if binding was already held
		
		return false;

	}

	//called when we deliberately stop the service - this way the bind is fully zeroed out
	public static synchronized void release(Context mCon) {
		if(conn != null) {
			mCon.unbindService(conn);
			conn = null;
			mediator = null;
		} 
	}
	
	public static synchronized boolean serviceActive(Context mCon) {
		boolean exists = false;
		//for extra redundancy, call through to method in the mediator
		
		if (mediator==null) {
			Log.e("verify bind","failed because we don't have mediator stub");
		}

		else {
			try {
			exists = mediator.Exists();
		} catch (RemoteException re) {
			Log.e("unknown failure" , "had mediator stub but couldn't check active" );
			}
		}
		
		Log.v("verify bind","result is " + exists);
		return exists;
	}
	
	public static synchronized void startService(Context mCon){
		SharedPreferences settings = mCon.getSharedPreferences("myLock", 0);
		boolean guard = settings.getBoolean("wallpaper", false);
		
		Intent i = new Intent();
		if (guard) i.setClassName("i4nc4mp.myLock", "i4nc4mp.myLock.BasicGuardService");
		else i.setClassName("i4nc4mp.myLock", "i4nc4mp.myLock.AutoDismiss");
		mCon.startService(i);
		
		ToggleWidget.makeView(mCon, true);
		Log.d( "manage mediator", "start call" );
}

	public static synchronized void stopService(Context mCon) {
		SharedPreferences settings = mCon.getSharedPreferences("myLock", 0);
		boolean guard = settings.getBoolean("wallpaper", false);
		
		release(mCon);
		//kill the binding
		
		Intent i = new Intent();
		if (guard) i.setClassName("i4nc4mp.myLock", "i4nc4mp.myLock.BasicGuardService");
		else i.setClassName("i4nc4mp.myLock", "i4nc4mp.myLock.AutoDismiss");
		mCon.stopService(i);
		
		ToggleWidget.makeView(mCon, false);
		Log.d( "manage mediator", "stop call" );
}
	
	public static synchronized void invokeToggler(Context mCon, boolean on) {
		Intent i = new Intent();
		
		i.setClassName("i4nc4mp.myLock", "i4nc4mp.myLock.Toggler");
		i.putExtra("i4nc4mp.myLock.TargetState", on);
		mCon.startService(i);
	}
	
	//only used for external toggle requests
	//toggler service handles requesting this method
	public static synchronized void updateEnablePref(boolean on, Context mCon) {
		SharedPreferences set = mCon.getSharedPreferences("myLock", 0);
		SharedPreferences.Editor editor = set.edit();
	    editor.putBoolean("enabled", on);

	    // Don't forget to commit your edits!!!
	    editor.commit();
	    
	}
	
}
package i4nc4mp.myLock;



import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.IBinder;
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
        public synchronized void onServiceConnected(ComponentName className, 
			IBinder boundService ) {
          mediator = IsActive.Stub.asInterface((IBinder)boundService);
          Log.v("service connected","bind to existent service");
          //always occurs immediately after service is started. would be a safe point to send widget update
          
        }

        public synchronized void onServiceDisconnected(ComponentName className) {
          mediator = null;
          Log.v("service disconnected","service death");
          
          if (c==null) return;
          
          Toast.makeText(c, "unexpected myLock stop", Toast.LENGTH_LONG).show();
          
          //updateEnablePref(false, c);
          
          ToggleWidget.makeView(c, false);
        }
    };
	
	public static synchronized boolean bind(Context mCon) {
		boolean exists;
		
		//What we do here is attempt to bind, if we don't appear to have it already
		//Always succeeds when we call it right after toggling on
		
		
		//Future calls merely return the existence of mediator as true
		//When called and there is no mediator, there is no bind and it can't be made
		//we don't auto create.
		
		if (c==null) c=mCon;//store our context ref so we can use it if service dies
		
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

	//called when we deliberately stop the service - this way the bind is fully zeroed out
	public static synchronized void release(Context mCon) {
		if(conn != null) {
			mCon.unbindService(conn);
			conn = null;
			mediator = null;
		} 
	}
	
	public static synchronized void invokeToggler(Context mCon, boolean on) {
		Intent i = new Intent();
		
		i.setClassName("i4nc4mp.myLock", "i4nc4mp.myLock.Toggler");
		i.putExtra("i4nc4mp.myLock.TargetState", on);
		mCon.startService(i);
	}
	
	public static synchronized void startService(Context mCon){
		SharedPreferences settings = mCon.getSharedPreferences("myLock", 0);
		boolean guard = settings.getBoolean("wallpaper", false);
		
		Intent i = new Intent();
		if (guard) i.setClassName("i4nc4mp.myLock", "i4nc4mp.myLock.BasicGuardService");
		else i.setClassName("i4nc4mp.myLock", "i4nc4mp.myLock.AutoDismiss");
		mCon.startService(i);
		
		bind(mCon);//we always hold the binding while officially active
		
		ToggleWidget.makeView(mCon, true);
		Log.d( "manage mediator", "start call" );
}

	public static synchronized void stopService(Context mCon) {
		SharedPreferences settings = mCon.getSharedPreferences("myLock", 0);
		boolean guard = settings.getBoolean("wallpaper", false);
		
		release(mCon);
		Intent i = new Intent();
		if (guard) i.setClassName("i4nc4mp.myLock", "i4nc4mp.myLock.BasicGuardService");
		else i.setClassName("i4nc4mp.myLock", "i4nc4mp.myLock.AutoDismiss");
		mCon.stopService(i);
		
		ToggleWidget.makeView(mCon, false);
		Log.d( "manage mediator", "stop call" );
}
	//only used for external toggle requests
	//toggler service handles requesting this methods
	public static synchronized void updateEnablePref(boolean on, Context mCon) {
		SharedPreferences set = mCon.getSharedPreferences("myLock", 0);
		SharedPreferences.Editor editor = set.edit();
	    editor.putBoolean("enabled", on);

	    // Don't forget to commit your edits!!!
	    editor.commit();
	    
	}
	
}
package i4nc4mp.myLock.phone;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.telephony.TelephonyManager;

//here is a receiver that will put the Call Prompt up just when calls start ringing
//this way we don't have to complicate the lock skip mediation with this process
public class PhoneReceiver extends BroadcastReceiver {
    
	//private Handler serviceHandler;
    //private Task myTask = new Task();
    
    //private Context mCon;
	
	@Override
    public void onReceive(Context context, Intent intent) {
		//mCon = context;

            SharedPreferences p = context.getSharedPreferences("myLockphone",0);
            
            boolean prompt = p.getBoolean("callPrompt", false);
            boolean lock = p.getBoolean("touchLock", false);
            // Check phone state
            String state = intent.getStringExtra(TelephonyManager.EXTRA_STATE);
            //String number = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER);

            if (state.equals(TelephonyManager.EXTRA_STATE_RINGING) && prompt) {
            	Class g = DummyPrompt.class;
    			Intent dummy = new Intent(context, g);
            	
            	dummy.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
            			| Intent.FLAG_ACTIVITY_NO_USER_ACTION
            			| Intent.FLAG_ACTIVITY_NO_HISTORY);
            	            	
            	context.startActivity(dummy);
            	//Dummy will get more precise handoff to phone screen
            	
            	//serviceHandler = new Handler();
            	//serviceHandler.postDelayed(myTask, 2000L);                    
            }
            
            //Screen mediator is a service. System doesn't allow screen receivers via Intent Filter
            //We have to create a manual registration for the broadcast within the service
            if (lock) {            
            	if (state.equals(TelephonyManager.EXTRA_STATE_OFFHOOK)) {
            	//users requesting launch the touch guard on call start
            	
            		Intent m = new Intent(context, ScreenMediator.class);
            		context.startService(m);
                }
            	else if (state.equals(TelephonyManager.EXTRA_STATE_IDLE)) {
            	           	
            		Intent m = new Intent(context, ScreenMediator.class);
            		context.stopService(m);
            	}
            }
    }
	
	/*
	class Task implements Runnable {
    	public void run() {  
    		CallPrompt.launch(mCon);
        	
        	serviceHandler.removeCallbacks(myTask);
        	serviceHandler = null;
    	}};
	*/
	
}
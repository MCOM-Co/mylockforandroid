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
    
	private Handler serviceHandler;
    private Task myTask = new Task();
    
    private Context mCon;
	
	@Override
    public void onReceive(Context context, Intent intent) {
		mCon = context;

            SharedPreferences p = context.getSharedPreferences("myLockphone",0);
            
            boolean prompt = p.getBoolean("callPrompt", false);
            boolean lock = p.getBoolean("touchLock", false);
            // Check phone state
            String state = intent.getStringExtra(TelephonyManager.EXTRA_STATE);
            //String number = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER);

            if (state.equals(TelephonyManager.EXTRA_STATE_RINGING) && prompt) {
            	serviceHandler = new Handler();
            	serviceHandler.postDelayed(myTask, 2000L);                    
            }
            
            if (lock) {            
            	if (state.equals(TelephonyManager.EXTRA_STATE_OFFHOOK)) {
            	//SharedPreferences.Editor e = p.edit();
            	//e.putBoolean("callActive", true);
            	
              	Intent m = new Intent(context, ScreenMediator.class);
            	context.startService(m);
                }
            	else if (state.equals(TelephonyManager.EXTRA_STATE_IDLE)) {
            	//SharedPreferences.Editor e = p.edit();
            	//e.putBoolean("callActive", false);
            	           	
                Intent m = new Intent(context, ScreenMediator.class);
                context.stopService(m);
            	}
            }
    }
	
	class Task implements Runnable {
    	public void run() {  
    		Intent prompt = new Intent();
        	
        	prompt.setClassName("i4nc4mp.myLock.phone","i4nc4mp.myLock.phone.CallPrompt");
        	prompt.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
        			| Intent.FLAG_ACTIVITY_NO_USER_ACTION);
        	//otherwise it would immediately stop vibration & sound
        	
        	mCon.startActivity(prompt);
        	
        	serviceHandler.removeCallbacks(myTask);
        	serviceHandler = null;
    	}};
	
	
}
package i4nc4mp.myLock;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Handler;
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

            SharedPreferences p = context.getSharedPreferences("myLock", 0);
            
            boolean on = true;//p.getBoolean("CallPrompt", false)

            // Check phone state
            String phone_state = intent.getStringExtra(TelephonyManager.EXTRA_STATE);
            //String number = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER);

            if (phone_state.equals(TelephonyManager.EXTRA_STATE_RINGING) && on) {
            	serviceHandler = new Handler();
            	serviceHandler.postDelayed(myTask, 1000L);                    
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
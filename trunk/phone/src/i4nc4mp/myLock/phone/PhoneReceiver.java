package i4nc4mp.myLock.phone;

import i4nc4mp.myLock.phone.PromptMediator.Dummy;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.telephony.TelephonyManager;

public class PhoneReceiver extends BroadcastReceiver {
	
	@Override
    public void onReceive(Context context, Intent intent) {
	Context mCon = context;
		
		SharedPreferences p = context.getSharedPreferences("myLockphone",0);
            
            boolean prompt = p.getBoolean("callPrompt", false);
            boolean lock = p.getBoolean("touchLock", false);

            
            String state = intent.getStringExtra(TelephonyManager.EXTRA_STATE);
            //String number = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER);

            if (state.equals(TelephonyManager.EXTRA_STATE_RINGING) && prompt) {
                	Intent pm = new Intent(context, PromptMediator.class);
                	context.startService(pm);
                	//stopping is handled after 2 seconds in the service itself
                	
                	Intent dummy = new Intent(mCon, Dummy.class);
                	
                	dummy.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                			| Intent.FLAG_ACTIVITY_NO_USER_ACTION
                			| Intent.FLAG_ACTIVITY_NO_HISTORY);
                	            	
                	mCon.startActivity(dummy);
                	//more precise handoff to phone screen
                	//as long as it starts before phone
                	//I'm only seeing this happen when the call comes in from sleep, on emulator
                }
                /*else if (state.equals(TelephonyManager.EXTRA_STATE_OFFHOOK) || state.equals(TelephonyManager.EXTRA_STATE_IDLE)) {
                	Intent pm = new Intent(context, PhoneMediator.class);
                	context.stopService(pm);
                }*/
            
            //System doesn't allow screen broadcast receivers via manifest Intent Filter
            //We will register for the screen off broadcast inside the mediator service
            if (lock) {            
            	if (state.equals(TelephonyManager.EXTRA_STATE_OFFHOOK)) {            	
            		Intent m = new Intent(context, ScreenMediator.class);
            		context.startService(m);
                }
            	else if (state.equals(TelephonyManager.EXTRA_STATE_IDLE)) {
               		Intent m = new Intent(context, ScreenMediator.class);
            		context.stopService(m);
            	}
            }
    }	
}
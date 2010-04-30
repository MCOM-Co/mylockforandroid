package i4nc4mp.myLock.phone;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.telephony.TelephonyManager;

public class PhoneReceiver extends BroadcastReceiver {
	
	@Override
    public void onReceive(Context context, Intent intent) {
		
		SharedPreferences p = context.getSharedPreferences("myLockphone",0);
            
            boolean prompt = p.getBoolean("callPrompt", false);
            boolean lock = p.getBoolean("touchLock", false);

            
            String state = intent.getStringExtra(TelephonyManager.EXTRA_STATE);
            //String number = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER);

            if (state.equals(TelephonyManager.EXTRA_STATE_RINGING) && prompt) {
                	//Intent pm = new Intent(context, PromptMediator.class);
                	//context.startService(pm);
                
            	//in the real device operating environment, immediately launching the prompt should work
            	//what we need to do is have the prompt relaunch itself if it gets stopped
            	//it would be catching the case it starts faster than the phone all on its own
            	
                	CallPrompt.launch(context);
                	
                	//the only time we manage to start visible phase before phone
                	//is call coming from sleep - prompt handles this itself in onStop
                }
                /*else if (state.equals(TelephonyManager.EXTRA_STATE_OFFHOOK) || state.equals(TelephonyManager.EXTRA_STATE_IDLE)) {
                	Intent pm = new Intent(context, PhoneMediator.class);
                	context.stopService(pm);
                }*/
            
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
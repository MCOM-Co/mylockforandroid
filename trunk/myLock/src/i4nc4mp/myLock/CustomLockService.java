package i4nc4mp.myLock;

import android.content.Context;
import android.content.Intent;
import android.provider.Settings.SettingNotFoundException;


public class CustomLockService extends MediatorService {
	
	public boolean unlocked = true;
	//when true screen off will start the lockscreen.
	
	public int patternsetting = 0;
	//we'll see if the user has pattern enabled when we startup
	//so we can disable it and then restore when we finish
	
	@Override
	public void onDestroy() {
		super.onDestroy();
		
		if (patternsetting == 1) {
			android.provider.Settings.System.putInt(getContentResolver(), 
    			android.provider.Settings.System.LOCK_PATTERN_ENABLED, 1);
    	//re-enable pattern lock if applicable
	}
}

	@Override
	public void onRestartCommand() {
		//user unlock in the lock activity finishes that and sends a start command to notify mediator
		unlocked = true;//ensure next screen off will StartLock
	}
	
	@Override
	public void onFirstStart() {//we have to toggle pattern lock off to use a custom lockscreen
		try {
			patternsetting = android.provider.Settings.System.getInt(getContentResolver(), android.provider.Settings.System.LOCK_PATTERN_ENABLED);
		} catch (SettingNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		if (patternsetting == 1) {    	
    	android.provider.Settings.System.putInt(getContentResolver(), 
    			android.provider.Settings.System.LOCK_PATTERN_ENABLED, 0); 
		}
	}
	
	@Override
	public void onScreenSleep() {
		//when sleep after an unlock, start the lockscreen again
		if (receivingcall || placingcall) return;//don't lock during a call
		if (unlocked) {
        	unlocked = false;
        	StartLock(getApplicationContext());
		}
		
		return;//prevents unresponsive broadcast error
	}
	
	private void StartLock(Context context) {

		Intent closeDialogs = new Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS);
		        context.sendBroadcast(closeDialogs);
		        

		        Class w = Lockscreen.class;
		       

		/* launch UI, explicitly stating that this is not due to user action
		         * so that the current app's notification management is not disturbed */
		        Intent lockscreen = new Intent(context, w);
		        
		        lockscreen.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
		                //| Intent.FLAG_ACTIVITY_NO_USER_ACTION
		                | Intent.FLAG_ACTIVITY_NO_ANIMATION);
		        context.startActivity(lockscreen);
		}
	
	//TODO add handling of different call event reactions
	//essentially anytime a call ends it seems our next screen off is not creating the lockscreen again
	//we have a few cases
	//1- call ends while screen was off. user turns on screen to find the regular lockscreen
	//2- call ends while screen is one, user sees the lockscreen. if sleep, next wakeup still has lockscreen
	//3- incoming call is missed or ignored results in seeing the lockscreen also
}
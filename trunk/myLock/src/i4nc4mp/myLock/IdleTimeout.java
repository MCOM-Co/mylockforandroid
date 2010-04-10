package i4nc4mp.myLock;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class IdleTimeout extends BroadcastReceiver {
	//for these guarded modes we do not have to do anything except stop the service
	//if the guard exists it will merely wakeup and call the lockscreen cancel
	//which will not work because pattern mode had been restored when service exits
	
	//advanced mode is the only one where its tricky
	//and requires closing the lock activity & re-enable keyguard call
	
	@Override
	public void onReceive(Context context, Intent intent) {
		// TODO Auto-generated method stub
		if("i4nc4mp.myLock.IDLE_TIMEOUT".equals(intent.getAction())) {
			//this is the action we are registered for via manifest declaration
			
			//close the current service via Toggler with target false
			Intent i = new Intent();
			i.setClassName("i4nc4mp.myLock", "i4nc4mp.myLock.Toggler");
			i.putExtra("i4nc4mp.myLock.TargetState", false);
			
			//start up the user present to wait for next unlock
			Intent u = new Intent();
		    u.setClassName("i4nc4mp.myLock", "i4nc4mp.myLock.UserPresentService");
		    context.startService(u);		
		}
	}
}
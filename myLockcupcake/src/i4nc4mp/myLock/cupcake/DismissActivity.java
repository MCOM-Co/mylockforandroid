package i4nc4mp.myLock.cupcake;

import i4nc4mp.myLock.cupcake.ManageKeyguard.LaunchOnKeyguardExit;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;

//PORTED - Android 2.0 Has an activity that already FULLY clears the lockscreen, avoiding the secure exit logic
//This port works exactly the same way, just is a bit slower
//When I run it on 2.1 device you see all the lockscreen visuals flashing twice just like alpha 2c
//I'm suspecting it will look smoother on a real 1.5/1.6 device. The emulator has pretty smooth response


public class DismissActivity extends Activity {
      private boolean done = true;
      private boolean kg;
      
      @Override
      protected void onCreate(Bundle icicle) {
    	  super.onCreate(icicle);

    	  Context mCon = getApplicationContext();
    	  
          ManageKeyguard.initialize(mCon);
          kg = ManageKeyguard.inKeyguardRestrictedInputMode();
          
          //safety check. tries to avoid a conflict with another popup
          if (!kg) {
        	  finish();
        	  return;
          }
          else {
        	  done = false;
    	  
          ManageKeyguard.disableKeyguard(mCon);
    	  
          requestWindowFeature(android.view.Window.FEATURE_NO_TITLE);
          getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
      
          Log.v("dismiss","creating dismiss window");

          setContentView(R.layout.dismisslayout);
      
      IntentFilter userunlock = new IntentFilter (Intent.ACTION_USER_PRESENT);
      registerReceiver(unlockdone, userunlock);
          }

}      
  
  BroadcastReceiver unlockdone = new BroadcastReceiver() {
	    
	    public static final String present = "android.intent.action.USER_PRESENT";

	    @Override
	    public void onReceive(Context context, Intent intent) {
	    	if (!intent.getAction().equals(present)) return;
	    	if (!done) {
	    		Log.v("dismiss user present","sending to back");
	    		done = true;
	    	   	moveTaskToBack(true);
	    	}
	    }
  };
  
  @Override
  public void onWindowFocusChanged(boolean hasFocus) {
	  ManageKeyguard.exitKeyguardSecurely(new LaunchOnKeyguardExit() {
	        public void LaunchOnKeyguardExitSuccess() {
	           Log.v("secure exit success", "gained focus & cleared KG");
	           
	        	}
			});
  }
  
  @Override
  public void onDestroy() {
      super.onDestroy();
                  
      if (kg) {
    	  unregisterReceiver(unlockdone);
    	  if (done) ManageKeyguard.reenableKeyguard();
      }
      Log.v("destroy_dismiss","Destroying");
      
  }
}
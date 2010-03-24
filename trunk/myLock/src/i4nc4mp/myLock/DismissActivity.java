package i4nc4mp.myLock;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.PowerManager;
import android.os.SystemClock;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;

//One shot dismiss_keyguard activity. functions by launching, then finishing 50 ms later
//we use it so we don't have to use the pre-2.0 dismiss & secure exit commands (which are really strict)


//after the start dismiss - essentially we set a pendingDismiss flag, then when resume happens during the flag
//we then call finish - this seems to create a visible lag
//it seems this could be fixed by doing a moveTaskToBack on the guard activity once dismiss actually gains focus

public class DismissActivity extends Activity {
      
      protected void onCreate(Bundle icicle) {
      super.onCreate(icicle);

      
      requestWindowFeature(android.view.Window.FEATURE_NO_TITLE);
      getWindow().addFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
    		  | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
    		  | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
      
      Log.v("dismiss","creating dismiss window");
      
           
      updateLayout();

}      
      protected View inflateView(LayoutInflater inflater) {
      return inflater.inflate(R.layout.dismisslayout, null);
  }

  private void updateLayout() {
      LayoutInflater inflater = LayoutInflater.from(this);

      setContentView(inflateView(inflater));
  }
  
  
  @Override
  public void onWindowFocusChanged (boolean hasFocus) {
      if (hasFocus) {
    	  //this should be the first safe point to kill the guard and start finishing
    	  //should eliminate the need for the finish delay thread.
    	  Log.v("handoff complete","dismiss window gained focus, closing all");
    	  
    	  //send callback to the guard which will start closing it
    	  Intent intent = new Intent("i4nc4mp.myLock.lifecycle.CALL_START");
          getApplicationContext().sendBroadcast(intent);
          
          PowerManager myPM = (PowerManager) getApplicationContext().getSystemService(Context.POWER_SERVICE);
          myPM.userActivity(SystemClock.uptimeMillis(), true);
          
          moveTaskToBack(true);
          //this actually ensures a clean finish because we have no history flag
          //FIXME let's get a callback from mediator as soon as it receives the stop callback
          //that way we know guard has been moved to back while we are still up front
          //then we can move to back safely revealing activity, and not the keyguard window
          }
  }
  
  @Override
  public void onDestroy() {
      super.onDestroy();
     
      Log.v("destroy_dismiss","Destroying");
  }
}
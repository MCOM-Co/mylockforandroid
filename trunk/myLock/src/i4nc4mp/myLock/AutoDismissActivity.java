package i4nc4mp.myLock;

import android.app.Activity;

import android.content.Intent;
import android.os.Bundle;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;

//One shot dismiss_keyguard activity. functions by launching, then finishing 50 ms later
//we use it so we don't have to use the pre-2.0 dismiss & secure exit commands (which are really strict)

public class AutoDismissActivity extends Activity {
 
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
  public void onResume() {
	  super.onResume();
	  
	  Log.v("dismiss","resume occurred");
  }
  
  @Override
  public void onPause() {
	  super.onPause();
	  
	  Log.v("dismiss","pause occurred");
  }
  
 
  @Override
  public void onWindowFocusChanged (boolean hasFocus) {
      if (hasFocus) {
    	  //this should be the first safe point to kill the guard and start finishing
    	  //should eliminate the need for the finish delay thread.
    	  Log.v("dismiss complete","gained focus, doing finish broadcasts");
          
          //PowerManager myPM = (PowerManager) getApplicationContext().getSystemService(Context.POWER_SERVICE);
          //myPM.userActivity(SystemClock.uptimeMillis(), true);
    	  
    	//callback mediator for final handling of the stupid wake lock
          Intent intent = new Intent("i4nc4mp.myLock.lifecycle.LOCKSCREEN_EXITED");
          getApplicationContext().sendBroadcast(intent);
    	  
          
          moveTaskToBack(true);
          //this actually ensures a clean finish because we have no history flag
          
          
          
          }
  }
  
  @Override
  public void onDestroy() {
      super.onDestroy();
     
      Log.v("destroy_dismiss","Destroying");
  }
}
package i4nc4mp.myLock;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;

//One shot dismiss_keyguard activity. functions by launching, then finishing 50 ms later
//we use it so we don't have to use the pre-2.0 dismiss & secure exit commands (which are really strict)

public class DismissActivity extends Activity {
      
      private Handler serviceHandler;
      private Task myTask = new Task();
      
      protected void onCreate(Bundle icicle) {
      super.onCreate(icicle);

      
      requestWindowFeature(android.view.Window.FEATURE_NO_TITLE);
      getWindow().addFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
    		  | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
    		  | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
      //ineffective - the screen still fails to stay awake. seems to be going off when KG shows for an instant
      
      Log.v("dismiss","creating dismiss window");
      
      serviceHandler = new Handler();
      
      updateLayout();
      //Can't call finish here, the timing won't recognize that keyguard is finished being dismissed
      //setResult(RESULT_OK);
      serviceHandler.postDelayed(myTask, 50L);
}
      
      protected View inflateView(LayoutInflater inflater) {
      return inflater.inflate(R.layout.guardlayout, null);
  }

  private void updateLayout() {
      LayoutInflater inflater = LayoutInflater.from(this);

      setContentView(inflateView(inflater));
  }
  
  /*protected void onPostCreate(Bundle savedInstanceState) {
              super.onPostCreate(savedInstanceState);
  
              
              
      }*/
  
  class Task implements Runnable {
              public void run() {
            	  finish();
              }}
  
  @Override
  public void onDestroy() {
      super.onDestroy();
      
      serviceHandler.removeCallbacks(myTask);
      serviceHandler = null;
     
      Log.v("destroy_dismiss","Destroying");

      //Intent intent = new Intent("i4nc4mp.myLockcomplete.lifecycle.CALL_START");
      //getApplicationContext().sendBroadcast(intent);
  }
}
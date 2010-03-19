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


//the issue we have here is the handoff from the guard activity to this... sometimes the KG is coming back
//inbetween the two and shutting the screen back off (bug). even happens rarely after the timing fix

//The only way to prevent it is to protect guard activity from trying to finish until it gets resumed back
//after the start dismiss - essentially we set a pendingDismiss flag, then when resume happens during the flag
//we then call finish - this seems to create a visible lag
//it seems this could be fixed by doing a moveTaskToBack on the guard activity once dismiss actually gains focus

public class DismissActivity extends Activity {
      
      //private Handler serviceHandler;
      //private Task myTask = new Task();
      
      protected void onCreate(Bundle icicle) {
      super.onCreate(icicle);

      
      requestWindowFeature(android.view.Window.FEATURE_NO_TITLE);
      getWindow().addFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
    		  | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
    		  | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
      
      Log.v("dismiss","creating dismiss window");
      
      //serviceHandler = new Handler();
      
      updateLayout();
      //Can't call finish here, the timing won't recognize that keyguard is finished being dismissed
      //setResult(RESULT_OK);
      //serviceHandler.postDelayed(myTask, 50L);
}
      
      protected View inflateView(LayoutInflater inflater) {
      return inflater.inflate(R.layout.guardlayout, null);
  }

  private void updateLayout() {
      LayoutInflater inflater = LayoutInflater.from(this);

      setContentView(inflateView(inflater));
  }
  
  /*class Task implements Runnable {
              public void run() {
            	  finish();
              }}*/
  
  @Override
  public void onWindowFocusChanged (boolean hasFocus) {
      if (hasFocus) {
    	  //this should be the first safe point to kill the guard and start finishing
    	  //should eliminate the need for the finish delay thread.
    	  Log.v("dismiss complete","gained focus, doing finish broadcasts");
    	  
    	  //send callback to the guard which will start closing it
    	  Intent intent = new Intent("i4nc4mp.myLock.lifecycle.CALL_START");
          getApplicationContext().sendBroadcast(intent);
          
          moveTaskToBack(true);
          //this actually ensures a clean finish because we have no history flag
          }
  }
  
  @Override
  public void onDestroy() {
      super.onDestroy();
      
      //serviceHandler.removeCallbacks(myTask);
      //serviceHandler = null;
     
      Log.v("destroy_dismiss","Destroying");
  }
}
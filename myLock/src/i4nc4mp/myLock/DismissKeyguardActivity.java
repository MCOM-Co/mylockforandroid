package i4nc4mp.myLock;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;

//I'll call this to kill the lockscreen from both the standard mode and the custom lock mode
//for custom lock it is the workaround to a power unlock request during a silent locked-key wake
//the timer service will start this, causing the regular lockscreen to destroy because of focus change
public class DismissKeyguardActivity extends Activity {
	//Intent.FLAG_ACTIVITY_NO_HISTORY - causes this activity to end after user leaves it, unconditionally
	//when I launch with this flag at screen off it seems to wake screen and end itself.
	
	//should be useful for a one shot keyguard stop
	//to be more situationally useful than keyguard manager commands
	private Handler serviceHandler;
	private Task myTask = new Task();
	
	protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        
        requestWindowFeature(android.view.Window.FEATURE_NO_TITLE);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
        		| WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
        Log.v("dismiss","creating dismiss window");
        updateLayout();
        //Can't call finish here
        //Have to cause delay like in the first alpha 2 window based implementation. we're creating this at wakeup essentially
        //no use in the instant unlock but useful for the bug fix in unlock from within quiet-wake
}
	
	protected View inflateView(LayoutInflater inflater) {
        return inflater.inflate(R.layout.lockactivity, null);
    }

    private void updateLayout() {
        LayoutInflater inflater = LayoutInflater.from(this);

        setContentView(inflateView(inflater));
    }
    
    protected void onPostCreate(Bundle savedInstanceState) {
		super.onPostCreate(savedInstanceState);
								
			serviceHandler = new Handler();
			serviceHandler.postDelayed(myTask, 50L);
		
	}
    
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
    }
}
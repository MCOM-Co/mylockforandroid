package i4nc4mp.myLock;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;

//one shot window that works better than securely exit hack from alpha 2
//should always be started by a mediator service
//it finishes itself off
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
        		| WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
        		| WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        Log.v("dismiss","creating dismiss window");
        
        //setVisible(false);
        //Tells the OS that we don't want to display any UI
        //we never have a UI for interaction
        //TODO might mess up the functionality, might enhance it, we shall see
        
        updateLayout();
        //Can't call finish here
        //Have to cause delay like in the first alpha 2 window based implementation. we're creating this at wakeup essentially
        //no use in the instant unlock but useful for the bug fix in unlock from within quiet-wake
        
        
        //we can use getIntent() to see the intent we got started by. this will allow me to handle different start reasons
        //by just implementing different custom intents and registering the activity for them
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
       //possibly add a user activity poke here, sometimes OS thinks it should sleep again after this guy gets destroyed
        	//it appears the glitch and send back to sleep originates before this, the OS actually is trying to ignore the users wake command
        	//since it comes so soon after the sleep command that apparently isn't done
        	//we get "ignoring user action while going to sleep
        	//the bug never happens while the stay awake service is running the persistent wakelock
        Log.v("destroy_dismiss","Destroying");
    }
}
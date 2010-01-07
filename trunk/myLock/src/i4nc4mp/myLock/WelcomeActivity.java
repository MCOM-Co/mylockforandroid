package i4nc4mp.myLock;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;

//starting point is the alarmalert prompt.
//this window itself dismisses keyguard.

//android color = #ffa4c639

public class WelcomeActivity extends Activity {
	
	public boolean noWelcome = false;
	//default is now to keep it up for alpha testing
	//past version autolyzed the window as a kg skip workaround
	
	private Handler serviceHandler;
	private Task myTask = new Task();
	
	//very very complicated business.
	@Override
    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        requestWindowFeature(android.view.Window.FEATURE_NO_TITLE);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                | WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
                | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
        //removed this flag | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
        //without it, screen seems to sleep but keyguard doesn't come back
        updateLayout();
	}
	
	//has something to do with whether we see what was behind or see a fullscreen with wallpaper BG
	protected View inflateView(LayoutInflater inflater) {
        return inflater.inflate(R.layout.welcomeactivity, null);
    }

    private void updateLayout() {
        LayoutInflater inflater = LayoutInflater.from(this);

        setContentView(inflateView(inflater));
    }
	
	/*
	@Override
    public void onBackPressed() {
        // Don't allow back to dismiss.
        return;
    }*/
    
    protected void onPostCreate(Bundle savedInstanceState) {
		super.onPostCreate(savedInstanceState);
		if (noWelcome) {
			//wait half a second and call finish--- stupid hack
						
			serviceHandler = new Handler();
			serviceHandler.postDelayed(myTask, 50L);
		}
	}
    
    class Task implements Runnable {
		public void run() {
			finish();
		}}
    
    @Override
    protected void onStop() {
        super.onStop();
        // Don't hang around.
        finish();
    }
    
    @Override
    public void onDestroy() {
        super.onDestroy();
        
        if(noWelcome) {
        	serviceHandler.removeCallbacks(myTask);
        	serviceHandler = null;
        }
        
        Log.v("destroyWelcome","Destroying");
    }
    
    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        // Do this on key down to handle a few of the system keys.
        boolean up = event.getAction() == KeyEvent.ACTION_UP;
        switch (event.getKeyCode()) {
            // Volume keys and camera keys do not dismiss the welcome
            case KeyEvent.KEYCODE_VOLUME_UP:
            case KeyEvent.KEYCODE_VOLUME_DOWN:
            case KeyEvent.KEYCODE_CAMERA:
            case KeyEvent.KEYCODE_FOCUS:
                if (up) {
                    break;
                    }
                
                return true;
            
    		default:
                break;
        }
        return super.dispatchKeyEvent(event);
    }
}
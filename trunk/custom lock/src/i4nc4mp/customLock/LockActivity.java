package i4nc4mp.customLock;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;

//starting point is the alarmalert prompt.
//this window itself dismisses keyguard.

//we are going to call it up when the screen goes OFF
//then attempt to mediate which keys will cause a result at that point

//android color = #ffa4c639

public class LockActivity extends Activity {
        
        private Handler serviceHandler;
        private Task myTask = new Task();
        
        //private boolean unlock = false;
        
        //very very complicated business.
        @Override
    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        requestWindowFeature(android.view.Window.FEATURE_NO_TITLE);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);
        //removed | WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
        /*
         * this flag pushes the activity up and gets it ready at screen off but lockscreen rules stay in effect
         * that means when finish is called, we see the lockscreen again
         * that means this flag is good for showing a custom lockscreen to user
         * it would have to be paired with the current full exit method after that
         * to be of any use
         */
        
        /*
         * with dismiss keyguard, same happens but every key wakes phone after that (no lockscreen rules)
         * therefore, we can set up wakelock manipulation and set customization
         * so user chooses which buttons fire the shorter wakelock vs which will exit (perceived as unlock)
        */
        
        //issue right now is that I cannot get the wakelock to behave as expected.
        //the screen is staying on when we release, when documentation says it shouldn't
        
        updateLayout();
        
        serviceHandler = new Handler();
        
        //IntentFilter onfilter = new IntentFilter (Intent.ACTION_SCREEN_ON);
        //registerReceiver(screenon, onfilter);
        }
        
        /*
        BroadcastReceiver screenon = new BroadcastReceiver() {
    		
    		public static final String TAG = "screenon";
    		public static final String Screen = "android.intent.action.SCREEN_ON";
    		
    		

    		@Override
    		public void onReceive(Context context, Intent intent) {
    			if (!intent.getAction().equals(Screen)) return;
    			Log.v(TAG,"screen on happened");
    	
    			if (unlock) {
    				finish();
     			}
    }};*/
        
        //has something to do with whether we see what was behind or see a fullscreen with wallpaper BG
        protected View inflateView(LayoutInflater inflater) {
        return inflater.inflate(R.layout.lockactivity, null);
    }

    private void updateLayout() {
        LayoutInflater inflater = LayoutInflater.from(this);

        setContentView(inflateView(inflater));
    }
        
        
        @Override
    public void onBackPressed() {
        // Don't allow back to dismiss.
        return;
    }
    
    protected void onPostCreate(Bundle savedInstanceState) {
                super.onPostCreate(savedInstanceState);
        }
    
    class Task implements Runnable {
                public void run() {
                	finish();
                	//if (unlock) finish();
                	//else ManageWakeLock.releaseFull();
                	//problem is screen doesn't go off
                	//the expected behavior of the PM is not happening
                	}
    }
    
    @Override
    protected void onStop() {
        super.onStop();
        // Don't hang around.
        finish();
    }
    
    @Override
    public void onDestroy() {
        super.onDestroy();
        
       serviceHandler.removeCallbacks(myTask);
       serviceHandler = null;
      
       //unregisterReceiver(screenon);
        
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
                
                //do something like schedule a very fast screen off
                
                Log.v("key event","locked keys are happening");
              //the problem here is that it seems the first press never registers here
                //the 2nd try always gets the log items
       
                //unlock = false;
                                
                //serviceHandler.postDelayed(myTask, 1000L);
                //possible workaround here would be to send the power key event
                //don't know if it is possible to generate that and cause OS to force sleep
                
                
                return true;
                //returning true means we handled the event so don't pass it to other processes
            
                default:
                	Log.v("key event","unlock keys are happening");
                	//unlock = true;
                	serviceHandler.postDelayed(myTask, 1L);
                break;
                //break without return means pass on to other processes
                //don't consume the press
        }
        return super.dispatchKeyEvent(event);
    }
}
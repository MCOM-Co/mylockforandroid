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
        
        private boolean unlock = false;
        //we will set this true if we want the screen wake to close the lockscreen
        //a key event that should not unlock will set it false
        //the key event handling default case sets true when it isn't a locked key
        
        //very very complicated business.
        @Override
    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        requestWindowFeature(android.view.Window.FEATURE_NO_TITLE);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);
        //removed | WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
        //removed | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
        //removed | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
        //these were only used for ensuring it would work and come up when secure pattern was on
        //they also wake it up from sleep and cause stay awake
        
        //i'm going to try calling the activity when screen sleeps, without waking up
        //and go from there
        
        //if we stop dismissing kg and do show when locked instead
        //it should let us grab and release a wakelock 3 sec timer or timer of choice
        //we want a very short timeout if it isn't an unlock key.
        updateLayout();
        
        serviceHandler = new Handler();
        
        //next, register for screen on broadcast so we can autolyze when the user opens the screen
        IntentFilter onfilter = new IntentFilter (Intent.ACTION_SCREEN_ON);
        registerReceiver(screenon, onfilter);
        }
        
        BroadcastReceiver screenon = new BroadcastReceiver() {
    		
    		public static final String TAG = "screenon";
    		public static final String Screen = "android.intent.action.SCREEN_ON";
    		
    		

    		@Override
    		public void onReceive(Context context, Intent intent) {
    			if (!intent.getAction().equals(Screen)) return;
    			Log.v(TAG,"screen on happened");
    			//SharedPreferences settings = getSharedPreferences("myLock", 0);
    			//boolean welcome = settings.getBoolean("welcome", false);
    			
    			//StartLock(context);
    			if (unlock) finish();
    }};
        
        //has something to do with whether we see what was behind or see a fullscreen with wallpaper BG
        protected View inflateView(LayoutInflater inflater) {
        return inflater.inflate(R.layout.lockactivity, null);
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
        }
    
    class Task implements Runnable {
                public void run() {
                        ManageWakeLock.releaseFull();
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
      
       unregisterReceiver(screenon);
        
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
                //might be so fast user never sees screen come on
                //example
                //serviceHandler = new Handler(); -- moved to oncreate
                Log.v("key event","locked keys are happening");                
                //ManageWakeLock.releaseFull();
                //ManageWakeLock.goToSleep(getApplicationContext()); complete fail
                unlock = false;
                //serviceHandler.postDelayed(myTask, 1L);//this task releases the WL
                //the problem here is that it seems the first press never registers here.
                //the 2nd try always gets the log items
                
                
                return true;
                //returning true means we handled the event so don't pass the buck
                //this would mean we keep it locked while returning true for these
                //the break means that it doesn't handle it on up, but on down.
            
                default:
                	Log.v("key event","unlock keys are happening");
                	unlock = true;
                break;
                //this means that all other key events just break
                //meaning they are passed on for other handling
        }
        return super.dispatchKeyEvent(event);
    }
}
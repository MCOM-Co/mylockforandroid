package i4nc4mp.myLockcomplete;

//One shot dismiss_keyguard activity. functions by launching, then finishing 50 ms later
//we use it so we don't have to use the pre-2.0 dismiss & secure exit commands (which are really strict)

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;

public class DismissActivity extends Activity {
        //Intent.FLAG_ACTIVITY_NO_HISTORY - causes this activity to end after user leaves it, unconditionally
        //when I launch with this flag at screen off it seems to wake screen and end itself.
        
        private Handler serviceHandler;
        private Task myTask = new Task();
        
        protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        
        requestWindowFeature(android.view.Window.FEATURE_NO_TITLE);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);
        Log.v("dismiss","creating dismiss window");
        
        serviceHandler = new Handler();
        
        updateLayout();
        //Can't call finish here, the timing won't recognize that keyguard is finished being dismissed
}
        
        protected View inflateView(LayoutInflater inflater) {
        return inflater.inflate(R.layout.dismissactivity, null);
    }

    private void updateLayout() {
        LayoutInflater inflater = LayoutInflater.from(this);

        setContentView(inflateView(inflater));
    }
    
    protected void onPostCreate(Bundle savedInstanceState) {
                super.onPostCreate(savedInstanceState);
    
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
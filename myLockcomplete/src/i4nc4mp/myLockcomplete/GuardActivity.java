package i4nc4mp.myLockcomplete;

import i4nc4mp.myLockcomplete.ManageKeyguard.LaunchOnKeyguardExit;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.PowerManager;
import android.os.SystemClock;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;


//Guarded mode for use only without pattern mode
//which does the secure lockscreen exit
//that was used by the original alpha 2c

public class GuardActivity extends Activity {
                
        //very very complicated business.
        @Override
    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        requestWindowFeature(android.view.Window.FEATURE_NO_TITLE);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);
        
        updateLayout();
        
        
        }
               
    protected View inflateView(LayoutInflater inflater) {
        return inflater.inflate(R.layout.lockactivity, null);
    }

    private void updateLayout() {
        LayoutInflater inflater = LayoutInflater.from(this);

        setContentView(inflateView(inflater));
    }
        
    //TODO we might be able to offer customization of advanced power save for bumped slider
    //need to try this, low priority
    
    @Override
    public void onResume() {
        super.onResume();
        DoExit(getApplicationContext());
    }
    
    
    public void DoExit(Context context) {//try the alpha keyguard manager secure exit
        
        //ManageKeyguard.initialize(context);
        PowerManager pm = (PowerManager) getSystemService (Context.POWER_SERVICE); 
        pm.userActivity(SystemClock.uptimeMillis(), false);
        //ensure it will be awake
        
        ManageKeyguard.disableKeyguard(getApplicationContext());
        //advantage here is we don't have to do a task delay
        //because we're already showing on top of keyguard this gets the job done
        
        
        ManageKeyguard.exitKeyguardSecurely(new LaunchOnKeyguardExit() {
            public void LaunchOnKeyguardExitSuccess() {
               Log.v("doExit", "This is the exit callback");
               finish();
                }});            
    }
    
    
}
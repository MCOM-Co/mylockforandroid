package i4nc4mp.myLock;


import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

import android.os.Bundle;
import android.os.Handler;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;

import android.view.WindowManager;

//The lockscreen that comes up over the top of secure pattern mode. This will be placed by a subclass of the mediator
//No key handling since we can't handle keys with the show when locked flag

public class ShowWhenLockedActivity extends Activity {
                
	//private Handler serviceHandler;
	//private Task myTask = new Task();
	
	//public int bright = 10;
	
        @Override
    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        //Log.v("create nolock","about to request window params");
        requestWindowFeature(android.view.Window.FEATURE_NO_TITLE);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);
        //we could actually delete this param and then allow the activity to get sent to back when system restores the KG. \
        //this would be the place to call the go to sleep we designed in the first iteration of custom lock
       
       updateLayout();
       

       //serviceHandler = new Handler();
       //serviceHandler.postDelayed(myTask, 100L);
        
           
        }
        
            
    protected View inflateView(LayoutInflater inflater) {
        return inflater.inflate(R.layout.lockactivity, null);
    }

    private void updateLayout() {
        LayoutInflater inflater = LayoutInflater.from(this);

        setContentView(inflateView(inflater));
    }
    
    /*
    @Override
    public void onResume() {
        super.onResume();
        
        if (hasWindowFocus()) finish();//we'll close once user wakes up device, yielding to the system keyguard
    }*/
    
    
    @Override
    public void onDestroy() {
        super.onDestroy();  
        	//serviceHandler.removeCallbacks(myTask);
        	//serviceHandler = null;
    }
}
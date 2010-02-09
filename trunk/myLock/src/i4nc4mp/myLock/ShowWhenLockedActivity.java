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

//solved the same thing this activity did by using disable / reenable kg calls in the mediator service
//this activity is no longer used, was just a stepping stone to the better solution

public class ShowWhenLockedActivity extends Activity {
                
	//private Handler serviceHandler;
	//private Task myTask = new Task();
	
	//public int bright = 10;
	
        @Override
    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        //Log.v("create nolock","about to request window params");
        requestWindowFeature(android.view.Window.FEATURE_NO_TITLE);
        //getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);
        //we could actually delete this param and then allow the activity to get sent to back when system restores the KG. \
        //this would be the place to call the go to sleep we designed in the first iteration of custom lock
       
       updateLayout();
       
       setBright((float) 0.0);
       
       //not sure what effect these calls will have while KG is actually already dismissed. 
       //I still have to call disable to get a secure exit during a show when locked activity, normally
       //ManageKeyguard.disableKeyguard(getApplicationContext());
       //ManageKeyguard.reenableKeyguard();
       //these worked one time when the setbright call happened before the update layout
       //log showed screen on, half sec later screen off, and then immediately keyguard was up
       

       //serviceHandler = new Handler();
       //serviceHandler.postDelayed(myTask, 100L);
        
           
        }
        
        public void setBright(float value) {
        	Window mywindow = getWindow();
        	
        	WindowManager.LayoutParams lp = mywindow.getAttributes();

    		lp.screenBrightness = value;

    		mywindow.setAttributes(lp);
        }
        
        //call this task to turn off the screen in a fadeout.

        /*
        class Task implements Runnable {
        	public void run() {                
        		if (bright != 0) {
        			setBright(bright/100); //start at 10% bright and go to 0 (screen off)

        			bright--;
        			serviceHandler.postDelayed(myTask, 100L);
                    }
        		else {
        			setBright((float) 0.0); 
        			
        			bright = 10;//put bright back
        		}
        	}
        }*/
                
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
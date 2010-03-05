package i4nc4mp.myLockcomplete;


import java.util.GregorianCalendar;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;

//The lockscreen that comes up over the top of secure pattern mode. This will be placed by a subclass of the mediator
//No key handling since we can't handle keys with the show when locked flag
//the mediator for this needs to get the user_present broadcast to know that it needs to get ready to restore this lock at next screen off

//we can actually combine idle timeout with this if we want to provide an always customized lockscreen experience that still goes secure

//the real point of this mode though is for users who always want that level of security in place behind their lockscreen experience.

//we can use this also for guarded mode
//I believe we can simply call securely exit here, which will require pattern screen in pattern mode, but otherwise dismisses keyguard.
//we can call finish upon kg exit success callback.

public class ShowWhenLockedActivity extends Activity {
                
	//private Handler serviceHandler;
	//private Task myTask = new Task();
	
	//public int bright = 10;
	
	private Button mrewindIcon;
    private Button mplayIcon;
    private Button mpauseIcon;
    private Button mforwardIcon;
    
    public TextView curhour;
    public TextView curmin;
    
    public TextView batt;
    
    
    //private AudioManager am = (AudioManager) getApplicationContext().getSystemService(Context.AUDIO_SERVICE);
	
        @Override
    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        //Log.v("create nolock","about to request window params");
        requestWindowFeature(android.view.Window.FEATURE_NO_TITLE);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);
        //we could actually delete this param and then allow the activity to get sent to back when system restores the KG. \
        //this would be the place to call the go to sleep we designed in the first iteration of custom lock
       
       updateLayout();
       

       curhour = (TextView) findViewById(R.id.hourText);
       
       curmin = (TextView) findViewById(R.id.minText);
       
       batt = (TextView) findViewById(R.id.batt);
       
      updateClock();
       
       mrewindIcon = (Button) findViewById(R.id.PrevButton); 
       
       mrewindIcon.setOnClickListener(new View.OnClickListener() {
           public void onClick(View v) {
            Intent intent;
            intent = new Intent("com.android.music.musicservicecommand.previous");
            getApplicationContext().sendBroadcast(intent);
            }
         });

       mplayIcon = (Button) findViewById(R.id.PlayToggle); 

       mplayIcon.setOnClickListener(new View.OnClickListener() {
           public void onClick(View v) {
            Intent intent;
            //if (am.isMusicActive()) {
            	intent = new Intent("com.android.music.musicservicecommand.togglepause");
                getApplicationContext().sendBroadcast(intent);
            //}
            /*else {
            	intent = new Intent();
            	intent.setClassName("com.android.music","com.android.music.MediaPlaybackService");
            	startService(intent);
            }*/
            /*if (!am.isMusicActive()) {
                mpauseIcon.setVisibility(View.VISIBLE);
                mplayIcon.setVisibility(View.GONE);
                }*/
            }
         });

       /*mpauseIcon = (ImageButton) findViewById(R.id.pauseIcon); 

       mpauseIcon.setOnClickListener(new View.OnClickListener() {
           public void onClick(View v) {
            Intent intent;
            intent = new Intent("com.android.music.musicservicecommand.togglepause");
            getBaseContext().sendBroadcast(intent);
            if (am.isMusicActive()) {
                mplayIcon.setVisibility(View.VISIBLE);
                mpauseIcon.setVisibility(View.GONE);
                }
            }
         });*/

       mforwardIcon = (Button) findViewById(R.id.NextButton); 

       mforwardIcon.setOnClickListener(new View.OnClickListener() {
           public void onClick(View v) {
            Intent intent;
            intent = new Intent("com.android.music.musicservicecommand.next");
            getApplicationContext().sendBroadcast(intent);
            }
         });
        
           
        }
        
        
        public void updateClock() {
        	GregorianCalendar Calendar = new GregorianCalendar();         
            
        	int mHour = Calendar.get(GregorianCalendar.HOUR_OF_DAY);
        	int mMin = Calendar.get(GregorianCalendar.MINUTE);
        	
        	String hour = new String("");
        	String min = new String("");
        	
            if (mHour <10) hour = hour + "0";
            hour = hour + mHour;
            
            if (mMin <10) min = min + "0";
            min = min + mMin;
            
            curhour.setText(hour);
            curmin.setText(min);
            
            
            //update battery as it is also a form of time passing
            
            SharedPreferences settings = getSharedPreferences("myLock", 0);
            int battlevel = settings.getInt("BattLevel", 0);
            
            batt.setText(battlevel + "%");
            
            
        }    
            
    protected View inflateView(LayoutInflater inflater) {
        return inflater.inflate(R.layout.lockactivity, null);
    }

    private void updateLayout() {
        LayoutInflater inflater = LayoutInflater.from(this);

        setContentView(inflateView(inflater));
    }
    
    @Override
    public void onBackPressed() {
        	finish();
        return;
    }
    
    @Override
    public void onResume() {
    	super.onResume();
    	updateClock();
    }
}
package i4nc4mp.myLock;


import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.Toast;


public class SettingsActivity extends Activity {
	
	//private SharedPreferences settings = null;
	//private SharedPreferences.Editor editor = null;
	
	public boolean triedstart = false;
	
	public boolean fgmode = true;
    public boolean awake = false;
    public boolean welcomemode = false;
    	
	/** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settingsactivity);
       
      getPrefs();//grabs our user's current settings for startup commands
      
       Button toggle = (Button)findViewById(R.id.toggleButton);
        
       toggle.setOnClickListener(new OnClickListener() {
          	public void onClick(View v){
          		TryToggle();
          	}
          });
       
       final CheckBox fg = (CheckBox)findViewById(R.id.fgBox);
       
       fg.setChecked((fgmode)); 
       
       fg.setOnClickListener(new OnClickListener() {

           public void onClick(View v) {
        	   SharedPreferences set = getSharedPreferences("myLock", 0);
        	   SharedPreferences.Editor editor = set.edit();
               editor.putBoolean("FG", fg.isChecked());

               // Don't forget to commit your edits!!!
               editor.commit();
               startService();//call start service, so it can get FG pref & do change
           }
       });
       
       final CheckBox wake = (CheckBox)findViewById(R.id.wakeBox);

       wake.setChecked((awake));        
               
       wake.setOnClickListener(new OnClickListener() {

                   public void onClick(View v) {
                	   SharedPreferences set = getSharedPreferences("myLock", 0);
                	   SharedPreferences.Editor editor = set.edit(); 
                	   
                	   editor.putBoolean("StayAwake", wake.isChecked());

                       // Don't forget to commit your edits!!!
                       editor.commit();
                       //finally, do the change
                       if (wake.isChecked()) StartWake();
                       else StopWake();
                       //at boot when option is enabled the service starts wake
                       //wake is never stopped, except by the user here
                   }
               });
       
       final CheckBox welcome = (CheckBox)findViewById(R.id.welcomeBox);
       
       welcome.setChecked((welcomemode));        
       
       welcome.setOnClickListener(new OnClickListener() {

                   public void onClick(View v) {
                	   SharedPreferences set = getSharedPreferences("myLock", 0);
                	   SharedPreferences.Editor editor = set.edit(); 
                	   
                	   editor.putBoolean("welcome", welcome.isChecked());

                       // Don't forget to commit your edits!!!
                       editor.commit();
                       //finally, do the change
                   }
               });
    }
    
    public void getPrefs() {
    	SharedPreferences settings = getSharedPreferences("myLock", 0);
    	
    	/*if (settings == null) {
        	Log.v("getprefs","failed due to settings null. using defaults.");
        	return;        
        }*/
    	
        fgmode = settings.getBoolean("FG", true);
        awake = settings.getBoolean("StayAwake", false);
        welcomemode = settings.getBoolean("welcome", false);
    }
    
    private void startService(){
   			Intent i = new Intent();
   			i.setClassName("i4nc4mp.myLock", "i4nc4mp.myLock.LockMediatorService");
   			startService(i);
   			Log.d( getClass().getSimpleName(), "startService()" );
   		}
    
    private void stopService() {
			Intent i = new Intent();
			i.setClassName("i4nc4mp.myLock", "i4nc4mp.myLock.LockMediatorService");
			stopService(i);
			Log.d( getClass().getSimpleName(), "stopService()" );
			Toast.makeText(SettingsActivity.this, "myLock has been stopped", Toast.LENGTH_SHORT).show();
			finish();
    }
   
    private void TryToggle() {
    	if (!triedstart) {
    		startService();
    		Toast.makeText(SettingsActivity.this, "intializing... press toggle to confirm stop myLock", Toast.LENGTH_SHORT).show();
    		triedstart = true;
    	}
    	//first attempt to start. the service does nothing if already initialized
    	//else if (conn == null) bindService();
    	else {
    		triedstart = false;
    		stopService();
    	}//the user has clicked a 2nd time. confirming they do want the lockscreen re-enabled
    }
    
    private void StartWake() {
    	
    		Toast.makeText(SettingsActivity.this, "stay awake initialized", Toast.LENGTH_SHORT).show();
    		ManageWakeLock.acquireFull(getApplicationContext());
    		//when it starts at boot, the notification will say stay awake is on
    		//so if user has wake on but fg off, they won't get a message at boot but it still starts
    	}
    
    private void StopWake() {
    	//PowerManager pm = (PowerManager) getSystemService (Context.POWER_SERVICE); 
		//pm.userActivity(SystemClock.uptimeMillis()+ 10, false);
		
		ManageWakeLock.DoCancel(getApplicationContext(), 100);//sets a 10 second timeout
		Toast.makeText(SettingsActivity.this, "auto-sleep re-enabled", Toast.LENGTH_SHORT).show();
    }

protected void onDestroy() {
	  super.onDestroy();
	  Log.d( getClass().getSimpleName(), "onDestroy()" );
	}
}


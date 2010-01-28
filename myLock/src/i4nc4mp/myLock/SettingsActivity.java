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
	
	public boolean triedstart = false;
	
	
	public boolean persistentNotif = true;
    public boolean awake = false;
    //public boolean customLock = false;
    public boolean customLock = true;
    public boolean boot = false;
    	
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
                       ToggleWake();
                       if (wake.isChecked()) Toast.makeText(SettingsActivity.this, "stay awake initialized", Toast.LENGTH_SHORT).show();
                       else Toast.makeText(SettingsActivity.this, "stay awake disabled", Toast.LENGTH_SHORT).show();
                       //at boot when option is enabled the service starts wake
                       //wake is never stopped, except by the user here
                   }
               });
       
       /*final CheckBox welcome = (CheckBox)findViewById(R.id.welcomeBox);
       
       welcome.setChecked((customLock));        
       
       welcome.setOnClickListener(new OnClickListener() {

                   public void onClick(View v) {
                	   SharedPreferences set = getSharedPreferences("myLock", 0);
                	   SharedPreferences.Editor editor = set.edit(); 
                	   
                	   editor.putBoolean("welcome", welcome.isChecked());

                       // Don't forget to commit your edits!!!
                       editor.commit();
                 
                       //The current mode needs to be stopped
                       stopService();//it will go by the existing pref
                       triedstart = false;//ensures next toggle command will start the new mode
                       Toast.makeText(SettingsActivity.this, "Press toggle to complete mode change", Toast.LENGTH_SHORT).show();
                       customLock = !customLock;//toggle it locally for reference of next toggle press

                   }
               });*/
       
       final CheckBox fg = (CheckBox)findViewById(R.id.fgBox);
       
       fg.setChecked((persistentNotif)); 
       
       fg.setOnClickListener(new OnClickListener() {

           public void onClick(View v) {
        	   SharedPreferences set = getSharedPreferences("myLock", 0);
        	   SharedPreferences.Editor editor = set.edit();
               editor.putBoolean("FG", fg.isChecked());

               // Don't forget to commit your edits!!!
               editor.commit();
               
               startService();//call start service, so it can react to the change
           }
       });
                     
       final CheckBox bootup = (CheckBox)findViewById(R.id.bootBox);
       
       bootup.setChecked((boot));        
       
       bootup.setOnClickListener(new OnClickListener() {

                   public void onClick(View v) {
                	   SharedPreferences set = getSharedPreferences("myLock", 0);
                	   SharedPreferences.Editor editor = set.edit(); 
                	   
                	   editor.putBoolean("boot", bootup.isChecked());

                       // Don't forget to commit your edits!!!
                       editor.commit();
                   }
               });
    }
    
    public void getPrefs() {
    	SharedPreferences settings = getSharedPreferences("myLock", 0);
    	
    	/*if (settings == null) {
        	Log.v("getprefs","failed due to settings null. using defaults.");
        	return;        
        }*/
    	
        persistentNotif = settings.getBoolean("FG", true);
        awake = settings.getBoolean("StayAwake", false);
        //customLock = settings.getBoolean("welcome", false);
        boot = settings.getBoolean("boot", false);
    }
    
    /*start and stop methods rely on pref and are only used by toggle button*/
    private void startService(){
   			Intent i = new Intent();
   			if (!customLock) i.setClassName("i4nc4mp.myLock", "i4nc4mp.myLock.NoLockService");
   			else i.setClassName("i4nc4mp.myLock", "i4nc4mp.myLock.CustomLockService");
   			startService(i);
   			Log.d( getClass().getSimpleName(), "startService()" );
   		}
    
    private void stopService() {
			Intent i = new Intent();
			if (!customLock) i.setClassName("i4nc4mp.myLock", "i4nc4mp.myLock.NoLockService");
			else i.setClassName("i4nc4mp.myLock", "i4nc4mp.myLock.CustomLockService");
			stopService(i);
			Log.d( getClass().getSimpleName(), "stopService()" );
    }
   
    private void TryToggle() {
    	if (!triedstart) {
    		startService();
    		Toast.makeText(SettingsActivity.this, "Intialized... press toggle again to stop myLock", Toast.LENGTH_SHORT).show();
    		triedstart = true;
    	}
    	else {
    		triedstart = false;
    		stopService();
    		Toast.makeText(SettingsActivity.this, "myLock has been stopped", Toast.LENGTH_SHORT).show();
    	}
    }

    private void ToggleWake() {    	
    		Intent i = new Intent();
    		i.setClassName("i4nc4mp.myLock", "i4nc4mp.myLock.StayAwakeService");
    		startService(i);
    		//service handles itself, closing if start is called while it is active
    	}

protected void onDestroy() {
	  super.onDestroy();
	  Log.d( getClass().getSimpleName(), "onDestroy()" );
	}
}


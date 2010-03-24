package i4nc4mp.myLock;


import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.CheckBox;
import android.widget.Toast;

//=====most reasonable moderate = want pattern to come on if phone is left idle for too long
//If not in secure mode, idle timeout option is set. User enters a number of minutes. if 0, no timeout gets enabled
//when set to 1 or more, idle timer will run

//TODO implement a box user can put number of minutes to enable idle lockdown
//TODO implement ToggleButton to utilize pref serviceactive and get rid of TryToggle

public class SettingsActivity extends Activity {
	
	public boolean triedstart = false;
		
	public boolean persistentNotif = true;
    
    public boolean boot = false;
    //public boolean shakewake = false;
    
    public boolean guard = false;
    
    public boolean active = false;

    public CheckBox toggle;
    	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settingsactivity);
       
      getPrefs();//grabs our user's current settings for startup commands
      
     toggle = (CheckBox)findViewById(R.id.activeBox);
      
      toggle.setChecked(active);
        
      toggle.setOnClickListener(new OnClickListener() {
          	public void onClick(View v){
          		if (toggle.isChecked()) {
          			startService();
            		Toast.makeText(SettingsActivity.this, "myLock is now enabled", Toast.LENGTH_SHORT).show();
          		}
          		else {
          			//the stop case will do nothing if the service had crashed or been force closed
          			//or if the device was rebooted without a clean exit
          			//it will still think it is running
          			stopService();
          			Toast.makeText(SettingsActivity.this, "myLock is now disabled", Toast.LENGTH_SHORT).show();
          		}
          	}
          });
       
       /*final CheckBox shake = (CheckBox)findViewById(R.id.shakeBox);

       shake.setChecked((shakewake));        
               
       shake.setOnClickListener(new OnClickListener() {

                   public void onClick(View v) {
                	   SharedPreferences set = getSharedPreferences("myLock", 0);
                	   SharedPreferences.Editor editor = set.edit(); 
                	   
                	   editor.putBoolean("ShakeWakeup", shake.isChecked());

                       // Don't forget to commit your edits!!!
                       editor.commit();
                       //finally, do the change
                       Intent i = new Intent();
               			i.setClassName("i4nc4mp.myLock", "i4nc4mp.myLock.ShakeWakeupService");
               			
                       if (shake.isChecked()) {
                    	   startService(i);
                    	   Toast.makeText(SettingsActivity.this, "shake wakeup initialized", Toast.LENGTH_SHORT).show();
                       }
                       else {
                    	   stopService(i);
                    	   Toast.makeText(SettingsActivity.this, "shake wakeup disabled", Toast.LENGTH_SHORT).show();
                       }
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
               getPrefs();
               if (active) startService();//call start service, so it can react to the change
               //make sure not to start it up if we aren't already
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
       
       final CheckBox slideguard = (CheckBox)findViewById(R.id.slideGuard);
       
       slideguard.setChecked((guard));        
       
       slideguard.setOnClickListener(new OnClickListener() {

    	   public void onClick(View v) {
               SharedPreferences set = getSharedPreferences("myLock", 0);
               SharedPreferences.Editor editor = set.edit(); 
               
               editor.putBoolean("slideGuard", slideguard.isChecked());

               // Don't forget to commit your edits!!!
               editor.commit();
     
               //The current mode needs to be stopped
               stopService();//it will go by the existing pref
               toggle.setChecked(false);
               Toast.makeText(SettingsActivity.this, "myLock must disable to apply change.", Toast.LENGTH_SHORT).show();
               guard = !guard;//toggle it locally for reference of next toggle press
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
        
        boot = settings.getBoolean("boot", false);
        //shakewake = settings.getBoolean("ShakeWakeup", false);
        
        active = settings.getBoolean("serviceactive", false);
        
        guard = settings.getBoolean("slideGuard", false);
    }
    
    @Override
    public void onResume() {
    	super.onResume();
    	
    	getPrefs();
    	toggle.setChecked(active);
    }
    
    /*start and stop methods rely on pref and are only used by toggle button*/
    private void startService(){
   			Intent i = new Intent();
   			
   			if (guard) i.setClassName("i4nc4mp.myLock", "i4nc4mp.myLock.BasicGuardService");
   			else i.setClassName("i4nc4mp.myLock", "i4nc4mp.myLock.AutoDismiss");
   			startService(i);
   			Log.d( getClass().getSimpleName(), "startService()" );
   		}
    
    private void stopService() {
			Intent i = new Intent();
			if (guard) i.setClassName("i4nc4mp.myLock", "i4nc4mp.myLock.BasicGuardService");
			else i.setClassName("i4nc4mp.myLock", "i4nc4mp.myLock.AutoDismiss");
			stopService(i);
			Log.d( getClass().getSimpleName(), "stopService()" );
    }
    
    
   //TODO get binding working or a way to look for whether the service is running so I can use a toggleButton instead
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
    
    @Override
    public void onBackPressed() {
    	finish();
    }

protected void onDestroy() {
	  super.onDestroy();
	  Log.d( getClass().getSimpleName(), "onDestroy()" );
	}
}


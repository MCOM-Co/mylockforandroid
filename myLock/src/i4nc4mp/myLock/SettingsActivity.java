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

public class SettingsActivity extends Activity {
	
	public boolean triedstart = false;
		
	public boolean persistentNotif = false;
    
    public boolean boot = false;
    public boolean shakewake = false;
    
    public boolean guard = false;
    
    public boolean WPlockscreen = false;
    
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
          			active = true;//so we will know if what to do if user changes any more checks after this
            		Toast.makeText(SettingsActivity.this, "myLock is now enabled", Toast.LENGTH_SHORT).show();
          		}
          		else {
          			//the stop case will do nothing if the service had crashed or been force closed
          			//or if the device was rebooted without a clean exit
          			//it will still think it is running
          			stopService();
          			active = false;
          			Toast.makeText(SettingsActivity.this, "myLock is now disabled", Toast.LENGTH_SHORT).show();
          		}
          	}
          });
              
       final CheckBox fg = (CheckBox)findViewById(R.id.fgBox);
       
       fg.setChecked((persistentNotif)); 
       
       fg.setOnClickListener(new OnClickListener() {

           public void onClick(View v) {
        	   SharedPreferences set = getSharedPreferences("myLock", 0);
        	   SharedPreferences.Editor editor = set.edit();
               editor.putBoolean("FG", fg.isChecked());

               // Don't forget to commit your edits!!!
               editor.commit();
               
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
       
       final CheckBox shake = (CheckBox)findViewById(R.id.shakeBox);

       shake.setChecked((shakewake));        
               
       shake.setOnClickListener(new OnClickListener() {

                   public void onClick(View v) {
                	   SharedPreferences set = getSharedPreferences("myLock", 0);
                	   SharedPreferences.Editor editor = set.edit(); 
                	   
                	   editor.putBoolean("shake", shake.isChecked());

                       // Don't forget to commit your edits!!!
                       editor.commit();
                       //finally, do the change
                       
                       if (active) startService();
                       //this start just has the mediator check prefs and notice the change
               			                       
                   }
               });
       shake.setVisibility(View.GONE);//hide it for now
       
       final CheckBox slideguard = (CheckBox)findViewById(R.id.slideGuard);
       
       slideguard.setChecked((guard));        
       
       slideguard.setOnClickListener(new OnClickListener() {

    	   public void onClick(View v) {
               SharedPreferences set = getSharedPreferences("myLock", 0);
               SharedPreferences.Editor editor = set.edit(); 
               
               editor.putBoolean("slideGuard", slideguard.isChecked());

               // Don't forget to commit your edits!!!
               editor.commit();
               
             //finally, do the change
               
               if (active && !WPlockscreen) startService();
               //the autodismiss mediator needs to know we toggled the mode
               //wallpaper lockscreen just checks when it is created
               }
               });
    
    
    final CheckBox wpbox = (CheckBox)findViewById(R.id.wplock);
    
    wpbox.setChecked((WPlockscreen));        
    
    wpbox.setOnClickListener(new OnClickListener() {

 	   public void onClick(View v) {
            SharedPreferences set = getSharedPreferences("myLock", 0);
            SharedPreferences.Editor editor = set.edit(); 
            
            editor.putBoolean("wallpaper", wpbox.isChecked());
            //WPlockscreen = wpbox.isChecked();
            
            // Don't forget to commit your edits!!!
            editor.commit();
  
            if (active) {
            	stopService();//stop existing mode
            	WPlockscreen = !WPlockscreen;
            	startService();//startup the new mediator
            }
            else WPlockscreen = wpbox.isChecked();
            

            }
    });
 }
    
    public void getPrefs() {
    	SharedPreferences settings = getSharedPreferences("myLock", 0);
    	
    	/*if (settings == null) {
        	Log.v("getprefs","failed due to settings null. using defaults.");
        	return;        
        }*/
    	
        persistentNotif = settings.getBoolean("FG", false);
        
        boot = settings.getBoolean("boot", false);
        shakewake = settings.getBoolean("shake", false);
        
        active = settings.getBoolean("serviceactive", false);
        
        guard = settings.getBoolean("slideGuard", false);
        
        WPlockscreen = settings.getBoolean("wallpaper", false);
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
   			
   			if (WPlockscreen) i.setClassName("i4nc4mp.myLock", "i4nc4mp.myLock.BasicGuardService");
   			else i.setClassName("i4nc4mp.myLock", "i4nc4mp.myLock.AutoDismiss");
   			startService(i);
   			Log.d( getClass().getSimpleName(), "startService()" );
   		}
    
    private void stopService() {
			Intent i = new Intent();
			if (WPlockscreen) i.setClassName("i4nc4mp.myLock", "i4nc4mp.myLock.BasicGuardService");
			else i.setClassName("i4nc4mp.myLock", "i4nc4mp.myLock.AutoDismiss");
			stopService(i);
			Log.d( getClass().getSimpleName(), "stopService()" );
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


package i4nc4mp.myLock.cupcake;


import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.provider.Settings.SettingNotFoundException;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.CheckBox;
import android.widget.Toast;

//getSharedPreferences("myLockAutoUnlockprefs", Context.MODE_WORLD_READABLE + Context.MODE_WORLD_WRITEABLE);

public class SettingsActivity extends Activity {
	
	public boolean triedstart = false;
		
	public boolean persistentNotif = false;
    
    public boolean security = false;
    public boolean shakewake = false;
    
    public boolean guard = false;
    
    public boolean WPlockscreen = false;
    
    public boolean active = false;

    public CheckBox toggle;
    public CheckBox secured; 
    	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
       
      getPrefs();
     
      //TODO
      //case - settings is launched and still has active pref from service being killed in some fashion
      //in this instance we want to call a duplicate start command, this ensures it is now running
      //to match checkbox
            
     toggle = (CheckBox) findViewById(R.id.activeBox);

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
              
             
       final CheckBox shake = (CheckBox)findViewById(R.id.shakeBox);

       shake.setChecked((shakewake));        
               
       shake.setOnClickListener(new OnClickListener() {

                   public void onClick(View v) {
                	   SharedPreferences set = getSharedPreferences("myLock", 0);
                	   SharedPreferences.Editor editor = set.edit(); 
                	   
                	   editor.putBoolean("shake", shake.isChecked());

                       // Don't forget to commit your edits!!!
                       editor.commit();            			                       
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
               }
               });
    
    
        
    secured = (CheckBox)findViewById(R.id.secureBox);
    
    secured.setChecked((security));        
    
    secured.setOnClickListener(new OnClickListener() {

                public void onClick(View v) {
             	   //hint to the observant user
                	Toast.makeText(SettingsActivity.this, "Please change security from system prefs", Toast.LENGTH_LONG).show();
                	secured.setChecked(!secured.isChecked());
                	//undo the change
                }
            });
 }
    
    public void getPrefs() {
    	SharedPreferences settings = getSharedPreferences("myLock", 0);
    	
        
        shakewake = settings.getBoolean("shake", false);
        
        active = settings.getBoolean("serviceactive", false);
        
        guard = settings.getBoolean("slideGuard", false);
        
        
        //if service is not active, force security setting based on system
        //if it is active we are going to rely on the pref setting
                
        if (!active) {
        	
        security = getPatternSetting();
        
        SharedPreferences.Editor e = settings.edit();
        e.putBoolean("security", security);
        e.commit();
        }
        else security = settings.getBoolean("security", false);
    }
    
    public boolean getPatternSetting() {
    	int patternsetting = 0;

        try {
                patternsetting = android.provider.Settings.System.getInt(getContentResolver(), android.provider.Settings.System.LOCK_PATTERN_ENABLED);
        } catch (SettingNotFoundException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
        }
        
        boolean s = (patternsetting == 1);
        
        return s;
    }
    
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.options_menu, menu);
        return true;
    }
    
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case R.id.idlesetup:
        	Intent setup = new Intent();
        	setup.setClassName("i4nc4mp.myLock.cupcake", "i4nc4mp.myLock.cupcake.IdleSetup");
        	try {
        		startActivity(setup);
        	}
        	catch (ActivityNotFoundException e) {
        		//Toast.makeText(SettingsActivity.this, "Please download Idle Lock addon", Toast.LENGTH_LONG).show();
        	}
            return true;
        }
        return false;
    }
    
    @Override
    public void onResume() {
    	super.onResume();
    	
    	getPrefs();
    	toggle.setChecked(active);
    	secured.setChecked(security);
    }
    
    /*start and stop methods rely on pref and are only used by toggle button*/
    private void startService(){
   			Intent i = new Intent();
   			
   			i.setClassName("i4nc4mp.myLock.cupcake", "i4nc4mp.myLock.cupcake.AutoDismiss");
   			startService(i);
   			Log.d( getClass().getSimpleName(), "startService()" );
   		}
    
    private void stopService() {
			Intent i = new Intent();
			i.setClassName("i4nc4mp.myLock.cupcake", "i4nc4mp.myLock.cupcake.AutoDismiss");
			stopService(i);
			Log.d( getClass().getSimpleName(), "stopService()" );
    }
}


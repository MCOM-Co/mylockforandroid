package i4nc4mp.myLock;


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
		
	private boolean persistentNotif = false;	
    
    private boolean shakewake = false;
    
    private boolean guard = false;
    
    private boolean WPlockscreen = false;
    
    
    private boolean security = false;
    
    private boolean enabled = false;
    
    private boolean active = false;	
    

    public CheckBox toggle;
    public CheckBox secured;
    	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settingsactivity);
            
        getPrefs();
        //moving Prefs into a real pref activity
        
     toggle = (CheckBox)findViewById(R.id.activeBox);
        
      toggle.setOnClickListener(new OnClickListener() {
    	  public void onClick(View v){
        		enabled = toggle.isChecked();
        		ManageMediator.invokeToggler(getApplicationContext(), enabled);
        		}
          });    
    
    final CheckBox wpbox = (CheckBox)findViewById(R.id.wplock);
    
    wpbox.setChecked((WPlockscreen));        
    
    wpbox.setOnClickListener(new OnClickListener() {

 	   public void onClick(View v) {
            SharedPreferences set = getSharedPreferences("myLock", 0);
            SharedPreferences.Editor editor = set.edit(); 
            
              
            if (enabled) {
            	Toast.makeText(SettingsActivity.this, "Please disable myLock first", Toast.LENGTH_LONG).show();
            	wpbox.setChecked(!wpbox.isChecked());
            }
            //FIXME - we will have a radiogroup that hides when enabled. 
            //user will understand when it reappears on disable that only select mode during disable
            else {
            editor.putBoolean("wallpaper", wpbox.isChecked());
            //WPlockscreen = wpbox.isChecked();
            // Don't forget to commit your edits!!!
            editor.commit();
            }

            }
    });
    
    secured = (CheckBox)findViewById(R.id.secureBox);       
    
    secured.setOnClickListener(new OnClickListener() {

                public void onClick(View v) {
             	   //hint to the observant user
                	Toast.makeText(SettingsActivity.this, "Please change security from system prefs", Toast.LENGTH_LONG).show();
                	secured.setChecked(!secured.isChecked());
                	//undo the change
                }
            });
 }
    
    public void getStatus() {
    	SharedPreferences settings = getSharedPreferences("myLock", 0);
    	
    	enabled = settings.getBoolean("enabled", false);//only set by toggler
    	
    	active = ManageMediator.bind(getApplicationContext());
        
        if (enabled && !active) {
        	//toggleService(true);
        	//active = true;
        	Log.e("pref getStatus","mediator bind failed");
        	 Toast.makeText(SettingsActivity.this, "error: myLock service not found", Toast.LENGTH_LONG).show();
        }
        
        //if service is not active, force security setting based on system
        //if it is active we are going to rely on the pref setting
                
        if (!enabled) {
        	
        security = getPatternSetting();
        
        SharedPreferences.Editor e = settings.edit();
        e.putBoolean("security", security);
        e.commit();
        }
        else security = settings.getBoolean("security", false);	
    }
    
    //TODO about to be converted to a pref activity Menu entry
    public void getPrefs() {
    	SharedPreferences settings = getSharedPreferences("myLock", 0);
    	
        persistentNotif = settings.getBoolean("FG", false);
        
        WPlockscreen = settings.getBoolean("wallpaper", false);
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
        	setup.setClassName("i4nc4mp.myLock.idleLock", "i4nc4mp.myLock.idleLock.IdleSetup");
        	try {
        		startActivity(setup);
        	}
        	catch (ActivityNotFoundException e) {
        		Toast.makeText(SettingsActivity.this, "Please download Idle Lock addon", Toast.LENGTH_LONG).show();
        	}
            return true;
        }
        return false;
    }
    
    @Override
    public void onResume() {
    	super.onResume();
    	
    	getStatus();
    	
    	toggle.setChecked(enabled);
    	secured.setChecked(security);
    }
    
    @Override
    public void onBackPressed() {
    	finish();
    }
}


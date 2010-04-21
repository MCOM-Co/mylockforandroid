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

//Most important top level state items handled here
//Does user want the service active?
//Is pattern security active?

//User launches individual prefs screens from menu. 
//We want security and service state to be understood by user in the primary setup screen

public class SettingsActivity extends Activity {
    
    private boolean security = false;
    
    private boolean enabled = false;
    
    private boolean active = false;
    
    
    private CheckBox toggle;
    private CheckBox secured; 
    	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
            
     toggle = (CheckBox) findViewById(R.id.activeBox);
        
      toggle.setOnClickListener(new OnClickListener() {
          	public void onClick(View v){
          		enabled = toggle.isChecked();
          		toggleService(enabled);
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
    
    public void getPrefs() {
    	//Is service Active? Should it be active?
    	//Is security pattern on? Was myLock started with pattern enabled?
    	
    	SharedPreferences settings = getSharedPreferences("myLock", 0);
            
        enabled = settings.getBoolean("enabled", false);
        
		//ManageMediator m = new ManageMediator();
        //active = m.bind(getApplicationContext());
        //m = null;
        
        active = ManageMediator.bind(getApplicationContext());
        
        if (enabled && !active) {
        	toggleService(true);
        	active = true;
        }
        //start if we aren't active & user's last known intention was enabled
        //case would be a crash or task killer/force stop
        //user will see a toast that we have done the enable
    
        if (!enabled) {//only check for security change while not enabled
        	
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
    	int id = item.getItemId();
    	Intent setup = new Intent();
        if(id ==  R.id.idlesetup) setup.setClassName("i4nc4mp.myLock.cupcake", "i4nc4mp.myLock.cupcake.IdleSetup");
        else if (id == R.id.mainprefs) setup.setClassName("i4nc4mp.myLock.cupcake", "i4nc4mp.myLock.cupcake.MainPreferenceActivity");
        
        	try {
        		startActivity(setup);
        	}
        	catch (ActivityNotFoundException e) {
        		return false;
        	}
        	return true;
    }
    
    @Override
    public void onResume() {
    	super.onResume();
    	
    	getPrefs();
    	toggle.setChecked(enabled);
    	//in case toggler got invoked by widget or plug-in
    	secured.setChecked(security);
    }
    
    private void toggleService(boolean on){
   			Intent i = new Intent();
   			
   			i.setClassName("i4nc4mp.myLock.cupcake", "i4nc4mp.myLock.cupcake.Toggler");
   			i.putExtra("i4nc4mp.myLock.TargetState", on);
   			startService(i);
   			Log.d( getClass().getSimpleName(), "startService()" );
   		}    
}


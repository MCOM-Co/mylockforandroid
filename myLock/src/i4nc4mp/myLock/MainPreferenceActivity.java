package i4nc4mp.myLock;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.Preference.OnPreferenceClickListener;
import android.provider.Settings.SettingNotFoundException;
import android.util.Log;
import android.widget.Toast;

public class MainPreferenceActivity extends PreferenceActivity {
	private SharedPreferences myprefs;
	
    private boolean security = false;
    
    private boolean enabled = false;
    
    private boolean active = false;
	
	
        @Override
        protected void onCreate(Bundle savedInstanceState) {
                super.onCreate(savedInstanceState);
                                
                addPreferencesFromResource(R.xml.mainpref);
                //instead of a layout we set ourselves to a pref tree
                
                getPreferenceManager().setSharedPreferencesName("myLock");
                //tell the prefs persisted to go in the already existing pref file
                
                //Next, we have to set some things up just like we did in the old settings activity
                //we use findPreference instead of findViewById
                
                CheckBoxPreference toggle = (CheckBoxPreference) findPreference("enabled");
                if (toggle == null) Log.e("pref activity","didn't find preference");
                else {
                	toggle.setOnPreferenceClickListener(new OnPreferenceClickListener() {

    					@Override
    					public boolean onPreferenceClick(Preference preference) {
    						if(preference.getKey().equals("enabled")) {
    							Context mCon = getApplicationContext();
    						
    							boolean state = ManageMediator.bind(mCon);
    				        	if(!state) ManageMediator.startService(mCon);
    				        	else ManageMediator.stopService(mCon);
    							
    							return true;
    					}
    						else return false;
                    	
    					};
                    });
                }
                
        }
        
        @Override
        protected void onStart() {
        	super.onStart();
        	
        	getPrefs();
        }
        
        private void getPrefs() {
        	//set all the prefs in activity to the stored values - special handling for security
        	myprefs = getSharedPreferences("myLock", 0);
        	
        	((CheckBoxPreference) findPreference("FG")).setChecked(myprefs.getBoolean("FG", false));
        	((CheckBoxPreference) findPreference("slideGuard")).setChecked(myprefs.getBoolean("slideGuard", false));
        	
        	
        	getStatus();//check out how we should be displaying security
        	
        	((CheckBoxPreference) findPreference("enabled")).setChecked(enabled);
        	((CheckBoxPreference) findPreference("security")).setChecked(security);
        }
        
        private void getStatus() {
        	
        	enabled = myprefs.getBoolean("enabled", false);
        	
        	active = ManageMediator.bind(getApplicationContext());
            
            if (enabled && !active) {
            //case of crashed service or improper setting persistence
            	enabled = false;
            	Log.e("pref getStatus","mediator bind failed");
            	Toast.makeText(MainPreferenceActivity.this, "error: myLock service not found", Toast.LENGTH_LONG).show();
            }
            
            //Show security state - find actual system state while not enabled                  
            if (!enabled) security = getPatternSetting();
            //if this is a change it's automatically persisted (thanks pref activity)
            
            else security = myprefs.getBoolean("security", false);
            //necessary to show the pref state while active, since we suppress system pref
        }
        
        private boolean getPatternSetting() {
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
       
}
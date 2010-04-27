package i4nc4mp.myLock;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import android.provider.Settings.SettingNotFoundException;
import android.util.Log;

//what we need is when this is launched, a short handler is spawned, and the checkbox for service status
//is replaced by a little "refreshing" spin-wheel until the status of mediator is determined

public class MainPreferenceActivity extends PreferenceActivity {
	private SharedPreferences myprefs;
	
    private boolean security = false;
    
    private boolean enabled = false;
    
    private boolean active = false;
	    
    Handler serviceHandler;
    Task verifyBindTask = new Task();
	
        @Override
        protected void onCreate(Bundle savedInstanceState) {
                super.onCreate(savedInstanceState);
                                
                addPreferencesFromResource(R.xml.mainpref);
                //instead of a layout we set ourselves to a pref tree
                
                getPreferenceManager().setSharedPreferencesName("myLock");
                //tell the prefs persisted to go in the already existing pref file
                
                serviceHandler = new Handler();
                
                //Next, we have to set some things up just like we did in the old settings activity
                //we use findPreference instead of findViewById
                
                CheckBoxPreference toggle = (CheckBoxPreference) findPreference("enabled");
                if (toggle == null) Log.e("pref activity","didn't find toggle item");
                else {
                	toggle.setOnPreferenceClickListener(new OnPreferenceClickListener() {

    					@Override
    					public boolean onPreferenceClick(Preference preference) {
    						if(preference.getKey().equals("enabled")) {
    							Context mCon = getApplicationContext();
    						
    							//Check the state that is verified during onStart
    							if(!enabled) ManageMediator.startService(mCon);
    				        	else ManageMediator.stopService(mCon);
    							
    							return true;
    					}
    						else return false;
                    	
    					};
                    });
                }
                
                
                ListPreference mPref = (ListPreference) findPreference("mode");
                if (mPref == null) Log.e("pref activity","didn't find mode item");
                else {
                mPref.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {

					@Override
					public boolean onPreferenceChange(Preference preference,
							Object newValue) {
						String v = (String) newValue;
						Log.v("changed mode","the new mode is " + v);
						
						int n = Integer.parseInt(v);
						
						updateMode(n);
						return true;
					}
                });
                }
                
        }
        
        @Override
        protected void onStart() {
        	super.onStart();
        	
        	getPrefs();
        }
        
        private void getPrefs() {
        	//we need to show the user's existing prefs and get status for service & security
        	myprefs = getSharedPreferences("myLock", 0);
        	
        	//These 2 and the mode will go by strict pref reading
        	((CheckBoxPreference) findPreference("FG")).setChecked(myprefs.getBoolean("FG", false));
        	((CheckBoxPreference) findPreference("slideGuard")).setChecked(myprefs.getBoolean("slideGuard", false));
        	
        	int m = 4;
        	try {
        		m = Integer.parseInt(myprefs.getString("mode", "0"));
        	}
        	catch (NumberFormatException x) {
        		//I don't care
        		m=4;
        	}
        	        	
        	if (m!=4) updateMode(m);
        	
        	
        	//Security and Service statuses need extra handling
        	enabled = myprefs.getBoolean("enabled", false);
        	
        	
        	if (enabled) {
        		//verify that the service is active, we will get true if we held the bind as expected
        		active = ManageMediator.bind(getApplicationContext());
            
        		//necessary to show the pref state while active, since we suppress system pref
        		if (active)	updateStatus(true);
        		else serviceHandler.postDelayed(verifyBindTask, 100L);
        		//we have to wait then doublecheck to allow time for bind to execute        		
        	}
        	else updateStatus(false);
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
        
        //check security and post status results
        //param is so we can pass the determined service state
        //we have to base security handling on that state
        private void updateStatus(boolean on) {
        	if (on) {
        		enabled = true;
        		security = myprefs.getBoolean("security", false);
        	}
        	else {
        		enabled = false;
        		security = getPatternSetting();
        		//find actual system state while not enabled so we can store to pref
                //if this is a change it's automatically persisted
        	}
        	((CheckBoxPreference) findPreference("enabled")).setChecked(enabled);
        	((CheckBoxPreference) findPreference("security")).setChecked(security);
        }
        
        private void updateMode(int m) {
            String s = new String();
            
            switch (m) {
            	case ManageMediator.MODE_BASIC:
            		s = "Basic Auto-Unlock";
            		break;
            	case ManageMediator.MODE_HIDDEN:
            		s = "Hide Lockscreen & Auto-Unlock";
            		break;
            	case ManageMediator.MODE_ADVANCED:
            		s = "Advanced Full Disable";
            		break;
            }
            
            findPreference("mode").setSummary(s);
        }
        
        class Task implements Runnable {
            public void run() {
            	updateStatus(ManageMediator.serviceActive(getApplicationContext()));
            	}
    	}
        
        @Override
        protected void onDestroy() {
        	super.onDestroy();
        	
        	serviceHandler.removeCallbacks(verifyBindTask);

    		serviceHandler = null;
        }
       
}
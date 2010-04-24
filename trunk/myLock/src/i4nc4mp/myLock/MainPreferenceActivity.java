package i4nc4mp.myLock;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.view.View;

//Design goals for this
//We need to have prefs that hide when the service is running (we hold the static bind)
//Those prefs are the mode selection, mainly (for 1.4)

//I want the toggle widget layout to also show as the enable element. 
//I think we can create a layout that houses toggle button and import it in the preference
//this may be outside the scope of preference but i think it could be done

//for now, we have a separate settings screen which handles mediator mode, status, and security
//(for user awareness of the need to have myLock disabled to make changes to those key factors)

public class MainPreferenceActivity extends PreferenceActivity {
	SharedPreferences myprefs;
	
        @Override
        protected void onCreate(Bundle savedInstanceState) {
                super.onCreate(savedInstanceState);
                myprefs = getSharedPreferences("myLock", 0);
                
                addPreferencesFromResource(R.xml.mainpref);
                getPreferenceManager().setSharedPreferencesName("myLock");
                //tell the prefs persisted to go in the already existing pref file
                //this way we aren't juggling our old file & the "default" normally used by pref activity
                
                //CheckBoxPreference toggle = (CheckBoxPreference) findPreference("toggle");
                //toggle.setChecked(myprefs.getBoolean("enabled", false));
                //CompoundButton icon = (CompoundButton) findViewById(toggle.getWidgetLayoutResource());
                
                //if (toggle.isChecked()) icon.setButtonDrawable(R.drawable.widg_on_icon);
                //else icon.setButtonDrawable(R.drawable.widg_off_icon);
                
                
                
                //we need to set a listener
                /*
                toggle.setOnPreferenceClickListener(new OnPreferenceClickListener() {

					@Override
					public boolean onPreferenceClick(Preference preference) {
						if(preference.getKey().equals(toggle.getKey())) {
						Toast.makeText(MainPreferenceActivity.this, "I Am Toggle", Toast.LENGTH_LONG).show();
						return true;
					}
						else return false;
                	
					};
                });
                */
        }
        
        public void iAmToggle(View v) {
        	Context mCon = getApplicationContext();
        	//Toast.makeText(MainPreferenceActivity.this, "I Am Toggle", Toast.LENGTH_LONG).show();
        	boolean state = ManageMediator.bind(mCon);//myprefs.getBoolean("enabled", false);
        	if(!state) ManageMediator.startService(mCon);
        	else ManageMediator.stopService(mCon);
        	//ManageMediator.toggleService(getApplicationContext(), !state);
        }
                
       
}
package i4nc4mp.myLock.phone;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.PreferenceActivity;

public class PrefActivity extends PreferenceActivity {
	
	@Override
    protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
                            
            addPreferencesFromResource(R.xml.pref);
            
            getPreferenceManager().setSharedPreferencesName("myLockphone");
	}
	
	@Override
    protected void onStart() {
    	super.onStart();
    	
    	getPrefs();
    }
    
    private void getPrefs() {
    	//we need to show the user's existing prefs, this isn't done automatically by the activity
    	SharedPreferences myprefs = getSharedPreferences("myLockphone", 0);
    	
    	((CheckBoxPreference) findPreference("callPrompt")).setChecked(myprefs.getBoolean("callPrompt", false));
    	//((CheckBoxPreference) findPreference("rejectEnabled")).setChecked(myprefs.getBoolean("rejectEnabled", false));
    	((CheckBoxPreference) findPreference("cameraAccept")).setChecked(myprefs.getBoolean("cameraAccept", false));
    	
    	((CheckBoxPreference) findPreference("touchLock")).setChecked(myprefs.getBoolean("touchLock", false));
}
}
package i4nc4mp.myLock.cupcake;

import android.os.Bundle;
import android.preference.PreferenceActivity;

public class MainPreferenceActivity extends PreferenceActivity {
        @Override
        protected void onCreate(Bundle savedInstanceState) {
                super.onCreate(savedInstanceState);
                addPreferencesFromResource(R.xml.preferences);
                getPreferenceManager().setSharedPreferencesName("myLock");
                //tell the prefs persisted to go in the already existing pref file
                //this way we aren't juggling our old file & the "default" normally used by pref activity
        }
}
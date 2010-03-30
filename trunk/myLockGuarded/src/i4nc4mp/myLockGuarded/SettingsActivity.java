package i4nc4mp.myLockGuarded;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.Toast;

//settings - we need a few branches. we will have 3 modes - (radio selector)

//Basic lockscreen disable - (a2c, pre 2.0 keyguard manager interaction)
//		====DO WE NEED THIS MODE?

//Utility lockscreen - dismiss_keyguard activity, allows button customization dialogue
/*User Types
	+any key instant unlock (IMPATIENT: never want guarding) they should just use screen mode widget
	+customizable lockscreen (MODERATE: may want a specific key or all keys to wake lockscreen but may want instant unlock too)
	+slide to unlock + util lockscreen, with any key wake (WORRIERS: never want instant unlock)
 */

//Secure mode - show_when_locked activity, no button customization. just gives the utility lockscreen on top of pattern mode
//=====special type of user who has secure mode and wants to access the util lock before having to do pattern

//=====most reasonable moderate = want pattern to come on if phone is left idle for too long
//If not in secure mode, idle timeout option is set. User enters a number of minutes. if 0, no timeout gets enabled
//when set to 1 or more, idle timer will run


public class SettingsActivity extends Activity {
	
	public boolean triedstart = false;
	
	
	public boolean persistentNotif = true;
    
    //public boolean customLock = false;
    //public boolean customLock = true;
    
    public boolean boot = false;
    //public boolean shakewake = false;
    
    public boolean secure = false;
    	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settingsactivity);
       
      getPrefs();//grabs our user's current settings for startup commands
      
       Button toggle = (Button)findViewById(R.id.toggleButton);
        
       toggle.setOnClickListener(new OnClickListener() {
          	public void onClick(View v){
          		TryToggle();
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
               			i.setClassName("i4nc4mp.myLockGuarded", "i4nc4mp.myLockGuarded.ShakeWakeupService");
               			
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
       
       final CheckBox security = (CheckBox)findViewById(R.id.secureBox);
       
       security.setChecked((secure));        
       
       security.setOnClickListener(new OnClickListener() {

                   public void onClick(View v) {
                	   SharedPreferences set = getSharedPreferences("myLock", 0);
                	   SharedPreferences.Editor editor = set.edit(); 
                	   
                	   editor.putBoolean("secure", security.isChecked());

                       // Don't forget to commit your edits!!!
                       editor.commit();
                 
                       //The current mode needs to be stopped
                       stopService();//it will go by the existing pref
                       triedstart = false;//ensures next toggle command will start the new mode
                       Toast.makeText(SettingsActivity.this, "Press toggle to complete mode change", Toast.LENGTH_SHORT).show();
                       secure = !secure;//toggle it locally for reference of next toggle press

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
               
               startService();//call start service, so it can react to the change
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
    }
    
    public void getPrefs() {
    	SharedPreferences settings = getSharedPreferences("myLock", 0);
    	
    	/*if (settings == null) {
        	Log.v("getprefs","failed due to settings null. using defaults.");
        	return;        
        }*/
    	
        persistentNotif = settings.getBoolean("FG", false);
        //customLock = settings.getBoolean("welcome", false);
        boot = settings.getBoolean("boot", false);
        //shakewake = settings.getBoolean("ShakeWakeup", false);
        secure = settings.getBoolean("secure", false);
    }
    
    /*start and stop methods rely on pref and are only used by toggle button*/
    private void startService(){
   			Intent i = new Intent();
   			//if (!customLock) i.setClassName("i4nc4mp.myLockGuarded", "i4nc4mp.myLockGuarded.LockSkipService");
   			if (secure) i.setClassName("i4nc4mp.myLockGuarded", "i4nc4mp.myLockGuarded.SecureLockService");
   			else i.setClassName("i4nc4mp.myLockGuarded", "i4nc4mp.myLockGuarded.GuardService");
   			startService(i);
   			Log.d( getClass().getSimpleName(), "startService()" );
   		}
    
    private void stopService() {
			Intent i = new Intent();
			//if (!customLock) i.setClassName("i4nc4mp.myLockGuarded", "i4nc4mp.myLockGuarded.LockSkipService");
			if (secure) i.setClassName("i4nc4mp.myLockGuarded", "i4nc4mp.myLockGuarded.SecureLockService");
			else i.setClassName("i4nc4mp.myLockGuarded", "i4nc4mp.myLockGuarded.GuardService");
			stopService(i);
			Log.d( getClass().getSimpleName(), "stopService()" );
    }
    
    
   //TODO get binding working or a way to look for whether the service is running so I can use a toggleButton instead
    private void TryToggle() {
    	if (!triedstart) {
    		startService();
    		Toast.makeText(SettingsActivity.this, "Started up... press toggle again to stop myLock", Toast.LENGTH_SHORT).show();
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


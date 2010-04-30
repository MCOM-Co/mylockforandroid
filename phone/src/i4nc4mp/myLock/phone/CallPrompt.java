package i4nc4mp.myLock.phone;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

//I aint no dummy (prompt)

public class CallPrompt extends Activity {

	private boolean success = false;
	
	public static void launch(Context mCon) {
		
		Intent prompt = new Intent(mCon,CallPrompt.class);

    	prompt.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
    			| Intent.FLAG_ACTIVITY_NO_USER_ACTION);
    	
    	mCon.startActivity(prompt);
	}
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
				
		
		setContentView(R.layout.main);
			
		
		Button answer = (Button) findViewById(R.id.mid);
		
		answer.setOnClickListener(new OnClickListener() {
          	public void onClick(View v){
          		answer();
          	}
		});
		
		//I would actually like to have the power key reject
		//can't figure out if we can "cause" reject
		//In emulator the back key causes reject.
		//So, we might try passing back along as false, manually in the key handle method
		//this would pass it to the system so if droid software is normally stopping handling, we could ignore
		
	}
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
		
		unregisterReceiver(PhoneState);
	}
	
	@Override
	protected void onStart() {
		super.onStart();
		Log.v("call prompt","starting");
		
		IntentFilter ph = new IntentFilter (TelephonyManager.ACTION_PHONE_STATE_CHANGED);
		
		registerReceiver(PhoneState, ph);
	}
	
	@Override
	protected void onStop() {
		super.onStop();
		
		Log.v("prompt onStop","verifying success");
		
		if (!success) launch(getApplicationContext());
	}
	
	void answer() {
		success = true;
		
		Intent answer = new Intent(Intent.ACTION_MEDIA_BUTTON);

  		//most certainly does work
		//special thanks the auto answer open source app
		//which demonstrated this answering functionality
  		answer.putExtra(Intent.EXTRA_KEY_EVENT, new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_HEADSETHOOK));
  		sendOrderedBroadcast(answer, null);
  		moveTaskToBack(true);
  		finish();
	}
	
	//doesn't work
	void reject() {
		Intent reject = new  Intent(Intent.ACTION_MEDIA_BUTTON);
		
		//reject.putExtra(Intent.EXTRA_KEY_EVENT, new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_ENDCALL));
		reject.putExtra(Intent.EXTRA_KEY_EVENT, new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_BACK));
		
		sendOrderedBroadcast(reject, null);
	}
	
	@Override
	public void onBackPressed() {
		super.onBackPressed();
		success = true;
	}
	
	//we don't want to exist after phone changes to active state or goes back to idle
	BroadcastReceiver PhoneState = new BroadcastReceiver() {
		
		
		@Override
		public void onReceive(Context context, Intent intent) {
			if (!intent.getAction().equals("android.intent.action.PHONE_STATE")) return;
			String state = intent.getStringExtra(TelephonyManager.EXTRA_STATE);
			if (state.equals(TelephonyManager.EXTRA_STATE_OFFHOOK) || state.equals(TelephonyManager.EXTRA_STATE_IDLE)) {
				if (!success && !isFinishing()) {
					//no known intentional dismissal and not already finishing
					//need to finish to avoid handing out after missed calls
					Log.v("call start or return to idle","no user input success - closing the prompt");
					success = true;//so re-start won't fire
					finish();
				}
			}

			return;
	    		
		}};
	
	//let's allow the camera press to accept this call
	@Override
    public boolean dispatchKeyEvent(KeyEvent event) {
		switch (event.getKeyCode()) {
		/*case KeyEvent.KEYCODE_BACK:
			//moveTaskToBack(true);
	  		//finish();
			reject();
			return false;*/
		
			//Drop the back key through to the system - this does not cause reject as expected
			//it's possible the phone screen is checking if it has focus as a condition
			
			/*
			 * 04-28 17:48:28.034: INFO/ActivityManager(53): Starting activity:
			 * 		Intent { act=android.intent.action.MAIN flg=0x10840000 cmp=com.android.phone/.InCallScreen }
			 *
			 * 
			 * 04-28 17:43:39.985: DEBUG/InCallScreen(200): onBackPressed()...
			 * 04-28 17:43:39.995: DEBUG/InCallScreen(200): BACK key while ringing: reject the call
			 */
			
		case KeyEvent.KEYCODE_FOCUS:
			return true;
			//this event occurs - if passed on, phone retakes focus
			//so let's consume it to avoid that outcome
		case KeyEvent.KEYCODE_CAMERA:
			if (getSharedPreferences("myLockphone", 0).getBoolean("cameraAccept", false))
					answer();
			return true;
		default:
			break;
		}
		return super.dispatchKeyEvent(event);
	}
}
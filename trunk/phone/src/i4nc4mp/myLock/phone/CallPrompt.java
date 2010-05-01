package i4nc4mp.myLock.phone;

import android.app.Activity;
import android.app.ActivityManager;
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
	
	private ActivityManager am;
	
	public static void launch(Context mCon) {
		
		Intent prompt = new Intent(mCon,CallPrompt.class);

    	prompt.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
    			| Intent.FLAG_ACTIVITY_NO_USER_ACTION);
    	
    	mCon.startActivity(prompt);
	}
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		if (!getSharedPreferences("myLockphone", 0).getBoolean("callPrompt", true)) {
			
			setContentView(R.layout.cancelhint);
			//just the hint to user for camera accept and back to get to sliders
			//only camera can answer in this case
			
			//it's a workaround
			//I don't know how to make a window that doesn't block the sliders
			//such that it can still get key events
		}
		else {
		
			
			setContentView(R.layout.answerprompt);
			
			Button answer = (Button) findViewById(R.id.answer);
			
			answer.setOnClickListener(new OnClickListener() {
	          	public void onClick(View v){
	          		answer();
	          	}
			});
			
			//setContentView(R.layout.main);
			
		
			/*
		Button answer = (Button) findViewById(R.id.answerbutton);
		
		answer.setOnClickListener(new OnClickListener() {
          	public void onClick(View v){
          		answer();
          	}
		});
		
		Button reject = (Button) findViewById(R.id.rejectbutton);
		
		reject.setOnClickListener(new OnClickListener() {
          	public void onClick(View v){
          		reject();
          	}
		});*/
			}
		
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
		//We are sneaky.
		//We can relaunch if phone lagged in starting, so then tries to cancel our visible lifecycle
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
	
	//Mr. Tedd's discovered workaround.
	//Not a surgical strike but essentially effective
	void reject() {
		success = true;
		
		am = (ActivityManager)getSystemService(ACTIVITY_SERVICE);
		am.restartPackage("com.android.providers.telephony");
        am.restartPackage("com.android.phone");
        
        //requires permission to restart packages
        
        moveTaskToBack(true);
  		finish();
	}
	
	//i think this isn't in 1.5
	@Override
	public void onBackPressed() {
		super.onBackPressed();
		success = true;
	}
	
	//we don't want to exist after phone changes to active state or goes back to idle
	//we also don't want to rely on this receiver to close us after success
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
package i4nc4mp.myLock.phone;

import android.app.Activity;

import android.content.Intent;
import android.os.Bundle;
import android.preference.PreferenceManager;

import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

public class CallPrompt extends Activity {
	
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
		
	}
	
	
	void answer() {
		Intent answer = new Intent(Intent.ACTION_MEDIA_BUTTON);

  		//most certainly does work
		//special thanks the auto answer open source app
		//which demonstrated this answering functionality
  		answer.putExtra(Intent.EXTRA_KEY_EVENT, new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_HEADSETHOOK));
  		sendOrderedBroadcast(answer, null);
  		moveTaskToBack(true);
  		finish();
	}
	
	//let's allow the camera press to accept this call
	@Override
    public boolean dispatchKeyEvent(KeyEvent event) {
		switch (event.getKeyCode()) {
		case KeyEvent.KEYCODE_FOCUS:
			return true;
			//this event occurs - if passed on, phone retakes focus
			//so let's consume it to avoid that outcome
		case KeyEvent.KEYCODE_CAMERA:
			if (PreferenceManager.getDefaultSharedPreferences(getApplicationContext())
					.getBoolean("cameraAccept", false)) answer();
			return true;
		default:
			break;
		}
		return super.dispatchKeyEvent(event);
	}
}
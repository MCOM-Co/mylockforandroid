package i4nc4mp.myLock;

import android.app.Activity;

import android.content.Intent;
import android.os.Bundle;

import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

public class CallPrompt extends Activity {
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
				
		
		setContentView(R.layout.callprompt);
			
		
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
		Intent new_intent = new Intent(Intent.ACTION_MEDIA_BUTTON);

		//special thanks the auto answer open source app
		//which demonstrated this answering functionality
  		new_intent.putExtra(Intent.EXTRA_KEY_EVENT, new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_HEADSETHOOK));
  		sendOrderedBroadcast(new_intent, null);
  		moveTaskToBack(true);
  		finish();
	}
	
	//let's allow the camera press to accept this call
	@Override
    public boolean dispatchKeyEvent(KeyEvent event) {
		switch (event.getKeyCode()) {
		case KeyEvent.KEYCODE_CAMERA:
			answer();
			return true;
		default:
			break;
		}
		return super.dispatchKeyEvent(event);
	}
}
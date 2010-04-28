package i4nc4mp.myLock.phone;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;

public class DummyPrompt extends Activity {
	//Launched with no history flag. But we're going to try to catch it's on pause or on stop
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.callguard);
		
		Log.v("dummy prompt","on create");		
	}
	
	@Override
	protected void onStart() {
		super.onStart();
		
		Log.v("dummy prompt","start");
	}
	
	@Override
	protected void onPause() {
		super.onPause();
		
		Log.v("dummy prompt","pause");
	}
	
	//We never gain focus, as we get resumed, then paused, then stopped when phone is fully ready
	@Override
	public void onWindowFocusChanged(boolean hasFocus) {
		super.onWindowFocusChanged(hasFocus);
		
		Log.v("dummy prompt","Focus is now " + hasFocus);
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		
		Log.v("dummy prompt","resume");
	}
	
	@Override
	protected void onStop() {
		super.onStop();
		
		Log.v("dummy prompt","stop - launching real Prompt");
		
		CallPrompt.launch(getApplicationContext());
	}
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
		
		Log.v("dummy prompt","on destroy");
	}
}
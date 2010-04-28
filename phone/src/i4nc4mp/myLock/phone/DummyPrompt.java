package i4nc4mp.myLock.phone;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;

public class DummyPrompt extends Activity {
	//Launched with no history flag. But we're going to try to catch it's on pause or on stop
	
	private boolean done = false;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.dummy);
		
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
	
	//Expected to start, resume, pause, get focus, pause, lose focus, then stop
	@Override
	public void onWindowFocusChanged(boolean hasFocus) {
		super.onWindowFocusChanged(hasFocus);
		
		Log.v("dummy prompt","Focus is now " + hasFocus);
		
		//Problem case, create is delayed
		//What we get is create, start, resume, then focus
		//means phone started too fast and we are over it
		//pressing back completes (stops us)
		
		/*
		if (hasFocus && !done) {
			done = true;
			CallPrompt.launch(getApplicationContext());
			moveTaskToBack(true);
		}*/
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
		
		if (!done) {
			done = true;
			CallPrompt.launch(getApplicationContext());
			moveTaskToBack(true);
		}
	}
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
		
		Log.v("dummy prompt","on destroy");
	}
}
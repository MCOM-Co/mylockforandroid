package i4nc4mp.myLock.phone;

import android.app.Activity;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;

/*
 * 
 * Normal case (always faster than 2000ms total)
 * 	A - Service starts, sending dummy start intent
 * 	B - Dummy onCreate, start, sometimes resume/pause with focus gain/loss
 *  C - Phone begins visible stage, causing onStop in Dummy
 *  D - Dummy onStop - invokes the Launch of CallPrompt
 *  E - Task execution - still tries to invoke Launch but CallPrompt is singleInstance
 *  Service is stopped.
 *  
 *  
 * Abnormal case
 * 	A - Service starts, sending dummy start intent
 * 	C - Phone starts visible lifecycle abnormally fast, ahead of the Dummy start intent processing
 * 	B - Dummy onCreate, starts, gains focus. It's waiting for a stop that will never come
 * 	E - Task execution - invokes the Launch of CallPrompt, service is stopped.
 *  D - Dummy onStop- still tries to invoke Launch but CallPrompt is singleInstance
 *
 *
 *User feedback indicates that the abnormal case is the more often real-world outcome
 *Phone launches slow in emulator but usually fast in the device
 */


//Not in use --- no longer necessary
//but great example of a way to streamline the other Mediator/Activity pairings
//the inner class activity is a perfect setup because we gain simple access to static members of the mediator
public class PromptMediator extends Service {

	private static Handler serviceHandler; 
	//the static handler can be synchronized to the instance of the dummy
	//that's why we have it as an inner class
	//this is the simplest way to synchronize the results
	//much easier than sending system broadcasts or maintaining pref file keys
	
	//the drawback is we have a delay in creating the dummy...
	
	private int count = 0;
    private Context mCon;
	    
    private boolean dummyStarted = false;
    private boolean dummySuccess = false;
    
    //phone may beat the dummy to visible phase
    
	@Override
	public IBinder onBind(Intent intent) {
		// TODO Auto-generated method stub
		return null;
	}
	
	@Override
	public void onCreate() {
		super.onCreate();
		mCon = getApplicationContext();
		
		Log.v("prompt mediator","created- launching dummy");
		
		initHandler();
		
		//temporarily moved the start of dummy into the actual receiver just for testing
	}
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		super.onStartCommand(intent, flags, startId);
				
		Log.v("prompt mediator","starting timing thread");
				
		waitforDummy();
    	
		return START_STICKY;
	}
    	
    @Override
    public void onDestroy() {
    	super.onDestroy();
    	serviceHandler = null;
    	//what this does is allows dummy to realize the instance of the service is gone
    	//the handler only exists while service is running.

    }
    
    protected void initHandler() {
    	serviceHandler = new Handler() {
			public void handleMessage(Message msg) {
				super.handleMessage(msg);
				
				switch (msg.what) {
					case 0:
						Log.v("handler","dummy started, count is " + count);
						dummyStarted = true;
						break;
					case 1:
						Log.v("handler","dummy success, count is " + count);
						dummySuccess = true;
						stopSelf();
						break;
					case 2:
						if (!dummyStarted || !dummySuccess) {
							Log.v("dummy fallout","dummy result - " + dummyStarted);
							CallPrompt.launch(getApplicationContext());
							stopSelf();
						}						
						break;
					default:
						break;
					}
				}
		};
    }
    
    protected void waitforDummy() {
   	
    	//our thread will essentially wait for the dummy to hit start and stop checkpoints
    	new Thread() {

    	public void run() {
    			try {
        			Thread.sleep(2000);} catch (InterruptedException e) {
        			}
        		serviceHandler.sendMessage(Message.obtain(serviceHandler, 2));
    		
    	}
    	       }.start();
    	    }
    
    public static class Dummy extends Activity {
    	//When the phone app stops us, it means it is time to launch the call prompt
    	//sometimes phone launches quicker, so it gives us focus on top instead of stopping us
    	//that case fixes itself when user hits back to close the dummy
    	
    	private boolean done = false;
    	
    	@Override
    	protected void onCreate(Bundle savedInstanceState) {
    		super.onCreate(savedInstanceState);
    		
    		//setContentView(R.layout.dummy);
    		
    		Log.v("dummy prompt","on create");
    	}
    	
    	@Override
    	protected void onStart() {
    		super.onStart();
    		
    		if (serviceHandler != null) 
    			serviceHandler.sendMessage(Message.obtain(serviceHandler, 0));
    		else {
    			done = true;
    			finish();
    			//need to abort. Null here means the service instance handled fallout
    		}
    		
    		Log.v("dummy prompt","start");
    	}
    	
    	public void iAmDone() {
    		if (!done) {
    			done = true;

    			if (serviceHandler != null)
    				serviceHandler.sendMessage(Message.obtain(serviceHandler, 1));
    			else {
    				return;
        			//need to abort. Null here means the service instance handled fallout
        		}
    			
    			CallPrompt.launch(getApplicationContext());
    			moveTaskToBack(true);
    		}
    	}
    	
    	@Override
    	protected void onPause() {
    		super.onPause();
    		
    		Log.v("dummy prompt","pause");
    	}
    	
    	@Override
    	public void onWindowFocusChanged(boolean hasFocus) {
    		super.onWindowFocusChanged(hasFocus);
    		
    		//Log.v("dummy prompt","Focus is now " + hasFocus);
    		
    		if (hasFocus) Log.v("dummy prompt","gained focus");
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
    		
    		iAmDone();
    	}
    	
    	@Override
    	protected void onDestroy() {
    		super.onDestroy();
    		
    		Log.v("dummy prompt","on destroy");
    	}
    }
}
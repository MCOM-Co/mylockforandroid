package i4nc4mp.myLockGuarded;

import i4nc4mp.myLockGuarded.ManageKeyguard.LaunchOnKeyguardExit;
import i4nc4mp.myLockGuarded.WidgInfo.WidgTable;

import java.util.GregorianCalendar;

import android.app.Activity;
import android.appwidget.AppWidgetHost;
import android.appwidget.AppWidgetHostView;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProviderInfo;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.PowerManager;
import android.os.SystemClock;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.TextView;


//When it is time to exit (by back key or slidingtab) we start a one shot dismiss activity.
//The dismiss activity will load, wait for focus gain, then finish
//Here, we finish in the background after that via callback

//For this lifecycle, we go dormant for any outside event
//such as incoming call ringing, alarm, handcent popup, etc.
//we detect going dormant by losing focus while already paused.
//if focus loss occurs while not paused, it means the user is actively navigating out of the woken lockscreen

//for the exits that occur from navigation, we're forced to use the pre-2.0 exit method
//due to bugs in the overall implementation of the new flags.. there's no way to really allow the navigation exit
//when only show_when_locked is active. i can't seem to make it cooperate with dismissActivity
//because the KG comes back and blocks it
//however, for instant exit, the dismissActivity code is flawless

public class GuardActivity extends Activity {

	Handler serviceHandler;
  Task myTask = new Task();


/* Lifecycle flags */
public boolean starting = true;//flag off after we successfully gain focus. flag on when we send task to back
public boolean finishing = false;//flag on when an event causes unlock, back off when onStart comes in again (relocked)

public boolean paused = false;

public boolean idle = false;

public boolean dormant = false;
//special lifecycle phase- we are waiting in the background for outside event to return focus to us
//an example of this is while a call is ringing. we have to force the state
//because the call prompt acts like a user notification panel nav

public boolean pendingExit = false;
//special lifecycle phase- when we lose focus and aren't paused, we launch a KG pause
//two outcomes, either we securely exit if a pause comes in meaning user is navigating out
//or else we are going to get focus back and re-enable keyguard

public boolean slideWakeup = false;
//we will set this when we detect slideopen, only used with instant unlock (replacement for 2c ver)

public boolean pendingDismiss = false;
//will be set true when we launch the dismiss window for auto and user requested exits
//this ensures focus changes and pause/resume will be ignored to allow dismiss activity to finish

public boolean resurrected = false;
//just to handle return from dormant, avoid treating it same as a user initiated wake

//====Items in the default custom lockscreen



public TextView curhour;
public TextView curmin;

public TextView batt;

//======== widget stuff
private AppWidgetManager mAppWidgetManager;
private AppWidgetHost mAppWidgetHost;



static final int APPWIDGET_HOST_ID = 2037;
//this int identifies you. The Launcher's ID is 1024.
//If you were to implement different hosts that need to be distinguished
//then the ID is a shortcut for passing what you're doing to the correct one 



private AppWidgetHostView widgets[] = new AppWidgetHostView[16];
//the views will be repopulated at oncreate
//we are usually created at sleep, so user doesn't perceive lag

private int[] widgetId = new int[16];
//this is used per instance of the activity
//we build it during onCreate based on the widget entries we get from the database

private int widgCount = 0;
//our reference is increased each widget that is re-populated or added by user
//every activity that pulls the widgets in from database will use this as the local ref of added widgets
//this way when we clear them we can quickly do it without having to read the DB again




private int mRowWidth = 0;
private int mRowHeight = 0;
//we'll just total up the widgets that get added
//441 x 108 is the size that comes in from a 4x1 built in widget (music)





private RelativeLayout[] rows = new RelativeLayout[4];
//here we can actually just create relative layouts to live inside of the parent (id is @widgets)
//what this enables is a workaround so we can still fill the remaining space even if a 2x or taller is placed
//TODO not yet implemented.

//=======================================
  
  //very very complicated business.
  @Override
protected void onCreate(Bundle icicle) {
  super.onCreate(icicle);

  requestWindowFeature(android.view.Window.FEATURE_NO_TITLE);
  getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);
  		//| WindowManager.LayoutParams.FLAG_FULLSCREEN);
  
  updateLayout();
  
  //this is the custom lockscreen stuff
  curhour = (TextView) findViewById(R.id.hourText);
  
  curmin = (TextView) findViewById(R.id.minText);
  
  batt = (TextView) findViewById(R.id.batt);
  
 updateClock();
  /*
  mrewindIcon = (Button) findViewById(R.id.PrevButton); 
  
  mrewindIcon.setOnClickListener(new View.OnClickListener() {
      public void onClick(View v) {
       Intent intent;
       intent = new Intent("com.android.music.musicservicecommand.previous");
       getApplicationContext().sendBroadcast(intent);
       }
    });

  mplayIcon = (Button) findViewById(R.id.PlayToggle); 

  mplayIcon.setOnClickListener(new View.OnClickListener() {
      public void onClick(View v) {
       Intent intent;
       intent = new Intent("com.android.music.musicservicecommand.togglepause");
       getApplicationContext().sendBroadcast(intent);
       
       }
    });
  
  mforwardIcon = (Button) findViewById(R.id.NextButton); 

  mforwardIcon.setOnClickListener(new View.OnClickListener() {
      public void onClick(View v) {
       Intent intent;
       intent = new Intent("com.android.music.musicservicecommand.next");
       getApplicationContext().sendBroadcast(intent);
       }
    });
    */
 
  IntentFilter callbegin = new IntentFilter ("i4nc4mp.myLockGuarded.lifecycle.CALL_START");
  registerReceiver(callStarted, callbegin);  
  
  IntentFilter callpend = new IntentFilter ("i4nc4mp.myLockGuarded.lifecycle.CALL_PENDING");
  registerReceiver(callPending, callpend);
  
  IntentFilter idleFinish = new IntentFilter ("i4nc4mp.myLockGuarded.lifecycle.IDLE_TIMEOUT");
  registerReceiver(idleExit, idleFinish);
          
  serviceHandler = new Handler();
  
//Here we initialize widget manager.
  mAppWidgetManager = AppWidgetManager.getInstance(this);
  mAppWidgetHost = new AppWidgetHost(this, APPWIDGET_HOST_ID);
  mAppWidgetHost.startListening();
  
  MakeWidgets();
}
  
  private void MakeWidgets() {
  	String[] projection = new String[] {
              WidgTable._ID,
              //WidgTable._COUNT,
              WidgTable.ID,
           };

  //Get the base URI
  Uri w =  WidgTable.CONTENT_URI;

  //Make the query. 
  Cursor managedCursor = managedQuery(w,
          projection, // Which columns to return 
          null,       // Which rows to return (all rows)
          null,       // Selection arguments (none)
          //ascending order by actual order (representing the real order they were added)
          WidgTable._ID + " ASC");

  	iterate(managedCursor);
  }

  private void iterate(Cursor cur){ 
      if (cur.moveToFirst()) {
          int mId;
          //int mCount;

          int idColumn = cur.getColumnIndex(WidgTable.ID);
          //int cColumn = cur.getColumnIndex(WidgTable._COUNT);
          //causes FC if there are no entries... must be a way to check for this
              
          //mCount = cur.getInt(cColumn);
          //Log.v("getting IDs from DB...", mCount + " wIDs obtained");
          do {
              // Get the field values
              mId = cur.getInt(idColumn);
             
              // Do something with the values. 
              
              RepopulateWidgetView(mId);

          } while (cur.moveToNext());

      }
      else Log.v("iterate","empty DB");
  }
  
  private AppWidgetHostView attachWidget(AppWidgetHostView widget, int w, int h){ 
  	
  	/*
  	 * DisplayMetrics metrics = new DisplayMetrics();
  	 
  	 getWindowManager().getDefaultDisplay().getMetrics(metrics);
  	 
  	 Log.v("Logical density"," " + metrics.density);
  	 Log.v("Density DPI"," " + metrics.densityDpi);
  	 Log.v("absolute height px"," " + metrics.heightPixels);
  	 Log.v("absolute width px"," " + metrics.widthPixels);
  	 
  	 Log.v("Font scaling"," " + metrics.scaledDensity);
  	 
  	 Log.v("pixels per inch x"," " + metrics.xdpi);
  	 Log.v("pixels per inch y"," " + metrics.ydpi);
  	*/
  	int realW = w;
  	int realH = h;
      
      //the size i am seeing on the home screen for a 4x1
      //in a screen cap it is 454 x 124 on either nexus or droid.
  	boolean shouldFill = false;
      if (w > 350) {
      	realW = -1;
      shouldFill = true;
      	//reasonable limit for a 3x wide should be around 335 or 340
      	//the home launcher seems to set 4x to fill parent, so we will too
      	//when they do that they also increase the h to around 124 from the 108 min value
      if (h < 110) realH = 150;
      }
      else {
      if (w < 110) realW = 124;
      if (h < 110) realH = 124;
      }
      //we will set the fill always for 4 wide, but let the sent height handle itself if greater than 1 tall
      
      //these values we start with come from the density independent pixel reading that comes with widgets
      //72dip comes out to be 108 here, which is the value im getting from 1x1s
      //that's x1.5, the pixels per inch on our device in both x and y

      //On the droid, a 4x1 comes out 454x124, that would mean 13 padding on either side
      //in other words they are doing fill parent and the default padding is impacting them
      
      //4x1 comes out 390 x 80 (via screencap) when i leave this default value
      //441 x 108 is the literal pixel amount that comes out in the log from these w and h values
      //this is the thing I can't explain
      
  	//108 pixels = 72 dip * (density / 160) -- our pixel density is 240
  	//441 pix = x dip * (240/160 ==== 294 dip
      
      RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams (realW, realH);
      //(-1,-1);//(realW, realH);


      boolean newRow = false;
      
      if (mRowWidth + w > 444) {
      	//the widget will not fit on this row
      	Log.v("new row", mRowWidth + " was ending width of last row, storing in case of undo");
      	
      	
      	mRowWidth = w;
      	
      	newRow = true;
      }
      else mRowWidth += w;
      
      
      
      
      //if (mRowHeight + h > 109)
      	//special case... its a multi row widget
      	//to handle it we need to allow this row's height to be 2x, 3x, 4x
      	//the issue is that multi row sizes exceed the scope of the basic idea of putting utility widgets
      	//it will become very complicated to place the next choices effectively, but possible
      	//for now we'll expect only widgets that are a 4x2 or 4x3 or 4x4. trying to place a 3x2 will be harder
      
      
      if (widgCount == 0) {
      	params.addRule(RelativeLayout.ALIGN_PARENT_TOP);
      	//params.addRule(RelativeLayout.CENTER_HORIZONTAL);
      }
      //first widget goes at the top of the relative view widget area
      else if (newRow) {
      	params.addRule(RelativeLayout.BELOW, widgets[widgCount-1].getId());
      	//params.addRule(RelativeLayout.CENTER_HORIZONTAL);
      }
      //if starting new row just choose below the position of last widget
      else {
      	params.addRule(RelativeLayout.RIGHT_OF, widgets[widgCount-1].getId());
      	params.addRule(RelativeLayout.ALIGN_BOTTOM, widgets[widgCount-1].getId());
      }
      //adding to same row just be on the right of previous widget and bottom edge aligned to it's bottom edge
       
      
      
      widget.setLayoutParams(params); 
      //if (!shouldFill) widget.setPadding(0, 0, 0, 0); else
      widget.setPadding(0, 0, 0, 0);
       
      
      
      widget.setId(100+widgCount);
      return widget; 
      }

  private void RepopulateWidgetView(int Id) {
  	//places one widget based on the manager ID we are passing
  	//we use this to put in a previously chosen widget. it will be done in onCreate
  	//this allows us to just iterate this passing each ID in order
  	
  	
  	AppWidgetProviderInfo appWidgetInfo = mAppWidgetManager.getAppWidgetInfo(Id);
  	//the info is the widget itself
      

      
      int width = appWidgetInfo.minWidth;
      int height = appWidgetInfo.minHeight;
      
      //here's our actual view
      RelativeLayout parent= (RelativeLayout) findViewById(R.id.widgets); 

      widgets[widgCount] = attachWidget(mAppWidgetHost.createView(this, Id, appWidgetInfo), width, height);
      //attach the existing widget and keep the reference
          
      parent.addView(widgets[widgCount]);//populate the view itself
      widgCount++;
      //increment as if it was the first time user is adding
      //this will allow us to get the counter up where it should be for the next one the user chooses to add
  }
  
  public void updateClock() {
          GregorianCalendar Calendar = new GregorianCalendar();         
      
          int mHour = Calendar.get(GregorianCalendar.HOUR_OF_DAY);
          int mMin = Calendar.get(GregorianCalendar.MINUTE);
          
          String hour = new String("");
          String min = new String("");
          
      if (mHour <10) hour = hour + "0";
      hour = hour + mHour;
      
      if (mMin <10) min = min + "0";
      min = min + mMin;
      
      curhour.setText(hour);
      curmin.setText(min);
      
      
      //update battery as it is also a form of time passing
      
      SharedPreferences settings = getSharedPreferences("myLock", 0);
      int battlevel = settings.getInt("BattLevel", 0);
      
      batt.setText(battlevel + "%");
      
      
  }
  
  
         
protected View inflateView(LayoutInflater inflater) {
  return inflater.inflate(R.layout.lockactivity, null);
}

private void updateLayout() {
  LayoutInflater inflater = LayoutInflater.from(this);

  setContentView(inflateView(inflater));
}
  
@Override
public void onBackPressed() {
  //Back will cause unlock
  
  StartDismiss(getApplicationContext());
  finishing=true;
}

BroadcastReceiver callStarted = new BroadcastReceiver() {
  @Override
  public void onReceive(Context context, Intent intent) {
  if (!intent.getAction().equals("i4nc4mp.myLockGuarded.lifecycle.CALL_START")) return;
  
  //we are going to be dormant while this happens, therefore we need to force finish
  Log.v("guard received broadcast","completing callback and finish");
  
  StopCallback();
  finish();
  
  return;
  }};
  
BroadcastReceiver callPending = new BroadcastReceiver() {
  @Override
     public void onReceive(Context context, Intent intent) {
  if (!intent.getAction().equals("i4nc4mp.myLockGuarded.lifecycle.CALL_PENDING")) return;
          //incoming call does not steal focus till user grabs a tab
          //lifecycle treats this like a home key exit
          //forcing dormant state here will allow us to only exit if call is answered
          dormant = true;
          return;                 
  }};


  BroadcastReceiver idleExit = new BroadcastReceiver() {
  @Override
public void onReceive(Context context, Intent intent) {
  if (!intent.getAction().equals("i4nc4mp.myLockGuarded.lifecycle.IDLE_TIMEOUT")) return;
  
  finishing = true;
  idle = true;
  
  Log.v("exit intent received","calling finish");
  finish();//we will still have focus because this comes from the mediator as a wake event
  return;
  }};

  class Task implements Runnable {
  public void run() {
          
          ManageKeyguard.exitKeyguardSecurely(new LaunchOnKeyguardExit() {
      public void LaunchOnKeyguardExitSuccess() {
         Log.v("doExit", "This is the exit callback");
         StopCallback();
         finish();
          }});
  }}

  @Override
protected void onStop() {
  super.onStop();
  
  if (pendingDismiss) return;
  
  if (finishing) {
          Log.v("lock stop","we have been unlocked by a user exit request");
  }
  else if (paused) {
          if (hasWindowFocus()) {
  
          //stop is called, we were already paused, and still have focus
          //this means something is about to take focus, we should go dormant
          dormant = true;
          Log.v("lock stop","detected external event about to take focus, setting dormant");
          }
      else if (!hasWindowFocus()) {
          //we got paused, lost focus, then finally stopped
          //this only happens if user is navigating out via notif, popup, or home key shortcuts
          Log.v("lock stop","onStop is telling mediator we have been unlocked by user navigation");
          
          if (dormant) finishing = true;//dialog popup other than notif panel allowed a nav exit
      }
  }
  else Log.v("unexpected onStop","lockscreen was stopped for unknown reason");
  
  if (finishing) {
	  //most finish commands will already call these tw
	  //user exit unlock only calls finish and has set this finishing flag
	  //FIXME finishing and pendingDismiss might be redundant
	  //since pendingdismiss was added for instant unlock mode
          StopCallback();
          finish();
  }
  
}

@Override
protected void onPause() {
  super.onPause();
  
  paused = true;
  
  try {
	     mAppWidgetHost.stopListening();
	 } catch (NullPointerException ex) {
	     Log.w("lockscreen destroy", "problem while stopping AppWidgetHost during Lockscreen destruction", ex);
	 }
  
  if (!starting && !hasWindowFocus() && !pendingDismiss) {
          //case: we yielded focus to something but didn't pause. Example: notif panel
          //pause in this instance means something else is launching, that is about to try to stop us
          //so we need to exit now, as it is a user nav, not a dormancy event
          Log.v("navigation exit","got paused without focus, starting dismiss sequence");
          
          //anytime we lose focus before pause, we are calling disable
          //this will exit properly as we navigate out
          ManageKeyguard.exitKeyguardSecurely(new LaunchOnKeyguardExit() {
          public void LaunchOnKeyguardExitSuccess() {
             Log.v("doExit", "This is the exit callback");
             StopCallback();
             finish();
              }});
          
  }
  else {
  	if (hasWindowFocus()) Log.v("lock paused","normal pause - we still have focus");
  	else Log.v("lock paused","exit pause - don't have focus");
  	if (slideWakeup) {
  		Log.v("returning to sleep","toggling slide wakeup false");
  		slideWakeup = false;
  	}
  	if (resurrected) {
  		Log.v("returning to sleep","toggling resurrected false");
  		resurrected = false;
  		//sometimes the invalid screen on doesn't happen
  		//in that case we just turn off the flag at next pause
  	}
  }
}

@Override
protected void onResume() {
  super.onResume();
  Log.v("lock resume","resuming, focus is " + hasWindowFocus());

  paused = false;  
  
  if (mAppWidgetHost != null) mAppWidgetHost.startListening();
  
  updateClock();
}

@Override
public void onDestroy() {
  super.onDestroy();
          
 serviceHandler.removeCallbacks(myTask);

 serviceHandler = null;
 
 unregisterReceiver(callStarted);
 unregisterReceiver(callPending);
 unregisterReceiver(idleExit);
 
 try {
     mAppWidgetHost.stopListening();
 } catch (NullPointerException ex) {
     Log.w("lockscreen destroy", "problem while stopping AppWidgetHost during Lockscreen destruction", ex);
 }
 
 //StopCallback();
 //doesnt always get sent by the time screen off is going to happen again.
  
 Log.v("destroy Guard","Destroying");
}

@Override
public void onWindowFocusChanged (boolean hasFocus) {	
  if (hasFocus) {
          Log.v("focus change","we have gained focus");
          //Catch first focus gain after onStart here.
          //this allows us to know if we actually got as far as having focus (expected but bug sometimes prevents
          if (starting) {
                  starting = false;
                  //set our own lifecycle reference now that we know we started and got focus properly
                  
                  //tell mediator it is no longer waiting for us to start up
                  StartCallback();
          }
          else if (dormant) {
                  Log.v("regained","we are no longer dormant");
                  dormant = false;
                  resurrected = true;
          }
          else if (pendingExit) {
                  Log.v("regained","we are no longer pending nav exit");
                  pendingExit = false;
                  ManageKeyguard.reenableKeyguard();
          }
  }
  else if (!pendingDismiss) {                                                  
          if (!finishing && paused) {
//Handcent popup issue-- we haven't gotten resume & screen on yet
//Handcent is taking focus first thing
//So it is now behaving like an open of notif panel where we aren't stopped and aren't even getting paused
        	  
       //we really need to know we were just resumed and had screen come on to do this exit
       //Need to implement the method check tool so we can rely on mediator bind in pre 2.1
                          
                          if (dormant) Log.v("dormant handoff complete","the external event now has focus");
                          else {
                        	  if (isScreenOn()) {
                                  Log.v("home key exit","launching full secure exit");
                                                                                  
                                  ManageKeyguard.disableKeyguard(getApplicationContext());
                                  serviceHandler.postDelayed(myTask, 50);
                        	  }
                        	  else {
                        		  //Here's the handcent case
                        		  //if you then exit via a link on pop,
                        		  //we do get the user nav handling on onStop
                        		  Log.v("popup event","focus handoff before screen on, nav exit possible");
                        		  dormant = true;                            
                        		  //we need to be dormant so we realize once the popup goes away
                        	  }
                          }
          }
          else if (!paused) {
                  //not paused, losing focus, we are going to manually disable KG
                  Log.v("focus yielded while active","about to exit through notif nav");
                  pendingExit = true;
                  ManageKeyguard.disableKeyguard(getApplicationContext());
          }
  }
  
}

protected void onStart() {
  super.onStart();
  Log.v("lockscreen start success","setting flags");
  
  if (finishing) {
          finishing = false;
          Log.v("re-start","we got restarted while in Finishing phase, wtf");
          //since we are sometimes being brought back, safe to ensure flags are like at creation
  }
}

public void StartCallback() {
  Intent i = new Intent("i4nc4mp.myLockGuarded.lifecycle.LOCKSCREEN_PRIMED");
  getApplicationContext().sendBroadcast(i);
}

public void StopCallback() {
  Intent i = new Intent("i4nc4mp.myLockGuarded.lifecycle.LOCKSCREEN_EXITED");
  getApplicationContext().sendBroadcast(i);
}

public void StartDismiss(Context context) {
  
	PowerManager myPM = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
  myPM.userActivity(SystemClock.uptimeMillis(), false);
  //the KeyguardViewMediator poke doesn't have enough time to register before our handoff sometimes (rare)
  //this might impact the nexus more than droid. need to test further
  //result is the screen is off (as the code is successful)
  //but no keyguard, have to hit any key to wake it back up
	
  //we solved this by using a wake lock, but only applicable in instant unlock circumstance
  
  Class w = DismissActivity.class;
                
  Intent dismiss = new Intent(context, w);
  dismiss.setFlags(//Intent.FLAG_ACTIVITY_NEW_TASK//For some reason it requires this even though we're already an activity
                  Intent.FLAG_ACTIVITY_NO_USER_ACTION//Just helps avoid conflicting with other important notifications
                  | Intent.FLAG_ACTIVITY_NO_HISTORY//Ensures the activity WILL be finished after the one time use
                  | Intent.FLAG_ACTIVITY_NO_ANIMATION);
                  
  pendingDismiss = true;
  startActivity(dismiss);
}

public boolean isScreenOn() {
	//Allows us to tap into the 2.1 screen check if available
	
	boolean on = false;
	
	if(Integer.parseInt(Build.VERSION.SDK) < 7) { 
		//we will bind to mediator and ask for the isAwake, if on pre 2.1
		//for now we will just use a pref since we only need it during life cycle
		//so we don't have to also get a possibly unreliable screen on broadcast within activity
		Log.v("pre 2.1 screen check","grabbing screen state from prefs");
		SharedPreferences settings = getSharedPreferences("myLock", 0);
	   	on = settings.getBoolean("screen", false);
		
	}
	else {
		PowerManager myPM = (PowerManager) getApplicationContext().getSystemService(Context.POWER_SERVICE);
		on = myPM.isScreenOn();
	}
	
	return on;
}
}

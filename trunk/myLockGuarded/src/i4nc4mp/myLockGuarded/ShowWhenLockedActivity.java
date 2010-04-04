package i4nc4mp.myLockGuarded;




import i4nc4mp.myLockGuarded.WidgInfo.WidgTable;

import java.util.GregorianCalendar;

import android.app.Activity;
import android.appwidget.AppWidgetHost;
import android.appwidget.AppWidgetHostView;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProviderInfo;

import android.content.SharedPreferences;
import android.database.Cursor;

import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;

import android.widget.RelativeLayout;
import android.widget.TextView;

//The lockscreen that comes up over the top of secure pattern mode. This will be placed by a subclass of the mediator
//No key handling since we can't handle keys with the show when locked flag
//the mediator for this needs to get the user_present broadcast to know that it needs to get ready to restore this lock at next screen off

//we can actually combine idle timeout with this if we want to provide an always customized lockscreen experience that still goes secure

//the real point of this mode though is for users who always want that level of security in place behind their lockscreen experience.

//we can use this also for guarded mode
//I believe we can simply call securely exit here, which will require pattern screen in pattern mode, but otherwise dismisses keyguard.
//we can call finish upon kg exit success callback.

public class ShowWhenLockedActivity extends Activity {
                
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
	
    public TextView curhour;
    public TextView curmin;
    
    public TextView batt;
    
    
    //private AudioManager am = (AudioManager) getApplicationContext().getSystemService(Context.AUDIO_SERVICE);
	
        @Override
    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        //Log.v("create nolock","about to request window params");
        requestWindowFeature(android.view.Window.FEATURE_NO_TITLE);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);
        //we could actually delete this param and then allow the activity to get sent to back when system restores the KG. \
        //this would be the place to call the go to sleep we designed in the first iteration of custom lock
       
       updateLayout();
       

       curhour = (TextView) findViewById(R.id.hourText);
       
       curmin = (TextView) findViewById(R.id.minText);
       
       batt = (TextView) findViewById(R.id.batt);
       
      updateClock();
       
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
        	finish();
        return;
    }
    
    @Override
    public void onPause() {
    	super.onPause();
    	
    	try {
    	     mAppWidgetHost.stopListening();
    	 } catch (NullPointerException ex) {
    	     Log.w("lockscreen pause", "problem while stopping AppWidgetHost during Lockscreen destruction", ex);
    	 }
    }
    
    @Override
    public void onResume() {
    	super.onResume();
    	
    	if (mAppWidgetHost != null) mAppWidgetHost.startListening();
    	
    	updateClock();
    }
    
    @Override
    public void onDestroy() {

        super.onDestroy();

        try {
            mAppWidgetHost.stopListening();
        } catch (NullPointerException ex) {
            Log.w("lockscreen destroy", "problem while stopping AppWidgetHost during Lockscreen destruction", ex);
        }
    }
}
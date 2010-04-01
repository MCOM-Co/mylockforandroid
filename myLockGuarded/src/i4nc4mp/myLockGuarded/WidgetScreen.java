package i4nc4mp.myLockGuarded;

//setup screen for the widgets
//our content provider will ensure the choices get imported into the real lock activity
import i4nc4mp.myLockGuarded.WidgInfo.WidgTable;

import java.util.ArrayList;

import android.app.Activity;
import android.appwidget.AppWidgetHost;
import android.appwidget.AppWidgetHostView;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProviderInfo;
import android.content.ComponentName;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.Toast;


public class WidgetScreen extends Activity {
	
private AppWidgetManager mAppWidgetManager;
private AppWidgetHost mAppWidgetHost;

//private LayoutInflater mInflater;

static final int APPWIDGET_HOST_ID = 2037;
//this int identifies you. The Launcher's ID is 1024.
//If you were to implement different hosts that need to be distinguished
//then the ID is a shortcut for passing what you're doing to the correct one 


private static final int REQUEST_CREATE_APPWIDGET = 5;
private static final int REQUEST_PICK_APPWIDGET = 9;

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
private int[] widths = new int[16];


private int currentRow = 0;//used to determine where we are adding till space runs out

private int RowWidth[] = new int[16];
//when we do a new row, we store the ending width so we can properly undo

private int mRowWidth = 0;
private int mRowHeight = 0;
//we'll just total up the widgets that get added
//441 x 108 is the size that comes in from a 4x1 built in widget (music)





private RelativeLayout[] rows = new RelativeLayout[4];
//here we can actually just create relative layouts to live inside of the parent (id is @widgets)
//what this enables is a workaround so we can still fill the remaining space even if a 2x or taller is placed
//TODO not yet implemented.




@Override
protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    
    requestWindowFeature(android.view.Window.FEATURE_NO_TITLE);
  
    updateLayout();
 
	//Here we initialize widget manager.
    mAppWidgetManager = AppWidgetManager.getInstance(this);
    mAppWidgetHost = new AppWidgetHost(this, APPWIDGET_HOST_ID);
    mAppWidgetHost.startListening();
    
    MakeWidgets();
    //MAKE WIDGETS!
    //This part is all that we need to add to the lockscreen activity

}

private void updateLayout() {
    LayoutInflater inflater = LayoutInflater.from(this);

    setContentView(inflateView(inflater));
}

//override is for when we subclass LockActivity for purpose pass the blank slate layout
//@Override
protected View inflateView(LayoutInflater inflater) {
	
  
	return inflater.inflate(R.layout.lockactivity, null);
}

@Override
public void onBackPressed() {
	finish();
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


public boolean onCreateOptionsMenu(Menu menu) {
    menu.add(0, 1, 0, "Add Widget");
    menu.add(0, 2, 0, "Undo Last");
    menu.add(0, 3, 0, "Start Over");
   // menu.add(0, 4, 0, "Done");
    return true;
}

/* Handles item selections */
public boolean onOptionsItemSelected(MenuItem item) {
    switch (item.getItemId()) {
    case 1:
        if (widgCount<15) doWidgetPick();
        else {
        	Toast.makeText(WidgetScreen.this, "NO MORE!", Toast.LENGTH_SHORT).show();
        }
        return true;
    case 2: 
    	if (widgCount > 0) {
    		undoLastWidget();
    	   	//not equipped for selective delete
    	   	//due to our current positioning logic we need to peel them back one at a time
    	}//can't undo if we are already at 0 waiting for a first add
    	else Toast.makeText(WidgetScreen.this, "YOU'RE AFTER MY ROBOT BEE!", Toast.LENGTH_SHORT).show();
    	return true;
    case 3:
    	//auto undo
    	do {
    		undoLastWidget();
    	}
    	while (widgCount > 0);
    	return true;
    }
    return false;
}

//here's where it gets really fun. we're going to launch the widget pick-list intent 

//there is a bug with this intent/process where they have made it dependent on defining a custom extra widget. 
//The "search" widget is not coded as an AppWidgetProvider for some reason 
//they insert it into the list when pick intent is called, and if you don't insert a custom item 
//you get a null pointer exception when trying to start 
protected void doWidgetPick() {
	int appWidgetId = WidgetScreen.this.mAppWidgetHost.allocateAppWidgetId();

    Intent pickIntent = new Intent(AppWidgetManager.ACTION_APPWIDGET_PICK);
    pickIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
    
    /* custom extra that has to be there or else NPE will happen due to android bug   */
  //this is pulled from the Launcher source, I just changed a few things as it's just a dummy entry at this point 
    ArrayList<AppWidgetProviderInfo> customInfo =
            new ArrayList<AppWidgetProviderInfo>();
    AppWidgetProviderInfo info = new AppWidgetProviderInfo();
    info.provider = new ComponentName(getPackageName(), "XXX.YYY");
    info.label = "i love android";
    info.icon = R.drawable.icon;
    customInfo.add(info);
    pickIntent.putParcelableArrayListExtra(
            AppWidgetManager.EXTRA_CUSTOM_INFO, customInfo);
    ArrayList<Bundle> customExtras = new ArrayList<Bundle>();
    Bundle b = new Bundle();
    b.putString("custom_widget", "search_widget");
    customExtras.add(b);
    pickIntent.putParcelableArrayListExtra(
            AppWidgetManager.EXTRA_CUSTOM_EXTRAS, customExtras);
    /* that's a lot of lines that are there for no function at all */
        
    
    // start the pick activity
    startActivityForResult(pickIntent, REQUEST_PICK_APPWIDGET);
    //because we've defined ourselves as a singleTask activity, it will allow this intent to be part of the task

}


@Override
protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    Log.v("result",requestCode + ", " + resultCode + ", " + data);
  //when the user completes the pick process, both these cases come back, according to log. 
  //looks like this is happening because Pick does a check to see if a config needs to be launched first 
  //if not it just sends the create intent 
    
    
    if (resultCode == RESULT_OK) {
        switch (requestCode) {
        	case REQUEST_PICK_APPWIDGET:
        		addAppWidget(data);
        		break;
        	case REQUEST_CREATE_APPWIDGET:
                completeAddAppWidget(data);
                break;
        }
    }
    else if ((requestCode == REQUEST_PICK_APPWIDGET ||
            requestCode == REQUEST_CREATE_APPWIDGET) && resultCode == RESULT_CANCELED &&
            data != null) {
        // Clean up the appWidgetId if we canceled
        int appWidgetId = data.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, -1);
        if (appWidgetId != -1) {
            mAppWidgetHost.deleteAppWidgetId(appWidgetId);
        }
    }
}


private void completeAddAppWidget(Intent data){
	//actually creates the view for the widget

    Bundle extras = data.getExtras();
    int appWidgetId = extras.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, -1);

    AppWidgetProviderInfo appWidgetInfo = mAppWidgetManager.getAppWidgetInfo(appWidgetId);
    

    /* Launcher would calculate the grid spans needed to fit this widget
     * It would also do a check operation to abort if the cell user picked wasn't acceptable
     * given the size of the widget they chose
     */
    
    //What we'll do is log the info about the widget for help in letting user reposition it
    
    int width = appWidgetInfo.minWidth;
    int height = appWidgetInfo.minHeight;
    
/*
next we need to make a record of where we are adding this widget
what the launcher is doing is spawning a helper object where it saves details about the widget
it saves the number of cells wide and tall the widget is
it adds the spawned object to the array list for widgetinfos
the array list is a member of LauncherInfo helper object
the model seems to retain the references to everything that's been placed on the Launcher
*/
    //we can get a reference to our main view here, and then add a relative layout to it.
    //I can probably directly reference the relative layout I want and then add widgets filling in from the top
    //just need to figure out how to determine if the widget being selected is too long to fit on existing row
    //to decide whether to place it on right of last widget or on the bottom
    RelativeLayout parent= (RelativeLayout) findViewById(R.id.widgets); 
    
    //Log.v("getting parent ref","the ID of the parent is " + parent.getId());

    //AppWidgetHostView newWidget = mAppWidgetHost.createView(this, appWidgetId, appWidgetInfo);


    widgets[widgCount] = attachWidget(mAppWidgetHost.createView(this, appWidgetId, appWidgetInfo), width, height);
    parent.addView(widgets[widgCount]);
    //created the VIEW and positioned it on our view
    //the manager now has given us a widget ID we can use to make this same widget view again
    
    /*
    int finalH, finalW;
    
    finalH = widgets[widgCount].getHeight();
    finalW = widgets[widgCount].getWidth();
    Log.v("widget view generated","final size is " + finalW + "x" + finalH);
    */
    
    Log.v("widget was added","the manager ID = " + appWidgetId + "size = " + width + "x" + height);
    
    ContentValues values = new ContentValues();

    values.put(WidgTable.ID, appWidgetId);
    values.put(WidgTable.ROW, currentRow);
    values.put(WidgTable.WIDTH, width);
    values.put(WidgTable.HEIGHT, height);
   

    Uri uri = getContentResolver().insert(WidgTable.CONTENT_URI, values);
    Log.v("user picked a widget","added and written to DB: " + uri);
    
    //now set our reference locally and up the count so it is ready for next add
    widgetId[widgCount] = appWidgetId;
    widgCount++;
    
    
    
       
        //so every single widget that gets created is one instance of the AppWidgetHostView.
}


//the created new widget is passed raw with the data about its size, then we figure out how to position it
//if we had to go to next row, increment currentRow so the DB will be set correctly
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
    	RowWidth[currentRow] = mRowWidth;
    	currentRow++;
    	
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
     
    widths[widgCount] = w;
    
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

public AppWidgetHost getAppWidgetHost() {
    return mAppWidgetHost;
}




void addAppWidget(Intent data) {
    // TODO: catch bad widget exception when sent
    int appWidgetId = data.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, -1);

    String customWidget = data.getStringExtra("custom_widget");
    if ("search_widget".equals(customWidget)) {//user picked the extra
        // We don't need this any more, since this isn't a real app widget.
        mAppWidgetHost.deleteAppWidgetId(appWidgetId);
        //scold user for disobedience
    } else {
        AppWidgetProviderInfo appWidget = mAppWidgetManager.getAppWidgetInfo(appWidgetId);

        if (appWidget.configure != null) {
            // Launch over to configure widget, if needed
            Intent intent = new Intent(AppWidgetManager.ACTION_APPWIDGET_CONFIGURE);
            intent.setComponent(appWidget.configure);
            intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);

            startActivityForResult(intent, REQUEST_CREATE_APPWIDGET);
        } else {
            // Otherwise just add it
            onActivityResult(REQUEST_CREATE_APPWIDGET, Activity.RESULT_OK, data);
        }
    }
}

/**
 * Re-listen when widgets are reset.
 */
private void onAppWidgetReset() {
    mAppWidgetHost.startListening();
}

private void undoLastWidget() {
	widgCount--;//the incremented value was waiting for next add, peel it back to remove last
	
	Log.v("delete request", "trying to remove " + widgCount);
		
	
	RelativeLayout parent= (RelativeLayout) findViewById(R.id.widgets);
	
	if (mAppWidgetManager == null)
	{
		Log.v("big problem","the manager is null somehow");
		mAppWidgetManager = AppWidgetManager.getInstance(this);
	}
	
	//deduct its width from the count
	/*
	AppWidgetProviderInfo mInfo = mAppWidgetManager.getAppWidgetInfo(widgetId[widgCount]);
	if (mInfo == null) {
		Log.v("big problem","couldn't get the widg info");
	} //don't know why we can't reliably get access to the info, anyway easy workaround
	//just will store a local array of the widths of all added widgets
	else {*/
		int w = widths[widgCount];//mInfo.minWidth;
	
	
	
	
	if (mRowWidth == w && currentRow != 0) {
		//when not in first row and our width equals row width
		//go back to previous row and set the old width active
		currentRow--;
		mRowWidth = RowWidth[currentRow];
		}
	else mRowWidth -= w;
	//otherwise just deduct width from it since this widget is now gone but row still has others
		
	
	//remove the view itself
	 parent.removeView(widgets[widgCount]);
	
	 //tell the manager it is being trashed
	 if (mAppWidgetHost == null) {
		Log.v("big problem","the host is null somehow");
		 mAppWidgetHost = new AppWidgetHost(this, APPWIDGET_HOST_ID);
	 
	    mAppWidgetHost.startListening();
	 }
	 mAppWidgetHost.deleteAppWidgetId(widgetId[widgCount]);
	 
	 
	 int tableID = widgCount + 1;
	 //content://i4nc4mp.customLock.widgidprovider/wID/1 is first whereas widgCount starts at 0
	 
	 //Uri mUri = Uri.withAppendedPath(WidgTable.CONTENT_URI, "/" + tableID);
	 
	 //clear the record for it in the DB
	 getContentResolver().delete(Uri.withAppendedPath(WidgTable.CONTENT_URI, "/" + tableID), null, null);
	 
	 
	 //WOOHOO!	 
}

@Override
public void onDestroy() {
    //mDestroyed = true;

    super.onDestroy();

    try {
        mAppWidgetHost.stopListening();
    } catch (NullPointerException ex) {
        Log.w("lockscreen destroy", "problem while stopping AppWidgetHost during Lockscreen destruction", ex);
    }
}

}
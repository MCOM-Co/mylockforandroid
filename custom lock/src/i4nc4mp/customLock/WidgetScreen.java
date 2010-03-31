package i4nc4mp.customLock;

import i4nc4mp.customLock.WidgInfo.WidgTable;

import java.io.IOException;
import java.util.ArrayList;

import android.app.Activity;
import android.appwidget.AppWidgetHost;
import android.appwidget.AppWidgetHostView;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProviderInfo;
import android.content.ComponentName;
import android.content.ContentValues;
import android.content.Intent;
import android.database.SQLException;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Contacts.People;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.RelativeLayout;


public class WidgetScreen extends Activity {
//for testing we have set this up as a Launcher/Main icon
//for implementation we will make it subclass LockActivity
	//IN FACT the standalone activity fits what we will need so users can open it and set up their widgets
	
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


private int currentRow = 0;//used to determine where we are adding till space runs out

//we really need to store widgets in 4 to keep row organization straight
private int[] row0 = new int[4];
private int[] row1 = new int[4];
private int[] row2 = new int[4];
private int[] row3 = new int[4];

private int[] quant = new int[4];
//this gives the amount of widgets in each row
// quant[0] == 2 would ensure row0[0 & 1] were repopulated before changing to row1

//what the code does is check the size and adds to the row we are in till we hit the limit
//then, we can reference item 0 in the row and use the relative Below to position 0 of the next row
//these will center in parent in terms of the horizontal.

//first test will fill a single row then inform user the row is out of space.
//this row will be the slot below the clock, then we will set up the launch ver with the 2nd row below it
//also might be possible to let user specify a row later on (via the options choices)

//its possible almost all of this data setup can be put in the SQL.
// all we need to do is make sure we can check how many exist on each row, when we run into a 0 item,
//then move to the next row as 0 would mean none was added there


@Override
protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    
    requestWindowFeature(android.view.Window.FEATURE_NO_TITLE);
  
    updateLayout();
 
//====get or create the database for storing the widget IDs and quantities on each row
//TODO
    //FIXME
//========= Database should now be open for us to check 

	//Here we initialize widget manager.
    mAppWidgetManager = AppWidgetManager.getInstance(this);
    mAppWidgetHost = new AppWidgetHost(this, APPWIDGET_HOST_ID);
    mAppWidgetHost.startListening();
    
  
    //Next, we need to repopulate the widgets user had chosen already
    
    //first, ask the DB how many items exist
    int rCount = 0;
    
    //now, fill our widgetId array with the IDs from the database
            
    //now all we have to do is iterate repopulate once for each Id we have
    //it does the positioning work on its own just like the first attach would
    //RepopulateWidgetView(widgetId[i]);
    

}

//from LockActivity
private void updateLayout() {
    LayoutInflater inflater = LayoutInflater.from(this);

    setContentView(inflateView(inflater));
}

//override is for when we subclass LockActivity for purpose pass the blank slate layout
//@Override
protected View inflateView(LayoutInflater inflater) {
	
  
	return inflater.inflate(R.layout.mylockscreen, null);
}

@Override
public void onBackPressed() {
	moveTaskToBack(true);
	//this makes sure we don't get killed unless system forces it
}


public boolean onCreateOptionsMenu(Menu menu) {
    menu.add(0, 1, 0, "Add Widget");
    return true;
}

/* Handles item selections */
public boolean onOptionsItemSelected(MenuItem item) {
    switch (item.getItemId()) {
    case 1:
        doWidgetPick();
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
    RelativeLayout parent= (RelativeLayout) findViewById(R.id.mylockscreen); 
    
    //Log.v("getting parent ref","the ID of the parent is " + parent.getId());

    //AppWidgetHostView newWidget = mAppWidgetHost.createView(this, appWidgetId, appWidgetInfo);


    widgets[widgCount] = attachWidget(mAppWidgetHost.createView(this, appWidgetId, appWidgetInfo), width, height);
    parent.addView(widgets[widgCount]);
    //created the VIEW and positioned it on our view
    //the manager now has given us a widget ID we can use to make this same widget view again
    
    
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
         
    RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams 
    (LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT); 
    if (widgCount == 0) params.addRule(RelativeLayout.ALIGN_PARENT_TOP);
    //first widget goes at the top of the relative view widget area
    else params.addRule(RelativeLayout.RIGHT_OF, widgets[widgCount-1].getId());
    //after that we put them on the right
    
    //FIXME need to logic to know we ran out of space...
     
    widget.setLayoutParams(params); 
    
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
    RelativeLayout parent= (RelativeLayout) findViewById(R.id.mylockscreen); 

    widgets[widgCount] = attachWidget(mAppWidgetHost.createView(this, Id, appWidgetInfo), width, height);
    //attach the existing widget and keep the reference
        
    parent.addView(widgets[widgCount]);//populate the view itself
    widgCount++;
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
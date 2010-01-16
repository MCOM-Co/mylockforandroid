package i4nc4mp.customLock;

import java.util.ArrayList;

import android.app.Activity;
import android.appwidget.AppWidgetHost;
import android.appwidget.AppWidgetHostView;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProviderInfo;
import android.content.ComponentName;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;


public class Lockscreen extends Activity {
//for testing we have set this up as a Launcher/Main icon
//for implementation we will make it subclass LockActivity
	
	
private AppWidgetManager mAppWidgetManager;
private AppWidgetHost mAppWidgetHost;

//private LayoutInflater mInflater;

static final int APPWIDGET_HOST_ID = 2037;
//this int identifies you. The Launcher's ID is 1024.
//If you were to implement different hosts that need to be distinguished
//then the ID is a shortcut for passing what you're doing to the correct one 


private static final int REQUEST_CREATE_APPWIDGET = 5;
private static final int REQUEST_PICK_APPWIDGET = 9;

private AppWidgetHostView widgetview;

@Override
protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    //next two are copied from LockActivity
    //TODO comment out when subclassing from it
    requestWindowFeature(android.view.Window.FEATURE_NO_TITLE);
    updateLayout();
    
    //mInflater = getLayoutInflater();
    
    mAppWidgetManager = AppWidgetManager.getInstance(this);

    mAppWidgetHost = new AppWidgetHost(this, APPWIDGET_HOST_ID);
    mAppWidgetHost.startListening();

}

//from LockActivity
private void updateLayout() {
    LayoutInflater inflater = LayoutInflater.from(this);

    setContentView(inflateView(inflater));
    //if (mInflater != null) mInflater = inflater;
}

//override is for when we subclass LockActivity for purpose pass the blank slate layout
//@Override
protected View inflateView(LayoutInflater inflater) {
    if(widgetview==null) return inflater.inflate(R.layout.mylockscreen, null);
    else return inflater.inflate(R.layout.mylockscreen, widgetview);
    //tries to pass the widget host view
    //what this will do once implemented is pass the view that is parent to all widget children
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
	int appWidgetId = Lockscreen.this.mAppWidgetHost.allocateAppWidgetId();

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
    
    int widgetwidth = appWidgetInfo.minWidth;
    int widgetheight = appWidgetInfo.minHeight;
    
/*
next we need to make a record of where we are adding this widget
what the launcher is doing is spawning a helper object where it saves details about the widget
it saves the number of cells wide and tall the widget is
it adds the spawned object to the array list for widgetinfos
the array list is a member of LauncherInfo helper object
the model seems to retain the references to everything that's been placed on the Launcher
*/


        widgetview = mAppWidgetHost.createView(this, appWidgetId, appWidgetInfo);
        //launcher is doing something to pass this view to their the workspace or the celllayout
        //I have to learn how to maintain a framelayout so i can attach the widgets to it
        //it seems in the Launcher a workspace contains all the screens and the CellLayout is one screen
        
        updateLayout();
        //the inflate method will see if widgetview is null - when not null it passes the widgetview as the parent.
        //that operation creates the widget you chose in the center of the screen
        //and the widget works.
        
        //to get it working the inflate method would pass a parent that we interact with to place each widgets
        //so every single widget that gets created is one instance of the AppWidgetHostView.
        //the viewgroup we would have to maintain holds all the appwidgethostviews/
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
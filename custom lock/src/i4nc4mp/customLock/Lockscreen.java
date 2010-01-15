package i4nc4mp.customLock;

import java.util.ArrayList;

import android.app.Activity;
import android.appwidget.AppWidgetHost;
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

private LayoutInflater mInflater;

static final int APPWIDGET_HOST_ID = 2037;

private static final int REQUEST_CREATE_APPWIDGET = 5;
private static final int REQUEST_PICK_APPWIDGET = 9;

@Override
protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    //next two are copied from LockActivity
    //TODO comment out when subclassing from it
    requestWindowFeature(android.view.Window.FEATURE_NO_TITLE);
    updateLayout();
    
    mInflater = getLayoutInflater();
    
    mAppWidgetManager = AppWidgetManager.getInstance(this);

    mAppWidgetHost = new AppWidgetHost(this, APPWIDGET_HOST_ID);
    mAppWidgetHost.startListening();

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

//user presses menu, then selects Add Widget from the popup menu
protected void doWidgetPick() {
	int appWidgetId = Lockscreen.this.mAppWidgetHost.allocateAppWidgetId();

    Intent pickIntent = new Intent(AppWidgetManager.ACTION_APPWIDGET_PICK);
    pickIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
    
    /* custom extra that has to be there or else NPE will happen due to android bug   */
    ArrayList<AppWidgetProviderInfo> customInfo =
            new ArrayList<AppWidgetProviderInfo>();
    AppWidgetProviderInfo info = new AppWidgetProviderInfo();
    info.provider = new ComponentName(getPackageName(), "XXX.YYY");
    info.label = "i love android";//getString(R.string.group_search);
    info.icon = R.drawable.icon;//R.drawable.ic_search_widget;
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
    //what's happening here is that result comes back immediately as 9, 0, null
    //logcat says that activity is starting as a new task so canceling result
}


@Override
protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    Log.v("result",requestCode + ", " + resultCode + ", " + data);
    //Canceled (0) is coming back immediately due to the "Starting as a new task" log
    if (resultCode == RESULT_OK) {
        switch (requestCode) {
        	case REQUEST_PICK_APPWIDGET:
        		addAppWidget(data);
        		break;
        	case REQUEST_CREATE_APPWIDGET:
                //completeAddAppWidget(data, mAddItemCellInfo, !mDesktopLocked);
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
    
	/*
	mWaitingForResult = false;

    // The pattern used here is that a user PICKs a specific application,
    // which, depending on the target, might need to CREATE the actual target.

    // For example, the user would PICK_SHORTCUT for "Music playlist", and we
    // launch over to the Music app to actually CREATE_SHORTCUT.

    if (resultCode == RESULT_OK && mAddItemCellInfo != null) {
        switch (requestCode) {
            case REQUEST_PICK_APPLICATION:
                completeAddApplication(this, data, mAddItemCellInfo, !mDesktopLocked);
                break;
            case REQUEST_PICK_SHORTCUT:
                processShortcut(data, REQUEST_PICK_APPLICATION, REQUEST_CREATE_SHORTCUT);
                break;
            case REQUEST_CREATE_SHORTCUT:
                completeAddShortcut(data, mAddItemCellInfo, !mDesktopLocked);
                break;
            case REQUEST_PICK_LIVE_FOLDER:
                addLiveFolder(data);
                break;
            case REQUEST_CREATE_LIVE_FOLDER:
                completeAddLiveFolder(data, mAddItemCellInfo, !mDesktopLocked);
                break;
            case REQUEST_PICK_APPWIDGET:
                addAppWidget(data);
                break;
            case REQUEST_CREATE_APPWIDGET:
                completeAddAppWidget(data, mAddItemCellInfo, !mDesktopLocked);
                break;
        }
    } else if ((requestCode == REQUEST_PICK_APPWIDGET ||
            requestCode == REQUEST_CREATE_APPWIDGET) && resultCode == RESULT_CANCELED &&
            data != null) {
        // Clean up the appWidgetId if we canceled
        int appWidgetId = data.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, -1);
        if (appWidgetId != -1) {
            mAppWidgetHost.deleteAppWidgetId(appWidgetId);
        }
    }*/
}


/*
 //Add a widget to the workspace.
 
 //@param data The intent describing the appWidgetId.
 //@param cellInfo The position on screen where to create the widget.

//this segment appears to be needed as a result of users long press anywhere
//that the custom subclass is catching


private void completeAddAppWidget(Intent data, //CellLayout.CellInfo cellInfo,
        boolean insertAtFirst) {

    Bundle extras = data.getExtras();
    int appWidgetId = extras.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, -1);

    //if (LOGD) d(LOG_TAG, "dumping extras content="+extras.toString());

    AppWidgetProviderInfo appWidgetInfo = mAppWidgetManager.getAppWidgetInfo(appWidgetId);

    // Calculate the grid spans needed to fit this widget
    //CellLayout layout = (CellLayout) mWorkspace.getChildAt(cellInfo.screen);
    //int[] spans = layout.rectToCell(appWidgetInfo.minWidth, appWidgetInfo.minHeight);


    
    // Try finding open space on Launcher screen
    final int[] xy = mCellCoordinates;
    if (!findSlot(cellInfo, xy, spans[0], spans[1])) {
        if (appWidgetId != -1) mAppWidgetHost.deleteAppWidgetId(appWidgetId);
        return;
    }

    //Build Launcher-specific widget info and save to database
    LauncherAppWidgetInfo launcherInfo = new LauncherAppWidgetInfo(appWidgetId);
    launcherInfo.spanX = spans[0];
    launcherInfo.spanY = spans[1];

    LauncherModel.addItemToDatabase(this, launcherInfo,
            LauncherSettings.Favorites.CONTAINER_DESKTOP,
            mWorkspace.getCurrentScreen(), xy[0], xy[1], false);

    if (!mRestoring) {
        sModel.addDesktopAppWidget(launcherInfo);

        //Perform actual inflation because we're live
        launcherInfo.hostView = mAppWidgetHost.createView(this, appWidgetId, appWidgetInfo);

        launcherInfo.hostView.setAppWidget(appWidgetId, appWidgetInfo);
        launcherInfo.hostView.setTag(launcherInfo);

        mWorkspace.addInCurrentScreen(launcherInfo.hostView, xy[0], xy[1],
                launcherInfo.spanX, launcherInfo.spanY, insertAtFirst);
    } else if (sModel.isDesktopLoaded()) {
        sModel.addDesktopAppWidget(launcherInfo);
    }
}*/

public AppWidgetHost getAppWidgetHost() {
    return mAppWidgetHost;
}



/* this gets called in the onclick for the add to home dialog, to invoke the widget picking thing
  case AddAdapter.ITEM_APPWIDGET: {
                    int appWidgetId = Launcher.this.mAppWidgetHost.allocateAppWidgetId();

                    Intent pickIntent = new Intent(AppWidgetManager.ACTION_APPWIDGET_PICK);
                    pickIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
                    // add the search widget
                    ArrayList<AppWidgetProviderInfo> customInfo =
                            new ArrayList<AppWidgetProviderInfo>();
                    AppWidgetProviderInfo info = new AppWidgetProviderInfo();
                    info.provider = new ComponentName(getPackageName(), "XXX.YYY");
                    info.label = getString(R.string.group_search);
                    info.icon = R.drawable.ic_search_widget;
                    customInfo.add(info);
                    pickIntent.putParcelableArrayListExtra(
                            AppWidgetManager.EXTRA_CUSTOM_INFO, customInfo);
                    ArrayList<Bundle> customExtras = new ArrayList<Bundle>();
                    Bundle b = new Bundle();
                    b.putString(EXTRA_CUSTOM_WIDGET, SEARCH_WIDGET);
                    customExtras.add(b);
                    pickIntent.putParcelableArrayListExtra(
                            AppWidgetManager.EXTRA_CUSTOM_EXTRAS, customExtras);
                    // start the pick activity
                    startActivityForResult(pickIntent, REQUEST_PICK_APPWIDGET);
                    break;
                }
 */


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
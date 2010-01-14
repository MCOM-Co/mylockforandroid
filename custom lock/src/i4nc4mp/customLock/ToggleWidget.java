package i4nc4mp.customLock;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.RemoteViews;

public class ToggleWidget extends AppWidgetProvider {
	
	//TODO custom intent string
	//"i4nc4mp.myLock.intent.action.TOGGLE_LOCKSCREEN"
	
	public boolean enabled = false;
	@Override 
    public void onEnabled(Context context) {	
		
		AppWidgetManager mgr = AppWidgetManager.getInstance(context);
		//retrieve a ref to the manager so we can pass a view update
		
		Intent i = new Intent();
		i.setClassName("i4nc4mp.customLock", "i4nc4mp.customLock.Toggler");
		PendingIntent myPI = PendingIntent.getService(context, 0, i, 0);
		//intent to start Toggler service
		
        // Get the layout for the App Widget
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.togglelayout);
        
        //attach the click listener for the service start command intent
        views.setOnClickPendingIntent(R.id.toggleButton, myPI);
        
        //define the componenet for self
        ComponentName comp = new ComponentName(context.getPackageName(), ToggleWidget.class.getName());
        
        //tell the manager to update all instances of the toggle widget with the click listener
        mgr.updateAppWidget(comp, views);
        enabled = true;
	}
	
	@Override
	public void onUpdate (Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
		//at boot this seems to be getting called twice
		if (!enabled) {//catches the case that we are booting
			
		//Intent clickintent = new Intent ("i4nc4mp.myLock.intent.action.TOGGLE_LOCKSCREEN");
		//PendingIntent myPI = PendingIntent.getBroadcast(context, 0, clickintent, 0);
				
		Intent i = new Intent();
		i.setClassName("i4nc4mp.customLock", "i4nc4mp.customLock.Toggler");
		PendingIntent myPI = PendingIntent.getService(context, 0, i, 0);
		//tells the widget button to do start command on toggler service when clicked.
		
        // Get the layout for the App Widget and attach an on-click listener to the button
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.togglelayout);
        ComponentName comp = new ComponentName(context.getPackageName(), ToggleWidget.class.getName());
        
        views.setOnClickPendingIntent(R.id.toggleButton, myPI);
        appWidgetManager.updateAppWidget(comp, views);
		//sends this to update the actual widget view that has been spawned
        
        //context.startService(new Intent(context, Toggler.class));
        //no longer auto-start the toggler, user must click widget
        
        enabled = true;
		}//as far as I can tell, repeat calls to this do not cause errors
	}
}
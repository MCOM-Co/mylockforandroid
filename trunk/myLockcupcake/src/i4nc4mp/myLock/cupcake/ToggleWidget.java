package i4nc4mp.myLock.cupcake;

//how this works - the widget starts toggler service when clicked
//toggler's onstart does try toggle

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;

public class ToggleWidget extends AppWidgetProvider {
	
	//TODO custom intent string
	//"i4nc4mp.myLock.intent.action.TOGGLE_LOCKSCREEN"
	
	public boolean enabled = false;
	@Override 
  public void onEnabled(Context context) {	
		
		AppWidgetManager mgr = AppWidgetManager.getInstance(context);
		//retrieve a ref to the manager so we can pass a view update
		
		//here we can access shared prefs to determine which mode the user has enabled so we toggle the correct one
		//for now I just have one static mediator mode running from toggler.
		//the idea is to make each mode a separate mediator that has to be started after other mode is stopped
		
		Intent i = new Intent();
		i.setClassName("i4nc4mp.myLock.cupcake", "i4nc4mp.myLock.cupcake.Toggler");
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
		i.setClassName("i4nc4mp.myLock.cupcake", "i4nc4mp.myLock.cupcake.Toggler");
		PendingIntent myPI = PendingIntent.getService(context, 0, i, 0);
		//tells the widget button to do start command on toggler service when clicked.
		
      // Get the layout for the App Widget and attach an on-click listener to the button
      RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.togglelayout);
      ComponentName comp = new ComponentName(context.getPackageName(), ToggleWidget.class.getName());
      
      views.setOnClickPendingIntent(R.id.toggleButton, myPI);
      appWidgetManager.updateAppWidget(comp, views);
		//sends this to update the actual widget view that has been spawned
      
      enabled = true;
		}//as far as I can tell, repeat calls to this do not cause errors
		else {
			//use this case to check the serviceactive state in prefs and choose the image we want
		}
	}
}
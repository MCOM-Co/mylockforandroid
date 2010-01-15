package i4nc4mp.customLock;

import android.appwidget.AppWidgetHost;
import android.appwidget.AppWidgetHostView;
import android.appwidget.AppWidgetProviderInfo;
import android.content.Context;
//not used yet
//Copied over from Launcher source-----

//this does nothing other than ensure the view that is created is the subclassed one
//which introduces long press pickup, not an inherent part of widgethost but part of the launcher
/**
 * Specific {@link AppWidgetHost} that creates our {@link LauncherAppWidgetHostView}
 * which correctly captures all long-press events. This ensures that users can
 * always pick up and move widgets.
 */
public class LockscreenAppWidgetHost extends AppWidgetHost {
    public LockscreenAppWidgetHost(Context context, int hostId) {
        super(context, hostId);
    }
    
    @Override
    protected AppWidgetHostView onCreateView(Context context, int appWidgetId,
            AppWidgetProviderInfo appWidget) {
        return new AppWidgetHostView(context);
    }
}
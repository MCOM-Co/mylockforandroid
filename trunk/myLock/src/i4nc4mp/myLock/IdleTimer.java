package i4nc4mp.myLock;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

public class IdleTimer {
    private static final int REQUEST_ID = 0;

    //intent will be caught in both the mediator and the lockactivity
    private static PendingIntent buildIntent(Context ctx) {
        Intent intent = new Intent("i4nc4mp.myLock.IDLE_TIMEOUT");
        PendingIntent sender = PendingIntent.getBroadcast(ctx, REQUEST_ID, intent, PendingIntent.FLAG_CANCEL_CURRENT);

        return sender;
    }

    public static void start(Context ctx) {
    	SharedPreferences settings = ctx.getSharedPreferences("myLock", 0);
    	int minutes = settings.getInt("idletime", 30);
    	
    	long timeout = minutes * 60000;
    	
        long triggerTime = System.currentTimeMillis() + timeout;

        AlarmManager am = (AlarmManager) ctx.getSystemService(Context.ALARM_SERVICE);

        am.set(AlarmManager.RTC_WAKEUP, triggerTime, buildIntent(ctx));
    }

    public static void cancel(Context ctx) {
        AlarmManager am = (AlarmManager) ctx.getSystemService(Context.ALARM_SERVICE);

        am.cancel(buildIntent(ctx));
    }

}
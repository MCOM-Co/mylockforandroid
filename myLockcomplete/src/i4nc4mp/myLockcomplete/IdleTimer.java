package i4nc4mp.myLockcomplete;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

public class IdleTimer {
    private static final int REQUEST_ID = 0;
    private static final long DEFAULT_TIMEOUT = 1 * 60 * 1000;  // 1 minute for testing

    //intent will be caught in both the mediator and the lockactivity
    private static PendingIntent buildIntent(Context ctx) {
        Intent intent = new Intent("i4nc4mp.myLockcomplete.lifecycle.IDLE_TIMEOUT");
        PendingIntent sender = PendingIntent.getBroadcast(ctx, REQUEST_ID, intent, PendingIntent.FLAG_CANCEL_CURRENT);

        return sender;
    }

    public static void start(Context ctx) {
        long triggerTime = System.currentTimeMillis() + DEFAULT_TIMEOUT;

        AlarmManager am = (AlarmManager) ctx.getSystemService(Context.ALARM_SERVICE);

        am.set(AlarmManager.RTC, triggerTime, buildIntent(ctx));
    }

    public static void cancel(Context ctx) {
        AlarmManager am = (AlarmManager) ctx.getSystemService(Context.ALARM_SERVICE);

        am.cancel(buildIntent(ctx));
    }

}
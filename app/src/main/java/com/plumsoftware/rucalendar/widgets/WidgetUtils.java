package com.plumsoftware.rucalendar.widgets;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import com.plumsoftware.rucalendar.activities.MainActivity;

import java.util.Calendar;

public final class WidgetUtils {

    public static final String ACTION_WIDGET_MIDNIGHT_UPDATE =
            "com.plumsoftware.rucalendar.widgets.ACTION_WIDGET_MIDNIGHT_UPDATE";
    public static final String EXTRA_OPEN_DATE = "open_date";

    private WidgetUtils() {
    }

    public static PendingIntent openDatePendingIntent(Context context, long epochDay) {
        Intent intent = new Intent(context, MainActivity.class);
        intent.putExtra(EXTRA_OPEN_DATE, epochDay);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        return PendingIntent.getActivity(
                context,
                (int) (epochDay & 0xFFFF),
                intent,
                pendingIntentFlags()
        );
    }

    public static void scheduleMidnightUpdate(Context context) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) {
            return;
        }

        Intent intent = new Intent(context, WidgetUpdateReceiver.class);
        intent.setAction(ACTION_WIDGET_MIDNIGHT_UPDATE);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                0,
                intent,
                pendingIntentFlags()
        );

        Calendar nextMidnight = Calendar.getInstance();
        nextMidnight.add(Calendar.DAY_OF_MONTH, 1);
        nextMidnight.set(Calendar.HOUR_OF_DAY, 0);
        nextMidnight.set(Calendar.MINUTE, 1);
        nextMidnight.set(Calendar.SECOND, 0);
        nextMidnight.set(Calendar.MILLISECOND, 0);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    nextMidnight.getTimeInMillis(),
                    pendingIntent
            );
        } else {
            alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    nextMidnight.getTimeInMillis(),
                    pendingIntent
            );
        }
    }

    public static void updateAllWidgets(Context context) {
        AppWidgetManager manager = AppWidgetManager.getInstance(context);
        int[] todayIds = manager.getAppWidgetIds(new ComponentName(context, TodayWidgetProvider.class));
        if (todayIds.length > 0) {
            TodayWidgetProvider.updateWidgets(context, manager, todayIds);
        }

        int[] weekIds = manager.getAppWidgetIds(new ComponentName(context, WeekWidgetProvider.class));
        if (weekIds.length > 0) {
            WeekWidgetProvider.updateWidgets(context, manager, weekIds);
        }
    }

    private static int pendingIntentFlags() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE;
        }
        return PendingIntent.FLAG_UPDATE_CURRENT;
    }
}

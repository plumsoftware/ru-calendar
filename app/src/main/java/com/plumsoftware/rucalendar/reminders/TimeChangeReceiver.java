package com.plumsoftware.rucalendar.reminders;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.plumsoftware.rucalendar.widget.WidgetUpdater;

/**
 * Системные события времени: смена часового пояса или времени, перезагрузка, полночь.
 * Напоминания срабатывают по местному времени — перепланируем их; виджеты обновляем (п. 5.1, 5.2).
 */
public class TimeChangeReceiver extends BroadcastReceiver {
    /** Обновление виджетов в 00:00, планирует WidgetUpdater. */
    public static final String ACTION_MIDNIGHT = "com.plumsoftware.rucalendar.action.MIDNIGHT";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (!ACTION_MIDNIGHT.equals(intent.getAction())) {
            ReminderScheduler.rescheduleAll(context);
        }
        WidgetUpdater.updateAll(context);
    }
}

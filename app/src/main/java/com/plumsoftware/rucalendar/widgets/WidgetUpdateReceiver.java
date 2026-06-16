package com.plumsoftware.rucalendar.widgets;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class WidgetUpdateReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null) {
            return;
        }

        String action = intent.getAction();
        if (Intent.ACTION_BOOT_COMPLETED.equals(action)
                || WidgetUtils.ACTION_WIDGET_MIDNIGHT_UPDATE.equals(action)) {
            WidgetUtils.updateAllWidgets(context);
            WidgetUtils.scheduleMidnightUpdate(context);
        }
    }
}

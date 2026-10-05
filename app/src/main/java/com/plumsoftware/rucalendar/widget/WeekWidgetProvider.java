package com.plumsoftware.rucalendar.widget;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;

/** Виджет «Эта неделя» (ТЗ п. 5.1). */
public class WeekWidgetProvider extends AppWidgetProvider {
    @Override
    public void onUpdate(Context context, AppWidgetManager manager, int[] appWidgetIds) {
        WidgetUpdater.updateAll(context);
    }
}

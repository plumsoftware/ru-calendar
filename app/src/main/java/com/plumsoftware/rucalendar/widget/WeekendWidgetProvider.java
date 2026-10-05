package com.plumsoftware.rucalendar.widget;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;

/** Виджет «До выходного» (ТЗ п. 5.1). */
public class WeekendWidgetProvider extends AppWidgetProvider {
    @Override
    public void onUpdate(Context context, AppWidgetManager manager, int[] appWidgetIds) {
        WidgetUpdater.updateAll(context);
    }
}

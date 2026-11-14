package com.plumsoftware.rucalendar.widget;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;

/** Виджет «Месяц»: календарная сетка текущего месяца. */
public class MonthWidgetProvider extends AppWidgetProvider {
    @Override
    public void onUpdate(Context context, AppWidgetManager manager, int[] appWidgetIds) {
        WidgetUpdater.updateAll(context);
    }
}

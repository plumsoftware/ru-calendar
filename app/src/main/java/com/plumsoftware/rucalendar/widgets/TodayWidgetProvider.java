package com.plumsoftware.rucalendar.widgets;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.view.View;
import android.widget.RemoteViews;

import com.plumsoftware.rucalendar.R;

import java.util.Calendar;

public class TodayWidgetProvider extends AppWidgetProvider {

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        updateWidgets(context, appWidgetManager, appWidgetIds);
        WidgetUtils.scheduleMidnightUpdate(context);
    }

    @Override
    public void onEnabled(Context context) {
        WidgetUtils.scheduleMidnightUpdate(context);
    }

    static void updateWidgets(Context context, AppWidgetManager manager, int[] appWidgetIds) {
        Calendar today = WidgetDateUtils.todayAtNoon();
        int hiddenCount = WidgetCelebrationRepository.getHiddenTodayEventsCount();

        for (int appWidgetId : appWidgetIds) {
            RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_today);

            views.setTextViewText(R.id.widget_today_day_number,
                    String.valueOf(today.get(Calendar.DAY_OF_MONTH)));
            views.setTextViewText(R.id.widget_today_month_year,
                    WidgetDateUtils.formatMonthYear(today));

            Intent serviceIntent = new Intent(context, TodayWidgetService.class);
            serviceIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
            views.setRemoteAdapter(R.id.widget_today_list, serviceIntent);
            views.setEmptyView(R.id.widget_today_list, R.id.widget_today_label);

            if (hiddenCount > 0) {
                views.setViewVisibility(R.id.widget_today_more, View.VISIBLE);
                views.setTextViewText(
                        R.id.widget_today_more,
                        context.getString(R.string.widget_today_more_events, hiddenCount)
                );
            } else {
                views.setViewVisibility(R.id.widget_today_more, View.GONE);
            }

            views.setOnClickPendingIntent(
                    R.id.widget_today_root,
                    WidgetUtils.openDatePendingIntent(context, WidgetDateUtils.toEpochDay(today))
            );

            manager.updateAppWidget(appWidgetId, views);
            manager.notifyAppWidgetViewDataChanged(appWidgetId, R.id.widget_today_list);
        }
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);
        if (AppWidgetManager.ACTION_APPWIDGET_UPDATE.equals(intent.getAction())) {
            AppWidgetManager manager = AppWidgetManager.getInstance(context);
            int[] ids = manager.getAppWidgetIds(new ComponentName(context, TodayWidgetProvider.class));
            if (ids.length > 0) {
                updateWidgets(context, manager, ids);
            }
        }
    }
}

package com.plumsoftware.rucalendar.widgets;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.view.View;
import android.widget.RemoteViews;

import androidx.core.content.ContextCompat;

import com.plumsoftware.rucalendar.R;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

public class WeekWidgetProvider extends AppWidgetProvider {

    private static final int[][] DAY_ROOT_IDS = {
            {R.id.day0_root}, {R.id.day1_root}, {R.id.day2_root}, {R.id.day3_root},
            {R.id.day4_root}, {R.id.day5_root}, {R.id.day6_root}
    };
    private static final int[] DAY_WEEKDAY_IDS = {
            R.id.day0_weekday, R.id.day1_weekday, R.id.day2_weekday, R.id.day3_weekday,
            R.id.day4_weekday, R.id.day5_weekday, R.id.day6_weekday
    };
    private static final int[] DAY_DATE_IDS = {
            R.id.day0_date, R.id.day1_date, R.id.day2_date, R.id.day3_date,
            R.id.day4_date, R.id.day5_date, R.id.day6_date
    };
    private static final int[][] DAY_DOT_IDS = {
            {R.id.day0_dot0, R.id.day0_dot1, R.id.day0_dot2},
            {R.id.day1_dot0, R.id.day1_dot1, R.id.day1_dot2},
            {R.id.day2_dot0, R.id.day2_dot1, R.id.day2_dot2},
            {R.id.day3_dot0, R.id.day3_dot1, R.id.day3_dot2},
            {R.id.day4_dot0, R.id.day4_dot1, R.id.day4_dot2},
            {R.id.day5_dot0, R.id.day5_dot1, R.id.day5_dot2},
            {R.id.day6_dot0, R.id.day6_dot1, R.id.day6_dot2}
    };

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
        List<Calendar> stripDays = WidgetCelebrationRepository.getWeekStripDays();
        Calendar today = WidgetDateUtils.todayAtNoon();
        int primaryTextColor = ContextCompat.getColor(context, R.color.widget_text_primary);
        int whiteColor = ContextCompat.getColor(context, R.color.md_theme_light_onPrimary);

        for (int appWidgetId : appWidgetIds) {
            RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_week);

            for (int index = 0; index < stripDays.size(); index++) {
                Calendar day = stripDays.get(index);
                boolean isToday = WidgetDateUtils.isSameDay(day, today);

                views.setTextViewText(DAY_WEEKDAY_IDS[index], WidgetDateUtils.formatWeekdayShort(day));
                views.setTextViewText(
                        DAY_DATE_IDS[index],
                        String.valueOf(day.get(Calendar.DAY_OF_MONTH))
                );

                if (isToday) {
                    views.setInt(DAY_DATE_IDS[index], "setBackgroundResource", R.drawable.widget_today_circle);
                    views.setInt(DAY_DATE_IDS[index], "setTextColor", whiteColor);
                } else {
                    views.setInt(DAY_DATE_IDS[index], "setBackgroundResource", 0);
                    views.setInt(DAY_DATE_IDS[index], "setTextColor", primaryTextColor);
                }

                bindDayDots(views, index, day);

                views.setOnClickPendingIntent(
                        DAY_ROOT_IDS[index][0],
                        WidgetUtils.openDatePendingIntent(context, WidgetDateUtils.toEpochDay(day))
                );
            }

            Intent serviceIntent = new Intent(context, WeekWidgetService.class);
            serviceIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
            views.setRemoteAdapter(R.id.widget_week_list, serviceIntent);
            views.setEmptyView(R.id.widget_week_list, R.id.widget_week_upcoming_label);

            manager.updateAppWidget(appWidgetId, views);
            manager.notifyAppWidgetViewDataChanged(appWidgetId, R.id.widget_week_list);
        }
    }

    private static void bindDayDots(RemoteViews views, int dayIndex, Calendar day) {
        Set<WidgetEventType> types = WidgetCelebrationRepository.getEventTypesForDate(day);
        List<WidgetEventType> typeList = new ArrayList<>(types);
        Collections.sort(typeList, new Comparator<WidgetEventType>() {
            @Override
            public int compare(WidgetEventType left, WidgetEventType right) {
                return Integer.compare(left.getSortPriority(), right.getSortPriority());
            }
        });

        for (int dotIndex = 0; dotIndex < DAY_DOT_IDS[dayIndex].length; dotIndex++) {
            int dotId = DAY_DOT_IDS[dayIndex][dotIndex];
            if (dotIndex < typeList.size() && dotIndex < 3) {
                views.setViewVisibility(dotId, View.VISIBLE);
                views.setImageViewResource(
                        dotId,
                        WidgetDotDrawables.smallDrawableForType(typeList.get(dotIndex))
                );
            } else if (dotIndex == 2 && typeList.size() > 3) {
                views.setViewVisibility(dotId, View.VISIBLE);
                views.setImageViewResource(dotId, R.drawable.widget_dot_more);
            } else {
                views.setViewVisibility(dotId, View.GONE);
            }
        }
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);
        if (AppWidgetManager.ACTION_APPWIDGET_UPDATE.equals(intent.getAction())) {
            AppWidgetManager manager = AppWidgetManager.getInstance(context);
            int[] ids = manager.getAppWidgetIds(new ComponentName(context, WeekWidgetProvider.class));
            if (ids.length > 0) {
                updateWidgets(context, manager, ids);
            }
        }
    }
}

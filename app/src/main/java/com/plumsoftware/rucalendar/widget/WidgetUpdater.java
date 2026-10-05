package com.plumsoftware.rucalendar.widget;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.view.View;
import android.widget.RemoteViews;

import androidx.core.content.ContextCompat;

import com.plumsoftware.rucalendar.R;
import com.plumsoftware.rucalendar.activities.MainActivity;
import com.plumsoftware.rucalendar.data.CalendarRepository;
import com.plumsoftware.rucalendar.data.DayEvent;
import com.plumsoftware.rucalendar.data.HolidayRepository;
import com.plumsoftware.rucalendar.data.MonthModel;
import com.plumsoftware.rucalendar.data.RuDates;
import com.plumsoftware.rucalendar.reminders.ReminderWorker;
import com.plumsoftware.rucalendar.reminders.TimeChangeReceiver;
import com.plumsoftware.rucalendar.ui.MarkerBitmaps;
import com.plumsoftware.rucalendar.ui.TypeColors;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Собирает три виджета рабочего стола (ТЗ п. 5.1). Виджеты обновляются в 00:00 по местному времени,
 * при смене часового пояса, после перезагрузки и когда меняются данные в приложении.
 */
public final class WidgetUpdater {
    private static final String[] WEEKDAY_LABELS = {"ПН", "ВТ", "СР", "ЧТ", "ПТ", "СБ", "ВС"};
    private static final int[] DAY_IDS = {R.id.week_day_0, R.id.week_day_1, R.id.week_day_2, R.id.week_day_3,
            R.id.week_day_4, R.id.week_day_5, R.id.week_day_6};
    private static final int[] LABEL_IDS = {R.id.week_label_0, R.id.week_label_1, R.id.week_label_2, R.id.week_label_3,
            R.id.week_label_4, R.id.week_label_5, R.id.week_label_6};
    private static final int[] NUMBER_IDS = {R.id.week_number_0, R.id.week_number_1, R.id.week_number_2,
            R.id.week_number_3, R.id.week_number_4, R.id.week_number_5, R.id.week_number_6};
    private static final int[] MARKER_IDS = {R.id.week_markers_0, R.id.week_markers_1, R.id.week_markers_2,
            R.id.week_markers_3, R.id.week_markers_4, R.id.week_markers_5, R.id.week_markers_6};
    private static final int[] EVENT_IDS = {R.id.week_event_0, R.id.week_event_1};
    private static final int[] EVENT_MARKER_IDS = {R.id.week_event_marker_0, R.id.week_event_marker_1};
    private static final int[] EVENT_TITLE_IDS = {R.id.week_event_title_0, R.id.week_event_title_1};
    private static final int[] EVENT_RELATIVE_IDS = {R.id.week_event_relative_0, R.id.week_event_relative_1};

    private WidgetUpdater() {
    }

    /** Обновить все установленные виджеты и запланировать обновление в полночь. */
    public static void updateAll(Context context) {
        Context app = context.getApplicationContext();
        AppWidgetManager manager = AppWidgetManager.getInstance(app);
        update(app, manager, ids(app, manager, TodayWidgetProvider.class), Kind.TODAY);
        update(app, manager, ids(app, manager, WeekendWidgetProvider.class), Kind.WEEKEND);
        update(app, manager, ids(app, manager, WeekWidgetProvider.class), Kind.WEEK);
        scheduleMidnight(app);
    }

    enum Kind {TODAY, WEEKEND, WEEK}

    static void update(Context context, AppWidgetManager manager, int[] ids, Kind kind) {
        if (ids.length == 0) return;
        RemoteViews views;
        switch (kind) {
            case TODAY:
                views = today(context);
                break;
            case WEEKEND:
                views = weekend(context);
                break;
            default:
                views = week(context);
        }
        manager.updateAppWidget(ids, views);
    }

    private static int[] ids(Context context, AppWidgetManager manager, Class<?> provider) {
        return manager.getAppWidgetIds(new ComponentName(context, provider));
    }

    // region «Сегодня»

    private static RemoteViews today(Context context) {
        LocalDate today = LocalDate.now();
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_today);
        views.setTextViewText(R.id.widget_stripe, (RuDates.monthName(today.getMonthValue()) + " · "
                + RuDates.weekdayShort(today)).toUpperCase(new Locale("ru")));
        views.setTextViewText(R.id.widget_day, String.valueOf(today.getDayOfMonth()));

        List<DayEvent> events = new CalendarRepository(context).eventsOn(today);
        if (events.isEmpty()) {
            // Если событий нет — день недели полностью
            views.setViewVisibility(R.id.widget_marker, View.GONE);
            views.setTextViewText(R.id.widget_event, RuDates.weekday(today));
        } else {
            DayEvent first = events.get(0);
            views.setViewVisibility(R.id.widget_marker, View.VISIBLE);
            views.setImageViewBitmap(R.id.widget_marker, MarkerBitmaps.marker(context, first.type, first.userColor));
            views.setTextViewText(R.id.widget_event, first.title);
        }
        views.setOnClickPendingIntent(R.id.widget_root, open(context, monthUri(today), 1000));
        return views;
    }

    // endregion

    // region «До выходного»

    private static RemoteViews weekend(Context context) {
        LocalDate today = LocalDate.now();
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_weekend);
        // Считаются только официальные праздники
        HolidayRepository.Occurrence next = HolidayRepository.get(context).nextNonWorking(today);
        if (next == null) {
            views.setTextViewText(R.id.widget_count, "—");
            views.setTextViewText(R.id.widget_count_word, "");
            views.setTextViewText(R.id.widget_holiday, "");
            return views;
        }
        long days = RuDates.daysBetween(today, next.date);
        String full = RuDates.days(days);
        views.setTextViewText(R.id.widget_count, String.valueOf(days));
        views.setTextViewText(R.id.widget_count_word, full.substring(full.indexOf(' ') + 1));
        views.setTextViewText(R.id.widget_holiday, RuDates.dayMonth(next.date) + " · " + next.holiday.name);
        views.setOnClickPendingIntent(R.id.widget_root,
                open(context, ReminderWorker.dayUri(next.date, next.holiday.id), 2000));
        return views;
    }

    // endregion

    // region «Эта неделя»

    private static RemoteViews week(Context context) {
        LocalDate today = LocalDate.now();
        LocalDate monday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate sunday = monday.plusDays(6);
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_week);
        views.setTextViewText(R.id.week_range, range(monday, sunday));

        CalendarRepository repository = new CalendarRepository(context);
        int[] typeColors = TypeColors.colors(context);
        Map<YearMonth, MonthModel> models = new HashMap<>();
        int primary = ContextCompat.getColor(context, R.color.ds_text_primary);
        int secondary = ContextCompat.getColor(context, R.color.ds_text_secondary);
        int weekend = ContextCompat.getColor(context, R.color.ds_weekend_text);

        for (int i = 0; i < 7; i++) {
            LocalDate d = monday.plusDays(i);
            YearMonth ym = YearMonth.from(d);
            MonthModel model = models.get(ym);
            if (model == null) {
                model = repository.month(ym);
                models.put(ym, model);
            }
            MonthModel.Day day = model.days[d.getDayOfMonth() - 1];
            boolean past = d.isBefore(today);
            int numberColor = day.dayOff ? weekend : (past ? secondary : primary);

            views.setTextViewText(LABEL_IDS[i], WEEKDAY_LABELS[i]);
            views.setTextColor(LABEL_IDS[i], day.dayOff ? weekend : secondary);
            views.setTextViewText(NUMBER_IDS[i], String.valueOf(d.getDayOfMonth()));
            views.setTextColor(NUMBER_IDS[i], numberColor);
            // Рамка «сегодня»
            views.setInt(NUMBER_IDS[i], "setBackgroundResource", d.equals(today) ? R.drawable.widget_today_ring : 0);
            views.setImageViewBitmap(MARKER_IDS[i], MarkerBitmaps.row(context, day.markers, typeColors));
            views.setContentDescription(DAY_IDS[i], day.accessibilityText);
            views.setOnClickPendingIntent(DAY_IDS[i], open(context, monthUri(d), 3000 + i));
        }

        // Два ближайших события с отсчётом, свои события — наравне с праздниками
        List<DayEvent> upcoming = new ArrayList<>();
        for (LocalDate d = today; upcoming.size() < 2 && d.isBefore(today.plusDays(366)); d = d.plusDays(1)) {
            for (DayEvent e : repository.eventsOn(d)) {
                if (upcoming.size() < 2) upcoming.add(e);
            }
        }
        views.setViewVisibility(R.id.week_events, upcoming.isEmpty() ? View.GONE : View.VISIBLE);
        for (int i = 0; i < EVENT_IDS.length; i++) {
            if (i >= upcoming.size()) {
                views.setViewVisibility(EVENT_IDS[i], View.GONE);
                continue;
            }
            DayEvent e = upcoming.get(i);
            views.setViewVisibility(EVENT_IDS[i], View.VISIBLE);
            views.setImageViewBitmap(EVENT_MARKER_IDS[i], MarkerBitmaps.marker(context, e.type, e.userColor));
            views.setTextViewText(EVENT_TITLE_IDS[i], e.title);
            views.setTextViewText(EVENT_RELATIVE_IDS[i], relative(RuDates.daysBetween(today, e.date)));
            views.setOnClickPendingIntent(EVENT_IDS[i], open(context, ReminderWorker.dayUri(e.date, e.key()), 3100 + i));
        }
        return views;
    }

    /** «28 сентября – 4 октября»; в пределах месяца — «5 – 11 октября». */
    private static String range(LocalDate from, LocalDate to) {
        if (from.getMonthValue() == to.getMonthValue()) {
            return from.getDayOfMonth() + " – " + RuDates.dayMonth(to);
        }
        return RuDates.dayMonth(from) + " – " + RuDates.dayMonth(to);
    }

    private static String relative(long days) {
        if (days == 0) return "сегодня";
        if (days == 1) return "завтра";
        return RuDates.days(days);
    }

    // endregion

    /** rucalendar://month?date=2026-10-03 — открывает «Месяц» на этом дне. */
    static Uri monthUri(LocalDate date) {
        return new Uri.Builder().scheme("rucalendar").authority("month")
                .appendQueryParameter("date", date.toString())
                .build();
    }

    private static PendingIntent open(Context context, Uri uri, int requestCode) {
        Intent intent = new Intent(Intent.ACTION_VIEW, uri, context, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        return PendingIntent.getActivity(context, requestCode, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    /** Обновление в 00:00 по местному времени. */
    private static void scheduleMidnight(Context context) {
        AlarmManager alarms = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarms == null) return;
        long midnight = LocalDate.now().plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli() + 1000;
        Intent intent = new Intent(context, TimeChangeReceiver.class).setAction(TimeChangeReceiver.ACTION_MIDNIGHT);
        PendingIntent pending = PendingIntent.getBroadcast(context, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        boolean exact = Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarms.canScheduleExactAlarms();
        if (exact && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarms.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, midnight, pending);
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarms.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, midnight, pending);
        } else {
            alarms.setExact(AlarmManager.RTC_WAKEUP, midnight, pending);
        }
    }
}

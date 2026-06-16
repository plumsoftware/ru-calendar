package com.plumsoftware.rucalendar.widgets;

import java.util.Calendar;
import java.util.Locale;
import java.util.TimeZone;

public final class WidgetDateUtils {

    private static final String[] MONTHS_GENITIVE = {
            "января", "февраля", "марта", "апреля", "мая", "июня",
            "июля", "августа", "сентября", "октября", "ноября", "декабря"
    };

    private WidgetDateUtils() {
    }

    public static long toEpochDay(Calendar calendar) {
        Calendar utc = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        utc.set(Calendar.YEAR, calendar.get(Calendar.YEAR));
        utc.set(Calendar.MONTH, calendar.get(Calendar.MONTH));
        utc.set(Calendar.DAY_OF_MONTH, calendar.get(Calendar.DAY_OF_MONTH));
        utc.set(Calendar.HOUR_OF_DAY, 0);
        utc.set(Calendar.MINUTE, 0);
        utc.set(Calendar.SECOND, 0);
        utc.set(Calendar.MILLISECOND, 0);
        long millis = utc.getTimeInMillis();
        return millis / 86_400_000L;
    }

    public static Calendar fromEpochDay(long epochDay) {
        Calendar utc = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        utc.setTimeInMillis(epochDay * 86_400_000L);
        Calendar local = Calendar.getInstance();
        local.set(Calendar.YEAR, utc.get(Calendar.YEAR));
        local.set(Calendar.MONTH, utc.get(Calendar.MONTH));
        local.set(Calendar.DAY_OF_MONTH, utc.get(Calendar.DAY_OF_MONTH));
        local.set(Calendar.HOUR_OF_DAY, 12);
        local.set(Calendar.MINUTE, 0);
        local.set(Calendar.SECOND, 0);
        local.set(Calendar.MILLISECOND, 0);
        return local;
    }

    public static Calendar todayAtNoon() {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, 12);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar;
    }

    public static boolean isSameDay(Calendar first, Calendar second) {
        return first.get(Calendar.YEAR) == second.get(Calendar.YEAR)
                && first.get(Calendar.MONTH) == second.get(Calendar.MONTH)
                && first.get(Calendar.DAY_OF_MONTH) == second.get(Calendar.DAY_OF_MONTH);
    }

    public static String formatMonthYear(Calendar calendar) {
        return MONTHS_GENITIVE[calendar.get(Calendar.MONTH)] + " " + calendar.get(Calendar.YEAR);
    }

    public static String formatWeekdayShort(Calendar calendar) {
        return new java.text.SimpleDateFormat("EE", new Locale("ru"))
                .format(calendar.getTime())
                .replace(".", "");
    }
}

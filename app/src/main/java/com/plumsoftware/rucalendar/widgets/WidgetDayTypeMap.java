package com.plumsoftware.rucalendar.widgets;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

final class WidgetDayTypeMap {

    private static final Map<String, String> OVERRIDES = new HashMap<>();

    static {
        put(Calendar.JANUARY, 1, "holiday");
        put(Calendar.JANUARY, 2, "holiday");
        put(Calendar.JANUARY, 3, "holiday");
        put(Calendar.JANUARY, 4, "holiday");
        put(Calendar.JANUARY, 5, "holiday");
        put(Calendar.JANUARY, 6, "holiday");
        put(Calendar.JANUARY, 7, "holiday");
        put(Calendar.JANUARY, 8, "holiday");
        put(Calendar.JANUARY, 12, "prof");
        put(Calendar.JANUARY, 13, "mDate");
        put(Calendar.JANUARY, 14, "not official holiday");
        put(Calendar.JANUARY, 21, "mDate");
        put(Calendar.JANUARY, 25, "not official holiday");
        put(Calendar.JANUARY, 27, "mDate");

        put(Calendar.FEBRUARY, 8, "prof");
        put(Calendar.FEBRUARY, 9, "prof");
        put(Calendar.FEBRUARY, 10, "prof");
        put(Calendar.FEBRUARY, 14, "not official holiday");
        put(Calendar.FEBRUARY, 15, "mDate");
        put(Calendar.FEBRUARY, 23, "holiday");
        put(Calendar.FEBRUARY, 27, "prof");

        put(Calendar.MARCH, 8, "holiday");
        put(Calendar.MARCH, 9, "prof");
        put(Calendar.MARCH, 11, "prof");
        put(Calendar.MARCH, 12, "prof");
        put(Calendar.MARCH, 14, "prof");
        put(Calendar.MARCH, 18, "not official holiday");
        put(Calendar.MARCH, 19, "prof");
        put(Calendar.MARCH, 27, "prof");
        put(Calendar.MARCH, 29, "prof");

        put(Calendar.APRIL, 1, "not official holiday");
        put(Calendar.APRIL, 2, "not official holiday");
        put(Calendar.APRIL, 4, "prof");
        put(Calendar.APRIL, 8, "prof");
        put(Calendar.APRIL, 12, "mDate");
        put(Calendar.APRIL, 26, "mDate");
        put(Calendar.APRIL, 27, "mDate");
        put(Calendar.APRIL, 28, "prof");
        put(Calendar.APRIL, 30, "prof");

        put(Calendar.MAY, 1, "holiday");
        put(Calendar.MAY, 7, "prof");
        put(Calendar.MAY, 9, "holiday");
        put(Calendar.MAY, 20, "prof");
        put(Calendar.MAY, 21, "prof");
        put(Calendar.MAY, 24, "prof");
        put(Calendar.MAY, 25, "prof");
        put(Calendar.MAY, 26, "prof");
        put(Calendar.MAY, 27, "prof");
        put(Calendar.MAY, 28, "prof");
        put(Calendar.MAY, 29, "prof");
        put(Calendar.MAY, 31, "prof");

        put(Calendar.JUNE, 1, "not official holiday");
        put(Calendar.JUNE, 2, "mDate");
        put(Calendar.JUNE, 5, "prof");
        put(Calendar.JUNE, 6, "not official holiday");
        put(Calendar.JUNE, 8, "prof");
        put(Calendar.JUNE, 12, "holiday");
        put(Calendar.JUNE, 14, "prof");
        put(Calendar.JUNE, 20, "prof");
        put(Calendar.JUNE, 22, "mDate");
        put(Calendar.JUNE, 26, "prof");
        put(Calendar.JUNE, 27, "not official holiday");
        put(Calendar.JUNE, 30, "prof");

        put(Calendar.JULY, 3, "prof");
        put(Calendar.JULY, 4, "prof");
        put(Calendar.JULY, 8, "not official holiday");
        put(Calendar.JULY, 11, "prof");
        put(Calendar.JULY, 17, "prof");
        put(Calendar.JULY, 18, "prof");
        put(Calendar.JULY, 25, "prof");
        put(Calendar.JULY, 28, "mDate");
        put(Calendar.JULY, 30, "prof");

        put(Calendar.AUGUST, 2, "mDate");
        put(Calendar.AUGUST, 6, "prof");
        put(Calendar.AUGUST, 8, "prof");
        put(Calendar.AUGUST, 12, "mDate");
        put(Calendar.AUGUST, 15, "prof");
        put(Calendar.AUGUST, 18, "prof");
        put(Calendar.AUGUST, 22, "not official holiday");
        put(Calendar.AUGUST, 27, "prof");
        put(Calendar.AUGUST, 29, "prof");
        put(Calendar.AUGUST, 31, "prof");

        put(Calendar.SEPTEMBER, 1, "not official holiday");
        put(Calendar.SEPTEMBER, 3, "mDate");
        put(Calendar.SEPTEMBER, 4, "mDate");
        put(Calendar.SEPTEMBER, 5, "prof");
        put(Calendar.SEPTEMBER, 8, "prof");
        put(Calendar.SEPTEMBER, 9, "prof");
        put(Calendar.SEPTEMBER, 12, "prof");
        put(Calendar.SEPTEMBER, 13, "prof");
        put(Calendar.SEPTEMBER, 19, "prof");
        put(Calendar.SEPTEMBER, 24, "prof");
        put(Calendar.SEPTEMBER, 26, "prof");
        put(Calendar.SEPTEMBER, 27, "not official holiday");
        put(Calendar.SEPTEMBER, 28, "prof");

        put(Calendar.OCTOBER, 1, "not official holiday");
        put(Calendar.OCTOBER, 4, "mDate");
        put(Calendar.OCTOBER, 5, "prof");
        put(Calendar.OCTOBER, 6, "prof");
        put(Calendar.OCTOBER, 16, "not official holiday");
        put(Calendar.OCTOBER, 20, "prof");
        put(Calendar.OCTOBER, 23, "prof");
        put(Calendar.OCTOBER, 24, "mDate");
        put(Calendar.OCTOBER, 25, "prof");
        put(Calendar.OCTOBER, 29, "prof");
        put(Calendar.OCTOBER, 30, "mDate");
        put(Calendar.OCTOBER, 31, "prof");

        put(Calendar.NOVEMBER, 1, "prof");
        put(Calendar.NOVEMBER, 4, "holiday");
        put(Calendar.NOVEMBER, 5, "prof");
        put(Calendar.NOVEMBER, 7, "mDate");
        put(Calendar.NOVEMBER, 9, "prof");
        put(Calendar.NOVEMBER, 10, "prof");
        put(Calendar.NOVEMBER, 11, "prof");
        put(Calendar.NOVEMBER, 13, "mDate");
        put(Calendar.NOVEMBER, 14, "prof");
        put(Calendar.NOVEMBER, 21, "prof");
        put(Calendar.NOVEMBER, 22, "prof");
        put(Calendar.NOVEMBER, 27, "not official holiday");
        put(Calendar.NOVEMBER, 30, "not official holiday");

        put(Calendar.DECEMBER, 3, "mDate");
        put(Calendar.DECEMBER, 5, "prof");
        put(Calendar.DECEMBER, 9, "mDate");
        put(Calendar.DECEMBER, 12, "mDate");
        put(Calendar.DECEMBER, 18, "prof");
        put(Calendar.DECEMBER, 20, "prof");
        put(Calendar.DECEMBER, 22, "prof");
        put(Calendar.DECEMBER, 27, "prof");
    }

    private WidgetDayTypeMap() {
    }

    private static void put(int month, int day, String type) {
        OVERRIDES.put(month + ":" + day, type);
    }

    static String getDayType(int month, int day) {
        String type = OVERRIDES.get(month + ":" + day);
        return type != null ? type : "default";
    }

    static WidgetEventType toEventType(String dayType) {
        if ("holiday".equals(dayType)) {
            return WidgetEventType.HOLIDAY;
        }
        if ("mDate".equals(dayType)) {
            return WidgetEventType.MEMORY;
        }
        if ("prof".equals(dayType) || "current".equals(dayType)) {
            return WidgetEventType.PROFESSIONAL;
        }
        if ("not official holiday".equals(dayType)) {
            return WidgetEventType.UNOFFICIAL;
        }
        return null;
    }
}

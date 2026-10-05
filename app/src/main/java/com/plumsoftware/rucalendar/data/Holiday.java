package com.plumsoftware.rucalendar.data;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;

/**
 * Праздник из базы assets/holidays.json (ТЗ п. 6.1).
 * Дата фиксированная (month + day) или плавающая (month + nth + weekday, nth = -1 — последний).
 */
public final class Holiday {
    public final String id;
    public final int month;
    public final int day;
    public final int nth;
    public final int weekday;
    public final EventType type;
    public final String name;
    public final String description;
    public final boolean nonWorking;

    public Holiday(String id, int month, int day, int nth, int weekday, EventType type,
                   String name, String description, boolean nonWorking) {
        this.id = id;
        this.month = month;
        this.day = day;
        this.nth = nth;
        this.weekday = weekday;
        this.type = type;
        this.name = name;
        this.description = description;
        this.nonWorking = nonWorking;
    }

    public boolean isFloating() {
        return nth != 0;
    }

    /** Дата праздника в указанном году. */
    public LocalDate dateIn(int year) {
        if (isFloating()) {
            return LocalDate.of(year, month, 1)
                    .with(TemporalAdjusters.dayOfWeekInMonth(nth, DayOfWeek.of(weekday)));
        }
        if (month == 2 && day == 29 && !LocalDate.of(year, 1, 1).isLeapYear()) {
            return LocalDate.of(year, 2, 28);
        }
        return LocalDate.of(year, month, day);
    }
}

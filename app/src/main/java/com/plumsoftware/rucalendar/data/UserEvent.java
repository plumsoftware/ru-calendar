package com.plumsoftware.rucalendar.data;

import java.time.LocalDate;

/** Собственное событие пользователя (ТЗ п. 6.1). Хранится отдельно от базы праздников. */
public final class UserEvent {
    public static final int REMINDER_OFF = -1;

    public final String id;
    public final String title;
    public final String description;
    public final LocalDate date;
    public final boolean yearly;
    public final int color;
    /** Упреждение в днях: 0, 1 или 7; REMINDER_OFF — напоминание выключено. */
    public final int reminderDaysBefore;
    /** Время напоминания в минутах от полуночи. */
    public final int reminderMinutes;

    public UserEvent(String id, String title, String description, LocalDate date, boolean yearly,
                     int color, int reminderDaysBefore, int reminderMinutes) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.date = date;
        this.yearly = yearly;
        this.color = color;
        this.reminderDaysBefore = reminderDaysBefore;
        this.reminderMinutes = reminderMinutes;
    }

    /** Ежегодное событие 29 февраля в невисокосный год показывается 28 февраля (п. 4.6). */
    public boolean occursOn(LocalDate day) {
        if (!yearly) return date.equals(day);
        if (day.getYear() < date.getYear()) return false;
        int month = date.getMonthValue();
        int dom = date.getDayOfMonth();
        if (month == 2 && dom == 29 && !day.isLeapYear()) dom = 28;
        return day.getMonthValue() == month && day.getDayOfMonth() == dom;
    }
}

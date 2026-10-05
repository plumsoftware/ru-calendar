package com.plumsoftware.rucalendar.data;

import androidx.annotation.Nullable;

import java.time.LocalDate;

/** Строка списка дня: праздник из базы или собственное событие. */
public final class DayEvent {
    public final LocalDate date;
    public final EventType type;
    public final String title;
    /** Цвет звезды для собственного события; для праздников не используется. */
    public final int userColor;
    @Nullable
    public final Holiday holiday;
    @Nullable
    public final UserEvent userEvent;

    private DayEvent(LocalDate date, EventType type, String title, int userColor,
                     @Nullable Holiday holiday, @Nullable UserEvent userEvent) {
        this.date = date;
        this.type = type;
        this.title = title;
        this.userColor = userColor;
        this.holiday = holiday;
        this.userEvent = userEvent;
    }

    public static DayEvent of(LocalDate date, Holiday holiday) {
        return new DayEvent(date, holiday.type, holiday.name, 0, holiday, null);
    }

    public static DayEvent of(LocalDate date, UserEvent event) {
        return new DayEvent(date, EventType.USER, event.title, event.color, null, event);
    }

    /** Подпись под названием: тип словами, для своих событий — с повтором (п. 4.1). */
    public String subtitle() {
        if (userEvent != null) {
            return EventType.USER.label + (userEvent.yearly ? " · каждый год" : " · один раз");
        }
        return type.label;
    }

    /** Устойчивый ключ события: id праздника или своего события. */
    public String key() {
        if (holiday != null) return holiday.id;
        return userEvent != null ? userEvent.id : title;
    }

    public String description() {
        if (holiday != null) return holiday.description;
        return userEvent != null ? userEvent.description : "";
    }
}

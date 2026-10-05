package com.plumsoftware.rucalendar.data;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

/** Подготовленные данные месяца для отрисовки сетки. */
public final class MonthModel {
    public final YearMonth month;
    /** Индекс = число месяца − 1. */
    public final Day[] days;

    public MonthModel(YearMonth month, Day[] days) {
        this.month = month;
        this.days = days;
    }

    public static final class Day {
        public final LocalDate date;
        /** Выходной по производственному календарю: красное число. */
        public final boolean dayOff;
        /** Нерабочий официальный праздник: белое число на красной плашке. */
        public final boolean officialHoliday;
        public final List<Marker> markers;
        public final String accessibilityText;

        public Day(LocalDate date, boolean dayOff, boolean officialHoliday, List<Marker> markers, String accessibilityText) {
            this.date = date;
            this.dayOff = dayOff;
            this.officialHoliday = officialHoliday;
            this.markers = markers;
            this.accessibilityText = accessibilityText;
        }
    }

    public static final class Marker {
        public final EventType type;
        /** Цвет звезды для собственного события. */
        public final int userColor;

        public Marker(EventType type, int userColor) {
            this.type = type;
            this.userColor = userColor;
        }
    }
}

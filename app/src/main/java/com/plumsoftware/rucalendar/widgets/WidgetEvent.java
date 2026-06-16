package com.plumsoftware.rucalendar.widgets;

import java.util.Calendar;

public class WidgetEvent {
    private final String name;
    private final String description;
    private final WidgetEventType type;
    private final Calendar date;

    public WidgetEvent(String name, String description, WidgetEventType type, Calendar date) {
        this.name = name;
        this.description = description;
        this.type = type;
        this.date = date;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public WidgetEventType getType() {
        return type;
    }

    public Calendar getDate() {
        return date;
    }

    public int getDayOfMonth() {
        return date.get(Calendar.DAY_OF_MONTH);
    }

    public long getEpochDay() {
        return WidgetDateUtils.toEpochDay(date);
    }
}

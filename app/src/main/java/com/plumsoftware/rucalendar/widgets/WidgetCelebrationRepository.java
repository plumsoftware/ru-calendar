package com.plumsoftware.rucalendar.widgets;

import android.content.Context;

import com.plumsoftware.rucalendar.events.Celebrations;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class WidgetCelebrationRepository {

    private static final int MAX_TODAY_VISIBLE = 4;
    private static final int MAX_WEEK_UPCOMING = 4;

    private WidgetCelebrationRepository() {
    }

    public static List<WidgetEvent> getEventsForDate(Calendar date) {
        int month = date.get(Calendar.MONTH);
        int day = date.get(Calendar.DAY_OF_MONTH);
        String dayType = WidgetDayTypeMap.getDayType(month, day);
        List<WidgetEvent> events = new ArrayList<>();

        Celebrations celebrations = new Celebrations(month, day);
        String raw;
        try {
            raw = celebrations.getDescription();
        } catch (IndexOutOfBoundsException e) {
            raw = "";
        }

        if (raw != null && !raw.isEmpty()) {
            String[] parts = raw.split("~del");
            for (String part : parts) {
                String[] pieces = part.split("~");
                if (pieces.length < 2) {
                    continue;
                }
                String name = pieces[0];
                String description = pieces[1];
                WidgetEvent event = buildNamedEvent(name, description, dayType, date);
                if (event != null) {
                    events.add(event);
                }
            }
        }

        if (events.isEmpty()) {
            WidgetEvent fallback = buildFallbackEvent(dayType, date);
            if (fallback != null) {
                events.add(fallback);
            }
        }

        return events;
    }

    public static List<WidgetEvent> getTodayEvents() {
        return getEventsForDate(WidgetDateUtils.todayAtNoon());
    }

    public static int getHiddenTodayEventsCount() {
        List<WidgetEvent> all = getTodayEvents();
        return Math.max(0, all.size() - MAX_TODAY_VISIBLE);
    }

    public static List<WidgetEvent> getTodayEventsForList() {
        List<WidgetEvent> all = getTodayEvents();
        if (all.size() <= MAX_TODAY_VISIBLE) {
            return all;
        }
        return new ArrayList<>(all.subList(0, MAX_TODAY_VISIBLE));
    }

    public static List<Calendar> getWeekStripDays() {
        Calendar today = WidgetDateUtils.todayAtNoon();
        List<Calendar> days = new ArrayList<>(7);

        if (today.get(Calendar.DAY_OF_WEEK) == Calendar.MONDAY) {
            for (int i = 0; i < 7; i++) {
                Calendar day = (Calendar) today.clone();
                day.add(Calendar.DAY_OF_MONTH, i);
                days.add(day);
            }
            return days;
        }

        for (int offset = -3; offset <= 3; offset++) {
            Calendar day = (Calendar) today.clone();
            day.add(Calendar.DAY_OF_MONTH, offset);
            days.add(day);
        }
        return days;
    }

    public static Set<WidgetEventType> getEventTypesForDate(Calendar date) {
        List<WidgetEvent> events = getEventsForDate(date);
        Set<WidgetEventType> types = new LinkedHashSet<>();
        for (WidgetEvent event : events) {
            types.add(event.getType());
        }
        return types;
    }

    public static List<WidgetEvent> getUpcomingEvents() {
        Calendar start = WidgetDateUtils.todayAtNoon();
        List<WidgetEvent> upcoming = new ArrayList<>();

        for (int offset = 0; offset <= 7; offset++) {
            Calendar day = (Calendar) start.clone();
            day.add(Calendar.DAY_OF_MONTH, offset);
            upcoming.addAll(getEventsForDate(day));
        }

        Collections.sort(upcoming, new Comparator<WidgetEvent>() {
            @Override
            public int compare(WidgetEvent left, WidgetEvent right) {
                long leftDay = WidgetDateUtils.toEpochDay(left.getDate());
                long rightDay = WidgetDateUtils.toEpochDay(right.getDate());
                if (leftDay != rightDay) {
                    return Long.compare(leftDay, rightDay);
                }
                int typeCompare = Integer.compare(
                        left.getType().getSortPriority(),
                        right.getType().getSortPriority()
                );
                if (typeCompare != 0) {
                    return typeCompare;
                }
                return left.getName().compareToIgnoreCase(right.getName());
            }
        });

        if (upcoming.size() <= MAX_WEEK_UPCOMING) {
            return upcoming;
        }
        return new ArrayList<>(upcoming.subList(0, MAX_WEEK_UPCOMING));
    }

    public static String typeLabel(Context context, WidgetEventType type) {
        return context.getString(type.getLabelResId());
    }

    private static WidgetEvent buildNamedEvent(
            String name,
            String description,
            String dayType,
            Calendar date
    ) {
        if ("День российской науки".equals(name)) {
            return new WidgetEvent(name, description, WidgetEventType.MEMORY, date);
        }
        if ("День юриста".equals(name)) {
            return new WidgetEvent(name, description, WidgetEventType.PROFESSIONAL, date);
        }

        if ("holiday".equals(dayType) && !name.isEmpty() && !description.isEmpty()) {
            return new WidgetEvent(name, description, WidgetEventType.HOLIDAY, date);
        }
        if ("current".equals(dayType) && !name.isEmpty() && !description.isEmpty()) {
            return new WidgetEvent(name, description, WidgetEventType.PROFESSIONAL, date);
        }
        if ("mDate".equals(dayType)) {
            return new WidgetEvent(name, description, WidgetEventType.MEMORY, date);
        }
        if ("prof".equals(dayType)) {
            return new WidgetEvent(name, description, WidgetEventType.PROFESSIONAL, date);
        }
        if ("not official holiday".equals(dayType)) {
            return new WidgetEvent(name, description, WidgetEventType.UNOFFICIAL, date);
        }

        WidgetEventType inferred = WidgetDayTypeMap.toEventType(dayType);
        if (inferred != null && !name.isEmpty()) {
            return new WidgetEvent(name, description, inferred, date);
        }
        if (!name.isEmpty()) {
            return new WidgetEvent(name, description, WidgetEventType.PROFESSIONAL, date);
        }
        return null;
    }

    private static WidgetEvent buildFallbackEvent(String dayType, Calendar date) {
        if ("holiday".equals(dayType)) {
            return new WidgetEvent(
                    "Выходной",
                    "Отличный повод встретиться с друзьями!",
                    WidgetEventType.HOLIDAY,
                    date
            );
        }
        return null;
    }
}

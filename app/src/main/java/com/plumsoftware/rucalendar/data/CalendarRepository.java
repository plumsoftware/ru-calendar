package com.plumsoftware.rucalendar.data;

import android.content.Context;

import java.text.Collator;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Сводит праздники, собственные события, фильтры и производственный календарь для экранов. */
public final class CalendarRepository {
    private final HolidayRepository holidays;
    private final UserEventRepository userEvents;
    private final ProductionCalendarRepository production;
    private final FilterPreferences filters;
    private final Comparator<DayEvent> order;

    public CalendarRepository(Context context) {
        holidays = HolidayRepository.get(context);
        userEvents = UserEventRepository.get(context);
        production = ProductionCalendarRepository.get(context);
        filters = new FilterPreferences(context);
        final Collator collator = Collator.getInstance(new Locale("ru"));
        order = (a, b) -> {
            int byType = a.type.compareTo(b.type);
            return byType != 0 ? byType : collator.compare(a.title, b.title);
        };
    }

    public FilterPreferences filters() {
        return filters;
    }

    public HolidayRepository holidays() {
        return holidays;
    }

    public ProductionCalendarRepository production() {
        return production;
    }

    /** События дня с учётом фильтров, в порядке п. 6.3. */
    public List<DayEvent> eventsOn(LocalDate date) {
        List<DayEvent> result = new ArrayList<>();
        for (Holiday holiday : holidays.on(date)) {
            if (filters.isEnabled(holiday.type)) result.add(DayEvent.of(date, holiday));
        }
        if (filters.isEnabled(EventType.USER)) {
            for (UserEvent event : userEvents.on(date)) result.add(DayEvent.of(date, event));
        }
        Collections.sort(result, order);
        return result;
    }

    /** События с учётом фильтров за период [from, toExclusive) в хронологическом порядке. */
    public List<DayEvent> range(LocalDate from, LocalDate toExclusive) {
        List<DayEvent> result = new ArrayList<>();
        for (LocalDate d = from; d.isBefore(toExclusive); d = d.plusDays(1)) result.addAll(eventsOn(d));
        return result;
    }

    /**
     * Поиск по названию и описанию без учёта регистра, «е» и «ё» — одна буква;
     * запрос вида «4 ноября» ищет по дате (п. 4.4).
     */
    public List<DayEvent> search(String query, LocalDate from, LocalDate toExclusive) {
        return filter(range(from, toExclusive), query);
    }

    /**
     * Избранные праздники за период в хронологическом порядке. Фильтры типов не применяются:
     * в избранное праздник добавляют вручную.
     */
    public List<DayEvent> favorites(FavoritesRepository favorites, LocalDate from, LocalDate toExclusive) {
        List<DayEvent> result = new ArrayList<>();
        for (LocalDate d = from; d.isBefore(toExclusive); d = d.plusDays(1)) {
            List<DayEvent> day = new ArrayList<>();
            for (Holiday holiday : holidays.on(d)) {
                if (favorites.isFavorite(holiday.id)) day.add(DayEvent.of(d, holiday));
            }
            Collections.sort(day, order);
            result.addAll(day);
        }
        return result;
    }

    /** Оставить события, подходящие под запрос; пустой запрос — все. */
    public List<DayEvent> filter(List<DayEvent> events, String query) {
        String q = normalize(query);
        if (q.isEmpty()) return events;
        Matcher m = DATE_QUERY.matcher(q);
        int day = 0;
        int month = 0;
        if (m.matches()) {
            day = Integer.parseInt(m.group(1));
            month = RuDates.parseMonth(m.group(2));
        }
        List<DayEvent> result = new ArrayList<>();
        for (DayEvent event : events) {
            if (month != 0) {
                if (event.date.getDayOfMonth() == day && event.date.getMonthValue() == month) result.add(event);
            } else if (normalize(event.title).contains(q) || normalize(event.description()).contains(q)) {
                result.add(event);
            }
        }
        return result;
    }

    private static final Pattern DATE_QUERY = Pattern.compile("(\\d{1,2})\\s+([а-я]{3,})");

    static String normalize(String text) {
        return text.toLowerCase(new Locale("ru")).replace('ё', 'е').replaceAll("\\s+", " ").trim();
    }

    /** Модель месяца для сетки: по ячейке на каждый день месяца. */
    public MonthModel month(YearMonth month) {
        MonthModel.Day[] days = new MonthModel.Day[month.lengthOfMonth()];
        for (int i = 0; i < days.length; i++) {
            LocalDate date = month.atDay(i + 1);
            List<DayEvent> events = eventsOn(date);
            List<MonthModel.Marker> markers = new ArrayList<>(5);
            boolean[] seen = new boolean[EventType.values().length];
            for (DayEvent event : events) {
                // Не более одного маркера каждого типа (п. 2.1)
                if (seen[event.type.ordinal()]) continue;
                seen[event.type.ordinal()] = true;
                markers.add(new MonthModel.Marker(event.type, event.userColor));
            }
            days[i] = new MonthModel.Day(
                    date,
                    production.isDayOff(date),
                    holidays.nonWorkingOn(date) != null,
                    markers,
                    describe(date, events));
        }
        return new MonthModel(month, days);
    }

    /** Описание ячейки для TalkBack: дата, число событий, их типы (п. 7). */
    private static String describe(LocalDate date, List<DayEvent> events) {
        StringBuilder sb = new StringBuilder(RuDates.dayMonthWeekday(date));
        if (events.isEmpty()) {
            sb.append(", событий нет");
        } else {
            sb.append(", ").append(RuDates.plural(events.size(), "событие", "события", "событий"));
            for (DayEvent event : events) sb.append(", ").append(event.type.label);
        }
        return sb.toString();
    }
}

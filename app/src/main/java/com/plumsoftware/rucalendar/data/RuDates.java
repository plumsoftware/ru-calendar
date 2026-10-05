package com.plumsoftware.rucalendar.data;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/** Русские названия дат и склонения, без зависимости от локали устройства (интерфейс только на русском). */
public final class RuDates {
    private static final String[] MONTHS = {"Январь", "Февраль", "Март", "Апрель", "Май", "Июнь",
            "Июль", "Август", "Сентябрь", "Октябрь", "Ноябрь", "Декабрь"};
    private static final String[] MONTHS_GENITIVE = {"января", "февраля", "марта", "апреля", "мая", "июня",
            "июля", "августа", "сентября", "октября", "ноября", "декабря"};
    private static final String[] WEEKDAYS = {"Понедельник", "Вторник", "Среда", "Четверг", "Пятница",
            "Суббота", "Воскресенье"};

    private static final String[] WEEKDAYS_SHORT = {"пн", "вт", "ср", "чт", "пт", "сб", "вс"};
    private static final String[] WEEKDAYS_ON = {"в понедельник", "во вторник", "в среду", "в четверг",
            "в пятницу", "в субботу", "в воскресенье"};
    private static final String[] MONTHS_SHORT = {"янв", "фев", "мар", "апр", "мая", "июн",
            "июл", "авг", "сен", "окт", "ноя", "дек"};

    private RuDates() {
    }

    /** «Октябрь». */
    public static String monthName(int month) {
        return MONTHS[month - 1];
    }

    /** «октября». */
    public static String monthGenitive(int month) {
        return MONTHS_GENITIVE[month - 1];
    }

    /** «Суббота». */
    public static String weekday(LocalDate date) {
        return WEEKDAYS[date.getDayOfWeek().getValue() - 1];
    }

    /** «пн». */
    public static String weekdayShort(LocalDate date) {
        return WEEKDAYS_SHORT[date.getDayOfWeek().getValue() - 1];
    }

    /** «среда». */
    public static String weekdayLower(LocalDate date) {
        return weekday(date).toLowerCase(java.util.Locale.ROOT);
    }

    /** «во вторник». */
    public static String onWeekday(LocalDate date) {
        return WEEKDAYS_ON[date.getDayOfWeek().getValue() - 1];
    }

    /** «18 окт». */
    public static String dayMonthShort(LocalDate date) {
        return date.getDayOfMonth() + " " + MONTHS_SHORT[date.getMonthValue() - 1];
    }

    /** Индекс месяца 1–12 по началу слова в родительном или именительном падеже («ноя», «ноября»), иначе 0. */
    public static int parseMonth(String word) {
        if (word.length() < 3) return 0;
        for (int i = 0; i < 12; i++) {
            if (MONTHS_GENITIVE[i].startsWith(word) || MONTHS[i].toLowerCase(java.util.Locale.ROOT).startsWith(word)
                    || word.startsWith(MONTHS_GENITIVE[i].substring(0, 3))) {
                return i + 1;
            }
        }
        return 0;
    }

    /** «4 ноября». */
    public static String dayMonth(LocalDate date) {
        return date.getDayOfMonth() + " " + monthGenitive(date.getMonthValue());
    }

    /** «Суббота, 3 октября». */
    public static String weekdayDayMonth(LocalDate date) {
        return weekday(date) + ", " + dayMonth(date);
    }

    /** «3 октября, суббота». */
    public static String dayMonthWeekday(LocalDate date) {
        return dayMonth(date) + ", " + weekday(date).toLowerCase(java.util.Locale.ROOT);
    }

    /** Разница календарных дат по местному времени (п. 6.3). */
    public static long daysBetween(LocalDate from, LocalDate to) {
        return ChronoUnit.DAYS.between(from, to);
    }

    /** Чип с отсчётом в списке дня: «сегодня», «завтра», «через N дн.», «прошло» (п. 4.1). */
    public static String relativeShort(long days) {
        if (days == 0) return "сегодня";
        if (days == 1) return "завтра";
        if (days > 1) return "через " + days + " дн.";
        return "прошло";
    }

    /** Отсчёт в карточке дня и на экране праздника: «сегодня», «завтра», «через 32 дня», «прошло». */
    public static String relativeLong(long days) {
        if (days == 0) return "сегодня";
        if (days == 1) return "завтра";
        if (days > 1) return "через " + days(days);
        return "прошло";
    }

    /** Часть суток для подписи под датой уведомления. */
    public static String partOfDay(int minutes) {
        int hour = minutes / 60;
        if (hour >= 5 && hour < 12) return "утром";
        if (hour >= 12 && hour < 17) return "днём";
        if (hour >= 17 && hour < 23) return "вечером";
        return "ночью";
    }

    /** «09:00». */
    public static String time(int minutes) {
        return String.format(java.util.Locale.ROOT, "%02d:%02d", minutes / 60, minutes % 60);
    }

    /** «1 день», «2 дня», «5 дней». */
    public static String days(long n) {
        return plural(n, "день", "дня", "дней");
    }

    public static String plural(long n, String one, String few, String many) {
        long mod100 = Math.abs(n) % 100;
        long mod10 = mod100 % 10;
        String word;
        if (mod100 >= 11 && mod100 <= 14) word = many;
        else if (mod10 == 1) word = one;
        else if (mod10 >= 2 && mod10 <= 4) word = few;
        else word = many;
        return n + " " + word;
    }
}

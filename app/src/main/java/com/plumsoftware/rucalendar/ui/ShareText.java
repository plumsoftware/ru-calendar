package com.plumsoftware.rucalendar.ui;

import android.content.Intent;

import com.plumsoftware.rucalendar.data.Holiday;
import com.plumsoftware.rucalendar.data.RuDates;

import java.time.LocalDate;

/** Текст для системного меню «Поделиться». */
public final class ShareText {
    private ShareText() {
    }

    public static Intent intent(Holiday holiday, LocalDate date) {
        return intent(holiday.name, holiday.description, date);
    }

    public static Intent intent(String title, String description, LocalDate date) {
        String about = firstParagraph(description);
        String text = RuDates.dayMonth(date) + " — " + title + (about.isEmpty() ? "" : "\n\n" + about)
                + "\n\nКалендарь — праздники России";
        return new Intent(Intent.ACTION_SEND)
                .setType("text/plain")
                .putExtra(Intent.EXTRA_TEXT, text);
    }

    private static String firstParagraph(String text) {
        int end = text.indexOf('\n');
        return (end > 0 ? text.substring(0, end) : text).trim();
    }
}

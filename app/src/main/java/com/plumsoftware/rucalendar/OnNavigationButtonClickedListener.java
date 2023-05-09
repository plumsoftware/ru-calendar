package com.plumsoftware.rucalendar;

import java.util.Calendar;
import java.util.Map;

public interface OnNavigationButtonClickedListener {

    /**
     * Called when a month navigation button is called
     * @param whichButton Either {@code CustomCalendar.PREVIOUS} or {@code CustomCalendar.NEXT}
     * @param newMonth Calendar representation of the month that will be displayed next (including the day of month that will be selected)
     * @return For the new month, an array such that the first element is a map linking date to its description (This description will be accessible from the {@code desc} parameter of the onDateSelected method of OnDateSelectedListener) and the second element is a map linking date to the tag to be set on its date view (This tag will be accessible from the {@code view} parameter of the onDateSelected method of the OnDateSelectedListener)
     */
    public Map<Integer, Object>[] onNavigationButtonClicked(int whichButton, Calendar newMonth);
}

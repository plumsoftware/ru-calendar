package com.plumsoftware.rucalendar.widgets;

import com.plumsoftware.rucalendar.R;

public enum WidgetEventType {
    HOLIDAY(R.color.widget_dot_holiday, R.string.widget_type_holiday, 0),
    MEMORY(R.color.widget_dot_memory, R.string.widget_type_memory, 1),
    PROFESSIONAL(R.color.widget_dot_professional, R.string.widget_type_professional, 2),
    UNOFFICIAL(R.color.widget_dot_unofficial, R.string.widget_type_unofficial, 3);

    private final int colorResId;
    private final int labelResId;
    private final int sortPriority;

    WidgetEventType(int colorResId, int labelResId, int sortPriority) {
        this.colorResId = colorResId;
        this.labelResId = labelResId;
        this.sortPriority = sortPriority;
    }

    public int getColorResId() {
        return colorResId;
    }

    public int getLabelResId() {
        return labelResId;
    }

    public int getSortPriority() {
        return sortPriority;
    }
}

package com.plumsoftware.rucalendar.widgets;

import com.plumsoftware.rucalendar.R;

final class WidgetDotDrawables {

    private WidgetDotDrawables() {
    }

    static int drawableForType(WidgetEventType type) {
        switch (type) {
            case HOLIDAY:
                return R.drawable.widget_dot_holiday;
            case MEMORY:
                return R.drawable.widget_dot_memory;
            case PROFESSIONAL:
                return R.drawable.widget_dot_professional;
            case UNOFFICIAL:
            default:
                return R.drawable.widget_dot_unofficial;
        }
    }

    static int smallDrawableForType(WidgetEventType type) {
        switch (type) {
            case HOLIDAY:
                return R.drawable.widget_dot_holiday;
            case MEMORY:
                return R.drawable.widget_dot_memory;
            case PROFESSIONAL:
                return R.drawable.widget_dot_professional;
            case UNOFFICIAL:
            default:
                return R.drawable.widget_dot_unofficial;
        }
    }
}

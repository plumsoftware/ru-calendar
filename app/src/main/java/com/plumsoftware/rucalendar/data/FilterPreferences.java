package com.plumsoftware.rucalendar.data;

import android.content.Context;
import android.content.SharedPreferences;

/** Состояние фильтров типов событий; сохраняется между запусками (п. 4.1). */
public final class FilterPreferences {
    private static final String PREFS = "filters";

    private final SharedPreferences prefs;

    public FilterPreferences(Context context) {
        prefs = context.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public boolean isEnabled(EventType type) {
        return prefs.getBoolean(type.key, true);
    }

    public void setEnabled(EventType type, boolean enabled) {
        prefs.edit().putBoolean(type.key, enabled).apply();
    }
}

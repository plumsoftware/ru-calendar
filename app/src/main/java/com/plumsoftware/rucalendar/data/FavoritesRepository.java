package com.plumsoftware.rucalendar.data;

import android.content.Context;
import android.content.SharedPreferences;

/** Избранные праздники (ТЗ п. 6.3). Отдельного экрана избранного в макетах нет. */
public final class FavoritesRepository {
    private static final String PREFS = "favorites";

    private final SharedPreferences prefs;

    public FavoritesRepository(Context context) {
        prefs = context.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public boolean isFavorite(String holidayId) {
        return prefs.getBoolean(holidayId, false);
    }

    /** Переключить и вернуть новое состояние. */
    public boolean toggle(String holidayId) {
        boolean favorite = !isFavorite(holidayId);
        if (favorite) prefs.edit().putBoolean(holidayId, true).apply();
        else prefs.edit().remove(holidayId).apply();
        return favorite;
    }
}

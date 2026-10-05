package com.plumsoftware.rucalendar.data;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Включённые напоминания о праздниках: упреждение и время (ТЗ п. 4.3, 6.3). */
public final class ReminderRepository {
    private static final String PREFS = "holiday_reminders";

    public static final int DEFAULT_DAYS_BEFORE = 1;
    public static final int DEFAULT_MINUTES = 9 * 60;

    public static final class Reminder {
        public final String holidayId;
        /** 0 — в тот же день, 1 — за день, 7 — за неделю. */
        public final int daysBefore;
        /** Минуты от полуночи. */
        public final int minutes;

        public Reminder(String holidayId, int daysBefore, int minutes) {
            this.holidayId = holidayId;
            this.daysBefore = daysBefore;
            this.minutes = minutes;
        }
    }

    private final SharedPreferences prefs;

    public ReminderRepository(Context context) {
        prefs = context.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    @Nullable
    public Reminder get(String holidayId) {
        return parse(holidayId, prefs.getString(holidayId, null));
    }

    public void put(Reminder reminder) {
        prefs.edit().putString(reminder.holidayId, reminder.daysBefore + ":" + reminder.minutes).apply();
    }

    public void remove(String holidayId) {
        prefs.edit().remove(holidayId).apply();
    }

    public List<Reminder> all() {
        List<Reminder> result = new ArrayList<>();
        for (Map.Entry<String, ?> entry : prefs.getAll().entrySet()) {
            Reminder reminder = parse(entry.getKey(), String.valueOf(entry.getValue()));
            if (reminder != null) result.add(reminder);
        }
        return result;
    }

    @Nullable
    private static Reminder parse(String holidayId, @Nullable String value) {
        if (value == null) return null;
        String[] parts = value.split(":");
        if (parts.length != 2) return null;
        try {
            return new Reminder(holidayId, Integer.parseInt(parts[0]), Integer.parseInt(parts[1]));
        } catch (NumberFormatException e) {
            return null;
        }
    }
}

package com.plumsoftware.rucalendar.data;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Собственные события и недавние цвета, хранятся в SharedPreferences. */
public final class UserEventRepository {
    private static final String PREFS = "user_events";
    private static final String KEY_EVENTS = "events";
    private static final String KEY_RECENT_COLORS = "recent_colors";
    private static final int RECENT_COLORS_SIZE = 5;
    private static final String[] DEFAULT_RECENT_COLORS = {"#7A4DD8", "#D6336C", "#0E8F8F", "#B7791F", "#4A5568"};

    private static volatile UserEventRepository instance;

    private final SharedPreferences prefs;
    private List<UserEvent> events;

    public static UserEventRepository get(Context context) {
        if (instance == null) {
            synchronized (UserEventRepository.class) {
                if (instance == null) {
                    instance = new UserEventRepository(context.getApplicationContext());
                }
            }
        }
        return instance;
    }

    private UserEventRepository(Context context) {
        prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public synchronized List<UserEvent> all() {
        if (events == null) events = readEvents();
        return Collections.unmodifiableList(events);
    }

    @androidx.annotation.Nullable
    public UserEvent byId(String id) {
        for (UserEvent event : all()) {
            if (event.id.equals(id)) return event;
        }
        return null;
    }

    public List<UserEvent> on(LocalDate day) {
        List<UserEvent> result = new ArrayList<>();
        for (UserEvent event : all()) {
            if (event.occursOn(day)) result.add(event);
        }
        return result;
    }

    public synchronized void save(UserEvent event) {
        List<UserEvent> list = new ArrayList<>(all());
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).id.equals(event.id)) {
                list.remove(i);
                break;
            }
        }
        list.add(event);
        events = list;
        writeEvents(list);
        pushRecentColor(event.color);
    }

    public synchronized void delete(String id) {
        List<UserEvent> list = new ArrayList<>(all());
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).id.equals(id)) {
                list.remove(i);
                break;
            }
        }
        events = list;
        writeEvents(list);
    }

    /** Ровно пять цветов без дублей; до первого сохранения — стартовый набор (п. 6.1). */
    public List<Integer> recentColors() {
        List<Integer> result = new ArrayList<>(RECENT_COLORS_SIZE);
        String stored = prefs.getString(KEY_RECENT_COLORS, null);
        if (stored != null) {
            try {
                JSONArray array = new JSONArray(stored);
                for (int i = 0; i < array.length() && result.size() < RECENT_COLORS_SIZE; i++) {
                    result.add(array.getInt(i));
                }
            } catch (JSONException ignored) {
                result.clear();
            }
        }
        if (result.size() < RECENT_COLORS_SIZE) {
            for (String hex : DEFAULT_RECENT_COLORS) {
                int color = Color.parseColor(hex);
                if (result.size() < RECENT_COLORS_SIZE && !result.contains(color)) result.add(color);
            }
        }
        return result;
    }

    private void pushRecentColor(int color) {
        List<Integer> colors = recentColors();
        colors.remove(Integer.valueOf(color));
        colors.add(0, color);
        while (colors.size() > RECENT_COLORS_SIZE) colors.remove(colors.size() - 1);
        prefs.edit().putString(KEY_RECENT_COLORS, new JSONArray(colors).toString()).apply();
    }

    private List<UserEvent> readEvents() {
        List<UserEvent> result = new ArrayList<>();
        String stored = prefs.getString(KEY_EVENTS, null);
        if (stored == null) return result;
        try {
            JSONArray array = new JSONArray(stored);
            for (int i = 0; i < array.length(); i++) {
                JSONObject o = array.getJSONObject(i);
                result.add(new UserEvent(
                        o.getString("id"),
                        o.getString("title"),
                        o.optString("description", ""),
                        LocalDate.parse(o.getString("date")),
                        o.optBoolean("yearly", true),
                        o.getInt("color"),
                        o.optInt("reminderDaysBefore", UserEvent.REMINDER_OFF),
                        o.optInt("reminderMinutes", 9 * 60)));
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return result;
    }

    private void writeEvents(List<UserEvent> list) {
        JSONArray array = new JSONArray();
        try {
            for (UserEvent event : list) {
                array.put(new JSONObject()
                        .put("id", event.id)
                        .put("title", event.title)
                        .put("description", event.description)
                        .put("date", event.date.toString())
                        .put("yearly", event.yearly)
                        .put("color", event.color)
                        .put("reminderDaysBefore", event.reminderDaysBefore)
                        .put("reminderMinutes", event.reminderMinutes));
            }
        } catch (JSONException e) {
            throw new IllegalStateException(e);
        }
        prefs.edit().putString(KEY_EVENTS, array.toString()).apply();
    }
}

package com.plumsoftware.rucalendar.data;

import android.content.Context;

import androidx.annotation.Nullable;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** База праздников, поставляется с приложением в assets/holidays.json. */
public final class HolidayRepository {
    private static final String ASSET = "holidays.json";
    private static volatile HolidayRepository instance;

    private final List<Holiday> holidays;
    /** Год → (дата → праздники). Плавающие даты считаются для каждого года отдельно. */
    private final Map<Integer, Map<LocalDate, List<Holiday>>> byYear = new HashMap<>();

    public static HolidayRepository get(Context context) {
        if (instance == null) {
            synchronized (HolidayRepository.class) {
                if (instance == null) {
                    instance = new HolidayRepository(context.getApplicationContext());
                }
            }
        }
        return instance;
    }

    private HolidayRepository(Context context) {
        holidays = Collections.unmodifiableList(parse(readAsset(context)));
    }

    public List<Holiday> all() {
        return holidays;
    }

    @Nullable
    public Holiday byId(String id) {
        for (Holiday holiday : holidays) {
            if (holiday.id.equals(id)) return holiday;
        }
        return null;
    }

    public synchronized List<Holiday> on(LocalDate date) {
        Map<LocalDate, List<Holiday>> year = byYear.get(date.getYear());
        if (year == null) {
            year = new HashMap<>();
            for (Holiday holiday : holidays) {
                LocalDate d = holiday.dateIn(date.getYear());
                List<Holiday> list = year.get(d);
                if (list == null) {
                    list = new ArrayList<>(2);
                    year.put(d, list);
                }
                list.add(holiday);
            }
            byYear.put(date.getYear(), year);
        }
        List<Holiday> result = year.get(date);
        return result == null ? Collections.<Holiday>emptyList() : result;
    }

    /** Нерабочий официальный праздник в этот день. */
    @Nullable
    public Holiday nonWorkingOn(LocalDate date) {
        for (Holiday holiday : on(date)) {
            if (holiday.nonWorking) return holiday;
        }
        return null;
    }

    /** Ближайший праздничный нерабочий день строго после указанной даты (п. 6.3). */
    @Nullable
    public Occurrence nextNonWorking(LocalDate after) {
        Holiday best = null;
        LocalDate bestDate = null;
        for (int year = after.getYear(); year <= after.getYear() + 1; year++) {
            for (Holiday holiday : holidays) {
                if (!holiday.nonWorking) continue;
                LocalDate d = holiday.dateIn(year);
                if (d.isAfter(after) && (bestDate == null || d.isBefore(bestDate))) {
                    best = holiday;
                    bestDate = d;
                }
            }
            if (best != null) return new Occurrence(best, bestDate);
        }
        return null;
    }

    /** Праздник в конкретную дату. */
    public static final class Occurrence {
        public final Holiday holiday;
        public final LocalDate date;

        Occurrence(Holiday holiday, LocalDate date) {
            this.holiday = holiday;
            this.date = date;
        }
    }

    private static List<Holiday> parse(String json) {
        List<Holiday> result = new ArrayList<>();
        try {
            JSONArray events = new JSONObject(json).getJSONArray("events");
            for (int i = 0; i < events.length(); i++) {
                JSONObject o = events.getJSONObject(i);
                result.add(new Holiday(
                        o.getString("id"),
                        o.getInt("month"),
                        o.optInt("day", 0),
                        o.optInt("nth", 0),
                        o.optInt("weekday", 0),
                        EventType.fromKey(o.getString("type")),
                        o.getString("name"),
                        o.optString("description", ""),
                        o.optBoolean("nonWorking", false)));
            }
        } catch (JSONException e) {
            throw new IllegalStateException("Broken " + ASSET, e);
        }
        return result;
    }

    private static String readAsset(Context context) {
        try (InputStream in = context.getAssets().open(ASSET)) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buffer = new byte[8192];
            int n;
            while ((n = in.read(buffer)) > 0) out.write(buffer, 0, n);
            return new String(out.toByteArray(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("Can't read " + ASSET, e);
        }
    }
}

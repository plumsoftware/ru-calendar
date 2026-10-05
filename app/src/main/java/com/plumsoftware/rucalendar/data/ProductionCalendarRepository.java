package com.plumsoftware.rucalendar.data;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.Nullable;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.Year;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Производственный календарь (рабочие, выходные, сокращённые дни).
 * <p>
 * Данные берутся с isdayoff.ru и кэшируются по годам в файлах. Сначала всегда отдаётся кэш;
 * {@link #refresh(int)} в фоне запрашивает API с таймаутом 5 с и при успехе перезаписывает кэш
 * и оповещает слушателей. Если для года нет ни кэша, ни ответа API — выходными считаются
 * суббота, воскресенье и нерабочие праздники из базы.
 */
public final class ProductionCalendarRepository {
    private static final String TAG = "ProductionCalendar";
    private static final String URL_TEMPLATE = "https://isdayoff.ru/api/getdata?year=%d&cc=ru&pre=1";
    private static final int TIMEOUT_MS = 5000;
    private static final String CACHE_DIR = "production_calendar";

    public enum DayKind {WORKING, DAY_OFF, SHORT}

    public interface Listener {
        void onYearUpdated(int year);
    }

    private static volatile ProductionCalendarRepository instance;

    private final File cacheDir;
    private final HolidayRepository holidays;
    private final Map<Integer, String> years = new HashMap<>();
    private final Set<Integer> cacheChecked = new HashSet<>();
    private final Set<Integer> requested = new HashSet<>();
    private final List<Listener> listeners = new CopyOnWriteArrayList<>();
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    public static ProductionCalendarRepository get(Context context) {
        if (instance == null) {
            synchronized (ProductionCalendarRepository.class) {
                if (instance == null) {
                    instance = new ProductionCalendarRepository(context.getApplicationContext());
                }
            }
        }
        return instance;
    }

    private ProductionCalendarRepository(Context context) {
        cacheDir = new File(context.getFilesDir(), CACHE_DIR);
        holidays = HolidayRepository.get(context);
    }

    public void addListener(Listener listener) {
        listeners.add(listener);
    }

    public void removeListener(Listener listener) {
        listeners.remove(listener);
    }

    /** Есть ли для года данные из API (свежие или из кэша). */
    public boolean hasData(int year) {
        return yearData(year) != null;
    }

    public DayKind kind(LocalDate date) {
        String data = yearData(date.getYear());
        if (data != null) {
            switch (data.charAt(date.getDayOfYear() - 1)) {
                case '1':
                    return DayKind.DAY_OFF;
                case '2':
                    return DayKind.SHORT;
                default:
                    return DayKind.WORKING;
            }
        }
        DayOfWeek dow = date.getDayOfWeek();
        if (dow == DayOfWeek.SATURDAY || dow == DayOfWeek.SUNDAY || holidays.nonWorkingOn(date) != null) {
            return DayKind.DAY_OFF;
        }
        return DayKind.WORKING;
    }

    public boolean isDayOff(LocalDate date) {
        return kind(date) == DayKind.DAY_OFF;
    }

    /** Запросить год с сервера; повторно за сессию не запрашивается. */
    public void refresh(final int year) {
        synchronized (this) {
            if (!requested.add(year)) return;
        }
        executor.execute(() -> {
            String body = fetch(year);
            if (!isValid(body, year)) {
                Log.w(TAG, "isdayoff: no valid data for " + year + ", using cache");
                synchronized (ProductionCalendarRepository.this) {
                    requested.remove(year);
                }
                return;
            }
            writeCache(year, body);
            synchronized (ProductionCalendarRepository.this) {
                years.put(year, body);
            }
            mainHandler.post(() -> {
                for (Listener listener : listeners) listener.onYearUpdated(year);
            });
        });
    }

    @Nullable
    private synchronized String yearData(int year) {
        if (!cacheChecked.contains(year)) {
            cacheChecked.add(year);
            String cached = readCache(year);
            if (isValid(cached, year) && !years.containsKey(year)) years.put(year, cached);
        }
        return years.get(year);
    }

    @Nullable
    private static String fetch(int year) {
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) new URL(String.format(java.util.Locale.ROOT, URL_TEMPLATE, year)).openConnection();
            connection.setConnectTimeout(TIMEOUT_MS);
            connection.setReadTimeout(TIMEOUT_MS);
            if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) return null;
            return readAll(connection.getInputStream());
        } catch (IOException e) {
            Log.w(TAG, "isdayoff request failed: " + e);
            return null;
        } finally {
            if (connection != null) connection.disconnect();
        }
    }

    /** Ответ API — по символу на каждый день года: 0 рабочий, 1 выходной, 2 сокращённый, 4 рабочий (covid). */
    private static boolean isValid(@Nullable String data, int year) {
        if (data == null || data.length() != Year.of(year).length()) return false;
        for (int i = 0; i < data.length(); i++) {
            char c = data.charAt(i);
            if (c != '0' && c != '1' && c != '2' && c != '4') return false;
        }
        return true;
    }

    private File cacheFile(int year) {
        return new File(cacheDir, year + ".txt");
    }

    @Nullable
    private String readCache(int year) {
        File file = cacheFile(year);
        if (!file.exists()) return null;
        try (InputStream in = new FileInputStream(file)) {
            return readAll(in);
        } catch (IOException e) {
            return null;
        }
    }

    private void writeCache(int year, String data) {
        if (!cacheDir.exists() && !cacheDir.mkdirs()) return;
        File tmp = new File(cacheDir, year + ".tmp");
        try (FileOutputStream out = new FileOutputStream(tmp)) {
            out.write(data.getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            Log.w(TAG, "Can't write cache for " + year, e);
            return;
        }
        if (!tmp.renameTo(cacheFile(year))) tmp.delete();
    }

    private static String readAll(InputStream in) throws IOException {
        StringBuilder builder = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) builder.append(line.trim());
        }
        return builder.toString();
    }
}

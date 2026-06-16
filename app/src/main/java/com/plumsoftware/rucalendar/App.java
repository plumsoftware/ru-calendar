package com.plumsoftware.rucalendar;

import android.app.Application;
import android.content.SharedPreferences;

import androidx.appcompat.app.AppCompatDelegate;

import com.plumsoftware.rucalendar.config.AdsConfig;
import com.plumsoftware.rucalendar.config.MyBuildConfig;

import io.appmetrica.analytics.AppMetrica;
import io.appmetrica.analytics.AppMetricaConfig;

public class App extends Application {
    @Override
    public void onCreate() {
        super.onCreate();

        AdsConfig.init(BuildConfig.PLATFORM);
        SharedPreferences appSettings = getSharedPreferences("app_settings", MODE_PRIVATE);
        boolean isDarkTheme = appSettings.getBoolean("dark_theme", false);
        AppCompatDelegate.setDefaultNightMode(isDarkTheme
                ? AppCompatDelegate.MODE_NIGHT_YES
                : AppCompatDelegate.MODE_NIGHT_NO);

        // Creating an extended library configuration.
        AppMetricaConfig config = AppMetricaConfig.newConfigBuilder(MyBuildConfig.RUSTORE_APP_METRICA_API_KEY).build();
        // Initializing the AppMetrica SDK.
        AppMetrica.activate(this, config);
    }
}

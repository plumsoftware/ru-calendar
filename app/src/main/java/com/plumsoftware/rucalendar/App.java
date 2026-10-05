package com.plumsoftware.rucalendar;

import android.app.Application;

import androidx.appcompat.app.AppCompatDelegate;

import com.yandex.mobile.ads.common.YandexAds;
import com.plumsoftware.rucalendar.config.MyBuildConfig;
import com.plumsoftware.rucalendar.data.ProductionCalendarRepository;
import com.plumsoftware.rucalendar.widget.WidgetUpdater;

import io.appmetrica.analytics.AppMetrica;
import io.appmetrica.analytics.AppMetricaConfig;

public class App extends Application {
    @Override
    public void onCreate() {
        super.onCreate();

        // Автоинициализация SDK Яндекса выключена в манифесте; реклама загружается после неё
        YandexAds.initialize(this, () -> {
        });
        // Тема следует системной (ТЗ п. 5.3)
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);

        // Creating an extended library configuration.
        AppMetricaConfig config = AppMetricaConfig.newConfigBuilder(MyBuildConfig.RUSTORE_APP_METRICA_API_KEY).build();
        // Initializing the AppMetrica SDK.
        AppMetrica.activate(this, config);

        // Свежий производственный календарь — обновить виджеты
        ProductionCalendarRepository.get(this).addListener(year -> WidgetUpdater.updateAll(this));
    }
}

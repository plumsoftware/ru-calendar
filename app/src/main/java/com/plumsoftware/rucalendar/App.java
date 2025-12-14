package com.plumsoftware.rucalendar;

import android.app.Application;

import com.plumsoftware.rucalendar.config.MyBuildConfig;

import io.appmetrica.analytics.AppMetrica;
import io.appmetrica.analytics.AppMetricaConfig;

public class App extends Application {
    @Override
    public void onCreate() {
        super.onCreate();

        // Creating an extended library configuration.
        AppMetricaConfig config = AppMetricaConfig.newConfigBuilder(MyBuildConfig.RUSTORE_APP_METRICA_API_KEY).build();
        // Initializing the AppMetrica SDK.
        AppMetrica.activate(this, config);
    }
}

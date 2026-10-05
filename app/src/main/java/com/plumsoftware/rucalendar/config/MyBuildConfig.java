package com.plumsoftware.rucalendar.config;

public final class MyBuildConfig {
    public static String RUSTORE_APP_METRICA_API_KEY = "640dd7c7-f297-41c0-8337-df72654ccf8f";

    /** Значения BuildConfig.PLATFORM, задаются product flavor-ами в app/build.gradle. */
    public static final int PLATFORM_RUSTORE = 1;
    public static final int PLATFORM_HUAWEI_APP_GALLERY = 2;
    public static final int PLATFORM_GOOGLE_PLAY = 3;

    // Рекламные ID перенесены в app/build.gradle (productFlavors) и читаются через AdsConfig

    private MyBuildConfig() {
    }
}

package com.plumsoftware.rucalendar.config;

import com.plumsoftware.rucalendar.BuildConfig;

/**
 * Рекламные блоки. ID и флаги задаются в app/build.gradle: в каждом product flavor (магазине) свои ID,
 * в debug-сборке — демо-блоки Яндекса.
 */
public final class AdsConfig {
    /** Баннер на главной странице — внизу «Ленты». */
    public static final String BANNER_MAIN_SCREEN_AD = BuildConfig.AD_FEED_BANNER_ID;
    /** Баннер внизу экрана о празднике. */
    public static final String BANNER_EVENT_SCREEN_AD = BuildConfig.AD_EVENT_BANNER_ID;
    /** Реклама при открытии приложения. */
    public static final String OPEN_MAIN_SCREEN_AD = BuildConfig.AD_APP_OPEN_ID;
    /** Межстраничная реклама: при закрытии экрана праздника и после создания своего события. */
    public static final String INTERSTITIAL_AD = BuildConfig.AD_INTERSTITIAL_ID;

    /** Флаги: false — реклама этого вида даже не загружается. */
    public static final boolean SHOW_OPEN_MAIN_SCREEN_AD = BuildConfig.SHOW_APP_OPEN_AD;
    public static final boolean SHOW_MAIN_SCREEN_BANNER = BuildConfig.SHOW_FEED_BANNER_AD;
    public static final boolean SHOW_EVENT_SCREEN_BANNER = BuildConfig.SHOW_EVENT_BANNER_AD;

    // VK (myTarget) — используется только старым EventActivity
    public static final int BANNER_MAIN_SCREEN_AD_VK = BuildConfig.DEBUG ? 0 : 1919524;
    public static final int BANNER_EVENT_SCREEN_AD_VK = BuildConfig.DEBUG ? 0 : 1919527;
    public static final int INTERSTITIAL_AD_VK = BuildConfig.DEBUG ? 0 : 1919530;

    private AdsConfig() {
    }
}

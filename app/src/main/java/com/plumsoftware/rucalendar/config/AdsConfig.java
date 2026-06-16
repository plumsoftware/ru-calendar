package com.plumsoftware.rucalendar.config;

import com.plumsoftware.rucalendar.BuildConfig;

public final class AdsConfig {
    public static String BANNER_MAIN_SCREEN_AD = "";
    public static int BANNER_MAIN_SCREEN_AD_VK = BuildConfig.DEBUG ? 0 : 1919524;
    public static int BANNER_EVENT_SCREEN_AD_VK = BuildConfig.DEBUG ? 0 : 1919527;
    public static String BANNER_EVENT_SCREEN_AD = "";
    public static String OPEN_MAIN_SCREEN_AD = "";
    public static boolean SHOW_OPEN_MAIN_SCREEN_AD = false;
    public static String INTERSTITIAL_AD = "";
    public static int INTERSTITIAL_AD_VK = BuildConfig.DEBUG ? 0 : 1919530;

    private AdsConfig() {
    }

    public static void init(int platform) {
        SHOW_OPEN_MAIN_SCREEN_AD = BuildConfig.SHOW_OPEN_AD;

        if (BuildConfig.DEBUG) {
            BANNER_MAIN_SCREEN_AD = "";
            BANNER_EVENT_SCREEN_AD = "";
            OPEN_MAIN_SCREEN_AD = "";
            INTERSTITIAL_AD = "";
            return;
        }

        switch (platform) {
            case MyBuildConfig.PLATFORM_HUAWEI_APP_GALLERY:
                BANNER_MAIN_SCREEN_AD = MyBuildConfig.HUAWEI_BANNER_MAIN_SCREEN_AD;
                BANNER_EVENT_SCREEN_AD = MyBuildConfig.HUAWEI_BANNER_EVENT_SCREEN_AD;
                OPEN_MAIN_SCREEN_AD = MyBuildConfig.HUAWEI_OPEN_MAIN_SCREEN_AD;
                INTERSTITIAL_AD = MyBuildConfig.HUAWEI_INTERSTITIAL_AD;
                break;
            case MyBuildConfig.PLATFORM_GOOGLE_PLAY:
                BANNER_MAIN_SCREEN_AD = MyBuildConfig.GOOGLE_PLAY_BANNER_MAIN_SCREEN_AD;
                BANNER_EVENT_SCREEN_AD = MyBuildConfig.GOOGLE_PLAY_BANNER_EVENT_SCREEN_AD;
                OPEN_MAIN_SCREEN_AD = MyBuildConfig.GOOGLE_PLAY_OPEN_MAIN_SCREEN_AD;
                INTERSTITIAL_AD = MyBuildConfig.GOOGLE_PLAY_INTERSTITIAL_AD;
                break;
            case MyBuildConfig.PLATFORM_RUSTORE:
            default:
                BANNER_MAIN_SCREEN_AD = MyBuildConfig.RUSTORE_BANNER_MAIN_SCREEN_AD;
                BANNER_EVENT_SCREEN_AD = MyBuildConfig.RUSTORE_BANNER_EVENT_SCREEN_AD;
                OPEN_MAIN_SCREEN_AD = MyBuildConfig.RUSTORE_OPEN_MAIN_SCREEN_AD;
                INTERSTITIAL_AD = MyBuildConfig.RUSTORE_INTERSTITIAL_AD;
                break;
        }
    }
}

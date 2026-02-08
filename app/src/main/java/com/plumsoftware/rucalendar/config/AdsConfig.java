package com.plumsoftware.rucalendar.config;

import com.plumsoftware.rucalendar.BuildConfig;

public final class AdsConfig {
    public static String BANNER_MAIN_SCREEN_AD = BuildConfig.DEBUG ? "" : BuildConfig.mainScreenBannerId;
    public static int BANNER_MAIN_SCREEN_AD_VK = BuildConfig.DEBUG ? 0 : 1919524;
    public static int BANNER_EVENT_SCREEN_AD_VK = BuildConfig.DEBUG ? 0 : 1919527;
    public static String BANNER_EVENT_SCREEN_AD = BuildConfig.DEBUG ? "" : BuildConfig.eventScreenBannerId;
    public static String OPEN_MAIN_SCREEN_AD = BuildConfig.DEBUG ? "" : BuildConfig.openAdsId;
    public static boolean SHOW_OPEN_MAIN_SCREEN_AD = true;
    public static String INTERSTITIAL_AD = BuildConfig.DEBUG ? "" : BuildConfig.interstitialAdsId;
    public static int INTERSTITIAL_AD_VK = BuildConfig.DEBUG ? 0 : 1919530;
}

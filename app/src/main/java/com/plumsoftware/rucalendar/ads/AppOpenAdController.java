package com.plumsoftware.rucalendar.ads;

import android.os.SystemClock;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.Lifecycle;

import com.plumsoftware.rucalendar.config.AdsConfig;
import com.yandex.mobile.ads.appopenad.AppOpenAd;
import com.yandex.mobile.ads.appopenad.AppOpenAdEventListener;
import com.yandex.mobile.ads.appopenad.AppOpenAdLoadListener;
import com.yandex.mobile.ads.appopenad.AppOpenAdLoader;
import com.yandex.mobile.ads.common.AdError;
import com.yandex.mobile.ads.common.AdRequest;
import com.yandex.mobile.ads.common.AdRequestError;
import com.yandex.mobile.ads.common.ImpressionData;

/**
 * Реклама при открытии приложения. При выключенном флаге SHOW_APP_OPEN_AD не загружается вовсе.
 * Показывается, только если загрузилась быстро — иначе пользователь уже работает с приложением.
 */
public final class AppOpenAdController {
    private static final long MAX_WAIT_MS = 4000;

    private final AppCompatActivity activity;
    @Nullable
    private AppOpenAd ad;
    private long startedAt;

    public AppOpenAdController(AppCompatActivity activity) {
        this.activity = activity;
    }

    public void loadAndShow() {
        if (!AdsConfig.SHOW_OPEN_MAIN_SCREEN_AD || AdsConfig.OPEN_MAIN_SCREEN_AD.isEmpty()) return;
        startedAt = SystemClock.elapsedRealtime();
        new AppOpenAdLoader(activity.getApplicationContext()).loadAd(
                new AdRequest.Builder(AdsConfig.OPEN_MAIN_SCREEN_AD).build(), new AppOpenAdLoadListener() {
                    @Override
                    public void onAdLoaded(@NonNull AppOpenAd appOpenAd) {
                        ad = appOpenAd;
                        boolean inTime = SystemClock.elapsedRealtime() - startedAt <= MAX_WAIT_MS;
                        boolean resumed = activity.getLifecycle().getCurrentState().isAtLeast(Lifecycle.State.RESUMED);
                        if (inTime && resumed) show();
                        else clear();
                    }

                    @Override
                    public void onAdFailedToLoad(@NonNull AdRequestError error) {
                    }
                });
    }

    private void show() {
        if (ad == null) return;
        ad.setAdEventListener(new AppOpenAdEventListener() {
            @Override
            public void onAdShown() {
            }

            @Override
            public void onAdFailedToShow(@NonNull AdError adError) {
                clear();
            }

            @Override
            public void onAdDismissed() {
                clear();
            }

            @Override
            public void onAdClicked() {
            }

            @Override
            public void onAdImpression(@Nullable ImpressionData impressionData) {
            }
        });
        ad.show(activity);
    }

    public void clear() {
        if (ad != null) {
            ad.setAdEventListener(null);
            ad = null;
        }
    }
}

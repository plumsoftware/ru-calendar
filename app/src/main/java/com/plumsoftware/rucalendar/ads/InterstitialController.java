package com.plumsoftware.rucalendar.ads;

import android.app.Dialog;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.plumsoftware.rucalendar.R;
import com.plumsoftware.rucalendar.config.AdsConfig;
import com.yandex.mobile.ads.common.AdError;
import com.yandex.mobile.ads.common.AdRequest;
import com.yandex.mobile.ads.common.AdRequestError;
import com.yandex.mobile.ads.common.ImpressionData;
import com.yandex.mobile.ads.interstitial.InterstitialAd;
import com.yandex.mobile.ads.interstitial.InterstitialAdEventListener;
import com.yandex.mobile.ads.interstitial.InterstitialAdLoadListener;
import com.yandex.mobile.ads.interstitial.InterstitialAdLoader;

/**
 * Межстраничная реклама экрана: загружается при входе на экран ({@link #load()}), показывается
 * перед действием ({@link #showThen(Runnable)}). Если реклама ещё грузится — показывается индикатор
 * загрузки; если не загрузилась за {@link #WAIT_TIMEOUT_MS} или с ошибкой — действие выполняется без рекламы.
 */
public final class InterstitialController {
    private static final long WAIT_TIMEOUT_MS = 5000;

    private enum State {IDLE, LOADING, LOADED, FAILED}

    private final AppCompatActivity activity;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable timeout = this::finishPending;

    private State state = State.IDLE;
    @Nullable
    private InterstitialAdLoader loader;
    @Nullable
    private InterstitialAd ad;
    @Nullable
    private Runnable pending;
    @Nullable
    private Dialog progress;

    public InterstitialController(AppCompatActivity activity) {
        this.activity = activity;
    }

    private static boolean enabled() {
        return AdsConfig.INTERSTITIAL_AD != null && !AdsConfig.INTERSTITIAL_AD.isEmpty();
    }

    public void load() {
        if (!enabled() || state == State.LOADING || state == State.LOADED) return;
        state = State.LOADING;
        loader = new InterstitialAdLoader(activity);
        loader.loadAd(new AdRequest.Builder(AdsConfig.INTERSTITIAL_AD).build(), new InterstitialAdLoadListener() {
            @Override
            public void onAdLoaded(@NonNull InterstitialAd interstitialAd) {
                ad = interstitialAd;
                state = State.LOADED;
                if (pending != null) {
                    handler.removeCallbacks(timeout);
                    show();
                }
            }

            @Override
            public void onAdFailedToLoad(@NonNull AdRequestError error) {
                state = State.FAILED;
                if (pending != null) finishPending();
            }
        });
    }

    /** Показать рекламу, затем выполнить действие (обычно закрыть экран). */
    public void showThen(Runnable action) {
        if (pending != null) return;
        pending = action;
        switch (state) {
            case LOADED:
                show();
                break;
            case LOADING:
                showProgress();
                handler.postDelayed(timeout, WAIT_TIMEOUT_MS);
                break;
            default:
                finishPending();
        }
    }

    private void show() {
        InterstitialAd current = ad;
        if (current == null || activity.isFinishing() || activity.isDestroyed()) {
            finishPending();
            return;
        }
        current.setAdEventListener(new InterstitialAdEventListener() {
            @Override
            public void onAdShown() {
                dismissProgress();
            }

            @Override
            public void onAdFailedToShow(@NonNull AdError adError) {
                finishPending();
            }

            @Override
            public void onAdDismissed() {
                finishPending();
            }

            @Override
            public void onAdClicked() {
            }

            @Override
            public void onAdImpression(@Nullable ImpressionData impressionData) {
            }
        });
        current.show(activity);
    }

    private void finishPending() {
        handler.removeCallbacks(timeout);
        dismissProgress();
        releaseAd();
        Runnable action = pending;
        pending = null;
        if (action != null) action.run();
    }

    private void showProgress() {
        if (progress != null || activity.isFinishing()) return;
        progress = new AlertDialog.Builder(activity)
                .setView(LayoutInflater.from(activity).inflate(R.layout.dialog_ad_progress, null))
                .setCancelable(false)
                .create();
        progress.show();
    }

    private void dismissProgress() {
        if (progress != null) {
            if (progress.isShowing()) progress.dismiss();
            progress = null;
        }
    }

    /** Реклама одноразовая: после показа или ошибки освобождаем её. */
    private void releaseAd() {
        if (ad != null) {
            ad.setAdEventListener(null);
            ad = null;
        }
        if (state == State.LOADED) state = State.IDLE;
    }

    public void destroy() {
        handler.removeCallbacks(timeout);
        dismissProgress();
        releaseAd();
        loader = null;
        pending = null;
    }
}

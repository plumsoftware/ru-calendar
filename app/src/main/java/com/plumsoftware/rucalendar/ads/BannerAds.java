package com.plumsoftware.rucalendar.ads;

import android.app.Activity;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.yandex.mobile.ads.banner.BannerAdEventListener;
import com.yandex.mobile.ads.banner.BannerAdSize;
import com.yandex.mobile.ads.banner.BannerAdView;
import com.yandex.mobile.ads.common.AdRequest;
import com.yandex.mobile.ads.common.AdRequestError;
import com.yandex.mobile.ads.common.ImpressionData;

/** Адаптивный sticky-баннер Яндекса внизу экрана. */
public final class BannerAds {

    public interface Callback {
        /** Баннер загружен и показан; height — высота контейнера в пикселях. */
        void onShown(int height);
    }

    private BannerAds() {
    }

    /**
     * Загрузить баннер в контейнер. Контейнер скрыт, пока баннер не загрузится.
     * Вызывающий должен вызвать {@link BannerAdView#destroy()} при уничтожении экрана.
     */
    @Nullable
    public static BannerAdView load(Activity activity, FrameLayout container, @Nullable String adUnitId, Callback callback) {
        if (adUnitId == null || adUnitId.isEmpty()) return null;
        container.setVisibility(View.GONE);
        BannerAdView banner = new BannerAdView(activity);
        container.removeAllViews();
        container.addView(banner, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        DisplayMetrics metrics = activity.getResources().getDisplayMetrics();
        int widthPx = container.getWidth() > 0 ? container.getWidth() : metrics.widthPixels;
        banner.setAdSize(BannerAdSize.sticky(activity, Math.round(widthPx / metrics.density)));
        banner.setBannerAdEventListener(new BannerAdEventListener() {
            @Override
            public void onAdLoaded() {
                container.setVisibility(View.VISIBLE);
                container.post(() -> callback.onShown(container.getHeight()));
            }

            @Override
            public void onAdFailedToLoad(@NonNull AdRequestError error) {
                container.setVisibility(View.GONE);
            }

            @Override
            public void onAdClicked() {
            }

            @Override
            public void onImpression(@Nullable ImpressionData impressionData) {
            }
        });
        banner.loadAd(new AdRequest.Builder(adUnitId).build());
        return banner;
    }
}

package com.plumsoftware.rucalendar.activities;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import com.plumsoftware.rucalendar.events.CelebrationItem;
import com.plumsoftware.rucalendar.dialog.ProgressDialog;
import com.plumsoftware.rucalendar.R;
import com.yandex.mobile.ads.banner.BannerAdEventListener;
import com.yandex.mobile.ads.banner.BannerAdSize;
import com.yandex.mobile.ads.banner.BannerAdView;
import com.yandex.mobile.ads.common.AdRequest;
import com.yandex.mobile.ads.common.AdRequestConfiguration;
import com.yandex.mobile.ads.common.AdRequestError;
import com.yandex.mobile.ads.common.ImpressionData;
import com.yandex.mobile.ads.common.InitializationListener;
import com.yandex.mobile.ads.common.MobileAds;
import com.yandex.mobile.ads.interstitial.InterstitialAd;
import com.yandex.mobile.ads.interstitial.InterstitialAdEventListener;
import com.yandex.mobile.ads.interstitial.InterstitialAdLoadListener;
import com.yandex.mobile.ads.interstitial.InterstitialAdLoader;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class EventActivity extends AppCompatActivity {
    private ProgressDialog progressDialog = new ProgressDialog();
    @Nullable
    private InterstitialAd mInterstitialAd = null;
    @Nullable
    private InterstitialAdLoader mInterstitialAdLoader = null;
    private BannerAdView mBannerAdView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_event);

        MobileAds.initialize(EventActivity.this, () -> {

        });

        TextView textView = findViewById(R.id.textDescription);
        androidx.appcompat.widget.Toolbar toolbar = findViewById(R.id.toolbar2);
        mBannerAdView = (BannerAdView) findViewById(R.id.adView);

        mBannerAdView.setAdUnitId("R-M-2215793-1");
        mBannerAdView.setAdSize(BannerAdSize.inlineSize(EventActivity.this, 300, 100));

//         Создание объекта таргетирования рекламы.
        final AdRequest adRequestB = new AdRequest.Builder().build();

        mInterstitialAdLoader = new InterstitialAdLoader(EventActivity.this);

//        Setup event data
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            CelebrationItem celebrationItem = getIntent().getSerializableExtra("event", CelebrationItem.class);

            if (celebrationItem != null) {
                textView.setText(celebrationItem.getDesc());
            }
            if (celebrationItem != null) {
                toolbar.setTitle(celebrationItem.getName());
            }
            if (celebrationItem != null) {
                toolbar.setSubtitle(new SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(new Date(celebrationItem.getTimeInMillis())));
            }
        } else {
            textView.setText(getIntent().getStringExtra("desc"));
            toolbar.setTitle(getIntent().getStringExtra("name"));
            toolbar.setSubtitle(new SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(new Date(getIntent().getLongExtra("time", 1000000))));
        }

//        Clickers

        mInterstitialAdLoader.setAdLoadListener(new InterstitialAdLoadListener() {
            @Override
            public void onAdLoaded(@NonNull final InterstitialAd interstitialAd) {
                mInterstitialAd = interstitialAd;
                progressDialog.dismiss();
                mInterstitialAd.show(EventActivity.this);
                finish();
            }

            @Override
            public void onAdFailedToLoad(@NonNull final AdRequestError adRequestError) {
                progressDialog.dismiss();
                finish();
            }
        });

//         Регистрация слушателя для отслеживания событий, происходящих в баннерной рекламе.
        mBannerAdView.setBannerAdEventListener(new BannerAdEventListener() {
            @Override
            public void onAdLoaded() {

            }

            @Override
            public void onAdFailedToLoad(@NonNull AdRequestError adRequestError) {

            }

            @Override
            public void onAdClicked() {

            }

            @Override
            public void onLeftApplication() {

            }

            @Override
            public void onReturnedToApplication() {

            }

            @Override
            public void onImpression(@Nullable ImpressionData impressionData) {

            }
        });

        // Загрузка объявления.
        mBannerAdView.loadAd(adRequestB);
    }

    @Override
    public void onBackPressed() {
//        super.onBackPressed();
        // Загрузка объявления
        progressDialog.showDialog(EventActivity.this);
        if (mInterstitialAdLoader != null) {
            final AdRequestConfiguration adRequestConfiguration =
                    new AdRequestConfiguration.Builder("R-M-2215793-2").build();
            mInterstitialAdLoader.loadAd(adRequestConfiguration);
        }
    }
}
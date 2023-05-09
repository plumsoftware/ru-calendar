package com.plumsoftware.rucalendar;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.widget.TextView;

import com.yandex.mobile.ads.banner.AdSize;
import com.yandex.mobile.ads.banner.BannerAdEventListener;
import com.yandex.mobile.ads.banner.BannerAdView;
import com.yandex.mobile.ads.common.AdRequest;
import com.yandex.mobile.ads.common.AdRequestError;
import com.yandex.mobile.ads.common.ImpressionData;
import com.yandex.mobile.ads.common.InitializationListener;
import com.yandex.mobile.ads.common.MobileAds;
import com.yandex.mobile.ads.interstitial.InterstitialAd;
import com.yandex.mobile.ads.interstitial.InterstitialAdEventListener;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class EventActivity extends AppCompatActivity {
    private ProgressDialog progressDialog = new ProgressDialog();
    private InterstitialAd mInterstitialAd;
    private BannerAdView mBannerAdView;
    private AdRequest adRequest;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_event);

        MobileAds.initialize(this, new InitializationListener() {
            @Override
            public void onInitializationCompleted() {

            }
        });

        TextView textView = findViewById(R.id.textDescription);
        androidx.appcompat.widget.Toolbar toolbar = findViewById(R.id.toolbar2);
        mBannerAdView = (BannerAdView) findViewById(R.id.adView);

//        mBannerAdView.setAdUnitId("R-M-1752331-1");
        mBannerAdView.setAdUnitId("R-M-2215793-1");
        mBannerAdView.setAdSize(AdSize.flexibleSize(AdSize.FULL_SCREEN.getWidth(EventActivity.this), 50));

//         Создание объекта таргетирования рекламы.
        final AdRequest adRequestB = new AdRequest.Builder().build();

        mInterstitialAd = new InterstitialAd(EventActivity.this);
        mInterstitialAd.setAdUnitId("R-M-2215793-2");
        adRequest = new AdRequest.Builder().build();

//        Setup event data
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            CelebrationItem celebrationItem = getIntent().getSerializableExtra("event", CelebrationItem.class);

            textView.setText(celebrationItem.getDesc());
            toolbar.setTitle(celebrationItem.getName());
            toolbar.setSubtitle(new SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(new Date(celebrationItem.getTimeInMillis())));
        } else {
            textView.setText(getIntent().getStringExtra("desc"));
            toolbar.setTitle(getIntent().getStringExtra("name"));
            toolbar.setSubtitle(new SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(new Date(getIntent().getLongExtra("time", 1000000))));
        }

//        Clickers
        // Регистрация слушателя для отслеживания событий, происходящих в рекламе.
        mInterstitialAd.setInterstitialAdEventListener(new InterstitialAdEventListener() {
            @Override
            public void onAdLoaded() {
                progressDialog.dismiss();
                mInterstitialAd.show();
                finish();
                //swipeRefreshLayout.setRefreshing(false);
            }

            @Override
            public void onAdFailedToLoad(@NonNull AdRequestError adRequestError) {
                //Toast.makeText(MainActivity.this, adRequestError.getDescription().toString(), Toast.LENGTH_LONG).show();
                progressDialog.dismiss();
                finish();
                //swipeRefreshLayout.setRefreshing(false);
            }

            @Override
            public void onAdShown() {
                //MainActivity.swipeRefreshLayout.setRefreshing(false);
            }

            @Override
            public void onAdDismissed() {
                progressDialog.dismiss();
                finish();
                //MainActivity.swipeRefreshLayout.setRefreshing(false);
            }

            @Override
            public void onAdClicked() {
                progressDialog.dismiss();
                finish();
            }

            @Override
            public void onLeftApplication() {
                //MainActivity.swipeRefreshLayout.setRefreshing(false);
            }

            @Override
            public void onReturnedToApplication() {

            }

            @Override
            public void onImpression(@Nullable ImpressionData impressionData) {

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
        mInterstitialAd.loadAd(adRequest);
    }
}
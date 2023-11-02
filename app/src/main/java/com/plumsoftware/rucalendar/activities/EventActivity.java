package com.plumsoftware.rucalendar.activities;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.WindowManager;
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
import java.util.Arrays;
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

        DisplayMetrics displayMetrics = new DisplayMetrics();
        WindowManager windowManager = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
        windowManager.getDefaultDisplay().getMetrics(displayMetrics);
        int screenWidth = displayMetrics.widthPixels;

        MobileAds.initialize(EventActivity.this, () -> {

        });

        TextView textView = findViewById(R.id.textDescription);
        androidx.appcompat.widget.Toolbar toolbar = findViewById(R.id.toolbar2);
        mBannerAdView = (BannerAdView) findViewById(R.id.adView);

        mBannerAdView.setAdUnitId("R-M-1752331-1"); //Google Play
        mBannerAdView.setAdSize(BannerAdSize.inlineSize(EventActivity.this, screenWidth, 50));

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

        toolbar.setOnMenuItemClickListener(new androidx.appcompat.widget.Toolbar.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {

                switch (item.getItemId()) {
                    case R.id.bug_report:
                        String message = "Ошибка в событии " + new SimpleDateFormat("dd.MM", Locale.getDefault()).format(new Date(getIntent().getLongExtra("time", 1000000)));
                        message = message + "\n" + getIntent().getStringExtra("name");
                        message += "\n" + "Комментарий:" + "\n";
                        String subject = "Отчёт об ошибке";
                        String TO = "plumsoftwareofficial@gmail.com";

                        Intent mailIntent = new Intent(Intent.ACTION_VIEW);
                        Uri data = Uri.parse("mailto:?subject=" + subject + "&body=" + message + "&to=" + TO);
                        mailIntent.setData(data);
                        startActivity(Intent.createChooser(mailIntent, "Отправить отчёт о неточности с помощью..."));
                        return true;
                }
                return false;
            }
        });

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
                    new AdRequestConfiguration.Builder("R-M-1752331-5").build(); //Google Play
            mInterstitialAdLoader.loadAd(adRequestConfiguration);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.event_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.bug_report:

                break;
        }
        return super.onOptionsItemSelected(item);
    }
}
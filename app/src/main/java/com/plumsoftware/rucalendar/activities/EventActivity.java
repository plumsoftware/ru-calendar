package com.plumsoftware.rucalendar.activities;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.work.Data;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.Toast;

import com.plumsoftware.rucalendar.config.AdsConfig;
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
import com.yandex.mobile.ads.common.AdError;
import com.yandex.mobile.ads.interstitial.InterstitialAd;
import com.yandex.mobile.ads.interstitial.InterstitialAdEventListener;
import com.yandex.mobile.ads.interstitial.InterstitialAdLoadListener;
import com.yandex.mobile.ads.interstitial.InterstitialAdLoader;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import com.plumsoftware.rucalendar.services.MyNotificationWorker;

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

        long date;
        String name;

        MobileAds.initialize(EventActivity.this, () -> {

        });

        SharedPreferences sp = getSharedPreferences("ads_showing", Context.MODE_APPEND);
        int interstitial = sp.getInt("interstitial", 0);
        int banner = sp.getInt("banner2", 0);

        View rootView = findViewById(R.id.root_layout);
        if (Build.VERSION.SDK_INT <= 35) {
            ViewCompat.setOnApplyWindowInsetsListener(rootView, (v, windowInsets) -> {
                Insets insets1 = windowInsets.getInsets(WindowInsetsCompat.Type.statusBars());
                Insets insets2 = windowInsets.getInsets(WindowInsetsCompat.Type.navigationBars());

                v.setPadding(0,insets1.top, 0, insets2.bottom);

                return windowInsets;
            });
        }

        TextView textView = findViewById(R.id.textDescription);
        CheckBox reminde = findViewById(R.id.reminde);
        androidx.appcompat.widget.Toolbar toolbar = findViewById(R.id.toolbar2);
        mBannerAdView = (BannerAdView) findViewById(R.id.adView);

        mBannerAdView.setAdUnitId(AdsConfig.BANNER_EVENT_SCREEN_AD);
        mBannerAdView.setAdSize(BannerAdSize.inlineSize(EventActivity.this, screenWidth, 50));

//         Создание объекта таргетирования рекламы.
        final AdRequest adRequestB = new AdRequest.Builder().build();

//        Setup event data
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            CelebrationItem celebrationItem = getIntent().getSerializableExtra("event", CelebrationItem.class);

            name = celebrationItem.getName();
            date = celebrationItem.getTimeInMillis();

            if (celebrationItem != null) {
                textView.setText(celebrationItem.getDesc());
            }
            if (celebrationItem != null) {
                toolbar.setTitle(name);
            }
            if (celebrationItem != null) {
                toolbar.setSubtitle(new SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(new Date(date)));
            }
        } else {

            name = getIntent().getStringExtra("name");
            date = getIntent().getLongExtra("time", 1000000);

            textView.setText(getIntent().getStringExtra("desc"));
            toolbar.setTitle(name);
            toolbar.setSubtitle(new SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(new Date(date)));
        }

//        Clickers
        long finalDate = date;
        String finalName = name;
        toolbar.setOnMenuItemClickListener(new androidx.appcompat.widget.Toolbar.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {

                switch (item.getItemId()) {
                    case R.id.bug_report:
                        String message = "Ошибка в событии " + new SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(new Date(finalDate));
                        message = message + "\n" + finalName;
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

        //Ads
        if (interstitial >= 4) {
            mInterstitialAdLoader = new InterstitialAdLoader(EventActivity.this);
            mInterstitialAdLoader.setAdLoadListener(new InterstitialAdLoadListener() {
                @Override
                public void onAdLoaded(@NonNull final InterstitialAd interstitialAd) {
                    mInterstitialAd = interstitialAd;
                    progressDialog.dismiss();
                    showAd();
                }

                @Override
                public void onAdFailedToLoad(@NonNull final AdRequestError adRequestError) {
                    progressDialog.dismiss();
                    finish();
                }
            });
        } else {
            sp.edit().putInt("interstitial", (interstitial + 1)).apply();
        }

        // Загрузка объявления.
        if (banner >= 2) {
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
            mBannerAdView.loadAd(adRequestB);
        } else {
            sp.edit().putInt("banner2", (banner + 1)).apply();
        }

        // Получаем SharedPreferences
        SharedPreferences prefs = getSharedPreferences("WorkerPrefs", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        boolean isCheckedPref = prefs.getBoolean(name, false);
        reminde.setChecked(isCheckedPref);

        reminde.setOnCheckedChangeListener((compoundButton, isChecked) -> {

            if (isChecked) {

                Calendar calendar = Calendar.getInstance();
                calendar.setTimeInMillis(date);
                calendar.set(Calendar.HOUR_OF_DAY, 10);
                calendar.set(Calendar.MINUTE, 0);
                calendar.set(Calendar.SECOND, 0);
                calendar.set(Calendar.MILLISECOND, 0);

                long targetTime = calendar.getTimeInMillis();

                long currentTime = System.currentTimeMillis();
                long delay = targetTime - currentTime;

                // Подготавливаем данные
                Data inputData = new Data.Builder()
                        .putString("notification_title", finalName)
                        .build();

                // Создаём WorkRequest
                OneTimeWorkRequest workRequest = new OneTimeWorkRequest.Builder(MyNotificationWorker.class)
                        .setInputData(inputData)
                        .setInitialDelay(delay, TimeUnit.MILLISECONDS)
                        .build();

                // Сохраняем ID воркера
                UUID workId = workRequest.getId();
                editor.putBoolean(finalName, true);
                editor.putString(finalName + "_reminder_worker_id", workId.toString());
                editor.apply();

                // Запускаем воркер
                WorkManager.getInstance(this).enqueue(workRequest);
            } else {
                // Читаем сохранённый ID
                String workIdStr = prefs.getString(finalName + "_reminder_worker_id", null);
                if (workIdStr != null) {
                    try {
                        UUID workId = UUID.fromString(workIdStr);
                        WorkManager.getInstance(this).cancelWorkById(workId);
                    } catch (IllegalArgumentException e) {
                        Log.e("Worker", "Invalid UUID saved", e);
                    }
                }

                // Обновляем состояние
                editor.putBoolean(finalName, false);
                editor.remove(finalName + "_reminder_worker_id"); // можно удалить, можно оставить
                editor.apply();
            }
        });
    }

    @Override
    public void onBackPressed() {
//        super.onBackPressed();
        // Загрузка объявления
        if (mInterstitialAdLoader != null) {
            progressDialog.showDialog(EventActivity.this);
            final AdRequestConfiguration adRequestConfiguration =
                    new AdRequestConfiguration.Builder(AdsConfig.INTERSTITIAL_AD).build(); //RuStore
            mInterstitialAdLoader.loadAd(adRequestConfiguration);
        } else {
            finish();
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

    private void showAd() {
        if (mInterstitialAd != null) {
            mInterstitialAd.setAdEventListener(new InterstitialAdEventListener() {
                @Override
                public void onAdShown() {
                    // Called when ad is shown.
                }

                @Override
                public void onAdFailedToShow(@NonNull final AdError adError) {
                    // Called when an InterstitialAd failed to show.
                    finish();
                }

                @Override
                public void onAdDismissed() {
                    // Called when ad is dismissed.
                    // Clean resources after Ad dismissed
                    if (mInterstitialAd != null) {
                        mInterstitialAd.setAdEventListener(null);
                        mInterstitialAd = null;
                    }
                    finish();
                }

                @Override
                public void onAdClicked() {
                    // Called when a click is recorded for an ad.
                    finish();
                }

                @Override
                public void onAdImpression(@Nullable final ImpressionData impressionData) {
                    // Called when an impression is recorded for an ad.
                }
            });
            mInterstitialAd.show(this);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mInterstitialAdLoader != null) {
            mInterstitialAdLoader.setAdLoadListener(null);
            mInterstitialAdLoader = null;
        }
        destroyInterstitialAd();
    }

    private void destroyInterstitialAd() {
        if (mInterstitialAd != null) {
            mInterstitialAd.setAdEventListener(null);
            mInterstitialAd = null;
        }
    }
}
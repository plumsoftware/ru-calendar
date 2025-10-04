package com.plumsoftware.rucalendar.activities;

import androidx.annotation.AttrRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.work.Data;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowInsetsController;
import android.view.WindowManager;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.analytics.FirebaseAnalytics;
import com.my.target.ads.MyTargetView;
import com.my.target.common.MyTargetManager;
import com.my.target.common.models.IAdLoadingError;
import com.plumsoftware.rucalendar.BuildConfig;
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

    private FirebaseAnalytics mFirebaseAnalytics;

    private MyTargetView adView;
    private BannerAdView mBannerAdView;
    private com.my.target.ads.InterstitialAd adVkInt;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setupEdgeToEdge();
        setContentView(R.layout.activity_event);

        View rootView = findViewById(android.R.id.content);
        rootView.setOnApplyWindowInsetsListener((v, insets) -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
//                int bottomBarHeight = insets.getInsets(WindowInsets.Type.navigationBars()).bottom;
                v.setPadding(v.getPaddingLeft(), 0, v.getPaddingRight(), 0);
            }
            return insets;
        });

        mFirebaseAnalytics = FirebaseAnalytics.getInstance(this);

        DisplayMetrics displayMetrics = new DisplayMetrics();
        WindowManager windowManager = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
        windowManager.getDefaultDisplay().getMetrics(displayMetrics);
        int screenWidth = displayMetrics.widthPixels;

        long date;
        String name;
        String color;

        MobileAds.initialize(EventActivity.this, () -> {
        });

        SharedPreferences sp = getSharedPreferences("ads_showing", Context.MODE_APPEND);
        int interstitial = sp.getInt("interstitial", 0);
        int banner = sp.getInt("banner2", 0);

        TextView textView = findViewById(R.id.textDescription);
        TextView dateTextView = findViewById(R.id.event_date);
        TextView nameTextView = findViewById(R.id.event_name);
        ImageView back = findViewById(R.id.back);
        ImageView notif = findViewById(R.id.notif);
        adView = findViewById(R.id.view_ad_e);
        mBannerAdView = findViewById(R.id.ad_view_id);
        adView.setSlotId(AdsConfig.BANNER_EVENT_SCREEN_AD_VK);

        MyTargetManager.setDebugMode(BuildConfig.DEBUG);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(false); // отключаем стандартную стрелку
            getSupportActionBar().setDisplayShowTitleEnabled(false); // скрываем заголовок
        }

//         Создание объекта таргетирования рекламы.
        final AdRequest adRequestB = new AdRequest.Builder().build();

//        Setup event data
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            CelebrationItem celebrationItem = getIntent().getSerializableExtra("event", CelebrationItem.class);

            assert celebrationItem != null;
            name = celebrationItem.getName();
            date = celebrationItem.getTimeInMillis();
            color = celebrationItem.getColor();

            // Получаем текущий фон как RippleDrawable
            RippleDrawable rippleDrawable = (RippleDrawable) back.getBackground();

            // Получаем "основной" слой (item без ripple — это первый layer)
            Drawable layer = rippleDrawable.getDrawable(0);

            // Если это GradientDrawable (shape oval), меняем его цвет
            if (layer instanceof GradientDrawable) {
                ((GradientDrawable) layer).setColor(Integer.parseInt(color));
            }


            if (celebrationItem != null) {
                textView.setText(celebrationItem.getDesc());
            }
            if (celebrationItem != null) {
                nameTextView.setText(name);
            }
            if (celebrationItem != null) {
                dateTextView.setText(new SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(new Date(date)));
            }
        } else {

            name = getIntent().getStringExtra("name");
            date = getIntent().getLongExtra("time", 1000000);

            color = getIntent().getStringExtra("color");

            // Получаем текущий фон как RippleDrawable
            RippleDrawable rippleDrawable = (RippleDrawable) back.getBackground();

            // Получаем "основной" слой (item без ripple — это первый layer)
            Drawable layer = rippleDrawable.getDrawable(0);

            // Если это GradientDrawable (shape oval), меняем его цвет
            if (layer instanceof GradientDrawable) {
                assert color != null;
                ((GradientDrawable) layer).setColor(Integer.parseInt(color));
            }

            textView.setText(getIntent().getStringExtra("desc"));
            nameTextView.setText(name);
            dateTextView.setText(new SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(new Date(date)));
        }

//        Clickers
        String finalName = name;

        back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });

//        String message = "Ошибка в событии " + new SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(new Date(finalDate));
//        message = message + "\n" + finalName;
//        message += "\n" + "Комментарий:" + "\n";
//        String subject = "Отчёт об ошибке";
//        String TO = "plumsoftwareofficial@gmail.com";
//
//        Intent mailIntent = new Intent(Intent.ACTION_VIEW);
//        Uri data = Uri.parse("mailto:?subject=" + subject + "&body=" + message + "&to=" + TO);
//        mailIntent.setData(data);
//        startActivity(Intent.createChooser(mailIntent, "Отправить отчёт о неточности с помощью..."));


        //Ads
        if (interstitial >= 4) {
            mInterstitialAdLoader = new InterstitialAdLoader(EventActivity.this);
            mInterstitialAdLoader.setAdLoadListener(new InterstitialAdLoadListener() {
                @Override
                public void onAdLoaded(@NonNull final InterstitialAd interstitialAd) {
                    mInterstitialAd = interstitialAd;
                    progressDialog.dismiss();
                    showAd();
                    mFirebaseAnalytics.logEvent("RSY_INTERSTITIAL_LOADED", null);
                }

                @Override
                public void onAdFailedToLoad(@NonNull final AdRequestError adRequestError) {
                    showVkIntAd();
                }
            });
        } else {
            sp.edit().putInt("interstitial", (interstitial + 1)).apply();
        }

        // Загрузка объявления.
        if (banner >= 3) {
            loadRSYAds();
        } else {
            sp.edit().putInt("banner2", (banner + 1)).apply();
        }

        // Получаем SharedPreferences
        SharedPreferences prefs = getSharedPreferences("WorkerPrefs", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        final boolean[] isCheckedPref = {prefs.getBoolean(name, false)};

        RippleDrawable rippleDrawable = (RippleDrawable) notif.getBackground();
        Drawable layer = rippleDrawable.getDrawable(0);
        if (isCheckedPref[0]) {
            // Если это GradientDrawable (shape oval), меняем его цвет
            if (layer instanceof GradientDrawable) {
                assert color != null;
                ((GradientDrawable) layer).setColor(Integer.parseInt(color));
            }
        } else {
            // Если это GradientDrawable (shape oval), меняем его цвет
            if (layer instanceof GradientDrawable) {
                ((GradientDrawable) layer).setColor(ContextCompat.getColor(this, android.R.color.white));
            }
        }

        notif.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!isCheckedPref[0]) {
                    isCheckedPref[0] = true;
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
                    WorkManager.getInstance(EventActivity.this).enqueue(workRequest);

                    // Если это GradientDrawable (shape oval), меняем его цвет
                    if (layer instanceof GradientDrawable) {
                        assert color != null;
                        ((GradientDrawable) layer).setColor(Integer.parseInt(color));
                    }
                } else {
                    // Читаем сохранённый ID
                    isCheckedPref[0] = false;
                    String workIdStr = prefs.getString(finalName + "_reminder_worker_id", null);
                    if (workIdStr != null) {
                        try {
                            UUID workId = UUID.fromString(workIdStr);
                            WorkManager.getInstance(EventActivity.this).cancelWorkById(workId);
                        } catch (IllegalArgumentException e) {
                            Log.e("Worker", "Invalid UUID saved", e);
                        }
                    }

                    // Обновляем состояние
                    editor.putBoolean(finalName, false);
                    editor.remove(finalName + "_reminder_worker_id"); // можно удалить, можно оставить
                    editor.apply();

                    if (layer instanceof GradientDrawable) {
                        ((GradientDrawable) layer).setColor(ContextCompat.getColor(EventActivity.this, android.R.color.white));
                    }
                }
            }
        });
    }

    @Override
    public void onBackPressed() {
//        super.onBackPressed();
        // Загрузка объявления
        if (mInterstitialAdLoader != null) {
            showRsyIntAd();
        } else {
            showVkIntAd();
        }
    }

    public int getThemeColor(@AttrRes int attr) {
        TypedValue typedValue = new TypedValue();
        getTheme().resolveAttribute(attr, typedValue, true);
        if (typedValue.resourceId != 0) {
            return ContextCompat.getColor(this, typedValue.resourceId);
        } else {
            return typedValue.data;
        }
    }

//    @Override
//    public boolean onCreateOptionsMenu(Menu menu) {
//        getMenuInflater().inflate(R.menu.event_menu, menu);
//        return true;
//    }
//
//    @Override
//    public boolean onOptionsItemSelected(MenuItem item) {
//        switch (item.getItemId()) {
//            case R.id.bug_report:
//
//                break;
//        }
//        return super.onOptionsItemSelected(item);
//    }

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
                    showVkIntAd();
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
        } else {
            showVkIntAd();
        }
    }

    private void loadVkId() {
        adView.setAdSize(MyTargetView.AdSize.ADSIZE_320x50);
        adView.setListener(new MyTargetView.MyTargetViewListener() {
            @Override
            public void onLoad(@NonNull MyTargetView myTargetView) {
                Log.d("[myTarget]", "onLoad");
                rsyBannerShow(false);
                vkBannerShow(true);

                mFirebaseAnalytics.logEvent("VK_BANNER_LOADED", null);
            }

            @Override
            public void onNoAd(@NonNull IAdLoadingError iAdLoadingError, @NonNull MyTargetView myTargetView) {
                Log.d("[myTarget]", "onNoAd");
                loadRSYAds();
            }

            @Override
            public void onShow(@NonNull MyTargetView myTargetView) {
                Log.d("[myTarget]", "onShow");
            }

            @Override
            public void onClick(@NonNull MyTargetView myTargetView) {
                Log.d("[myTarget]", "onClick");
            }
        });
        adView.load();
    }

    private void loadRSYAds() {
        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        int screenWidth = displayMetrics.widthPixels;
        int screenHeight = displayMetrics.heightPixels;

        double screenInches = Math.sqrt(Math.pow(screenWidth / displayMetrics.xdpi, 2) +
                Math.pow(screenHeight / displayMetrics.ydpi, 2));

        int bannerHeight;
        if (screenInches >= MainActivity.TABLET_SCREEN_SIZE_THRESHOLD) {
            bannerHeight = (int) (screenHeight * 0.08);
        } else {
            bannerHeight = (int) (screenHeight * 0.036);
        }
        mBannerAdView.setAdUnitId(AdsConfig.BANNER_EVENT_SCREEN_AD);
        mBannerAdView.setAdSize(BannerAdSize.inlineSize(this, screenWidth, bannerHeight));

        final AdRequest adRequest = new AdRequest.Builder().build();

        // Регистрация слушателя для отслеживания событий, происходящих в баннерной рекламе.
        mBannerAdView.setBannerAdEventListener(new BannerAdEventListener() {
            @Override
            public void onAdLoaded() {
                rsyBannerShow(true);
                vkBannerShow(false);

                mFirebaseAnalytics.logEvent("RSY_BANNER_LOADED", null);
            }

            @Override
            public void onAdFailedToLoad(@NonNull AdRequestError adRequestError) {
                loadVkId();
            }

            @Override
            public void onAdClicked() {

            }

            @Override
            public void onLeftApplication() {
                //progressDialog.dismiss();
            }

            @Override
            public void onReturnedToApplication() {

            }

            @Override
            public void onImpression(@Nullable ImpressionData impressionData) {

            }
        });

        // Загрузка объявления.
        mBannerAdView.loadAd(adRequest);
    }

    private void rsyBannerShow(boolean isShow) {
        if (isShow) {
            mBannerAdView.setVisibility(View.VISIBLE);
        } else {
            mBannerAdView.setVisibility(View.GONE);
        }
    }

    private void vkBannerShow(boolean isShow) {
        if (isShow) {
            adView.setVisibility(View.VISIBLE);
        } else {
            adView.setVisibility(View.GONE);
        }
    }

    private void showVkIntAd() {
        adVkInt = new com.my.target.ads.InterstitialAd(AdsConfig.INTERSTITIAL_AD_VK, this);
        adVkInt.setListener(new com.my.target.ads.InterstitialAd.InterstitialAdListener() {

            @Override
            public void onLoad(@NonNull com.my.target.ads.InterstitialAd interstitialAd) {
                progressDialog.dismiss();

                mFirebaseAnalytics.logEvent("VK_INTERSTITIAL_LOADED", null);
            }

            @Override
            public void onNoAd(@NonNull IAdLoadingError iAdLoadingError, @NonNull com.my.target.ads.InterstitialAd interstitialAd) {
                progressDialog.dismiss();
                finish();
            }

            @Override
            public void onClick(@NonNull com.my.target.ads.InterstitialAd interstitialAd) {

            }

            @Override
            public void onFailedToShow(@NonNull com.my.target.ads.InterstitialAd interstitialAd) {
                progressDialog.dismiss();
                finish();
            }

            @Override
            public void onDismiss(@NonNull com.my.target.ads.InterstitialAd interstitialAd) {
                progressDialog.dismiss();
                finish();
            }

            @Override
            public void onVideoCompleted(@NonNull com.my.target.ads.InterstitialAd interstitialAd) {

            }

            @Override
            public void onDisplay(@NonNull com.my.target.ads.InterstitialAd interstitialAd) {
                progressDialog.dismiss();
            }
        });

        // Запускаем загрузку данных
        adVkInt.load();
    }

    private void showRsyIntAd() {
        progressDialog.showDialog(EventActivity.this);
        final AdRequestConfiguration adRequestConfiguration =
                new AdRequestConfiguration.Builder(AdsConfig.INTERSTITIAL_AD).build();
        assert mInterstitialAdLoader != null;
        mInterstitialAdLoader.loadAd(adRequestConfiguration);
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

    private void setupEdgeToEdge() {
        Window window = getWindow();

        // Определяем текущую тему (светлая/темная)
        int nightModeFlags = getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
        boolean isDarkTheme = nightModeFlags == Configuration.UI_MODE_NIGHT_YES;

        int systemUiVisibilityFlags = View.SYSTEM_UI_FLAG_LAYOUT_STABLE |
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN |
                View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION;

        // Делаем статус бар и нав бар прозрачными
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            // Настройка цвета иконок для Android 5-10
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (!isDarkTheme) {
                    // СВЕТЛАЯ ТЕМА - ТЕМНЫЕ ИКОНКИ
                    systemUiVisibilityFlags |= View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
                }
                // Для темной темы оставляем светлые иконки (по умолчанию)
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                if (!isDarkTheme) {
                    // СВЕТЛАЯ ТЕМА - ТЕМНЫЕ ИКОНКИ НАВИГАЦИИ
                    systemUiVisibilityFlags |= View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
                }
                // Для темной темы оставляем светлые иконки (по умолчанию)
            }

            window.getDecorView().setSystemUiVisibility(systemUiVisibilityFlags);
            window.setStatusBarColor(Color.TRANSPARENT);
            window.setNavigationBarColor(Color.TRANSPARENT);
        }

        // Для Android 10+ убираем затемнение под нав баром
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.setNavigationBarContrastEnforced(false);
        }

        // Для Android 11+ используем новый API
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.setDecorFitsSystemWindows(false);

            WindowInsetsController controller = window.getInsetsController();
            if (controller != null) {
                // Убеждаемся, что нав бар остается видимым
                controller.setSystemBarsBehavior(WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);

                // Настройка цвета иконок для Android 11+
                if (!isDarkTheme) {
                    // СВЕТЛАЯ ТЕМА - ТЕМНЫЕ ИКОНКИ
                    controller.setSystemBarsAppearance(
                            WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS |
                                    WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS,
                            WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS |
                                    WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS
                    );
                } else {
                    // ТЕМНАЯ ТЕМА - СВЕТЛЫЕ ИКОНКИ (убираем флаги светлых иконок)
                    controller.setSystemBarsAppearance(
                            0,
                            WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS |
                                    WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS
                    );
                }
            }
        }
    }
}
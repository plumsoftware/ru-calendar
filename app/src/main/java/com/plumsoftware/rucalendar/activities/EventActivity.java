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
import androidx.work.ExistingWorkPolicy;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
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
import android.view.ViewGroup;
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
import com.plumsoftware.rucalendar.services.MyNotificationWorker;
import com.yandex.mobile.ads.banner.BannerAdEventListener;
import com.yandex.mobile.ads.banner.BannerAdSize;
import com.yandex.mobile.ads.banner.BannerAdView;
import com.yandex.mobile.ads.common.AdRequest;
import com.yandex.mobile.ads.common.AdRequestError;
import com.yandex.mobile.ads.common.ImpressionData;
import com.yandex.mobile.ads.common.InitializationListener;
import com.yandex.mobile.ads.common.YandexAds;
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
import java.util.concurrent.TimeUnit;

public class EventActivity extends AppCompatActivity {
    private static final String BUG_REPORT_EMAIL = "Plumsoftware@yandex.ru";
    private static final String BUG_REPORT_SUBJECT = "Баг в приложении \"Календарь - праздники России\"";

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

        // Инициализация остается без изменений
        YandexAds.initialize(EventActivity.this, () -> {
        });

        SharedPreferences sp = getSharedPreferences("ads_showing", Context.MODE_APPEND);
        int interstitial = sp.getInt("interstitial", 0);
        int banner = sp.getInt("banner2", 0);

        TextView textView = findViewById(R.id.textDescription);
        TextView dateTextView = findViewById(R.id.event_date);
        TextView nameTextView = findViewById(R.id.event_name);
        ImageView bugReportButton = findViewById(R.id.bug_report_button);
        ImageView back = findViewById(R.id.back);
        ImageView notif = findViewById(R.id.notif);
        View bottomAdsContainer = findViewById(R.id.bottom_ads_container);
        adView = findViewById(R.id.view_ad_e);
        mBannerAdView = findViewById(R.id.ad_view_id);
        adView.setSlotId(AdsConfig.BANNER_EVENT_SCREEN_AD_VK);

        bugReportButton.post(() -> {
            ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) bugReportButton.getLayoutParams();
            params.bottomMargin = bottomAdsContainer.getHeight() + 16;
            bugReportButton.setLayoutParams(params);
        });

        MyTargetManager.setDebugMode(BuildConfig.DEBUG);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(false);
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }

        // Setup event data (поддерживает как Serializable, так и deep link extras)
        CelebrationItem celebrationItem = null;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            celebrationItem = getIntent().getSerializableExtra("event", CelebrationItem.class);
        } else {
            Object legacyEvent = getIntent().getSerializableExtra("event");
            if (legacyEvent instanceof CelebrationItem) {
                celebrationItem = (CelebrationItem) legacyEvent;
            }
        }

        if (celebrationItem != null) {
            name = celebrationItem.getName();
            date = celebrationItem.getTimeInMillis();
            color = celebrationItem.getColor();
            textView.setText(celebrationItem.getDesc());
        } else {
            name = getIntent().getStringExtra("name");
            date = getIntent().getLongExtra("time", System.currentTimeMillis());
            color = getIntent().getStringExtra("color");
            textView.setText(getIntent().getStringExtra("desc"));
        }

        if (name == null) {
            name = "";
        }
        if (color == null) {
            color = String.valueOf(ContextCompat.getColor(this, R.color.blue_container));
        }
        final String finalColor = color;
        final int eventColorInt = parseEventColor(finalColor);

        applyNavButtonColor(back, eventColorInt);

        nameTextView.setText(name);
        dateTextView.setText(new SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(new Date(date)));

        String finalName = name;
        back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });

        bugReportButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent emailIntent = new Intent(Intent.ACTION_SENDTO);
                emailIntent.setData(Uri.parse("mailto:"));
                emailIntent.putExtra(Intent.EXTRA_EMAIL, new String[]{BUG_REPORT_EMAIL});
                emailIntent.putExtra(Intent.EXTRA_SUBJECT, BUG_REPORT_SUBJECT);

                Intent chooserIntent = Intent.createChooser(emailIntent, getString(R.string.settings_choose_email));
                if (emailIntent.resolveActivity(getPackageManager()) != null) {
                    startActivity(chooserIntent);
                } else {
                    Toast.makeText(EventActivity.this, "Почтовое приложение не найдено", Toast.LENGTH_SHORT).show();
                }
            }
        });

        // Ads
        if (interstitial >= 4) {
            mInterstitialAdLoader = new InterstitialAdLoader(EventActivity.this);
            showRsyIntAd();
        } else {
            sp.edit().putInt("interstitial", (interstitial + 1)).apply();
        }

        if (banner >= 3) {
            loadRSYAds();
        } else {
            sp.edit().putInt("banner2", (banner + 1)).apply();
        }

        SharedPreferences prefs = getSharedPreferences("WorkerPrefs", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        final boolean[] isCheckedPref = {prefs.getBoolean(name, false)};
        final long[] savedTriggerTime = {prefs.getLong(name + "_trigger_time", -1L)};

        if (isCheckedPref[0]) {
            applyNavButtonColor(notif, eventColorInt);
        } else {
            applyNavButtonColor(notif, ContextCompat.getColor(this, android.R.color.white));
        }

        notif.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                assert finalName != null;
                int notificationId = finalName.hashCode();
                String uniqueWorkName = "event_notification_" + notificationId;

                if (!isCheckedPref[0]) {
                    showDateTimePicker(savedTriggerTime[0], date, selectedTimeMillis -> {
                        long delayMillis = selectedTimeMillis - System.currentTimeMillis();
                        if (delayMillis <= 0) {
                            Toast.makeText(EventActivity.this, "Невозможно установить уведомление", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        Data inputData = new Data.Builder()
                                .putString("notification_title", finalName)
                                .putInt("notification_id", notificationId)
                                .putLong("event_time", date)
                                .putString("event_name", finalName)
                                .putString("event_desc", textView.getText().toString())
                                .putString("event_color", finalColor)
                                .build();

                        OneTimeWorkRequest workRequest = new OneTimeWorkRequest.Builder(MyNotificationWorker.class)
                                .setInitialDelay(delayMillis, TimeUnit.MILLISECONDS)
                                .setInputData(inputData)
                                .addTag(uniqueWorkName)
                                .build();

                        WorkManager.getInstance(EventActivity.this)
                                .enqueueUniqueWork(uniqueWorkName, ExistingWorkPolicy.REPLACE, workRequest);

                        savedTriggerTime[0] = selectedTimeMillis;
                        isCheckedPref[0] = true;
                        editor.putBoolean(finalName, true);
                        editor.putLong(finalName + "_trigger_time", selectedTimeMillis);
                        editor.apply();

                        applyNavButtonColor(notif, eventColorInt);
                        Toast.makeText(EventActivity.this, "Уведомление включено", Toast.LENGTH_SHORT).show();
                    });

                } else {
                    WorkManager.getInstance(EventActivity.this).cancelUniqueWork(uniqueWorkName);

                    isCheckedPref[0] = false;
                    editor.putBoolean(finalName, false);
                    editor.remove(finalName + "_trigger_time");
                    editor.apply();

                    applyNavButtonColor(notif, ContextCompat.getColor(EventActivity.this, android.R.color.white));
                    Toast.makeText(EventActivity.this, "Уведомление выключено", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private interface OnDateTimeSelectedListener {
        void onSelected(long selectedTimeMillis);
    }

    private void showDateTimePicker(long currentTriggerMillis, long defaultEventDateMillis, OnDateTimeSelectedListener listener) {
        Calendar initialCalendar = Calendar.getInstance();
        if (currentTriggerMillis > 0) {
            initialCalendar.setTimeInMillis(currentTriggerMillis);
        } else {
            initialCalendar.setTimeInMillis(defaultEventDateMillis);
            initialCalendar.set(Calendar.HOUR_OF_DAY, 10);
            initialCalendar.set(Calendar.MINUTE, 0);
            initialCalendar.set(Calendar.SECOND, 0);
            initialCalendar.set(Calendar.MILLISECOND, 0);
        }

        DatePickerDialog datePickerDialog = new DatePickerDialog(
                this,
                (view, year, month, dayOfMonth) -> {
                    Calendar selectedCalendar = Calendar.getInstance();
                    selectedCalendar.set(Calendar.YEAR, year);
                    selectedCalendar.set(Calendar.MONTH, month);
                    selectedCalendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);

                    TimePickerDialog timePickerDialog = new TimePickerDialog(
                            EventActivity.this,
                            (timeView, hourOfDay, minute) -> {
                                selectedCalendar.set(Calendar.HOUR_OF_DAY, hourOfDay);
                                selectedCalendar.set(Calendar.MINUTE, minute);
                                selectedCalendar.set(Calendar.SECOND, 0);
                                selectedCalendar.set(Calendar.MILLISECOND, 0);
                                listener.onSelected(selectedCalendar.getTimeInMillis());
                            },
                            initialCalendar.get(Calendar.HOUR_OF_DAY),
                            initialCalendar.get(Calendar.MINUTE),
                            true
                    );
                    timePickerDialog.show();
                },
                initialCalendar.get(Calendar.YEAR),
                initialCalendar.get(Calendar.MONTH),
                initialCalendar.get(Calendar.DAY_OF_MONTH)
        );
        datePickerDialog.show();
    }

    @Override
    public void onBackPressed() {
        if (mInterstitialAdLoader != null) {
            progressDialog.showDialog(EventActivity.this);
            showAd();
        } else {
            finish();
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

    private void showAd() {
        if (mInterstitialAd != null) {
            mInterstitialAd.setAdEventListener(new InterstitialAdEventListener() {
                @Override
                public void onAdShown() {
                    progressDialog.dismiss();
                }

                @Override
                public void onAdFailedToShow(@NonNull final AdError adError) {
                    progressDialog.dismiss();
                    finish();
                }

                @Override
                public void onAdDismissed() {
                    progressDialog.dismiss();
                    if (mInterstitialAd != null) {
                        mInterstitialAd.setAdEventListener(null);
                        mInterstitialAd = null;
                    }
                    finish();
                }

                @Override
                public void onAdClicked() {

                    progressDialog.dismiss();
                    finish();
                }

                @Override
                public void onAdImpression(@Nullable final ImpressionData impressionData) { }
            });
            mInterstitialAd.show(this);
        } else {
            finish();
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
        mBannerAdView.setAdSize(BannerAdSize.inline(this, screenWidth, bannerHeight));

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
            public void onAdClicked() { }

            @Override
            public void onImpression(@Nullable ImpressionData impressionData) { }
        });
        mBannerAdView.loadAd(new AdRequest.Builder(AdsConfig.BANNER_EVENT_SCREEN_AD).build());
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
            }

            @Override
            public void onClick(@NonNull com.my.target.ads.InterstitialAd interstitialAd) { }

            @Override
            public void onFailedToShow(@NonNull com.my.target.ads.InterstitialAd interstitialAd) {
                progressDialog.dismiss();
            }

            @Override
            public void onDismiss(@NonNull com.my.target.ads.InterstitialAd interstitialAd) {
                progressDialog.dismiss();
                finish();
            }

            @Override
            public void onVideoCompleted(@NonNull com.my.target.ads.InterstitialAd interstitialAd) { }

            @Override
            public void onDisplay(@NonNull com.my.target.ads.InterstitialAd interstitialAd) {
                progressDialog.dismiss();
            }
        });

        adVkInt.load();
    }

    private void showRsyIntAd() {
        final AdRequest adRequest = new AdRequest.Builder(AdsConfig.INTERSTITIAL_AD).build();

        assert mInterstitialAdLoader != null;

        mInterstitialAdLoader.loadAd(adRequest, new InterstitialAdLoadListener() {
            @Override
            public void onAdLoaded(@NonNull final InterstitialAd interstitialAd) {
                mInterstitialAd = interstitialAd;
            }

            @Override
            public void onAdFailedToLoad(@NonNull final AdRequestError adRequestError) {
                // Предзагрузка не удалась — не закрываем экран и не запускаем fallback
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mInterstitialAdLoader != null) {
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

    private int parseEventColor(String colorString) {
        if (colorString == null || colorString.isEmpty()) {
            return ContextCompat.getColor(this, R.color.blue_container);
        }
        try {
            if (colorString.startsWith("#")) {
                return Color.parseColor(colorString);
            }
            return Integer.parseInt(colorString);
        } catch (IllegalArgumentException e) {
            return ContextCompat.getColor(this, R.color.blue_container);
        }
    }

    private void applyNavButtonColor(ImageView button, int color) {
        Drawable background = button.getBackground();
        if (background instanceof RippleDrawable) {
            Drawable layer = ((RippleDrawable) background).getDrawable(0);
            if (layer instanceof GradientDrawable) {
                ((GradientDrawable) layer.mutate()).setColor(color);
            }
        }
    }

    private void setupEdgeToEdge() {
        Window window = getWindow();

        int nightModeFlags = getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
        boolean isDarkTheme = nightModeFlags == Configuration.UI_MODE_NIGHT_YES;

        int systemUiVisibilityFlags = View.SYSTEM_UI_FLAG_LAYOUT_STABLE |
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN |
                View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (!isDarkTheme) {
                    systemUiVisibilityFlags |= View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
                }
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                if (!isDarkTheme) {
                    systemUiVisibilityFlags |= View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
                }
            }

            window.getDecorView().setSystemUiVisibility(systemUiVisibilityFlags);
            window.setStatusBarColor(Color.TRANSPARENT);
            window.setNavigationBarColor(Color.TRANSPARENT);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.setNavigationBarContrastEnforced(false);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.setDecorFitsSystemWindows(false);

            WindowInsetsController controller = window.getInsetsController();
            if (controller != null) {
                controller.setSystemBarsBehavior(WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);

                if (!isDarkTheme) {
                    controller.setSystemBarsAppearance(
                            WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS |
                                    WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS,
                            WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS |
                                    WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS
                    );
                } else {
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
package com.plumsoftware.rucalendar.activities;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.widget.NestedScrollView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.WindowManager;
import android.widget.ImageView;

/**
 import org.naishadhparmar.zcustomcalendar.CustomCalendar;
 import org.naishadhparmar.zcustomcalendar.Property;
 **/

import com.airbnb.lottie.LottieAnimationView;
import com.google.android.material.appbar.AppBarLayout;
import com.plumsoftware.rucalendar.adapters.CelebrationAdapter;
import com.plumsoftware.rucalendar.config.AdsConfig;
import com.plumsoftware.rucalendar.dialog.InfoDialog;
import com.plumsoftware.rucalendar.dialog.ProgressDialog;
import com.plumsoftware.rucalendar.events.CelebrationItem;
import com.plumsoftware.rucalendar.events.Celebrations;
import com.plumsoftware.rucalendar.calendardata.MyCustomCalendar;
import com.plumsoftware.rucalendar.repositories.OnDateSelectedListener;
import com.plumsoftware.rucalendar.repositories.OnNavigationButtonClickedListener;
import com.plumsoftware.rucalendar.calendardata.Property;
import com.plumsoftware.rucalendar.R;
import com.yandex.mobile.ads.appopenad.AppOpenAd;
import com.yandex.mobile.ads.appopenad.AppOpenAdEventListener;
import com.yandex.mobile.ads.appopenad.AppOpenAdLoadListener;
import com.yandex.mobile.ads.appopenad.AppOpenAdLoader;
import com.yandex.mobile.ads.banner.BannerAdEventListener;
import com.yandex.mobile.ads.banner.BannerAdSize;
import com.yandex.mobile.ads.banner.BannerAdView;
import com.yandex.mobile.ads.common.AdError;
import com.yandex.mobile.ads.common.AdRequest;
import com.yandex.mobile.ads.common.AdRequestConfiguration;
import com.yandex.mobile.ads.common.AdRequestError;
import com.yandex.mobile.ads.common.ImpressionData;
import com.yandex.mobile.ads.common.InitializationListener;
import com.yandex.mobile.ads.common.MobileAds;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class MainActivity extends AppCompatActivity implements OnNavigationButtonClickedListener {
    protected MyCustomCalendar myCustomCalendar;
    protected LottieAnimationView animationView;
    protected HashMap<Integer, Object> mapDateToDesc;
    protected Calendar calendar, extraCalendar;
    protected SwipeRefreshLayout swipeRefreshLayout;
    protected ImageView infoImageView;

    private ProgressDialog progressDialog = new ProgressDialog();
    private final double TABLET_SCREEN_SIZE_THRESHOLD = 7.0;

    protected List<Integer>
            januaryList,
            februaryList,
            marchList,
            aprilList,
            mayList,
            juneList,
            julyList,
            augustList,
            septemberList,
            octoberList,
            novemberList,
            decemberList,

    januaryListFuture,
            februaryListFuture,
            marchListFuture,
            aprilListFuture,
            mayListFuture,
            juneListFuture,
            julyListFuture,
            augustListFuture,
            septemberListFuture,
            octoberListFuture,
            novemberListFuture,
            decemberListFuture,

    januaryListPast,
            februaryListPast,
            marchListPast,
            aprilListPast,
            mayListPast,
            juneListPast,
            julyListPast,
            augustListPast,
            septemberListPast,
            octoberListPast,
            novemberListPast,
            decemberListPast;

    protected List<List<Integer>> months;
    protected List<List<Integer>> monthsFuture;
    protected List<List<Integer>> monthsPast;
    protected String countryCode = Locale.getDefault().getCountry().toLowerCase(Locale.ROOT);

    protected NestedScrollView nestedScrollView;

    protected AppBarLayout appBarLayout;
    //    protected boolean b;

    private AppOpenAd mAppOpenAd = null;

    @SuppressLint("NotifyDataSetChanged")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
//        Заглушка для темы
//        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        setContentView(R.layout.menu_layout);

        MobileAds.initialize(this, () -> {
        });

        Context context = MainActivity.this;
        Activity activity = MainActivity.this;


//        region::App open Ads
        progressDialog.showDialog(context);
        final AppOpenAdLoader appOpenAdLoader = new AppOpenAdLoader(context);
        final String AD_UNIT_ID = AdsConfig.OPEN_MAIN_SCREEN_AD;
        final AdRequestConfiguration adRequestConfiguration = new AdRequestConfiguration.Builder(AD_UNIT_ID).build();

        AppOpenAdEventListener appOpenAdEventListener = new AppOpenAdEventListener() {
            @Override
            public void onAdShown() {
                // Called when ad is shown.
            }

            @Override
            public void onAdFailedToShow(@NonNull final AdError adError) {
                // Called when ad failed to show.
            }

            @Override
            public void onAdDismissed() {
                // Called when ad is dismissed.
                // Clean resources after dismiss and preload new ad.
                clearAppOpenAd();
            }

            @Override
            public void onAdClicked() {
                // Called when a click is recorded for an ad.
            }

            @Override
            public void onAdImpression(@Nullable final ImpressionData impressionData) {
                // Called when an impression is recorded for an ad.
            }
        };

        AppOpenAdLoadListener appOpenAdLoadListener = new AppOpenAdLoadListener() {
            @Override
            public void onAdLoaded(@NonNull final AppOpenAd appOpenAd) {
                mAppOpenAd = appOpenAd;
                mAppOpenAd.setAdEventListener(appOpenAdEventListener);
                progressDialog.dismiss();
                showAppOpenAd();
            }

            @Override
            public void onAdFailedToLoad(@NonNull final AdRequestError adRequestError) {
                progressDialog.dismiss();
            }
        };

        appOpenAdLoader.setAdLoadListener(appOpenAdLoadListener);
        appOpenAdLoader.loadAd(adRequestConfiguration);

//        endregion

        myCustomCalendar = (MyCustomCalendar) activity.findViewById(R.id.custom_calendar);
        swipeRefreshLayout = (SwipeRefreshLayout) activity.findViewById(R.id.refreshLayout);
        RecyclerView recyclerView = (RecyclerView) activity.findViewById(R.id.recyclerView);
        infoImageView = activity.findViewById(R.id.infoImageView);
        animationView = (LottieAnimationView) activity.findViewById(R.id.animationView);
        nestedScrollView = (NestedScrollView) findViewById(R.id.nestedScrollView);
        appBarLayout = (AppBarLayout) findViewById(R.id.appbar);
//        TextView textView = (TextView) activity.findViewById(R.id.textView);
        HashMap<Object, Property> mapDescToProp = new HashMap<>();
        List<CelebrationItem> celebrations = new ArrayList<>();
        InfoDialog infoDialog = new InfoDialog(context);
//        b = countryCode.equals("ru");

//        if (!countryCode.equals("ru") && !countryCode.equals("kz") && !countryCode.equals("ua") && !countryCode.equals("by")) {
//            Toast.makeText(this, "Data is not available in your country. Default country is Russia.", Toast.LENGTH_LONG).show();
//        }

//        switch (countryCode) {
//            case "kz":
//                textView.setText("Календарь Казахстана");
//                break;
//            case "ua":
//                textView.setText("Календарь Украины");
//                break;
//            case "by":
//                textView.setText("Календарь РБ");
//                break;
//            case "ru":
//                textView.setText("Календарь РФ");
//                break;
//        }

        swipeRefreshLayout.setColorSchemeResources(R.color.blue_, R.color.red_, R.color.green_, R.color.orange_);

        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        int screenWidth = displayMetrics.widthPixels;
        int screenHeight = displayMetrics.heightPixels;

        double screenInches = Math.sqrt(Math.pow(screenWidth / displayMetrics.xdpi, 2) +
                Math.pow(screenHeight / displayMetrics.ydpi, 2));

        int bannerHeight;
        if (screenInches >= TABLET_SCREEN_SIZE_THRESHOLD) {
            bannerHeight = (int) (screenHeight * 0.08);
        } else {
            bannerHeight = (int) (screenHeight * 0.036);
        }
        BannerAdView mBannerAdView = (BannerAdView) findViewById(R.id.adView);
        mBannerAdView.setAdUnitId(AdsConfig.BANNER_MAIN_SCREEN_AD);
        mBannerAdView.setAdSize(BannerAdSize.inlineSize(context, screenWidth, bannerHeight));

        final AdRequest adRequest = new AdRequest.Builder().build();

        // Регистрация слушателя для отслеживания событий, происходящих в баннерной рекламе.
        mBannerAdView.setBannerAdEventListener(new BannerAdEventListener() {
            @Override
            public void onAdLoaded() {
                //progressDialog.dismiss();
            }

            @Override
            public void onAdFailedToLoad(@NonNull AdRequestError adRequestError) {
                //progressDialog.dismiss();
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


//        if (b) {
        Property propDefault = new Property();
        propDefault.layoutResource = R.layout.default_layout;
        propDefault.dateTextViewResource = R.id.textViewDate;
        mapDescToProp.put("default", propDefault);

        Property propUnavailable = new Property();
        propUnavailable.layoutResource = R.layout.unavailable_layout;
        propUnavailable.dateTextViewResource = R.id.textViewDate;
        mapDescToProp.put("disabled", propUnavailable);

        Property propHoliday = new Property();
        propHoliday.layoutResource = R.layout.holiday_layout;
        propHoliday.dateTextViewResource = R.id.textViewDate;
        mapDescToProp.put("holiday", propHoliday);

        Property propShort = new Property();
        propShort.layoutResource = R.layout.short_layout;
        propShort.dateTextViewResource = R.id.textViewDate;
        mapDescToProp.put("short", propShort);

        Property propCurrent = new Property();
        propCurrent.layoutResource = R.layout.current_layout;
        propCurrent.dateTextViewResource = R.id.textViewDate;
        mapDescToProp.put("current", propCurrent);

        Property propMDate = new Property();
        propMDate.layoutResource = R.layout.memory_date_view;
        propMDate.dateTextViewResource = R.id.textViewDate;
        mapDescToProp.put("mDate", propMDate);

        Property propProf = new Property();
        propProf.layoutResource = R.layout.prof_view;
        propProf.dateTextViewResource = R.id.textViewDate;
        mapDescToProp.put("prof", propProf);

        Property propNOH = new Property();
        propNOH.layoutResource = R.layout.not_off_holiday_view;
        propNOH.dateTextViewResource = R.id.textViewDate;
        mapDescToProp.put("not official holiday", propNOH);

        myCustomCalendar.setMapDescToProp(mapDescToProp);

        mapDateToDesc = new HashMap<>();
        calendar = Calendar.getInstance();
        extraCalendar = Calendar.getInstance();

        for (int i = 0; i < calendar.getActualMaximum(Calendar.DAY_OF_MONTH); i++) {
            int day = i + 1;

            mapDateToDesc.put(day, "default");
        }

        int month = calendar.get(Calendar.MONTH) + 1;

        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                String extraLink = new Link().buildLink(calendar.get(Calendar.YEAR), month, countryCode, 1, 0, 0);
                new ExtraData(calendar.get(Calendar.MONTH), calendar.get(Calendar.YEAR)).execute(extraLink);
            }
        });

        String extraLink = new Link().buildLink(calendar.get(Calendar.YEAR), month, countryCode, 1, 0, 0);
        new ExtraData(calendar.get(Calendar.MONTH), calendar.get(Calendar.YEAR)).execute(extraLink);

        myCustomCalendar.setDate(calendar, mapDateToDesc);

//        if (b) {
        Celebrations celebrationsClass = new Celebrations(calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH));
        String name1 = "";
        String descS1 = "";
        String color = "";
        long timeInMillis = calendar.getTimeInMillis();

        try {
            String[] split = celebrationsClass.getDescription().split("~del");
            for (String s : split) {
                name1 = s.split("~")[0];
                descS1 = s.split("~")[1];
                color = "#F57F17";
                celebrations.add(new CelebrationItem(name1, descS1, color, timeInMillis));
            }
        } catch (IndexOutOfBoundsException e) {
            e.printStackTrace();
        }

//        }

        CelebrationAdapter celebrationAdapter = new CelebrationAdapter(this, MainActivity.this, celebrations);
        celebrationAdapter.notifyDataSetChanged();
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setHasFixedSize(true);
        recyclerView.setAdapter(celebrationAdapter);

        myCustomCalendar.setOnNavigationButtonClickedListener(MyCustomCalendar.NEXT, this);
        myCustomCalendar.setOnNavigationButtonClickedListener(MyCustomCalendar.PREVIOUS, this);
        myCustomCalendar.setOnDateSelectedListener(new OnDateSelectedListener() {
            @SuppressLint("ResourceType")
            @Override
            public void onDateSelected(View view, Calendar selectedDate, Object desc) {
                celebrations.clear();
                String name1 = "";
                String descS1 = "";
                String color = "";
                long timeInMillis = selectedDate.getTimeInMillis();

                Celebrations celebrationsClass = new Celebrations(selectedDate.get(Calendar.MONTH), selectedDate.get(Calendar.DAY_OF_MONTH));

                try {
                    celebrationsClass.getDescription();
                    appBarLayout.setExpanded(false, true);
                } catch (IndexOutOfBoundsException e) {
                    e.printStackTrace();
                }


                try {
                    String[] split = celebrationsClass.getDescription().split("~del");
                    animationView.setVisibility(View.INVISIBLE);
                    recyclerView.setVisibility(View.VISIBLE);
                    for (String s : split) {
                        name1 = s.split("~")[0];
                        descS1 = s.split("~")[1];
//                            color = "#F57F17";
//                            celebrations.add(new CelebrationItem(name1, descS1, color, timeInMillis));

//                            Проверяем вторые события
                        if (name1.equals("День российской науки")) {
//                            if (b) {
                            color = "#ffdcc1";
                            celebrations.add(new CelebrationItem(name1, descS1, color, timeInMillis));
//                            }
                        } else if (name1.equals("День юриста")) {
                            color = "#D8D7F8";
                            celebrations.add(new CelebrationItem(name1, descS1, color, timeInMillis));
                        } else {
                            if ("holiday".equals(desc) && !name1.isEmpty() && !descS1.isEmpty()) {
                                celebrations.add(new CelebrationItem(name1, descS1, "#ffdad5", timeInMillis));
                            }
                            if ("holiday".equals(desc) && name1.isEmpty() && descS1.isEmpty()) {
                                celebrations.add(new CelebrationItem("Выходной", "Отличный повод встретиться с друзьями!", "#ffdad5", timeInMillis));
                            }
                            if ("short".equals(desc)) {
                                celebrations.add(new CelebrationItem("Сокращённый рабочий день", "Этот день предпразднечный.", "#ecddf7", timeInMillis));
                            }
                            if ("current".equals(desc) && !name1.isEmpty() && !descS1.isEmpty()) {
//                                    color = String.valueOf(getAttrColor(context, com.google.android.material.R.attr.colorTertiary));
//                                    color = String.valueOf(ContextCompat.getColor(context, com.google.android.material.R.attr.colorTertiary));
                                if (AppCompatDelegate.getDefaultNightMode() == 1) {
                                    color = "#715573";
                                } else {
                                    color = "#DEBCDF";
                                }
                                celebrations.add(new CelebrationItem(name1, descS1, color, timeInMillis));
                            }
                            if ("current".equals(desc) && name1.isEmpty() && descS1.isEmpty()) {
                                recyclerView.setVisibility(View.VISIBLE);
                            }
                            if ("mDate".equals(desc)) {
                                color = "#d7e8cd";
                                celebrations.add(new CelebrationItem(name1, descS1, color, timeInMillis));
                            }
                            if ("prof".equals(desc)) {
                                color = "#D8D7F8";
                                celebrations.add(new CelebrationItem(name1, descS1, color, timeInMillis));
                            }
                            if ("not official holiday".equals(desc)) {
                                color = "#ffdcc1";
                                celebrations.add(new CelebrationItem(name1, descS1, color, timeInMillis));
                            }
                        }
                    }
                } catch (IndexOutOfBoundsException e) {
                    e.printStackTrace();
                    recyclerView.setVisibility(View.INVISIBLE);
                    animationView.setVisibility(View.VISIBLE);
                    if ("holiday".equals(desc)) {
                        celebrations.add(new CelebrationItem("Выходной", "Отличный повод встретиться с друзьями!", "#ffdad5", timeInMillis));
                        animationView.setVisibility(View.INVISIBLE);
                    }
                }

//                RecyclerView recyclerViewBottomSheetDialog = (RecyclerView) view.findViewById(R.id.recyclerView);
                celebrationAdapter.notifyDataSetChanged();
                recyclerView.setLayoutManager(new LinearLayoutManager(MainActivity.this, LinearLayoutManager.VERTICAL, false));
                recyclerView.setHasFixedSize(true);
                recyclerView.setAdapter(celebrationAdapter);

                if ("default".equals(desc)) {
                    recyclerView.setVisibility(View.VISIBLE);
                    animationView.setVisibility(View.VISIBLE);
                }
                if ("unavailable".equals(desc)) {
                    recyclerView.setVisibility(View.VISIBLE);
                } else {
                    recyclerView.setVisibility(View.VISIBLE);
                }
            }
        });

        String extraYearLink = new Link().buildYearLink(extraCalendar.get(Calendar.YEAR), countryCode, 1, 0, 0);
        new ExtraDataCalendarClick(extraCalendar.get(Calendar.YEAR)).execute(extraYearLink);

        String extraYearLinkFuture = new Link().buildYearLink(extraCalendar.get(Calendar.YEAR) + 1, countryCode, 1, 0, 0);
        new ExtraDataCalendarClickFuture(extraCalendar.get(Calendar.YEAR) + 1).execute(extraYearLinkFuture);

        String extraYearLinkPast = new Link().buildYearLink(extraCalendar.get(Calendar.YEAR) - 1, countryCode, 1, 0, 0);
        new ExtraDataCalendarClickPast(extraCalendar.get(Calendar.YEAR) - 1).execute(extraYearLinkPast);
//        } else {
//            Property propDefault = new Property();
//            propDefault.layoutResource = R.layout.default_layout;
//            propDefault.dateTextViewResource = R.id.textViewDate;
//            mapDescToProp.put("default", propDefault);
//
//            Property propUnavailable = new Property();
//            propUnavailable.layoutResource = R.layout.unavailable_layout;
//            propUnavailable.dateTextViewResource = R.id.textViewDate;
//            mapDescToProp.put("disabled", propUnavailable);
//
//            Property propHoliday = new Property();
//            propHoliday.layoutResource = R.layout.holiday_layout;
//            propHoliday.dateTextViewResource = R.id.textViewDate;
//            mapDescToProp.put("holiday", propHoliday);
//
//            Property propShort = new Property();
//            propShort.layoutResource = R.layout.short_layout;
//            propShort.dateTextViewResource = R.id.textViewDate;
//            mapDescToProp.put("short", propShort);
//
//            Property propCurrent = new Property();
//            propCurrent.layoutResource = R.layout.current_layout;
//            propCurrent.dateTextViewResource = R.id.textViewDate;
//            mapDescToProp.put("current", propCurrent);
//
//            myCustomCalendar.setMapDescToProp(mapDescToProp);
//
//            mapDateToDesc = new HashMap<>();
//            calendar = Calendar.getInstance();
//            extraCalendar = Calendar.getInstance();
//
//            for (int i = 0; i < calendar.getActualMaximum(Calendar.DAY_OF_MONTH); i++) {
//                int day = i + 1;
//
//                mapDateToDesc.put(day, "default");
//            }
//
//            int month = calendar.get(Calendar.MONTH) + 1;
//
//            swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
//                @Override
//                public void onRefresh() {
//                    String extraLink = new Link().buildLink(calendar.get(Calendar.YEAR), month, countryCode, 1, 0, 0);
//                    new ExtraData(calendar.get(Calendar.MONTH), calendar.get(Calendar.YEAR)).execute(extraLink);
//                }
//            });
//
//            String extraLink = new Link().buildLink(calendar.get(Calendar.YEAR), month, countryCode, 1, 0, 0);
//            new ExtraData(calendar.get(Calendar.MONTH), calendar.get(Calendar.YEAR)).execute(extraLink);
//
//            myCustomCalendar.setDate(calendar, mapDateToDesc);
//
//            if (b) {
//                Celebrations celebrationsClass = new Celebrations(calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH));
//                String name = "";
//                String descS = "";
//                String color = "";
//                long timeInMillis = calendar.getTimeInMillis();
//
//                try {
//                    String[] split = celebrationsClass.getDescription().split("~del");
//                    for (String s : split) {
//                        name = s.split("~")[0];
//                        descS = s.split("~")[1];
//                        color = "#F57F17";
//                        celebrations.add(new CelebrationItem(name, descS, color, timeInMillis));
//                    }
//                } catch (IndexOutOfBoundsException e) {
//                    e.printStackTrace();
//                    //recyclerView.setVisibility(View.GONE);
//                }
//                celebrations.add(new CelebrationItem(name, descS, color, timeInMillis));
//            }
//
//            CelebrationAdapter celebrationAdapter = new CelebrationAdapter(this, MainActivity.this, celebrations);
//            celebrationAdapter.notifyDataSetChanged();
//            recyclerView.setLayoutManager(new LinearLayoutManager(this));
//            recyclerView.setHasFixedSize(true);
//            recyclerView.setAdapter(celebrationAdapter);
//
//            myCustomCalendar.setOnNavigationButtonClickedListener(MyCustomCalendar.NEXT, this);
//            myCustomCalendar.setOnNavigationButtonClickedListener(MyCustomCalendar.PREVIOUS, this);
//            myCustomCalendar.setOnDateSelectedListener(new OnDateSelectedListener() {
//                @Override
//                public void onDateSelected(View view, Calendar selectedDate, Object desc) {
//                    celebrations.clear();
//                    String name = "";
//                    String descS = "";
//                    String color = "";
//                    long timeInMillis = selectedDate.getTimeInMillis();
//
//                    if ("holiday".equals(desc)) {
//                        celebrations.add(new CelebrationItem("Выходной", "Отличный повод встретиться с друзьями!", "#ffdad5", timeInMillis));
//                    } else if ("short".equals(desc)) {
//                        celebrations.add(new CelebrationItem("Сокращённый рабочий день", "Этот день предпразднечный.", "#ecddf7", timeInMillis));
//                    } else if ("current".equals(desc)) {
//                        recyclerView.setVisibility(View.VISIBLE);
//                    }
//
//                    celebrationAdapter.notifyDataSetChanged();
//                    recyclerView.setLayoutManager(new LinearLayoutManager(MainActivity.this));
//                    recyclerView.setHasFixedSize(true);
//                    recyclerView.setAdapter(celebrationAdapter);
//
//                    if ("default".equals(desc)) {
//                        recyclerView.setVisibility(View.VISIBLE);
//                    } else if ("unavailable".equals(desc)) {
//                        recyclerView.setVisibility(View.VISIBLE);
//                    } else {
//                        recyclerView.setVisibility(View.VISIBLE);
//                    }
//                }
//            });
//
//            String extraYearLink = new Link().buildYearLink(extraCalendar.get(Calendar.YEAR), countryCode, 1, 0, 0);
//            new ExtraDataCalendarClick(extraCalendar.get(Calendar.YEAR)).execute(extraYearLink);
//
//            String extraYearLinkFuture = new Link().buildYearLink(extraCalendar.get(Calendar.YEAR) + 1, countryCode, 1, 0, 0);
//            new ExtraDataCalendarClickFuture(extraCalendar.get(Calendar.YEAR) + 1).execute(extraYearLinkFuture);
//
//            String extraYearLinkPast = new Link().buildYearLink(extraCalendar.get(Calendar.YEAR) - 1, countryCode, 1, 0, 0);
//            new ExtraDataCalendarClickPast(extraCalendar.get(Calendar.YEAR) - 1).execute(extraYearLinkPast);
//        }

        infoImageView.setOnClickListener(view -> {
            infoDialog.show();
        });
    }

    @Override
    public Map<Integer, Object>[] onNavigationButtonClicked(int whichButton, Calendar newMonth) {
        Map<Integer, Object>[] arr = new Map[2];
        List<Integer> integers;

//        count++;

//        mInterstitialAd = new InterstitialAd(MainActivity.this);
//        mInterstitialAd.setAdUnitId("R-M-1752331-2");
//        mInterstitialAd.setAdUnitId("R-M-2215793-2"); this one

//        if (count % 3 == 0 && count != 0) {
//            ProgressDialog progressDialog = new ProgressDialog();
//            //AlertDialog dialog = showDialog();
//            // Создание объекта таргетирования рекламы.
//            progressDialog.showDialog(MainActivity.this);
//            final AdRequest adRequest = new AdRequest.Builder().build();
//            //swipeRefreshLayout.setRefreshing(true);
//
//            // Регистрация слушателя для отслеживания событий, происходящих в рекламе.
//            mInterstitialAd.setInterstitialAdEventListener(new InterstitialAdEventListener() {
//                @Override
//                public void onAdLoaded() {
//                    progressDialog.dismiss();
//                    mInterstitialAd.show();
//                    //swipeRefreshLayout.setRefreshing(false);
//                }
//
//                @Override
//                public void onAdFailedToLoad(@NonNull AdRequestError adRequestError) {
//                    //Toast.makeText(MainActivity.this, adRequestError.getDescription().toString(), Toast.LENGTH_LONG).show();
//                    progressDialog.dismiss();
//                    //swipeRefreshLayout.setRefreshing(false);
//                }
//
//                @Override
//                public void onAdShown() {
//                    //MainActivity.swipeRefreshLayout.setRefreshing(false);
//                }
//
//                @Override
//                public void onAdDismissed() {
//                    progressDialog.dismiss();
//                    //MainActivity.swipeRefreshLayout.setRefreshing(false);
//                }
//
//                @Override
//                public void onAdClicked() {
//                    progressDialog.dismiss();
//                }
//
//                @Override
//                public void onLeftApplication() {
//                    //MainActivity.swipeRefreshLayout.setRefreshing(false);
//                }
//
//                @Override
//                public void onReturnedToApplication() {
//
//                }
//
//                @Override
//                public void onImpression(@Nullable ImpressionData impressionData) {
//
//                }
//            });
//
//            // Загрузка объявления.
//            mInterstitialAd.loadAd(adRequest);
//        }

        if (newMonth.get(Calendar.YEAR) == calendar.get(Calendar.YEAR)) {
            arr[0] = new HashMap<>();
            integers = months.get(newMonth.get(Calendar.MONTH));

            for (int i = 0; i < integers.size(); i++) {
                int index = integers.get(i);

                int date = i + 1;

                switch (index) {
                    case 1:
                        arr[0].put(date, "holiday");
                        break;
                    case 2:
                        arr[0].put(date, "short");
                        break;
                    case 4:
                        arr[0].put(date, "covid");
                        break;
                    default:
                        arr[0].put(date, "default");
                        break;
                }
            }
            if (newMonth.get(Calendar.MONTH) == calendar.get(Calendar.MONTH) && newMonth.get(Calendar.YEAR) == calendar.get(Calendar.YEAR)) {
                arr[0].put(calendar.get(Calendar.DAY_OF_MONTH), "current");
            }
            arr[1] = null;
        }
        if (newMonth.get(Calendar.YEAR) == calendar.get(Calendar.YEAR) + 1) {
            arr[0] = new HashMap<>();
            integers = monthsFuture.get(newMonth.get(Calendar.MONTH));

            for (int i = 0; i < integers.size(); i++) {
                int index = integers.get(i);

                int date = i + 1;

                switch (index) {
                    case 1:
                        arr[0].put(date, "holiday");
                        break;
                    case 2:
                        arr[0].put(date, "short");
                        break;
                    case 4:
                        arr[0].put(date, "covid");
                        break;
                    default:
                        arr[0].put(date, "default");
                        break;
                }
            }
            if (newMonth.get(Calendar.MONTH) == calendar.get(Calendar.MONTH) && newMonth.get(Calendar.YEAR) == calendar.get(Calendar.YEAR)) {
                arr[0].put(calendar.get(Calendar.DAY_OF_MONTH), "current");
            }
            arr[1] = null;
        }
        if (newMonth.get(Calendar.YEAR) == calendar.get(Calendar.YEAR) - 1) {
            arr[0] = new HashMap<>();
            integers = monthsPast.get(newMonth.get(Calendar.MONTH));

            for (int i = 0; i < integers.size(); i++) {
                int index = integers.get(i);

                int date = i + 1;

                switch (index) {
                    case 1:
                        arr[0].put(date, "holiday");
                        break;
                    case 2:
                        arr[0].put(date, "short");
                        break;
                    case 4:
                        arr[0].put(date, "covid");
                        break;
                    default:
                        arr[0].put(date, "default");
                        break;
                }
            }
            if (newMonth.get(Calendar.MONTH) == calendar.get(Calendar.MONTH) && newMonth.get(Calendar.YEAR) == calendar.get(Calendar.YEAR)) {
                arr[0].put(calendar.get(Calendar.DAY_OF_MONTH), "current");
            }
            arr[1] = null;
        }

        try {
//            if (b) {
            switch (newMonth.get(Calendar.MONTH)) {
                case Calendar.JANUARY:
                    arr[0].put(1, "holiday");
                    arr[0].put(2, "holiday");
                    arr[0].put(3, "holiday");
                    arr[0].put(4, "holiday");
                    arr[0].put(5, "holiday");
                    arr[0].put(6, "holiday");
                    arr[0].put(7, "holiday");
                    arr[0].put(8, "holiday");
                    arr[0].put(12, "prof");//День работника прокураторы
                    arr[0].put(13, "mDate");//День печати
                    arr[0].put(14, "not official holiday");//Старый Новый год
                    arr[0].put(21, "mDate");//День памяти инженерных войск
                    arr[0].put(27, "mDate");//День снятия блокады города Ленинград
                    arr[0].put(25, "not official holiday");//Татьянин день(День студента)
                    arr[1] = null;
                    break;
                case Calendar.FEBRUARY:
                    arr[0].put(8, "prof");//День стоматолога
                    arr[0].put(9, "prof");//День работника гражданской авиации
                    arr[0].put(10, "prof");//День дипломатического работника
                    arr[0].put(14, "not official holiday");//День святого Валентина
                    arr[0].put(15, "mDate");//День памяти воинов-интернационалистов
                    arr[0].put(23, "holiday");
                    arr[0].put(27, "prof");//День Сил специальных операций
                    arr[1] = null;
                    break;
                case Calendar.MARCH:
                    arr[0].put(8, "holiday");
                    arr[0].put(9, "prof");
                    arr[0].put(11, "prof");//День работников органов наркоконтроля
                    arr[0].put(12, "prof");//День работников уголовно-исполнительной системы
                    arr[0].put(14, "prof");//День работника геодезиста
                    arr[0].put(18, "not official holiday"); //День воссоединения Крыма с Россией
                    arr[0].put(19, "prof");//День моряка-подводника
                    arr[0].put(27, "prof");//День нац гвардии России
                    arr[0].put(29, "prof");//День специаличста юридической службы
                    arr[1] = null;
                    break;
                case Calendar.APRIL:
                    arr[0].put(1, "not official holiday");
                    arr[0].put(2, "not official holiday");//День единения народов
                    arr[0].put(4, "prof");//День геолога
                    arr[0].put(8, "prof");//День сотрудников военных коммиссариатов
                    arr[0].put(12, "mDate");//День космонавтики
                    arr[0].put(26, "mDate");//День памяти погибших в радиационных авариях и катострофах
                    arr[0].put(27, "mDate");//День российского парламентаризма
                    arr[0].put(28, "prof");//День работника скорой медицинской помощи
                    arr[0].put(30, "prof");//День пожарной охраны
                    arr[1] = null;
                    break;
                case Calendar.MAY:
                    arr[0].put(1, "holiday");
                    arr[0].put(7, "prof");//День работников связи
                    arr[0].put(9, "holiday");
                    arr[0].put(20, "prof");//Всемирный день метрологии
                    arr[0].put(21, "prof");//День полярника
                    arr[0].put(24, "prof");//День кадровика
                    arr[0].put(25, "prof");//День филолога
                    arr[0].put(26, "prof");//День российского предпринимательства
                    arr[0].put(27, "prof");//Общероссийский день библиотек
                    arr[0].put(28, "prof");//День пограничника
                    arr[0].put(29, "prof");//День Химика
                    arr[0].put(31, "prof");//День российской адвокатуры
                    arr[1] = null;
                    break;
                case Calendar.JUNE:
                    arr[0].put(1, "not official holiday");
                    arr[0].put(2, "mDate");
                    arr[0].put(5, "prof");
                    arr[0].put(6, "not official holiday");
                    arr[0].put(8, "prof");
                    arr[0].put(12, "holiday");
                    arr[0].put(14, "prof");
                    arr[0].put(20, "prof");
                    arr[0].put(22, "mDate");
                    arr[0].put(26, "prof");
                    arr[0].put(27, "not official holiday");
                    arr[0].put(30, "prof");
                    arr[1] = null;
                    break;
                case Calendar.JULY:
                    arr[0].put(3, "prof");
                    arr[0].put(4, "prof");
                    arr[0].put(8, "not official holiday");
                    arr[0].put(11, "prof");
                    arr[0].put(17, "prof");
                    arr[0].put(18, "prof");
                    arr[0].put(25, "prof");
                    arr[0].put(28, "mDate");
                    arr[0].put(30, "prof");
                    arr[1] = null;
                    break;
                case Calendar.AUGUST:
                    arr[0].put(2, "mDate");
                    arr[0].put(6, "prof");
                    arr[0].put(8, "prof");
                    arr[0].put(12, "mDate");
                    arr[0].put(15, "prof");
                    arr[0].put(18, "prof");
                    arr[0].put(22, "not official holiday");
                    arr[0].put(27, "prof");
                    arr[0].put(29, "prof");
                    arr[0].put(31, "prof");
                    arr[1] = null;
                    break;
                case Calendar.SEPTEMBER:
                    arr[0].put(1, "not official holiday");
                    arr[0].put(3, "mDate");
                    arr[0].put(4, "mDate");
                    arr[0].put(5, "prof");
                    arr[0].put(8, "prof");
                    arr[0].put(9, "prof");
                    arr[0].put(12, "prof");
                    arr[0].put(13, "prof");
                    arr[0].put(19, "prof");
                    arr[0].put(24, "prof");
                    arr[0].put(26, "prof");
                    arr[0].put(27, "not official holiday");
                    arr[0].put(28, "prof");
                    arr[0].put(30, "prof");
                    arr[1] = null;
                    break;
                case Calendar.OCTOBER:
                    arr[0].put(1, "not official holiday");
                    arr[0].put(4, "mDate");
                    arr[0].put(5, "prof");
                    arr[0].put(6, "prof");
                    arr[0].put(16, "not official holiday");//День отца
                    arr[0].put(20, "prof");
                    arr[0].put(23, "prof");
                    arr[0].put(24, "mDate");
                    arr[0].put(25, "prof");
                    arr[0].put(29, "prof");
                    arr[0].put(30, "mDate");
                    arr[0].put(31, "prof");
                    arr[1] = null;
                    break;
                case Calendar.NOVEMBER:
                    arr[0].put(1, "prof");
                    arr[0].put(4, "holiday");
                    arr[0].put(5, "prof");
                    arr[0].put(7, "mDate");
                    arr[0].put(9, "prof");
                    arr[0].put(10, "prof");
                    arr[0].put(11, "prof");
                    arr[0].put(13, "mDate");
                    arr[0].put(14, "prof");
                    arr[0].put(21, "prof");
                    arr[0].put(22, "prof");
                    arr[0].put(27, "not official holiday"); //День матери
                    arr[0].put(30, "not official holiday");
                    arr[1] = null;
                    break;
                case Calendar.DECEMBER:
                    arr[0].put(3, "mDate");
                    arr[0].put(5, "prof");
                    arr[0].put(9, "mDate");
                    arr[0].put(12, "mDate");
                    arr[0].put(18, "prof");
                    arr[0].put(20, "prof");
                    arr[0].put(22, "prof");
                    arr[0].put(27, "prof");
                    arr[1] = null;
                    break;
                default:
                    throw new IllegalStateException("Unexpected value: " + newMonth.get(Calendar.MONTH));
            }
//            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (newMonth.get(Calendar.MONTH) == calendar.get(Calendar.MONTH) && newMonth.get(Calendar.YEAR) == calendar.get(Calendar.YEAR)) {
            arr[0].put(calendar.get(Calendar.DAY_OF_MONTH), "current");
        }

        return arr;
    }

    @SuppressLint("StaticFieldLeak")
    private class ExtraData extends AsyncTask<String, String, String> {
        private int month = 0;
        private int year = 0;

        public ExtraData(int month, int year) {
            this.month = month;
            this.year = year;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            myCustomCalendar.setVisibility(View.INVISIBLE);
            swipeRefreshLayout.setRefreshing(true);
        }

        @Override
        protected String doInBackground(String... strings) {
            HttpURLConnection httpURLConnection = null;
            BufferedReader bufferedReader = null;

            try {
                URL url = new URL(strings[0]);
                httpURLConnection = (HttpURLConnection) url.openConnection();
                httpURLConnection.connect();

                InputStream inputStream = httpURLConnection.getInputStream();
                bufferedReader = new BufferedReader(new InputStreamReader(inputStream));

                StringBuilder stringBuilder = new StringBuilder();
                String line = "*";

                while ((line = bufferedReader.readLine()) != null) {
                    stringBuilder.append(line);
                }
                String resultData = stringBuilder.toString();

                //Convert data
                StringReader stringReader = new StringReader(resultData);
                StringBuilder builder = new StringBuilder();
                int charsRead = -1;
                char[] chars = new char[35];
                do {
                    charsRead = stringReader.read(chars, 0, chars.length);
                    if (charsRead > 0) {
                        builder.append(chars, 0, charsRead);
                    }
                } while (charsRead > 0);
                resultData = builder.toString();
                return resultData;
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                if (httpURLConnection != null)
                    httpURLConnection.disconnect();
                if (bufferedReader != null) {
                    try {
                        bufferedReader.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
            return null;
        }

        @SuppressLint("SetTextI18n")
        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);

            List<String> strings = new ArrayList<>();

            try {

                char[] chars = result.toCharArray();
                String[] strs = new String[chars.length];

                int l = chars.length;

                for (int i = 0; i < l; i++) {
                    strs[i] = String.valueOf(chars[i]);
                }

                strings.addAll(Arrays.asList(strs));
            } catch (Exception e) {
                e.printStackTrace();
//                Toast.makeText(MainActivity.this, "Ошибка загрузки событий.", Toast.LENGTH_SHORT).show();
            }

            runOnUiThread(new Runnable() {
                @SuppressLint("SuspiciousIndentation")
                @Override
                public void run() {
                    myCustomCalendar.setVisibility(View.INVISIBLE);

                    for (int i = 0; i < strings.size(); i++) {
                        int index = Integer.parseInt(strings.get(i));

                        int date = i + 1;

                        switch (index) {
                            case 1:
                                mapDateToDesc.put(date, "holiday");
                                break;
                            case 2:
                                mapDateToDesc.put(date, "short");
                                break;
                            case 4:
                                mapDateToDesc.put(date, "covid");
                                break;
                            default:
                                mapDateToDesc.put(date, "default");
                                break;
                        }
                    }

                    if (year == calendar.get(Calendar.YEAR) && month == calendar.get(Calendar.MONTH))
                        mapDateToDesc.put(calendar.get(Calendar.DAY_OF_MONTH), "current");

//                    if (b) {
                    switch (calendar.get(Calendar.MONTH)) {
                        case Calendar.JANUARY:
                            mapDateToDesc.put(1, "holiday");
                            mapDateToDesc.put(2, "holiday");
                            mapDateToDesc.put(3, "holiday");
                            mapDateToDesc.put(4, "holiday");
                            mapDateToDesc.put(5, "holiday");
                            mapDateToDesc.put(6, "holiday");
                            mapDateToDesc.put(7, "holiday");
                            mapDateToDesc.put(8, "holiday");
                            mapDateToDesc.put(12, "prof");//День работника прокураторы
                            mapDateToDesc.put(13, "mDate");//День печати
                            mapDateToDesc.put(14, "not official holiday");//Старый Новый год
                            mapDateToDesc.put(21, "mDate");//День памяти инженерных войск
                            mapDateToDesc.put(27, "mDate");//День снятия блокады города Ленинград
                            mapDateToDesc.put(25, "not official holiday");//Татьянин день(День студента)
                            break;
                        case Calendar.FEBRUARY:
                            mapDateToDesc.put(8, "prof");//День стоматолога
                            mapDateToDesc.put(9, "prof");//День работника гражданской авиации
                            mapDateToDesc.put(10, "prof");//День дипломатического работника
                            mapDateToDesc.put(14, "not official holiday");//День святого Валентина
                            mapDateToDesc.put(15, "mDate");//День памяти воинов-интернационалистов
                            mapDateToDesc.put(23, "holiday");
                            mapDateToDesc.put(27, "prof");//День Сил специальных операций
                            break;
                        case Calendar.MARCH:
                            mapDateToDesc.put(8, "holiday");
                            mapDateToDesc.put(9, "prof");
                            mapDateToDesc.put(11, "prof");//День работников органов наркоконтроля
                            mapDateToDesc.put(12, "prof");//День работников уголовно-исполнительной системы
                            mapDateToDesc.put(14, "prof");//День работника геодезиста
                            mapDateToDesc.put(18, "not official holiday");
                            mapDateToDesc.put(19, "prof");//День моряка-подводника
                            mapDateToDesc.put(27, "prof");//День нац гвардии России
                            mapDateToDesc.put(29, "prof");//День специаличста юридической службы
                            break;
                        case Calendar.APRIL:
                            mapDateToDesc.put(1, "not official holiday");
                            mapDateToDesc.put(2, "not official holiday");//День единения народов
                            mapDateToDesc.put(4, "prof");//День геолога
                            mapDateToDesc.put(8, "prof");//День сотрудников военных коммиссариатов
                            mapDateToDesc.put(12, "mDate");//День космонавтики
                            mapDateToDesc.put(26, "mDate");//День памяти погибших в радиационных авариях и катострофах
                            mapDateToDesc.put(27, "mDate");//День российского парламентаризма
                            mapDateToDesc.put(28, "prof");//День работника скорой медицинской помощи
                            mapDateToDesc.put(30, "prof");//День пожарной охраны
                            break;
                        case Calendar.MAY:
                            mapDateToDesc.put(1, "holiday");
                            mapDateToDesc.put(7, "prof");//День работников связи
                            mapDateToDesc.put(9, "holiday");
                            mapDateToDesc.put(20, "prof");//Всемирный день метрологии
                            mapDateToDesc.put(21, "prof");//День полярника
                            mapDateToDesc.put(24, "prof");//День кадровика
                            mapDateToDesc.put(25, "prof");//День филолога
                            mapDateToDesc.put(26, "prof");//День российского предпринимательства
                            mapDateToDesc.put(27, "prof");//Общероссийский день библиотек
                            mapDateToDesc.put(28, "prof");//День пограничника
                            mapDateToDesc.put(29, "prof");//День Химика
                            mapDateToDesc.put(31, "prof");//День российской адвокатуры
                            break;
                        case Calendar.JUNE:
                            mapDateToDesc.put(1, "not official holiday");
                            mapDateToDesc.put(2, "mDate");
                            mapDateToDesc.put(5, "prof");
                            mapDateToDesc.put(6, "not official holiday");
                            mapDateToDesc.put(8, "prof");
                            mapDateToDesc.put(12, "holiday");
                            mapDateToDesc.put(14, "prof");
                            mapDateToDesc.put(20, "prof");
                            mapDateToDesc.put(22, "mDate");
                            mapDateToDesc.put(26, "prof");
                            mapDateToDesc.put(27, "not official holiday");
                            mapDateToDesc.put(30, "prof");
                            break;
                        case Calendar.JULY:
                            mapDateToDesc.put(3, "prof");
                            mapDateToDesc.put(4, "prof");
                            mapDateToDesc.put(8, "not official holiday");
                            mapDateToDesc.put(11, "prof");
                            mapDateToDesc.put(17, "prof");
                            mapDateToDesc.put(18, "prof");
                            mapDateToDesc.put(25, "prof");
                            mapDateToDesc.put(28, "mDate");
                            mapDateToDesc.put(30, "prof");
                            break;
                        case Calendar.AUGUST:
                            mapDateToDesc.put(2, "mDate");
                            mapDateToDesc.put(6, "prof");
                            mapDateToDesc.put(8, "prof");
                            mapDateToDesc.put(12, "mDate");
                            mapDateToDesc.put(15, "prof");
                            mapDateToDesc.put(18, "prof");
                            mapDateToDesc.put(22, "not official holiday");
                            mapDateToDesc.put(27, "prof");
                            mapDateToDesc.put(29, "prof");
                            mapDateToDesc.put(31, "prof");
                            break;
                        case Calendar.SEPTEMBER:
                            mapDateToDesc.put(1, "not official holiday");
                            mapDateToDesc.put(3, "mDate");
                            mapDateToDesc.put(4, "mDate");
                            mapDateToDesc.put(5, "prof");
                            mapDateToDesc.put(8, "prof");
                            mapDateToDesc.put(9, "prof");
                            mapDateToDesc.put(12, "prof");
                            mapDateToDesc.put(13, "prof");
                            mapDateToDesc.put(19, "prof");
                            mapDateToDesc.put(24, "prof");
                            mapDateToDesc.put(26, "prof");
                            mapDateToDesc.put(27, "not official holiday");
                            mapDateToDesc.put(28, "prof");
                            mapDateToDesc.put(30, "prof");
                            break;
                        case Calendar.OCTOBER:
                            mapDateToDesc.put(1, "not official holiday");
                            mapDateToDesc.put(4, "mDate");
                            mapDateToDesc.put(5, "prof");
                            mapDateToDesc.put(6, "prof");
                            mapDateToDesc.put(16, "not official holiday");//День отца
                            mapDateToDesc.put(20, "prof");
                            mapDateToDesc.put(23, "prof");
                            mapDateToDesc.put(24, "mDate");
                            mapDateToDesc.put(25, "prof");
                            mapDateToDesc.put(29, "prof");
                            mapDateToDesc.put(30, "mDate");
                            mapDateToDesc.put(31, "prof");
                            break;
                        case Calendar.NOVEMBER:
                            mapDateToDesc.put(1, "prof");
                            mapDateToDesc.put(4, "holiday");
                            mapDateToDesc.put(5, "prof");
                            mapDateToDesc.put(7, "mDate");
                            mapDateToDesc.put(9, "prof");
                            mapDateToDesc.put(10, "prof");
                            mapDateToDesc.put(11, "prof");
                            mapDateToDesc.put(13, "mDate");
                            mapDateToDesc.put(14, "prof");
                            mapDateToDesc.put(21, "prof");
                            mapDateToDesc.put(22, "prof");
                            mapDateToDesc.put(27, "not official holiday"); //День матери
                            mapDateToDesc.put(30, "not official holiday");
                            break;
                        case Calendar.DECEMBER:
                            mapDateToDesc.put(3, "mDate");
                            mapDateToDesc.put(5, "prof");
                            mapDateToDesc.put(9, "mDate");
                            mapDateToDesc.put(12, "mDate");
                            mapDateToDesc.put(18, "prof");
                            mapDateToDesc.put(20, "prof");
                            mapDateToDesc.put(22, "prof");
                            mapDateToDesc.put(27, "prof");
                            break;
                        default:
                            throw new IllegalStateException("Unexpected value: " + calendar.get(Calendar.MONTH));
                    }
//                    }

                    myCustomCalendar.setDate(calendar, mapDateToDesc);
                    myCustomCalendar.setVisibility(View.VISIBLE);
                    swipeRefreshLayout.setRefreshing(false);
                }
            });
        }

        @Override
        protected void onCancelled() {
            super.onCancelled();
        }
    }

    @SuppressLint("StaticFieldLeak")
    private class ExtraDataCalendarClick extends AsyncTask<String, String, String> {
        protected int year;

        public ExtraDataCalendarClick(int year) {
            this.year = year;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected String doInBackground(String... strings) {
            HttpURLConnection httpURLConnection = null;
            BufferedReader bufferedReader = null;

            try {
                URL url = new URL(strings[0]);
                httpURLConnection = (HttpURLConnection) url.openConnection();
                httpURLConnection.connect();

                InputStream inputStream = httpURLConnection.getInputStream();
                bufferedReader = new BufferedReader(new InputStreamReader(inputStream));

                StringBuilder stringBuilder = new StringBuilder();
                String line = "*";

                while ((line = bufferedReader.readLine()) != null) {
                    stringBuilder.append(line);
                }
                String resultData = stringBuilder.toString();

                //Convert data
                StringReader stringReader = new StringReader(resultData);
                StringBuilder builder = new StringBuilder();
                int charsRead = -1;
                char[] chars = new char[370];
                do {
                    charsRead = stringReader.read(chars, 0, chars.length);
                    if (charsRead > 0) {
                        builder.append(chars, 0, charsRead);
                    }
                } while (charsRead > 0);
                resultData = builder.toString();
                return resultData;
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                if (httpURLConnection != null)
                    httpURLConnection.disconnect();
                if (bufferedReader != null) {
                    try {
                        bufferedReader.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
            return null;
        }

        @SuppressLint("SetTextI18n")
        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);

            List<String> strings = new ArrayList<>();
            try {
                char[] chars = result.toCharArray();
                String[] strs = new String[chars.length];

                int l = chars.length;

                for (int i = 0; i < l; i++) {
                    strs[i] = String.valueOf(chars[i]);
                }

                strings.addAll(Arrays.asList(strs));
            } catch (Exception e) {
                e.printStackTrace();
//                Toast.makeText(MainActivity.this, "Ошибка загрузки событий.", Toast.LENGTH_SHORT).show();
            }

            runOnUiThread(new Runnable() {
                @Override
                public void run() {

                    extraCalendar.set(Calendar.YEAR, year);

                    extraCalendar.set(Calendar.MONTH, Calendar.JANUARY);
                    int max0 = extraCalendar.getActualMaximum(Calendar.DAY_OF_MONTH);

                    extraCalendar.set(Calendar.MONTH, Calendar.FEBRUARY);
                    int max1 = extraCalendar.getActualMaximum(Calendar.DAY_OF_MONTH);

                    extraCalendar.set(Calendar.MONTH, Calendar.MARCH);
                    int max2 = extraCalendar.getActualMaximum(Calendar.DAY_OF_MONTH);

                    extraCalendar.set(Calendar.MONTH, Calendar.APRIL);
                    int max3 = extraCalendar.getActualMaximum(Calendar.DAY_OF_MONTH);

                    extraCalendar.set(Calendar.MONTH, Calendar.MAY);
                    int max4 = extraCalendar.getActualMaximum(Calendar.DAY_OF_MONTH);

                    extraCalendar.set(Calendar.MONTH, Calendar.JUNE);
                    int max5 = extraCalendar.getActualMaximum(Calendar.DAY_OF_MONTH);

                    extraCalendar.set(Calendar.MONTH, Calendar.JULY);
                    int max6 = extraCalendar.getActualMaximum(Calendar.DAY_OF_MONTH);

                    extraCalendar.set(Calendar.MONTH, Calendar.AUGUST);
                    int max7 = extraCalendar.getActualMaximum(Calendar.DAY_OF_MONTH);

                    extraCalendar.set(Calendar.MONTH, Calendar.SEPTEMBER);
                    int max8 = extraCalendar.getActualMaximum(Calendar.DAY_OF_MONTH);

                    extraCalendar.set(Calendar.MONTH, Calendar.OCTOBER);
                    int max9 = extraCalendar.getActualMaximum(Calendar.DAY_OF_MONTH);

                    extraCalendar.set(Calendar.MONTH, Calendar.NOVEMBER);
                    int max10 = extraCalendar.getActualMaximum(Calendar.DAY_OF_MONTH);

                    extraCalendar.set(Calendar.MONTH, Calendar.DECEMBER);
                    int max11 = extraCalendar.getActualMaximum(Calendar.DAY_OF_MONTH);

                    if (isLeapYear(extraCalendar.get(Calendar.YEAR)))
                        max1 = 29;
                    else
                        max1 = 28;

                    int[] ints = new int[]{max0, max1, max2, max3, max4, max5, max6, max7, max8, max9, max10, max11};

                    januaryList = new ArrayList<>();
                    februaryList = new ArrayList<>();
                    marchList = new ArrayList<>();
                    aprilList = new ArrayList<>();
                    mayList = new ArrayList<>();
                    juneList = new ArrayList<>();
                    julyList = new ArrayList<>();
                    augustList = new ArrayList<>();
                    septemberList = new ArrayList<>();
                    octoberList = new ArrayList<>();
                    novemberList = new ArrayList<>();
                    decemberList = new ArrayList<>();

                    months = new ArrayList<>();

                    months.add(januaryList);
                    months.add(februaryList);
                    months.add(marchList);
                    months.add(aprilList);
                    months.add(mayList);
                    months.add(juneList);
                    months.add(julyList);
                    months.add(augustList);
                    months.add(septemberList);
                    months.add(octoberList);
                    months.add(novemberList);
                    months.add(decemberList);

                    int sum = 0;
                    int finish = ints[0];

                    for (int i = 0; i <= 11; i++) {
                        List<Integer> integers = months.get(i);
                        for (int j = sum; j < finish; j++) {
                            try {
                                integers.add(Integer.parseInt(strings.get(j)));
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                        sum = sum + ints[i];

                        if (i != 11)
                            finish = finish + ints[i + 1];
                    }
                }
            });
        }

        @Override
        protected void onCancelled() {
            super.onCancelled();
        }
    }

    @SuppressLint("StaticFieldLeak")
    private class ExtraDataCalendarClickFuture extends AsyncTask<String, String, String> {
        protected int year;

        public ExtraDataCalendarClickFuture(int year) {
            this.year = year;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected String doInBackground(String... strings) {
            HttpURLConnection httpURLConnection = null;
            BufferedReader bufferedReader = null;

            try {
                URL url = new URL(strings[0]);
                httpURLConnection = (HttpURLConnection) url.openConnection();
                httpURLConnection.connect();

                InputStream inputStream = httpURLConnection.getInputStream();
                bufferedReader = new BufferedReader(new InputStreamReader(inputStream));

                StringBuilder stringBuilder = new StringBuilder();
                String line = "*";

                while ((line = bufferedReader.readLine()) != null) {
                    stringBuilder.append(line);
                }
                String resultData = stringBuilder.toString();

                //Convert data
                StringReader stringReader = new StringReader(resultData);
                StringBuilder builder = new StringBuilder();
                int charsRead = -1;
                char[] chars = new char[370];
                do {
                    charsRead = stringReader.read(chars, 0, chars.length);
                    if (charsRead > 0) {
                        builder.append(chars, 0, charsRead);
                    }
                } while (charsRead > 0);
                resultData = builder.toString();
                return resultData;
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                if (httpURLConnection != null)
                    httpURLConnection.disconnect();
                if (bufferedReader != null) {
                    try {
                        bufferedReader.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
            return null;
        }

        @SuppressLint("SetTextI18n")
        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);

            List<String> strings = new ArrayList<>();

            try {
                char[] chars = result.toCharArray();
                String[] strs = new String[chars.length];

                int l = chars.length;

                for (int i = 0; i < l; i++) {
                    strs[i] = String.valueOf(chars[i]);
                }

                strings.addAll(Arrays.asList(strs));
            } catch (Exception e) {
                e.printStackTrace();
//                Toast.makeText(MainActivity.this, "Ошибка загрузки событий.", Toast.LENGTH_SHORT).show();
            }

            runOnUiThread(new Runnable() {
                @Override
                public void run() {

                    extraCalendar.set(Calendar.YEAR, year);

                    extraCalendar.set(Calendar.MONTH, Calendar.JANUARY);
                    int max0 = extraCalendar.getActualMaximum(Calendar.DAY_OF_MONTH);

                    extraCalendar.set(Calendar.MONTH, Calendar.FEBRUARY);
                    int max1 = extraCalendar.getActualMaximum(Calendar.DAY_OF_MONTH);

                    extraCalendar.set(Calendar.MONTH, Calendar.MARCH);
                    int max2 = extraCalendar.getActualMaximum(Calendar.DAY_OF_MONTH);

                    extraCalendar.set(Calendar.MONTH, Calendar.APRIL);
                    int max3 = extraCalendar.getActualMaximum(Calendar.DAY_OF_MONTH);

                    extraCalendar.set(Calendar.MONTH, Calendar.MAY);
                    int max4 = extraCalendar.getActualMaximum(Calendar.DAY_OF_MONTH);

                    extraCalendar.set(Calendar.MONTH, Calendar.JUNE);
                    int max5 = extraCalendar.getActualMaximum(Calendar.DAY_OF_MONTH);

                    extraCalendar.set(Calendar.MONTH, Calendar.JULY);
                    int max6 = extraCalendar.getActualMaximum(Calendar.DAY_OF_MONTH);

                    extraCalendar.set(Calendar.MONTH, Calendar.AUGUST);
                    int max7 = extraCalendar.getActualMaximum(Calendar.DAY_OF_MONTH);

                    extraCalendar.set(Calendar.MONTH, Calendar.SEPTEMBER);
                    int max8 = extraCalendar.getActualMaximum(Calendar.DAY_OF_MONTH);

                    extraCalendar.set(Calendar.MONTH, Calendar.OCTOBER);
                    int max9 = extraCalendar.getActualMaximum(Calendar.DAY_OF_MONTH);

                    extraCalendar.set(Calendar.MONTH, Calendar.NOVEMBER);
                    int max10 = extraCalendar.getActualMaximum(Calendar.DAY_OF_MONTH);

                    extraCalendar.set(Calendar.MONTH, Calendar.DECEMBER);
                    int max11 = extraCalendar.getActualMaximum(Calendar.DAY_OF_MONTH);

                    if (isLeapYear(extraCalendar.get(Calendar.YEAR)))
                        max1 = 29;
                    else
                        max1 = 28;

                    int[] ints = new int[]{max0, max1, max2, max3, max4, max5, max6, max7, max8, max9, max10, max11};

                    januaryListFuture = new ArrayList<>();
                    februaryListFuture = new ArrayList<>();
                    marchListFuture = new ArrayList<>();
                    aprilListFuture = new ArrayList<>();
                    mayListFuture = new ArrayList<>();
                    juneListFuture = new ArrayList<>();
                    julyListFuture = new ArrayList<>();
                    augustListFuture = new ArrayList<>();
                    septemberListFuture = new ArrayList<>();
                    octoberListFuture = new ArrayList<>();
                    novemberListFuture = new ArrayList<>();
                    decemberListFuture = new ArrayList<>();

                    monthsFuture = new ArrayList<>();

                    monthsFuture.add(januaryListFuture);
                    monthsFuture.add(februaryListFuture);
                    monthsFuture.add(marchListFuture);
                    monthsFuture.add(aprilListFuture);
                    monthsFuture.add(mayListFuture);
                    monthsFuture.add(juneListFuture);
                    monthsFuture.add(julyListFuture);
                    monthsFuture.add(augustListFuture);
                    monthsFuture.add(septemberListFuture);
                    monthsFuture.add(octoberListFuture);
                    monthsFuture.add(novemberListFuture);
                    monthsFuture.add(decemberListFuture);

                    int sum = 0;
                    int finish = ints[0];

                    for (int i = 0; i < 12; i++) {
                        List<Integer> integers = monthsFuture.get(i);
                        for (int j = sum; j < finish; j++) {
                            try {
                                integers.add(Integer.parseInt(strings.get(j)));
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                        sum = sum + ints[i];
                        if (i != 11)
                            finish = finish + ints[i + 1];
                    }
                }
            });
        }

        @Override
        protected void onCancelled() {
            super.onCancelled();
        }
    }

    @SuppressLint("StaticFieldLeak")
    private class ExtraDataCalendarClickPast extends AsyncTask<String, String, String> {
        protected int year;

        public ExtraDataCalendarClickPast(int year) {
            this.year = year;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected String doInBackground(String... strings) {
            HttpURLConnection httpURLConnection = null;
            BufferedReader bufferedReader = null;

            try {
                URL url = new URL(strings[0]);
                httpURLConnection = (HttpURLConnection) url.openConnection();
                httpURLConnection.connect();

                InputStream inputStream = httpURLConnection.getInputStream();
                bufferedReader = new BufferedReader(new InputStreamReader(inputStream));

                StringBuilder stringBuilder = new StringBuilder();
                String line = "*";

                while ((line = bufferedReader.readLine()) != null) {
                    stringBuilder.append(line);
                }
                String resultData = stringBuilder.toString();

                //Convert data
                StringReader stringReader = new StringReader(resultData);
                StringBuilder builder = new StringBuilder();
                int charsRead = -1;
                char[] chars = new char[370];
                do {
                    charsRead = stringReader.read(chars, 0, chars.length);
                    if (charsRead > 0) {
                        builder.append(chars, 0, charsRead);
                    }
                } while (charsRead > 0);
                resultData = builder.toString();
                return resultData;
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                if (httpURLConnection != null)
                    httpURLConnection.disconnect();
                if (bufferedReader != null) {
                    try {
                        bufferedReader.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
            return null;
        }

        @SuppressLint("SetTextI18n")
        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);

            List<String> strings = new ArrayList<>();

            try {
                char[] chars = result.toCharArray();
                String[] strs = new String[chars.length];

                int l = chars.length;

                for (int i = 0; i < l; i++) {
                    strs[i] = String.valueOf(chars[i]);
                }

                strings.addAll(Arrays.asList(strs));
            } catch (Exception e) {
                e.printStackTrace();
//                Toast.makeText(MainActivity.this, "Ошибка загрузки событий.", Toast.LENGTH_SHORT).show();
            }

            runOnUiThread(new Runnable() {
                @Override
                public void run() {

                    extraCalendar.set(Calendar.YEAR, year);

                    extraCalendar.set(Calendar.MONTH, Calendar.JANUARY);
                    int max0 = extraCalendar.getActualMaximum(Calendar.DAY_OF_MONTH);

                    extraCalendar.set(Calendar.MONTH, Calendar.FEBRUARY);
                    int max1 = extraCalendar.getActualMaximum(Calendar.DAY_OF_MONTH);

                    extraCalendar.set(Calendar.MONTH, Calendar.MARCH);
                    int max2 = extraCalendar.getActualMaximum(Calendar.DAY_OF_MONTH);

                    extraCalendar.set(Calendar.MONTH, Calendar.APRIL);
                    int max3 = extraCalendar.getActualMaximum(Calendar.DAY_OF_MONTH);

                    extraCalendar.set(Calendar.MONTH, Calendar.MAY);
                    int max4 = extraCalendar.getActualMaximum(Calendar.DAY_OF_MONTH);

                    extraCalendar.set(Calendar.MONTH, Calendar.JUNE);
                    int max5 = extraCalendar.getActualMaximum(Calendar.DAY_OF_MONTH);

                    extraCalendar.set(Calendar.MONTH, Calendar.JULY);
                    int max6 = extraCalendar.getActualMaximum(Calendar.DAY_OF_MONTH);

                    extraCalendar.set(Calendar.MONTH, Calendar.AUGUST);
                    int max7 = extraCalendar.getActualMaximum(Calendar.DAY_OF_MONTH);

                    extraCalendar.set(Calendar.MONTH, Calendar.SEPTEMBER);
                    int max8 = extraCalendar.getActualMaximum(Calendar.DAY_OF_MONTH);

                    extraCalendar.set(Calendar.MONTH, Calendar.OCTOBER);
                    int max9 = extraCalendar.getActualMaximum(Calendar.DAY_OF_MONTH);

                    extraCalendar.set(Calendar.MONTH, Calendar.NOVEMBER);
                    int max10 = extraCalendar.getActualMaximum(Calendar.DAY_OF_MONTH);

                    extraCalendar.set(Calendar.MONTH, Calendar.DECEMBER);
                    int max11 = extraCalendar.getActualMaximum(Calendar.DAY_OF_MONTH);

                    if (isLeapYear(extraCalendar.get(Calendar.YEAR)))
                        max1 = 29;
                    else
                        max1 = 28;

                    int[] ints = new int[]{max0, max1, max2, max3, max4, max5, max6, max7, max8, max9, max10, max11};

                    januaryListPast = new ArrayList<>();
                    februaryListPast = new ArrayList<>();
                    marchListPast = new ArrayList<>();
                    aprilListPast = new ArrayList<>();
                    mayListPast = new ArrayList<>();
                    juneListPast = new ArrayList<>();
                    julyListPast = new ArrayList<>();
                    augustListPast = new ArrayList<>();
                    septemberListPast = new ArrayList<>();
                    octoberListPast = new ArrayList<>();
                    novemberListPast = new ArrayList<>();
                    decemberListPast = new ArrayList<>();

                    monthsPast = new ArrayList<>();

                    monthsPast.add(januaryListPast);
                    monthsPast.add(februaryListPast);
                    monthsPast.add(marchListPast);
                    monthsPast.add(aprilListPast);
                    monthsPast.add(mayListPast);
                    monthsPast.add(juneListPast);
                    monthsPast.add(julyListPast);
                    monthsPast.add(augustListPast);
                    monthsPast.add(septemberListPast);
                    monthsPast.add(octoberListPast);
                    monthsPast.add(novemberListPast);
                    monthsPast.add(decemberListPast);

                    int sum = 0;
                    int finish = ints[0];

                    for (int i = 0; i < 12; i++) {
                        List<Integer> integers = monthsPast.get(i);
                        for (int j = sum; j < finish; j++) {
                            try {
                                integers.add(Integer.parseInt(strings.get(j)));
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                        sum = sum + ints[i];
                        if (i != 11)
                            finish = finish + ints[i + 1];
                    }
                }
            });
        }

        @Override
        protected void onCancelled() {
            super.onCancelled();
        }
    }

    public static class Link {
        public String buildLink(int year, int month, String cc, int pre, int covid, int sd) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder
                    .append("https://isdayoff.ru/api/getdata?year=")
                    .append(year)
                    .append("&month=")
                    .append(month)
                    .append("&cc=")
                    .append(cc.toLowerCase(Locale.ROOT))
                    .append("&pre=[")
                    .append(pre)
                    .append("]delimeter=%0A&covid=[")
                    .append(covid)
                    .append("]&sd=[")
                    .append(sd)
                    .append("]");
            return stringBuilder.toString();
        }

        public String buildYearLink(int year, String cc, int pre, int covid, int sd) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder
                    .append("https://isdayoff.ru/api/getdata?year=")
                    .append(year)
                    .append("&cc=")
                    .append(cc.toLowerCase(Locale.ROOT))
                    .append("&pre=[")
                    .append(pre)
                    .append("]delimeter=%0A&covid=[")
                    .append(covid)
                    .append("]&sd=[")
                    .append(sd)
                    .append("]");
            return stringBuilder.toString();
        }
    }

    public static int getAttrColor(Context context, int attr) {
        TypedValue typedValue = new TypedValue();
        context.getTheme().resolveAttribute(attr, typedValue, true);
        return typedValue.data;
    }

    public static boolean isLeapYear(int year) {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.YEAR, year);
        return cal.getActualMaximum(Calendar.DAY_OF_YEAR) > 365;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        clearAppOpenAd();
    }

    private void showAppOpenAd() {
        if (mAppOpenAd != null) {
            mAppOpenAd.show(MainActivity.this);
        }
    }

    private void clearAppOpenAd() {
        if (mAppOpenAd != null) {
            mAppOpenAd.setAdEventListener(null);
            mAppOpenAd = null;
        }
    }
}
package com.plumsoftware.rucalendar.activities;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.drawable.ClipDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.widget.ImageViewCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import android.animation.ObjectAnimator;

import com.plumsoftware.rucalendar.R;
import com.plumsoftware.rucalendar.ads.AppOpenAdController;
import com.plumsoftware.rucalendar.data.DayEvent;
import com.plumsoftware.rucalendar.ui.Motion;
import com.plumsoftware.rucalendar.ui.event.UserEventActivity;
import com.plumsoftware.rucalendar.ui.feed.FeedFragment;
import com.plumsoftware.rucalendar.ui.year.YearFragment;
import com.google.android.material.snackbar.Snackbar;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import com.plumsoftware.rucalendar.ui.month.MonthFragment;
import com.plumsoftware.rucalendar.ui.sheet.DaySheetFragment;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;

/** Главный экран: нижняя панель «Месяц» / «Лента» / «Год» (ТЗ п. 3). */
public class MainActivity extends AppCompatActivity
        implements MonthFragment.Host, FeedFragment.Host, YearFragment.Host, DaySheetFragment.Host {

    private static final String TAG_MONTH = "month";
    private static final String TAG_FEED = "feed";
    private static final String TAG_YEAR = "year";
    private static final String STATE_TAB = "tab";

    private static final int TAB_MONTH = 0;
    private static final int TAB_FEED = 1;
    private static final int TAB_YEAR = 2;

    private int currentTab = TAB_MONTH;
    private OnBackPressedCallback backToMonth;
    private ActivityResultLauncher<Intent> eventLauncher;
    private AppOpenAdController appOpenAd;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Отрисовка от края до края: фон под строкой состояния и панелью жестов (п. 7)
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        setContentView(R.layout.activity_main);

        FragmentManager fm = getSupportFragmentManager();
        if (savedInstanceState == null) {
            fm.beginTransaction()
                    .add(R.id.fragment_container, new MonthFragment(), TAG_MONTH)
                    .add(R.id.fragment_container, new FeedFragment(), TAG_FEED)
                    .add(R.id.fragment_container, new YearFragment(), TAG_YEAR)
                    .commitNow();
            fm.beginTransaction().hide(feed()).hide(year()).commitNow();
        } else {
            currentTab = savedInstanceState.getInt(STATE_TAB, TAB_MONTH);
        }

        eventLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            Intent data = result.getData();
            if (result.getResultCode() != RESULT_OK || data == null) return;
            String dateString = data.getStringExtra(UserEventActivity.RESULT_DATE);
            boolean deleted = data.getBooleanExtra(UserEventActivity.RESULT_DELETED, false);
            if (dateString != null) {
                // «Месяц» открывается на дате события, день выбран
                LocalDate date = LocalDate.parse(dateString);
                selectTab(TAB_MONTH, false);
                MonthFragment month = month();
                if (month != null && month.getView() != null) {
                    month.showDate(date);
                    if (!deleted) month.celebrate(date);
                }
            }
            showToast(deleted ? R.string.event_deleted : R.string.event_saved);
        });

        // «Назад» с любой вкладки возвращает на «Месяц», с «Месяца» — выход из приложения
        backToMonth = new OnBackPressedCallback(currentTab != TAB_MONTH) {
            @Override
            public void handleOnBackPressed() {
                selectTab(TAB_MONTH, true);
            }
        };
        getOnBackPressedDispatcher().addCallback(this, backToMonth);

        setupBottomNav();
        renderTabs(false);
        // После пересоздания активности ссылка уже обработана
        if (savedInstanceState == null) handleDeepLinkIntent(getIntent());

        // Реклама при открытии: при выключенном флаге SHOW_APP_OPEN_AD не загружается
        appOpenAd = new AppOpenAdController(this);
        if (savedInstanceState == null) appOpenAd.loadAndShow();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (appOpenAd != null) appOpenAd.clear();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(STATE_TAB, currentTab);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleDeepLinkIntent(intent);
    }

    // region Хосты фрагментов

    @Override
    public void openDay(DayEvent event) {
        DaySheetFragment.show(getSupportFragmentManager(), event.date, event.key());
    }

    @Override
    public void openSearch() {
        selectTab(TAB_FEED, true);
        feed().focusSearch();
    }

    @Override
    public void openNewEvent(LocalDate date) {
        eventLauncher.launch(UserEventActivity.newEvent(this, date));
    }

    @Override
    public void editUserEvent(DayEvent event) {
        if (event.userEvent != null) eventLauncher.launch(UserEventActivity.editEvent(this, event.userEvent.id));
    }

    @Override
    public void openMonth(YearMonth target) {
        selectTab(TAB_MONTH, true);
        MonthFragment month = month();
        if (month == null || month.getView() == null) return;
        LocalDate today = LocalDate.now();
        month.showDate(YearMonth.from(today).equals(target) ? today : target.atDay(1));
    }

    // endregion

    /** Всплывающая подсказка над нижней панелью, держится 3 с (п. 9.2). */
    private void showToast(int text) {
        Snackbar.make(findViewById(R.id.fragment_container), text, 3000)
                .setAnchorView(R.id.bottom_nav)
                .show();
    }

    private MonthFragment month() {
        return (MonthFragment) getSupportFragmentManager().findFragmentByTag(TAG_MONTH);
    }

    private FeedFragment feed() {
        return (FeedFragment) getSupportFragmentManager().findFragmentByTag(TAG_FEED);
    }

    private YearFragment year() {
        return (YearFragment) getSupportFragmentManager().findFragmentByTag(TAG_YEAR);
    }

    @Nullable
    private Fragment fragmentFor(int tab) {
        if (tab == TAB_MONTH) return month();
        if (tab == TAB_FEED) return feed();
        return year();
    }

    private void selectTab(int tab, boolean animate) {
        if (tab == currentTab) return;
        final Fragment from = fragmentFor(currentTab);
        final Fragment to = fragmentFor(tab);
        currentTab = tab;
        backToMonth.setEnabled(tab != TAB_MONTH);
        renderTabs(animate);
        if (from == null || to == null) return;

        final View fromView = from.getView();
        if (!animate || fromView == null) {
            getSupportFragmentManager().beginTransaction().hide(from).show(to).commit();
            return;
        }
        // Старый экран затухает за 90 мс, новый проявляется с масштабом 0,96 → 1; всего 200 мс (п. 9.2)
        fromView.animate().cancel();
        fromView.animate().alpha(0f).setDuration(90).setInterpolator(Motion.STANDARD).withEndAction(() -> {
            fromView.setAlpha(1f);
            getSupportFragmentManager().beginTransaction().hide(from).show(to).commitNow();
            View toView = to.getView();
            if (toView == null) return;
            toView.setAlpha(0f);
            toView.setScaleX(0.96f);
            toView.setScaleY(0.96f);
            toView.animate().alpha(1f).scaleX(1f).scaleY(1f).setDuration(110).setInterpolator(Motion.STANDARD).start();
        }).start();
    }

    private void setupBottomNav() {
        final View nav = findViewById(R.id.bottom_nav);
        final int navHeight = getResources().getDimensionPixelSize(R.dimen.bottom_nav_height);
        ViewCompat.setOnApplyWindowInsetsListener(nav, (v, windowInsets) -> {
            Insets bars = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());
            ViewGroup.LayoutParams lp = v.getLayoutParams();
            lp.height = navHeight + bars.bottom;
            v.setLayoutParams(lp);
            v.setPadding(bars.left, 0, bars.right, bars.bottom);
            return windowInsets;
        });
        findViewById(R.id.tab_month).setOnClickListener(v -> selectTab(TAB_MONTH, true));
        findViewById(R.id.tab_feed).setOnClickListener(v -> selectTab(TAB_FEED, true));
        findViewById(R.id.tab_year).setOnClickListener(v -> selectTab(TAB_YEAR, true));
    }

    private void renderTabs(boolean animate) {
        renderTab(R.id.tab_month, R.id.tab_month_icon, R.id.tab_month_label, currentTab == TAB_MONTH, animate);
        renderTab(R.id.tab_feed, R.id.tab_feed_icon, R.id.tab_feed_label, currentTab == TAB_FEED, animate);
        renderTab(R.id.tab_year, R.id.tab_year_icon, R.id.tab_year_label, currentTab == TAB_YEAR, animate);
    }

    private void renderTab(int tabId, int iconId, int labelId, boolean active, boolean animate) {
        View tab = findViewById(tabId);
        ImageView icon = findViewById(iconId);
        TextView label = findViewById(labelId);
        tab.setSelected(active);
        int color = ContextCompat.getColor(this, active ? R.color.ds_text_primary : R.color.ds_text_secondary);
        ImageViewCompat.setImageTintList(icon, ColorStateList.valueOf(color));
        label.setTextColor(color);
        label.setTypeface(ResourcesCompat.getFont(this, active ? R.font.golos_text_bold : R.font.golos_text_medium));
        if (!active) {
            icon.setBackground(null);
            return;
        }
        // Капсула активной вкладки растягивается от центра за 150 мс (п. 9.2)
        ClipDrawable capsule = new ClipDrawable(ContextCompat.getDrawable(this, R.drawable.bg_tab_active),
                Gravity.CENTER_HORIZONTAL, ClipDrawable.HORIZONTAL);
        icon.setBackground(capsule);
        if (animate) {
            capsule.setLevel(0);
            ObjectAnimator animator = ObjectAnimator.ofInt(capsule, "level", 0, 10000);
            animator.setDuration(150);
            animator.setInterpolator(Motion.DECELERATE);
            animator.start();
        } else {
            capsule.setLevel(10000);
        }
    }

    private void handleDeepLinkIntent(Intent intent) {
        if (intent == null || !Intent.ACTION_VIEW.equals(intent.getAction())) {
            return;
        }
        Uri data = intent.getData();
        if (data == null || !"rucalendar".equals(data.getScheme())) {
            return;
        }
        if ("day".equals(data.getHost())) {
            openDayFromLink(data);
            return;
        }
        if ("month".equals(data.getHost())) {
            openMonthFromLink(data);
            return;
        }
        if (!"event".equals(data.getHost())) {
            return;
        }

        // Старые напоминания, поставленные до редизайна, открывают прежний экран праздника
        String eventName = data.getQueryParameter("name");
        String eventDesc = data.getQueryParameter("desc");
        String eventColor = data.getQueryParameter("color");
        String eventTimeString = data.getQueryParameter("time");

        long eventTimeMillis = System.currentTimeMillis();
        if (eventTimeString != null) {
            try {
                eventTimeMillis = Long.parseLong(eventTimeString);
            } catch (NumberFormatException ignored) {
            }
        }

        Intent eventIntent = new Intent(this, EventActivity.class);
        eventIntent.putExtra("time", eventTimeMillis);
        eventIntent.putExtra("name", eventName);
        eventIntent.putExtra("desc", eventDesc);
        eventIntent.putExtra("color", eventColor);
        startActivity(eventIntent);
    }

    /** rucalendar://month?date=2026-10-03 — виджеты открывают «Месяц» на этом дне. */
    private void openMonthFromLink(Uri data) {
        LocalDate date = parseDate(data.getQueryParameter("date"));
        if (date == null) return;
        selectTab(TAB_MONTH, false);
        MonthFragment month = month();
        if (month != null) month.showDateWhenReady(date);
    }

    @Nullable
    private static LocalDate parseDate(@Nullable String value) {
        if (value == null) return null;
        try {
            return LocalDate.parse(value);
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    /** rucalendar://day?date=2026-11-04&id=1104-1 — уведомление и виджеты открывают карточку дня. */
    private void openDayFromLink(Uri data) {
        LocalDate date = parseDate(data.getQueryParameter("date"));
        if (date == null) return;
        selectTab(TAB_MONTH, false);
        MonthFragment month = month();
        if (month != null) month.showDateWhenReady(date);
        DaySheetFragment.show(getSupportFragmentManager(), date, data.getQueryParameter("id"));
    }
}

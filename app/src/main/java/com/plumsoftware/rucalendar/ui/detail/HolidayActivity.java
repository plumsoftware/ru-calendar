package com.plumsoftware.rucalendar.ui.detail;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.content.res.ColorStateList;
import android.os.Build;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import com.plumsoftware.rucalendar.R;
import com.plumsoftware.rucalendar.ads.BannerAds;
import com.plumsoftware.rucalendar.ads.InterstitialController;
import com.plumsoftware.rucalendar.config.AdsConfig;
import com.yandex.mobile.ads.banner.BannerAdView;
import com.plumsoftware.rucalendar.data.FavoritesRepository;
import com.plumsoftware.rucalendar.data.Holiday;
import com.plumsoftware.rucalendar.data.HolidayRepository;
import com.plumsoftware.rucalendar.data.ProductionCalendarRepository;
import com.plumsoftware.rucalendar.data.ReminderRepository;
import com.plumsoftware.rucalendar.data.RuDates;
import com.plumsoftware.rucalendar.reminders.ReminderScheduler;
import com.plumsoftware.rucalendar.ui.FlowLayout;
import com.plumsoftware.rucalendar.ui.ShareText;
import com.plumsoftware.rucalendar.ui.reminder.ReminderBlock;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** О празднике: полное описание, напоминание, даты в другие годы (ТЗ п. 4.3). */
public class HolidayActivity extends AppCompatActivity {
    private static final String EXTRA_HOLIDAY_ID = "holiday_id";
    private static final String EXTRA_DATE = "date";

    private Holiday holiday;
    private LocalDate date;
    private ReminderRepository reminders;
    private FavoritesRepository favorites;
    private ReminderBlock reminderBlock;
    private ActivityResultLauncher<String> permissionLauncher;
    private InterstitialController interstitial;
    @Nullable
    private BannerAdView banner;
    /** Высота контейнера с баннером; 0 — баннера нет. */
    private int bannerHeight;

    public static void start(Context context, String holidayId, @Nullable LocalDate date) {
        Intent intent = new Intent(context, HolidayActivity.class)
                .putExtra(EXTRA_HOLIDAY_ID, holidayId);
        if (date != null) intent.putExtra(EXTRA_DATE, date.toString());
        if (!(context instanceof android.app.Activity)) intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        holiday = HolidayRepository.get(this).byId(String.valueOf(getIntent().getStringExtra(EXTRA_HOLIDAY_ID)));
        if (holiday == null) {
            finish();
            return;
        }
        String dateExtra = getIntent().getStringExtra(EXTRA_DATE);
        date = dateExtra != null ? LocalDate.parse(dateExtra) : nextOccurrence(holiday);
        reminders = new ReminderRepository(this);
        favorites = new FavoritesRepository(this);

        permissionLauncher = registerForActivityResult(new ActivityResultContracts.RequestPermission(),
                granted -> reminderBlock.onPermissionResult(granted));

        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        // Цветная шапка под строкой состояния — светлые значки в обеих темах
        new WindowInsetsControllerCompat(getWindow(), getWindow().getDecorView()).setAppearanceLightStatusBars(false);
        setContentView(R.layout.activity_holiday);

        bindHeader();
        ((TextView) findViewById(R.id.description)).setText(holiday.description);
        bindReminder();
        bindOtherYears();
        applyInsets();
        setupAds();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (interstitial != null) interstitial.destroy();
        if (banner != null) banner.destroy();
    }

    /** Межстраничная реклама грузится при входе и показывается при закрытии; баннер — внизу экрана. */
    private void setupAds() {
        interstitial = new InterstitialController(this);
        interstitial.load();
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                close();
            }
        });
        if (AdsConfig.SHOW_EVENT_SCREEN_BANNER) {
            FrameLayout container = findViewById(R.id.ad_container);
            container.post(() -> {
                if (isFinishing() || isDestroyed()) return;
                banner = BannerAds.load(this, container, AdsConfig.BANNER_EVENT_SCREEN_AD, height -> {
                    bannerHeight = height;
                    ViewCompat.requestApplyInsets(findViewById(R.id.scroll));
                });
            });
        }
    }

    private void close() {
        interstitial.showThen(this::finish);
    }

    private void applyInsets() {
        View header = findViewById(R.id.header);
        View body = findViewById(R.id.body);
        final float density = getResources().getDisplayMetrics().density;
        final int bodyBottom = body.getPaddingBottom();
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.scroll), (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            header.setPadding(header.getPaddingLeft(), bars.top + Math.round(8 * density),
                    header.getPaddingRight(), header.getPaddingBottom());
            // Под баннером — отступ на его высоту + 12 dp, чтобы конец страницы не прятался
            View ad = findViewById(R.id.ad_container);
            ad.setPadding(0, 0, 0, bars.bottom);
            int bottom = bannerHeight > 0 ? bannerHeight + Math.round(12 * density) : bars.bottom;
            body.setPadding(body.getPaddingLeft(), body.getPaddingTop(), body.getPaddingRight(), bodyBottom + bottom);
            return insets;
        });
    }

    private void bindHeader() {
        float density = getResources().getDisplayMetrics().density;
        int solid = ContextCompat.getColor(this, holiday.type.solidColorRes);
        int deep = ContextCompat.getColor(this, holiday.type.deepColorRes);

        GradientDrawable headerBg = new GradientDrawable();
        headerBg.setColor(solid);
        float r = 36 * density;
        headerBg.setCornerRadii(new float[]{0, 0, 0, 0, r, r, r, r});
        findViewById(R.id.header).setBackground(headerBg);

        findViewById(R.id.back).setOnClickListener(v -> close());

        ImageButton share = findViewById(R.id.share);
        share.setBackground(circle(deep));
        share.setOnClickListener(v -> startActivity(Intent.createChooser(ShareText.intent(holiday, date), null)));

        ImageButton favorite = findViewById(R.id.favorite);
        favorite.setBackground(circle(deep));
        renderFavorite(favorite, favorites.isFavorite(holiday.id));
        favorite.setOnClickListener(v -> renderFavorite(favorite, favorites.toggle(holiday.id)));

        ((TextView) findViewById(R.id.day_number)).setText(String.valueOf(date.getDayOfMonth()));
        ((TextView) findViewById(R.id.month_genitive)).setText(RuDates.monthGenitive(date.getMonthValue()));
        ((TextView) findViewById(R.id.weekday_year)).setText(RuDates.weekdayLower(date) + " · " + date.getYear());
        ((TextView) findViewById(R.id.title)).setText(holiday.name);

        FlowLayout chips = findViewById(R.id.chips);
        // Чип типа — белый с тёмным текстом, остальные — тёмные с белым (макет «О празднике»)
        chips.addView(chip(holiday.type.label, 0xFFFFFFFF, deep));
        if (ProductionCalendarRepository.get(this).isDayOff(date)) {
            chips.addView(chip(getString(R.string.sheet_day_off), deep, 0xFFFFFFFF));
        }
        chips.addView(chip(RuDates.relativeLong(RuDates.daysBetween(LocalDate.now(), date)), deep, 0xFFFFFFFF));
    }

    private void renderFavorite(ImageButton button, boolean favorite) {
        button.setImageResource(favorite ? R.drawable.ic_star_filled : R.drawable.ic_favorite);
        button.setContentDescription(getString(favorite ? R.string.cd_favorite_remove : R.string.cd_favorite_add));
    }

    private void bindReminder() {
        reminderBlock = new ReminderBlock(findViewById(R.id.reminder_block),
                (daysBefore, minutes) -> {
                    ReminderRepository.Reminder probe = new ReminderRepository.Reminder(holiday.id, daysBefore, minutes);
                    LocalDate eventDate = ReminderScheduler.nextEventDate(holiday, probe, LocalDateTime.now());
                    return ReminderScheduler.triggerTime(eventDate, probe).toLocalDate();
                },
                new ReminderBlock.Callback() {
                    @Override
                    public void onRequestPermission() {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
                        } else {
                            reminderBlock.onPermissionResult(true);
                        }
                    }

                    @Override
                    public void onChanged(boolean enabled, int daysBefore, int minutes) {
                        if (enabled) {
                            ReminderScheduler.enable(HolidayActivity.this,
                                    new ReminderRepository.Reminder(holiday.id, daysBefore, minutes));
                        } else {
                            ReminderScheduler.disable(HolidayActivity.this, holiday.id);
                        }
                    }
                });
        ReminderRepository.Reminder saved = reminders.get(holiday.id);
        if (saved != null) {
            reminderBlock.bind(true, saved.daysBefore, saved.minutes);
        } else {
            reminderBlock.bind(false, ReminderRepository.DEFAULT_DAYS_BEFORE, ReminderRepository.DEFAULT_MINUTES);
        }
    }

    /** Три плитки: текущий год и два следующих; для плавающих дат — и число (п. 4.3). */
    private void bindOtherYears() {
        LinearLayout container = findViewById(R.id.other_years);
        float density = getResources().getDisplayMetrics().density;
        int currentYear = LocalDate.now().getYear();
        for (int i = 0; i < 3; i++) {
            int year = currentYear + i;
            LocalDate d = holiday.dateIn(year);
            boolean current = i == 0;

            LinearLayout tile = new LinearLayout(this);
            tile.setOrientation(LinearLayout.VERTICAL);
            int ph = Math.round(12 * density);
            int pv = Math.round(10 * density);
            tile.setPadding(ph, pv, ph, pv);
            GradientDrawable bg = new GradientDrawable();
            bg.setCornerRadius(16 * density);
            bg.setColor(current
                    ? ContextCompat.getColor(this, holiday.type.plateColorRes)
                    : ContextCompat.getColor(this, R.color.ds_bg_screen));
            tile.setBackground(bg);

            TextView yearView = new TextView(this);
            yearView.setText(String.valueOf(year));
            yearView.setTypeface(ResourcesCompat.getFont(this, R.font.unbounded_bold));
            yearView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
            yearView.setTextColor(current
                    ? ContextCompat.getColor(this, holiday.type.colorRes)
                    : ContextCompat.getColor(this, R.color.ds_text_primary));
            tile.addView(yearView);

            TextView dayView = new TextView(this);
            String text = holiday.isFloating()
                    ? RuDates.dayMonthShort(d) + ", " + RuDates.weekdayShort(d)
                    : RuDates.weekdayLower(d);
            dayView.setText(text);
            dayView.setTypeface(ResourcesCompat.getFont(this, R.font.golos_text_regular));
            dayView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
            dayView.setTextColor(ContextCompat.getColor(this, R.color.ds_text_body));
            LinearLayout.LayoutParams dayLp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            dayLp.topMargin = Math.round(2 * density);
            tile.addView(dayView, dayLp);
            tile.setContentDescription(year + ": " + RuDates.dayMonth(d) + ", " + RuDates.weekdayLower(d));
            tile.setFocusable(true);

            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
            if (i > 0) lp.setMarginStart(Math.round(8 * density));
            container.addView(tile, lp);
        }
    }

    private RippleDrawable circle(int color) {
        GradientDrawable shape = new GradientDrawable();
        shape.setShape(GradientDrawable.OVAL);
        shape.setColor(color);
        return new RippleDrawable(ColorStateList.valueOf(0x33FFFFFF), shape, null);
    }

    private TextView chip(String text, int background, int textColor) {
        float density = getResources().getDisplayMetrics().density;
        TextView chip = new TextView(this);
        chip.setText(text);
        chip.setTextColor(textColor);
        chip.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        chip.setTypeface(ResourcesCompat.getFont(this, R.font.golos_text_semibold));
        chip.setGravity(Gravity.CENTER_VERTICAL);
        int ph = Math.round(10 * density);
        int pv = Math.round(5 * density);
        chip.setPadding(ph, pv, ph, pv);
        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(10 * density);
        bg.setColor(background);
        chip.setBackground(bg);
        return chip;
    }

    private static LocalDate nextOccurrence(Holiday holiday) {
        LocalDate today = LocalDate.now();
        LocalDate d = holiday.dateIn(today.getYear());
        return d.isBefore(today) ? holiday.dateIn(today.getYear() + 1) : d;
    }
}

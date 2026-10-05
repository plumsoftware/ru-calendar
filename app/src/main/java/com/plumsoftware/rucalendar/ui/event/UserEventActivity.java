package com.plumsoftware.rucalendar.ui.event;

import android.Manifest;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.ColorUtils;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.widget.ImageViewCompat;

import com.google.android.material.datepicker.MaterialDatePicker;
import com.plumsoftware.rucalendar.R;
import com.plumsoftware.rucalendar.ads.InterstitialController;
import com.plumsoftware.rucalendar.data.EventType;
import com.plumsoftware.rucalendar.data.ProductionCalendarRepository;
import com.plumsoftware.rucalendar.data.ReminderRepository;
import com.plumsoftware.rucalendar.data.RuDates;
import com.plumsoftware.rucalendar.data.UserEvent;
import com.plumsoftware.rucalendar.data.UserEventRepository;
import com.plumsoftware.rucalendar.reminders.ReminderScheduler;
import com.plumsoftware.rucalendar.ui.Motion;
import com.plumsoftware.rucalendar.ui.TypeColors;
import com.plumsoftware.rucalendar.ui.reminder.NotificationPermission;
import com.plumsoftware.rucalendar.ui.reminder.ReminderBlock;
import com.plumsoftware.rucalendar.widget.WidgetUpdater;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/** Новое событие / Изменить событие (ТЗ п. 4.6). */
public class UserEventActivity extends AppCompatActivity {
    public static final String EXTRA_DATE = "date";
    public static final String EXTRA_EVENT_ID = "event_id";
    /** Результат: дата сохранённого события; при удалении — флаг. */
    public static final String RESULT_DATE = "result_date";
    public static final String RESULT_DELETED = "result_deleted";

    private static final float SATURATION = 0.65f;
    private static final int LIGHTNESS_MIN = 25;
    private static final int LIGHTNESS_MAX = 70;

    private UserEventRepository repository;
    @Nullable
    private UserEvent editing;

    private LocalDate date;
    private boolean yearly = true;
    private int color;
    /** Индекс выбранного недавнего цвета; −1 — свой цвет с ползунков. */
    private int recentIndex;
    private boolean reminderEnabled = true;
    private int reminderDays = ReminderRepository.DEFAULT_DAYS_BEFORE;
    private int reminderMinutes = ReminderRepository.DEFAULT_MINUTES;
    private String initialState;
    private boolean saveAfterPermission;

    private List<Integer> recentColors;
    private EditText titleInput;
    private EditText descriptionInput;
    private TextView saveButton;
    private TextView dateValue;
    private View dateRow;
    private SwitchCompat yearlySwitch;
    private TextView previewDay;
    private ImageView previewDayStar;
    private ImageView previewPlate;
    private TextView previewTitle;
    private TextView previewSubtitle;
    private View badgeSwatch;
    private TextView badgeHex;
    private LinearLayout recentContainer;
    private ColorSliderView hueSlider;
    private ColorSliderView lightnessSlider;
    private ReminderBlock reminderBlock;
    private int displayedColor;
    @Nullable
    private ValueAnimator colorAnimator;
    private ActivityResultLauncher<String> permissionLauncher;
    /** Межстраничная реклама после создания нового события; при редактировании не показывается. */
    @Nullable
    private InterstitialController interstitial;

    public static Intent newEvent(Context context, LocalDate date) {
        return new Intent(context, UserEventActivity.class).putExtra(EXTRA_DATE, date.toString());
    }

    public static Intent editEvent(Context context, String eventId) {
        return new Intent(context, UserEventActivity.class).putExtra(EXTRA_EVENT_ID, eventId);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        repository = UserEventRepository.get(this);
        String eventId = getIntent().getStringExtra(EXTRA_EVENT_ID);
        editing = eventId != null ? repository.byId(eventId) : null;
        recentColors = repository.recentColors();

        if (editing != null) {
            date = editing.date;
            yearly = editing.yearly;
            color = editing.color;
            recentIndex = recentColors.indexOf(editing.color);
            reminderEnabled = editing.reminderDaysBefore != UserEvent.REMINDER_OFF;
            if (reminderEnabled) reminderDays = editing.reminderDaysBefore;
            reminderMinutes = editing.reminderMinutes;
        } else {
            String dateExtra = getIntent().getStringExtra(EXTRA_DATE);
            date = dateExtra != null ? LocalDate.parse(dateExtra) : LocalDate.now();
            // По умолчанию выбран первый из недавних цветов
            recentIndex = 0;
            color = recentColors.get(0);
        }
        displayedColor = color;

        permissionLauncher = registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
            reminderBlock.onPermissionResult(granted);
            if (saveAfterPermission) {
                saveAfterPermission = false;
                save();
            }
        });

        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        setContentView(R.layout.activity_user_event);
        bindViews();
        applyInsets();

        if (editing != null) {
            ((TextView) findViewById(R.id.screen_title)).setText(R.string.event_edit_title);
            titleInput.setText(editing.title);
            descriptionInput.setText(editing.description);
            View delete = findViewById(R.id.delete);
            delete.setVisibility(View.VISIBLE);
            delete.setOnClickListener(v -> confirmDelete());
        }
        yearlySwitch.setChecked(yearly);
        setSlidersFromColor(color);
        buildRecentColors();
        reminderBlock.bind(reminderEnabled, reminderDays, reminderMinutes);
        render();
        initialState = snapshot();

        // При открытии фокус в поле названия, клавиатура открыта
        titleInput.requestFocus();
        titleInput.setSelection(titleInput.getText().length());
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);

        if (editing == null) {
            // Реклама загружается при входе на экран, чтобы к сохранению уже была готова
            interstitial = new InterstitialController(this);
            interstitial.load();
        }

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                close();
            }
        });
    }

    private void bindViews() {
        titleInput = findViewById(R.id.input_title);
        descriptionInput = findViewById(R.id.input_description);
        saveButton = findViewById(R.id.save);
        dateValue = findViewById(R.id.date_value);
        dateRow = findViewById(R.id.date_row);
        yearlySwitch = findViewById(R.id.yearly_switch);
        previewDay = findViewById(R.id.preview_day);
        previewDayStar = findViewById(R.id.preview_day_star);
        previewPlate = findViewById(R.id.preview_plate);
        previewTitle = findViewById(R.id.preview_title);
        previewSubtitle = findViewById(R.id.preview_subtitle);
        badgeSwatch = findViewById(R.id.color_badge_swatch);
        badgeHex = findViewById(R.id.color_badge_hex);
        recentContainer = findViewById(R.id.recent_colors);
        hueSlider = findViewById(R.id.hue_slider);
        lightnessSlider = findViewById(R.id.lightness_slider);

        findViewById(R.id.close).setOnClickListener(v -> close());
        saveButton.setOnClickListener(v -> save());
        dateRow.setOnClickListener(v -> showDatePicker());
        yearlySwitch.setOnCheckedChangeListener((button, checked) -> {
            yearly = checked;
            render();
        });

        TextWatcher watcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                render();
            }
        };
        titleInput.addTextChangedListener(watcher);
        descriptionInput.addTextChangedListener(watcher);

        hueSlider.setRange(0, 359);
        lightnessSlider.setRange(LIGHTNESS_MIN, LIGHTNESS_MAX);
        hueSlider.setTrackColors(new int[]{0xFFE03131, 0xFFE0A800, 0xFF37B24D, 0xFF15AABF, 0xFF3654D9, 0xFFAE3EC9, 0xFFE03131});
        ColorSliderView.Listener sliderListener = (slider, value) -> {
            // Движение ползунка снимает выбор с «Недавних»
            recentIndex = -1;
            setColor(sliderColor(), false);
            updateSliderVisuals();
            renderRecentSelection(true);
        };
        hueSlider.setListener(sliderListener);
        lightnessSlider.setListener(sliderListener);

        reminderBlock = new ReminderBlock(findViewById(R.id.reminder_block), this::notificationDate,
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
                        reminderEnabled = enabled;
                        reminderDays = daysBefore;
                        reminderMinutes = minutes;
                    }
                });
    }

    /** Выбор даты в стиле приложения; MaterialDatePicker работает с полуночью по UTC. */
    private void showDatePicker() {
        long selection = date.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli();
        MaterialDatePicker<Long> picker = MaterialDatePicker.Builder.datePicker()
                .setTitleText(R.string.event_field_date)
                .setSelection(selection)
                .build();
        picker.addOnPositiveButtonClickListener(millis -> {
            date = Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate();
            render();
        });
        picker.show(getSupportFragmentManager(), "event_date");
    }

    private void applyInsets() {
        View content = findViewById(R.id.content);
        final float density = getResources().getDisplayMetrics().density;
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.scroll), (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            Insets ime = insets.getInsets(WindowInsetsCompat.Type.ime());
            content.setPadding(content.getPaddingLeft(), bars.top + Math.round(12 * density),
                    content.getPaddingRight(), Math.max(bars.bottom, ime.bottom) + Math.round(24 * density));
            return insets;
        });
    }

    // region Состояние и предпросмотр

    private String title() {
        return titleInput.getText().toString().trim();
    }

    private void render() {
        boolean canSave = !title().isEmpty();
        saveButton.setEnabled(canSave);
        saveButton.setAlpha(canSave ? 1f : 0.4f);

        String dateText = RuDates.weekdayDayMonth(date) + " " + date.getYear();
        dateValue.setText(dateText);
        dateRow.setContentDescription(getString(R.string.cd_change_date, dateText));

        // Предпросмотр обновляется сразу при вводе названия, смене цвета или повтора
        previewDay.setText(String.valueOf(date.getDayOfMonth()));
        previewDay.setTextColor(ContextCompat.getColor(this,
                ProductionCalendarRepository.get(this).isDayOff(date) ? R.color.ds_weekend_text : R.color.ds_text_primary));
        previewTitle.setText(title().isEmpty() ? getString(R.string.event_untitled) : title());
        previewSubtitle.setText(EventType.USER.label + (yearly ? " · каждый год" : " · один раз"));
        applyDisplayedColor(displayedColor);
        reminderBlock.bind(reminderEnabled, reminderDays, reminderMinutes);
    }

    private void applyDisplayedColor(int c) {
        ImageViewCompat.setImageTintList(previewDayStar, ColorStateList.valueOf(c));
        ImageViewCompat.setImageTintList(previewPlate, ColorStateList.valueOf(c));
        GradientDrawable plate = new GradientDrawable();
        plate.setCornerRadius(14 * getResources().getDisplayMetrics().density);
        plate.setColor(TypeColors.plate(this, EventType.USER, c));
        previewPlate.setBackground(plate);
        GradientDrawable swatch = new GradientDrawable();
        swatch.setShape(GradientDrawable.OVAL);
        swatch.setColor(c);
        badgeSwatch.setBackground(swatch);
    }

    /** Цвет предпросмотра и капсулы с кодом перетекает за 150 мс (п. 9.2). */
    private void setColor(int newColor, boolean animate) {
        color = newColor;
        badgeHex.setText(hex(newColor));
        if (colorAnimator != null) colorAnimator.cancel();
        if (!animate) {
            displayedColor = newColor;
            applyDisplayedColor(newColor);
            return;
        }
        colorAnimator = ValueAnimator.ofArgb(displayedColor, newColor);
        colorAnimator.setDuration(150);
        colorAnimator.setInterpolator(Motion.STANDARD);
        colorAnimator.addUpdateListener(a -> {
            displayedColor = (int) a.getAnimatedValue();
            applyDisplayedColor(displayedColor);
        });
        colorAnimator.start();
    }

    private String snapshot() {
        return title() + "|" + descriptionInput.getText() + "|" + date + "|" + yearly + "|" + color + "|"
                + reminderEnabled + "|" + reminderDays + "|" + reminderMinutes;
    }

    private boolean isDirty() {
        return !snapshot().equals(initialState);
    }

    // endregion

    // region Цвет

    private void buildRecentColors() {
        recentContainer.removeAllViews();
        float density = getResources().getDisplayMetrics().density;
        for (int i = 0; i < recentColors.size(); i++) {
            final int index = i;
            int c = recentColors.get(i);
            FrameLayout swatch = new FrameLayout(this);
            swatch.setContentDescription(getString(R.string.cd_recent_color, hex(c)));
            swatch.setClickable(true);
            swatch.setFocusable(true);

            View ring = new View(this);
            GradientDrawable ringShape = new GradientDrawable();
            ringShape.setShape(GradientDrawable.OVAL);
            ringShape.setStroke(Math.round(2 * density), ContextCompat.getColor(this, R.color.ds_text_primary));
            ring.setBackground(ringShape);
            ring.setTag("ring");
            swatch.addView(ring, new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT));

            View dot = new View(this);
            GradientDrawable dotShape = new GradientDrawable();
            dotShape.setShape(GradientDrawable.OVAL);
            dotShape.setColor(c);
            dot.setBackground(dotShape);
            int dotSize = Math.round(36 * density);
            swatch.addView(dot, new FrameLayout.LayoutParams(dotSize, dotSize, Gravity.CENTER));

            swatch.setOnClickListener(v -> {
                recentIndex = index;
                setColor(recentColors.get(index), true);
                setSlidersFromColor(recentColors.get(index));
                renderRecentSelection(true);
            });
            int target = Math.round(48 * density);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(target, target);
            if (i > 0) lp.setMarginStart(Math.round(14 * density));
            recentContainer.addView(swatch, lp);
        }
        renderRecentSelection(false);
        setColor(color, false);
    }

    /** Выбранный цвет обведён кольцом 2 dp; кольцо проявляется за 150 мс. */
    private void renderRecentSelection(boolean animate) {
        for (int i = 0; i < recentContainer.getChildCount(); i++) {
            View swatch = recentContainer.getChildAt(i);
            View ring = swatch.findViewWithTag("ring");
            boolean selected = i == recentIndex;
            swatch.setSelected(selected);
            float target = selected ? 1f : 0f;
            if (animate) ring.animate().alpha(target).setDuration(150).setInterpolator(Motion.STANDARD).start();
            else ring.setAlpha(target);
        }
    }

    private void setSlidersFromColor(int c) {
        float[] hsl = new float[3];
        ColorUtils.colorToHSL(c, hsl);
        hueSlider.setValue(Math.round(hsl[0]) % 360);
        lightnessSlider.setValue(Math.round(hsl[2] * 100));
        updateSliderVisuals();
    }

    private void updateSliderVisuals() {
        int hue = hueSlider.getValue();
        int current = sliderColor();
        hueSlider.setThumbColor(current);
        lightnessSlider.setThumbColor(current);
        // Дорожка «Яркости» — от тёмного к светлому выбранного оттенка
        lightnessSlider.setTrackColors(new int[]{
                hsl(hue, LIGHTNESS_MIN), hsl(hue, (LIGHTNESS_MIN + LIGHTNESS_MAX) / 2), hsl(hue, LIGHTNESS_MAX)});
    }

    /** Насыщенность фиксирована — 65 %. */
    private int sliderColor() {
        return hsl(hueSlider.getValue(), lightnessSlider.getValue());
    }

    private static int hsl(int hue, int lightness) {
        return ColorUtils.HSLToColor(new float[]{hue, SATURATION, lightness / 100f});
    }

    private static String hex(int c) {
        return String.format(Locale.ROOT, "#%06X", 0xFFFFFF & c);
    }

    // endregion

    // region Напоминание

    /** Дата уведомления для текущих даты, повтора и упреждения. */
    private LocalDate notificationDate(int daysBefore, int minutes) {
        UserEvent probe = new UserEvent("probe", "", "", date, yearly, color, daysBefore, minutes);
        LocalDate eventDate = ReminderScheduler.nextUserEventDate(probe, LocalDateTime.now());
        return (eventDate != null ? eventDate : date).minusDays(daysBefore);
    }

    // endregion

    // region Сохранение, удаление, выход

    private void save() {
        if (title().isEmpty()) return;
        if (reminderEnabled && NotificationPermission.needsRequest(this) && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Разрешение запрашивается при первом включении напоминания; по умолчанию оно включено
            saveAfterPermission = true;
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
            return;
        }
        String id = editing != null ? editing.id : UUID.randomUUID().toString();
        UserEvent event = new UserEvent(id, title(), descriptionInput.getText().toString().trim(), date, yearly, color,
                reminderEnabled ? reminderDays : UserEvent.REMINDER_OFF, reminderMinutes);
        repository.save(event);
        ReminderScheduler.scheduleUserEvent(this, event);
        WidgetUpdater.updateAll(this);
        setResult(RESULT_OK, new Intent().putExtra(RESULT_DATE, date.toString()));
        if (interstitial != null) {
            saveButton.setEnabled(false);
            interstitial.showThen(this::finish);
        } else {
            finish();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (interstitial != null) interstitial.destroy();
    }

    private void confirmDelete() {
        if (editing == null) return;
        new AlertDialog.Builder(this)
                .setTitle(R.string.event_delete_confirm)
                .setNegativeButton(R.string.event_cancel, null)
                .setPositiveButton(R.string.event_delete_action, (dialog, which) -> {
                    repository.delete(editing.id);
                    ReminderScheduler.cancelUserEvent(this, editing.id);
                    WidgetUpdater.updateAll(this);
                    setResult(RESULT_OK, new Intent()
                            .putExtra(RESULT_DATE, editing.date.toString())
                            .putExtra(RESULT_DELETED, true));
                    finish();
                })
                .show();
    }

    /** Крестик и «Назад»: при несохранённых изменениях — диалог «Не сохранять событие?». */
    private void close() {
        if (!isDirty()) {
            finish();
            return;
        }
        new AlertDialog.Builder(this)
                .setTitle(R.string.event_discard_title)
                .setNegativeButton(R.string.event_discard_continue, null)
                .setPositiveButton(R.string.event_discard_action, (dialog, which) -> finish())
                .show();
    }

    // endregion
}

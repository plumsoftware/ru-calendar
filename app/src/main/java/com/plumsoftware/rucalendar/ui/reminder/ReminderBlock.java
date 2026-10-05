package com.plumsoftware.rucalendar.ui.reminder;

import android.content.Context;
import android.content.ContextWrapper;
import android.transition.ChangeBounds;
import android.transition.Fade;
import android.transition.TransitionManager;
import android.transition.TransitionSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.widget.SwitchCompat;
import androidx.fragment.app.FragmentActivity;

import com.google.android.material.timepicker.MaterialTimePicker;
import com.google.android.material.timepicker.TimeFormat;

import com.plumsoftware.rucalendar.R;
import com.plumsoftware.rucalendar.data.RuDates;
import com.plumsoftware.rucalendar.ui.Motion;

import java.time.LocalDate;

/**
 * Блок «Напоминание»: переключатель, три варианта упреждения, рассчитанная дата уведомления
 * и время (ТЗ п. 4.3). Разметка — view_reminder_block.xml.
 */
public final class ReminderBlock {

    public interface Callback {
        /** Включение требует системного разрешения; ответ передать в {@link #onPermissionResult(boolean)}. */
        void onRequestPermission();

        void onChanged(boolean enabled, int daysBefore, int minutes);
    }

    /** Дата, в которую придёт уведомление при данном упреждении. */
    public interface NotificationDate {
        LocalDate dateFor(int daysBefore, int minutes);
    }

    private static final int[] OPTION_DAYS = {0, 1, 7};

    private final Context context;
    private final ViewGroup root;
    private final SwitchCompat toggle;
    private final View details;
    private final TextView[] options;
    private final TextView dateView;
    private final TextView hintView;
    private final TextView timeView;
    private final TextView permissionHint;
    private final NotificationDate notificationDate;
    private final Callback callback;

    private boolean enabled;
    private int daysBefore;
    private int minutes;
    private boolean binding;

    public ReminderBlock(View block, NotificationDate notificationDate, Callback callback) {
        this.context = block.getContext();
        this.root = (ViewGroup) block;
        this.notificationDate = notificationDate;
        this.callback = callback;
        toggle = block.findViewById(R.id.reminder_switch);
        details = block.findViewById(R.id.reminder_details);
        options = new TextView[]{
                block.findViewById(R.id.reminder_option_same_day),
                block.findViewById(R.id.reminder_option_day_before),
                block.findViewById(R.id.reminder_option_week_before)};
        dateView = block.findViewById(R.id.reminder_date);
        hintView = block.findViewById(R.id.reminder_hint);
        timeView = block.findViewById(R.id.reminder_time);
        permissionHint = block.findViewById(R.id.reminder_permission_hint);

        toggle.setOnCheckedChangeListener((button, isChecked) -> {
            if (binding) return;
            if (isChecked && NotificationPermission.needsRequest(context)) {
                callback.onRequestPermission();
                return;
            }
            setEnabledInternal(isChecked);
        });
        for (int i = 0; i < options.length; i++) {
            final int days = OPTION_DAYS[i];
            options[i].setOnClickListener(v -> {
                daysBefore = days;
                render(false);
                notifyChanged();
            });
        }
        timeView.setOnClickListener(v -> showTimePicker());
        permissionHint.setOnClickListener(v -> NotificationPermission.openSettings(context));
    }

    /** Выбор времени в стиле приложения (цвета берутся из Theme.Calendar), 24-часовой формат. */
    private void showTimePicker() {
        FragmentActivity activity = findActivity(context);
        if (activity == null) return;
        MaterialTimePicker picker = new MaterialTimePicker.Builder()
                .setTimeFormat(TimeFormat.CLOCK_24H)
                .setHour(minutes / 60)
                .setMinute(minutes % 60)
                .setInputMode(MaterialTimePicker.INPUT_MODE_CLOCK)
                .setTitleText(R.string.reminder_time_title)
                .build();
        picker.addOnPositiveButtonClickListener(v -> {
            minutes = picker.getHour() * 60 + picker.getMinute();
            render(false);
            notifyChanged();
        });
        picker.show(activity.getSupportFragmentManager(), "reminder_time");
    }

    @Nullable
    private static FragmentActivity findActivity(Context context) {
        while (context instanceof ContextWrapper) {
            if (context instanceof FragmentActivity) return (FragmentActivity) context;
            context = ((ContextWrapper) context).getBaseContext();
        }
        return null;
    }

    /** Показать сохранённое состояние, не вызывая callback. */
    public void bind(boolean enabled, int daysBefore, int minutes) {
        this.enabled = enabled;
        this.daysBefore = daysBefore;
        this.minutes = minutes;
        binding = true;
        toggle.setChecked(enabled);
        binding = false;
        render(false);
    }

    public void onPermissionResult(boolean granted) {
        if (granted) {
            permissionHint.setVisibility(View.GONE);
            setEnabledInternal(true);
        } else {
            // При отказе переключатель возвращается в «выкл» и показывается подсказка
            binding = true;
            toggle.setChecked(false);
            binding = false;
            enabled = false;
            permissionHint.setVisibility(View.VISIBLE);
            render(true);
            notifyChanged();
        }
    }

    private void setEnabledInternal(boolean value) {
        enabled = value;
        render(true);
        notifyChanged();
    }

    private void notifyChanged() {
        callback.onChanged(enabled, daysBefore, minutes);
    }

    private void render(boolean animate) {
        if (animate) {
            // Варианты и время раскрываются по высоте и проявляются за 200 мс (п. 9.2)
            TransitionSet set = new TransitionSet()
                    .addTransition(new Fade())
                    .addTransition(new ChangeBounds())
                    .setOrdering(TransitionSet.ORDERING_TOGETHER)
                    .setDuration(200)
                    .setInterpolator(Motion.STANDARD);
            ViewGroup scene = root.getParent() instanceof ViewGroup ? (ViewGroup) root.getParent() : root;
            TransitionManager.beginDelayedTransition(scene, set);
        }
        details.setVisibility(enabled ? View.VISIBLE : View.GONE);
        for (int i = 0; i < options.length; i++) {
            boolean selected = OPTION_DAYS[i] == daysBefore;
            options[i].setSelected(selected);
            options[i].setTypeface(androidx.core.content.res.ResourcesCompat.getFont(context,
                    selected ? R.font.golos_text_semibold : R.font.golos_text_medium));
        }
        dateView.setText(RuDates.weekdayDayMonth(notificationDate.dateFor(daysBefore, minutes)));
        hintView.setText("Уведомление придёт " + RuDates.partOfDay(minutes));
        String time = RuDates.time(minutes);
        timeView.setText(time);
        timeView.setContentDescription(context.getString(R.string.cd_reminder_time, time));
    }
}

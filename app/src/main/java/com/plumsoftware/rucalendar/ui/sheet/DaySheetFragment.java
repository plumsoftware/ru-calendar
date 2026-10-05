package com.plumsoftware.rucalendar.ui.sheet;

import android.Manifest;
import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.Spanned;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.PagerSnapHelper;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.plumsoftware.rucalendar.R;
import com.plumsoftware.rucalendar.data.CalendarRepository;
import com.plumsoftware.rucalendar.data.DayEvent;
import com.plumsoftware.rucalendar.data.EventType;
import com.plumsoftware.rucalendar.data.ProductionCalendarRepository;
import com.plumsoftware.rucalendar.data.ReminderRepository;
import com.plumsoftware.rucalendar.data.RuDates;
import com.plumsoftware.rucalendar.reminders.ReminderScheduler;
import com.plumsoftware.rucalendar.ui.FontSpan;
import com.plumsoftware.rucalendar.ui.detail.HolidayActivity;
import com.plumsoftware.rucalendar.ui.reminder.NotificationPermission;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

/**
 * Карточка дня — нижний лист поверх затемнённого экрана (ТЗ п. 4.2). Закрывается свайпом вниз,
 * нажатием на фон, крестиком и системной кнопкой «Назад». Несколько событий дня листаются по горизонтали.
 */
public class DaySheetFragment extends BottomSheetDialogFragment {
    public static final String TAG = "day_sheet";

    /** Действия, которые выполняет хост. */
    public interface Host {
        void editUserEvent(DayEvent event);
    }

    private static final String ARG_DATE = "date";
    private static final String ARG_KEY = "key";

    private CalendarRepository repository;
    private ReminderRepository reminders;
    private LocalDate date;
    private List<DayEvent> events;
    private PageAdapter adapter;
    @Nullable
    private DayEvent pendingReminder;
    private ActivityResultLauncher<String> permissionLauncher;

    public static void show(FragmentManager fm, LocalDate date, @Nullable String eventKey) {
        androidx.fragment.app.Fragment existing = fm.findFragmentByTag(TAG);
        if (existing instanceof androidx.fragment.app.DialogFragment) {
            ((androidx.fragment.app.DialogFragment) existing).dismissAllowingStateLoss();
        }
        DaySheetFragment fragment = new DaySheetFragment();
        Bundle args = new Bundle();
        args.putString(ARG_DATE, date.toString());
        args.putString(ARG_KEY, eventKey);
        fragment.setArguments(args);
        fragment.show(fm, TAG);
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        repository = new CalendarRepository(context);
        reminders = new ReminderRepository(context);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        permissionLauncher = registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
            DayEvent event = pendingReminder;
            pendingReminder = null;
            if (event == null) return;
            if (granted) {
                enableDefaultReminder(event);
            } else {
                Toast.makeText(requireContext(), R.string.reminder_permission_denied, Toast.LENGTH_LONG).show();
            }
        });
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        BottomSheetDialog dialog = (BottomSheetDialog) super.onCreateDialog(savedInstanceState);
        BottomSheetBehavior<?> behavior = dialog.getBehavior();
        behavior.setSkipCollapsed(true);
        behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
        return dialog;
    }

    @Override
    public void onStart() {
        super.onStart();
        Dialog dialog = getDialog();
        if (dialog == null) return;
        Window window = dialog.getWindow();
        // Затемнение фона — чёрный 52 %
        if (window != null) window.setDimAmount(0.52f);
        View sheet = dialog.findViewById(com.google.android.material.R.id.design_bottom_sheet);
        if (sheet != null) sheet.setBackgroundColor(Color.TRANSPARENT);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.sheet_day, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        date = LocalDate.parse(requireArguments().getString(ARG_DATE));
        String key = requireArguments().getString(ARG_KEY);
        events = repository.eventsOn(date);
        if (events.isEmpty()) {
            dismissAllowingStateLoss();
            return;
        }
        int start = 0;
        for (int i = 0; i < events.size(); i++) {
            if (events.get(i).key().equals(key)) start = i;
        }

        final int basePadding = view.getPaddingBottom();
        ViewCompat.setOnApplyWindowInsetsListener(view, (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.navigationBars());
            v.setPadding(v.getPaddingLeft(), v.getPaddingTop(), v.getPaddingRight(), basePadding + bars.bottom);
            return insets;
        });

        RecyclerView pages = view.findViewById(R.id.pages);
        pages.setLayoutManager(new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false));
        adapter = new PageAdapter();
        pages.setAdapter(adapter);
        if (events.size() > 1) new PagerSnapHelper().attachToRecyclerView(pages);
        pages.scrollToPosition(start);
    }

    @Override
    public void onResume() {
        super.onResume();
        // Напоминание могли настроить на экране праздника
        if (adapter != null) adapter.notifyDataSetChanged();
    }

    private void onRemindClicked(DayEvent event) {
        if (event.holiday == null) return;
        if (reminders.get(event.holiday.id) != null) {
            // Уже включено — настроить на экране праздника
            HolidayActivity.start(requireContext(), event.holiday.id, date);
            return;
        }
        if (NotificationPermission.needsRequest(requireContext()) && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pendingReminder = event;
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
            return;
        }
        enableDefaultReminder(event);
    }

    /** «Напомнить» ставит напоминание с настройками по умолчанию: за день, 09:00. */
    private void enableDefaultReminder(DayEvent event) {
        if (event.holiday == null) return;
        ReminderRepository.Reminder reminder = new ReminderRepository.Reminder(event.holiday.id,
                ReminderRepository.DEFAULT_DAYS_BEFORE, ReminderRepository.DEFAULT_MINUTES);
        ReminderScheduler.enable(requireContext(), reminder);
        LocalDate eventDate = ReminderScheduler.nextEventDate(event.holiday, reminder, LocalDateTime.now());
        LocalDateTime trigger = ReminderScheduler.triggerTime(eventDate, reminder);
        Toast.makeText(requireContext(), "Напомню " + RuDates.dayMonth(trigger.toLocalDate()) + " в "
                + RuDates.time(reminder.minutes), Toast.LENGTH_SHORT).show();
        adapter.notifyDataSetChanged();
    }

    private final class PageAdapter extends RecyclerView.Adapter<PageHolder> {
        @NonNull
        @Override
        public PageHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View page = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_day_page, parent, false);
            return new PageHolder(page);
        }

        @Override
        public void onBindViewHolder(@NonNull PageHolder holder, int position) {
            holder.bind(events.get(position), position);
        }

        @Override
        public int getItemCount() {
            return events.size();
        }
    }

    private final class PageHolder extends RecyclerView.ViewHolder {
        private final View leaf;
        private final TextView leafMonth;
        private final TextView leafDay;
        private final TextView leafWeekday;
        private final TextView chipDayOff;
        private final TextView chipRelative;
        private final TextView title;
        private final TextView type;
        private final LinearLayout dots;
        private final TextView description;
        private final View shortDay;
        private final TextView shortDayNumber;
        private final TextView shortDayText;
        private final TextView more;
        private final TextView remind;

        PageHolder(View page) {
            super(page);
            leaf = page.findViewById(R.id.leaf);
            leafMonth = page.findViewById(R.id.leaf_month);
            leafDay = page.findViewById(R.id.leaf_day);
            leafWeekday = page.findViewById(R.id.leaf_weekday);
            chipDayOff = page.findViewById(R.id.chip_day_off);
            chipRelative = page.findViewById(R.id.chip_relative);
            title = page.findViewById(R.id.title);
            type = page.findViewById(R.id.type);
            dots = page.findViewById(R.id.dots);
            description = page.findViewById(R.id.description);
            shortDay = page.findViewById(R.id.short_day);
            shortDayNumber = page.findViewById(R.id.short_day_number);
            shortDayText = page.findViewById(R.id.short_day_text);
            more = page.findViewById(R.id.btn_more);
            remind = page.findViewById(R.id.btn_remind);
            leaf.setClipToOutline(true);
            page.findViewById(R.id.btn_close).setOnClickListener(v -> dismiss());
        }

        void bind(DayEvent event, int position) {
            Context context = itemView.getContext();
            ProductionCalendarRepository production = repository.production();

            int stripe = event.type == EventType.USER ? event.userColor : ContextCompat.getColor(context, event.type.solidColorRes);
            leafMonth.setBackgroundColor(stripe);
            leafMonth.setText(RuDates.monthName(date.getMonthValue()).toUpperCase(new Locale("ru")));
            leafDay.setText(String.valueOf(date.getDayOfMonth()));
            leafWeekday.setText(RuDates.weekdayLower(date));

            chipDayOff.setVisibility(production.isDayOff(date) ? View.VISIBLE : View.GONE);
            chipRelative.setText(RuDates.relativeLong(RuDates.daysBetween(LocalDate.now(), date)));
            title.setText(event.title);
            type.setText(event.subtitle());
            bindDots(position);

            String text = event.description();
            description.setText(text);
            description.setVisibility(text.isEmpty() ? View.GONE : View.VISIBLE);
            bindShortDay(production);

            if (event.holiday != null) {
                more.setText(R.string.sheet_more);
                more.setOnClickListener(v -> HolidayActivity.start(context, event.holiday.id, date));
                remind.setVisibility(View.VISIBLE);
                boolean on = reminders.get(event.holiday.id) != null;
                remind.setText(on ? R.string.sheet_reminder_on : R.string.sheet_remind);
                remind.setOnClickListener(v -> onRemindClicked(event));
            } else {
                // У собственного события вместо «Подробнее» — «Изменить»; напоминание настраивается в форме
                more.setText(R.string.sheet_edit);
                more.setOnClickListener(v -> {
                    dismiss();
                    ((Host) requireActivity()).editUserEvent(event);
                });
                remind.setVisibility(View.GONE);
            }
            if (events.size() > 1) {
                ViewCompat.setStateDescription(itemView, getString(R.string.cd_page, position + 1, events.size()));
            }
        }

        private void bindDots(int position) {
            dots.removeAllViews();
            if (events.size() < 2) {
                dots.setVisibility(View.GONE);
                return;
            }
            dots.setVisibility(View.VISIBLE);
            float density = getResources().getDisplayMetrics().density;
            int active = ContextCompat.getColor(requireContext(), R.color.ds_text_primary);
            int inactive = ContextCompat.getColor(requireContext(), R.color.ds_outline_dashed);
            for (int i = 0; i < events.size(); i++) {
                ImageView dot = new ImageView(requireContext());
                GradientDrawable shape = new GradientDrawable();
                shape.setShape(GradientDrawable.OVAL);
                shape.setColor(i == position ? active : inactive);
                int size = Math.round(6 * density);
                shape.setSize(size, size);
                dot.setImageDrawable(shape);
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(size, size);
                if (i > 0) lp.setMarginStart(Math.round(6 * density));
                dots.addView(dot, lp);
            }
        }

        /** Блок показывается, если предыдущий рабочий день сокращённый. */
        private void bindShortDay(ProductionCalendarRepository production) {
            LocalDate previous = date.minusDays(1);
            for (int i = 0; i < 14 && production.isDayOff(previous); i++) previous = previous.minusDays(1);
            if (production.kind(previous) != ProductionCalendarRepository.DayKind.SHORT) {
                shortDay.setVisibility(View.GONE);
                return;
            }
            shortDay.setVisibility(View.VISIBLE);
            shortDayNumber.setText(String.valueOf(previous.getDayOfMonth()));
            String on = RuDates.onWeekday(previous);
            String sentence = Character.toUpperCase(on.charAt(0)) + on.substring(1) + ", "
                    + RuDates.dayMonth(previous) + ", рабочий день на час короче.";
            String bold = getString(R.string.sheet_short_day_title);
            SpannableString span = new SpannableString(bold + " " + sentence);
            span.setSpan(new FontSpan(ResourcesCompat.getFont(requireContext(), R.font.golos_text_semibold)),
                    0, bold.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            shortDayText.setText(span);
        }
    }
}

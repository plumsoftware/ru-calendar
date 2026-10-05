package com.plumsoftware.rucalendar.ui.month;

import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.transition.ChangeBounds;
import android.transition.Fade;
import android.transition.TransitionManager;
import android.transition.TransitionSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.widget.TextViewCompat;
import androidx.fragment.app.Fragment;

import com.plumsoftware.rucalendar.R;
import com.plumsoftware.rucalendar.data.CalendarRepository;
import com.plumsoftware.rucalendar.data.DayEvent;
import com.plumsoftware.rucalendar.data.EventType;
import com.plumsoftware.rucalendar.data.HolidayRepository;
import com.plumsoftware.rucalendar.data.ProductionCalendarRepository;
import com.plumsoftware.rucalendar.data.RuDates;
import com.plumsoftware.rucalendar.ui.MarkerDrawable;
import com.plumsoftware.rucalendar.ui.Motion;
import com.plumsoftware.rucalendar.ui.TypeColors;
import com.plumsoftware.rucalendar.widget.WidgetUpdater;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

/** Экран «Месяц» (ТЗ п. 4.1). */
public class MonthFragment extends Fragment implements ProductionCalendarRepository.Listener {

    /** Переходы на экраны, которые открывает «Месяц». */
    public interface Host {
        void openDay(DayEvent event);

        void openSearch();

        void openNewEvent(LocalDate date);
    }

    private static final String STATE_MONTH = "month";
    private static final String STATE_SELECTED = "selected";

    private CalendarRepository repository;
    private LocalDate today;
    private LocalDate selected;

    private View contentView;
    private TextView yearView;
    private TextView monthTitleView;
    private View monthTitleBlock;
    private MonthGridView grid;
    private LinearLayout chipsContainer;
    private final List<FilterChipView> chips = new ArrayList<>();
    private ViewGroup dayCard;
    private TextView dayTitle;
    private TextView dayRelative;
    private LinearLayout dayEvents;
    private TextView dayEmpty;
    private View nearestView;
    private TextView nearestTitle;
    private TextView nearestCounter;
    private View fab;
    /** Дата из ссылки, пришедшей до создания вида фрагмента. */
    @Nullable
    private LocalDate pendingDate;

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        repository = new CalendarRepository(context);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_month, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        contentView = view.findViewById(R.id.content);
        yearView = view.findViewById(R.id.year);
        monthTitleView = view.findViewById(R.id.month_title);
        monthTitleBlock = view.findViewById(R.id.month_title_block);
        grid = view.findViewById(R.id.grid);
        chipsContainer = view.findViewById(R.id.chips);
        dayCard = view.findViewById(R.id.day_card);
        dayTitle = view.findViewById(R.id.day_title);
        dayRelative = view.findViewById(R.id.day_relative);
        dayEvents = view.findViewById(R.id.day_events);
        dayEmpty = view.findViewById(R.id.day_empty);
        nearestView = view.findViewById(R.id.nearest);
        nearestTitle = view.findViewById(R.id.nearest_title);
        nearestCounter = view.findViewById(R.id.nearest_counter);
        fab = view.findViewById(R.id.fab_add);

        today = LocalDate.now();
        YearMonth month = YearMonth.from(today);
        selected = today;
        if (savedInstanceState != null) {
            String m = savedInstanceState.getString(STATE_MONTH);
            String s = savedInstanceState.getString(STATE_SELECTED);
            if (m != null) month = YearMonth.parse(m);
            if (s != null) selected = LocalDate.parse(s);
        }
        if (pendingDate != null) {
            selected = pendingDate;
            month = YearMonth.from(pendingDate);
            pendingDate = null;
        }

        applyInsets(view);
        buildWeekdays(view.findViewById(R.id.weekdays));
        buildChips();

        grid.setModelProvider(repository::month);
        grid.setToday(today);
        grid.showMonth(month, false);
        grid.setSelected(selected, false);
        grid.setListener(new MonthGridView.Listener() {
            @Override
            public void onDaySelected(LocalDate date) {
                selected = date;
                renderDay(true);
            }

            @Override
            public void onMonthChanged(YearMonth newMonth) {
                onMonthShown(newMonth, true);
            }
        });

        view.findViewById(R.id.prev_month).setOnClickListener(v -> grid.showMonth(grid.getMonth().minusMonths(1), true));
        view.findViewById(R.id.next_month).setOnClickListener(v -> grid.showMonth(grid.getMonth().plusMonths(1), true));
        view.findViewById(R.id.search).setOnClickListener(v -> host().openSearch());
        // Нажатие на название месяца возвращает к сегодняшней дате
        monthTitleBlock.setOnClickListener(v -> goToToday());
        fab.setOnClickListener(v -> host().openNewEvent(selected));
        nearestView.setOnClickListener(v -> {
            HolidayRepository.Occurrence next = repository.holidays().nextNonWorking(today);
            if (next != null) host().openDay(DayEvent.of(next.date, next.holiday));
        });

        renderHeader(month, false);
        renderDay(false);
        renderNearest();
        repository.production().refresh(month.getYear());
    }

    @Override
    public void onStart() {
        super.onStart();
        repository.production().addListener(this);
    }

    @Override
    public void onResume() {
        super.onResume();
        // Дата могла смениться после полуночи; свои события — измениться на другом экране
        LocalDate now = LocalDate.now();
        if (!now.equals(today)) {
            today = now;
            grid.setToday(now);
            renderNearest();
        }
        grid.reloadData();
        renderDay(false);
    }

    @Override
    public void onStop() {
        super.onStop();
        repository.production().removeListener(this);
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        if (grid != null) outState.putString(STATE_MONTH, grid.getMonth().toString());
        if (selected != null) outState.putString(STATE_SELECTED, selected.toString());
    }

    @Override
    public void onYearUpdated(int year) {
        if (grid != null) grid.reloadData();
    }

    /** Как {@link #showDate}, но если вид ещё не создан — применить дату при создании. */
    public void showDateWhenReady(LocalDate date) {
        if (grid == null || getView() == null) pendingDate = date;
        else showDate(date);
    }

    /** Открыть месяц с выбранным днём, например после сохранения своего события или из «Года». */
    public void showDate(LocalDate date) {
        selected = date;
        grid.showMonth(YearMonth.from(date), true);
        grid.setSelected(date, true);
        renderHeader(YearMonth.from(date), true);
        renderDay(true);
    }

    /** После сохранения своего события звезда в ячейке дня появляется: масштаб 0 → 1,2 → 1 (п. 9.2). */
    public void celebrate(LocalDate date) {
        grid.reloadData();
        grid.popMarkers(date);
    }

    private void goToToday() {
        showDate(today);
    }

    private void onMonthShown(YearMonth month, boolean animate) {
        renderHeader(month, animate);
        repository.production().refresh(month.getYear());
        if (!YearMonth.from(selected).equals(month)) {
            // В новом месяце выбран сегодняшний день, если он там есть, иначе первое число
            selected = YearMonth.from(today).equals(month) ? today : month.atDay(1);
            grid.setSelected(selected, animate);
            renderDay(animate);
        }
    }

    private void applyInsets(View root) {
        final float density = getResources().getDisplayMetrics().density;
        final int top = contentView.getPaddingTop();
        ViewCompat.setOnApplyWindowInsetsListener(root, (v, windowInsets) -> {
            Insets bars = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());
            int navBar = getResources().getDimensionPixelSize(R.dimen.bottom_nav_height);
            contentView.setPadding(contentView.getPaddingLeft(), top + bars.top + Math.round(12 * density),
                    contentView.getPaddingRight(), bars.bottom + navBar + Math.round((56 + 8 + 16) * density));
            ViewGroup.MarginLayoutParams lp = (ViewGroup.MarginLayoutParams) fab.getLayoutParams();
            lp.bottomMargin = bars.bottom + navBar + Math.round(8 * density);
            fab.setLayoutParams(lp);
            return windowInsets;
        });
        ViewCompat.requestApplyInsets(root);
    }

    private void buildWeekdays(LinearLayout container) {
        String[] names = {"Пн", "Вт", "Ср", "Чт", "Пт", "Сб", "Вс"};
        int weekend = ContextCompat.getColor(requireContext(), R.color.ds_weekend_text);
        for (int i = 0; i < names.length; i++) {
            TextView tv = new TextView(requireContext());
            TextViewCompat.setTextAppearance(tv, R.style.TextAppearance_Calendar_Weekday);
            tv.setText(names[i]);
            tv.setGravity(android.view.Gravity.CENTER);
            if (i >= 5) tv.setTextColor(weekend);
            container.addView(tv, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        }
    }

    private void buildChips() {
        float density = getResources().getDisplayMetrics().density;
        for (EventType type : EventType.FILTER_ORDER) {
            FilterChipView chip = new FilterChipView(requireContext(), type);
            chip.setChecked(repository.filters().isEnabled(type));
            chip.setOnClickListener(v -> toggleFilter(chip));
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            if (!chips.isEmpty()) lp.setMarginStart(Math.round(8 * density));
            chipsContainer.addView(chip, lp);
            chips.add(chip);
        }
    }

    private void toggleFilter(FilterChipView chip) {
        boolean enabled = !chip.isChecked();
        repository.filters().setEnabled(chip.getType(), enabled);
        chip.setChecked(enabled);
        grid.reloadDataWithFade();
        WidgetUpdater.updateAll(requireContext());
        renderDay(true);
    }

    private void renderHeader(YearMonth month, boolean animate) {
        String year = String.valueOf(month.getYear());
        String name = RuDates.monthName(month.getMonthValue());
        monthTitleBlock.setContentDescription(getString(R.string.cd_back_to_today, name, month.getYear()));
        if (!animate || (name.contentEquals(monthTitleView.getText()) && year.contentEquals(yearView.getText()))) {
            yearView.setText(year);
            monthTitleView.setText(name);
            return;
        }
        // Название месяца меняется через затухание (п. 9.2)
        monthTitleView.animate().cancel();
        monthTitleView.animate().alpha(0f).setDuration(100).setInterpolator(Motion.STANDARD)
                .withEndAction(() -> {
                    yearView.setText(year);
                    monthTitleView.setText(name);
                    monthTitleView.animate().alpha(1f).setDuration(100).setInterpolator(Motion.STANDARD).start();
                }).start();
    }

    private void renderDay(boolean animate) {
        if (animate) {
            // Старое содержимое затухает, новое проявляется, высота меняется плавно (п. 9.2)
            TransitionSet set = new TransitionSet()
                    .addTransition(new Fade())
                    .addTransition(new ChangeBounds())
                    .setOrdering(TransitionSet.ORDERING_TOGETHER)
                    .setDuration(150)
                    .setInterpolator(Motion.STANDARD);
            TransitionManager.beginDelayedTransition((ViewGroup) contentView, set);
        }
        dayTitle.setText(RuDates.weekdayDayMonth(selected));
        dayRelative.setText(RuDates.relativeShort(RuDates.daysBetween(today, selected)));

        dayEvents.removeAllViews();
        List<DayEvent> events = repository.eventsOn(selected);
        LayoutInflater inflater = LayoutInflater.from(requireContext());
        float density = getResources().getDisplayMetrics().density;
        for (int i = 0; i < events.size(); i++) {
            DayEvent event = events.get(i);
            View row = inflater.inflate(R.layout.item_day_event, dayEvents, false);
            bindRow(row, event, density);
            if (i > 0) {
                ((ViewGroup.MarginLayoutParams) row.getLayoutParams()).topMargin = Math.round(12 * density);
            }
            dayEvents.addView(row);
        }
        dayEvents.setVisibility(events.isEmpty() ? View.GONE : View.VISIBLE);
        dayEmpty.setVisibility(events.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private void bindRow(View row, DayEvent event, float density) {
        Context context = requireContext();
        ImageView plate = row.findViewById(R.id.plate);
        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(14 * density);
        bg.setColor(TypeColors.plate(context, event.type, event.userColor));
        plate.setBackground(bg);
        int size;
        switch (event.type) {
            case USER:
                size = Math.round(16 * density);
                break;
            case MEMORIAL:
                size = Math.round(10 * density);
                break;
            default:
                size = Math.round(12 * density);
        }
        plate.setImageDrawable(new MarkerDrawable(event.type,
                TypeColors.markerColor(context, event.type, event.userColor), size, density));
        ((TextView) row.findViewById(R.id.title)).setText(event.title);
        ((TextView) row.findViewById(R.id.subtitle)).setText(event.subtitle());
        row.setOnClickListener(v -> host().openDay(event));
    }

    private void renderNearest() {
        HolidayRepository.Occurrence next = repository.holidays().nextNonWorking(today);
        if (next == null) {
            nearestView.setVisibility(View.GONE);
            return;
        }
        nearestView.setVisibility(View.VISIBLE);
        nearestTitle.setText(RuDates.dayMonth(next.date) + " · " + next.holiday.name);
        nearestCounter.setText(RuDates.days(RuDates.daysBetween(today, next.date)));
    }

    private Host host() {
        return (Host) requireActivity();
    }
}

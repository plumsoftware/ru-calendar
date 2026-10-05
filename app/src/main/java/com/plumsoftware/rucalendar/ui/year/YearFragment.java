package com.plumsoftware.rucalendar.ui.year;

import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;

import com.plumsoftware.rucalendar.R;
import com.plumsoftware.rucalendar.data.HolidayRepository;
import com.plumsoftware.rucalendar.data.ProductionCalendarRepository;
import com.plumsoftware.rucalendar.data.RuDates;
import com.plumsoftware.rucalendar.ui.FlowLayout;
import com.plumsoftware.rucalendar.ui.Motion;

import java.time.LocalDate;
import java.time.Year;
import java.time.YearMonth;

/** Год — производственный календарь (ТЗ п. 4.5). */
public class YearFragment extends Fragment implements ProductionCalendarRepository.Listener {

    public interface Host {
        void openMonth(YearMonth month);
    }

    private static final String STATE_YEAR = "year";

    private ProductionCalendarRepository production;
    private HolidayRepository holidays;
    private YearGridView grid;
    private TextView yearTitle;
    private View totals;
    private View notApproved;
    private TextView workValue;
    private TextView workLabel;
    private TextView offValue;
    private TextView hoursValue;
    private TextView hoursLabel;

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        production = ProductionCalendarRepository.get(context);
        holidays = HolidayRepository.get(context);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_year, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        grid = view.findViewById(R.id.year_grid);
        yearTitle = view.findViewById(R.id.year_title);
        totals = view.findViewById(R.id.totals);
        notApproved = view.findViewById(R.id.not_approved);
        workValue = view.findViewById(R.id.total_work_value);
        workLabel = view.findViewById(R.id.total_work_label);
        offValue = view.findViewById(R.id.total_off_value);
        hoursValue = view.findViewById(R.id.total_hours_value);
        hoursLabel = view.findViewById(R.id.total_hours_label);

        int year = savedInstanceState != null
                ? savedInstanceState.getInt(STATE_YEAR, LocalDate.now().getYear())
                : LocalDate.now().getYear();
        grid.setRepositories(production, holidays);
        grid.setToday(LocalDate.now());
        grid.setYear(year);
        grid.setListener(month -> ((Host) requireActivity()).openMonth(month));

        view.findViewById(R.id.prev_year).setOnClickListener(v -> changeYear(-1));
        view.findViewById(R.id.next_year).setOnClickListener(v -> changeYear(1));

        buildLegend(view.findViewById(R.id.legend));
        applyInsets(view);
        renderYear(false);
        production.refresh(year);
    }

    @Override
    public void onStart() {
        super.onStart();
        production.addListener(this);
    }

    @Override
    public void onStop() {
        super.onStop();
        production.removeListener(this);
    }

    @Override
    public void onResume() {
        super.onResume();
        grid.setToday(LocalDate.now());
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        if (grid != null) outState.putInt(STATE_YEAR, grid.getYear());
    }

    @Override
    public void onYearUpdated(int year) {
        if (grid != null && grid.getYear() == year) {
            grid.reload();
            renderYear(false);
        }
    }

    private void changeYear(int direction) {
        final int year = grid.getYear() + direction;
        production.refresh(year);
        float shift = grid.getWidth() * 0.5f;
        // Сетка месяцев сдвигается по горизонтали, числа итогов меняются через затухание — 200 мс (п. 9.2)
        grid.animate().cancel();
        grid.animate().translationX(-direction * shift).alpha(0f).setDuration(100).setInterpolator(Motion.STANDARD)
                .withEndAction(() -> {
                    grid.setYear(year);
                    renderYear(true);
                    grid.setTranslationX(direction * shift);
                    grid.animate().translationX(0f).alpha(1f).setDuration(100).setInterpolator(Motion.STANDARD).start();
                }).start();
    }

    private void renderYear(boolean animate) {
        int year = grid.getYear();
        yearTitle.setText(String.valueOf(year));
        yearTitle.setContentDescription(getString(R.string.year_overline) + " " + year);

        // Если календарь на год не получен с сервера и не лежит в кэше — переносы неизвестны
        boolean approved = production.hasData(year);
        totals.setVisibility(approved ? View.VISIBLE : View.GONE);
        notApproved.setVisibility(approved ? View.GONE : View.VISIBLE);
        if (!approved) return;

        // Итоги считаются из списков дней: часы = рабочие дни × 8 − число сокращённых дней (п. 6.2)
        int work = 0;
        int off = 0;
        int shortDays = 0;
        LocalDate d = LocalDate.of(year, 1, 1);
        for (int i = 0; i < Year.of(year).length(); i++, d = d.plusDays(1)) {
            ProductionCalendarRepository.DayKind kind = production.kind(d);
            if (kind == ProductionCalendarRepository.DayKind.DAY_OFF) {
                off++;
            } else {
                work++;
                if (kind == ProductionCalendarRepository.DayKind.SHORT) shortDays++;
            }
        }
        int hours = work * 8 - shortDays;
        final int fWork = work;
        final int fOff = off;
        Runnable apply = () -> {
            workValue.setText(String.valueOf(fWork));
            workLabel.setText(pluralWord(fWork, "рабочий день", "рабочих дня", "рабочих дней"));
            offValue.setText(String.valueOf(fOff));
            hoursValue.setText(String.valueOf(hours));
            hoursLabel.setText(pluralWord(hours, "час", "часа", "часов") + " при 40-часовой неделе");
        };
        if (!animate) {
            apply.run();
            return;
        }
        totals.animate().cancel();
        totals.animate().alpha(0f).setDuration(100).setInterpolator(Motion.STANDARD).withEndAction(() -> {
            apply.run();
            totals.animate().alpha(1f).setDuration(100).setInterpolator(Motion.STANDARD).start();
        }).start();
    }

    /** Только слово после числа: «рабочих дней». */
    private static String pluralWord(int n, String one, String few, String many) {
        String full = RuDates.plural(n, one, few, many);
        return full.substring(full.indexOf(' ') + 1);
    }

    private void buildLegend(FlowLayout legend) {
        float density = getResources().getDisplayMetrics().density;
        Context context = requireContext();
        int text = ContextCompat.getColor(context, R.color.ds_text_body);

        legend.addView(legendItem(square(ContextCompat.getColor(context, R.color.ds_holiday_plate), 0), null,
                getString(R.string.legend_holiday), text));
        TextView twelve = new TextView(context);
        twelve.setText("12");
        twelve.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        twelve.setTypeface(ResourcesCompat.getFont(context, R.font.golos_text_bold));
        twelve.setTextColor(ContextCompat.getColor(context, R.color.ds_weekend_text));
        legend.addView(legendItem(null, twelve, getString(R.string.legend_day_off), text));
        legend.addView(legendItem(square(0, ContextCompat.getColor(context, R.color.ds_text_primary)), null,
                getString(R.string.legend_short), text));
        legend.addView(legendItem(square(0, ContextCompat.getColor(context, R.color.ds_accent)), null,
                getString(R.string.legend_today), text));
        // Отступ между элементами легенды — 16 dp по горизонтали
        for (int i = 0; i < legend.getChildCount(); i++) {
            View child = legend.getChildAt(i);
            child.setPadding(0, 0, Math.round(10 * density), 0);
        }
    }

    private GradientDrawable square(int fill, int stroke) {
        float density = getResources().getDisplayMetrics().density;
        GradientDrawable d = new GradientDrawable();
        d.setCornerRadius(5 * density);
        int size = Math.round(14 * density);
        d.setSize(size, size);
        d.setColor(fill);
        if (stroke != 0) d.setStroke(Math.round(1.5f * density), stroke);
        return d;
    }

    private View legendItem(@Nullable GradientDrawable icon, @Nullable TextView iconText, String label, int textColor) {
        Context context = requireContext();
        float density = getResources().getDisplayMetrics().density;
        LinearLayout item = new LinearLayout(context);
        item.setOrientation(LinearLayout.HORIZONTAL);
        item.setGravity(Gravity.CENTER_VERTICAL);
        if (icon != null) {
            View swatch = new View(context);
            swatch.setBackground(icon);
            int size = Math.round(14 * density);
            item.addView(swatch, new LinearLayout.LayoutParams(size, size));
        } else if (iconText != null) {
            item.addView(iconText);
        }
        TextView tv = new TextView(context);
        tv.setText(label);
        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        tv.setTypeface(ResourcesCompat.getFont(context, R.font.golos_text_regular));
        tv.setTextColor(textColor);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMarginStart(Math.round(6 * density));
        item.addView(tv, lp);
        return item;
    }

    private void applyInsets(View root) {
        View content = root.findViewById(R.id.year_content);
        final float density = getResources().getDisplayMetrics().density;
        final int navBar = getResources().getDimensionPixelSize(R.dimen.bottom_nav_height);
        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            content.setPadding(content.getPaddingLeft(), bars.top + Math.round(12 * density),
                    content.getPaddingRight(), bars.bottom + navBar + Math.round(16 * density));
            return insets;
        });
        ViewCompat.requestApplyInsets(root);
    }
}

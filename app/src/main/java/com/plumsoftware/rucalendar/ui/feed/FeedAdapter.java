package com.plumsoftware.rucalendar.ui.feed;

import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.plumsoftware.rucalendar.R;
import com.plumsoftware.rucalendar.data.DayEvent;
import com.plumsoftware.rucalendar.data.EventType;
import com.plumsoftware.rucalendar.data.ProductionCalendarRepository;
import com.plumsoftware.rucalendar.data.RuDates;
import com.plumsoftware.rucalendar.ui.MarkerDrawable;
import com.plumsoftware.rucalendar.ui.TypeColors;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Список «Ленты»: заголовки месяцев и строки событий. */
final class FeedAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    interface OnEventClick {
        void onClick(DayEvent event);
    }

    private static final int TYPE_HEADER = 0;
    private static final int TYPE_EVENT = 1;

    private final List<Object> items = new ArrayList<>();
    private final ProductionCalendarRepository production;
    private final OnEventClick onClick;
    private LocalDate today = LocalDate.now();

    FeedAdapter(ProductionCalendarRepository production, OnEventClick onClick) {
        this.production = production;
        this.onClick = onClick;
    }

    /** События в хронологическом порядке; заголовок группы — название месяца прописными (п. 4.4). */
    void submit(List<DayEvent> events, LocalDate today) {
        this.today = today;
        items.clear();
        YearMonth current = null;
        for (DayEvent event : events) {
            YearMonth month = YearMonth.from(event.date);
            if (!month.equals(current)) {
                current = month;
                String title = RuDates.monthName(month.getMonthValue());
                if (month.getYear() != today.getYear()) title += " " + month.getYear();
                items.add(title.toUpperCase(new Locale("ru")));
            }
            items.add(event);
        }
        notifyDataSetChanged();
    }

    @Override
    public int getItemViewType(int position) {
        return items.get(position) instanceof DayEvent ? TYPE_EVENT : TYPE_HEADER;
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (viewType == TYPE_HEADER) {
            return new RecyclerView.ViewHolder(inflater.inflate(R.layout.item_feed_header, parent, false)) {
            };
        }
        return new EventHolder(inflater.inflate(R.layout.item_feed_event, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        Object item = items.get(position);
        if (holder instanceof EventHolder) ((EventHolder) holder).bind((DayEvent) item);
        else ((TextView) holder.itemView).setText((String) item);
    }

    private final class EventHolder extends RecyclerView.ViewHolder {
        private final View tile;
        private final TextView tileDay;
        private final TextView tileWeekday;
        private final TextView title;
        private final ImageView marker;
        private final TextView type;
        private final TextView relative;

        EventHolder(View view) {
            super(view);
            tile = view.findViewById(R.id.tile);
            tileDay = view.findViewById(R.id.tile_day);
            tileWeekday = view.findViewById(R.id.tile_weekday);
            title = view.findViewById(R.id.title);
            marker = view.findViewById(R.id.marker);
            type = view.findViewById(R.id.type);
            relative = view.findViewById(R.id.relative);
        }

        void bind(DayEvent event) {
            Context context = itemView.getContext();
            float density = context.getResources().getDisplayMetrics().density;
            boolean isToday = event.date.equals(today);
            boolean official = event.type == EventType.OFFICIAL;
            boolean dayOff = production.isDayOff(event.date);

            // Плитка: сегодня — синяя рамка; официальный — красная заливка; остальные — серая
            GradientDrawable bg = new GradientDrawable();
            bg.setCornerRadius(16 * density);
            if (official) bg.setColor(ContextCompat.getColor(context, R.color.ds_holiday_plate));
            else if (!isToday) bg.setColor(ContextCompat.getColor(context, R.color.ds_bg_screen));
            if (isToday) bg.setStroke(Math.round(2 * density), ContextCompat.getColor(context, R.color.ds_accent));
            tile.setBackground(bg);

            int dayColor = official ? ContextCompat.getColor(context, R.color.ds_on_holiday_plate)
                    : ContextCompat.getColor(context, R.color.ds_text_primary);
            int weekdayColor = official ? dayColor
                    : ContextCompat.getColor(context, dayOff ? R.color.ds_weekend_text : R.color.ds_text_secondary);
            tileDay.setText(String.valueOf(event.date.getDayOfMonth()));
            tileDay.setTextColor(dayColor);
            tileWeekday.setText(RuDates.weekdayShort(event.date));
            tileWeekday.setTextColor(weekdayColor);

            title.setText(event.title);
            int markerSize = Math.round((event.type == EventType.USER ? 10 : 8) * density);
            marker.setImageDrawable(new MarkerDrawable(event.type,
                    TypeColors.markerColor(context, event.type, event.userColor), markerSize, density));
            type.setText(shortType(event));

            long days = RuDates.daysBetween(today, event.date);
            relative.setText(relativeText(days));
            if (days == 0) {
                // «сегодня» — синий чип
                GradientDrawable chip = new GradientDrawable();
                chip.setCornerRadius(10 * density);
                chip.setColor(ContextCompat.getColor(context, R.color.ds_rel_chip_bg));
                relative.setBackground(chip);
                int ph = Math.round(10 * density);
                int pv = Math.round(4 * density);
                relative.setPadding(ph, pv, ph, pv);
                relative.setTextColor(ContextCompat.getColor(context, R.color.ds_rel_chip_text));
            } else {
                relative.setBackground(null);
                relative.setPadding(0, 0, 0, 0);
                relative.setTextColor(ContextCompat.getColor(context, R.color.ds_text_secondary));
            }

            itemView.setContentDescription(RuDates.dayMonthWeekday(event.date) + ". " + event.title + ". "
                    + shortType(event) + ". " + relativeText(days));
            itemView.setOnClickListener(v -> onClick.onClick(event));
        }
    }

    private static String shortType(DayEvent event) {
        switch (event.type) {
            case OFFICIAL:
                return event.holiday != null && event.holiday.nonWorking ? "Официальный · выходной" : "Официальный";
            case PROFESSIONAL:
                return "Профессиональный";
            case UNOFFICIAL:
                return "Неофициальный";
            case MEMORIAL:
                return "Памятная дата";
            default:
                return event.subtitle();
        }
    }

    /** «сегодня», «завтра», далее «N дней» со склонением (п. 4.4). */
    private static String relativeText(long days) {
        if (days == 0) return "сегодня";
        if (days == 1) return "завтра";
        if (days > 1) return RuDates.days(days);
        return "прошло";
    }
}

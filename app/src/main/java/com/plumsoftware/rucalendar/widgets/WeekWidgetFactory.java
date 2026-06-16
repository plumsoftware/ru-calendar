package com.plumsoftware.rucalendar.widgets;

import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;

import com.plumsoftware.rucalendar.R;

import java.util.List;

class WeekWidgetFactory implements RemoteViewsService.RemoteViewsFactory {

    private final Context context;
    private List<WidgetEvent> events;

    WeekWidgetFactory(Context context, Intent intent) {
        this.context = context.getApplicationContext();
    }

    @Override
    public void onCreate() {
    }

    @Override
    public void onDataSetChanged() {
        events = WidgetCelebrationRepository.getUpcomingEvents();
    }

    @Override
    public void onDestroy() {
        events = null;
    }

    @Override
    public int getCount() {
        return events == null ? 0 : events.size();
    }

    @Override
    public RemoteViews getViewAt(int position) {
        if (events == null || position >= events.size()) {
            return null;
        }
        WidgetEvent event = events.get(position);
        RemoteViews row = new RemoteViews(context.getPackageName(), R.layout.widget_week_event_item);

        row.setTextViewText(R.id.widget_week_event_day, String.valueOf(event.getDayOfMonth()));
        row.setTextViewText(R.id.widget_week_event_name, event.getName());
        row.setImageViewResource(
                R.id.widget_week_event_dot,
                WidgetDotDrawables.smallDrawableForType(event.getType())
        );

        return row;
    }

    @Override
    public RemoteViews getLoadingView() {
        return null;
    }

    @Override
    public int getViewTypeCount() {
        return 1;
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public boolean hasStableIds() {
        return true;
    }
}

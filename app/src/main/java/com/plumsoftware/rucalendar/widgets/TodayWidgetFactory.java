package com.plumsoftware.rucalendar.widgets;

import android.content.Context;
import android.content.Intent;
import android.view.View;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;

import com.plumsoftware.rucalendar.R;

import java.util.List;

class TodayWidgetFactory implements RemoteViewsService.RemoteViewsFactory {

    private final Context context;
    private List<WidgetEvent> events;

    TodayWidgetFactory(Context context, Intent intent) {
        this.context = context.getApplicationContext();
    }

    @Override
    public void onCreate() {
    }

    @Override
    public void onDataSetChanged() {
        events = WidgetCelebrationRepository.getTodayEventsForList();
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
        RemoteViews row = new RemoteViews(context.getPackageName(), R.layout.widget_today_item);

        row.setTextViewText(R.id.widget_today_item_name, event.getName());
        row.setTextViewText(
                R.id.widget_today_item_type,
                WidgetCelebrationRepository.typeLabel(context, event.getType())
        );
        row.setImageViewResource(
                R.id.widget_today_item_dot,
                WidgetDotDrawables.drawableForType(event.getType())
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

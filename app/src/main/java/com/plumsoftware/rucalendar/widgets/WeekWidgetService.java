package com.plumsoftware.rucalendar.widgets;

import android.content.Intent;
import android.widget.RemoteViewsService;

public class WeekWidgetService extends RemoteViewsService {

    @Override
    public RemoteViewsFactory onGetViewFactory(Intent intent) {
        return new WeekWidgetFactory(getApplicationContext(), intent);
    }
}

package com.plumsoftware.rucalendar.ui;

import android.content.Context;
import android.graphics.Color;

import androidx.core.content.ContextCompat;
import androidx.core.graphics.ColorUtils;

import com.plumsoftware.rucalendar.data.EventType;

/** Цвета типов событий для текущей темы. */
public final class TypeColors {
    private TypeColors() {
    }

    /** Цвет маркера по ordinal типа. */
    public static int[] colors(Context context) {
        EventType[] types = EventType.values();
        int[] result = new int[types.length];
        for (EventType type : types) result[type.ordinal()] = ContextCompat.getColor(context, type.colorRes);
        return result;
    }

    public static int color(Context context, EventType type) {
        return ContextCompat.getColor(context, type.colorRes);
    }

    /** Фон плашки; для своего события — его цвет с непрозрачностью 14 % поверх карточки (п. 2.1). */
    public static int plate(Context context, EventType type, int userColor) {
        if (type == EventType.USER) {
            int surface = ContextCompat.getColor(context, com.plumsoftware.rucalendar.R.color.ds_surface);
            return ColorUtils.compositeColors(ColorUtils.setAlphaComponent(userColor, Math.round(255 * 0.14f)), surface);
        }
        return ContextCompat.getColor(context, type.plateColorRes);
    }

    public static int markerColor(Context context, EventType type, int userColor) {
        return type == EventType.USER ? (userColor != 0 ? userColor : Color.parseColor("#7A4DD8")) : color(context, type);
    }
}

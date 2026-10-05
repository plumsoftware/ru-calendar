package com.plumsoftware.rucalendar.ui;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;

import com.plumsoftware.rucalendar.data.EventType;
import com.plumsoftware.rucalendar.data.MonthModel;

import java.util.List;

/** Маркеры типов в виде картинок — для уведомлений и виджетов, где нельзя рисовать своим View. */
public final class MarkerBitmaps {
    private MarkerBitmaps() {
    }

    /** Плашка sizeDp × sizeDp со скруглением 14/40 и маркером типа по центру (как в списке дня). */
    public static Bitmap plate(Context context, EventType type, int userColor, int sizeDp) {
        float density = context.getResources().getDisplayMetrics().density;
        int size = Math.round(sizeDp * density);
        Bitmap bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setColor(TypeColors.plate(context, type, userColor));
        float radius = size * 14f / 40f;
        canvas.drawRoundRect(new RectF(0, 0, size, size), radius, radius, paint);
        paint.setColor(TypeColors.markerColor(context, type, userColor));
        float marker = size * (type == EventType.USER ? 16f : type == EventType.MEMORIAL ? 10f : 12f) / 40f;
        MarkerDrawable.draw(canvas, paint, type, size / 2f, size / 2f, marker, density);
        return bitmap;
    }

    /** Одиночный маркер: 8 dp, звезда — 10 dp. */
    public static Bitmap marker(Context context, EventType type, int userColor) {
        float density = context.getResources().getDisplayMetrics().density;
        int size = Math.round(10 * density);
        Bitmap bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setColor(TypeColors.markerColor(context, type, userColor));
        float markerSize = (type == EventType.USER ? 10 : 8) * density;
        MarkerDrawable.draw(new Canvas(bitmap), paint, type, size / 2f, size / 2f, markerSize, density);
        return bitmap;
    }

    /** Ряд маркеров под числом дня (7 dp, звезда 10 dp, отступ 3 dp); ширина — 40 dp, высота — 10 dp. */
    public static Bitmap row(Context context, List<MonthModel.Marker> markers, int[] typeColors) {
        float density = context.getResources().getDisplayMetrics().density;
        int width = Math.round(40 * density);
        int height = Math.round(10 * density);
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        if (markers.isEmpty()) return bitmap;
        Canvas canvas = new Canvas(bitmap);
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        float gap = 3 * density;
        float total = -gap;
        for (MonthModel.Marker m : markers) total += size(m, density) + gap;
        float x = (width - total) / 2f;
        for (MonthModel.Marker m : markers) {
            float s = size(m, density);
            paint.setColor(m.type == EventType.USER ? m.userColor : typeColors[m.type.ordinal()]);
            MarkerDrawable.draw(canvas, paint, m.type, x + s / 2f, height / 2f, s, density);
            x += s + gap;
        }
        return bitmap;
    }

    private static float size(MonthModel.Marker m, float density) {
        return (m.type == EventType.USER ? 10 : 7) * density;
    }
}

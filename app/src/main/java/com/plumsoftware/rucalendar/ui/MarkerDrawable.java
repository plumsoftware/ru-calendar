package com.plumsoftware.rucalendar.ui;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.plumsoftware.rucalendar.data.EventType;

/**
 * Маркер типа события (ТЗ п. 2.1): официальный — квадрат со скруглением, профессиональный — круг,
 * неофициальный — кольцо, памятная дата — ромб, своё событие — пятиконечная звезда.
 */
public final class MarkerDrawable extends Drawable {
    private static final Path STAR_24 = new Path();

    static {
        // ic_star_filled на сетке 24 × 24
        float[] p = {12, 2, 14.9f, 8.6f, 22, 9.3f, 16.6f, 14.1f, 18.2f, 21.1f, 12, 17.4f,
                5.8f, 21.1f, 7.4f, 14.1f, 2, 9.3f, 9.1f, 8.6f};
        STAR_24.moveTo(p[0], p[1]);
        for (int i = 2; i < p.length; i += 2) STAR_24.lineTo(p[i], p[i + 1]);
        STAR_24.close();
    }

    // Переиспользуются в onDraw, рисование идёт только в UI-потоке
    private static final RectF RECT = new RectF();
    private static final Path STAR = new Path();
    private static final Matrix MATRIX = new Matrix();

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private int alpha = 255;
    private final EventType type;
    private final int color;
    private final int sizePx;
    private final float density;

    public MarkerDrawable(EventType type, int color, int sizePx, float density) {
        this.type = type;
        this.color = color;
        this.sizePx = sizePx;
        this.density = density;
    }

    @Override
    public void draw(@NonNull Canvas canvas) {
        paint.setColor(color);
        paint.setAlpha(Color.alpha(color) * alpha / 255);
        draw(canvas, paint, type, getBounds().exactCenterX(), getBounds().exactCenterY(), sizePx, density);
    }

    /**
     * Рисует маркер с центром в (cx, cy). size — номинальный размер маркера (7 dp в сетке, 8 dp в списках,
     * 12 dp в плашке; для звезды — её размер). Цвет и прозрачность берутся из paint.
     */
    public static void draw(Canvas canvas, Paint paint, EventType type, float cx, float cy, float size, float density) {
        float half = size / 2f;
        switch (type) {
            case OFFICIAL: {
                paint.setStyle(Paint.Style.FILL);
                float r = Math.max(2 * density, size * 0.28f);
                RECT.set(cx - half, cy - half, cx + half, cy + half);
                canvas.drawRoundRect(RECT, r, r, paint);
                break;
            }
            case PROFESSIONAL:
                paint.setStyle(Paint.Style.FILL);
                canvas.drawCircle(cx, cy, half, paint);
                break;
            case UNOFFICIAL: {
                float stroke = Math.max(2 * density, size * 0.25f);
                paint.setStyle(Paint.Style.STROKE);
                paint.setStrokeWidth(stroke);
                canvas.drawCircle(cx, cy, half - stroke / 2f, paint);
                paint.setStyle(Paint.Style.FILL);
                break;
            }
            case MEMORIAL: {
                // Квадрат, повёрнутый на 45°; сторона подобрана, чтобы ромб визуально совпадал по весу с кругом
                float side = size * 0.85f;
                paint.setStyle(Paint.Style.FILL);
                canvas.save();
                canvas.rotate(45, cx, cy);
                canvas.drawRect(cx - side / 2f, cy - side / 2f, cx + side / 2f, cy + side / 2f, paint);
                canvas.restore();
                break;
            }
            case USER: {
                paint.setStyle(Paint.Style.FILL);
                float scale = size / 24f;
                MATRIX.setScale(scale, scale);
                MATRIX.postTranslate(cx - half, cy - half);
                STAR_24.transform(MATRIX, STAR);
                canvas.drawPath(STAR, paint);
                break;
            }
        }
    }

    @Override
    public int getIntrinsicWidth() {
        return sizePx;
    }

    @Override
    public int getIntrinsicHeight() {
        return sizePx;
    }

    @Override
    public void setAlpha(int alpha) {
        this.alpha = alpha;
        invalidateSelf();
    }

    @Override
    public void setColorFilter(@Nullable ColorFilter colorFilter) {
        paint.setColorFilter(colorFilter);
        invalidateSelf();
    }

    @Override
    public int getOpacity() {
        return PixelFormat.TRANSLUCENT;
    }
}

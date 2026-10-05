package com.plumsoftware.rucalendar.ui;

import android.graphics.Typeface;
import android.text.TextPaint;
import android.text.style.MetricAffectingSpan;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/** Шрифт для части текста; TypefaceSpan(Typeface) доступен только с API 28. */
public final class FontSpan extends MetricAffectingSpan {
    @Nullable
    private final Typeface typeface;

    public FontSpan(@Nullable Typeface typeface) {
        this.typeface = typeface;
    }

    @Override
    public void updateDrawState(@NonNull TextPaint paint) {
        if (typeface != null) paint.setTypeface(typeface);
    }

    @Override
    public void updateMeasureState(@NonNull TextPaint paint) {
        if (typeface != null) paint.setTypeface(typeface);
    }
}

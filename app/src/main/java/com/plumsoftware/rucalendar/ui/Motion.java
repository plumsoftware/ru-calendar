package com.plumsoftware.rucalendar.ui;

import android.view.animation.Interpolator;

import androidx.core.view.animation.PathInterpolatorCompat;

/** Кривые анимаций, ТЗ п. 9.1. */
public final class Motion {
    /** Элемент появляется на экране. */
    public static final Interpolator DECELERATE = PathInterpolatorCompat.create(0f, 0f, 0f, 1f);
    /** Элемент уходит с экрана. */
    public static final Interpolator ACCELERATE = PathInterpolatorCompat.create(0.3f, 0f, 1f, 1f);
    /** Элемент меняется или перемещается, оставаясь на экране. */
    public static final Interpolator STANDARD = PathInterpolatorCompat.create(0.2f, 0f, 0f, 1f);

    private Motion() {
    }
}

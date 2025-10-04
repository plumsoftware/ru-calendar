package com.plumsoftware.rucalendar.repositories;

import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;

public class SwipeGestureListener extends GestureDetector.SimpleOnGestureListener {
    private static final int SWIPE_THRESHOLD = 100;
    private static final int SWIPE_VELOCITY_THRESHOLD = 100;

    @Override
    public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
        try {
            float diffY = e2.getY() - e1.getY();
            float diffX = e2.getX() - e1.getX();

            if (Math.abs(diffX) > Math.abs(diffY)) {
                // Горизонтальный свайп
                if (Math.abs(diffX) > SWIPE_THRESHOLD && Math.abs(velocityX) > SWIPE_VELOCITY_THRESHOLD) {
                    if (diffX > 0) {
                        onSwipeRight();
                    } else {
                        onSwipeLeft();
                    }
                    return true;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    public void onSwipeRight() {
        // Действие при свайпе вправо
    }

    public void onSwipeLeft() {
        // Действие при свайпе влево
    }
}

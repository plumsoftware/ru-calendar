package com.plumsoftware.rucalendar.ui.month;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.os.Bundle;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.animation.Interpolator;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat;
import androidx.customview.widget.ExploreByTouchHelper;

import com.plumsoftware.rucalendar.R;
import com.plumsoftware.rucalendar.data.EventType;
import com.plumsoftware.rucalendar.data.MonthModel;
import com.plumsoftware.rucalendar.ui.MarkerDrawable;
import com.plumsoftware.rucalendar.ui.Motion;
import com.plumsoftware.rucalendar.ui.TypeColors;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Сетка месяца (ТЗ п. 4.1): неделя с понедельника, 5–6 строк, маркеры типов под числом,
 * рамка «сегодня», подложка выбранного дня. Месяц листается горизонтальным свайпом:
 * сетка следует за пальцем и доводится до соседнего месяца при сдвиге больше 30 % ширины (п. 9.2).
 */
public class MonthGridView extends View {

    public interface Listener {
        void onDaySelected(LocalDate date);

        /** Месяц сменился свайпом или программно. */
        void onMonthChanged(YearMonth month);
    }

    public interface ModelProvider {
        MonthModel get(YearMonth month);
    }

    private static final float SWIPE_THRESHOLD = 0.3f;

    private final float density;
    private final float cellHeight;
    private final float rowGap;
    private final float numberBox;
    private final float numberTop;
    private final float markerCenterY;
    private final float markerGap;
    private final float markerSize;
    private final float starSize;
    private final float todayStroke;
    private final float todayRadius;
    private final float selectionRadius;
    private final float selectionInset;

    private final Paint fillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint strokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint numberPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint markerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Typeface numberTypeface;
    private final Typeface outsideTypeface;
    private final RectF rect = new RectF();

    private final int colorText;
    private final int colorWeekend;
    private final int colorOutside;
    private final int colorAccent;
    private final int colorSelected;
    private final int colorHolidayPlate;
    private final int colorOnHolidayPlate;
    private final int[] typeColors;

    private final int touchSlop;
    private final int minFlingVelocity;
    private final Helper accessibilityHelper;

    private YearMonth month = YearMonth.now();
    private LocalDate today = LocalDate.now();
    @Nullable
    private LocalDate selected;
    @Nullable
    private ModelProvider provider;
    @Nullable
    private Listener listener;

    private final Map<YearMonth, MonthModel> models = new HashMap<>();
    /** Модели до смены фильтров: маркеры затухают, новые проявляются (п. 9.2, «Фильтры»). */
    private Map<YearMonth, MonthModel> previousModels;
    private float markerFade = 1f;

    @Nullable
    private LocalDate popDate;
    private float popScale = 1f;
    @Nullable
    private ValueAnimator popAnimator;

    private float offset;
    private float selectionProgress = 1f;
    private int heightPx;

    private float downX;
    private float downY;
    private boolean dragging;
    private boolean tapCandidate;
    @Nullable
    private VelocityTracker velocityTracker;
    @Nullable
    private ValueAnimator pageAnimator;
    @Nullable
    private ValueAnimator heightAnimator;
    @Nullable
    private ValueAnimator selectionAnimator;
    @Nullable
    private ValueAnimator fadeAnimator;

    public MonthGridView(Context context) {
        this(context, null);
    }

    public MonthGridView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        density = getResources().getDisplayMetrics().density;
        cellHeight = 54 * density;
        rowGap = 2 * density;
        numberBox = 34 * density;
        numberTop = 4 * density;
        markerSize = 7 * density;
        starSize = 10 * density;
        markerGap = 3 * density;
        markerCenterY = numberTop + numberBox + 3 * density + 4 * density;
        todayStroke = 2 * density;
        todayRadius = 12 * density;
        selectionRadius = 16 * density;
        selectionInset = 3 * density;

        colorText = ContextCompat.getColor(context, R.color.ds_text_primary);
        colorWeekend = ContextCompat.getColor(context, R.color.ds_weekend_text);
        colorOutside = ContextCompat.getColor(context, R.color.ds_outside_text);
        colorAccent = ContextCompat.getColor(context, R.color.ds_accent);
        colorSelected = ContextCompat.getColor(context, R.color.ds_day_selected_bg);
        colorHolidayPlate = ContextCompat.getColor(context, R.color.ds_holiday_plate);
        colorOnHolidayPlate = ContextCompat.getColor(context, R.color.ds_on_holiday_plate);
        typeColors = TypeColors.colors(context);

        Typeface semibold = ResourcesCompat.getFont(context, R.font.golos_text_semibold);
        Typeface regular = ResourcesCompat.getFont(context, R.font.golos_text_regular);
        numberTypeface = semibold != null ? semibold : Typeface.DEFAULT_BOLD;
        outsideTypeface = regular != null ? regular : Typeface.DEFAULT;
        numberPaint.setTextAlign(Paint.Align.CENTER);
        numberPaint.setTextSize(16 * getResources().getDisplayMetrics().scaledDensity);
        // Табличные цифры (п. 2.3)
        numberPaint.setFontFeatureSettings("tnum");
        strokePaint.setStyle(Paint.Style.STROKE);
        strokePaint.setStrokeWidth(todayStroke);

        ViewConfiguration vc = ViewConfiguration.get(context);
        touchSlop = vc.getScaledTouchSlop();
        minFlingVelocity = vc.getScaledMinimumFlingVelocity() * 4;

        accessibilityHelper = new Helper(this);
        ViewCompat.setAccessibilityDelegate(this, accessibilityHelper);
        setFocusable(true);
        heightPx = heightForRows(rowsOf(month));
    }

    public void setListener(@Nullable Listener listener) {
        this.listener = listener;
    }

    public void setModelProvider(@Nullable ModelProvider provider) {
        this.provider = provider;
        models.clear();
        invalidate();
    }

    public YearMonth getMonth() {
        return month;
    }

    public void setToday(LocalDate today) {
        this.today = today;
        invalidate();
    }

    /** Перечитать данные (новые данные календаря, свои события). */
    public void reloadData() {
        models.clear();
        invalidate();
        accessibilityHelper.invalidateRoot();
    }

    /** Перечитать данные после смены фильтров: маркеры затухают и проявляются за 150 мс. */
    public void reloadDataWithFade() {
        previousModels = new HashMap<>(models);
        models.clear();
        if (fadeAnimator != null) fadeAnimator.cancel();
        fadeAnimator = ValueAnimator.ofFloat(0f, 1f);
        fadeAnimator.setDuration(150);
        fadeAnimator.setInterpolator(Motion.STANDARD);
        fadeAnimator.addUpdateListener(a -> {
            markerFade = (float) a.getAnimatedValue();
            invalidate();
        });
        fadeAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                previousModels = null;
                markerFade = 1f;
                invalidate();
            }
        });
        markerFade = 0f;
        fadeAnimator.start();
        accessibilityHelper.invalidateRoot();
    }

    /** Маркеры дня появляются: масштаб 0 → 1,2 → 1 за 250 мс. */
    public void popMarkers(LocalDate date) {
        if (popAnimator != null) popAnimator.cancel();
        popDate = date;
        popAnimator = ValueAnimator.ofFloat(0f, 1.2f, 1f);
        popAnimator.setDuration(250);
        popAnimator.setInterpolator(Motion.DECELERATE);
        popAnimator.addUpdateListener(a -> {
            popScale = (float) a.getAnimatedValue();
            invalidate();
        });
        popAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                popDate = null;
                popScale = 1f;
                invalidate();
            }
        });
        popAnimator.start();
    }

    public void setSelected(@Nullable LocalDate date, boolean animate) {
        if (date != null && date.equals(selected)) return;
        selected = date;
        if (selectionAnimator != null) selectionAnimator.cancel();
        if (animate && date != null) {
            // Подложка появляется: масштаб 0,9 → 1 и прозрачность 0 → 1 за 120 мс (п. 9.2)
            selectionAnimator = ValueAnimator.ofFloat(0f, 1f);
            selectionAnimator.setDuration(120);
            selectionAnimator.setInterpolator(Motion.DECELERATE);
            selectionAnimator.addUpdateListener(a -> {
                selectionProgress = (float) a.getAnimatedValue();
                invalidate();
            });
            selectionProgress = 0f;
            selectionAnimator.start();
        } else {
            selectionProgress = 1f;
        }
        invalidate();
    }

    /** Показать месяц; соседний месяц въезжает сбоку за 200 мс (п. 9.2). */
    public void showMonth(YearMonth target, boolean animate) {
        finishPageAnimation();
        if (target.equals(month)) return;
        boolean adjacent = target.equals(month.plusMonths(1)) || target.equals(month.minusMonths(1));
        if (!animate || !adjacent || getWidth() == 0) {
            commitMonth(target);
            return;
        }
        int direction = target.isAfter(month) ? 1 : -1;
        animatePage(direction, 0f, 200, Motion.STANDARD);
    }

    // region Отрисовка

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width = MeasureSpec.getSize(widthMeasureSpec);
        setMeasuredDimension(width, heightPx);
    }

    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        super.onDraw(canvas);
        int width = getWidth();
        canvas.save();
        canvas.clipRect(0, 0, width, getHeight());
        drawMonth(canvas, month, offset);
        if (offset < 0) drawMonth(canvas, month.plusMonths(1), offset + width);
        if (offset > 0) drawMonth(canvas, month.minusMonths(1), offset - width);
        canvas.restore();
    }

    private void drawMonth(Canvas canvas, YearMonth ym, float dx) {
        MonthModel model = model(ym);
        MonthModel previous = previousModels != null ? previousModels.get(ym) : null;
        float cellWidth = getWidth() / 7f;
        int lead = ym.atDay(1).getDayOfWeek().getValue() - 1;
        int length = ym.lengthOfMonth();
        int prevLength = ym.minusMonths(1).lengthOfMonth();
        int cells = rowsOf(ym) * 7;

        for (int i = 0; i < cells; i++) {
            int col = i % 7;
            int row = i / 7;
            float left = dx + col * cellWidth;
            if (left > getWidth() || left + cellWidth < 0) continue;
            float top = row * (cellHeight + rowGap);
            float cx = left + cellWidth / 2f;
            int day = i - lead + 1;
            boolean outside = day < 1 || day > length;
            int number = day < 1 ? prevLength + day : (day > length ? day - length : day);
            MonthModel.Day info = outside || model == null ? null : model.days[day - 1];

            if (info != null && info.date.equals(selected)) {
                float p = selectionProgress;
                float scale = 0.9f + 0.1f * p;
                float w = (cellWidth - 2 * selectionInset) * scale;
                float h = cellHeight * scale;
                rect.set(cx - w / 2f, top + (cellHeight - h) / 2f, cx + w / 2f, top + (cellHeight + h) / 2f);
                fillPaint.setColor(colorSelected);
                fillPaint.setAlpha((int) (255 * p));
                canvas.drawRoundRect(rect, selectionRadius, selectionRadius, fillPaint);
                fillPaint.setAlpha(255);
            }

            float boxTop = top + numberTop;
            rect.set(cx - numberBox / 2f, boxTop, cx + numberBox / 2f, boxTop + numberBox);
            if (info != null && info.officialHoliday) {
                fillPaint.setColor(colorHolidayPlate);
                canvas.drawRoundRect(rect, todayRadius, todayRadius, fillPaint);
            }
            if (info != null && info.date.equals(today)) {
                float inset = todayStroke / 2f;
                rect.inset(inset, inset);
                strokePaint.setColor(colorAccent);
                canvas.drawRoundRect(rect, todayRadius - inset, todayRadius - inset, strokePaint);
                rect.inset(-inset, -inset);
            }

            int color;
            if (outside) color = colorOutside;
            else if (info != null && info.officialHoliday) color = colorOnHolidayPlate;
            else if (info != null ? info.dayOff : col >= 5) color = colorWeekend;
            else color = colorText;
            numberPaint.setColor(color);
            numberPaint.setTypeface(outside ? outsideTypeface : numberTypeface);
            Paint.FontMetrics fm = numberPaint.getFontMetrics();
            float baseline = rect.centerY() - (fm.ascent + fm.descent) / 2f;
            canvas.drawText(String.valueOf(number), cx, baseline, numberPaint);

            // Дни соседних месяцев — без маркеров
            if (info == null) continue;
            boolean popping = info.date.equals(popDate);
            if (popping) {
                canvas.save();
                canvas.scale(popScale, popScale, cx, top + markerCenterY);
            }
            if (previous != null && markerFade < 1f) {
                drawMarkers(canvas, previous.days[day - 1].markers, cx, top, 1f - markerFade);
                drawMarkers(canvas, info.markers, cx, top, markerFade);
            } else {
                drawMarkers(canvas, info.markers, cx, top, 1f);
            }
            if (popping) canvas.restore();
        }
    }

    private void drawMarkers(Canvas canvas, List<MonthModel.Marker> markers, float cx, float top, float alpha) {
        if (markers.isEmpty() || alpha <= 0f) return;
        float total = 0;
        for (MonthModel.Marker m : markers) total += sizeOf(m);
        total += markerGap * (markers.size() - 1);
        float x = cx - total / 2f;
        float cy = top + markerCenterY;
        for (MonthModel.Marker m : markers) {
            float size = sizeOf(m);
            int color = m.type == EventType.USER ? m.userColor : typeColors[m.type.ordinal()];
            markerPaint.setColor(color);
            markerPaint.setAlpha((int) (255 * alpha));
            MarkerDrawable.draw(canvas, markerPaint, m.type, x + size / 2f, cy, size, density);
            x += size + markerGap;
        }
    }

    private float sizeOf(MonthModel.Marker marker) {
        return marker.type == EventType.USER ? starSize : markerSize;
    }

    // endregion

    // region Жесты

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (velocityTracker == null) velocityTracker = VelocityTracker.obtain();
        velocityTracker.addMovement(event);
        float x = event.getX();
        float y = event.getY();
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                // Новое касание прерывает текущую анимацию (п. 9)
                finishPageAnimation();
                downX = x;
                downY = y;
                dragging = false;
                tapCandidate = true;
                return true;
            case MotionEvent.ACTION_MOVE: {
                float dx = x - downX;
                float dy = y - downY;
                if (!dragging && Math.abs(dx) > touchSlop && Math.abs(dx) > Math.abs(dy)) {
                    dragging = true;
                    tapCandidate = false;
                    if (getParent() != null) getParent().requestDisallowInterceptTouchEvent(true);
                } else if (Math.abs(dy) > touchSlop) {
                    tapCandidate = false;
                }
                if (dragging) {
                    offset = dx;
                    invalidate();
                }
                return true;
            }
            case MotionEvent.ACTION_UP: {
                if (dragging) {
                    velocityTracker.computeCurrentVelocity(1000);
                    settle(velocityTracker.getXVelocity());
                } else if (tapCandidate) {
                    LocalDate date = dateAt(x, y);
                    if (date != null) {
                        performClick();
                        pick(date);
                    }
                }
                recycleTracker();
                dragging = false;
                return true;
            }
            case MotionEvent.ACTION_CANCEL:
                if (dragging) settle(0);
                recycleTracker();
                dragging = false;
                return true;
        }
        return super.onTouchEvent(event);
    }

    @Override
    public boolean performClick() {
        return super.performClick();
    }

    private void settle(float velocityX) {
        int width = getWidth();
        int direction = 0;
        if (offset < -width * SWIPE_THRESHOLD || velocityX < -minFlingVelocity) direction = 1;
        else if (offset > width * SWIPE_THRESHOLD || velocityX > minFlingVelocity) direction = -1;
        // Не листаем против движения пальца
        if (direction == 1 && offset > 0 || direction == -1 && offset < 0) direction = 0;
        animatePage(direction, offset, 200, Motion.DECELERATE);
    }

    /** direction: 1 — следующий месяц, −1 — предыдущий, 0 — вернуться. */
    private void animatePage(final int direction, float from, long duration, Interpolator interpolator) {
        float to = -direction * getWidth();
        pageAnimator = ValueAnimator.ofFloat(from, to);
        pageAnimator.setDuration(duration);
        pageAnimator.setInterpolator(interpolator);
        pageAnimator.addUpdateListener(a -> {
            offset = (float) a.getAnimatedValue();
            invalidate();
        });
        pageAnimator.addListener(new AnimatorListenerAdapter() {
            private boolean done;

            @Override
            public void onAnimationEnd(Animator animation) {
                if (done) return;
                done = true;
                pageAnimator = null;
                offset = 0;
                if (direction != 0) commitMonth(month.plusMonths(direction));
                else invalidate();
            }
        });
        pageAnimator.start();
    }

    private void finishPageAnimation() {
        if (pageAnimator != null) pageAnimator.end();
    }

    private void commitMonth(YearMonth target) {
        month = target;
        offset = 0;
        animateHeightTo(heightForRows(rowsOf(target)));
        invalidate();
        accessibilityHelper.invalidateRoot();
        if (listener != null) listener.onMonthChanged(target);
    }

    private void animateHeightTo(int target) {
        if (heightAnimator != null) heightAnimator.cancel();
        if (heightPx == target) return;
        if (!isLaidOut()) {
            heightPx = target;
            requestLayout();
            return;
        }
        heightAnimator = ValueAnimator.ofInt(heightPx, target);
        heightAnimator.setDuration(150);
        heightAnimator.setInterpolator(Motion.STANDARD);
        heightAnimator.addUpdateListener(a -> {
            heightPx = (int) a.getAnimatedValue();
            requestLayout();
        });
        heightAnimator.start();
    }

    private void pick(LocalDate date) {
        setSelected(date, true);
        if (listener != null) listener.onDaySelected(date);
        accessibilityHelper.invalidateVirtualView(date.getDayOfMonth());
        accessibilityHelper.sendEventForVirtualView(date.getDayOfMonth(),
                android.view.accessibility.AccessibilityEvent.TYPE_VIEW_CLICKED);
    }

    private void recycleTracker() {
        if (velocityTracker != null) {
            velocityTracker.recycle();
            velocityTracker = null;
        }
    }

    // endregion

    // region Геометрия

    private static int rowsOf(YearMonth ym) {
        int lead = ym.atDay(1).getDayOfWeek().getValue() - 1;
        return (lead + ym.lengthOfMonth() + 6) / 7;
    }

    private int heightForRows(int rows) {
        return (int) Math.ceil(rows * cellHeight + (rows - 1) * rowGap);
    }

    @Nullable
    private LocalDate dateAt(float x, float y) {
        if (getWidth() == 0) return null;
        int col = (int) (x / (getWidth() / 7f));
        int row = (int) (y / (cellHeight + rowGap));
        if (col < 0 || col > 6 || row < 0 || row >= rowsOf(month)) return null;
        int lead = month.atDay(1).getDayOfWeek().getValue() - 1;
        int day = row * 7 + col - lead + 1;
        if (day < 1 || day > month.lengthOfMonth()) return null;
        return month.atDay(day);
    }

    private void cellBounds(int day, Rect out) {
        int lead = month.atDay(1).getDayOfWeek().getValue() - 1;
        int index = lead + day - 1;
        float cellWidth = getWidth() / 7f;
        int left = (int) ((index % 7) * cellWidth);
        int top = (int) ((index / 7) * (cellHeight + rowGap));
        out.set(left, top, (int) (left + cellWidth), (int) (top + cellHeight));
    }

    @Nullable
    private MonthModel model(YearMonth ym) {
        MonthModel model = models.get(ym);
        if (model == null && provider != null) {
            model = provider.get(ym);
            models.put(ym, model);
        }
        return model;
    }

    // endregion

    // region Доступность: у каждой ячейки описание для TalkBack (п. 7)

    @Override
    protected boolean dispatchHoverEvent(MotionEvent event) {
        return accessibilityHelper.dispatchHoverEvent(event) || super.dispatchHoverEvent(event);
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        return accessibilityHelper.dispatchKeyEvent(event) || super.dispatchKeyEvent(event);
    }

    @Override
    protected void onFocusChanged(boolean gainFocus, int direction, @Nullable Rect previouslyFocusedRect) {
        super.onFocusChanged(gainFocus, direction, previouslyFocusedRect);
        accessibilityHelper.onFocusChanged(gainFocus, direction, previouslyFocusedRect);
    }

    private final class Helper extends ExploreByTouchHelper {
        private final Rect bounds = new Rect();

        Helper(View host) {
            super(host);
        }

        @Override
        protected int getVirtualViewAt(float x, float y) {
            LocalDate date = dateAt(x, y);
            return date == null ? INVALID_ID : date.getDayOfMonth();
        }

        @Override
        protected void getVisibleVirtualViews(List<Integer> virtualViewIds) {
            for (int day = 1; day <= month.lengthOfMonth(); day++) virtualViewIds.add(day);
        }

        @Override
        @SuppressWarnings("deprecation")
        protected void onPopulateNodeForVirtualView(int virtualViewId, @NonNull AccessibilityNodeInfoCompat node) {
            MonthModel model = model(month);
            String text = model != null && virtualViewId <= model.days.length
                    ? model.days[virtualViewId - 1].accessibilityText
                    : String.valueOf(virtualViewId);
            node.setContentDescription(text);
            cellBounds(virtualViewId, bounds);
            node.setBoundsInParent(bounds);
            node.addAction(AccessibilityNodeInfoCompat.ACTION_CLICK);
            node.setClickable(true);
            node.setSelected(month.atDay(virtualViewId).equals(selected));
        }

        @Override
        protected boolean onPerformActionForVirtualView(int virtualViewId, int action, @Nullable Bundle arguments) {
            if (action == AccessibilityNodeInfoCompat.ACTION_CLICK) {
                pick(month.atDay(virtualViewId));
                return true;
            }
            return false;
        }
    }

    // endregion
}

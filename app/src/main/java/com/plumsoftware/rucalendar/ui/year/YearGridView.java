package com.plumsoftware.rucalendar.ui.year;

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
import android.view.View;
import android.view.ViewConfiguration;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat;
import androidx.customview.widget.ExploreByTouchHelper;

import com.plumsoftware.rucalendar.R;
import com.plumsoftware.rucalendar.data.HolidayRepository;
import com.plumsoftware.rucalendar.data.ProductionCalendarRepository;
import com.plumsoftware.rucalendar.data.RuDates;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

/**
 * 12 мини-месяцев 3 × 4 (ТЗ п. 4.5): праздник — белая цифра на красном квадрате, выходной — красная цифра,
 * сокращённый день — цифра в рамке, сегодня — синяя рамка. Нажатие на мини-месяц открывает его.
 */
public class YearGridView extends View {

    public interface Listener {
        void onMonthClick(YearMonth month);
    }

    static final int HOLIDAY = 1;
    static final int OFF = 2;
    static final int SHORT = 3;
    static final int WORK = 0;

    private final float density;
    private final float cellHeight;
    private final float titleHeight;
    private final float titleGap;
    private final float gap;
    private final float blockHeight;
    private final float plate;

    private final Paint titlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint digitPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint fillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint strokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rect = new RectF();
    private final Typeface regular;
    private final Typeface semibold;
    private final Typeface bold;

    private final int colorText;
    private final int colorBody;
    private final int colorAccent;
    private final int colorWeekend;
    private final int colorPlate;
    private final int colorOnPlate;

    private final Helper helper;
    private final int touchSlop;

    private int year = LocalDate.now().getYear();
    private LocalDate today = LocalDate.now();
    /** Состояние каждого дня года: индекс — день года − 1. */
    private int[] kinds = new int[0];
    @Nullable
    private ProductionCalendarRepository production;
    @Nullable
    private HolidayRepository holidays;
    @Nullable
    private Listener listener;
    private float downX;
    private float downY;

    public YearGridView(Context context) {
        this(context, null);
    }

    public YearGridView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        density = getResources().getDisplayMetrics().density;
        float scaled = getResources().getDisplayMetrics().scaledDensity;
        cellHeight = 17 * density;
        titleHeight = 18 * density;
        titleGap = 6 * density;
        gap = 14 * density;
        blockHeight = titleHeight + titleGap + 6 * cellHeight;
        plate = 15 * density;

        colorText = ContextCompat.getColor(context, R.color.ds_text_primary);
        colorBody = ContextCompat.getColor(context, R.color.ds_text_body);
        colorAccent = ContextCompat.getColor(context, R.color.ds_accent);
        colorWeekend = ContextCompat.getColor(context, R.color.ds_weekend_text);
        colorPlate = ContextCompat.getColor(context, R.color.ds_holiday_plate);
        colorOnPlate = ContextCompat.getColor(context, R.color.ds_on_holiday_plate);

        regular = font(context, R.font.golos_text_regular, Typeface.DEFAULT);
        semibold = font(context, R.font.golos_text_semibold, Typeface.DEFAULT_BOLD);
        bold = font(context, R.font.golos_text_bold, Typeface.DEFAULT_BOLD);
        titlePaint.setTypeface(bold);
        titlePaint.setTextSize(13 * scaled);
        digitPaint.setTextAlign(Paint.Align.CENTER);
        digitPaint.setTextSize(10 * scaled);
        digitPaint.setFontFeatureSettings("tnum");
        strokePaint.setStyle(Paint.Style.STROKE);
        strokePaint.setStrokeWidth(1.5f * density);

        touchSlop = ViewConfiguration.get(context).getScaledTouchSlop();
        helper = new Helper(this);
        ViewCompat.setAccessibilityDelegate(this, helper);
        setFocusable(true);
    }

    private static Typeface font(Context context, int res, Typeface fallback) {
        Typeface tf = ResourcesCompat.getFont(context, res);
        return tf != null ? tf : fallback;
    }

    public void setRepositories(ProductionCalendarRepository production, HolidayRepository holidays) {
        this.production = production;
        this.holidays = holidays;
        reload();
    }

    public void setListener(@Nullable Listener listener) {
        this.listener = listener;
    }

    public void setYear(int year) {
        this.year = year;
        reload();
    }

    public int getYear() {
        return year;
    }

    public void setToday(LocalDate today) {
        this.today = today;
        invalidate();
    }

    public void reload() {
        if (production == null || holidays == null) return;
        int length = java.time.Year.of(year).length();
        kinds = new int[length];
        LocalDate d = LocalDate.of(year, 1, 1);
        for (int i = 0; i < length; i++, d = d.plusDays(1)) {
            ProductionCalendarRepository.DayKind kind = production.kind(d);
            if (kind == ProductionCalendarRepository.DayKind.DAY_OFF) {
                kinds[i] = holidays.nonWorkingOn(d) != null ? HOLIDAY : OFF;
            } else {
                kinds[i] = kind == ProductionCalendarRepository.DayKind.SHORT ? SHORT : WORK;
            }
        }
        invalidate();
        helper.invalidateRoot();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        setMeasuredDimension(MeasureSpec.getSize(widthMeasureSpec), (int) Math.ceil(4 * blockHeight + 3 * gap));
    }

    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        super.onDraw(canvas);
        if (kinds.length == 0) return;
        float colWidth = (getWidth() - 2 * gap) / 3f;
        float cellWidth = colWidth / 7f;
        Paint.FontMetrics tfm = titlePaint.getFontMetrics();
        for (int m = 1; m <= 12; m++) {
            float left = ((m - 1) % 3) * (colWidth + gap);
            float top = ((m - 1) / 3) * (blockHeight + gap);
            boolean current = year == today.getYear() && m == today.getMonthValue();
            titlePaint.setColor(current ? colorAccent : colorText);
            canvas.drawText(RuDates.monthName(m), left, top + titleHeight / 2f - (tfm.ascent + tfm.descent) / 2f, titlePaint);

            YearMonth ym = YearMonth.of(year, m);
            int lead = ym.atDay(1).getDayOfWeek().getValue() - 1;
            int dayOfYear = ym.atDay(1).getDayOfYear();
            float gridTop = top + titleHeight + titleGap;
            for (int day = 1; day <= ym.lengthOfMonth(); day++) {
                int index = lead + day - 1;
                float cx = left + (index % 7) * cellWidth + cellWidth / 2f;
                float cy = gridTop + (index / 7) * cellHeight + cellHeight / 2f;
                drawDay(canvas, cx, cy, day, kinds[dayOfYear + day - 2], current && day == today.getDayOfMonth());
            }
        }
    }

    private void drawDay(Canvas canvas, float cx, float cy, int day, int kind, boolean isToday) {
        float half = plate / 2f;
        float r = 5 * density;
        rect.set(cx - half, cy - half, cx + half, cy + half);
        int color;
        Typeface typeface;
        switch (kind) {
            case HOLIDAY:
                fillPaint.setColor(colorPlate);
                canvas.drawRoundRect(rect, r, r, fillPaint);
                color = colorOnPlate;
                typeface = bold;
                break;
            case OFF:
                color = colorWeekend;
                typeface = semibold;
                break;
            case SHORT:
                strokePaint.setColor(colorText);
                float inset = strokePaint.getStrokeWidth() / 2f;
                rect.inset(inset, inset);
                canvas.drawRoundRect(rect, r, r, strokePaint);
                color = colorText;
                typeface = regular;
                break;
            default:
                color = colorBody;
                typeface = regular;
        }
        if (isToday) {
            // Синяя рамка поверх любого состояния
            float h = cellHeight / 2f;
            rect.set(cx - h, cy - h, cx + h, cy + h);
            float inset = strokePaint.getStrokeWidth() / 2f;
            rect.inset(inset, inset);
            strokePaint.setColor(colorAccent);
            canvas.drawRoundRect(rect, 6 * density, 6 * density, strokePaint);
        }
        digitPaint.setColor(color);
        digitPaint.setTypeface(typeface);
        Paint.FontMetrics fm = digitPaint.getFontMetrics();
        canvas.drawText(String.valueOf(day), cx, cy - (fm.ascent + fm.descent) / 2f, digitPaint);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                downX = event.getX();
                downY = event.getY();
                return true;
            case MotionEvent.ACTION_UP:
                if (Math.abs(event.getX() - downX) < touchSlop && Math.abs(event.getY() - downY) < touchSlop) {
                    int month = monthAt(event.getX(), event.getY());
                    if (month > 0) {
                        performClick();
                        if (listener != null) listener.onMonthClick(YearMonth.of(year, month));
                    }
                }
                return true;
        }
        return super.onTouchEvent(event);
    }

    @Override
    public boolean performClick() {
        return super.performClick();
    }

    private int monthAt(float x, float y) {
        float colWidth = (getWidth() - 2 * gap) / 3f;
        int col = (int) (x / (colWidth + gap));
        int row = (int) (y / (blockHeight + gap));
        if (col < 0 || col > 2 || row < 0 || row > 3) return 0;
        return row * 3 + col + 1;
    }

    private void monthBounds(int month, Rect out) {
        float colWidth = (getWidth() - 2 * gap) / 3f;
        int left = (int) (((month - 1) % 3) * (colWidth + gap));
        int top = (int) (((month - 1) / 3) * (blockHeight + gap));
        out.set(left, top, (int) (left + colWidth), (int) (top + blockHeight));
    }

    private int workingDays(int month) {
        YearMonth ym = YearMonth.of(year, month);
        int start = ym.atDay(1).getDayOfYear() - 1;
        int count = 0;
        for (int i = 0; i < ym.lengthOfMonth() && start + i < kinds.length; i++) {
            int k = kinds[start + i];
            if (k == WORK || k == SHORT) count++;
        }
        return count;
    }

    @Override
    protected boolean dispatchHoverEvent(MotionEvent event) {
        return helper.dispatchHoverEvent(event) || super.dispatchHoverEvent(event);
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        return helper.dispatchKeyEvent(event) || super.dispatchKeyEvent(event);
    }

    @Override
    protected void onFocusChanged(boolean gainFocus, int direction, @Nullable Rect previouslyFocusedRect) {
        super.onFocusChanged(gainFocus, direction, previouslyFocusedRect);
        helper.onFocusChanged(gainFocus, direction, previouslyFocusedRect);
    }

    private final class Helper extends ExploreByTouchHelper {
        private final Rect bounds = new Rect();

        Helper(View host) {
            super(host);
        }

        @Override
        protected int getVirtualViewAt(float x, float y) {
            int month = monthAt(x, y);
            return month > 0 ? month : INVALID_ID;
        }

        @Override
        protected void getVisibleVirtualViews(List<Integer> ids) {
            for (int m = 1; m <= 12; m++) ids.add(m);
        }

        @Override
        @SuppressWarnings("deprecation")
        protected void onPopulateNodeForVirtualView(int id, @NonNull AccessibilityNodeInfoCompat node) {
            node.setContentDescription(RuDates.monthName(id) + " " + year + ", "
                    + RuDates.plural(workingDays(id), "рабочий день", "рабочих дня", "рабочих дней"));
            monthBounds(id, bounds);
            node.setBoundsInParent(bounds);
            node.addAction(AccessibilityNodeInfoCompat.ACTION_CLICK);
            node.setClickable(true);
        }

        @Override
        protected boolean onPerformActionForVirtualView(int id, int action, @Nullable Bundle arguments) {
            if (action == AccessibilityNodeInfoCompat.ACTION_CLICK && listener != null) {
                listener.onMonthClick(YearMonth.of(year, id));
                return true;
            }
            return false;
        }
    }
}

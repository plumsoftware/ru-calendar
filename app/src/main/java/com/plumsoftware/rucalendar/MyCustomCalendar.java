package com.plumsoftware.rucalendar;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.plumsoftware.rucalendar.R;

import java.util.Calendar;
import java.util.Map;

public class MyCustomCalendar extends LinearLayout {
    public static final int PREVIOUS = -1;
    public static final int NEXT = 1;
    public static final int THREE_LETTER_MONTH__WITH_YEAR = 0;
    public static final int FULL_MONTH__WITH_YEAR = 1;
    public static final int SUNDAY = 0;
    public static final int MONDAY = 1;
    public static final int TUESDAY = 2;
    public static final int WEDNESDAY = 3;
    public static final int THURSDAY = 4;
    public static final int FRIDAY = 5;
    public static final int SATURDAY = 6;
    private final String[] MONTHS = new String[]{"Январь", "Февраль", "Март", "Апрель", "Май", "Июнь", "Июль", "Август", "Сентябрь", "Октябрь", "Ноябрь", "Декабрь"};
    private Context context = null;
    private View view = null;
    private ImageButton butLeft = null;
    private ImageButton butRight = null;
    private TextView tvMonthYear = null;
    private TextView tvYear = null;
    private TextView[] tvDaysOfWeek = null;
    private LinearLayout llWeeks = null;
    private View[] btnAll = null;
    private Calendar selectedDate = null;
    private OnDateSelectedListener listener = null;
    private OnNavigationButtonClickedListener leftButtonListener = null;
    private OnNavigationButtonClickedListener rightButtonListener = null;
    private float rowHeight = 0.0F;
    private View selectedButton = null;
    private int startFrom = -1;
    private int monthYearFormat = -1;
    private int dayOfWeekLength = -1;
    private Drawable draLeftButton = null;
    private Drawable draRightButton = null;
    private Map<Integer, Object> mapDateToTag = null;
    private Map<Integer, Object> mapDateToDesc = null;
    private Map<Object, Property> mapDescToProp = null;

    public MyCustomCalendar(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.context = context;
        TypedArray attributes = context.obtainStyledAttributes(attrs, R.styleable.CustomCalendar);
        this.startFrom = attributes.getInt(R.styleable.CustomCalendar_day_of_week_start_from, 0);
        this.monthYearFormat = attributes.getInt(R.styleable.CustomCalendar_month_year_format, 0);
        this.dayOfWeekLength = attributes.getInt(R.styleable.CustomCalendar_day_of_week_length, 1);
        this.draLeftButton = attributes.getDrawable(R.styleable.CustomCalendar_left_button_src);
        this.draRightButton = attributes.getDrawable(R.styleable.CustomCalendar_right_button_src);
        this.rowHeight = attributes.getDimension(R.styleable.CustomCalendar_row_height, 0.0F);
        this.initialize();
    }

    private void initialize() {
        this.view = inflate(this.context, R.layout.customcalendar, this);
        this.butLeft = (ImageButton)this.findViewById(R.id.but_left);
        this.butRight = (ImageButton)this.findViewById(R.id.but_right);
        if (this.draLeftButton != null) {
            this.butLeft.setImageDrawable(this.draLeftButton);
        }

        if (this.draRightButton != null) {
            this.butRight.setImageDrawable(this.draRightButton);
        }

        this.tvMonthYear = (TextView)this.findViewById(R.id.tv_month_year);
        this.tvYear = (TextView)this.findViewById(R.id.textViewYear);
        this.tvDaysOfWeek = new TextView[7];
        this.tvDaysOfWeek[0] = (TextView)this.findViewById(R.id.tv_day_of_week_0);
        this.tvDaysOfWeek[1] = (TextView)this.findViewById(R.id.tv_day_of_week_1);
        this.tvDaysOfWeek[2] = (TextView)this.findViewById(R.id.tv_day_of_week_2);
        this.tvDaysOfWeek[3] = (TextView)this.findViewById(R.id.tv_day_of_week_3);
        this.tvDaysOfWeek[4] = (TextView)this.findViewById(R.id.tv_day_of_week_4);
        this.tvDaysOfWeek[5] = (TextView)this.findViewById(R.id.tv_day_of_week_5);
        this.tvDaysOfWeek[6] = (TextView)this.findViewById(R.id.tv_day_of_week_6);
        this.llWeeks = (LinearLayout)this.findViewById(R.id.ll_weeks);
        this.selectedDate = Calendar.getInstance();
        this.readyDaysOfWeek();
        this.setAll();
        this.butLeft.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                Calendar previousMonth = Calendar.getInstance();
                previousMonth.set(2, MyCustomCalendar.this.selectedDate.get(2) - 1 != -1 ? MyCustomCalendar.this.selectedDate.get(2) - 1 : 11);
                previousMonth.set(1, MyCustomCalendar.this.selectedDate.get(2) - 1 != -1 ? MyCustomCalendar.this.selectedDate.get(1) : MyCustomCalendar.this.selectedDate.get(1) - 1);
                previousMonth.set(5, MyCustomCalendar.this.selectedDate.get(5) < previousMonth.getActualMaximum(5) ? MyCustomCalendar.this.selectedDate.get(5) : previousMonth.getActualMaximum(5));
                MyCustomCalendar.this.selectedDate = previousMonth;
                if (MyCustomCalendar.this.rightButtonListener != null) {
                    Map<Integer, Object>[] arr = MyCustomCalendar.this.leftButtonListener.onNavigationButtonClicked(-1, previousMonth);
                    MyCustomCalendar.this.mapDateToDesc = arr[0];
                    MyCustomCalendar.this.mapDateToTag = arr[1];
                } else {
                    MyCustomCalendar.this.mapDateToDesc = null;
                    MyCustomCalendar.this.mapDateToTag = null;
                }

                MyCustomCalendar.this.setAll();
            }
        });
        this.butRight.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                Calendar nextMonth = Calendar.getInstance();
                nextMonth.set(2, MyCustomCalendar.this.selectedDate.get(2) + 1 != 12 ? MyCustomCalendar.this.selectedDate.get(2) + 1 : 0);
                nextMonth.set(1, MyCustomCalendar.this.selectedDate.get(2) + 1 != 12 ? MyCustomCalendar.this.selectedDate.get(1) : MyCustomCalendar.this.selectedDate.get(1) + 1);
                nextMonth.set(5, MyCustomCalendar.this.selectedDate.get(5) < nextMonth.getActualMaximum(5) ? MyCustomCalendar.this.selectedDate.get(5) : nextMonth.getActualMaximum(5));
                MyCustomCalendar.this.selectedDate = nextMonth;
                if (MyCustomCalendar.this.leftButtonListener != null) {
                    Map<Integer, Object>[] arr = MyCustomCalendar.this.leftButtonListener.onNavigationButtonClicked(1, nextMonth);
                    MyCustomCalendar.this.mapDateToDesc = arr[0];
                    MyCustomCalendar.this.mapDateToTag = arr[1];
                } else {
                    MyCustomCalendar.this.mapDateToDesc = null;
                    MyCustomCalendar.this.mapDateToTag = null;
                }

                MyCustomCalendar.this.setAll();
            }
        });
    }

    @SuppressLint("WrongConstant")
    private void setAll() {
        this.readyMonthAndYear();
        this.llWeeks.removeAllViews();
        this.btnAll = new View[this.selectedDate.getActualMaximum(5)];
        LinearLayout llWeek = new LinearLayout(this.context);
        llWeek.setLayoutParams(new LayoutParams(-1, this.rowHeight == 0.0F ? -2 : (int)this.rowHeight));
        llWeek.setOrientation(0);
        Calendar previousMonth = Calendar.getInstance();
        previousMonth.set(2, this.selectedDate.get(2) - 1 != -1 ? this.selectedDate.get(2) - 1 : 11);
        previousMonth.set(1, this.selectedDate.get(2) - 1 != -1 ? this.selectedDate.get(1) : this.selectedDate.get(1) - 1);
        Calendar thisMonth = Calendar.getInstance();
        thisMonth.set(2, this.selectedDate.get(2));
        thisMonth.set(1, this.selectedDate.get(1));
        thisMonth.set(5, 1);
        int j = thisMonth.get(7) - this.startFrom - 1;

        int index;
        for(index = 0; index < j; ++index) {
            View btn = null;
            if (this.mapDescToProp != null && this.mapDescToProp.get("disabled") != null && ((Property)this.mapDescToProp.get("disabled")).layoutResource != -1) {
                Property prop = (Property)this.mapDescToProp.get("disabled");
                btn = LayoutInflater.from(this.context).inflate(prop.layoutResource, (ViewGroup)null);
                if (prop.dateTextViewResource != -1 && ((View)btn).findViewById(prop.dateTextViewResource) != null) {
                    ((TextView)((View)btn).findViewById(prop.dateTextViewResource)).setText("" + (previousMonth.getActualMaximum(5) - (j - index - 1)));
                }
            } else {
                btn = new Button(this.context);
                ((Button)btn).setText("" + (previousMonth.getActualMaximum(5) - (j - index - 1)));
            }

            ((View)btn).setLayoutParams(new LayoutParams(0, -1, 1.0F));
            llWeek.addView((View)btn);
            ((View)btn).setEnabled(false);
        }

        index = 0;

        int i;
        for(i = 0; i < 7 - j; ++i) {
            this.btnAll[index] = this.readyButton(index + 1);
            this.btnAll[index].setEnabled(true);
            llWeek.addView(this.btnAll[index]);
            ++index;
        }

        this.llWeeks.addView(llWeek);

        while(thisMonth.getActualMaximum(5) - 7 > index) {
            llWeek = new LinearLayout(this.context);
            llWeek.setLayoutParams(new LayoutParams(-1, this.rowHeight == 0.0F ? -2 : (int)this.rowHeight));
            llWeek.setOrientation(0);

            for(i = 0; i < 7; ++i) {
                this.btnAll[index] = this.readyButton(index + 1);
                llWeek.addView(this.btnAll[index]);
                this.btnAll[index].setEnabled(true);
                ++index;
            }

            this.llWeeks.addView(llWeek);
        }

        llWeek = new LinearLayout(this.context);
        llWeek.setLayoutParams(new LayoutParams(-1, this.rowHeight == 0.0F ? -2 : (int)this.rowHeight));
        llWeek.setOrientation(0);

        for(i = 0; index < this.selectedDate.getActualMaximum(5); ++i) {
            this.btnAll[index] = this.readyButton(index + 1);
            llWeek.addView(this.btnAll[index]);
            this.btnAll[index].setEnabled(true);
            ++index;
        }

        for(int k = 1; k <= 7 - i; ++k) {
            View btn = null;
            if (this.mapDescToProp != null && this.mapDescToProp.get("disabled") != null) {
                Property prop = (Property)this.mapDescToProp.get("disabled");
                btn = LayoutInflater.from(this.context).inflate(prop.layoutResource, (ViewGroup)null);
                if (prop.dateTextViewResource != -1 && ((View)btn).findViewById(prop.dateTextViewResource) != null) {
                    ((TextView)((View)btn).findViewById(prop.dateTextViewResource)).setText("" + k);
                }
            } else {
                btn = new Button(this.context);
                ((Button)btn).setText("" + k);
            }

            ((View)btn).setLayoutParams(new LayoutParams(0, -1, 1.0F));
            llWeek.addView((View)btn);
            ((View)btn).setEnabled(false);
        }

        this.llWeeks.addView(llWeek);
    }

    private void readyDaysOfWeek() {
        String[] arrOfDaysOfWeek = this.getResources().getStringArray(R.array.days_of_week);
        int j = 0;

        int i;
        for(i = this.startFrom; i < 7; ++j) {
            if (this.dayOfWeekLength > arrOfDaysOfWeek[i].length()) {
                this.tvDaysOfWeek[j].setText(arrOfDaysOfWeek[i]);
            } else {
                this.tvDaysOfWeek[j].setText(arrOfDaysOfWeek[i].substring(0, this.dayOfWeekLength));
            }

            ++i;
        }

        for(i = 0; i < this.startFrom; ++j) {
            if (this.dayOfWeekLength > arrOfDaysOfWeek[i].length()) {
                this.tvDaysOfWeek[j].setText(arrOfDaysOfWeek[i]);
            } else {
                this.tvDaysOfWeek[j].setText(arrOfDaysOfWeek[i].substring(0, this.dayOfWeekLength));
            }

            ++i;
        }

    }

    @SuppressLint("SetTextI18n")
    private void readyMonthAndYear() {
        switch(this.monthYearFormat) {
            case 0:
                //this.tvMonthYear.setText(this.MONTHS[this.selectedDate.get(2)].substring(0, 3) + " " + this.selectedDate.get(1));
                this.tvMonthYear.setText(this.MONTHS[this.selectedDate.get(2)].substring(0, 3));
                this.tvYear.setText(Integer.toString(this.selectedDate.get(1)));
                break;
            case 1:
                //this.tvMonthYear.setText(this.MONTHS[this.selectedDate.get(2)] + " " + this.selectedDate.get(1));
                this.tvMonthYear.setText(this.MONTHS[this.selectedDate.get(2)]);
                this.tvYear.setText(Integer.toString(this.selectedDate.get(1)));
        }

    }

    private View readyButton(final int date) {
        final Object btn;
        if (this.mapDescToProp != null) {
            Property prop = (Property)this.mapDescToProp.get("default");
            if (this.mapDateToDesc != null && this.mapDateToDesc.get(new Integer(date)) != null && !this.mapDateToDesc.get(new Integer(date)).equals("default")) {
                prop = (Property)this.mapDescToProp.get(this.mapDateToDesc.get(new Integer(date)));
            }

            if (prop != null && prop.layoutResource != -1) {
                btn = LayoutInflater.from(this.context).inflate(prop.layoutResource, (ViewGroup)null);
                if (prop.dateTextViewResource != -1) {
                    ((TextView)((View)btn).findViewById(prop.dateTextViewResource)).setText("" + date);
                }

                ((View)btn).setEnabled(prop.enable);
            } else {
                btn = new Button(this.context);
                ((Button)btn).setText("" + date);
            }
        } else {
            btn = new Button(this.context);
            ((Button)btn).setText("" + date);
        }

        ((View)btn).setLayoutParams(new LayoutParams(0, -1, 1.0F));
        if (this.mapDateToTag != null) {
            ((View)btn).setTag(this.mapDateToTag.get(new Integer(date)));
        }

        ((View)btn).setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                MyCustomCalendar.this.selectedDate.set(5, date);
                if (MyCustomCalendar.this.listener != null) {
                    MyCustomCalendar.this.listener.onDateSelected((View)btn, MyCustomCalendar.this.selectedDate, MyCustomCalendar.this.mapDateToDesc.get(new Integer(date)));
                }

                if (MyCustomCalendar.this.selectedButton != null) {
                    MyCustomCalendar.this.selectedButton.setSelected(false);
                }

                ((View)btn).setSelected(true);
                MyCustomCalendar.this.selectedButton = (View)btn;
            }
        });
        if (this.selectedDate.get(5) == date) {
            ((View)btn).setSelected(true);
            this.selectedButton = (View)btn;
        }

        return (View)btn;
    }

    public void setMonthYearFormat(int monthYearFormat) {
        this.monthYearFormat = monthYearFormat;
        this.readyMonthAndYear();
    }

    public void setDayOfWeekLength(int length) {
        this.dayOfWeekLength = length;
        this.readyDaysOfWeek();
    }

    public void setRowHeight(float rowHeight) {
        if (rowHeight > 0.0F) {
            this.rowHeight = rowHeight;
            this.setAll();
        }

    }

    public void setDayOfWeekStartFrom(int whichDay) {
        this.startFrom = whichDay;
        this.setAll();
    }

    public void setNavigationButtonDrawable(int whichButton, int resourceId) {
        switch(whichButton) {
            case -1:
                this.butLeft.setImageResource(resourceId);
                break;
            case 1:
                this.butRight.setImageResource(resourceId);
        }

    }

    public void setNavigationButtonDrawable(int whichButton, Drawable drawable) {
        switch(whichButton) {
            case -1:
                this.butLeft.setImageDrawable(drawable);
                break;
            case 1:
                this.butRight.setImageDrawable(drawable);
        }

    }

    public void setDate(Calendar calendar) {
        this.selectedDate = calendar;
        this.setAll();
    }

    public void setDate(Calendar calendar, Map<Integer, Object> mapDateToDesc) {
        this.selectedDate = calendar;
        this.mapDateToDesc = mapDateToDesc;
        this.setAll();
    }

    public void setDate(Calendar calendar, Map<Integer, Object> mapDateToDesc, Map<Integer, Object> mapDateToTag) {
        this.selectedDate = calendar;
        this.mapDateToDesc = mapDateToDesc;
        this.mapDateToTag = mapDateToTag;
        this.setAll();
    }

    public void setMapDescToProp(Map<Object, Property> mapDescToProp) {
        this.mapDescToProp = mapDescToProp;
        this.setAll();
    }

    public void setOnDateSelectedListener(OnDateSelectedListener listener) {
        this.listener = listener;
        this.readyMonthAndYear();
    }

    public void setOnNavigationButtonClickedListener(int whichButton, OnNavigationButtonClickedListener listener) {
        if (whichButton == -1) {
            this.leftButtonListener = listener;
        } else if (whichButton == 1) {
            this.rightButtonListener = listener;
        }

    }

    public void setNavigationButtonEnabled(int whichButton, boolean enable) {
        if (whichButton == -1) {
            this.butLeft.setEnabled(enable);
        } else if (whichButton == 1) {
            this.butRight.setEnabled(enable);
        }

    }

    public TextView getMonthYearTextView() {
        return this.tvMonthYear;
    }

    public View[] getAllViews() {
        return this.btnAll;
    }

    public Calendar getSelectedDate() {
        return this.selectedDate;
    }
}

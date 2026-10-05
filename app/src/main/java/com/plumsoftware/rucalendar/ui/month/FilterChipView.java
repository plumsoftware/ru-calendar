package com.plumsoftware.rucalendar.ui.month;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.InsetDrawable;
import android.graphics.drawable.RippleDrawable;
import android.util.TypedValue;
import android.view.Gravity;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;
import androidx.core.view.ViewCompat;

import com.plumsoftware.rucalendar.R;
import com.plumsoftware.rucalendar.data.EventType;
import com.plumsoftware.rucalendar.ui.MarkerDrawable;
import com.plumsoftware.rucalendar.ui.TypeColors;

/**
 * Чип фильтра типа (п. 4.1). Область нажатия 44 dp, видимая капсула — 36 dp.
 * Выключенный чип — пунктирная обводка и зачёркнутая подпись.
 */
public class FilterChipView extends LinearLayout {
    private final EventType type;
    private final TextView label;
    private final float density;
    private boolean checked = true;

    public FilterChipView(Context context, EventType type) {
        super(context);
        this.type = type;
        density = getResources().getDisplayMetrics().density;
        setOrientation(HORIZONTAL);
        setGravity(Gravity.CENTER_VERTICAL);
        setMinimumHeight(dp(44));
        setPadding(dp(12), 0, dp(14), 0);
        setClickable(true);
        setFocusable(true);

        ImageView marker = new ImageView(context);
        int markerSize = type == EventType.USER ? dp(12) : (type == EventType.MEMORIAL ? dp(8) : dp(10));
        int color = TypeColors.markerColor(context, type, 0);
        marker.setImageDrawable(new MarkerDrawable(type, color, markerSize, density));
        marker.setScaleType(ImageView.ScaleType.CENTER);
        addView(marker, new LayoutParams(dp(12), dp(12)));

        label = new TextView(context);
        label.setText(type.filterLabel);
        label.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        label.setTypeface(ResourcesCompat.getFont(context, R.font.golos_text_medium));
        label.setSingleLine(true);
        LayoutParams lp = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        lp.setMarginStart(dp(8));
        addView(label, lp);

        setContentDescription(type.filterLabel);
        applyState();
    }

    public EventType getType() {
        return type;
    }

    public boolean isChecked() {
        return checked;
    }

    public void setChecked(boolean checked) {
        if (this.checked == checked) return;
        this.checked = checked;
        applyState();
    }

    private void applyState() {
        Context context = getContext();
        GradientDrawable capsule = new GradientDrawable();
        capsule.setCornerRadius(dp(18));
        if (checked) {
            capsule.setColor(ContextCompat.getColor(context, R.color.ds_surface));
            capsule.setStroke(dp(1), ContextCompat.getColor(context, R.color.ds_outline));
            label.setTextColor(ContextCompat.getColor(context, R.color.ds_text_primary));
            label.setPaintFlags(label.getPaintFlags() & ~Paint.STRIKE_THRU_TEXT_FLAG);
        } else {
            capsule.setColor(Color.TRANSPARENT);
            capsule.setStroke(dp(1), ContextCompat.getColor(context, R.color.ds_outline_dashed), dp(4), dp(3));
            label.setTextColor(ContextCompat.getColor(context, R.color.ds_text_secondary));
            label.setPaintFlags(label.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
        }
        GradientDrawable mask = new GradientDrawable();
        mask.setCornerRadius(dp(18));
        mask.setColor(Color.WHITE);
        Drawable content = new InsetDrawable(capsule, 0, dp(4), 0, dp(4));
        Drawable maskInset = new InsetDrawable(mask, 0, dp(4), 0, dp(4));
        setBackground(new RippleDrawable(ColorStateList.valueOf(0x1F000000), content, maskInset));

        ViewCompat.setStateDescription(this, context.getString(checked ? R.string.filter_on : R.string.filter_off));
        setSelected(checked);
    }

    private int dp(float value) {
        return Math.round(value * density);
    }
}

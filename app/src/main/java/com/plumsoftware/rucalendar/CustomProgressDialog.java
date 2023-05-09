package com.plumsoftware.rucalendar;

import android.app.Dialog;
import android.content.Context;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;

import com.plumsoftware.rucalendar.R;

public class CustomProgressDialog extends Dialog {
    public CustomProgressDialog(@NonNull Context context) {
        super(context);

        WindowManager.LayoutParams params = getWindow().getAttributes();
        params.gravity = Gravity.CENTER;
        getWindow().setAttributes(params);

        setCancelable(false);
        setOnCancelListener(null);
        setTitle(null);

        View view = LayoutInflater.from(context).inflate(R.layout.progress_dialog_layout, (ConstraintLayout) findViewById(R.id.layout), false);

        setContentView(view);
    }
}

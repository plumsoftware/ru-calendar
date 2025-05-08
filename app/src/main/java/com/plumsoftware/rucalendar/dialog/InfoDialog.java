package com.plumsoftware.rucalendar.dialog;

import android.app.Dialog;
import android.content.Context;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;

import androidx.annotation.NonNull;

import com.plumsoftware.rucalendar.R;

public class InfoDialog extends Dialog {
    public InfoDialog(@NonNull Context context) {
        super(context);

        WindowManager.LayoutParams params = getWindow().getAttributes();
        params.gravity = Gravity.CENTER;
        params.width = 350;
        getWindow().setAttributes(params);

        setCancelable(true);
        setOnCancelListener(null);
        setTitle(null);

        View view = LayoutInflater.from(context).inflate(R.layout.info_layout, findViewById(R.id.infoL), false);

        Button closeInfoDialog = view.findViewById(R.id.closeInfoDialog);
        closeInfoDialog.setOnClickListener(view_ -> this.dismiss());

        setContentView(view);
    }
}

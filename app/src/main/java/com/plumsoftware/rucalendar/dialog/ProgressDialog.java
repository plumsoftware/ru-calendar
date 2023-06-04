package com.plumsoftware.rucalendar.dialog;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;

import com.plumsoftware.rucalendar.R;

public class ProgressDialog {
    private Dialog dialog;

    public void showDialog(Context context) {
        @SuppressLint("InflateParams") View view = LayoutInflater.from(context).inflate(R.layout.progress_dialog, null, false);
        dialog = new Dialog(context);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setCancelable(false);
        dialog.setContentView(view);
        dialog.show();
    }

    public void dismiss() {
        try {
            if (dialog != null)
                dialog.dismiss();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
package com.plumsoftware.rucalendar.ui.reminder;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;

import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

/** Разрешение на уведомления запрашивается при первом включении напоминания (п. 4.3). */
public final class NotificationPermission {
    private NotificationPermission() {
    }

    /** Нужно ли запрашивать системное разрешение (Android 13+). */
    public static boolean needsRequest(Context context) {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED;
    }

    public static boolean isGranted(Context context) {
        return !needsRequest(context) && NotificationManagerCompat.from(context).areNotificationsEnabled();
    }

    /** Системные настройки уведомлений приложения. */
    public static void openSettings(Context context) {
        Intent intent;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            intent = new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                    .putExtra(Settings.EXTRA_APP_PACKAGE, context.getPackageName());
        } else {
            intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    Uri.fromParts("package", context.getPackageName(), null));
        }
        context.startActivity(intent);
    }
}

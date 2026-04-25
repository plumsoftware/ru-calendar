package com.plumsoftware.rucalendar.services;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import androidx.core.app.NotificationCompat;
import com.plumsoftware.rucalendar.R;
import com.plumsoftware.rucalendar.activities.MainActivity;
import android.Manifest;
import android.content.pm.PackageManager;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationManagerCompat;

public class EventNotificationScheduler extends BroadcastReceiver {
    private static final String CHANNEL_ID = "rucalendar_event_reminders";

    @Override
    public void onReceive(Context context, Intent intent) {
        // Получаем переданное название праздника и его уникальный ID
        String title = intent.getStringExtra("notification_title");
        if (title == null) title = "Напоминание о событии";

        int notificationId = intent.getIntExtra("notification_id", (int) System.currentTimeMillis());

        // Подготавливаем интент для открытия приложения при клике
        Intent appIntent = new Intent(context, MainActivity.class);
        appIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                notificationId,
                appIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);

        // Создание канала для Android 8+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Напоминания о событиях",
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription("Уведомления о выбранных событиях");
            channel.enableVibration(true);
            notificationManager.createNotificationChannel(channel);
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setContentTitle("Напоминание")
                .setContentText(title)
                .setSmallIcon(R.mipmap.ic_launcher_round)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent);

        // В Android 13+ нужно проверять наличие разрешения POST_NOTIFICATIONS
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
                notificationManager.notify(notificationId, builder.build());
            }
        } else {
            notificationManager.notify(notificationId, builder.build());
        }
    }
}
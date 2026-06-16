package com.plumsoftware.rucalendar.services;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.RingtoneManager;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.plumsoftware.rucalendar.R;

public class MyNotificationWorker extends Worker {

    private static final String CHANNEL_ID = "reminder_channel";

    public MyNotificationWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        String title = getInputData().getString("notification_title");
        int notificationId = getInputData().getInt("notification_id", (int) System.currentTimeMillis());
        long eventTime = getInputData().getLong("event_time", -1L);
        String eventName = getInputData().getString("event_name");
        String eventDesc = getInputData().getString("event_desc");
        String eventColor = getInputData().getString("event_color");
        if (title == null) title = "Напоминание";
        Uri defaultSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);

        Uri deepLink = new Uri.Builder()
                .scheme("rucalendar")
                .authority("event")
                .appendQueryParameter("name", eventName)
                .appendQueryParameter("desc", eventDesc)
                .appendQueryParameter("color", eventColor)
                .appendQueryParameter("time", String.valueOf(eventTime))
                .build();
        Intent deepLinkIntent = new Intent(Intent.ACTION_VIEW, deepLink);
        deepLinkIntent.setPackage(getApplicationContext().getPackageName());
        deepLinkIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);

        android.app.PendingIntent pendingIntent = android.app.PendingIntent.getActivity(
                getApplicationContext(),
                notificationId,
                deepLinkIntent,
                android.app.PendingIntent.FLAG_UPDATE_CURRENT | android.app.PendingIntent.FLAG_IMMUTABLE
        );

        createNotificationChannel();
        NotificationCompat.Builder builder = new NotificationCompat.Builder(getApplicationContext(), CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_round_circle_notifications)
                .setSound(defaultSound)
                .setVibrate(new long[]{200L, 200L})
                .setTicker("Наопминание")
                .setContentTitle("Напоминание")
                .setContentText(title)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(getApplicationContext());
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU
                && ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            return Result.success();
        }
        notificationManager.notify(notificationId, builder.build());

        return Result.success();
    }

    private void createNotificationChannel() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            CharSequence name = "Напоминания";
            String description = "Канал для напоминаний";
            int importance = android.app.NotificationManager.IMPORTANCE_DEFAULT;
            android.app.NotificationChannel channel = new android.app.NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);
            android.app.NotificationManager notificationManager = getApplicationContext().getSystemService(android.app.NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }
}
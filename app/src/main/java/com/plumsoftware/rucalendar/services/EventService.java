package com.plumsoftware.rucalendar.services;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Handler;
import android.os.IBinder;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.plumsoftware.rucalendar.R;
import com.plumsoftware.rucalendar.activities.MainActivity;
import com.plumsoftware.rucalendar.events.CelebrationItem;
import com.plumsoftware.rucalendar.events.Celebrations;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class EventService extends Service {
    private final static int INTERVAL = 6 * 60 * 60 * 1000; // интервал, через который сервис выполняет задачу
//    private final static int INTERVAL = 5000; // интервал, через который сервис выполняет задачу

    private List<CelebrationItem> celebrations = new ArrayList<>();
    private Calendar calendar = Calendar.getInstance();

    private Handler mHandler = new Handler();

    private Runnable mRunnable = new Runnable() {
        @Override
        public void run() {
            // здесь выполняются необходимые операции, например, подключение к OpenWeatherMap API и получения данных
            // после получения данных можно вывести уведомление в статусную строку
//            sendNotification(WeatherService.this, "Заголовок", "Сообщение", 1, R.drawable.ic_sun);
            sendNotification();
            mHandler.postDelayed(mRunnable, INTERVAL);
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        mHandler.post(mRunnable);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mHandler.removeCallbacks(mRunnable);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
//        region::Get data
//        celebrations.clear();
//        Celebrations celebrationsClass = new Celebrations(calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH));
//
//        String name1 = "";
//        String descS1 = "";
//        String color = "";
//        long timeInMillis = calendar.getTimeInMillis();
//
//        try {
//            String[] split = celebrationsClass.getDescription().split("~del");
//            for (String s : split) {
//                name1 = s.split("~")[0];
//                descS1 = s.split("~")[1];
//                color = "#F57F17";
//                celebrations.add(new CelebrationItem(name1, descS1, color, timeInMillis));
//            }
//        } catch (IndexOutOfBoundsException e) {
//            e.printStackTrace();
//        }
////        endregion
//        if (celebrations.size() != 0) {
        Intent i = new Intent(this, MainActivity.class);
        i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

        Notification notification = new NotificationCompat.Builder(EventService.this, "com.plumsoftware.rucalendar.default")
                .setContentTitle("\uD83C\uDF89 Не забудьте о праздниках!")
                .setContentText("\uD83C\uDF8A Узнайте детали в нашем календаре!")
                .setStyle(new NotificationCompat.BigTextStyle()
                        .bigText("\uD83C\uDF8A Узнайте детали в нашем календаре!"))
//                .setSmallIcon(R.drawable.ic_round_circle_notifications)
                .setSmallIcon(R.mipmap.ic_launcher_round)
                .setAutoCancel(true)
                .setContentIntent(PendingIntent.getActivity(EventService.this, 0, i, 0))
                .build();

//             Start the service in the foreground
        startForeground(1, notification);
//        }

        return START_STICKY; // Сервис будет перезапущен после выхода из-за нехватки ресурсов
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void sendNotification() {
        celebrations.clear();
        Celebrations celebrationsClass = new Celebrations(calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH));

        String name1 = "";
        String descS1 = "";
        String color = "";
        long timeInMillis = calendar.getTimeInMillis();

        try {
            String[] split = celebrationsClass.getDescription().split("~del");
            for (String s : split) {
                name1 = s.split("~")[0];
                descS1 = s.split("~")[1];
                color = "#F57F17";
                celebrations.add(new CelebrationItem(name1, descS1, color, timeInMillis));
            }
        } catch (IndexOutOfBoundsException e) {
            e.printStackTrace();
        }

        if (celebrations.size() != 0) {
            // Create notification builder
            NotificationCompat.Builder builder = new NotificationCompat.Builder(this, "com.plumsoftware.rucalendar.default")
//                    .setSmallIcon(R.drawable.ic_round_circle_notifications)
                    .setSmallIcon(R.mipmap.ic_launcher_round)
                    .setStyle(new NotificationCompat.BigTextStyle()
                            .bigText("Узнать подробнее про " + celebrations.get(0).getName() + " можно в приложении"))
                    .setContentTitle("\uD83D\uDD14Сегодня события (" + Integer.toString(celebrations.size()) + ")")
                    .setContentText("Узнать подробнее про " + celebrations.get(0).getName() + " можно в приложении")
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setAutoCancel(true)
                    .setCategory(NotificationCompat.CATEGORY_MESSAGE);

            // Create intent for MainActivity
            Intent intent = new Intent(this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, 0);

            // Set content information with intent
            builder.setContentIntent(pendingIntent);

            NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

            // Since android Oreo notification channel is needed
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                NotificationChannel channel = new NotificationChannel("com.plumsoftware.rucalendar.default",
                        "rucalendar channel",
                        NotificationManager.IMPORTANCE_HIGH);
                channel.setDescription("");
                channel.enableLights(true);
                channel.setLightColor(Color.BLUE);
                channel.enableVibration(true);
                channel.setVibrationPattern(new long[]{100, 200, 300, 400, 500, 400, 300, 200});
                notificationManager.createNotificationChannel(channel);
            }

            Notification notification = builder.build();

            // Send notification
            notificationManager.notify(1, notification);
        }
    }

}

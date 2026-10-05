package com.plumsoftware.rucalendar.reminders;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.plumsoftware.rucalendar.R;
import com.plumsoftware.rucalendar.activities.MainActivity;
import com.plumsoftware.rucalendar.data.EventType;
import com.plumsoftware.rucalendar.data.Holiday;
import com.plumsoftware.rucalendar.data.HolidayRepository;
import com.plumsoftware.rucalendar.data.ReminderRepository;
import com.plumsoftware.rucalendar.data.RuDates;
import com.plumsoftware.rucalendar.data.UserEvent;
import com.plumsoftware.rucalendar.data.UserEventRepository;
import com.plumsoftware.rucalendar.ui.MarkerBitmaps;
import com.plumsoftware.rucalendar.ui.ShareText;

import java.time.LocalDate;

/** Показывает напоминание и планирует следующее (п. 5.2). */
public class ReminderWorker extends Worker {
    public static final String CHANNEL_ID = "holiday_reminders";

    public ReminderWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        Context context = getApplicationContext();
        String dateString = getInputData().getString(ReminderScheduler.KEY_EVENT_DATE);
        if (dateString == null) return Result.success();
        LocalDate eventDate = LocalDate.parse(dateString);

        String holidayId = getInputData().getString(ReminderScheduler.KEY_HOLIDAY_ID);
        if (holidayId != null) {
            ReminderRepository.Reminder reminder = ReminderScheduler.reminderFor(context, holidayId);
            Holiday holiday = HolidayRepository.get(context).byId(holidayId);
            if (reminder == null || holiday == null) return Result.success();
            Intent share = ShareText.intent(holiday, eventDate);
            notify(context, holiday.id, holiday.type, 0, holiday.name, eventDate, reminder.daysBefore, share);
            // Ежегодное напоминание — на следующий год
            ReminderScheduler.schedule(context, reminder);
            return Result.success();
        }

        String userEventId = getInputData().getString(ReminderScheduler.KEY_USER_EVENT_ID);
        if (userEventId != null) {
            UserEvent event = UserEventRepository.get(context).byId(userEventId);
            if (event == null || event.reminderDaysBefore == UserEvent.REMINDER_OFF) return Result.success();
            Intent share = ShareText.intent(event.title, event.description, eventDate);
            notify(context, event.id, EventType.USER, event.color, event.title, eventDate, event.reminderDaysBefore, share);
            if (event.yearly) ReminderScheduler.scheduleUserEvent(context, event);
        }
        return Result.success();
    }

    private static void notify(Context context, String eventId, EventType type, int userColor, String title,
                               LocalDate eventDate, int daysBefore, Intent shareIntent) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        createChannel(context);
        int id = eventId.hashCode();

        // Нажатие открывает карточку дня
        Intent open = new Intent(Intent.ACTION_VIEW, dayUri(eventDate, eventId), context, MainActivity.class);
        open.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent openPending = PendingIntent.getActivity(context, id, open,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Intent share = Intent.createChooser(shareIntent, null);
        share.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        PendingIntent sharePending = PendingIntent.getActivity(context, id + 1, share,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        // Иконка уведомления — маркер типа на плашке его цвета
        Bitmap icon = MarkerBitmaps.plate(context, type, userColor, 40);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_round_circle_notifications)
                .setLargeIcon(icon)
                .setContentTitle(prefix(daysBefore) + " — " + title)
                .setContentText(RuDates.weekdayDayMonth(eventDate))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(openPending)
                .addAction(0, "Поделиться", sharePending)
                .setAutoCancel(true);
        NotificationManagerCompat.from(context).notify(id, builder.build());
    }

    private static String prefix(int daysBefore) {
        if (daysBefore == 0) return "Сегодня";
        if (daysBefore == 1) return "Завтра";
        if (daysBefore == 7) return "Через неделю";
        return "Через " + RuDates.days(daysBefore);
    }

    /** rucalendar://day?date=2026-11-04&id=1104-1 — открывает карточку дня. */
    public static Uri dayUri(LocalDate date, String eventId) {
        return new Uri.Builder().scheme("rucalendar").authority("day")
                .appendQueryParameter("date", date.toString())
                .appendQueryParameter("id", eventId)
                .build();
    }

    private static void createChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID,
                    "Напоминания о праздниках", NotificationManager.IMPORTANCE_DEFAULT);
            context.getSystemService(NotificationManager.class).createNotificationChannel(channel);
        }
    }
}

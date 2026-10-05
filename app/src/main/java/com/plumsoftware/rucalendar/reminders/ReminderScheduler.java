package com.plumsoftware.rucalendar.reminders;

import android.content.Context;

import androidx.annotation.Nullable;
import androidx.work.Data;
import androidx.work.ExistingWorkPolicy;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import com.plumsoftware.rucalendar.data.Holiday;
import com.plumsoftware.rucalendar.data.HolidayRepository;
import com.plumsoftware.rucalendar.data.ReminderRepository;
import com.plumsoftware.rucalendar.data.UserEvent;
import com.plumsoftware.rucalendar.data.UserEventRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.concurrent.TimeUnit;

/**
 * Планирует напоминания о праздниках и собственных событиях через WorkManager: задачи переживают перезагрузку.
 * Ежегодное событие после срабатывания планируется на следующий год, пока напоминание не выключат (п. 5.2).
 */
public final class ReminderScheduler {
    static final String KEY_HOLIDAY_ID = "holiday_id";
    static final String KEY_USER_EVENT_ID = "user_event_id";
    static final String KEY_EVENT_DATE = "event_date";

    private ReminderScheduler() {
    }

    /** Включить или обновить напоминание о празднике и запланировать ближайшее срабатывание. */
    public static void enable(Context context, ReminderRepository.Reminder reminder) {
        new ReminderRepository(context).put(reminder);
        schedule(context, reminder);
    }

    public static void disable(Context context, String holidayId) {
        new ReminderRepository(context).remove(holidayId);
        WorkManager.getInstance(context).cancelUniqueWork(holidayWork(holidayId));
    }

    /** Запланировать напоминание о собственном событии по его настройкам или отменить, если оно выключено. */
    public static void scheduleUserEvent(Context context, UserEvent event) {
        WorkManager wm = WorkManager.getInstance(context);
        if (event.reminderDaysBefore == UserEvent.REMINDER_OFF) {
            wm.cancelUniqueWork(userWork(event.id));
            return;
        }
        LocalDate eventDate = nextUserEventDate(event, LocalDateTime.now());
        if (eventDate == null) {
            // Разовое событие уже прошло
            wm.cancelUniqueWork(userWork(event.id));
            return;
        }
        Data input = new Data.Builder()
                .putString(KEY_USER_EVENT_ID, event.id)
                .putString(KEY_EVENT_DATE, eventDate.toString())
                .build();
        enqueue(context, userWork(event.id), triggerTime(eventDate, event.reminderDaysBefore, event.reminderMinutes), input);
    }

    public static void cancelUserEvent(Context context, String eventId) {
        WorkManager.getInstance(context).cancelUniqueWork(userWork(eventId));
    }

    /** Перепланировать все включённые напоминания, например после смены часового пояса. */
    public static void rescheduleAll(Context context) {
        for (ReminderRepository.Reminder reminder : new ReminderRepository(context).all()) {
            schedule(context, reminder);
        }
        for (UserEvent event : UserEventRepository.get(context).all()) {
            scheduleUserEvent(context, event);
        }
    }

    static void schedule(Context context, ReminderRepository.Reminder reminder) {
        Holiday holiday = HolidayRepository.get(context).byId(reminder.holidayId);
        if (holiday == null) return;
        LocalDate eventDate = nextEventDate(holiday, reminder, LocalDateTime.now());
        Data input = new Data.Builder()
                .putString(KEY_HOLIDAY_ID, holiday.id)
                .putString(KEY_EVENT_DATE, eventDate.toString())
                .build();
        enqueue(context, holidayWork(holiday.id), triggerTime(eventDate, reminder), input);
    }

    private static void enqueue(Context context, String name, LocalDateTime trigger, Data input) {
        long triggerAt = trigger.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
        long delay = Math.max(0, triggerAt - System.currentTimeMillis());
        OneTimeWorkRequest request = new OneTimeWorkRequest.Builder(ReminderWorker.class)
                .setInitialDelay(delay, TimeUnit.MILLISECONDS)
                .setInputData(input)
                .build();
        WorkManager.getInstance(context).enqueueUniqueWork(name, ExistingWorkPolicy.REPLACE, request);
    }

    /** Дата праздника, к которой относится ближайшее срабатывание после now. */
    public static LocalDate nextEventDate(Holiday holiday, ReminderRepository.Reminder reminder, LocalDateTime now) {
        for (int year = now.getYear(); ; year++) {
            LocalDate date = holiday.dateIn(year);
            if (triggerTime(date, reminder).isAfter(now)) return date;
        }
    }

    /** Дата собственного события для ближайшего срабатывания; null — разовое событие уже прошло. */
    @Nullable
    public static LocalDate nextUserEventDate(UserEvent event, LocalDateTime now) {
        int daysBefore = Math.max(0, event.reminderDaysBefore);
        if (!event.yearly) {
            return triggerTime(event.date, daysBefore, event.reminderMinutes).isAfter(now) ? event.date : null;
        }
        for (int year = Math.max(now.getYear(), event.date.getYear()); ; year++) {
            LocalDate date = occurrenceIn(event, year);
            if (triggerTime(date, daysBefore, event.reminderMinutes).isAfter(now)) return date;
        }
    }

    /** Ежегодное событие в указанном году; 29 февраля в невисокосный год — 28 февраля. */
    public static LocalDate occurrenceIn(UserEvent event, int year) {
        int month = event.date.getMonthValue();
        int day = event.date.getDayOfMonth();
        if (month == 2 && day == 29 && !LocalDate.of(year, 1, 1).isLeapYear()) day = 28;
        return LocalDate.of(year, month, day);
    }

    /** Когда придёт уведомление для конкретной даты праздника. */
    public static LocalDateTime triggerTime(LocalDate eventDate, ReminderRepository.Reminder reminder) {
        return triggerTime(eventDate, reminder.daysBefore, reminder.minutes);
    }

    public static LocalDateTime triggerTime(LocalDate eventDate, int daysBefore, int minutes) {
        return eventDate.minusDays(daysBefore).atStartOfDay().plusMinutes(minutes);
    }

    @Nullable
    static ReminderRepository.Reminder reminderFor(Context context, String holidayId) {
        return new ReminderRepository(context).get(holidayId);
    }

    private static String holidayWork(String holidayId) {
        return "holiday_reminder_" + holidayId;
    }

    private static String userWork(String eventId) {
        return "user_event_reminder_" + eventId;
    }
}

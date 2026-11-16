package com.example.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class SessionReminderReceiver : BroadcastReceiver() {
  override fun onReceive(context: Context, intent: Intent) {
    val bookingId = intent.getStringExtra(BookingNotificationHelper.EXTRA_BOOKING_ID)
    val subject = intent.getStringExtra(EXTRA_SUBJECT) ?: "Tutoring Session"
    val partnerName = intent.getStringExtra(EXTRA_PARTNER_NAME) ?: "Scholarly Expert"
    val timeLabel = intent.getStringExtra(EXTRA_TIME_LABEL) ?: "soon"

    BookingNotificationHelper.showBookingAlert(
      context = context,
      title = "Upcoming Session Reminder: $subject",
      body = "Your 1-on-1 session with $partnerName starts at $timeLabel. Tap to enter your classroom.",
      bookingId = bookingId,
      category = PushNotificationCategory.SESSION_REMINDER,
    )
  }

  companion object {
    const val EXTRA_SUBJECT = "extra_subject"
    const val EXTRA_PARTNER_NAME = "extra_partner_name"
    const val EXTRA_TIME_LABEL = "extra_time_label"
  }
}

object SessionReminderScheduler {
  fun scheduleReminder(
    context: Context,
    bookingId: String,
    subject: String,
    partnerName: String,
    dateLabel: String,
    timeLabel: String,
    leadMinutes: Int = 15,
  ) {
    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
    val triggerAtMillis = computeReminderTriggerMillis(dateLabel, timeLabel, leadMinutes)

    val intent = Intent(context, SessionReminderReceiver::class.java).apply {
      putExtra(BookingNotificationHelper.EXTRA_BOOKING_ID, bookingId)
      putExtra(SessionReminderReceiver.EXTRA_SUBJECT, subject)
      putExtra(SessionReminderReceiver.EXTRA_PARTNER_NAME, partnerName)
      putExtra(SessionReminderReceiver.EXTRA_TIME_LABEL, timeLabel)
    }

    val pendingIntent = PendingIntent.getBroadcast(
      context,
      bookingId.hashCode(),
      intent,
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    runCatching {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
      } else {
        alarmManager.set(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
      }
    }
  }

  fun cancelReminder(context: Context, bookingId: String) {
    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
    val intent = Intent(context, SessionReminderReceiver::class.java)
    val pendingIntent = PendingIntent.getBroadcast(
      context,
      bookingId.hashCode(),
      intent,
      PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE,
    ) ?: return
    runCatching { alarmManager.cancel(pendingIntent) }
  }

  internal fun computeReminderTriggerMillis(
    dateLabel: String,
    timeLabel: String,
    leadMinutes: Int = 15,
    nowMillis: Long = System.currentTimeMillis(),
  ): Long {
    val parsedStart = runCatching {
      val cleanDate = dateLabel.substringAfter(",", dateLabel).trim()
      val year = Calendar.getInstance().get(Calendar.YEAR)
      val fmt = SimpleDateFormat("MMM d yyyy h:mm a", Locale.US)
      fmt.parse("$cleanDate $year ${timeLabel.trim()}")?.time
    }.getOrNull()

    val candidate = if (parsedStart != null) {
      parsedStart - leadMinutes * 60_000L
    } else {
      nowMillis + 15 * 60_000L
    }

    // If parsed slot is in the past (e.g., template demo dates), schedule a short 5-minute reminder fallback
    return if (candidate > nowMillis + 10_000L) candidate else nowMillis + 5 * 60_000L
  }
}

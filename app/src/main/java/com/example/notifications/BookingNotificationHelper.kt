package com.example.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R

enum class PushNotificationCategory(
  val channelId: String,
  val channelName: String,
  val channelDescription: String,
) {
  BOOKING_CONFIRMATION(
    channelId = "scholarly_bookings_channel",
    channelName = "Booking Confirmations & Payouts",
    channelDescription = "Real-time FCM push alerts when tutoring sessions are booked, rescheduled, or settled.",
  ),
  SESSION_REMINDER(
    channelId = "scholarly_reminders_channel",
    channelName = "Upcoming Session Reminders",
    channelDescription = "Timely alerts before your upcoming tutoring session begins.",
  ),
  NEW_MESSAGE(
    channelId = "scholarly_messages_channel",
    channelName = "New Session Messages",
    channelDescription = "Real-time notifications when a student or tutor posts a session message.",
  ),
  ;

  companion object {
    fun fromType(type: String?): PushNotificationCategory = when (type) {
      "session_reminder" -> SESSION_REMINDER
      "new_message" -> NEW_MESSAGE
      else -> BOOKING_CONFIRMATION
    }
  }
}

object BookingNotificationHelper {
  const val EXTRA_BOOKING_ID = "extra_booking_id"
  const val EXTRA_NOTIFICATION_TYPE = "extra_notification_type"

  fun ensureAllChannels(context: Context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
      PushNotificationCategory.entries.forEach { category ->
        if (manager.getNotificationChannel(category.channelId) == null) {
          val channel = NotificationChannel(
            category.channelId,
            category.channelName,
            NotificationManager.IMPORTANCE_HIGH,
          ).apply {
            description = category.channelDescription
            enableVibration(true)
          }
          manager.createNotificationChannel(channel)
        }
      }
    }
  }

  fun hasNotificationPermission(context: Context): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.POST_NOTIFICATIONS,
      ) == PackageManager.PERMISSION_GRANTED
    } else {
      true
    }
  }

  fun showBookingAlert(
    context: Context,
    title: String,
    body: String,
    bookingId: String? = null,
    category: PushNotificationCategory = PushNotificationCategory.BOOKING_CONFIRMATION,
  ) {
    if (!hasNotificationPermission(context)) return
    ensureAllChannels(context)

    val launchIntent = Intent(context, MainActivity::class.java).apply {
      flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
      if (!bookingId.isNullOrBlank()) {
        putExtra(EXTRA_BOOKING_ID, bookingId)
      }
      putExtra(EXTRA_NOTIFICATION_TYPE, category.name)
    }

    val pendingIntent = PendingIntent.getActivity(
      context,
      (bookingId?.hashCode() ?: category.ordinal),
      launchIntent,
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    val notification = NotificationCompat.Builder(context, category.channelId)
      .setSmallIcon(R.mipmap.ic_launcher)
      .setContentTitle(title)
      .setContentText(body)
      .setStyle(NotificationCompat.BigTextStyle().bigText(body))
      .setPriority(NotificationCompat.PRIORITY_HIGH)
      .setContentIntent(pendingIntent)
      .setAutoCancel(true)
      .build()

    runCatching {
      val notificationId = ((bookingId?.hashCode() ?: 0) * 31 + System.currentTimeMillis().toInt())
        .let { if (it == Int.MIN_VALUE) 1 else kotlin.math.abs(it) }
      NotificationManagerCompat.from(context).notify(notificationId, notification)
    }
  }
}

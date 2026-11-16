package com.example.notifications

import android.util.Log
import com.example.R
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import java.util.UUID

class ScholarlyFirebaseMessagingService : FirebaseMessagingService() {

  override fun onNewToken(token: String) {
    super.onNewToken(token)
    Log.d(TAG, "Refreshed FCM registration token")
    val uid = Firebase.auth.currentUser?.uid ?: return
    val databaseId = applicationContext.getString(R.string.firestore_database_id)
    val db = FirebaseFirestore.getInstance(databaseId)
    val tokenDoc = db.collection("users").document(uid)
      .collection("fcm_tokens").document(FcmPushCoordinator.PRIMARY_TOKEN_DOC_ID)

    tokenDoc.get()
      .addOnSuccessListener { snap ->
        if (snap.exists()) {
          tokenDoc.update(
            mapOf(
              "token" to token.take(512),
              "deviceLabel" to FcmPushCoordinator.deviceLabel(),
              "updatedAt" to FieldValue.serverTimestamp(),
            )
          )
        } else {
          tokenDoc.set(
            mapOf(
              "userId" to uid,
              "token" to token.take(512),
              "deviceLabel" to FcmPushCoordinator.deviceLabel(),
              "role" to "student",
              "bookingConfirmationsEnabled" to true,
              "sessionRemindersEnabled" to true,
              "newMessagesEnabled" to true,
              "updatedAt" to FieldValue.serverTimestamp(),
            )
          )
        }
      }
      .addOnFailureListener { err ->
        Log.w(TAG, "Failed to persist refreshed FCM token: ${err.message}")
      }
  }

  override fun onMessageReceived(message: RemoteMessage) {
    super.onMessageReceived(message)
    val data = message.data
    val type = data["type"] ?: data["eventType"] ?: "booking_confirmation"
    val bookingId = data["bookingId"]?.takeIf { it.isNotBlank() }
    val title = message.notification?.title
      ?: data["title"]
      ?: defaultTitleForType(type)
    val body = message.notification?.body
      ?: data["body"]
      ?: "Tap to view your Scholarly tutoring update."

    val category = PushNotificationCategory.fromType(type)
    BookingNotificationHelper.showBookingAlert(
      context = applicationContext,
      title = title,
      body = body,
      bookingId = bookingId,
      category = category,
    )

    // Persist incoming FCM push into in-app notifications feed when signed in
    val uid = Firebase.auth.currentUser?.uid ?: return
    val allowedType = when (type) {
      "booking_confirmation", "session_reminder", "new_message", "session_update", "payout" -> type
      else -> "new_booking"
    }
    val databaseId = applicationContext.getString(R.string.firestore_database_id)
    val db = FirebaseFirestore.getInstance(databaseId)
    val notifId = "fcm_${UUID.randomUUID().toString().replace("-", "").take(12)}"
    val payload = mutableMapOf<String, Any>(
      "userId" to uid,
      "title" to title.take(100),
      "body" to body.take(300),
      "type" to allowedType,
      "isRead" to false,
      "createdAt" to FieldValue.serverTimestamp(),
    )
    if (bookingId != null) {
      payload["bookingId"] = bookingId.take(128)
    }
    db.collection("users").document(uid)
      .collection("notifications").document(notifId)
      .set(payload)
      .addOnFailureListener { err ->
        Log.w(TAG, "Failed to store incoming FCM alert: ${err.message}")
      }
  }

  private fun defaultTitleForType(type: String): String = when (type) {
    "session_reminder" -> "Upcoming Tutoring Session Reminder"
    "new_message" -> "New Session Message"
    else -> "Tutoring Booking Confirmed"
  }

  private companion object {
    const val TAG = "ScholarlyFCM"
  }
}

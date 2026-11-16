package com.example.notifications

import android.os.Build
import android.util.Log
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.tasks.await

object FcmPushCoordinator {
  const val PRIMARY_TOKEN_DOC_ID = "android_primary"
  const val TOPIC_ALL_USERS = "scholarly_all_users"
  const val TOPIC_STUDENTS = "scholarly_students"
  const val TOPIC_TUTORS = "scholarly_tutors"
  const val TOPIC_REMINDERS = "scholarly_session_reminders"

  fun deviceLabel(): String {
    val model = listOf(Build.MANUFACTURER, Build.MODEL)
      .filter { !it.isNullOrBlank() }
      .joinToString(" ")
      .ifBlank { "Android Device" }
    return "$model (API ${Build.VERSION.SDK_INT})".take(80)
  }

  suspend fun fetchRegistrationTokenOrNull(): String? {
    return runCatching {
      FirebaseMessaging.getInstance().token.await()
    }.onFailure { err ->
      Log.w(TAG, "FCM token unavailable in current environment: ${err.message}")
    }.getOrNull()
  }

  fun syncTopicSubscriptions(
    role: String,
    sessionRemindersEnabled: Boolean,
  ) {
    runCatching {
      val messaging = FirebaseMessaging.getInstance()
      messaging.subscribeToTopic(TOPIC_ALL_USERS)
      if (role == "tutor") {
        messaging.subscribeToTopic(TOPIC_TUTORS)
        messaging.unsubscribeFromTopic(TOPIC_STUDENTS)
      } else {
        messaging.subscribeToTopic(TOPIC_STUDENTS)
        messaging.unsubscribeFromTopic(TOPIC_TUTORS)
      }
      if (sessionRemindersEnabled) {
        messaging.subscribeToTopic(TOPIC_REMINDERS)
      } else {
        messaging.unsubscribeFromTopic(TOPIC_REMINDERS)
      }
    }.onFailure { err ->
      Log.w(TAG, "FCM topic sync skipped: ${err.message}")
    }
  }

  private const val TAG = "FcmPushCoordinator"
}

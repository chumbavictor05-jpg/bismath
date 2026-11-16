package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.model.BookingNotification
import com.example.data.model.ExpertApplication
import com.example.data.model.FcmDeviceToken
import com.example.data.model.PaymentTransaction
import com.example.data.model.PushDispatchEvent
import com.example.data.model.SavedPaymentMethod
import com.example.data.model.SavedTutor
import com.example.data.model.SessionMessage
import com.example.data.model.StudentProfile
import com.example.data.model.SubjectExpert
import com.example.data.model.TutorAvailabilityDay
import com.example.data.model.TutorPayout
import com.example.data.model.TutorReview
import com.example.data.model.TutoringSession
import com.example.data.remote.OperationType
import com.example.data.remote.handleFirestoreError
import com.example.notifications.FcmPushCoordinator
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.util.UUID

class TutoringRepository(
  private val db: FirebaseFirestore,
  private val auth: FirebaseAuth = Firebase.auth,
) {
  constructor(context: Context) : this(
    db = FirebaseFirestore.getInstance(
      context.applicationContext.getString(R.string.firestore_database_id)
    ),
    auth = Firebase.auth,
  )

  private fun requireUserId(): String {
    return auth.currentUser?.uid
      ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
  }

  private fun handleStreamError(
    error: Throwable,
    operationType: OperationType,
    path: String,
    expectedUid: String?,
  ) {
    val firestoreError = generateSequence(error) { it.cause }
      .filterIsInstance<FirebaseFirestoreException>()
      .firstOrNull()
    if (firestoreError == null && error is CancellationException) throw error
    val exceptionToLog = firestoreError ?: (error as? Exception)
    if (exceptionToLog != null && (expectedUid == null || auth.currentUser?.uid == expectedUid)) {
      handleFirestoreError(exceptionToLog, operationType, path, auth)
    }
  }

  // --- Student Profile ---

  fun observeProfile(userId: String): Flow<StudentProfile?> = flow {
    val docRef = db.collection("users").document(userId)
    emitAll(
      docRef.snapshots()
        .map { snapshot ->
          if (snapshot.exists()) {
            snapshot.toObject(
              StudentProfile::class.java,
              DocumentSnapshot.ServerTimestampBehavior.ESTIMATE,
            )
          } else {
            null
          }
        }
        .catch { error ->
          handleStreamError(error, OperationType.GET, docRef.path, userId)
          throw error
        }
    )
  }

  suspend fun upsertProfile(
    displayName: String,
    email: String?,
    gradeLevel: String,
    learningGoals: String,
    preferredSubjects: List<String>,
    weeklyGoalHours: Long,
    role: String = "student",
  ): Result<Unit> = runCatching {
    val uid = requireUserId()
    val docRef = db.collection("users").document(uid)
    val boundedSubjects = preferredSubjects.take(10)
    val sanitizedEmail = email?.takeIf { it.isNotBlank() }

    try {
      val existing = docRef.get().await()
      if (!existing.exists()) {
        val createPayload = mutableMapOf<String, Any>(
          "userId" to uid,
          "displayName" to displayName.trim().take(80).ifBlank { "Student" },
          "gradeLevel" to gradeLevel.trim().take(40).ifBlank { "Undergraduate" },
          "learningGoals" to learningGoals.trim().take(500),
          "preferredSubjects" to boundedSubjects,
          "weeklyGoalHours" to weeklyGoalHours.coerceIn(1L, 60L),
          "role" to if (role == "tutor") "tutor" else "student",
          "createdAt" to FieldValue.serverTimestamp(),
          "updatedAt" to FieldValue.serverTimestamp(),
        )
        if (sanitizedEmail != null) {
          createPayload["email"] = sanitizedEmail.take(120)
        }
        docRef.set(createPayload).await()
      } else {
        val updatePayload = mutableMapOf<String, Any>(
          "displayName" to displayName.trim().take(80).ifBlank { "Student" },
          "gradeLevel" to gradeLevel.trim().take(40).ifBlank { "Undergraduate" },
          "learningGoals" to learningGoals.trim().take(500),
          "preferredSubjects" to boundedSubjects,
          "weeklyGoalHours" to weeklyGoalHours.coerceIn(1L, 60L),
          "role" to if (role == "tutor") "tutor" else "student",
          "updatedAt" to FieldValue.serverTimestamp(),
        )
        if (sanitizedEmail != null) {
          updatePayload["email"] = sanitizedEmail.take(120)
        }
        docRef.update(updatePayload).await()
      }
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.WRITE, docRef.path, auth)
      throw e
    }
  }

  // --- Saved Tutors ---

  fun observeSavedTutors(userId: String): Flow<List<SavedTutor>> = flow {
    val colRef = db.collection("users").document(userId).collection("saved_tutors")
    emitAll(
      colRef.snapshots()
        .map { snapshot ->
          snapshot.toObjects(
            SavedTutor::class.java,
            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE,
          )
        }
        .catch { error ->
          handleStreamError(error, OperationType.LIST, colRef.path, userId)
          throw error
        }
    )
  }

  suspend fun toggleSavedTutor(expert: SubjectExpert, currentlySaved: Boolean): Result<Unit> = runCatching {
    val uid = requireUserId()
    val docRef = db.collection("users").document(uid)
      .collection("saved_tutors").document(expert.id)
    try {
      if (currentlySaved) {
        docRef.delete().await()
      } else {
        val payload = mapOf(
          "userId" to uid,
          "tutorId" to expert.id,
          "tutorName" to expert.name.take(80),
          "headline" to "${expert.title} · ${expert.institution}".take(120),
          "primarySubject" to expert.primaryCategory.take(50),
          "createdAt" to FieldValue.serverTimestamp(),
        )
        docRef.set(payload).await()
      }
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(
        e,
        if (currentlySaved) OperationType.DELETE else OperationType.CREATE,
        docRef.path,
        auth,
      )
      throw e
    }
  }

  // --- Tutoring Session Bookings + Transactions + Notifications ---

  fun observeBookings(userId: String): Flow<List<TutoringSession>> = flow {
    val colRef = db.collection("users").document(userId).collection("bookings")
    emitAll(
      colRef.snapshots()
        .map { snapshot ->
          snapshot.toObjects(
            TutoringSession::class.java,
            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE,
          )
        }
        .catch { error ->
          handleStreamError(error, OperationType.LIST, colRef.path, userId)
          throw error
        }
    )
  }

  suspend fun createBooking(
    expert: SubjectExpert,
    subject: String,
    topic: String,
    sessionFormat: String,
    scheduledDateLabel: String,
    scheduledTimeLabel: String,
    durationMinutes: Long,
    notes: String,
    studentName: String,
    paymentMethodLabel: String = "Google Pay",
    skipPayment: Boolean = false,
  ): Result<String> = runCatching {
    val uid = requireUserId()
    val bookingId = "session_${UUID.randomUUID().toString().replace("-", "").take(16)}"
    val docRef = db.collection("users").document(uid).collection("bookings").document(bookingId)
    val basePrice = if (skipPayment) {
      0L
    } else {
      ((expert.hourlyRateUsd.toLong() * durationMinutes) / 60L).coerceAtLeast(5L)
    }
    val platformFee = if (skipPayment) 0L else (basePrice * 10L / 100L).coerceAtLeast(1L)
    val totalCharged = basePrice
    val tutorNet = if (skipPayment) 0L else (basePrice - platformFee).coerceAtLeast(1L)
    val effectiveMethod = if (skipPayment) {
      "Skipped (Payment Optional)"
    } else {
      paymentMethodLabel.trim().take(80).ifBlank { "Google Pay" }
    }
    val initialPaymentStatus = if (skipPayment) "waived" else "authorized"
    val meetingCode = "SCH-${(1000..9999).random()}"

    val payload = mapOf(
      "userId" to uid,
      "tutorId" to expert.id,
      "tutorName" to expert.name.take(80),
      "tutorTitle" to "${expert.title} · ${expert.institution}".take(120),
      "subject" to subject.trim().take(60).ifBlank { expert.primaryCategory },
      "topic" to topic.trim().take(160).ifBlank { "${expert.primaryCategory} 1-on-1 Coaching" },
      "sessionFormat" to sessionFormat,
      "scheduledDateLabel" to scheduledDateLabel.trim().take(40),
      "scheduledTimeLabel" to scheduledTimeLabel.trim().take(30),
      "durationMinutes" to durationMinutes,
      "hourlyRateUsd" to expert.hourlyRateUsd.toLong(),
      "totalPriceUsd" to basePrice,
      "status" to "scheduled",
      "notes" to notes.trim().take(600),
      "meetingCode" to meetingCode,
      "paymentMethodLabel" to effectiveMethod,
      "paymentStatus" to initialPaymentStatus,
      "createdAt" to FieldValue.serverTimestamp(),
      "updatedAt" to FieldValue.serverTimestamp(),
    )

    try {
      docRef.set(payload).await()

      if (!skipPayment) {
        // Record payment authorization ledger entry
        val txId = "tx_${bookingId.removePrefix("session_")}"
        val txRef = db.collection("users").document(uid).collection("transactions").document(txId)
        txRef.set(
          mapOf(
            "userId" to uid,
            "bookingId" to bookingId,
            "tutorId" to expert.id,
            "tutorName" to expert.name.take(80),
            "amountUsd" to totalCharged,
            "platformFeeUsd" to platformFee,
            "tutorNetUsd" to tutorNet,
            "paymentMethodLabel" to effectiveMethod,
            "status" to "authorized",
            "receiptCode" to "RCP-${(10000..99999).random()}",
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp(),
          )
        ).await()
      }

      // Record booking confirmation notification
      val notifId = "notif_${UUID.randomUUID().toString().replace("-", "").take(12)}"
      val confirmTitle = "Booking Confirmed: ${expert.name}".take(100)
      val confirmBody = "${subject.take(40)} scheduled for $scheduledDateLabel at $scheduledTimeLabel ($durationMinutes min).".take(300)
      db.collection("users").document(uid).collection("notifications").document(notifId).set(
        mapOf(
          "userId" to uid,
          "title" to confirmTitle,
          "body" to confirmBody,
          "type" to "booking_confirmation",
          "bookingId" to bookingId,
          "isRead" to false,
          "createdAt" to FieldValue.serverTimestamp(),
        )
      ).await()

      // Queue FCM push dispatch for both student and tutor
      val dispatchId = "push_${UUID.randomUUID().toString().replace("-", "").take(12)}"
      db.collection("users").document(uid).collection("push_outbox").document(dispatchId).set(
        mapOf(
          "userId" to uid,
          "eventType" to "booking_confirmation",
          "recipientRole" to "both",
          "bookingId" to bookingId,
          "tutorId" to expert.id,
          "title" to confirmTitle,
          "body" to confirmBody,
          "deliveryStatus" to "queued",
          "createdAt" to FieldValue.serverTimestamp(),
        )
      ).await()

      // Add initial structured confirmation message inside the booking's message thread
      val msgId = "msg_${UUID.randomUUID().toString().replace("-", "").take(12)}"
      val msgRef = docRef.collection("messages").document(msgId)
      msgRef.set(
        mapOf(
          "userId" to uid,
          "bookingId" to bookingId,
          "senderRole" to "expert_system",
          "senderName" to expert.name.take(80),
          "body" to "Session confirmed for $scheduledDateLabel at $scheduledTimeLabel ($durationMinutes min). Share your questions or syllabus topics here before we meet, ${studentName.ifBlank { "Scholar" }}!",
          "createdAt" to FieldValue.serverTimestamp(),
        )
      ).await()
      bookingId
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, docRef.path, auth)
      throw e
    }
  }

  suspend fun rescheduleBooking(
    bookingId: String,
    dateLabel: String,
    timeLabel: String,
    topic: String,
    notes: String,
  ): Result<Unit> = runCatching {
    val uid = requireUserId()
    val docRef = db.collection("users").document(uid).collection("bookings").document(bookingId)
    val updates = mapOf(
      "scheduledDateLabel" to dateLabel.trim().take(40),
      "scheduledTimeLabel" to timeLabel.trim().take(30),
      "topic" to topic.trim().take(160),
      "notes" to notes.trim().take(600),
      "updatedAt" to FieldValue.serverTimestamp(),
    )
    try {
      docRef.update(updates).await()
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, docRef.path, auth)
      throw e
    }
  }

  suspend fun completeBooking(bookingId: String, closingNotes: String): Result<Unit> = runCatching {
    val uid = requireUserId()
    val docRef = db.collection("users").document(uid).collection("bookings").document(bookingId)
    try {
      val existingBooking = docRef.get().await()
      val isWaived = existingBooking.getString("paymentStatus") == "waived"
      val updates = mapOf(
        "status" to "completed",
        "notes" to closingNotes.trim().take(600),
        "paymentStatus" to if (isWaived) "waived" else "settled",
        "updatedAt" to FieldValue.serverTimestamp(),
      )
      docRef.update(updates).await()
      if (!isWaived) {
        // Settle matching transaction if present
        val txId = "tx_${bookingId.removePrefix("session_")}"
        val txRef = db.collection("users").document(uid).collection("transactions").document(txId)
        val txSnap = txRef.get().await()
        if (txSnap.exists() && txSnap.getString("status") == "authorized") {
          txRef.update(
            mapOf(
              "status" to "settled",
              "updatedAt" to FieldValue.serverTimestamp(),
            )
          ).await()
        }
      }
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, docRef.path, auth)
      throw e
    }
  }

  suspend fun cancelBooking(bookingId: String): Result<Unit> = runCatching {
    val uid = requireUserId()
    val docRef = db.collection("users").document(uid).collection("bookings").document(bookingId)
    try {
      val existingBooking = docRef.get().await()
      val isWaived = existingBooking.getString("paymentStatus") == "waived"
      val updates = mapOf(
        "status" to "cancelled",
        "paymentStatus" to if (isWaived) "waived" else "refunded",
        "updatedAt" to FieldValue.serverTimestamp(),
      )
      docRef.update(updates).await()
      if (!isWaived) {
        val txId = "tx_${bookingId.removePrefix("session_")}"
        val txRef = db.collection("users").document(uid).collection("transactions").document(txId)
        val txSnap = txRef.get().await()
        if (txSnap.exists() && txSnap.getString("status") == "authorized") {
          txRef.update(
            mapOf(
              "status" to "refunded",
              "updatedAt" to FieldValue.serverTimestamp(),
            )
          ).await()
        }
      }
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, docRef.path, auth)
      throw e
    }
  }

  // --- Session Messages ---

  fun observeSessionMessages(userId: String, bookingId: String): Flow<List<SessionMessage>> = flow {
    val colRef = db.collection("users").document(userId)
      .collection("bookings").document(bookingId)
      .collection("messages")
    emitAll(
      colRef.snapshots()
        .map { snapshot ->
          snapshot.toObjects(
            SessionMessage::class.java,
            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE,
          )
        }
        .catch { error ->
          handleStreamError(error, OperationType.LIST, colRef.path, userId)
          throw error
        }
    )
  }

  suspend fun sendSessionMessage(
    bookingId: String,
    senderName: String,
    body: String,
    tutorId: String = "tutor_elena_vance",
    subject: String = "Tutoring Session",
  ): Result<String> = runCatching {
    val uid = requireUserId()
    val messageId = "msg_${UUID.randomUUID().toString().replace("-", "").take(16)}"
    val docRef = db.collection("users").document(uid)
      .collection("bookings").document(bookingId)
      .collection("messages").document(messageId)
    val cleanSender = senderName.trim().take(80).ifBlank { "Student" }
    val cleanBody = body.trim().take(1000)

    val payload = mapOf(
      "userId" to uid,
      "bookingId" to bookingId,
      "senderRole" to "student",
      "senderName" to cleanSender,
      "body" to cleanBody,
      "createdAt" to FieldValue.serverTimestamp(),
    )
    try {
      docRef.set(payload).await()

      val msgNotifTitle = "New Message: ${subject.take(40)}".take(100)
      val msgNotifBody = "$cleanSender: ${cleanBody.take(180)}".take(300)

      val dispatchId = "push_${UUID.randomUUID().toString().replace("-", "").take(12)}"
      db.collection("users").document(uid).collection("push_outbox").document(dispatchId).set(
        mapOf(
          "userId" to uid,
          "eventType" to "new_message",
          "recipientRole" to "tutor",
          "bookingId" to bookingId,
          "tutorId" to tutorId.ifBlank { "tutor_elena_vance" },
          "title" to msgNotifTitle,
          "body" to msgNotifBody,
          "deliveryStatus" to "queued",
          "createdAt" to FieldValue.serverTimestamp(),
        )
      ).await()

      messageId
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, docRef.path, auth)
      throw e
    }
  }

  // --- Tutor Availability Calendar ---

  fun observeAvailability(userId: String): Flow<List<TutorAvailabilityDay>> = flow {
    val colRef = db.collection("users").document(userId).collection("availability")
    emitAll(
      colRef.snapshots()
        .map { snapshot ->
          snapshot.toObjects(
            TutorAvailabilityDay::class.java,
            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE,
          )
        }
        .catch { error ->
          handleStreamError(error, OperationType.LIST, colRef.path, userId)
          throw error
        }
    )
  }

  suspend fun saveAvailabilityDay(
    dayId: String,
    dayLabel: String,
    timeSlots: List<String>,
    isEnabled: Boolean,
  ): Result<Unit> = runCatching {
    val uid = requireUserId()
    val docRef = db.collection("users").document(uid).collection("availability").document(dayId)
    val boundedSlots = timeSlots.distinct().take(12)
    try {
      val existing = docRef.get().await()
      if (!existing.exists()) {
        docRef.set(
          mapOf(
            "userId" to uid,
            "dayLabel" to dayLabel.trim().take(30),
            "timeSlots" to boundedSlots,
            "isEnabled" to isEnabled,
            "updatedAt" to FieldValue.serverTimestamp(),
          )
        ).await()
      } else {
        docRef.update(
          mapOf(
            "dayLabel" to dayLabel.trim().take(30),
            "timeSlots" to boundedSlots,
            "isEnabled" to isEnabled,
            "updatedAt" to FieldValue.serverTimestamp(),
          )
        ).await()
      }
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.WRITE, docRef.path, auth)
      throw e
    }
  }

  // --- Notifications ---

  fun observeNotifications(userId: String): Flow<List<BookingNotification>> = flow {
    val colRef = db.collection("users").document(userId).collection("notifications")
    emitAll(
      colRef.snapshots()
        .map { snapshot ->
          snapshot.toObjects(
            BookingNotification::class.java,
            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE,
          )
        }
        .catch { error ->
          handleStreamError(error, OperationType.LIST, colRef.path, userId)
          throw error
        }
    )
  }

  suspend fun markNotificationRead(notificationId: String): Result<Unit> = runCatching {
    val uid = requireUserId()
    val docRef = db.collection("users").document(uid).collection("notifications").document(notificationId)
    try {
      docRef.update(mapOf("isRead" to true)).await()
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, docRef.path, auth)
      throw e
    }
  }

  // --- Payment Methods, Transactions & Payouts ---

  fun observePaymentMethods(userId: String): Flow<List<SavedPaymentMethod>> = flow {
    val colRef = db.collection("users").document(userId).collection("payment_methods")
    emitAll(
      colRef.snapshots()
        .map { snapshot ->
          snapshot.toObjects(
            SavedPaymentMethod::class.java,
            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE,
          )
        }
        .catch { error ->
          handleStreamError(error, OperationType.LIST, colRef.path, userId)
          throw error
        }
    )
  }

  suspend fun addPaymentMethod(
    methodType: String,
    label: String,
    last4: String,
    expiryLabel: String?,
    isDefault: Boolean,
  ): Result<String> = runCatching {
    val uid = requireUserId()
    val methodId = "pm_${UUID.randomUUID().toString().replace("-", "").take(14)}"
    val docRef = db.collection("users").document(uid).collection("payment_methods").document(methodId)
    val payload = mutableMapOf<String, Any>(
      "userId" to uid,
      "methodType" to methodType,
      "label" to label.trim().take(80),
      "last4" to last4.trim().take(8),
      "isDefault" to isDefault,
      "createdAt" to FieldValue.serverTimestamp(),
    )
    if (!expiryLabel.isNullOrBlank()) {
      payload["expiryLabel"] = expiryLabel.trim().take(16)
    }
    try {
      docRef.set(payload).await()
      methodId
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, docRef.path, auth)
      throw e
    }
  }

  suspend fun deletePaymentMethod(methodId: String): Result<Unit> = runCatching {
    val uid = requireUserId()
    val docRef = db.collection("users").document(uid).collection("payment_methods").document(methodId)
    try {
      docRef.delete().await()
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.DELETE, docRef.path, auth)
      throw e
    }
  }

  fun observeTransactions(userId: String): Flow<List<PaymentTransaction>> = flow {
    val colRef = db.collection("users").document(userId).collection("transactions")
    emitAll(
      colRef.snapshots()
        .map { snapshot ->
          snapshot.toObjects(
            PaymentTransaction::class.java,
            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE,
          )
        }
        .catch { error ->
          handleStreamError(error, OperationType.LIST, colRef.path, userId)
          throw error
        }
    )
  }

  fun observePayouts(userId: String): Flow<List<TutorPayout>> = flow {
    val colRef = db.collection("users").document(userId).collection("payouts")
    emitAll(
      colRef.snapshots()
        .map { snapshot ->
          snapshot.toObjects(
            TutorPayout::class.java,
            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE,
          )
        }
        .catch { error ->
          handleStreamError(error, OperationType.LIST, colRef.path, userId)
          throw error
        }
    )
  }

  suspend fun requestTutorPayout(
    amountUsd: Long,
    destinationLabel: String,
    payoutChannel: String,
    payoutType: String = "tutor_earnings",
  ): Result<String> = runCatching {
    val uid = requireUserId()
    val payoutId = "pay_${UUID.randomUUID().toString().replace("-", "").take(14)}"
    val docRef = db.collection("users").document(uid).collection("payouts").document(payoutId)
    val safeType = if (payoutType == "platform_commission") "platform_commission" else "tutor_earnings"
    val refPrefix = if (safeType == "platform_commission") "COM" else "PAY"
    val refCode = "$refPrefix-${(10000..99999).random()}"
    try {
      docRef.set(
        mapOf(
          "userId" to uid,
          "amountUsd" to amountUsd.coerceIn(1L, 50000L),
          "destinationLabel" to destinationLabel.trim().take(100),
          "payoutChannel" to payoutChannel,
          "payoutType" to safeType,
          "status" to "requested",
          "referenceCode" to refCode,
          "createdAt" to FieldValue.serverTimestamp(),
        )
      ).await()

      val notifId = "notif_${UUID.randomUUID().toString().replace("-", "").take(12)}"
      val notifTitle = if (safeType == "platform_commission") {
        "Commission Sent to M-Pesa: \$$amountUsd"
      } else {
        "Payout Requested: \$$amountUsd"
      }
      val notifBody = if (safeType == "platform_commission") {
        "10% platform commission (\$$amountUsd) transferred directly to M-Pesa ${destinationLabel.take(60)} ($refCode)."
      } else {
        "$payoutChannel transfer to ${destinationLabel.take(60)} ($refCode)."
      }
      db.collection("users").document(uid).collection("notifications").document(notifId).set(
        mapOf(
          "userId" to uid,
          "title" to notifTitle.take(100),
          "body" to notifBody.take(300),
          "type" to "payout",
          "isRead" to false,
          "createdAt" to FieldValue.serverTimestamp(),
        )
      ).await()
      payoutId
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, docRef.path, auth)
      throw e
    }
  }

  // --- Expert Verification Applications ---

  fun observeExpertApplication(userId: String): Flow<ExpertApplication?> = flow {
    val docRef = db.collection("tutor_applications").document(userId)
    emitAll(
      docRef.snapshots()
        .map { snapshot ->
          if (snapshot.exists()) {
            snapshot.toObject(
              ExpertApplication::class.java,
              DocumentSnapshot.ServerTimestampBehavior.ESTIMATE,
            )
          } else {
            null
          }
        }
        .catch { error ->
          handleStreamError(error, OperationType.GET, docRef.path, userId)
          throw error
        }
    )
  }

  suspend fun submitExpertApplication(
    fullName: String,
    credentials: String,
    institution: String,
    primarySubject: String,
    hourlyRateUsd: Long,
    bio: String,
  ): Result<Unit> = runCatching {
    val uid = requireUserId()
    val docRef = db.collection("tutor_applications").document(uid)
    try {
      val existing = docRef.get().await()
      if (!existing.exists()) {
        docRef.set(
          mapOf(
            "userId" to uid,
            "fullName" to fullName.trim().take(80),
            "credentials" to credentials.trim().take(120),
            "institution" to institution.trim().take(100),
            "primarySubject" to primarySubject.trim().take(60),
            "hourlyRateUsd" to hourlyRateUsd.coerceIn(15L, 300L),
            "bio" to bio.trim().take(800),
            "verificationStatus" to "pending_review",
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp(),
          )
        ).await()
      } else {
        docRef.update(
          mapOf(
            "fullName" to fullName.trim().take(80),
            "credentials" to credentials.trim().take(120),
            "institution" to institution.trim().take(100),
            "primarySubject" to primarySubject.trim().take(60),
            "hourlyRateUsd" to hourlyRateUsd.coerceIn(15L, 300L),
            "bio" to bio.trim().take(800),
            "updatedAt" to FieldValue.serverTimestamp(),
          )
        ).await()
      }
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.WRITE, docRef.path, auth)
      throw e
    }
  }

  // --- Verified Reviews ---

  fun observeAllPublishedReviews(): Flow<List<TutorReview>> = flow {
    val query = db.collection("reviews").whereEqualTo("isPublished", true)
    emitAll(
      query.snapshots()
        .map { snapshot ->
          snapshot.toObjects(
            TutorReview::class.java,
            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE,
          )
        }
        .catch { error ->
          handleStreamError(error, OperationType.LIST, "reviews", null)
          throw error
        }
    )
  }

  fun observeTutorReviews(tutorId: String): Flow<List<TutorReview>> = flow {
    val query = db.collection("reviews")
      .whereEqualTo("tutorId", tutorId)
      .whereEqualTo("isPublished", true)
    emitAll(
      query.snapshots()
        .map { snapshot ->
          snapshot.toObjects(
            TutorReview::class.java,
            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE,
          )
        }
        .catch { error ->
          handleStreamError(error, OperationType.LIST, "reviews", null)
          throw error
        }
    )
  }

  suspend fun submitTutorReview(
    tutorId: String,
    sessionId: String,
    authorName: String,
    rating: Long,
    comment: String,
    subject: String,
  ): Result<String> = runCatching {
    val uid = requireUserId()
    val reviewId = "rev_${UUID.randomUUID().toString().replace("-", "").take(16)}"
    val docRef = db.collection("reviews").document(reviewId)
    val payload = mapOf(
      "userId" to uid,
      "authorName" to authorName.trim().take(80).ifBlank { "Student" },
      "tutorId" to tutorId,
      "sessionId" to sessionId,
      "rating" to rating.coerceIn(1L, 5L),
      "comment" to comment.trim().take(500),
      "subject" to subject.trim().take(60),
      "isPublished" to true,
      "createdAt" to FieldValue.serverTimestamp(),
      "updatedAt" to FieldValue.serverTimestamp(),
    )
    try {
      docRef.set(payload).await()
      reviewId
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, docRef.path, auth)
      throw e
    }
  }

  // --- Firebase Cloud Messaging Tokens & Push Outbox ---

  fun observeFcmToken(
    userId: String,
    tokenDocId: String = FcmPushCoordinator.PRIMARY_TOKEN_DOC_ID,
  ): Flow<FcmDeviceToken?> = flow {
    val docRef = db.collection("users").document(userId)
      .collection("fcm_tokens").document(tokenDocId)
    emitAll(
      docRef.snapshots()
        .map { snapshot ->
          if (snapshot.exists()) {
            snapshot.toObject(
              FcmDeviceToken::class.java,
              DocumentSnapshot.ServerTimestampBehavior.ESTIMATE,
            )
          } else {
            null
          }
        }
        .catch { error ->
          handleStreamError(error, OperationType.GET, docRef.path, userId)
          throw error
        }
    )
  }

  suspend fun upsertFcmToken(
    token: String,
    deviceLabel: String,
    role: String,
    bookingConfirmationsEnabled: Boolean,
    sessionRemindersEnabled: Boolean,
    newMessagesEnabled: Boolean,
    tokenDocId: String = FcmPushCoordinator.PRIMARY_TOKEN_DOC_ID,
  ): Result<Unit> = runCatching {
    val uid = requireUserId()
    val docRef = db.collection("users").document(uid)
      .collection("fcm_tokens").document(tokenDocId)
    val safeRole = if (role == "tutor") "tutor" else "student"
    try {
      val existing = docRef.get().await()
      val payload = mapOf(
        "userId" to uid,
        "token" to token.trim().take(512),
        "deviceLabel" to deviceLabel.trim().take(80).ifBlank { "Android Device" },
        "role" to safeRole,
        "bookingConfirmationsEnabled" to bookingConfirmationsEnabled,
        "sessionRemindersEnabled" to sessionRemindersEnabled,
        "newMessagesEnabled" to newMessagesEnabled,
        "updatedAt" to FieldValue.serverTimestamp(),
      )
      if (!existing.exists()) {
        docRef.set(payload).await()
      } else {
        docRef.update(
          payload.filterKeys { it != "userId" }
        ).await()
      }
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.WRITE, docRef.path, auth)
      throw e
    }
  }

  fun observePushOutbox(userId: String): Flow<List<PushDispatchEvent>> = flow {
    val colRef = db.collection("users").document(userId).collection("push_outbox")
    emitAll(
      colRef.snapshots()
        .map { snapshot ->
          snapshot.toObjects(
            PushDispatchEvent::class.java,
            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE,
          )
        }
        .catch { error ->
          handleStreamError(error, OperationType.LIST, colRef.path, userId)
          throw error
        }
    )
  }

  suspend fun queuePushDispatchEvent(
    eventType: String,
    recipientRole: String,
    bookingId: String,
    tutorId: String,
    title: String,
    body: String,
    recordInAppAlert: Boolean = false,
  ): Result<String> = runCatching {
    val uid = requireUserId()
    val dispatchId = "push_${UUID.randomUUID().toString().replace("-", "").take(14)}"
    val docRef = db.collection("users").document(uid)
      .collection("push_outbox").document(dispatchId)
    val safeEvent = when (eventType) {
      "session_reminder", "new_message" -> eventType
      else -> "booking_confirmation"
    }
    val safeRecipient = when (recipientRole) {
      "student", "tutor" -> recipientRole
      else -> "both"
    }
    val cleanTitle = title.trim().take(100)
    val cleanBody = body.trim().take(300)
    try {
      docRef.set(
        mapOf(
          "userId" to uid,
          "eventType" to safeEvent,
          "recipientRole" to safeRecipient,
          "bookingId" to bookingId,
          "tutorId" to tutorId.ifBlank { "tutor_elena_vance" },
          "title" to cleanTitle,
          "body" to cleanBody,
          "deliveryStatus" to "queued",
          "createdAt" to FieldValue.serverTimestamp(),
        )
      ).await()

      if (recordInAppAlert) {
        val notifId = "notif_${UUID.randomUUID().toString().replace("-", "").take(12)}"
        db.collection("users").document(uid).collection("notifications").document(notifId).set(
          mapOf(
            "userId" to uid,
            "title" to cleanTitle,
            "body" to cleanBody,
            "type" to safeEvent,
            "bookingId" to bookingId,
            "isRead" to false,
            "createdAt" to FieldValue.serverTimestamp(),
          )
        ).await()
      }
      dispatchId
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, docRef.path, auth)
      throw e
    }
  }
}

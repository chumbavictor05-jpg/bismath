package com.example.data.model

import androidx.annotation.DrawableRes
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

data class SubjectExpert(
  val id: String,
  val name: String,
  val title: String,
  val institution: String,
  val verificationBadge: String,
  val primaryCategory: String,
  val subjects: List<String>,
  val rating: Double,
  val reviewCount: Int,
  val sessionsCompleted: Int,
  val hourlyRateUsd: Int,
  val responseTimeLabel: String,
  val bio: String,
  val credentialsList: List<String>,
  val languages: List<String>,
  val availableDays: List<String>,
  val availableTimeSlots: List<String>,
  @DrawableRes val avatarRes: Int,
  val isFeatured: Boolean = false,
  val countryLabel: String = "Global",
  val timeZoneLabel: String = "UTC",
)

@IgnoreExtraProperties
data class StudentProfile(
  @DocumentId val id: String = "",
  val userId: String = "",
  val displayName: String = "",
  val email: String? = null,
  val gradeLevel: String = "Undergraduate",
  val learningGoals: String = "",
  val preferredSubjects: List<String> = emptyList(),
  val weeklyGoalHours: Long = 4L,
  val role: String = "student",
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
)

@IgnoreExtraProperties
data class SavedTutor(
  @DocumentId val id: String = "",
  val userId: String = "",
  val tutorId: String = "",
  val tutorName: String = "",
  val headline: String = "",
  val primarySubject: String = "",
  @ServerTimestamp val createdAt: Timestamp? = null,
)

@IgnoreExtraProperties
data class TutoringSession(
  @DocumentId val id: String = "",
  val userId: String = "",
  val tutorId: String = "",
  val tutorName: String = "",
  val tutorTitle: String = "",
  val subject: String = "",
  val topic: String = "",
  val sessionFormat: String = "Video + Whiteboard",
  val scheduledDateLabel: String = "",
  val scheduledTimeLabel: String = "",
  val durationMinutes: Long = 60L,
  val hourlyRateUsd: Long = 65L,
  val totalPriceUsd: Long = 65L,
  val status: String = "scheduled",
  val notes: String = "",
  val meetingCode: String = "",
  val paymentMethodLabel: String? = null,
  val paymentStatus: String? = null,
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
)

@IgnoreExtraProperties
data class SessionMessage(
  @DocumentId val id: String = "",
  val userId: String = "",
  val bookingId: String = "",
  val senderRole: String = "student",
  val senderName: String = "",
  val body: String = "",
  @ServerTimestamp val createdAt: Timestamp? = null,
)

@IgnoreExtraProperties
data class TutorAvailabilityDay(
  @DocumentId val id: String = "",
  val userId: String = "",
  val dayLabel: String = "",
  val timeSlots: List<String> = emptyList(),
  val isEnabled: Boolean = true,
  @ServerTimestamp val updatedAt: Timestamp? = null,
)

@IgnoreExtraProperties
data class BookingNotification(
  @DocumentId val id: String = "",
  val userId: String = "",
  val title: String = "",
  val body: String = "",
  val type: String = "new_booking",
  val bookingId: String? = null,
  val isRead: Boolean = false,
  @ServerTimestamp val createdAt: Timestamp? = null,
)

@IgnoreExtraProperties
data class SavedPaymentMethod(
  @DocumentId val id: String = "",
  val userId: String = "",
  val methodType: String = "card",
  val label: String = "",
  val last4: String = "",
  val expiryLabel: String? = null,
  val isDefault: Boolean = false,
  @ServerTimestamp val createdAt: Timestamp? = null,
)

@IgnoreExtraProperties
data class PaymentTransaction(
  @DocumentId val id: String = "",
  val userId: String = "",
  val bookingId: String = "",
  val tutorId: String = "",
  val tutorName: String = "",
  val amountUsd: Long = 0L,
  val platformFeeUsd: Long = 0L,
  val tutorNetUsd: Long = 0L,
  val paymentMethodLabel: String = "",
  val status: String = "authorized",
  val receiptCode: String = "",
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
)

@IgnoreExtraProperties
data class TutorPayout(
  @DocumentId val id: String = "",
  val userId: String = "",
  val amountUsd: Long = 0L,
  val destinationLabel: String = "",
  val payoutChannel: String = "Bank ACH",
  val payoutType: String = "tutor_earnings",
  val status: String = "requested",
  val referenceCode: String = "",
  @ServerTimestamp val createdAt: Timestamp? = null,
)

@IgnoreExtraProperties
data class ExpertApplication(
  @DocumentId val id: String = "",
  val userId: String = "",
  val fullName: String = "",
  val credentials: String = "",
  val institution: String = "",
  val primarySubject: String = "",
  val hourlyRateUsd: Long = 50L,
  val bio: String = "",
  val verificationStatus: String = "pending_review",
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
)

@IgnoreExtraProperties
data class TutorReview(
  @DocumentId val id: String = "",
  val userId: String = "",
  val authorName: String = "",
  val tutorId: String = "",
  val sessionId: String = "",
  val rating: Long = 5L,
  val comment: String = "",
  val subject: String = "",
  val isPublished: Boolean = true,
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
)

@IgnoreExtraProperties
data class FcmDeviceToken(
  @DocumentId val id: String = "",
  val userId: String = "",
  val token: String = "",
  val deviceLabel: String = "",
  val role: String = "student",
  val bookingConfirmationsEnabled: Boolean = true,
  val sessionRemindersEnabled: Boolean = true,
  val newMessagesEnabled: Boolean = true,
  @ServerTimestamp val updatedAt: Timestamp? = null,
)

@IgnoreExtraProperties
data class PushDispatchEvent(
  @DocumentId val id: String = "",
  val userId: String = "",
  val eventType: String = "booking_confirmation",
  val recipientRole: String = "both",
  val bookingId: String = "",
  val tutorId: String = "",
  val title: String = "",
  val body: String = "",
  val deliveryStatus: String = "queued",
  @ServerTimestamp val createdAt: Timestamp? = null,
)

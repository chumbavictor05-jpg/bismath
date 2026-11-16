package com.example.ui

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.catalog.VerifiedExpertsCatalog
import com.example.data.model.BookingNotification
import com.example.data.model.ExpertApplication
import com.example.data.model.FcmDeviceToken
import com.example.data.model.PaymentTransaction
import com.example.data.model.SavedPaymentMethod
import com.example.data.model.SavedTutor
import com.example.data.model.SessionMessage
import com.example.data.model.StudentProfile
import com.example.data.model.SubjectExpert
import com.example.data.model.TutorAvailabilityDay
import com.example.data.model.TutorPayout
import com.example.data.model.TutorReview
import com.example.data.model.TutoringSession
import com.example.data.repository.TutoringRepository
import com.example.notifications.FcmPushCoordinator
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed interface UiState<out T> {
  data object Loading : UiState<Nothing>
  data class Success<T>(val data: T) : UiState<T>
  data class Error(val message: String) : UiState<Nothing>
}

enum class ExpertSortOption(val label: String) {
  TOP_RATED("Highest Rated"),
  PRICE_LOW("Price: Low to High"),
  MOST_EXPERIENCED("Most Sessions"),
}

data class RatingSummary(
  val averageRating: Double,
  val totalReviewCount: Int,
)

@OptIn(ExperimentalCoroutinesApi::class)
class ScholarlyViewModel(
  private val repository: TutoringRepository,
  private val currentUserId: String,
  private val defaultDisplayName: String,
  private val defaultEmail: String?,
) : ViewModel() {

  private fun <T> Flow<T>.toUiState(tag: String, fallbackMsg: String): StateFlow<UiState<T>> =
    map<T, UiState<T>> { UiState.Success(it) }
      .catch { error ->
        val firestoreError = generateSequence(error) { it.cause }
          .filterIsInstance<FirebaseFirestoreException>()
          .firstOrNull()
        if (firestoreError == null && error is CancellationException) throw error
        val rootError = firestoreError ?: error
        Log.w(TAG, "$tag stream failed", rootError)
        emit(UiState.Error(rootError.message ?: fallbackMsg))
      }
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = UiState.Loading,
      )

  val profileState: StateFlow<UiState<StudentProfile?>> =
    repository.observeProfile(currentUserId)
      .toUiState("Profile", "Failed to load student profile")

  val savedTutorsState: StateFlow<UiState<List<SavedTutor>>> =
    repository.observeSavedTutors(currentUserId)
      .toUiState("SavedTutors", "Failed to load saved experts")

  val bookingsState: StateFlow<UiState<List<TutoringSession>>> =
    repository.observeBookings(currentUserId)
      .map { list -> list.sortedByDescending { it.createdAt ?: Timestamp(0, 0) } }
      .toUiState("Bookings", "Failed to load tutoring sessions")

  val expertAppState: StateFlow<UiState<ExpertApplication?>> =
    repository.observeExpertApplication(currentUserId)
      .toUiState("ExpertApp", "Failed to load verification status")

  val availabilityState: StateFlow<UiState<List<TutorAvailabilityDay>>> =
    repository.observeAvailability(currentUserId)
      .toUiState("Availability", "Failed to load tutor calendar")

  val notificationsState: StateFlow<UiState<List<BookingNotification>>> =
    repository.observeNotifications(currentUserId)
      .map { list -> list.sortedByDescending { it.createdAt ?: Timestamp(0, 0) } }
      .toUiState("Notifications", "Failed to load notifications")

  val fcmTokenState: StateFlow<UiState<FcmDeviceToken?>> =
    repository.observeFcmToken(currentUserId)
      .toUiState("FcmToken", "Failed to load FCM notification settings")

  val paymentMethodsState: StateFlow<UiState<List<SavedPaymentMethod>>> =
    repository.observePaymentMethods(currentUserId)
      .toUiState("PaymentMethods", "Failed to load payment methods")

  val transactionsState: StateFlow<UiState<List<PaymentTransaction>>> =
    repository.observeTransactions(currentUserId)
      .map { list -> list.sortedByDescending { it.createdAt ?: Timestamp(0, 0) } }
      .toUiState("Transactions", "Failed to load transactions")

  val payoutsState: StateFlow<UiState<List<TutorPayout>>> =
    repository.observePayouts(currentUserId)
      .map { list -> list.sortedByDescending { it.createdAt ?: Timestamp(0, 0) } }
      .toUiState("Payouts", "Failed to load payouts")

  val allPublishedReviewsState: StateFlow<UiState<List<TutorReview>>> =
    repository.observeAllPublishedReviews()
      .map { list -> list.sortedByDescending { it.createdAt ?: Timestamp(0, 0) } }
      .toUiState("AllReviews", "Failed to load published reviews")

  val liveRatingSummaries: StateFlow<Map<String, RatingSummary>> =
    allPublishedReviewsState.map { state ->
      val liveReviews = (state as? UiState.Success)?.data ?: emptyList()
      val grouped = liveReviews.groupBy { it.tutorId }
      VerifiedExpertsCatalog.experts.associate { expert ->
        val tutorLive = grouped[expert.id].orEmpty()
        val baseSum = expert.rating * expert.reviewCount
        val liveSum = tutorLive.sumOf { it.rating.toDouble() }
        val totalCount = expert.reviewCount + tutorLive.size
        val avg = if (totalCount > 0) (baseSum + liveSum) / totalCount else expert.rating
        expert.id to RatingSummary(averageRating = avg, totalReviewCount = totalCount)
      }
    }.stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
      initialValue = VerifiedExpertsCatalog.experts.associate {
        it.id to RatingSummary(it.rating, it.reviewCount)
      },
    )

  private val _selectedCurrency = MutableStateFlow(WorldCurrency.fromLocale())
  val selectedCurrency: StateFlow<WorldCurrency> = _selectedCurrency.asStateFlow()

  private val _selectedTimezoneId = MutableStateFlow(java.util.TimeZone.getDefault().id)
  val selectedTimezoneId: StateFlow<String> = _selectedTimezoneId.asStateFlow()

  // Search and Filter Controls
  private val _searchQuery = MutableStateFlow("")
  val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

  private val _selectedCategory = MutableStateFlow("All")
  val selectedCategory: StateFlow<String> = _selectedCategory.asStateFlow()

  private val _selectedLanguage = MutableStateFlow("All")
  val selectedLanguage: StateFlow<String> = _selectedLanguage.asStateFlow()

  private val _maxHourlyRate = MutableStateFlow(100)
  val maxHourlyRate: StateFlow<Int> = _maxHourlyRate.asStateFlow()

  private val _sortOption = MutableStateFlow(ExpertSortOption.TOP_RATED)
  val sortOption: StateFlow<ExpertSortOption> = _sortOption.asStateFlow()

  val filteredExperts: StateFlow<List<SubjectExpert>> = combine(
    _searchQuery,
    _selectedCategory,
    _maxHourlyRate,
    _sortOption,
    liveRatingSummaries,
  ) { query, category, maxRate, sort, ratings ->
    FilterCriteria(query, category, maxRate, sort, ratings)
  }.combine(_selectedLanguage) { criteria, language ->
    val q = criteria.query.trim().lowercase()
    VerifiedExpertsCatalog.experts
      .filter { expert ->
        val matchesCategory = criteria.category == "All" ||
          expert.primaryCategory.equals(criteria.category, ignoreCase = true)
        val matchesLanguage = language == "All" ||
          expert.languages.any { it.equals(language, ignoreCase = true) }
        val matchesRate = expert.hourlyRateUsd <= criteria.maxRate
        val matchesQuery = q.isEmpty() ||
          expert.name.lowercase().contains(q) ||
          expert.institution.lowercase().contains(q) ||
          expert.countryLabel.lowercase().contains(q) ||
          expert.title.lowercase().contains(q) ||
          expert.primaryCategory.lowercase().contains(q) ||
          expert.languages.any { it.lowercase().contains(q) } ||
          expert.subjects.any { it.lowercase().contains(q) }
        matchesCategory && matchesLanguage && matchesRate && matchesQuery
      }
      .let { list ->
        when (criteria.sort) {
          ExpertSortOption.TOP_RATED -> list.sortedByDescending {
            criteria.ratings[it.id]?.averageRating ?: it.rating
          }
          ExpertSortOption.PRICE_LOW -> list.sortedBy { it.hourlyRateUsd }
          ExpertSortOption.MOST_EXPERIENCED -> list.sortedByDescending { it.sessionsCompleted }
        }
      }
  }.stateIn(
    scope = viewModelScope,
    started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
    initialValue = VerifiedExpertsCatalog.experts,
  )

  private data class FilterCriteria(
    val query: String,
    val category: String,
    val maxRate: Int,
    val sort: ExpertSortOption,
    val ratings: Map<String, RatingSummary>,
  )

  // Active Tutor Reviews
  private val _selectedTutorId = MutableStateFlow<String?>(null)
  val tutorReviewsState: StateFlow<UiState<List<TutorReview>>> =
    _selectedTutorId.flatMapLatest { tutorId ->
      if (tutorId.isNullOrBlank()) {
        flowOf(emptyList())
      } else {
        repository.observeTutorReviews(tutorId)
          .map { list -> list.sortedByDescending { it.createdAt ?: Timestamp(0, 0) } }
      }
    }.toUiState("Reviews", "Failed to load verified reviews")

  // Active Session Messages
  private val _selectedBookingId = MutableStateFlow<String?>(null)
  val sessionMessagesState: StateFlow<UiState<List<SessionMessage>>> =
    _selectedBookingId.flatMapLatest { bookingId ->
      if (bookingId.isNullOrBlank()) {
        flowOf(emptyList())
      } else {
        repository.observeSessionMessages(currentUserId, bookingId)
          .map { list -> list.sortedBy { it.createdAt ?: Timestamp(Long.MAX_VALUE / 2, 0) } }
      }
    }.toUiState("Messages", "Failed to load session messages")

  private val _builderModeEnabled = MutableStateFlow(true)
  val builderModeEnabled: StateFlow<Boolean> = _builderModeEnabled.asStateFlow()

  private val _isBusy = MutableStateFlow(false)
  val isBusy: StateFlow<Boolean> = _isBusy.asStateFlow()

  private val _events = MutableSharedFlow<String>()
  val events: SharedFlow<String> = _events.asSharedFlow()

  fun setBuilderModeEnabled(enabled: Boolean) {
    _builderModeEnabled.value = enabled
    viewModelScope.launch {
      _events.emit(
        if (enabled) "Builder Check Mode enabled — payment is optional across all flows"
        else "Standard payment checkout active"
      )
    }
  }

  private var initializedProfile = false

  fun ensureDefaultProfileIfNeeded(existingProfile: StudentProfile?) {
    if (existingProfile == null && !initializedProfile) {
      initializedProfile = true
      viewModelScope.launch {
        repository.upsertProfile(
          displayName = defaultDisplayName.ifBlank { "Scholar" },
          email = defaultEmail,
          gradeLevel = "Undergraduate",
          learningGoals = "Prepare for upcoming exams and strengthen core problem-solving skills.",
          preferredSubjects = listOf("Mathematics", "Computer Science"),
          weeklyGoalHours = 4L,
          role = "student",
        )
      }
    }
  }

  fun selectCurrency(currency: WorldCurrency) {
    _selectedCurrency.value = currency
  }

  fun selectTimezone(timezoneId: String) {
    _selectedTimezoneId.value = timezoneId
  }

  fun selectLanguage(language: String) {
    _selectedLanguage.value = language
  }

  fun updateSearchQuery(query: String) {
    _searchQuery.value = query
  }

  fun selectCategory(category: String) {
    _selectedCategory.value = category
  }

  fun updateMaxHourlyRate(rate: Int) {
    _maxHourlyRate.value = rate
  }

  fun updateSortOption(option: ExpertSortOption) {
    _sortOption.value = option
  }

  fun observeReviewsForTutor(tutorId: String) {
    _selectedTutorId.value = tutorId
  }

  fun observeMessagesForBooking(bookingId: String) {
    _selectedBookingId.value = bookingId
  }

  fun toggleBookmark(expert: SubjectExpert, currentlySaved: Boolean) {
    viewModelScope.launch {
      repository.toggleSavedTutor(expert, currentlySaved)
        .onSuccess {
          _events.emit(
            if (currentlySaved) "Removed ${expert.name} from saved experts"
            else "Saved ${expert.name} to your shortlist"
          )
        }
        .onFailure {
          _events.emit(it.localizedMessage ?: "Could not update saved expert")
        }
    }
  }

  fun createBooking(
    expert: SubjectExpert,
    subject: String,
    topic: String,
    sessionFormat: String,
    dateLabel: String,
    timeLabel: String,
    durationMinutes: Long,
    notes: String,
    paymentMethodLabel: String,
    skipPayment: Boolean = false,
    onBooked: (String) -> Unit,
  ) {
    viewModelScope.launch {
      _isBusy.value = true
      val studentName = (profileState.value as? UiState.Success)?.data?.displayName ?: defaultDisplayName
      repository.createBooking(
        expert = expert,
        subject = subject,
        topic = topic,
        sessionFormat = sessionFormat,
        scheduledDateLabel = dateLabel,
        scheduledTimeLabel = timeLabel,
        durationMinutes = durationMinutes,
        notes = notes,
        studentName = studentName,
        paymentMethodLabel = paymentMethodLabel,
        skipPayment = skipPayment,
      ).onSuccess { bookingId ->
        _isBusy.value = false
        _events.emit(
          if (skipPayment) "Session booked without payment (Payment Optional)"
          else "Session booked & payment authorized via $paymentMethodLabel"
        )
        onBooked(bookingId)
      }.onFailure { err ->
        _isBusy.value = false
        _events.emit(err.localizedMessage ?: "Failed to book tutoring session")
      }
    }
  }

  fun rescheduleBooking(
    bookingId: String,
    dateLabel: String,
    timeLabel: String,
    topic: String,
    notes: String,
    onDone: () -> Unit,
  ) {
    viewModelScope.launch {
      _isBusy.value = true
      repository.rescheduleBooking(bookingId, dateLabel, timeLabel, topic, notes)
        .onSuccess {
          _isBusy.value = false
          _events.emit("Session rescheduled to $dateLabel at $timeLabel")
          onDone()
        }
        .onFailure {
          _isBusy.value = false
          _events.emit(it.localizedMessage ?: "Failed to reschedule session")
        }
    }
  }

  fun completeBooking(
    bookingId: String,
    closingNotes: String,
    onCompletedPromptReview: () -> Unit,
  ) {
    viewModelScope.launch {
      _isBusy.value = true
      repository.completeBooking(bookingId, closingNotes)
        .onSuccess {
          _isBusy.value = false
          _events.emit("Session completed & payment settled! Share your review.")
          onCompletedPromptReview()
        }
        .onFailure {
          _isBusy.value = false
          _events.emit(it.localizedMessage ?: "Failed to complete session")
        }
    }
  }

  fun cancelBooking(bookingId: String) {
    viewModelScope.launch {
      _isBusy.value = true
      repository.cancelBooking(bookingId)
        .onSuccess {
          _isBusy.value = false
          _events.emit("Session cancelled & payment hold released")
        }
        .onFailure {
          _isBusy.value = false
          _events.emit(it.localizedMessage ?: "Failed to cancel session")
        }
    }
  }

  fun sendMessage(bookingId: String, body: String, onSent: () -> Unit) {
    val trimmed = body.trim()
    if (trimmed.isEmpty()) return
    viewModelScope.launch {
      val studentName = (profileState.value as? UiState.Success)?.data?.displayName ?: defaultDisplayName
      val booking = (bookingsState.value as? UiState.Success)?.data?.firstOrNull { it.id == bookingId }
      repository.sendSessionMessage(
        bookingId = bookingId,
        senderName = studentName,
        body = trimmed,
        tutorId = booking?.tutorId ?: "tutor_elena_vance",
        subject = booking?.subject ?: "Tutoring Session",
      )
        .onSuccess { onSent() }
        .onFailure {
          _events.emit(it.localizedMessage ?: "Could not send message")
        }
    }
  }

  fun syncFcmDeviceRegistration(role: String) {
    viewModelScope.launch {
      val existing = (fcmTokenState.value as? UiState.Success)?.data
      val bookingConfirmations = existing?.bookingConfirmationsEnabled ?: true
      val sessionReminders = existing?.sessionRemindersEnabled ?: true
      val newMessages = existing?.newMessagesEnabled ?: true

      val token = withContext(Dispatchers.IO) {
        FcmPushCoordinator.syncTopicSubscriptions(role, sessionReminders)
        FcmPushCoordinator.fetchRegistrationTokenOrNull()
      } ?: return@launch

      repository.upsertFcmToken(
        token = token,
        deviceLabel = FcmPushCoordinator.deviceLabel(),
        role = role,
        bookingConfirmationsEnabled = bookingConfirmations,
        sessionRemindersEnabled = sessionReminders,
        newMessagesEnabled = newMessages,
      )
    }
  }

  fun updateNotificationPreferences(
    bookingConfirmations: Boolean,
    sessionReminders: Boolean,
    newMessages: Boolean,
  ) {
    viewModelScope.launch {
      val existing = (fcmTokenState.value as? UiState.Success)?.data
      val role = (profileState.value as? UiState.Success)?.data?.role ?: existing?.role ?: "student"
      val token = withContext(Dispatchers.IO) {
        FcmPushCoordinator.syncTopicSubscriptions(role, sessionReminders)
        existing?.token?.takeIf { it.length >= 10 }
          ?: FcmPushCoordinator.fetchRegistrationTokenOrNull()
      }

      if (token != null) {
        repository.upsertFcmToken(
          token = token,
          deviceLabel = FcmPushCoordinator.deviceLabel(),
          role = role,
          bookingConfirmationsEnabled = bookingConfirmations,
          sessionRemindersEnabled = sessionReminders,
          newMessagesEnabled = newMessages,
        ).onSuccess {
          _events.emit("Push notification preferences updated")
        }.onFailure {
          _events.emit(it.localizedMessage ?: "Failed to update push preferences")
        }
      } else {
        _events.emit("Updated local alert settings (FCM token pending Play Services)")
      }
    }
  }

  fun queueUpcomingSessionReminder(session: TutoringSession, onQueued: () -> Unit = {}) {
    viewModelScope.launch {
      val title = "Upcoming Session Reminder: ${session.subject}"
      val body = "Session with ${session.tutorName} starts on ${session.scheduledDateLabel} at ${session.scheduledTimeLabel}."
      repository.queuePushDispatchEvent(
        eventType = "session_reminder",
        recipientRole = "both",
        bookingId = session.id,
        tutorId = session.tutorId,
        title = title,
        body = body,
        recordInAppAlert = true,
      ).onSuccess {
        _events.emit("Reminder scheduled for ${session.scheduledDateLabel} at ${session.scheduledTimeLabel}")
        onQueued()
      }.onFailure {
        _events.emit(it.localizedMessage ?: "Could not schedule reminder")
      }
    }
  }

  fun saveAvailabilityDay(dayId: String, dayLabel: String, timeSlots: List<String>, isEnabled: Boolean) {
    viewModelScope.launch {
      repository.saveAvailabilityDay(dayId, dayLabel, timeSlots, isEnabled)
        .onSuccess { _events.emit("Updated availability for $dayLabel") }
        .onFailure { _events.emit(it.localizedMessage ?: "Failed to update calendar") }
    }
  }

  fun markNotificationRead(notificationId: String) {
    viewModelScope.launch {
      repository.markNotificationRead(notificationId)
    }
  }

  fun addPaymentMethod(methodType: String, label: String, last4: String, expiry: String?) {
    viewModelScope.launch {
      repository.addPaymentMethod(methodType, label, last4, expiry, isDefault = true)
        .onSuccess { _events.emit("Saved payment method: $label") }
        .onFailure { _events.emit(it.localizedMessage ?: "Could not save payment method") }
    }
  }

  fun deletePaymentMethod(methodId: String) {
    viewModelScope.launch {
      repository.deletePaymentMethod(methodId)
        .onSuccess { _events.emit("Payment method removed") }
    }
  }

  fun requestTutorPayout(amountUsd: Long, destinationLabel: String, channel: String) {
    viewModelScope.launch {
      _isBusy.value = true
      repository.requestTutorPayout(
        amountUsd = amountUsd,
        destinationLabel = destinationLabel,
        payoutChannel = channel,
        payoutType = "tutor_earnings",
      )
        .onSuccess {
          _isBusy.value = false
          _events.emit("Payout of \$$amountUsd requested via $channel")
        }
        .onFailure {
          _isBusy.value = false
          _events.emit(it.localizedMessage ?: "Failed to request payout")
        }
    }
  }

  fun withdrawCommissionToMpesa(
    amountUsd: Long,
    phoneNumber: String,
    rememberMpesaNumber: Boolean,
  ) {
    val cleanPhone = phoneNumber.trim()
    if (cleanPhone.length < 9 || amountUsd < 1L) return
    viewModelScope.launch {
      _isBusy.value = true
      repository.requestTutorPayout(
        amountUsd = amountUsd,
        destinationLabel = cleanPhone,
        payoutChannel = "M-Pesa",
        payoutType = "platform_commission",
      )
        .onSuccess {
          if (rememberMpesaNumber) {
            val existing = (paymentMethodsState.value as? UiState.Success)?.data.orEmpty()
            val alreadySaved = existing.any {
              it.methodType == "mpesa" && it.label.contains(cleanPhone, ignoreCase = true)
            }
            if (!alreadySaved) {
              val digits = cleanPhone.filter { it.isDigit() }
              repository.addPaymentMethod(
                methodType = "mpesa",
                label = "M-Pesa ($cleanPhone)",
                last4 = digits.takeLast(4).ifBlank { "MPES" },
                expiryLabel = null,
                isDefault = true,
              )
            }
          }
          _isBusy.value = false
          val converted = GlobalLocalization.formatUsd(amountUsd, _selectedCurrency.value)
          _events.emit("10% commission ($converted) sent directly to M-Pesa $cleanPhone")
        }
        .onFailure {
          _isBusy.value = false
          _events.emit(it.localizedMessage ?: "Failed to withdraw commission to M-Pesa")
        }
    }
  }

  fun saveProfile(
    displayName: String,
    gradeLevel: String,
    learningGoals: String,
    preferredSubjects: List<String>,
    weeklyGoalHours: Long,
    role: String? = null,
    onSaved: () -> Unit = {},
  ) {
    viewModelScope.launch {
      _isBusy.value = true
      val effectiveRole = role ?: (profileState.value as? UiState.Success)?.data?.role ?: "student"
      repository.upsertProfile(
        displayName = displayName,
        email = defaultEmail,
        gradeLevel = gradeLevel,
        learningGoals = learningGoals,
        preferredSubjects = preferredSubjects,
        weeklyGoalHours = weeklyGoalHours,
        role = effectiveRole,
      ).onSuccess {
        _isBusy.value = false
        _events.emit("Academic profile updated")
        onSaved()
      }.onFailure {
        _isBusy.value = false
        _events.emit(it.localizedMessage ?: "Failed to update profile")
      }
    }
  }

  fun submitExpertApplication(
    fullName: String,
    credentials: String,
    institution: String,
    primarySubject: String,
    hourlyRateUsd: Long,
    bio: String,
    onSubmitted: () -> Unit,
  ) {
    viewModelScope.launch {
      _isBusy.value = true
      repository.submitExpertApplication(
        fullName = fullName,
        credentials = credentials,
        institution = institution,
        primarySubject = primarySubject,
        hourlyRateUsd = hourlyRateUsd,
        bio = bio,
      ).onSuccess {
        _isBusy.value = false
        _events.emit("Expert verification application submitted for review")
        onSubmitted()
      }.onFailure {
        _isBusy.value = false
        _events.emit(it.localizedMessage ?: "Failed to submit application")
      }
    }
  }

  fun submitReview(
    tutorId: String,
    sessionId: String,
    rating: Long,
    comment: String,
    subject: String,
    onSubmitted: () -> Unit,
  ) {
    viewModelScope.launch {
      _isBusy.value = true
      val authorName = (profileState.value as? UiState.Success)?.data?.displayName ?: defaultDisplayName
      repository.submitTutorReview(
        tutorId = tutorId,
        sessionId = sessionId,
        authorName = authorName,
        rating = rating,
        comment = comment,
        subject = subject,
      ).onSuccess {
        _isBusy.value = false
        _events.emit("Thank you! Verified review published.")
        onSubmitted()
      }.onFailure {
        _isBusy.value = false
        _events.emit(it.localizedMessage ?: "Failed to publish review")
      }
    }
  }

  private companion object {
    const val TAG = "ScholarlyVM"
    const val STOP_TIMEOUT_MILLIS = 5000L
  }
}

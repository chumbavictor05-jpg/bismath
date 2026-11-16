package com.example.data.repository

import com.example.base.FirestoreEmulatorTestBase
import com.example.data.catalog.VerifiedExpertsCatalog
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class TutoringRepositoryRuleTest : FirestoreEmulatorTestBase() {

  @Test
  fun upsertProfile_andObserveProfile_persistsAndEmits() = runBlocking {
    val uid = signInTestUser(ALICE_EMAIL)
    val repo = TutoringRepository(firestore, auth)

    val result = withTimeout(DEFAULT_TIMEOUT_MS) {
      repo.upsertProfile(
        displayName = "Alice Chen",
        email = ALICE_EMAIL,
        gradeLevel = "Undergraduate",
        learningGoals = "Master multivariable calculus and algorithms",
        preferredSubjects = listOf("Mathematics", "Computer Science"),
        weeklyGoalHours = 6L,
      )
    }
    assertTrue("Expected profile upsert success: ${result.exceptionOrNull()}", result.isSuccess)

    val profile = withTimeout(FLOW_TIMEOUT_MS) {
      repo.observeProfile(uid).first { it != null }
    }
    assertNotNull(profile)
    assertEquals("Alice Chen", profile?.displayName)
    assertEquals(6L, profile?.weeklyGoalHours)
  }

  @Test
  fun createBooking_recordsTransactionNotificationAndWelcomeMessage() = runBlocking {
    val uid = signInTestUser(ALICE_EMAIL)
    val repo = TutoringRepository(firestore, auth)
    val expert = VerifiedExpertsCatalog.experts.first()

    val createResult = withTimeout(DEFAULT_TIMEOUT_MS) {
      repo.createBooking(
        expert = expert,
        subject = "Calculus I–III",
        topic = "Stokes Theorem & Line Integrals",
        sessionFormat = "Video + Whiteboard",
        scheduledDateLabel = "Tue, Nov 18",
        scheduledTimeLabel = "4:00 PM",
        durationMinutes = 60L,
        notes = "Focusing on homework problem 4.",
        studentName = "Alice",
        paymentMethodLabel = "Visa •••• 4242",
      )
    }
    assertTrue("Expected booking creation success: ${createResult.exceptionOrNull()}", createResult.isSuccess)
    val bookingId = createResult.getOrThrow()

    val bookings = withTimeout(FLOW_TIMEOUT_MS) {
      repo.observeBookings(uid).first { list -> list.any { it.id == bookingId } }
    }
    assertTrue(bookings.any { it.id == bookingId && it.status == "scheduled" })

    val transactions = withTimeout(FLOW_TIMEOUT_MS) {
      repo.observeTransactions(uid).first { list -> list.any { it.bookingId == bookingId } }
    }
    assertTrue(transactions.any { it.bookingId == bookingId && it.status == "authorized" })

    val notifications = withTimeout(FLOW_TIMEOUT_MS) {
      repo.observeNotifications(uid).first { list -> list.any { it.bookingId == bookingId } }
    }
    assertTrue(notifications.any { it.bookingId == bookingId && !it.isRead })

    val messages = withTimeout(FLOW_TIMEOUT_MS) {
      repo.observeSessionMessages(uid, bookingId).first { it.isNotEmpty() }
    }
    assertEquals(1, messages.size)
  }

  @Test
  fun availabilityPaymentMethodsAndPayouts_persistSafely() = runBlocking {
    val uid = signInTestUser(ALICE_EMAIL)
    val repo = TutoringRepository(firestore, auth)

    val availResult = withTimeout(DEFAULT_TIMEOUT_MS) {
      repo.saveAvailabilityDay(
        dayId = "mon",
        dayLabel = "Mon, Nov 17",
        timeSlots = listOf("9:00 AM", "2:00 PM"),
        isEnabled = true,
      )
    }
    assertTrue("Availability save failed: ${availResult.exceptionOrNull()}", availResult.isSuccess)

    val pmResult = withTimeout(DEFAULT_TIMEOUT_MS) {
      repo.addPaymentMethod(
        methodType = "mpesa",
        label = "M-Pesa •••• 5678",
        last4 = "5678",
        expiryLabel = null,
        isDefault = true,
      )
    }
    assertTrue("M-Pesa payment method add failed: ${pmResult.exceptionOrNull()}", pmResult.isSuccess)

    val paypalResult = withTimeout(DEFAULT_TIMEOUT_MS) {
      repo.addPaymentMethod(
        methodType = "paypal",
        label = "PayPal (alice@example.com)",
        last4 = "alic",
        expiryLabel = null,
        isDefault = false,
      )
    }
    assertTrue("PayPal payment method add failed: ${paypalResult.exceptionOrNull()}", paypalResult.isSuccess)

    val payoutResult = withTimeout(DEFAULT_TIMEOUT_MS) {
      repo.requestTutorPayout(
        amountUsd = 120L,
        destinationLabel = "+254 712 345 678",
        payoutChannel = "M-Pesa",
      )
    }
    assertTrue("Payout request failed: ${payoutResult.exceptionOrNull()}", payoutResult.isSuccess)

    val commissionPayoutResult = withTimeout(DEFAULT_TIMEOUT_MS) {
      repo.requestTutorPayout(
        amountUsd = 15L,
        destinationLabel = "+254712345678",
        payoutChannel = "M-Pesa",
        payoutType = "platform_commission",
      )
    }
    assertTrue("Commission M-Pesa payout failed: ${commissionPayoutResult.exceptionOrNull()}", commissionPayoutResult.isSuccess)

    val payouts = withTimeout(FLOW_TIMEOUT_MS) {
      repo.observePayouts(uid).first { it.size >= 2 }
    }
    assertTrue(payouts.any { it.amountUsd == 120L && it.payoutType == "tutor_earnings" })
    assertTrue(payouts.any { it.amountUsd == 15L && it.payoutType == "platform_commission" && it.payoutChannel == "M-Pesa" })
  }

  @Test
  fun rescheduleAndCompleteBooking_settlesTransactionAndLocksTerminalState() = runBlocking {
    val uid = signInTestUser(ALICE_EMAIL)
    val repo = TutoringRepository(firestore, auth)
    val expert = VerifiedExpertsCatalog.experts.first()

    val bookingId = withTimeout(DEFAULT_TIMEOUT_MS) {
      repo.createBooking(
        expert = expert,
        subject = "Linear Algebra",
        topic = "Singular Value Decomposition",
        sessionFormat = "Exam Prep",
        scheduledDateLabel = "Wed, Nov 19",
        scheduledTimeLabel = "11:30 AM",
        durationMinutes = 45L,
        notes = "Midterm prep",
        studentName = "Alice",
      ).getOrThrow()
    }

    val rescheduleResult = withTimeout(DEFAULT_TIMEOUT_MS) {
      repo.rescheduleBooking(
        bookingId = bookingId,
        dateLabel = "Fri, Nov 21",
        timeLabel = "6:30 PM",
        topic = "SVD & Principal Components",
        notes = "Updated prep list",
      )
    }
    assertTrue("Reschedule failed: ${rescheduleResult.exceptionOrNull()}", rescheduleResult.isSuccess)

    val completeResult = withTimeout(DEFAULT_TIMEOUT_MS) {
      repo.completeBooking(bookingId, "Completed practice exam.")
    }
    assertTrue("Complete failed: ${completeResult.exceptionOrNull()}", completeResult.isSuccess)

    val settledTx = withTimeout(FLOW_TIMEOUT_MS) {
      repo.observeTransactions(uid).first { list -> list.any { it.bookingId == bookingId && it.status == "settled" } }
    }
    assertTrue(settledTx.isNotEmpty())

    val cancelAfterComplete = withTimeout(DEFAULT_TIMEOUT_MS) {
      repo.cancelBooking(bookingId)
    }
    assertFalse(cancelAfterComplete.isSuccess)
    val firestoreException = cancelAfterComplete.exceptionOrNull()?.unwrapFirestoreException()
    assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, firestoreException?.code)
  }

  @Test
  fun submitTutorReview_andObserveReviews_worksWithParentGate() = runBlocking {
    signInTestUser(ALICE_EMAIL)
    val repo = TutoringRepository(firestore, auth)
    val expert = VerifiedExpertsCatalog.experts.first()

    val bookingId = withTimeout(DEFAULT_TIMEOUT_MS) {
      repo.createBooking(
        expert = expert,
        subject = "Calculus",
        topic = "Integration by Parts",
        sessionFormat = "Homework Review",
        scheduledDateLabel = "Mon, Nov 17",
        scheduledTimeLabel = "9:00 AM",
        durationMinutes = 30L,
        notes = "Quick check",
        studentName = "Alice",
      ).getOrThrow()
    }

    val reviewResult = withTimeout(DEFAULT_TIMEOUT_MS) {
      repo.submitTutorReview(
        tutorId = expert.id,
        sessionId = bookingId,
        authorName = "Alice Chen",
        rating = 5L,
        comment = "Crystal-clear step-by-step walkthrough!",
        subject = "Calculus",
      )
    }
    assertTrue("Review failed: ${reviewResult.exceptionOrNull()}", reviewResult.isSuccess)
    val reviewId = reviewResult.getOrThrow()

    val reviews = withTimeout(FLOW_TIMEOUT_MS) {
      repo.observeAllPublishedReviews().first { list -> list.any { it.id == reviewId } }
    }
    assertTrue(reviews.any { it.id == reviewId && it.rating == 5L })
  }

  @Test
  fun crossUserBookingAccess_failsWithPermissionDenied() = runBlocking {
    val aliceUid = signInTestUser(ALICE_EMAIL)
    val aliceRepo = TutoringRepository(firestore, auth)
    val expert = VerifiedExpertsCatalog.experts.first()
    val bookingId = withTimeout(DEFAULT_TIMEOUT_MS) {
      aliceRepo.createBooking(
        expert = expert,
        subject = "Calculus",
        topic = "Taylor Series",
        sessionFormat = "Video + Whiteboard",
        scheduledDateLabel = "Mon, Nov 17",
        scheduledTimeLabel = "9:00 AM",
        durationMinutes = 60L,
        notes = "Private session",
        studentName = "Alice",
      ).getOrThrow()
    }

    signInTestUser(BOB_EMAIL)
    try {
      withTimeout(DEFAULT_TIMEOUT_MS) {
        firestore.collection("users").document(aliceUid)
          .collection("bookings").document(bookingId)
          .get()
          .await()
      }
      fail("Expected PERMISSION_DENIED for cross-user booking access")
    } catch (e: Exception) {
      val firestoreException = e.unwrapFirestoreException()
      assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, firestoreException?.code)
    }
  }

  @Test
  fun unauthenticatedObserveBookings_failsWithPermissionDenied() = runBlocking {
    auth.signOut()
    val repo = TutoringRepository(firestore, auth)
    try {
      withTimeout(FLOW_TIMEOUT_MS) {
        repo.observeBookings("some_uid_123").first()
      }
      fail("Expected PERMISSION_DENIED for unauthenticated booking observation")
    } catch (e: Exception) {
      val firestoreException = e.unwrapFirestoreException()
      assertNotNull("Expected nested FirebaseFirestoreException, got $e", firestoreException)
      assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, firestoreException?.code)
    }
  }

  @Test
  fun fcmTokenAndPushDispatchEvents_persistForConfirmationsRemindersAndMessages() = runBlocking {
    val uid = signInTestUser(ALICE_EMAIL)
    val repo = TutoringRepository(firestore, auth)
    val expert = VerifiedExpertsCatalog.experts.first()

    val tokenResult = withTimeout(DEFAULT_TIMEOUT_MS) {
      repo.upsertFcmToken(
        token = "fcm_sample_token_0987654321_xyz",
        deviceLabel = "Pixel 9 Test",
        role = "student",
        bookingConfirmationsEnabled = true,
        sessionRemindersEnabled = true,
        newMessagesEnabled = true,
      )
    }
    assertTrue("FCM token registration failed: ${tokenResult.exceptionOrNull()}", tokenResult.isSuccess)

    val observedToken = withTimeout(FLOW_TIMEOUT_MS) {
      repo.observeFcmToken(uid).first { it != null }
    }
    assertEquals("fcm_sample_token_0987654321_xyz", observedToken?.token)

    val bookingId = withTimeout(DEFAULT_TIMEOUT_MS) {
      repo.createBooking(
        expert = expert,
        subject = "Differential Equations",
        topic = "Laplace Transforms",
        sessionFormat = "Video + Whiteboard",
        scheduledDateLabel = "Thu, Nov 20",
        scheduledTimeLabel = "2:00 PM",
        durationMinutes = 60L,
        notes = "Prepare problem set",
        studentName = "Alice",
      ).getOrThrow()
    }

    withTimeout(DEFAULT_TIMEOUT_MS) {
      repo.sendSessionMessage(
        bookingId = bookingId,
        senderName = "Alice",
        body = "Uploaded my syllabus notes!",
        tutorId = expert.id,
        subject = "Differential Equations",
      ).getOrThrow()
    }

    withTimeout(DEFAULT_TIMEOUT_MS) {
      repo.queuePushDispatchEvent(
        eventType = "session_reminder",
        recipientRole = "both",
        bookingId = bookingId,
        tutorId = expert.id,
        title = "Upcoming Session Reminder",
        body = "Starts in 15 minutes",
        recordInAppAlert = true,
      ).getOrThrow()
    }

    val outbox = withTimeout(FLOW_TIMEOUT_MS) {
      repo.observePushOutbox(uid).first { events ->
        events.any { it.eventType == "booking_confirmation" } &&
          events.any { it.eventType == "new_message" } &&
          events.any { it.eventType == "session_reminder" }
      }
    }
    assertEquals(3, outbox.count { it.bookingId == bookingId })
  }

  @Test
  fun createBooking_withSkipPayment_allowsCompleteChatAndReviewWithoutTransaction() = runBlocking {
    val uid = signInTestUser(ALICE_EMAIL)
    val repo = TutoringRepository(firestore, auth)
    val expert = VerifiedExpertsCatalog.experts.first()

    val bookingId = withTimeout(DEFAULT_TIMEOUT_MS) {
      repo.createBooking(
        expert = expert,
        subject = "Physics",
        topic = "Quantum Mechanics Check",
        sessionFormat = "Video + Whiteboard",
        scheduledDateLabel = "Fri, Nov 21",
        scheduledTimeLabel = "10:00 AM",
        durationMinutes = 60L,
        notes = "Builder inspection booking",
        studentName = "Alice",
        paymentMethodLabel = "Skipped (Payment Optional)",
        skipPayment = true,
      ).getOrThrow()
    }

    val bookings = withTimeout(FLOW_TIMEOUT_MS) {
      repo.observeBookings(uid).first { list -> list.any { it.id == bookingId } }
    }
    val created = bookings.first { it.id == bookingId }
    assertEquals(0L, created.totalPriceUsd)
    assertEquals("waived", created.paymentStatus)

    val transactions = withTimeout(FLOW_TIMEOUT_MS) {
      repo.observeTransactions(uid).first()
    }
    assertFalse("Skipped-payment booking must not create a payment transaction", transactions.any { it.bookingId == bookingId })

    val completeResult = withTimeout(DEFAULT_TIMEOUT_MS) {
      repo.completeBooking(bookingId, "Inspected complete flow without payment.")
    }
    assertTrue("Complete waived booking failed: ${completeResult.exceptionOrNull()}", completeResult.isSuccess)

    val completedBookings = withTimeout(FLOW_TIMEOUT_MS) {
      repo.observeBookings(uid).first { list -> list.any { it.id == bookingId && it.status == "completed" } }
    }
    assertEquals("waived", completedBookings.first { it.id == bookingId }.paymentStatus)
  }

  private companion object {
    const val ALICE_EMAIL = "alice_scholar@test.com"
    const val BOB_EMAIL = "bob_scholar@test.com"
    const val DEFAULT_TIMEOUT_MS = 5000L
    const val FLOW_TIMEOUT_MS = 3000L
  }
}

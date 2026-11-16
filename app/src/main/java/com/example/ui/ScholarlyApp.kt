package com.example.ui

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.R
import com.example.auth.authStateFlow
import com.example.auth.signOutUser
import com.example.data.catalog.VerifiedExpertsCatalog
import com.example.data.repository.TutoringRepository
import com.example.notifications.BookingNotificationHelper
import com.example.notifications.PushNotificationCategory
import com.example.notifications.SessionReminderScheduler
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.DiscoverScreen
import com.example.ui.screens.ExpertDetailScreen
import com.example.ui.screens.ProfileScreen
import com.example.ui.screens.SavedExpertsScreen
import com.example.ui.screens.SessionWorkspaceScreen
import com.example.ui.screens.SessionsScreen
import com.example.ui.screens.TutorHubScreen
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore

const val ROUTE_DISCOVER = "discover"
const val ROUTE_SESSIONS = "sessions"
const val ROUTE_TUTOR_HUB = "tutor_hub"
const val ROUTE_SAVED = "saved"
const val ROUTE_PROFILE = "profile"
const val ROUTE_EXPERT_DETAIL = "expert/{tutorId}"
const val ROUTE_SESSION_WORKSPACE = "session/{bookingId}"

private data class TopDestination(
  val route: String,
  val labelRes: Int,
  val selectedIcon: ImageVector,
  val unselectedIcon: ImageVector,
  val testTag: String,
)

private val TopDestinations = listOf(
  TopDestination(ROUTE_DISCOVER, R.string.nav_discover, Icons.Filled.Explore, Icons.Outlined.Explore, "nav_discover"),
  TopDestination(ROUTE_SESSIONS, R.string.nav_sessions, Icons.Filled.CalendarMonth, Icons.Outlined.CalendarMonth, "nav_sessions"),
  TopDestination(ROUTE_TUTOR_HUB, R.string.nav_hub, Icons.Filled.AccountBalanceWallet, Icons.Outlined.AccountBalanceWallet, "nav_hub"),
  TopDestination(ROUTE_SAVED, R.string.nav_saved, Icons.Filled.Bookmark, Icons.Outlined.BookmarkBorder, "nav_saved"),
  TopDestination(ROUTE_PROFILE, R.string.nav_profile, Icons.Filled.Person, Icons.Outlined.Person, "nav_profile"),
)

@Composable
fun AppNavigation(
  auth: FirebaseAuth = Firebase.auth,
  deepLinkBookingId: String? = null,
  onDeepLinkHandled: () -> Unit = {},
) {
  val currentUser by auth.authStateFlow().collectAsStateWithLifecycle(initialValue = auth.currentUser)
  val user = currentUser

  if (user == null) {
    AuthScreen(onAuthSuccess = {})
  } else {
    val uid = user.uid
    key(uid) {
      MainScholarlyShell(
        currentUserId = uid,
        defaultDisplayName = user.displayName ?: "Scholar",
        defaultEmail = user.email,
        deepLinkBookingId = deepLinkBookingId,
        onDeepLinkHandled = onDeepLinkHandled,
      )
    }
  }
}

@Composable
fun MainScholarlyShell(
  currentUserId: String,
  defaultDisplayName: String,
  defaultEmail: String?,
  deepLinkBookingId: String? = null,
  onDeepLinkHandled: () -> Unit = {},
) {
  val viewModel: ScholarlyViewModel = viewModel(
    key = currentUserId,
    factory = viewModelFactory {
      initializer {
        val app = checkNotNull(this[APPLICATION_KEY]) {
          "APPLICATION_KEY missing from CreationExtras"
        }
        val databaseId = app.getString(R.string.firestore_database_id)
        val db = FirebaseFirestore.getInstance(databaseId)
        ScholarlyViewModel(
          repository = TutoringRepository(db),
          currentUserId = currentUserId,
          defaultDisplayName = defaultDisplayName,
          defaultEmail = defaultEmail,
        )
      }
    },
  )

  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  val navController = rememberNavController()
  val snackbarHostState = remember { SnackbarHostState() }

  val profileState by viewModel.profileState.collectAsStateWithLifecycle()
  val savedTutorsState by viewModel.savedTutorsState.collectAsStateWithLifecycle()
  val bookingsState by viewModel.bookingsState.collectAsStateWithLifecycle()
  val expertAppState by viewModel.expertAppState.collectAsStateWithLifecycle()
  val availabilityState by viewModel.availabilityState.collectAsStateWithLifecycle()
  val notificationsState by viewModel.notificationsState.collectAsStateWithLifecycle()
  val fcmTokenState by viewModel.fcmTokenState.collectAsStateWithLifecycle()
  val paymentMethodsState by viewModel.paymentMethodsState.collectAsStateWithLifecycle()
  val transactionsState by viewModel.transactionsState.collectAsStateWithLifecycle()
  val payoutsState by viewModel.payoutsState.collectAsStateWithLifecycle()
  val allPublishedReviewsState by viewModel.allPublishedReviewsState.collectAsStateWithLifecycle()
  val ratingSummaries by viewModel.liveRatingSummaries.collectAsStateWithLifecycle()
  val filteredExperts by viewModel.filteredExperts.collectAsStateWithLifecycle()
  val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
  val selectedCategory by viewModel.selectedCategory.collectAsStateWithLifecycle()
  val maxHourlyRate by viewModel.maxHourlyRate.collectAsStateWithLifecycle()
  val sortOption by viewModel.sortOption.collectAsStateWithLifecycle()
  val tutorReviewsState by viewModel.tutorReviewsState.collectAsStateWithLifecycle()
  val sessionMessagesState by viewModel.sessionMessagesState.collectAsStateWithLifecycle()
  val selectedCurrency by viewModel.selectedCurrency.collectAsStateWithLifecycle()
  val selectedTimezoneId by viewModel.selectedTimezoneId.collectAsStateWithLifecycle()
  val selectedLanguage by viewModel.selectedLanguage.collectAsStateWithLifecycle()
  val builderModeEnabled by viewModel.builderModeEnabled.collectAsStateWithLifecycle()
  val isBusy by viewModel.isBusy.collectAsStateWithLifecycle()

  LaunchedEffect(profileState) {
    val state = profileState
    if (state is UiState.Success) {
      viewModel.ensureDefaultProfileIfNeeded(state.data)
      val activeRole = state.data?.role ?: "student"
      viewModel.syncFcmDeviceRegistration(activeRole)
    }
  }

  LaunchedEffect(deepLinkBookingId) {
    if (!deepLinkBookingId.isNullOrBlank()) {
      navController.navigate("session/$deepLinkBookingId") {
        launchSingleTop = true
      }
      onDeepLinkHandled()
    }
  }

  LaunchedEffect(Unit) {
    viewModel.events.collect { msg ->
      snackbarHostState.showSnackbar(msg)
    }
  }

  val savedIds = remember(savedTutorsState) {
    (savedTutorsState as? UiState.Success)?.data?.map { it.tutorId }?.toSet() ?: emptySet()
  }
  val allBookings = remember(bookingsState) {
    (bookingsState as? UiState.Success)?.data ?: emptyList()
  }
  val paymentMethods = remember(paymentMethodsState) {
    (paymentMethodsState as? UiState.Success)?.data ?: emptyList()
  }
  val allPublishedReviews = remember(allPublishedReviewsState) {
    (allPublishedReviewsState as? UiState.Success)?.data ?: emptyList()
  }
  val unreadNotificationsCount = remember(notificationsState) {
    (notificationsState as? UiState.Success)?.data?.count { !it.isRead } ?: 0
  }
  val upcomingCount = remember(allBookings) {
    allBookings.count { it.status == "scheduled" }
  }
  val completedCount = remember(allBookings) {
    allBookings.count { it.status == "completed" }
  }
  val studentFirstName = remember(profileState, defaultDisplayName) {
    val name = (profileState as? UiState.Success)?.data?.displayName?.ifBlank { defaultDisplayName }
      ?: defaultDisplayName
    name.substringBefore(" ").ifBlank { "Scholar" }
  }

  val navBackStackEntry by navController.currentBackStackEntryAsState()
  val currentRoute = navBackStackEntry?.destination?.route
  val showTopLevelNav = currentRoute in TopDestinations.map { it.route }

  fun navigateTopLevel(route: String) {
    navController.navigate(route) {
      popUpTo(navController.graph.findStartDestination().id) {
        saveState = true
      }
      launchSingleTop = true
      restoreState = true
    }
  }

  BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
    val useNavRail = maxWidth >= 600.dp

    Scaffold(
      containerColor = MaterialTheme.colorScheme.background,
      contentWindowInsets = WindowInsets.statusBars,
      snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
      bottomBar = {
        if (showTopLevelNav && !useNavRail) {
          NavigationBar(
            windowInsets = WindowInsets.navigationBars,
            containerColor = MaterialTheme.colorScheme.surface,
          ) {
            TopDestinations.forEach { dest ->
              val selected = currentRoute == dest.route
              val label = stringResource(dest.labelRes)
              NavigationBarItem(
                selected = selected,
                onClick = { navigateTopLevel(dest.route) },
                icon = {
                  val badgeCount = when (dest.route) {
                    ROUTE_SESSIONS -> upcomingCount
                    ROUTE_TUTOR_HUB -> unreadNotificationsCount
                    else -> 0
                  }
                  if (badgeCount > 0) {
                    BadgedBox(badge = { Badge { Text("$badgeCount") } }) {
                      Icon(
                        imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                        contentDescription = label,
                      )
                    }
                  } else {
                    Icon(
                      imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                      contentDescription = label,
                    )
                  }
                },
                label = { Text(label) },
                modifier = Modifier.testTag(dest.testTag),
              )
            }
          }
        }
      },
    ) { innerPadding ->
      Row(
        modifier = Modifier
          .fillMaxSize()
          .padding(innerPadding),
      ) {
        if (showTopLevelNav && useNavRail) {
          NavigationRail(containerColor = MaterialTheme.colorScheme.surface) {
            TopDestinations.forEach { dest ->
              val selected = currentRoute == dest.route
              val label = stringResource(dest.labelRes)
              NavigationRailItem(
                selected = selected,
                onClick = { navigateTopLevel(dest.route) },
                icon = {
                  val badgeCount = when (dest.route) {
                    ROUTE_SESSIONS -> upcomingCount
                    ROUTE_TUTOR_HUB -> unreadNotificationsCount
                    else -> 0
                  }
                  if (badgeCount > 0) {
                    BadgedBox(badge = { Badge { Text("$badgeCount") } }) {
                      Icon(
                        imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                        contentDescription = label,
                      )
                    }
                  } else {
                    Icon(
                      imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                      contentDescription = label,
                    )
                  }
                },
                label = { Text(label) },
                modifier = Modifier.testTag(dest.testTag),
              )
            }
          }
        }

        NavHost(
          navController = navController,
          startDestination = ROUTE_DISCOVER,
          modifier = Modifier.weight(1f),
        ) {
          composable(ROUTE_DISCOVER) {
            DiscoverScreen(
              experts = filteredExperts,
              savedTutorIds = savedIds,
              ratingSummaries = ratingSummaries,
              selectedCurrency = selectedCurrency,
              selectedTimezoneId = selectedTimezoneId,
              selectedLanguage = selectedLanguage,
              searchQuery = searchQuery,
              selectedCategory = selectedCategory,
              maxHourlyRate = maxHourlyRate,
              sortOption = sortOption,
              studentFirstName = studentFirstName,
              upcomingCount = upcomingCount,
              onSelectCurrency = viewModel::selectCurrency,
              onSelectTimezone = viewModel::selectTimezone,
              onSelectLanguage = viewModel::selectLanguage,
              onSearchQueryChange = viewModel::updateSearchQuery,
              onSelectCategory = viewModel::selectCategory,
              onUpdateMaxRate = viewModel::updateMaxHourlyRate,
              onUpdateSort = viewModel::updateSortOption,
              onToggleBookmark = viewModel::toggleBookmark,
              onOpenExpert = { tutorId ->
                navController.navigate("expert/$tutorId")
              },
              onOpenSessionsTab = { navigateTopLevel(ROUTE_SESSIONS) },
            )
          }

          composable(ROUTE_SESSIONS) {
            SessionsScreen(
              bookingsState = bookingsState,
              selectedCurrency = selectedCurrency,
              selectedTimezoneId = selectedTimezoneId,
              onOpenSession = { bookingId ->
                navController.navigate("session/$bookingId")
              },
              onExploreExperts = { navigateTopLevel(ROUTE_DISCOVER) },
            )
          }

          composable(ROUTE_TUTOR_HUB) {
            val fcmPrefs = (fcmTokenState as? UiState.Success)?.data
            val upcomingScheduled = remember(allBookings) {
              allBookings.filter { it.status == "scheduled" }
            }
            TutorHubScreen(
              notificationsState = notificationsState,
              fcmTokenState = fcmTokenState,
              upcomingSessions = upcomingScheduled,
              availabilityState = availabilityState,
              paymentMethodsState = paymentMethodsState,
              transactionsState = transactionsState,
              payoutsState = payoutsState,
              selectedCurrency = selectedCurrency,
              selectedTimezoneId = selectedTimezoneId,
              builderModeEnabled = builderModeEnabled,
              isBusy = isBusy,
              onMarkNotificationRead = viewModel::markNotificationRead,
              onUpdatePushPreferences = viewModel::updateNotificationPreferences,
              onTriggerSessionReminder = { session ->
                SessionReminderScheduler.scheduleReminder(
                  context = context,
                  bookingId = session.id,
                  subject = session.subject,
                  partnerName = session.tutorName,
                  dateLabel = session.scheduledDateLabel,
                  timeLabel = session.scheduledTimeLabel,
                )
                viewModel.queueUpcomingSessionReminder(session) {
                  if (fcmPrefs?.sessionRemindersEnabled != false) {
                    BookingNotificationHelper.showBookingAlert(
                      context = context,
                      title = "Upcoming Session Reminder: ${session.subject}",
                      body = "Your session with ${session.tutorName} starts on ${session.scheduledDateLabel} at ${session.scheduledTimeLabel}.",
                      bookingId = session.id,
                      category = PushNotificationCategory.SESSION_REMINDER,
                    )
                  }
                }
              },
              onOpenSession = { bookingId ->
                navController.navigate("session/$bookingId")
              },
              onSaveAvailabilityDay = viewModel::saveAvailabilityDay,
              onAddPaymentMethod = viewModel::addPaymentMethod,
              onDeletePaymentMethod = viewModel::deletePaymentMethod,
              onRequestPayout = viewModel::requestTutorPayout,
              onWithdrawCommissionMpesa = viewModel::withdrawCommissionToMpesa,
            )
          }

          composable(ROUTE_SAVED) {
            SavedExpertsScreen(
              savedTutorsState = savedTutorsState,
              ratingSummaries = ratingSummaries,
              selectedCurrency = selectedCurrency,
              onToggleBookmark = viewModel::toggleBookmark,
              onOpenExpert = { tutorId ->
                navController.navigate("expert/$tutorId")
              },
              onExploreExperts = { navigateTopLevel(ROUTE_DISCOVER) },
            )
          }

          composable(ROUTE_PROFILE) {
            ProfileScreen(
              userEmail = defaultEmail,
              defaultDisplayName = defaultDisplayName,
              profileState = profileState,
              expertAppState = expertAppState,
              completedSessionsCount = completedCount,
              selectedCurrency = selectedCurrency,
              selectedTimezoneId = selectedTimezoneId,
              builderModeEnabled = builderModeEnabled,
              isBusy = isBusy,
              onSelectCurrency = viewModel::selectCurrency,
              onSelectTimezone = viewModel::selectTimezone,
              onToggleBuilderMode = viewModel::setBuilderModeEnabled,
              onSaveProfile = { name, grade, goals, subjects, hours ->
                viewModel.saveProfile(name, grade, goals, subjects, hours, onSaved = {})
              },
              onSubmitExpertApplication = { name, cred, inst, subj, rate, bio ->
                viewModel.submitExpertApplication(name, cred, inst, subj, rate, bio, onSubmitted = {})
              },
              onSignOut = {
                signOutUser(CredentialManager.create(context), scope)
              },
            )
          }

          composable(
            route = ROUTE_EXPERT_DETAIL,
            arguments = listOf(navArgument("tutorId") { type = NavType.StringType }),
          ) { entry ->
            val tutorId = entry.arguments?.getString("tutorId") ?: ""
            val expert = VerifiedExpertsCatalog.findById(tutorId)
            LaunchedEffect(tutorId) {
              viewModel.observeReviewsForTutor(tutorId)
            }
            if (expert != null) {
              val isSaved = savedIds.contains(expert.id)
              val bookedSlotsForTutor = remember(allBookings, expert.id) {
                allBookings
                  .filter { it.tutorId == expert.id && it.status == "scheduled" }
                  .map { "${it.scheduledDateLabel}|${it.scheduledTimeLabel}" }
                  .toSet()
              }
              ExpertDetailScreen(
                expert = expert,
                isSaved = isSaved,
                ratingSummary = ratingSummaries[expert.id],
                reviewsState = tutorReviewsState,
                paymentMethods = paymentMethods,
                bookedSlotsForTutor = bookedSlotsForTutor,
                selectedCurrency = selectedCurrency,
                selectedTimezoneId = selectedTimezoneId,
                builderModeEnabled = builderModeEnabled,
                isBusy = isBusy,
                onBack = { navController.popBackStack() },
                onToggleBookmark = { viewModel.toggleBookmark(expert, isSaved) },
                onBookSession = { subject, topic, format, date, time, duration, notes, paymentMethod, skipPayment ->
                  viewModel.createBooking(
                    expert = expert,
                    subject = subject,
                    topic = topic,
                    sessionFormat = format,
                    dateLabel = date,
                    timeLabel = time,
                    durationMinutes = duration,
                    notes = notes,
                    paymentMethodLabel = paymentMethod,
                    skipPayment = skipPayment,
                    onBooked = { bookingId ->
                      val fcmPrefs = (fcmTokenState as? UiState.Success)?.data
                      if (fcmPrefs?.bookingConfirmationsEnabled != false) {
                        BookingNotificationHelper.showBookingAlert(
                          context = context,
                          title = "Booking Confirmed: ${expert.name}",
                          body = "$subject scheduled for $date at $time ($duration min).",
                          bookingId = bookingId,
                          category = PushNotificationCategory.BOOKING_CONFIRMATION,
                        )
                      }
                      if (fcmPrefs?.sessionRemindersEnabled != false) {
                        SessionReminderScheduler.scheduleReminder(
                          context = context,
                          bookingId = bookingId,
                          subject = subject,
                          partnerName = expert.name,
                          dateLabel = date,
                          timeLabel = time,
                        )
                      }
                      navController.navigate("session/$bookingId")
                    },
                  )
                },
              )
            }
          }

          composable(
            route = ROUTE_SESSION_WORKSPACE,
            arguments = listOf(navArgument("bookingId") { type = NavType.StringType }),
          ) { entry ->
            val bookingId = entry.arguments?.getString("bookingId") ?: ""
            LaunchedEffect(bookingId) {
              viewModel.observeMessagesForBooking(bookingId)
            }
            val session = allBookings.firstOrNull { it.id == bookingId }
            if (session != null) {
              val hasSubmittedReview = remember(allPublishedReviews, session.id) {
                allPublishedReviews.any { it.sessionId == session.id }
              }
              val fcmPrefs = (fcmTokenState as? UiState.Success)?.data
              SessionWorkspaceScreen(
                session = session,
                messagesState = sessionMessagesState,
                hasSubmittedReview = hasSubmittedReview,
                selectedCurrency = selectedCurrency,
                selectedTimezoneId = selectedTimezoneId,
                isBusy = isBusy,
                onBack = { navController.popBackStack() },
                onSendMessage = { body ->
                  viewModel.sendMessage(bookingId, body) {
                    if (fcmPrefs?.newMessagesEnabled != false) {
                      BookingNotificationHelper.showBookingAlert(
                        context = context,
                        title = "Message Sent: ${session.subject}",
                        body = "Delivered to ${session.tutorName} (${session.scheduledDateLabel}).",
                        bookingId = bookingId,
                        category = PushNotificationCategory.NEW_MESSAGE,
                      )
                    }
                  }
                },
                onReschedule = { date, time, topic, notes ->
                  viewModel.rescheduleBooking(bookingId, date, time, topic, notes) {
                    if (fcmPrefs?.sessionRemindersEnabled != false) {
                      SessionReminderScheduler.scheduleReminder(
                        context = context,
                        bookingId = bookingId,
                        subject = session.subject,
                        partnerName = session.tutorName,
                        dateLabel = date,
                        timeLabel = time,
                      )
                    }
                  }
                },
                onComplete = { closingNotes, onCompletedPromptReview ->
                  SessionReminderScheduler.cancelReminder(context, bookingId)
                  viewModel.completeBooking(bookingId, closingNotes, onCompletedPromptReview)
                },
                onCancel = {
                  SessionReminderScheduler.cancelReminder(context, bookingId)
                  viewModel.cancelBooking(bookingId)
                },
                onScheduleReminder = {
                  SessionReminderScheduler.scheduleReminder(
                    context = context,
                    bookingId = session.id,
                    subject = session.subject,
                    partnerName = session.tutorName,
                    dateLabel = session.scheduledDateLabel,
                    timeLabel = session.scheduledTimeLabel,
                  )
                  viewModel.queueUpcomingSessionReminder(session) {
                    if (fcmPrefs?.sessionRemindersEnabled != false) {
                      BookingNotificationHelper.showBookingAlert(
                        context = context,
                        title = "Reminder Active: ${session.subject}",
                        body = "We'll alert you 15 min before ${session.scheduledDateLabel} at ${session.scheduledTimeLabel}.",
                        bookingId = session.id,
                        category = PushNotificationCategory.SESSION_REMINDER,
                      )
                    }
                  }
                },
                onSubmitReview = { rating, comment ->
                  viewModel.submitReview(
                    tutorId = session.tutorId,
                    sessionId = session.id,
                    rating = rating,
                    comment = comment,
                    subject = session.subject,
                    onSubmitted = {},
                  )
                },
              )
            }
          }
        }
      }
    }
  }
}

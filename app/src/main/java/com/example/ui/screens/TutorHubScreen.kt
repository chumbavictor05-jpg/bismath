package com.example.ui.screens

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.BookingNotification
import com.example.data.model.FcmDeviceToken
import com.example.data.model.PaymentTransaction
import com.example.data.model.SavedPaymentMethod
import com.example.data.model.TutorAvailabilityDay
import com.example.data.model.TutorPayout
import com.example.data.model.TutoringSession
import com.example.notifications.BookingNotificationHelper
import com.example.ui.GlobalLocalization
import com.example.ui.UiState
import com.example.ui.WorldCurrency
import com.example.ui.theme.LocalScholarlyColors

private val WEEK_DAYS = listOf(
  "mon" to "Mon, Nov 17",
  "tue" to "Tue, Nov 18",
  "wed" to "Wed, Nov 19",
  "thu" to "Thu, Nov 20",
  "fri" to "Fri, Nov 21",
  "sat" to "Sat, Nov 22",
)

private val STANDARD_SLOTS = listOf(
  "8:30 AM", "10:00 AM", "11:30 AM", "2:00 PM", "4:00 PM", "6:30 PM", "8:00 PM"
)

@Composable
fun TutorHubScreen(
  notificationsState: UiState<List<BookingNotification>>,
  fcmTokenState: UiState<FcmDeviceToken?>,
  upcomingSessions: List<TutoringSession>,
  availabilityState: UiState<List<TutorAvailabilityDay>>,
  paymentMethodsState: UiState<List<SavedPaymentMethod>>,
  transactionsState: UiState<List<PaymentTransaction>>,
  payoutsState: UiState<List<TutorPayout>>,
  selectedCurrency: WorldCurrency,
  selectedTimezoneId: String,
  builderModeEnabled: Boolean,
  isBusy: Boolean,
  onMarkNotificationRead: (String) -> Unit,
  onUpdatePushPreferences: (Boolean, Boolean, Boolean) -> Unit,
  onTriggerSessionReminder: (TutoringSession) -> Unit,
  onOpenSession: (String) -> Unit,
  onSaveAvailabilityDay: (String, String, List<String>, Boolean) -> Unit,
  onAddPaymentMethod: (String, String, String, String?) -> Unit,
  onDeletePaymentMethod: (String) -> Unit,
  onRequestPayout: (Long, String, String) -> Unit,
  onWithdrawCommissionMpesa: (Long, String, Boolean) -> Unit,
) {
  var selectedTab by remember { mutableIntStateOf(0) }
  val tabs = listOf("Calendar", "Alerts", "Wallet & Payouts")
  val notifications = (notificationsState as? UiState.Success)?.data.orEmpty()
  val unreadCount = notifications.count { !it.isRead }

  val notificationPermLauncher = rememberLauncherForActivityResult(
    ActivityResultContracts.RequestPermission()
  ) { }

  Column(
    modifier = Modifier
      .fillMaxSize()
      .background(MaterialTheme.colorScheme.background),
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 20.dp)
        .padding(top = 16.dp, bottom = 8.dp),
    ) {
      Text(
        text = "Schedule, Alerts & Payments",
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.secondary,
        fontWeight = FontWeight.SemiBold,
      )
      Spacer(modifier = Modifier.height(2.dp))
      Text(
        text = "Tutor & Wallet Hub",
        style = MaterialTheme.typography.headlineLarge,
      )
    }

    TabRow(
      selectedTabIndex = selectedTab,
      containerColor = MaterialTheme.colorScheme.background,
    ) {
      tabs.forEachIndexed { idx, title ->
        val label = if (idx == 1 && unreadCount > 0) "$title ($unreadCount)" else title
        Tab(
          selected = selectedTab == idx,
          onClick = { selectedTab = idx },
          text = {
            Text(
              text = label,
              style = MaterialTheme.typography.labelLarge,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
          },
          modifier = Modifier.testTag("hub_tab_$idx"),
        )
      }
    }

    when (selectedTab) {
      0 -> CalendarAvailabilityTab(
        availabilityList = (availabilityState as? UiState.Success)?.data.orEmpty(),
        selectedTimezoneId = selectedTimezoneId,
        onSaveDay = onSaveDayCallback@{ dayId, dayLabel, slots, enabled ->
          onSaveAvailabilityDay(dayId, dayLabel, slots, enabled)
        },
      )
      1 -> NotificationsTab(
        notifications = notifications,
        fcmToken = (fcmTokenState as? UiState.Success)?.data,
        upcomingSessions = upcomingSessions,
        onMarkRead = onMarkNotificationRead,
        onUpdatePushPreferences = onUpdatePushPreferences,
        onTriggerSessionReminder = onTriggerSessionReminder,
        onOpenSession = onOpenSession,
        onRequestSystemPermission = {
          if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
          }
        },
      )
      else -> WalletAndPayoutsTab(
        paymentMethods = (paymentMethodsState as? UiState.Success)?.data.orEmpty(),
        transactions = (transactionsState as? UiState.Success)?.data.orEmpty(),
        payouts = (payoutsState as? UiState.Success)?.data.orEmpty(),
        selectedCurrency = selectedCurrency,
        builderModeEnabled = builderModeEnabled,
        isBusy = isBusy,
        onAddPaymentMethod = onAddPaymentMethod,
        onDeletePaymentMethod = onDeletePaymentMethod,
        onRequestPayout = onRequestPayout,
        onWithdrawCommissionMpesa = onWithdrawCommissionMpesa,
      )
    }
  }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CalendarAvailabilityTab(
  availabilityList: List<TutorAvailabilityDay>,
  selectedTimezoneId: String,
  onSaveDay: (String, String, List<String>, Boolean) -> Unit,
) {
  val byDayId = remember(availabilityList) { availabilityList.associateBy { it.id } }
  val tzLabel = remember(selectedTimezoneId) { GlobalLocalization.formatTimezoneLabel(selectedTimezoneId) }

  LazyColumn(
    modifier = Modifier.fillMaxSize(),
    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    item {
      Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
        modifier = Modifier.fillMaxWidth(),
      ) {
        Row(
          modifier = Modifier.padding(14.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          Icon(
            imageVector = Icons.Filled.CalendarMonth,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
          )
          Text(
            text = "Manage your global weekly calendar ($tzLabel). Students worldwide see open slots converted to their local time.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
          )
        }
      }
    }

    items(WEEK_DAYS, key = { it.first }) { (dayId, dayLabel) ->
      val saved = byDayId[dayId]
      val isEnabled = saved?.isEnabled ?: (dayId in listOf("mon", "tue", "wed", "thu"))
      val activeSlots = saved?.timeSlots ?: listOf("10:00 AM", "4:00 PM")

      Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth(),
      ) {
        Column(
          modifier = Modifier.padding(16.dp),
          verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Column {
              Text(dayLabel, style = MaterialTheme.typography.titleMedium)
              Text(
                text = if (isEnabled) "${activeSlots.size} open slots" else "Unavailable",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
            Switch(
              checked = isEnabled,
              onCheckedChange = { checked ->
                onSaveDay(dayId, dayLabel, activeSlots, checked)
              },
              modifier = Modifier.testTag("availability_switch_$dayId"),
            )
          }

          if (isEnabled) {
            FlowRow(
              horizontalArrangement = Arrangement.spacedBy(6.dp),
              verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
              STANDARD_SLOTS.forEach { slot ->
                val selected = activeSlots.contains(slot)
                FilterChip(
                  selected = selected,
                  onClick = {
                    val updated = if (selected) activeSlots - slot else activeSlots + slot
                    onSaveDay(dayId, dayLabel, updated, true)
                  },
                  label = { Text(slot, style = MaterialTheme.typography.labelMedium) },
                )
              }
            }
          }
        }
      }
    }
  }
}

@Composable
private fun NotificationsTab(
  notifications: List<BookingNotification>,
  fcmToken: FcmDeviceToken?,
  upcomingSessions: List<TutoringSession>,
  onMarkRead: (String) -> Unit,
  onUpdatePushPreferences: (Boolean, Boolean, Boolean) -> Unit,
  onTriggerSessionReminder: (TutoringSession) -> Unit,
  onOpenSession: (String) -> Unit,
  onRequestSystemPermission: () -> Unit,
) {
  val context = LocalContext.current
  val scholarlyColors = LocalScholarlyColors.current
  val permissionGranted = remember(context) { BookingNotificationHelper.hasNotificationPermission(context) }
  val confirmEnabled = fcmToken?.bookingConfirmationsEnabled ?: true
  val remindersEnabled = fcmToken?.sessionRemindersEnabled ?: true
  val messagesEnabled = fcmToken?.newMessagesEnabled ?: true

  LazyColumn(
    modifier = Modifier.fillMaxSize(),
    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    // Firebase Cloud Messaging (FCM) Real-Time Push Status & Toggles
    item {
      Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier
          .fillMaxWidth()
          .testTag("fcm_settings_card"),
      ) {
        Column(
          modifier = Modifier.padding(16.dp),
          verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = "REAL-TIME FCM PUSH ALERTS",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
              )
              Text(
                text = if (fcmToken != null && fcmToken.token.length >= 10) {
                  "Device registered ••••${fcmToken.token.takeLast(6)}"
                } else {
                  "Ready for FCM & local session alerts"
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !permissionGranted) {
              Button(
                onClick = onRequestSystemPermission,
                modifier = Modifier.testTag("enable_push_permission_button"),
              ) {
                Text("Allow Push")
              }
            } else {
              Surface(
                shape = CircleShape,
                color = scholarlyColors.verifiedBadgeBg,
              ) {
                Text(
                  text = "ACTIVE",
                  style = MaterialTheme.typography.labelSmall,
                  color = scholarlyColors.verifiedBadgeFg,
                  modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                )
              }
            }
          }

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Text("Booking Confirmations", style = MaterialTheme.typography.bodyMedium)
            Switch(
              checked = confirmEnabled,
              onCheckedChange = { checked ->
                onUpdatePushPreferences(checked, remindersEnabled, messagesEnabled)
              },
              modifier = Modifier.testTag("fcm_toggle_bookings"),
            )
          }
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Text("Upcoming Session Reminders", style = MaterialTheme.typography.bodyMedium)
            Switch(
              checked = remindersEnabled,
              onCheckedChange = { checked ->
                onUpdatePushPreferences(confirmEnabled, checked, messagesEnabled)
              },
              modifier = Modifier.testTag("fcm_toggle_reminders"),
            )
          }
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Text("New Session Messages", style = MaterialTheme.typography.bodyMedium)
            Switch(
              checked = messagesEnabled,
              onCheckedChange = { checked ->
                onUpdatePushPreferences(confirmEnabled, remindersEnabled, checked)
              },
              modifier = Modifier.testTag("fcm_toggle_messages"),
            )
          }

          val nextSession = upcomingSessions.firstOrNull()
          if (nextSession != null) {
            OutlinedButton(
              onClick = { onTriggerSessionReminder(nextSession) },
              modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("send_upcoming_reminder_button"),
            ) {
              Icon(Icons.Filled.Alarm, contentDescription = null, modifier = Modifier.size(18.dp))
              Spacer(modifier = Modifier.width(8.dp))
              Text("Send Reminder for ${nextSession.subject} (${nextSession.scheduledTimeLabel})")
            }
          }
        }
      }
    }

    if (notifications.isEmpty()) {
      item {
        Surface(
          shape = MaterialTheme.shapes.large,
          color = MaterialTheme.colorScheme.surface,
          border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
          modifier = Modifier.fillMaxWidth(),
        ) {
          Column(
            modifier = Modifier.padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            Icon(
              imageVector = Icons.Filled.NotificationsActive,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.primary,
              modifier = Modifier.size(36.dp),
            )
            Text("No booking alerts yet", style = MaterialTheme.typography.titleMedium)
            Text(
              text = "Booking confirmations, upcoming session reminders, new messages, and payout receipts will appear here.",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        }
      }
    } else {
      items(notifications, key = { it.id }) { notif ->
        val notifIcon = when (notif.type) {
          "session_reminder" -> Icons.Filled.Alarm
          "new_message" -> Icons.AutoMirrored.Filled.Chat
          "payout" -> Icons.Filled.Payments
          "booking_confirmation", "new_booking" -> Icons.Filled.CheckCircle
          else -> Icons.Filled.NotificationsActive
        }
        Surface(
          shape = MaterialTheme.shapes.large,
          color = if (notif.isRead) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
          border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
          modifier = Modifier
            .fillMaxWidth()
            .clickable {
              if (!notif.isRead) onMarkRead(notif.id)
              notif.bookingId?.takeIf { it.isNotBlank() }?.let(onOpenSession)
            },
        ) {
          Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top,
          ) {
            Icon(
              imageVector = notifIcon,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.primary,
              modifier = Modifier.padding(top = 2.dp).size(20.dp),
            )
            Column(
              modifier = Modifier.weight(1f),
              verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
              ) {
                Text(notif.title, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                if (!notif.isRead) {
                  Surface(shape = CircleShape, color = MaterialTheme.colorScheme.secondary) {
                    Text(
                      text = "NEW",
                      style = MaterialTheme.typography.labelSmall,
                      color = MaterialTheme.colorScheme.onSecondary,
                      modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                    )
                  }
                }
              }
              Text(
                text = notif.body,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
          }
        }
      }
    }
  }
}

@Composable
private fun WalletAndPayoutsTab(
  paymentMethods: List<SavedPaymentMethod>,
  transactions: List<PaymentTransaction>,
  payouts: List<TutorPayout>,
  selectedCurrency: WorldCurrency,
  builderModeEnabled: Boolean,
  isBusy: Boolean,
  onAddPaymentMethod: (String, String, String, String?) -> Unit,
  onDeletePaymentMethod: (String) -> Unit,
  onRequestPayout: (Long, String, String) -> Unit,
  onWithdrawCommissionMpesa: (Long, String, Boolean) -> Unit,
) {
  var showAddMethodDialog by remember { mutableStateOf(false) }
  var showPayoutDialog by remember { mutableStateOf(false) }
  var showCommissionMpesaDialog by remember { mutableStateOf(false) }
  val scholarlyColors = LocalScholarlyColors.current

  val settledEarnings = transactions.filter { it.status == "settled" }.sumOf { it.tutorNetUsd }
  val settledPlatformShare = transactions.filter { it.status == "settled" }.sumOf { it.platformFeeUsd }
  val escrowPlatformShare = transactions.filter { it.status == "authorized" }.sumOf { it.platformFeeUsd }
  val authorizedEscrow = transactions.filter { it.status == "authorized" }.sumOf { it.tutorNetUsd }

  val totalTutorPaidOut = payouts.filter { it.payoutType != "platform_commission" }.sumOf { it.amountUsd }
  val availableForPayout = (settledEarnings - totalTutorPaidOut).coerceAtLeast(0L)

  val totalCommissionPaidOut = payouts.filter { it.payoutType == "platform_commission" }.sumOf { it.amountUsd }
  val availableCommission = (settledPlatformShare - totalCommissionPaidOut).coerceAtLeast(0L)
  val savedMpesaPhone = remember(paymentMethods) { extractSavedMpesaPhone(paymentMethods) }

  LazyColumn(
    modifier = Modifier.fillMaxSize(),
    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp),
  ) {
    // Payout Earnings Overview Card
    item {
      Surface(
        shape = MaterialTheme.shapes.large,
        modifier = Modifier.fillMaxWidth(),
      ) {
        Box(
          modifier = Modifier
            .background(
              Brush.linearGradient(
                colors = listOf(scholarlyColors.heroGradientStart, scholarlyColors.heroGradientEnd)
              )
            )
            .padding(18.dp),
        ) {
          Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Column {
                Text(
                  text = "GLOBAL PAYOUT BALANCE (${selectedCurrency.code})",
                  style = MaterialTheme.typography.labelSmall,
                  color = Color.White.copy(alpha = 0.8f),
                )
                Text(
                  text = GlobalLocalization.formatUsd(availableForPayout, selectedCurrency),
                  style = MaterialTheme.typography.displayMedium,
                  color = Color.White,
                )
              }
              Icon(
                imageVector = Icons.Filled.AccountBalance,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.85f),
                modifier = Modifier.size(32.dp),
              )
            }

            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
            ) {
              Text(
                text = "Escrow: ${GlobalLocalization.formatUsd(authorizedEscrow, selectedCurrency)}",
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.85f),
              )
              Text(
                text = "10% Fee: ${GlobalLocalization.formatUsd(settledPlatformShare, selectedCurrency)}",
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.85f),
              )
              Text(
                text = "Paid: ${GlobalLocalization.formatUsd(totalTutorPaidOut, selectedCurrency)}",
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.85f),
              )
            }

            Button(
              onClick = { showPayoutDialog = true },
              enabled = !isBusy && (availableForPayout > 0L || builderModeEnabled),
              modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("request_payout_button"),
            ) {
              Icon(Icons.Filled.Payments, contentDescription = null, modifier = Modifier.size(18.dp))
              Spacer(modifier = Modifier.width(8.dp))
              Text(
                when {
                  availableForPayout > 0L -> "Withdraw ${GlobalLocalization.formatUsd(availableForPayout, selectedCurrency)} Worldwide"
                  builderModeEnabled -> "Check Payout Flow (Builder Mode)"
                  else -> "Complete sessions to unlock payouts"
                }
              )
            }
          }
        }
      }
    }

    // 10% Platform Commission Direct M-Pesa Wallet Card
    item {
      PlatformCommissionCard(
        availableCommissionUsd = availableCommission,
        escrowCommissionUsd = escrowPlatformShare,
        settledCommissionUsd = settledPlatformShare,
        withdrawnCommissionUsd = totalCommissionPaidOut,
        savedMpesaPhone = savedMpesaPhone,
        selectedCurrency = selectedCurrency,
        builderModeEnabled = builderModeEnabled,
        isBusy = isBusy,
        onWithdrawCommissionClick = { showCommissionMpesaDialog = true },
      )
    }

    // Payment Methods Section
    item {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text("Payment Methods", style = MaterialTheme.typography.titleLarge)
        OutlinedButton(
          onClick = { showAddMethodDialog = true },
          modifier = Modifier.testTag("add_payment_method_button"),
        ) {
          Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
          Spacer(modifier = Modifier.width(4.dp))
          Text("Add Method")
        }
      }
    }

    if (paymentMethods.isEmpty()) {
      item {
        Surface(
          shape = MaterialTheme.shapes.medium,
          color = MaterialTheme.colorScheme.surface,
          border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
          modifier = Modifier.fillMaxWidth(),
        ) {
          Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
          ) {
            Icon(Icons.Filled.CreditCard, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Text(
              text = "Google Pay, M-Pesa, and PayPal are ready at checkout. Save your phone, email, card, or bank details here.",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        }
      }
    } else {
      items(paymentMethods, key = { it.id }) { pm ->
        Surface(
          shape = MaterialTheme.shapes.medium,
          color = MaterialTheme.colorScheme.surface,
          border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
          modifier = Modifier.fillMaxWidth(),
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
              Icon(Icons.Filled.CreditCard, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
              Column {
                Text(pm.label, style = MaterialTheme.typography.titleSmall)
                Text(
                  text = pm.expiryLabel?.let { "Expires $it" } ?: pm.methodType.uppercase(),
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
              }
            }
            IconButton(onClick = { onDeletePaymentMethod(pm.id) }) {
              Icon(Icons.Filled.DeleteOutline, contentDescription = "Remove payment method")
            }
          }
        }
      }
    }

    // Transactions Ledger
    item {
      Text("Session Transactions", style = MaterialTheme.typography.titleLarge)
    }

    if (transactions.isEmpty()) {
      item {
        Text(
          text = "No session transactions recorded yet.",
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
    } else {
      items(transactions, key = { it.id }) { tx ->
        Surface(
          shape = MaterialTheme.shapes.medium,
          color = MaterialTheme.colorScheme.surface,
          border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
          modifier = Modifier.fillMaxWidth(),
        ) {
          Row(
            modifier = Modifier.padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(10.dp),
              modifier = Modifier.weight(1f),
            ) {
              Icon(Icons.Filled.ReceiptLong, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
              Column {
                Text(tx.tutorName, style = MaterialTheme.typography.titleSmall)
                Text(
                  text = "${tx.paymentMethodLabel} · ${tx.receiptCode}",
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
              }
            }
            Column(horizontalAlignment = Alignment.End) {
              Text(
                text = GlobalLocalization.formatUsd(tx.amountUsd, selectedCurrency),
                style = MaterialTheme.typography.titleMedium,
              )
              Text(
                text = tx.status.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = if (tx.status == "settled") scholarlyColors.verifiedBadgeFg else MaterialTheme.colorScheme.secondary,
              )
            }
          }
        }
      }
    }

    // Payout History
    if (payouts.isNotEmpty()) {
      item {
        Text("Payout Disbursements", style = MaterialTheme.typography.titleLarge)
      }
      items(payouts, key = { it.id }) { payout ->
        Surface(
          shape = MaterialTheme.shapes.medium,
          color = MaterialTheme.colorScheme.surface,
          border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
          modifier = Modifier.fillMaxWidth(),
        ) {
          Row(
            modifier = Modifier.padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Column(modifier = Modifier.weight(1f)) {
              Text("${payout.payoutChannel} · ${payout.destinationLabel}", style = MaterialTheme.typography.titleSmall)
              val typeLabel = if (payout.payoutType == "platform_commission") {
                "10% Platform Commission"
              } else {
                "Tutor Earnings"
              }
              Text(
                text = "$typeLabel · ${payout.referenceCode}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
            Column(horizontalAlignment = Alignment.End) {
              Text(
                text = "+${GlobalLocalization.formatUsd(payout.amountUsd, selectedCurrency)}",
                style = MaterialTheme.typography.titleMedium,
                color = scholarlyColors.verifiedBadgeFg,
              )
              Text(payout.status.uppercase(), style = MaterialTheme.typography.labelSmall)
            }
          }
        }
      }
    }
  }

  if (showAddMethodDialog) {
    AddPaymentMethodDialog(
      onDismiss = { showAddMethodDialog = false },
      onAdd = { type, label, last4, expiry ->
        onAddPaymentMethod(type, label, last4, expiry)
        showAddMethodDialog = false
      },
    )
  }

  if (showPayoutDialog) {
    val effectiveMax = if (availableForPayout > 0L) availableForPayout else 100L
    RequestPayoutDialog(
      maxAmountUsd = effectiveMax,
      selectedCurrency = selectedCurrency,
      isBuilderPreview = availableForPayout == 0L && builderModeEnabled,
      onDismiss = { showPayoutDialog = false },
      onSubmit = { amount, dest, channel ->
        onRequestPayout(amount, dest, channel)
        showPayoutDialog = false
      },
    )
  }

  if (showCommissionMpesaDialog) {
    val effectiveCommMax = if (availableCommission > 0L) availableCommission else 50L
    WithdrawCommissionMpesaDialog(
      maxCommissionUsd = effectiveCommMax,
      initialMpesaPhone = savedMpesaPhone,
      selectedCurrency = selectedCurrency,
      isBuilderPreview = availableCommission == 0L && builderModeEnabled,
      onDismiss = { showCommissionMpesaDialog = false },
      onConfirm = { amountUsd, phone, remember ->
        onWithdrawCommissionMpesa(amountUsd, phone, remember)
        showCommissionMpesaDialog = false
      },
    )
  }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AddPaymentMethodDialog(
  onDismiss: () -> Unit,
  onAdd: (String, String, String, String?) -> Unit,
) {
  var methodType by remember { mutableStateOf("mpesa") }
  var label by remember { mutableStateOf("M-Pesa") }
  var identifier by remember { mutableStateOf("") }
  var expiry by remember { mutableStateOf("09/29") }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text("Add Payment Method") },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        FlowRow(
          horizontalArrangement = Arrangement.spacedBy(6.dp),
          verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          FilterChip(
            selected = methodType == "mpesa",
            onClick = {
              methodType = "mpesa"
              label = "M-Pesa"
              identifier = ""
            },
            label = { Text("M-Pesa") },
            modifier = Modifier.testTag("pm_type_mpesa"),
          )
          FilterChip(
            selected = methodType == "paypal",
            onClick = {
              methodType = "paypal"
              label = "PayPal"
              identifier = ""
            },
            label = { Text("PayPal") },
            modifier = Modifier.testTag("pm_type_paypal"),
          )
          FilterChip(
            selected = methodType == "card",
            onClick = {
              methodType = "card"
              label = "Visa"
              identifier = ""
            },
            label = { Text("Card") },
          )
          FilterChip(
            selected = methodType == "bank_ach",
            onClick = {
              methodType = "bank_ach"
              label = "Bank ACH"
              identifier = ""
            },
            label = { Text("Bank ACH") },
          )
          FilterChip(
            selected = methodType == "google_pay",
            onClick = {
              methodType = "google_pay"
              label = "Google Pay"
              identifier = "GPAY"
            },
            label = { Text("Google Pay") },
          )
        }

        if (methodType == "card" || methodType == "bank_ach") {
          OutlinedTextField(
            value = label,
            onValueChange = { label = it },
            label = { Text("Provider / Bank Name") },
            singleLine = true,
          )
        }

        when (methodType) {
          "mpesa" -> {
            OutlinedTextField(
              value = identifier,
              onValueChange = { identifier = it.take(15) },
              label = { Text("M-Pesa Phone Number") },
              placeholder = { Text("e.g. +254712345678") },
              singleLine = true,
              modifier = Modifier.testTag("pm_identifier_input"),
            )
          }
          "paypal" -> {
            OutlinedTextField(
              value = identifier,
              onValueChange = { identifier = it.take(60) },
              label = { Text("PayPal Email") },
              placeholder = { Text("e.g. scholar@example.com") },
              singleLine = true,
              modifier = Modifier.testTag("pm_identifier_input"),
            )
          }
          "google_pay" -> {
            Text(
              text = "Google Pay uses your signed-in Google Wallet.",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
          else -> {
            OutlinedTextField(
              value = identifier,
              onValueChange = { if (it.length <= 4) identifier = it },
              label = { Text("Last 4 Digits") },
              singleLine = true,
              modifier = Modifier.testTag("pm_identifier_input"),
            )
          }
        }

        if (methodType == "card") {
          OutlinedTextField(
            value = expiry,
            onValueChange = { expiry = it },
            label = { Text("Expiry (MM/YY)") },
            singleLine = true,
          )
        }
      }
    },
    confirmButton = {
      Button(
        onClick = {
          val cleanId = identifier.trim()
          val safeLast4 = when (methodType) {
            "google_pay" -> "GPAY"
            "paypal" -> cleanId.take(4).ifBlank { "PPAL" }
            else -> cleanId.takeLast(4).ifBlank { "0000" }
          }
          val formattedLabel = when (methodType) {
            "google_pay" -> "Google Pay"
            "mpesa" -> if (cleanId.isNotBlank()) "M-Pesa •••• ${cleanId.takeLast(4)}" else "M-Pesa"
            "paypal" -> if (cleanId.isNotBlank()) "PayPal ($cleanId)" else "PayPal"
            else -> "${label.trim()} •••• $safeLast4"
          }
          onAdd(methodType, formattedLabel, safeLast4, if (methodType == "card") expiry else null)
        },
        enabled = when (methodType) {
          "google_pay" -> true
          "mpesa" -> identifier.trim().length >= 4
          "paypal" -> identifier.trim().contains("@") && identifier.trim().length >= 5
          else -> label.trim().length >= 2 && identifier.trim().length >= 4
        },
        modifier = Modifier.testTag("save_payment_method_confirm"),
      ) {
        Text("Save Method")
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) { Text("Cancel") }
    },
  )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RequestPayoutDialog(
  maxAmountUsd: Long,
  selectedCurrency: WorldCurrency,
  isBuilderPreview: Boolean = false,
  onDismiss: () -> Unit,
  onSubmit: (Long, String, String) -> Unit,
) {
  var channel by remember { mutableStateOf("PayPal") }
  var destination by remember { mutableStateOf("tutor@paypal.com") }
  val channels = listOf("PayPal", "M-Pesa", "Bank ACH", "Instant Debit")
  val formattedMax = GlobalLocalization.formatUsd(maxAmountUsd, selectedCurrency)

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text("Request Worldwide Payout") },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        if (isBuilderPreview) {
          Text(
            text = "Builder Check Mode: Testing payout flow without prior paid sessions.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.primary,
          )
        }
        Text("Available to withdraw: $formattedMax", style = MaterialTheme.typography.titleSmall)
        FlowRow(
          horizontalArrangement = Arrangement.spacedBy(6.dp),
          verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          channels.forEach { ch ->
            FilterChip(
              selected = ch == channel,
              onClick = {
                channel = ch
                destination = when (ch) {
                  "M-Pesa" -> "+254 7XX XXX XXX"
                  "PayPal" -> "tutor@paypal.com"
                  else -> "IBAN / Account •••• 8810"
                }
              },
              label = { Text(if (ch == "Bank ACH") "Bank / IBAN" else ch) },
            )
          }
        }
        OutlinedTextField(
          value = destination,
          onValueChange = { destination = it },
          label = {
            Text(
              when (channel) {
                "M-Pesa" -> "M-Pesa Mobile Number (Intl)"
                "PayPal" -> "PayPal Account Email"
                else -> "Bank IBAN / Routing & Account"
              }
            )
          },
          singleLine = true,
        )
      }
    },
    confirmButton = {
      Button(
        onClick = { onSubmit(maxAmountUsd, destination, channel) },
        enabled = destination.trim().length >= 2 && maxAmountUsd >= 1L,
      ) {
        Text("Withdraw $formattedMax")
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) { Text("Cancel") }
    },
  )
}

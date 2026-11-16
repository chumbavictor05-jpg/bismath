package com.example.ui.screens

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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.VideoCall
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.catalog.VerifiedExpertsCatalog
import com.example.data.model.SessionMessage
import com.example.data.model.TutoringSession
import com.example.ui.GlobalLocalization
import com.example.ui.UiState
import com.example.ui.WorldCurrency
import com.example.ui.theme.LocalScholarlyColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SessionWorkspaceScreen(
  session: TutoringSession,
  messagesState: UiState<List<SessionMessage>>,
  hasSubmittedReview: Boolean,
  selectedCurrency: WorldCurrency,
  selectedTimezoneId: String,
  isBusy: Boolean,
  onBack: () -> Unit,
  onSendMessage: (String) -> Unit,
  onReschedule: (dateLabel: String, timeLabel: String, topic: String, notes: String) -> Unit,
  onComplete: (closingNotes: String, onCompletedPromptReview: () -> Unit) -> Unit,
  onCancel: () -> Unit,
  onScheduleReminder: () -> Unit,
  onSubmitReview: (rating: Long, comment: String) -> Unit,
) {
  var messageDraft by remember { mutableStateOf("") }
  var showRescheduleDialog by remember { mutableStateOf(false) }
  var showCancelConfirm by remember { mutableStateOf(false) }
  var showReviewDialog by remember { mutableStateOf(false) }
  val scholarlyColors = LocalScholarlyColors.current
  val expert = remember(session.tutorId) { VerifiedExpertsCatalog.findById(session.tutorId) }

  Scaffold(
    containerColor = MaterialTheme.colorScheme.background,
    topBar = {
      TopAppBar(
        title = {
          Column {
            Text(session.tutorName, style = MaterialTheme.typography.titleMedium)
            Text(
              text = "${session.subject} · ${session.meetingCode}",
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        },
        navigationIcon = {
          IconButton(onClick = onBack, modifier = Modifier.testTag("session_workspace_back")) {
            Icon(
              imageVector = Icons.AutoMirrored.Filled.ArrowBack,
              contentDescription = stringResource(R.string.cd_back),
            )
          }
        },
        colors = TopAppBarDefaults.topAppBarColors(
          containerColor = MaterialTheme.colorScheme.background,
        ),
      )
    },
    bottomBar = {
      Surface(
        tonalElevation = 2.dp,
        color = MaterialTheme.colorScheme.surface,
      ) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .imePadding()
            .padding(horizontal = 16.dp, vertical = 10.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          OutlinedTextField(
            value = messageDraft,
            onValueChange = { messageDraft = it },
            placeholder = { Text("Send prep question or homework note...") },
            singleLine = false,
            maxLines = 3,
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier
              .weight(1f)
              .testTag("session_message_input"),
          )
          Surface(
            shape = CircleShape,
            color = if (messageDraft.isNotBlank()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier
              .size(48.dp)
              .testTag("send_message_button")
              .clickable(enabled = messageDraft.isNotBlank()) {
                val text = messageDraft.trim()
                messageDraft = ""
                onSendMessage(text)
              },
          ) {
            Box(contentAlignment = Alignment.Center) {
              Icon(
                imageVector = Icons.AutoMirrored.Filled.Send,
                contentDescription = stringResource(R.string.cd_send_message),
                tint = if (messageDraft.isNotBlank()) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp),
              )
            }
          }
        }
      }
    },
  ) { innerPadding ->
    LazyColumn(
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding),
      contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
      verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
      // Classroom Header Card
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
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
              ) {
                Surface(shape = CircleShape, color = Color.White.copy(alpha = 0.18f)) {
                  Text(
                    text = session.status.uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                  )
                }
                Text(
                  text = "Room ${session.meetingCode}",
                  style = MaterialTheme.typography.labelSmall,
                  color = Color.White,
                )
              }

              Text(
                text = session.topic,
                style = MaterialTheme.typography.titleLarge,
                color = Color.White,
              )

              Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically,
              ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                  Icon(
                    imageVector = Icons.Filled.CalendarMonth,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.9f),
                    modifier = Modifier.size(16.dp),
                  )
                  Spacer(modifier = Modifier.width(6.dp))
                  Text(
                    text = "${session.scheduledDateLabel} · ${session.scheduledTimeLabel}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.9f),
                  )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                  Icon(
                    imageVector = Icons.Filled.VideoCall,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.9f),
                    modifier = Modifier.size(16.dp),
                  )
                  Spacer(modifier = Modifier.width(4.dp))
                  Text(
                    text = "${session.durationMinutes} min",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.9f),
                  )
                }
              }

              if (!session.paymentMethodLabel.isNullOrBlank()) {
                val paymentStatusLabel = (session.paymentStatus ?: "authorized").uppercase()
                val formattedAmount = GlobalLocalization.formatUsd(session.totalPriceUsd, selectedCurrency)
                val tzShort = GlobalLocalization.formatTimezoneLabel(selectedTimezoneId)
                val paymentSummary = if (session.paymentStatus == "waived") {
                  "Access: Payment Optional (Waived) · $tzShort"
                } else {
                  "Payment: ${session.paymentMethodLabel} · $paymentStatusLabel ($formattedAmount) · $tzShort"
                }
                Text(
                  text = paymentSummary,
                  style = MaterialTheme.typography.labelSmall,
                  color = Color.White.copy(alpha = 0.88f),
                )
              }

              if (session.notes.isNotBlank()) {
                Surface(
                  shape = MaterialTheme.shapes.small,
                  color = Color.White.copy(alpha = 0.14f),
                  modifier = Modifier.fillMaxWidth(),
                ) {
                  Text(
                    text = "Prep Notes: ${session.notes}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White,
                    modifier = Modifier.padding(10.dp),
                  )
                }
              }
            }
          }
        }
      }

      // Lifecycle Action Buttons
      item {
        when (session.status) {
          "scheduled" -> {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
              ) {
                OutlinedButton(
                  onClick = { showRescheduleDialog = true },
                  enabled = !isBusy,
                  modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .testTag("reschedule_session_button"),
                ) {
                  Text("Reschedule")
                }
                Button(
                  onClick = {
                    onComplete(
                      session.notes.ifBlank { "Session completed with ${session.tutorName}." }
                    ) {
                      if (!hasSubmittedReview) {
                        showReviewDialog = true
                      }
                    }
                  },
                  enabled = !isBusy,
                  modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .testTag("complete_session_button"),
                ) {
                  Icon(
                    imageVector = Icons.Filled.CheckCircle,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                  )
                  Spacer(modifier = Modifier.width(6.dp))
                  Text("Mark Complete")
                }
              }
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
              ) {
                TextButton(
                  onClick = onScheduleReminder,
                  enabled = !isBusy,
                  modifier = Modifier.testTag("schedule_reminder_button"),
                ) {
                  Icon(
                    imageVector = Icons.Filled.NotificationsActive,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                  )
                  Spacer(modifier = Modifier.width(6.dp))
                  Text("Remind Me (15m)")
                }
                TextButton(
                  onClick = { showCancelConfirm = true },
                  enabled = !isBusy,
                  colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.error,
                  ),
                  modifier = Modifier.testTag("cancel_session_button"),
                ) {
                  Text("Cancel Session")
                }
              }
            }
          }
          "completed" -> {
            if (hasSubmittedReview) {
              Surface(
                shape = MaterialTheme.shapes.medium,
                color = scholarlyColors.verifiedBadgeBg,
                modifier = Modifier.fillMaxWidth(),
              ) {
                Row(
                  modifier = Modifier.padding(14.dp),
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                  Icon(
                    imageVector = Icons.Filled.CheckCircle,
                    contentDescription = null,
                    tint = scholarlyColors.verifiedBadgeFg,
                  )
                  Text(
                    text = "Verified review published for ${session.tutorName}. Thank you!",
                    style = MaterialTheme.typography.labelLarge,
                    color = scholarlyColors.verifiedBadgeFg,
                  )
                }
              }
            } else {
              Surface(
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)),
                modifier = Modifier.fillMaxWidth(),
              ) {
                Column(
                  modifier = Modifier.padding(16.dp),
                  verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                  Text(
                    text = "How was your session with ${session.tutorName}?",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                  )
                  Text(
                    text = "Share a star rating and brief review to update ${session.tutorName}'s verified profile rating.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                  )
                  Button(
                    onClick = { showReviewDialog = true },
                    modifier = Modifier
                      .fillMaxWidth()
                      .height(48.dp)
                      .testTag("leave_review_button"),
                  ) {
                    Icon(
                      imageVector = Icons.Filled.Star,
                      contentDescription = null,
                      modifier = Modifier.size(18.dp),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Rate & Review ${session.tutorName}")
                  }
                }
              }
            }
          }
        }
      }

      // Prep Chat Header
      item {
        Text(
          text = "Pre-Session Q&A & Notes",
          style = MaterialTheme.typography.titleMedium,
          color = MaterialTheme.colorScheme.onBackground,
        )
      }

      when (messagesState) {
        is UiState.Loading -> {
          item {
            Box(modifier = Modifier.fillMaxWidth().padding(20.dp), contentAlignment = Alignment.Center) {
              CircularProgressIndicator(modifier = Modifier.size(28.dp))
            }
          }
        }
        is UiState.Error -> {
          item {
            Text(
              text = messagesState.message,
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.error,
            )
          }
        }
        is UiState.Success -> {
          items(messagesState.data, key = { it.id }) { msg ->
            MessageBubble(message = msg)
          }
        }
      }
    }
  }

  if (showRescheduleDialog) {
    RescheduleDialog(
      session = session,
      availableDays = expert?.availableDays ?: listOf("Mon, Nov 17", "Tue, Nov 18", "Thu, Nov 20"),
      availableTimes = expert?.availableTimeSlots ?: listOf("10:00 AM", "2:00 PM", "5:30 PM"),
      onDismiss = { showRescheduleDialog = false },
      onSave = { date, time, topic, notes ->
        onReschedule(date, time, topic, notes)
        showRescheduleDialog = false
      },
    )
  }

  if (showCancelConfirm) {
    AlertDialog(
      onDismissRequest = { showCancelConfirm = false },
      title = { Text("Cancel Tutoring Session?") },
      text = {
        Text("This will mark your ${session.scheduledDateLabel} session with ${session.tutorName} as cancelled.")
      },
      confirmButton = {
        TextButton(
          onClick = {
            showCancelConfirm = false
            onCancel()
          },
          colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
        ) {
          Text("Cancel Session")
        }
      },
      dismissButton = {
        TextButton(onClick = { showCancelConfirm = false }) {
          Text("Keep Session")
        }
      },
    )
  }

  if (showReviewDialog) {
    ReviewDialog(
      tutorName = session.tutorName,
      onDismiss = { showReviewDialog = false },
      onSubmit = { rating, comment ->
        onSubmitReview(rating, comment)
        showReviewDialog = false
      },
    )
  }
}

@Composable
private fun MessageBubble(message: SessionMessage) {
  val isStudent = message.senderRole == "student"
  Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = if (isStudent) Arrangement.End else Arrangement.Start,
  ) {
    Surface(
      shape = RoundedCornerShape(
        topStart = 16.dp,
        topEnd = 16.dp,
        bottomStart = if (isStudent) 16.dp else 4.dp,
        bottomEnd = if (isStudent) 4.dp else 16.dp,
      ),
      color = if (isStudent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
      border = if (isStudent) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
      modifier = Modifier.widthIn(max = 310.dp),
    ) {
      Column(
        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
      ) {
        Text(
          text = message.senderName,
          style = MaterialTheme.typography.labelSmall,
          color = if (isStudent) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f) else MaterialTheme.colorScheme.primary,
          fontWeight = FontWeight.SemiBold,
        )
        Text(
          text = message.body,
          style = MaterialTheme.typography.bodyMedium,
          color = if (isStudent) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
        )
      }
    }
  }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RescheduleDialog(
  session: TutoringSession,
  availableDays: List<String>,
  availableTimes: List<String>,
  onDismiss: () -> Unit,
  onSave: (String, String, String, String) -> Unit,
) {
  var date by remember { mutableStateOf(session.scheduledDateLabel) }
  var time by remember { mutableStateOf(session.scheduledTimeLabel) }
  var topic by remember { mutableStateOf(session.topic) }
  var notes by remember { mutableStateOf(session.notes) }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text("Reschedule Session") },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("New Day", style = MaterialTheme.typography.labelLarge)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
          availableDays.forEach { d ->
            FilterChip(selected = d == date, onClick = { date = d }, label = { Text(d) })
          }
        }
        Text("New Time", style = MaterialTheme.typography.labelLarge)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
          availableTimes.forEach { t ->
            FilterChip(selected = t == time, onClick = { time = t }, label = { Text(t) })
          }
        }
        OutlinedTextField(
          value = topic,
          onValueChange = { topic = it },
          label = { Text("Topic") },
          singleLine = true,
        )
        OutlinedTextField(
          value = notes,
          onValueChange = { notes = it },
          label = { Text("Prep Notes") },
          maxLines = 3,
        )
      }
    },
    confirmButton = {
      Button(onClick = { onSave(date, time, topic.ifBlank { session.topic }, notes) }) {
        Text("Save Changes")
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) { Text("Cancel") }
    },
  )
}

@Composable
private fun ReviewDialog(
  tutorName: String,
  onDismiss: () -> Unit,
  onSubmit: (Long, String) -> Unit,
) {
  var rating by remember { mutableLongStateOf(5L) }
  var comment by remember { mutableStateOf("") }
  val scholarlyColors = LocalScholarlyColors.current

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text("Review $tutorName") },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Select Rating", style = MaterialTheme.typography.labelLarge)
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
          (1L..5L).forEach { star ->
            IconButton(onClick = { rating = star }) {
              Icon(
                imageVector = if (star <= rating) Icons.Filled.Star else Icons.Outlined.StarOutline,
                contentDescription = "$star stars",
                tint = if (star <= rating) scholarlyColors.ratingStar else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(28.dp),
              )
            }
          }
        }
        OutlinedTextField(
          value = comment,
          onValueChange = { comment = it },
          label = { Text("Share your learning experience") },
          placeholder = { Text("How clear and helpful was the session?") },
          minLines = 3,
          modifier = Modifier.testTag("review_comment_input"),
        )
      }
    },
    confirmButton = {
      Button(
        onClick = { onSubmit(rating, comment.trim()) },
        enabled = comment.trim().length >= 3,
        modifier = Modifier.testTag("submit_review_button"),
      ) {
        Text("Publish Review")
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) { Text("Cancel") }
    },
  )
}

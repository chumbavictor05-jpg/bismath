package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.VideoCall
import androidx.compose.material.icons.outlined.EventAvailable
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.TutoringSession
import com.example.ui.GlobalLocalization
import com.example.ui.UiState
import com.example.ui.WorldCurrency
import com.example.ui.theme.LocalScholarlyColors

@Composable
fun SessionsScreen(
  bookingsState: UiState<List<TutoringSession>>,
  selectedCurrency: WorldCurrency,
  selectedTimezoneId: String,
  onOpenSession: (String) -> Unit,
  onExploreExperts: () -> Unit,
) {
  var selectedTab by remember { mutableIntStateOf(0) }
  val tabs = listOf("Upcoming", "Completed", "Cancelled")
  val tzLabel = remember(selectedTimezoneId) { GlobalLocalization.formatTimezoneLabel(selectedTimezoneId) }

  Column(
    modifier = Modifier
      .fillMaxSize()
      .background(MaterialTheme.colorScheme.background),
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 20.dp)
        .padding(top = 16.dp, bottom = 10.dp),
    ) {
      Text(
        text = "Global Schedule · $tzLabel",
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.secondary,
        fontWeight = FontWeight.SemiBold,
      )
      Spacer(modifier = Modifier.height(2.dp))
      Text(
        text = "Tutoring Sessions",
        style = MaterialTheme.typography.headlineLarge,
      )
    }

    when (bookingsState) {
      is UiState.Loading -> {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
          CircularProgressIndicator()
        }
      }
      is UiState.Error -> {
        Box(
          modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
          contentAlignment = Alignment.Center,
        ) {
          Text(
            text = bookingsState.message,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.error,
          )
        }
      }
      is UiState.Success -> {
        val all = bookingsState.data
        val upcoming = all.filter { it.status == "scheduled" }
        val completed = all.filter { it.status == "completed" }
        val cancelled = all.filter { it.status == "cancelled" }
        val counts = listOf(upcoming.size, completed.size, cancelled.size)

        TabRow(
          selectedTabIndex = selectedTab,
          containerColor = MaterialTheme.colorScheme.background,
        ) {
          tabs.forEachIndexed { index, title ->
            Tab(
              selected = selectedTab == index,
              onClick = { selectedTab = index },
              text = {
                Text(
                  text = "$title (${counts[index]})",
                  style = MaterialTheme.typography.labelLarge,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                )
              },
              modifier = Modifier.testTag("sessions_tab_$index"),
            )
          }
        }

        val currentList = when (selectedTab) {
          0 -> upcoming
          1 -> completed
          else -> cancelled
        }

        if (currentList.isEmpty()) {
          EmptySessionsState(
            isUpcoming = selectedTab == 0,
            onExploreExperts = onExploreExperts,
          )
        } else {
          LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
          ) {
            items(currentList, key = { it.id }) { session ->
              SessionCard(
                session = session,
                selectedCurrency = selectedCurrency,
                onClick = { onOpenSession(session.id) },
              )
            }
          }
        }
      }
    }
  }
}

@Composable
private fun SessionCard(
  session: TutoringSession,
  selectedCurrency: WorldCurrency,
  onClick: () -> Unit,
) {
  val scholarlyColors = LocalScholarlyColors.current
  val (statusBg, statusFg, statusLabel) = when (session.status) {
    "scheduled" -> Triple(
      MaterialTheme.colorScheme.primaryContainer,
      MaterialTheme.colorScheme.onPrimaryContainer,
      "SCHEDULED",
    )
    "completed" -> Triple(
      scholarlyColors.verifiedBadgeBg,
      scholarlyColors.verifiedBadgeFg,
      "COMPLETED",
    )
    else -> Triple(
      MaterialTheme.colorScheme.surfaceVariant,
      MaterialTheme.colorScheme.onSurfaceVariant,
      "CANCELLED",
    )
  }

  Surface(
    shape = MaterialTheme.shapes.large,
    color = MaterialTheme.colorScheme.surface,
    tonalElevation = 1.dp,
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    modifier = Modifier
      .fillMaxWidth()
      .testTag("session_card_${session.id}")
      .clickable(onClick = onClick),
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
        Surface(shape = CircleShape, color = statusBg) {
          Text(
            text = statusLabel,
            style = MaterialTheme.typography.labelSmall,
            color = statusFg,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
          )
        }
        Surface(
          shape = RoundedCornerShape(8.dp),
          color = MaterialTheme.colorScheme.surfaceVariant,
        ) {
          Text(
            text = session.meetingCode,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
          )
        }
      }

      Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
          text = session.topic,
          style = MaterialTheme.typography.titleLarge,
          maxLines = 2,
          overflow = TextOverflow.Ellipsis,
        )
        Text(
          text = "${session.tutorName} · ${session.subject}",
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.primary,
          fontWeight = FontWeight.Medium,
        )
      }

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(
            imageVector = Icons.Filled.CalendarMonth,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(16.dp),
          )
          Spacer(modifier = Modifier.width(6.dp))
          Text(
            text = "${session.scheduledDateLabel}, ${session.scheduledTimeLabel}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(
            imageVector = Icons.Filled.VideoCall,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(16.dp),
          )
          Spacer(modifier = Modifier.width(4.dp))
          val priceOrOptional = if (session.paymentStatus == "waived" || session.totalPriceUsd == 0L) {
            "Payment Optional"
          } else {
            GlobalLocalization.formatUsd(session.totalPriceUsd, selectedCurrency)
          }
          Text(
            text = "${session.durationMinutes}m · $priceOrOptional",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }

      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(top = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(
          text = session.sessionFormat,
          style = MaterialTheme.typography.labelMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(
            imageVector = Icons.Filled.ChatBubbleOutline,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(16.dp),
          )
          Spacer(modifier = Modifier.width(4.dp))
          Text(
            text = "Open Workspace & Chat",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
          )
        }
      }
    }
  }
}

@Composable
private fun EmptySessionsState(
  isUpcoming: Boolean,
  onExploreExperts: () -> Unit,
) {
  Box(
    modifier = Modifier
      .fillMaxSize()
      .padding(32.dp),
    contentAlignment = Alignment.Center,
  ) {
    Column(
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
        modifier = Modifier.size(72.dp),
      ) {
        Box(contentAlignment = Alignment.Center) {
          Icon(
            imageVector = Icons.Outlined.EventAvailable,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(34.dp),
          )
        }
      }
      Text(
        text = if (isUpcoming) stringResource(R.string.empty_sessions_title) else "No sessions here yet",
        style = MaterialTheme.typography.titleLarge,
        textAlign = TextAlign.Center,
      )
      Text(
        text = if (isUpcoming) {
          stringResource(R.string.empty_sessions_subtitle)
        } else {
          "Completed or cancelled sessions will appear here for your records."
        },
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
      )
      if (isUpcoming) {
        Spacer(modifier = Modifier.height(4.dp))
        Button(onClick = onExploreExperts, modifier = Modifier.testTag("empty_explore_button")) {
          Text("Browse Verified Experts")
        }
      }
    }
  }
}

package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.R
import com.example.data.catalog.VerifiedExpertsCatalog
import com.example.data.model.SavedPaymentMethod
import com.example.data.model.SubjectExpert
import com.example.data.model.TutorReview
import com.example.ui.GlobalLocalization
import com.example.ui.RatingSummary
import com.example.ui.UiState
import com.example.ui.WorldCurrency
import com.example.ui.theme.LocalScholarlyColors
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ExpertDetailScreen(
  expert: SubjectExpert,
  isSaved: Boolean,
  ratingSummary: RatingSummary?,
  reviewsState: UiState<List<TutorReview>>,
  paymentMethods: List<SavedPaymentMethod>,
  bookedSlotsForTutor: Set<String>,
  selectedCurrency: WorldCurrency,
  selectedTimezoneId: String,
  builderModeEnabled: Boolean,
  isBusy: Boolean,
  onBack: () -> Unit,
  onToggleBookmark: () -> Unit,
  onBookSession: (
    subject: String,
    topic: String,
    sessionFormat: String,
    dateLabel: String,
    timeLabel: String,
    durationMinutes: Long,
    notes: String,
    paymentMethodLabel: String,
    skipPayment: Boolean,
  ) -> Unit,
) {
  var showBookingSheet by remember { mutableStateOf(false) }
  val scholarlyColors = LocalScholarlyColors.current
  val displayRating = ratingSummary?.averageRating ?: expert.rating
  val displayReviewCount = ratingSummary?.totalReviewCount ?: expert.reviewCount

  Scaffold(
    containerColor = MaterialTheme.colorScheme.background,
    topBar = {
      TopAppBar(
        title = { Text("Expert Profile", style = MaterialTheme.typography.titleLarge) },
        navigationIcon = {
          IconButton(onClick = onBack, modifier = Modifier.testTag("expert_detail_back")) {
            Icon(
              imageVector = Icons.AutoMirrored.Filled.ArrowBack,
              contentDescription = stringResource(R.string.cd_back),
            )
          }
        },
        actions = {
          IconButton(onClick = onToggleBookmark, modifier = Modifier.testTag("expert_detail_bookmark")) {
            Icon(
              imageVector = if (isSaved) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
              contentDescription = stringResource(
                if (isSaved) R.string.cd_bookmark_remove else R.string.cd_bookmark_add
              ),
              tint = if (isSaved) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurface,
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
        tonalElevation = 3.dp,
        shadowElevation = 6.dp,
        color = MaterialTheme.colorScheme.surface,
      ) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 20.dp, vertical = 14.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Column {
            Text(
              text = "${GlobalLocalization.formatUsd(expert.hourlyRateUsd, selectedCurrency)}/hour",
              style = MaterialTheme.typography.headlineSmall,
              color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
              text = "30, 45, 60 or 90 min · ${selectedCurrency.code}",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
          Button(
            onClick = { showBookingSheet = true },
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier
              .height(50.dp)
              .testTag("open_booking_sheet_button"),
          ) {
            Text("Book 1-on-1 Session", style = MaterialTheme.typography.labelLarge)
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
      verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
      // Hero Identity Header
      item {
        Surface(
          shape = MaterialTheme.shapes.large,
          color = MaterialTheme.colorScheme.surface,
          border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
          modifier = Modifier.fillMaxWidth(),
        ) {
          Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
          ) {
            Row(
              horizontalArrangement = Arrangement.spacedBy(16.dp),
              verticalAlignment = Alignment.CenterVertically,
            ) {
              AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                  .data(expert.avatarRes)
                  .crossfade(true)
                  .build(),
                contentDescription = "${expert.name} portrait",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                  .size(86.dp)
                  .clip(RoundedCornerShape(22.dp)),
              )
              Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Surface(
                  shape = CircleShape,
                  color = scholarlyColors.verifiedBadgeBg,
                ) {
                  Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                  ) {
                    Icon(
                      imageVector = Icons.Filled.Verified,
                      contentDescription = null,
                      tint = scholarlyColors.verifiedBadgeFg,
                      modifier = Modifier.size(14.dp),
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                      text = expert.verificationBadge,
                      style = MaterialTheme.typography.labelSmall,
                      color = scholarlyColors.verifiedBadgeFg,
                    )
                  }
                }
                Text(
                  text = expert.name,
                  style = MaterialTheme.typography.headlineMedium,
                )
                Text(
                  text = "${expert.title} · ${expert.institution}",
                  style = MaterialTheme.typography.bodyMedium,
                  color = MaterialTheme.colorScheme.primary,
                  fontWeight = FontWeight.SemiBold,
                )
                Text(
                  text = "${expert.countryLabel} · ${expert.timeZoneLabel}",
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
              }
            }

            // Metrics strip
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .clip(MaterialTheme.shapes.medium)
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f))
                .padding(vertical = 12.dp, horizontal = 8.dp),
              horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
              MetricColumn(
                value = String.format(Locale.US, "%.2f ★", displayRating),
                label = "$displayReviewCount reviews",
              )
              MetricColumn(
                value = "${expert.sessionsCompleted}+",
                label = "Sessions",
              )
              MetricColumn(
                value = expert.responseTimeLabel.removePrefix("Replies in "),
                label = "Avg reply",
              )
            }
          }
        }
      }

      // About & Teaching Style
      item {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Text("Teaching Approach", style = MaterialTheme.typography.titleLarge)
          Text(
            text = expert.bio,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }

      // Subjects Covered
      item {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
          Text("Specializations", style = MaterialTheme.typography.titleLarge)
          FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            expert.subjects.forEach { subject ->
              Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
              ) {
                Text(
                  text = subject,
                  style = MaterialTheme.typography.labelLarge,
                  color = MaterialTheme.colorScheme.onPrimaryContainer,
                  modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                )
              }
            }
          }
        }
      }

      // Verified Academic Credentials
      item {
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
            Text("Verified Credentials", style = MaterialTheme.typography.titleMedium)
            expert.credentialsList.forEach { cred ->
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
              ) {
                Icon(
                  imageVector = Icons.Filled.CheckCircle,
                  contentDescription = null,
                  tint = scholarlyColors.verifiedBadgeFg,
                  modifier = Modifier.size(18.dp),
                )
                Text(
                  text = cred,
                  style = MaterialTheme.typography.bodyMedium,
                  color = MaterialTheme.colorScheme.onSurface,
                )
              }
            }
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
              Icon(
                imageVector = Icons.Filled.Language,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp),
              )
              Text(
                text = "Languages: ${expert.languages.joinToString(", ")}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
          }
        }
      }

      // Upcoming Availability Preview
      item {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Text("Available Slots This Week", style = MaterialTheme.typography.titleLarge)
          Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(
              imageVector = Icons.Outlined.Schedule,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.primary,
              modifier = Modifier.size(18.dp),
            )
            Text(
              text = "${expert.availableDays.joinToString(" · ")} (${expert.availableTimeSlots.joinToString(", ")})",
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        }
      }

      // Student Reviews from Firestore
      item {
        Text("Verified Student Reviews", style = MaterialTheme.typography.titleLarge)
      }

      when (reviewsState) {
        is UiState.Loading -> {
          item {
            Box(
              modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
              contentAlignment = Alignment.Center,
            ) {
              CircularProgressIndicator(modifier = Modifier.size(28.dp))
            }
          }
        }
        is UiState.Error -> {
          item {
            Text(
              text = reviewsState.message,
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.error,
            )
          }
        }
        is UiState.Success -> {
          if (reviewsState.data.isEmpty()) {
            item {
              Surface(
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.fillMaxWidth(),
              ) {
                Text(
                  text = "Book and complete a session with ${expert.name} to leave the first verified student review.",
                  style = MaterialTheme.typography.bodyMedium,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                  modifier = Modifier.padding(16.dp),
                )
              }
            }
          } else {
            items(reviewsState.data, key = { it.id }) { review ->
              ReviewCard(review = review)
            }
          }
        }
      }
    }
  }

  if (showBookingSheet) {
    BookingBottomSheet(
      expert = expert,
      paymentMethods = paymentMethods,
      bookedSlotsForTutor = bookedSlotsForTutor,
      selectedCurrency = selectedCurrency,
      selectedTimezoneId = selectedTimezoneId,
      builderModeEnabled = builderModeEnabled,
      isBusy = isBusy,
      onDismiss = { showBookingSheet = false },
      onConfirm = { subject, topic, format, date, time, duration, notes, paymentMethod, skipPayment ->
        onBookSession(subject, topic, format, date, time, duration, notes, paymentMethod, skipPayment)
        showBookingSheet = false
      },
    )
  }
}

@Composable
private fun MetricColumn(value: String, label: String) {
  Column(horizontalAlignment = Alignment.CenterHorizontally) {
    Text(
      text = value,
      style = MaterialTheme.typography.titleMedium,
      color = MaterialTheme.colorScheme.onSurface,
      fontWeight = FontWeight.Bold,
    )
    Text(
      text = label,
      style = MaterialTheme.typography.bodySmall,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
  }
}

@Composable
private fun ReviewCard(review: TutorReview) {
  val scholarlyColors = LocalScholarlyColors.current
  Surface(
    shape = MaterialTheme.shapes.medium,
    color = MaterialTheme.colorScheme.surface,
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    modifier = Modifier.fillMaxWidth(),
  ) {
    Column(
      modifier = Modifier.padding(14.dp),
      verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(
          text = review.authorName,
          style = MaterialTheme.typography.titleSmall,
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(
            imageVector = Icons.Filled.Star,
            contentDescription = null,
            tint = scholarlyColors.ratingStar,
            modifier = Modifier.size(16.dp),
          )
          Spacer(modifier = Modifier.width(4.dp))
          Text(
            text = "${review.rating}/5",
            style = MaterialTheme.typography.labelLarge,
          )
        }
      }
      Text(
        text = review.subject,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.primary,
      )
      Text(
        text = review.comment,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun BookingBottomSheet(
  expert: SubjectExpert,
  paymentMethods: List<SavedPaymentMethod>,
  bookedSlotsForTutor: Set<String>,
  selectedCurrency: WorldCurrency,
  selectedTimezoneId: String,
  builderModeEnabled: Boolean,
  isBusy: Boolean,
  onDismiss: () -> Unit,
  onConfirm: (
    subject: String,
    topic: String,
    sessionFormat: String,
    dateLabel: String,
    timeLabel: String,
    durationMinutes: Long,
    notes: String,
    paymentMethodLabel: String,
    skipPayment: Boolean,
  ) -> Unit,
) {
  var selectedSubject by remember { mutableStateOf(expert.subjects.first()) }
  var topic by remember { mutableStateOf("") }
  var selectedFormat by remember { mutableStateOf(VerifiedExpertsCatalog.sessionFormats.first()) }
  var selectedDate by remember { mutableStateOf(expert.availableDays.first()) }
  var selectedTime by remember {
    mutableStateOf(
      expert.availableTimeSlots.firstOrNull { !bookedSlotsForTutor.contains("${expert.availableDays.first()}|$it") }
        ?: expert.availableTimeSlots.first()
    )
  }
  var durationMinutes by remember { mutableLongStateOf(60L) }
  var notes by remember { mutableStateOf("") }
  var skipPayment by remember(builderModeEnabled) { mutableStateOf(builderModeEnabled) }

  val paymentOptions = remember(paymentMethods) {
    val builtIn = listOf("Google Pay", "M-Pesa", "PayPal", "International Card", "Bank Wire / SEPA")
    val savedLabels = paymentMethods.map { it.label }.distinct()
    (savedLabels + builtIn).distinct()
  }
  var selectedPaymentMethod by remember(paymentOptions) {
    mutableStateOf(
      paymentMethods.firstOrNull { it.isDefault }?.label ?: paymentOptions.first()
    )
  }

  val basePrice = if (skipPayment) 0L else ((expert.hourlyRateUsd.toLong() * durationMinutes) / 60L).coerceAtLeast(5L)
  val platformFee = if (skipPayment) 0L else (basePrice * 10L / 100L).coerceAtLeast(1L)
  val tutorNet = if (skipPayment) 0L else (basePrice - platformFee).coerceAtLeast(1L)
  val totalCharged = basePrice
  val isSelectedSlotBooked = bookedSlotsForTutor.contains("$selectedDate|$selectedTime")
  val studentTzLabel = remember(selectedTimezoneId) { GlobalLocalization.formatTimezoneLabel(selectedTimezoneId) }

  ModalBottomSheet(onDismissRequest = onDismiss) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .verticalScroll(rememberScrollState())
        .padding(horizontal = 24.dp)
        .padding(bottom = 32.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
      Text(
        text = "Schedule with ${expert.name}",
        style = MaterialTheme.typography.headlineSmall,
      )

      Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text("Subject", style = MaterialTheme.typography.titleSmall)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          expert.subjects.forEach { subj ->
            FilterChip(
              selected = subj == selectedSubject,
              onClick = { selectedSubject = subj },
              label = { Text(subj) },
            )
          }
        }
      }

      OutlinedTextField(
        value = topic,
        onValueChange = { topic = it },
        label = { Text("Session Topic or Problem Focus") },
        placeholder = { Text("e.g. Midterm Review: Eigenvalues") },
        singleLine = true,
        modifier = Modifier
          .fillMaxWidth()
          .testTag("booking_topic_input"),
      )

      Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text("Format", style = MaterialTheme.typography.titleSmall)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          VerifiedExpertsCatalog.sessionFormats.forEach { format ->
            FilterChip(
              selected = format == selectedFormat,
              onClick = { selectedFormat = format },
              label = { Text(format) },
            )
          }
        }
      }

      Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text("Select Day", style = MaterialTheme.typography.titleSmall)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          expert.availableDays.forEach { day ->
            FilterChip(
              selected = day == selectedDate,
              onClick = {
                selectedDate = day
                if (bookedSlotsForTutor.contains("$day|$selectedTime")) {
                  expert.availableTimeSlots.firstOrNull { !bookedSlotsForTutor.contains("$day|$it") }?.let {
                    selectedTime = it
                  }
                }
              },
              label = { Text(day) },
            )
          }
        }
      }

      Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text("Select Time Slot ($studentTzLabel)", style = MaterialTheme.typography.titleSmall)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          expert.availableTimeSlots.forEach { slot ->
            val isBooked = bookedSlotsForTutor.contains("$selectedDate|$slot")
            FilterChip(
              selected = slot == selectedTime && !isBooked,
              enabled = !isBooked,
              onClick = { selectedTime = slot },
              label = { Text(if (isBooked) "$slot (Booked)" else slot) },
            )
          }
        }
      }

      Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text("Duration", style = MaterialTheme.typography.titleSmall)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          VerifiedExpertsCatalog.durationOptionsMinutes.forEach { mins ->
            FilterChip(
              selected = mins.toLong() == durationMinutes,
              onClick = { durationMinutes = mins.toLong() },
              label = { Text("${mins}m") },
            )
          }
        }
      }

      Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Surface(
          shape = MaterialTheme.shapes.medium,
          color = if (skipPayment) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
          else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
          modifier = Modifier.fillMaxWidth(),
        ) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = "Payment Optional (Builder Check)",
                style = MaterialTheme.typography.titleSmall,
              )
              Text(
                text = if (skipPayment) "Book & test classroom, FCM, and reviews without paying"
                else "Tap to skip payment and check flow immediately",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
            Switch(
              checked = skipPayment,
              onCheckedChange = { skipPayment = it },
              modifier = Modifier.testTag("skip_payment_switch"),
            )
          }
        }

        if (!skipPayment) {
          Text("Payment Method", style = MaterialTheme.typography.titleSmall)
          FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            paymentOptions.forEach { methodLabel ->
              FilterChip(
                selected = methodLabel == selectedPaymentMethod,
                onClick = { selectedPaymentMethod = methodLabel },
                label = { Text(methodLabel) },
                modifier = Modifier.testTag("payment_method_chip_$methodLabel"),
              )
            }
          }
        }
      }

      OutlinedTextField(
        value = notes,
        onValueChange = { notes = it },
        label = { Text("Preparation Notes (Optional)") },
        placeholder = { Text("Share chapters, goals, or specific questions...") },
        minLines = 2,
        maxLines = 4,
        modifier = Modifier.fillMaxWidth(),
      )

      Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
        modifier = Modifier.fillMaxWidth(),
      ) {
        Column(
          modifier = Modifier.padding(14.dp),
          verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
          ) {
            Text(
              text = "$selectedDate · $selectedTime ($durationMinutes min)",
              style = MaterialTheme.typography.titleSmall,
            )
            Text(selectedFormat, style = MaterialTheme.typography.bodySmall)
          }
          if (skipPayment) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Text(
                text = "Payment Status",
                style = MaterialTheme.typography.titleSmall,
              )
              Text(
                text = "WAIVED (${GlobalLocalization.formatUsd(0L, selectedCurrency)})",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
              )
            }
          } else {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
            ) {
              Text("Tutor Payout (90%)", style = MaterialTheme.typography.bodySmall)
              Text(GlobalLocalization.formatUsd(tutorNet, selectedCurrency), style = MaterialTheme.typography.bodySmall)
            }
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
            ) {
              Text("Platform Share (10%)", style = MaterialTheme.typography.bodySmall)
              Text(GlobalLocalization.formatUsd(platformFee, selectedCurrency), style = MaterialTheme.typography.bodySmall)
            }
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Text(
                text = "Total via $selectedPaymentMethod",
                style = MaterialTheme.typography.titleSmall,
              )
              Text(
                text = GlobalLocalization.formatUsd(totalCharged, selectedCurrency),
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.primary,
              )
            }
          }
        }
      }

      Button(
        onClick = {
          onConfirm(
            selectedSubject,
            topic.ifBlank { "$selectedSubject Coaching" },
            selectedFormat,
            selectedDate,
            selectedTime,
            durationMinutes,
            notes,
            if (skipPayment) "Skipped (Payment Optional)" else selectedPaymentMethod,
            skipPayment,
          )
        },
        enabled = !isBusy && !isSelectedSlotBooked,
        modifier = Modifier
          .fillMaxWidth()
          .height(52.dp)
          .testTag("confirm_booking_button"),
      ) {
        if (isBusy) {
          CircularProgressIndicator(
            modifier = Modifier.size(20.dp),
            color = MaterialTheme.colorScheme.onPrimary,
            strokeWidth = 2.dp,
          )
        } else {
          val formattedTotal = GlobalLocalization.formatUsd(totalCharged, selectedCurrency)
          Text(
            when {
              isSelectedSlotBooked -> "Slot Already Booked"
              skipPayment -> "Confirm Booking Without Payment"
              else -> "Pay with $selectedPaymentMethod ($formattedTotal)"
            }
          )
        }
      }
    }
  }
}

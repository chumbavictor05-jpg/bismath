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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.catalog.VerifiedExpertsCatalog
import com.example.data.model.ExpertApplication
import com.example.data.model.StudentProfile
import com.example.ui.GlobalLocalization
import com.example.ui.UiState
import com.example.ui.WorldCurrency
import com.example.ui.theme.LocalScholarlyColors

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ProfileScreen(
  userEmail: String?,
  defaultDisplayName: String,
  profileState: UiState<StudentProfile?>,
  expertAppState: UiState<ExpertApplication?>,
  completedSessionsCount: Int,
  selectedCurrency: WorldCurrency,
  selectedTimezoneId: String,
  builderModeEnabled: Boolean,
  isBusy: Boolean,
  onSelectCurrency: (WorldCurrency) -> Unit,
  onSelectTimezone: (String) -> Unit,
  onToggleBuilderMode: (Boolean) -> Unit,
  onSaveProfile: (
    displayName: String,
    gradeLevel: String,
    learningGoals: String,
    preferredSubjects: List<String>,
    weeklyGoalHours: Long,
  ) -> Unit,
  onSubmitExpertApplication: (
    fullName: String,
    credentials: String,
    institution: String,
    primarySubject: String,
    hourlyRateUsd: Long,
    bio: String,
  ) -> Unit,
  onSignOut: () -> Unit,
) {
  var showEditProfileDialog by remember { mutableStateOf(false) }
  var showExpertAppDialog by remember { mutableStateOf(false) }
  val scholarlyColors = LocalScholarlyColors.current

  val profile = (profileState as? UiState.Success)?.data
  val expertApp = (expertAppState as? UiState.Success)?.data
  val displayName = profile?.displayName?.ifBlank { defaultDisplayName } ?: defaultDisplayName

  LazyColumn(
    modifier = Modifier
      .fillMaxSize()
      .background(MaterialTheme.colorScheme.background),
    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp),
  ) {
    item {
      Column {
        Text(
          text = "Student Account",
          style = MaterialTheme.typography.labelMedium,
          color = MaterialTheme.colorScheme.secondary,
          fontWeight = FontWeight.SemiBold,
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
          text = "Profile & Study Goals",
          style = MaterialTheme.typography.headlineLarge,
        )
      }
    }

    // Identity Card
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
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
          ) {
            Surface(
              shape = CircleShape,
              color = MaterialTheme.colorScheme.primary,
              modifier = Modifier.size(60.dp),
            ) {
              Box(contentAlignment = Alignment.Center) {
                Text(
                  text = displayName.firstOrNull()?.uppercase() ?: "S",
                  style = MaterialTheme.typography.headlineMedium,
                  color = MaterialTheme.colorScheme.onPrimary,
                )
              }
            }
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = displayName,
                style = MaterialTheme.typography.titleLarge,
              )
              if (!userEmail.isNullOrBlank()) {
                Text(
                  text = userEmail,
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
              }
              Spacer(modifier = Modifier.height(4.dp))
              Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
              ) {
                Text(
                  text = profile?.gradeLevel ?: "Undergraduate",
                  style = MaterialTheme.typography.labelSmall,
                  color = MaterialTheme.colorScheme.onPrimaryContainer,
                  modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                )
              }
            }
          }

          if (profileState is UiState.Loading) {
            CircularProgressIndicator(modifier = Modifier.size(24.dp))
          } else {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
              Surface(
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier.weight(1f),
              ) {
                Column(modifier = Modifier.padding(12.dp)) {
                  Text(
                    text = "${profile?.weeklyGoalHours ?: 4} hrs/wk",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                  )
                  Text(
                    text = "Weekly Study Goal",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                  )
                }
              }
              Surface(
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier.weight(1f),
              ) {
                Column(modifier = Modifier.padding(12.dp)) {
                  Text(
                    text = "$completedSessionsCount",
                    style = MaterialTheme.typography.titleMedium,
                    color = scholarlyColors.verifiedBadgeFg,
                  )
                  Text(
                    text = "Completed Sessions",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                  )
                }
              }
            }

            if (!profile?.learningGoals.isNullOrBlank()) {
              Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Learning Focus", style = MaterialTheme.typography.labelLarge)
                Text(
                  text = profile?.learningGoals ?: "",
                  style = MaterialTheme.typography.bodyMedium,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
              }
            }

            if (!profile?.preferredSubjects.isNullOrEmpty()) {
              FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
              ) {
                profile?.preferredSubjects?.forEach { subj ->
                  Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                  ) {
                    Text(
                      text = subj,
                      style = MaterialTheme.typography.labelMedium,
                      modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                    )
                  }
                }
              }
            }

            OutlinedButton(
              onClick = { showEditProfileDialog = true },
              enabled = !isBusy,
              modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("edit_profile_button"),
            ) {
              Icon(
                imageVector = Icons.Filled.Edit,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
              )
              Spacer(modifier = Modifier.width(8.dp))
              Text("Edit Study Profile & Goals")
            }
          }
        }
      }
    }

    // Builder Check Mode (Payment Optional)
    item {
      Surface(
        shape = MaterialTheme.shapes.large,
        color = if (builderModeEnabled) {
          MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
        } else {
          MaterialTheme.colorScheme.surface
        },
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier
          .fillMaxWidth()
          .testTag("builder_mode_card"),
      ) {
        Row(
          modifier = Modifier.padding(18.dp),
          horizontalArrangement = Arrangement.spacedBy(12.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp),
          ) {
            Text(
              text = "Builder Check Mode (Payment Optional)",
              style = MaterialTheme.typography.titleMedium,
            )
            Text(
              text = if (builderModeEnabled) {
                "Enabled: Book experts with \$0 payment waived, inspect classroom chat, alerts, reviews, payouts, and M-Pesa commissions freely."
              } else {
                "Disabled: Standard checkout payment and settled-balance checks apply."
              },
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
          Switch(
            checked = builderModeEnabled,
            onCheckedChange = onToggleBuilderMode,
            modifier = Modifier.testTag("builder_mode_switch"),
          )
        }
      }
    }

    // Worldwide Currency & Timezone Card
    item {
      Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth(),
      ) {
        Column(
          modifier = Modifier.padding(18.dp),
          verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          Text("Worldwide Currency & Timezone", style = MaterialTheme.typography.titleLarge)
          Text(
            text = "Timezone: ${GlobalLocalization.formatTimezoneLabel(selectedTimezoneId)}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.primary,
          )
          FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
          ) {
            WorldCurrency.entries.forEach { currency ->
              FilterChip(
                selected = currency == selectedCurrency,
                onClick = { onSelectCurrency(currency) },
                label = { Text(currency.code) },
              )
            }
          }
          FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
          ) {
            GlobalLocalization.popularTimezones.take(6).forEach { tz ->
              FilterChip(
                selected = tz.id == selectedTimezoneId,
                onClick = { onSelectTimezone(tz.id) },
                label = { Text(tz.cityLabel) },
              )
            }
          }
        }
      }
    }

    // Apply as Verified Subject Expert Card
    item {
      Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth(),
      ) {
        Column(
          modifier = Modifier.padding(18.dp),
          verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            Icon(
              imageVector = Icons.Filled.VerifiedUser,
              contentDescription = null,
              tint = scholarlyColors.verifiedBadgeFg,
              modifier = Modifier.size(24.dp),
            )
            Text(
              text = "Teach on Scholarly",
              style = MaterialTheme.typography.titleLarge,
            )
          }

          if (expertApp != null) {
            Surface(
              shape = CircleShape,
              color = MaterialTheme.colorScheme.secondaryContainer,
            ) {
              Text(
                text = "APPLICATION STATUS: PENDING ACADEMIC REVIEW",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
              )
            }
            Text(
              text = "${expertApp.fullName} · ${expertApp.credentials} (${expertApp.institution})",
              style = MaterialTheme.typography.titleSmall,
            )
            Text(
              text = "${expertApp.primarySubject} · ${GlobalLocalization.formatUsd(expertApp.hourlyRateUsd, selectedCurrency)}/hr",
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            OutlinedButton(
              onClick = { showExpertAppDialog = true },
              modifier = Modifier.fillMaxWidth().height(48.dp),
            ) {
              Text("Update Expert Application")
            }
          } else {
            Text(
              text = "Are you a PhD candidate, faculty member, or specialist anywhere in the world? Apply for verification to teach globally on Scholarly.",
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Button(
              onClick = { showExpertAppDialog = true },
              modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("apply_expert_button"),
            ) {
              Icon(
                imageVector = Icons.Filled.School,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
              )
              Spacer(modifier = Modifier.width(8.dp))
              Text("Apply as Subject Expert")
            }
          }
        }
      }
    }

    // Sign Out Action
    item {
      OutlinedButton(
        onClick = onSignOut,
        colors = ButtonDefaults.outlinedButtonColors(
          contentColor = MaterialTheme.colorScheme.error,
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.45f)),
        modifier = Modifier
          .fillMaxWidth()
          .height(50.dp)
          .testTag("sign_out_button"),
      ) {
        Icon(
          imageVector = Icons.AutoMirrored.Filled.Logout,
          contentDescription = stringResource(R.string.cd_sign_out),
          modifier = Modifier.size(18.dp),
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text("Sign Out")
      }
    }
  }

  if (showEditProfileDialog) {
    EditProfileDialog(
      initialName = displayName,
      initialGrade = profile?.gradeLevel ?: "Undergraduate",
      initialGoals = profile?.learningGoals ?: "",
      initialSubjects = profile?.preferredSubjects ?: listOf("Mathematics"),
      initialHours = profile?.weeklyGoalHours ?: 4L,
      onDismiss = { showEditProfileDialog = false },
      onSave = { name, grade, goals, subjects, hours ->
        onSaveProfile(name, grade, goals, subjects, hours)
        showEditProfileDialog = false
      },
    )
  }

  if (showExpertAppDialog) {
    ExpertApplicationDialog(
      initialName = expertApp?.fullName ?: displayName,
      initialCredentials = expertApp?.credentials ?: "",
      initialInstitution = expertApp?.institution ?: "",
      initialSubject = expertApp?.primarySubject ?: "Mathematics",
      initialRate = expertApp?.hourlyRateUsd ?: 65L,
      initialBio = expertApp?.bio ?: "",
      onDismiss = { showExpertAppDialog = false },
      onSubmit = { name, cred, inst, subj, rate, bio ->
        onSubmitExpertApplication(name, cred, inst, subj, rate, bio)
        showExpertAppDialog = false
      },
    )
  }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun EditProfileDialog(
  initialName: String,
  initialGrade: String,
  initialGoals: String,
  initialSubjects: List<String>,
  initialHours: Long,
  onDismiss: () -> Unit,
  onSave: (String, String, String, List<String>, Long) -> Unit,
) {
  var name by remember { mutableStateOf(initialName) }
  var grade by remember { mutableStateOf(initialGrade) }
  var goals by remember { mutableStateOf(initialGoals) }
  var selectedSubjects by remember { mutableStateOf(initialSubjects) }
  var hours by remember { mutableLongStateOf(initialHours) }
  val gradeLevels = listOf("High School", "Undergraduate", "Graduate", "Professional")
  val subjectOptions = VerifiedExpertsCatalog.categories.filter { it != "All" }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text("Edit Study Profile") },
    text = {
      Column(
        modifier = Modifier.verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        OutlinedTextField(
          value = name,
          onValueChange = { name = it },
          label = { Text("Display Name") },
          singleLine = true,
        )
        Text("Academic Level", style = MaterialTheme.typography.labelLarge)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
          gradeLevels.forEach { level ->
            FilterChip(
              selected = grade == level,
              onClick = { grade = level },
              label = { Text(level) },
            )
          }
        }
        Text("Preferred Subjects", style = MaterialTheme.typography.labelLarge)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
          subjectOptions.forEach { subj ->
            val checked = selectedSubjects.contains(subj)
            FilterChip(
              selected = checked,
              onClick = {
                selectedSubjects = if (checked) {
                  selectedSubjects - subj
                } else if (selectedSubjects.size < 10) {
                  selectedSubjects + subj
                } else {
                  selectedSubjects
                }
              },
              label = { Text(subj) },
            )
          }
        }
        Text("Weekly Study Goal: $hours hrs", style = MaterialTheme.typography.labelLarge)
        Slider(
          value = hours.toFloat(),
          onValueChange = { hours = it.toLong() },
          valueRange = 1f..20f,
          steps = 18,
        )
        OutlinedTextField(
          value = goals,
          onValueChange = { goals = it },
          label = { Text("Learning Goals") },
          minLines = 2,
          maxLines = 4,
        )
      }
    },
    confirmButton = {
      Button(
        onClick = { onSave(name.ifBlank { "Student" }, grade, goals, selectedSubjects, hours) },
      ) {
        Text("Save Profile")
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) { Text("Cancel") }
    },
  )
}

@Composable
private fun ExpertApplicationDialog(
  initialName: String,
  initialCredentials: String,
  initialInstitution: String,
  initialSubject: String,
  initialRate: Long,
  initialBio: String,
  onDismiss: () -> Unit,
  onSubmit: (String, String, String, String, Long, String) -> Unit,
) {
  var fullName by remember { mutableStateOf(initialName) }
  var credentials by remember { mutableStateOf(initialCredentials) }
  var institution by remember { mutableStateOf(initialInstitution) }
  var primarySubject by remember { mutableStateOf(initialSubject) }
  var hourlyRate by remember { mutableLongStateOf(initialRate) }
  var bio by remember { mutableStateOf(initialBio) }

  val canSubmit = fullName.trim().length >= 2 &&
    credentials.trim().length >= 2 &&
    institution.trim().length >= 2 &&
    primarySubject.trim().length >= 2 &&
    bio.trim().length >= 10

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text("Subject Expert Application") },
    text = {
      Column(
        modifier = Modifier.verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        OutlinedTextField(
          value = fullName,
          onValueChange = { fullName = it },
          label = { Text("Full Name") },
          singleLine = true,
        )
        OutlinedTextField(
          value = credentials,
          onValueChange = { credentials = it },
          label = { Text("Degree / Credentials (e.g. PhD Physics)") },
          singleLine = true,
        )
        OutlinedTextField(
          value = institution,
          onValueChange = { institution = it },
          label = { Text("University or Institution") },
          singleLine = true,
        )
        OutlinedTextField(
          value = primarySubject,
          onValueChange = { primarySubject = it },
          label = { Text("Primary Subject") },
          singleLine = true,
        )
        Text("Hourly Rate: \$$hourlyRate/hr", style = MaterialTheme.typography.labelLarge)
        Slider(
          value = hourlyRate.toFloat(),
          onValueChange = { hourlyRate = it.toLong() },
          valueRange = 25f..200f,
        )
        OutlinedTextField(
          value = bio,
          onValueChange = { bio = it },
          label = { Text("Teaching Bio (min 10 chars)") },
          minLines = 3,
          maxLines = 5,
        )
      }
    },
    confirmButton = {
      Button(
        onClick = { onSubmit(fullName, credentials, institution, primarySubject, hourlyRate, bio) },
        enabled = canSubmit,
      ) {
        Text("Submit for Verification")
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) { Text("Cancel") }
    },
  )
}

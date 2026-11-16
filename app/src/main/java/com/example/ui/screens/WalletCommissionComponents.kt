package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.model.SavedPaymentMethod
import com.example.ui.GlobalLocalization
import com.example.ui.WorldCurrency
import com.example.ui.theme.LocalScholarlyColors

internal fun extractSavedMpesaPhone(paymentMethods: List<SavedPaymentMethod>): String {
  val mpesa = paymentMethods.firstOrNull { it.methodType == "mpesa" } ?: return ""
  val insideParens = mpesa.label.substringAfter("(", "").substringBefore(")", "").trim()
  return insideParens.takeIf { it.count { ch -> ch.isDigit() } >= 6 }.orEmpty()
}

@Composable
fun PlatformCommissionCard(
  availableCommissionUsd: Long,
  escrowCommissionUsd: Long,
  settledCommissionUsd: Long,
  withdrawnCommissionUsd: Long,
  savedMpesaPhone: String,
  selectedCurrency: WorldCurrency,
  builderModeEnabled: Boolean,
  isBusy: Boolean,
  onWithdrawCommissionClick: () -> Unit,
) {
  val scholarlyColors = LocalScholarlyColors.current
  val availablePrimary = GlobalLocalization.formatUsd(availableCommissionUsd, selectedCurrency)
  val availableKes = GlobalLocalization.formatUsd(availableCommissionUsd, WorldCurrency.KES)

  Surface(
    shape = MaterialTheme.shapes.large,
    color = scholarlyColors.verifiedBadgeBg.copy(alpha = 0.45f),
    border = BorderStroke(1.dp, scholarlyColors.verifiedBadgeFg.copy(alpha = 0.35f)),
    modifier = Modifier
      .fillMaxWidth()
      .testTag("platform_commission_card"),
  ) {
    Column(
      modifier = Modifier.padding(18.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = "PLATFORM COMMISSION (10% REVENUE)",
            style = MaterialTheme.typography.labelSmall,
            color = scholarlyColors.verifiedBadgeFg,
            fontWeight = FontWeight.Bold,
          )
          Spacer(modifier = Modifier.height(2.dp))
          Text(
            text = availablePrimary,
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.onSurface,
          )
          if (selectedCurrency != WorldCurrency.KES) {
            Text(
              text = "≈ $availableKes via M-Pesa",
              style = MaterialTheme.typography.bodySmall,
              color = scholarlyColors.verifiedBadgeFg,
              fontWeight = FontWeight.Medium,
            )
          }
        }
        Surface(
          shape = MaterialTheme.shapes.medium,
          color = scholarlyColors.verifiedBadgeBg,
        ) {
          Icon(
            imageVector = Icons.Filled.PhoneAndroid,
            contentDescription = "Direct M-Pesa commission wallet",
            tint = scholarlyColors.verifiedBadgeFg,
            modifier = Modifier.padding(10.dp).size(26.dp),
          )
        }
      }

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        Text(
          text = "Escrow: ${GlobalLocalization.formatUsd(escrowCommissionUsd, selectedCurrency)}",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
          text = "Earned: ${GlobalLocalization.formatUsd(settledCommissionUsd, selectedCurrency)}",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
          text = "To M-Pesa: ${GlobalLocalization.formatUsd(withdrawnCommissionUsd, selectedCurrency)}",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }

      if (savedMpesaPhone.isNotBlank()) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          Icon(
            imageVector = Icons.Filled.AccountBalanceWallet,
            contentDescription = null,
            tint = scholarlyColors.verifiedBadgeFg,
            modifier = Modifier.size(16.dp),
          )
          Text(
            text = "Saved M-Pesa destination: $savedMpesaPhone",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface,
          )
        }
      }

      Button(
        onClick = onWithdrawCommissionClick,
        enabled = !isBusy && (availableCommissionUsd > 0L || builderModeEnabled),
        colors = ButtonDefaults.buttonColors(
          containerColor = scholarlyColors.verifiedBadgeFg,
          contentColor = MaterialTheme.colorScheme.onPrimary,
        ),
        modifier = Modifier
          .fillMaxWidth()
          .height(48.dp)
          .testTag("withdraw_commission_mpesa_button"),
      ) {
        Icon(Icons.Filled.PhoneAndroid, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text(
          when {
            availableCommissionUsd > 0L -> "Withdraw $availablePrimary to M-Pesa"
            builderModeEnabled -> "Check M-Pesa Commission Transfer"
            escrowCommissionUsd > 0L -> "${GlobalLocalization.formatUsd(escrowCommissionUsd, selectedCurrency)} Unlocks on Completion"
            else -> "No Settled Commission Yet"
          }
        )
      }
    }
  }
}

@Composable
fun WithdrawCommissionMpesaDialog(
  maxCommissionUsd: Long,
  initialMpesaPhone: String,
  selectedCurrency: WorldCurrency,
  isBuilderPreview: Boolean = false,
  onDismiss: () -> Unit,
  onConfirm: (amountUsd: Long, phoneNumber: String, rememberNumber: Boolean) -> Unit,
) {
  var mpesaPhone by remember(initialMpesaPhone) {
    mutableStateOf(initialMpesaPhone.ifBlank { "+2547" })
  }
  var amountText by remember(maxCommissionUsd) {
    mutableStateOf(maxCommissionUsd.toString())
  }
  var rememberNumber by remember(initialMpesaPhone) {
    mutableStateOf(initialMpesaPhone.isBlank())
  }

  val parsedAmount = amountText.trim().toLongOrNull() ?: 0L
  val validAmount = parsedAmount in 1L..maxCommissionUsd
  val digitCount = mpesaPhone.count { it.isDigit() }
  val validPhone = digitCount >= 9 && mpesaPhone.trim().length in 9..16

  val previewPrimary = GlobalLocalization.formatUsd(parsedAmount.coerceAtLeast(0L), selectedCurrency)
  val previewKes = GlobalLocalization.formatUsd(parsedAmount.coerceAtLeast(0L), WorldCurrency.KES)

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text("Withdraw Commission to M-Pesa") },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
          text = if (isBuilderPreview) {
            "Builder Check Mode: Test M-Pesa commission withdrawal without prior session payment."
          } else {
            "Direct 10% platform commission payout via M-Pesa instant transfer."
          },
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        OutlinedTextField(
          value = mpesaPhone,
          onValueChange = { input ->
            if (input.length <= 16) {
              mpesaPhone = input.filter { it.isDigit() || it == '+' || it == ' ' }
            }
          },
          label = { Text("M-Pesa Mobile Number") },
          placeholder = { Text("+254712345678") },
          supportingText = { Text("Enter Kenya/East Africa M-Pesa number (e.g. +2547... or 07...)") },
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
          singleLine = true,
          modifier = Modifier
            .fillMaxWidth()
            .testTag("commission_mpesa_phone_input"),
        )

        OutlinedTextField(
          value = amountText,
          onValueChange = { input ->
            if (input.all { it.isDigit() } && input.length <= 6) {
              amountText = input
            }
          },
          label = { Text("Commission Amount (USD)") },
          supportingText = {
            Text(
              if (validAmount) {
                "Transferring $previewPrimary ($previewKes) · Max $${maxCommissionUsd} USD"
              } else {
                "Enter 1 to $maxCommissionUsd USD"
              }
            )
          },
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
          singleLine = true,
          modifier = Modifier
            .fillMaxWidth()
            .testTag("commission_amount_input"),
        )

        Row(
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          FilterChip(
            selected = parsedAmount == maxCommissionUsd,
            onClick = { amountText = maxCommissionUsd.toString() },
            label = { Text("Full ($${maxCommissionUsd})") },
          )
        }

        Row(
          verticalAlignment = Alignment.CenterVertically,
          modifier = Modifier.fillMaxWidth(),
        ) {
          Checkbox(
            checked = rememberNumber,
            onCheckedChange = { rememberNumber = it },
          )
          Text(
            text = "Save M-Pesa number for 1-tap withdrawals",
            style = MaterialTheme.typography.bodySmall,
          )
        }
      }
    },
    confirmButton = {
      Button(
        onClick = {
          onConfirm(parsedAmount, mpesaPhone.trim(), rememberNumber)
        },
        enabled = validAmount && validPhone,
        modifier = Modifier.testTag("confirm_commission_mpesa_button"),
      ) {
        Text("Send $previewKes to M-Pesa")
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text("Cancel")
      }
    },
  )
}

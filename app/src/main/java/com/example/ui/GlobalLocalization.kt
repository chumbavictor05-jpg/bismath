package com.example.ui

import java.text.NumberFormat
import java.util.Currency
import java.util.Locale
import java.util.TimeZone
import kotlin.math.roundToLong

enum class WorldCurrency(
  val code: String,
  val symbol: String,
  val displayName: String,
  val ratePerUsd: Double,
  val isZeroDecimal: Boolean = false,
) {
  USD("USD", "$", "US Dollar", 1.0),
  EUR("EUR", "€", "Euro", 0.92),
  GBP("GBP", "£", "British Pound", 0.79),
  KES("KES", "KSh ", "Kenyan Shilling", 129.0, isZeroDecimal = true),
  NGN("NGN", "₦", "Nigerian Naira", 1620.0, isZeroDecimal = true),
  INR("INR", "₹", "Indian Rupee", 84.0, isZeroDecimal = true),
  CAD("CAD", "C$", "Canadian Dollar", 1.39),
  AUD("AUD", "A$", "Australian Dollar", 1.52),
  JPY("JPY", "¥", "Japanese Yen", 154.0, isZeroDecimal = true),
  BRL("BRL", "R$", "Brazilian Real", 5.75),
  ;

  companion object {
    fun fromLocale(locale: Locale = Locale.getDefault()): WorldCurrency {
      val euroCountries = setOf(
        "AT", "BE", "CY", "DE", "EE", "ES", "FI", "FR", "GR", "HR",
        "IE", "IT", "LT", "LU", "LV", "MT", "NL", "PT", "SI", "SK",
      )
      return when (locale.country.uppercase(Locale.US)) {
        "KE" -> KES
        "GB", "UK" -> GBP
        "NG" -> NGN
        "IN" -> INR
        "CA" -> CAD
        "AU" -> AUD
        "JP" -> JPY
        "BR" -> BRL
        in euroCountries -> EUR
        else -> USD
      }
    }
  }
}

data class WorldTimezoneOption(
  val id: String,
  val cityLabel: String,
)

object GlobalLocalization {
  val supportedLanguages: List<String> = listOf(
    "All",
    "English",
    "Spanish",
    "French",
    "German",
    "Mandarin",
    "Arabic",
    "Japanese",
    "Swahili",
    "Portuguese",
    "Hindi",
  )

  val popularTimezones: List<WorldTimezoneOption> = listOf(
    WorldTimezoneOption("America/New_York", "New York (EST/EDT)"),
    WorldTimezoneOption("America/Los_Angeles", "San Francisco (PST/PDT)"),
    WorldTimezoneOption("America/Sao_Paulo", "São Paulo (BRT)"),
    WorldTimezoneOption("Europe/London", "London (GMT/BST)"),
    WorldTimezoneOption("Europe/Paris", "Paris / Zurich (CET)"),
    WorldTimezoneOption("Africa/Nairobi", "Nairobi (EAT)"),
    WorldTimezoneOption("Africa/Lagos", "Lagos (WAT)"),
    WorldTimezoneOption("Asia/Dubai", "Dubai (GST)"),
    WorldTimezoneOption("Asia/Kolkata", "Mumbai / Delhi (IST)"),
    WorldTimezoneOption("Asia/Tokyo", "Tokyo (JST)"),
    WorldTimezoneOption("Australia/Sydney", "Sydney (AEST)"),
    WorldTimezoneOption("UTC", "Universal Time (UTC)"),
  )

  fun formatUsd(amountUsd: Number, currency: WorldCurrency): String {
    val converted = amountUsd.toDouble() * currency.ratePerUsd
    val rounded = converted.roundToLong()
    val numberFmt = NumberFormat.getIntegerInstance(Locale.US)
    return "${currency.symbol}${numberFmt.format(rounded)}"
  }

  fun formatUsdExact(amountUsd: Number, currency: WorldCurrency): String {
    val converted = amountUsd.toDouble() * currency.ratePerUsd
    return if (currency.isZeroDecimal || (currency == WorldCurrency.USD && converted == converted.roundToLong().toDouble())) {
      formatUsd(amountUsd, currency)
    } else {
      val fmt = NumberFormat.getNumberInstance(Locale.US).apply {
        minimumFractionDigits = 2
        maximumFractionDigits = 2
      }
      "${currency.symbol}${fmt.format(converted)}"
    }
  }

  fun formatTimezoneLabel(tzId: String): String {
    val tz = TimeZone.getTimeZone(tzId)
    val rawOffsetHours = tz.rawOffset / 3600000.0
    val wholeHours = rawOffsetHours.toInt()
    val mins = kotlin.math.abs(((rawOffsetHours - wholeHours) * 60).toInt())
    val sign = if (tz.rawOffset >= 0) "+" else "-"
    val absH = kotlin.math.abs(wholeHours)
    val offsetStr = if (mins == 0) {
      String.format(Locale.US, "UTC%s%02d:00", sign, absH)
    } else {
      String.format(Locale.US, "UTC%s%02d:%02d", sign, absH, mins)
    }
    val shortName = tz.getDisplayName(false, TimeZone.SHORT, Locale.US)
    return "$shortName ($offsetStr)"
  }

  fun safeCurrencyCode(currency: WorldCurrency): String {
    return runCatching { Currency.getInstance(currency.code).currencyCode }.getOrDefault(currency.code)
  }
}

package com.example.ui

import com.example.data.catalog.VerifiedExpertsCatalog
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GlobalLocalizationTest {

  @Test
  fun localeMapsToExpectedWorldCurrency() {
    assertEquals(WorldCurrency.KES, WorldCurrency.fromLocale(Locale("en", "KE")))
    assertEquals(WorldCurrency.EUR, WorldCurrency.fromLocale(Locale("fr", "FR")))
    assertEquals(WorldCurrency.EUR, WorldCurrency.fromLocale(Locale("de", "DE")))
    assertEquals(WorldCurrency.GBP, WorldCurrency.fromLocale(Locale("en", "GB")))
    assertEquals(WorldCurrency.INR, WorldCurrency.fromLocale(Locale("hi", "IN")))
    assertEquals(WorldCurrency.JPY, WorldCurrency.fromLocale(Locale("ja", "JP")))
    assertEquals(WorldCurrency.NGN, WorldCurrency.fromLocale(Locale("en", "NG")))
    assertEquals(WorldCurrency.BRL, WorldCurrency.fromLocale(Locale("pt", "BR")))
    assertEquals(WorldCurrency.USD, WorldCurrency.fromLocale(Locale("en", "US")))
  }

  @Test
  fun formatUsdConvertsAndFormatsAcrossCurrencies() {
    assertEquals("$75", GlobalLocalization.formatUsd(75, WorldCurrency.USD))
    assertEquals("$75", GlobalLocalization.formatUsdExact(75.0, WorldCurrency.USD))
    assertEquals("$67.50", GlobalLocalization.formatUsdExact(67.5, WorldCurrency.USD))
    assertEquals("KSh 12,900", GlobalLocalization.formatUsd(100, WorldCurrency.KES))
    assertEquals("€92", GlobalLocalization.formatUsd(100, WorldCurrency.EUR))
    assertEquals("¥15,400", GlobalLocalization.formatUsd(100, WorldCurrency.JPY))
  }

  @Test
  fun formatTimezoneLabelIncludesUtcOffset() {
    val utcLabel = GlobalLocalization.formatTimezoneLabel("UTC")
    assertTrue(utcLabel.contains("UTC+00:00"))
    val nairobiLabel = GlobalLocalization.formatTimezoneLabel("Africa/Nairobi")
    assertTrue(nairobiLabel.contains("UTC+03:00"))
  }

  @Test
  fun catalogIncludesGlobalVerifiedEducatorsAndLanguages() {
    val experts = VerifiedExpertsCatalog.experts
    assertTrue("Expected at least 10 global experts", experts.size >= 10)
    val languages = experts.flatMap { it.languages }.toSet()
    assertTrue(languages.containsAll(listOf("English", "Swahili", "Hindi", "Arabic", "Japanese", "Spanish", "French")))
    assertTrue(experts.any { it.countryLabel.contains("Kenya") })
    assertTrue(experts.any { it.countryLabel.contains("India") })
    assertTrue(experts.any { it.countryLabel.contains("Japan") })
    assertTrue(experts.any { it.countryLabel.contains("Brazil") })
  }
}

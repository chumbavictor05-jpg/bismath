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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.R
import com.example.data.catalog.VerifiedExpertsCatalog
import com.example.data.model.SubjectExpert
import com.example.ui.ExpertSortOption
import com.example.ui.GlobalLocalization
import com.example.ui.RatingSummary
import com.example.ui.WorldCurrency
import com.example.ui.theme.LocalScholarlyColors
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun DiscoverScreen(
  experts: List<SubjectExpert>,
  savedTutorIds: Set<String>,
  ratingSummaries: Map<String, RatingSummary>,
  selectedCurrency: WorldCurrency,
  selectedTimezoneId: String,
  selectedLanguage: String,
  searchQuery: String,
  selectedCategory: String,
  maxHourlyRate: Int,
  sortOption: ExpertSortOption,
  studentFirstName: String,
  upcomingCount: Int,
  onSelectCurrency: (WorldCurrency) -> Unit,
  onSelectTimezone: (String) -> Unit,
  onSelectLanguage: (String) -> Unit,
  onSearchQueryChange: (String) -> Unit,
  onSelectCategory: (String) -> Unit,
  onUpdateMaxRate: (Int) -> Unit,
  onUpdateSort: (ExpertSortOption) -> Unit,
  onToggleBookmark: (SubjectExpert, Boolean) -> Unit,
  onOpenExpert: (String) -> Unit,
  onOpenSessionsTab: () -> Unit,
) {
  var showFilterSheet by remember { mutableStateOf(false) }
  var showRegionSheet by remember { mutableStateOf(false) }
  val tzShort = remember(selectedTimezoneId) { GlobalLocalization.formatTimezoneLabel(selectedTimezoneId) }

  LazyColumn(
    modifier = Modifier
      .fillMaxSize()
      .background(MaterialTheme.colorScheme.background),
    contentPadding = PaddingValues(bottom = 24.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp),
  ) {
    // Editorial Header + Greeting + Search Bar
    item {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 20.dp)
          .padding(top = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = "Welcome back, $studentFirstName",
              style = MaterialTheme.typography.labelMedium,
              color = MaterialTheme.colorScheme.secondary,
              fontWeight = FontWeight.SemiBold,
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
              text = "Verified Subject Experts",
              style = MaterialTheme.typography.headlineLarge,
              color = MaterialTheme.colorScheme.onBackground,
            )
          }
          Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(
              shape = CircleShape,
              color = MaterialTheme.colorScheme.surfaceVariant,
              modifier = Modifier
                .minimumInteractiveComponentSize()
                .testTag("global_region_button")
                .clickable { showRegionSheet = true },
            ) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
              ) {
                Icon(
                  imageVector = Icons.Filled.Public,
                  contentDescription = "Select currency and timezone",
                  tint = MaterialTheme.colorScheme.primary,
                  modifier = Modifier.size(16.dp),
                )
                Text(
                  text = selectedCurrency.code,
                  style = MaterialTheme.typography.labelMedium,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
              }
            }
            if (upcomingCount > 0) {
              Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier
                  .minimumInteractiveComponentSize()
                  .clickable(onClick = onOpenSessionsTab),
              ) {
                Text(
                  text = "$upcomingCount upcoming",
                  style = MaterialTheme.typography.labelMedium,
                  color = MaterialTheme.colorScheme.onPrimaryContainer,
                  modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                )
              }
            }
          }
        }

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(10.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            placeholder = {
              Text(
                text = "Search calculus, Swahili, Nairobi, MIT...",
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
              )
            },
            leadingIcon = {
              Icon(
                imageVector = Icons.Filled.Search,
                contentDescription = stringResource(R.string.cd_search),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            },
            trailingIcon = {
              if (searchQuery.isNotEmpty()) {
                IconButton(onClick = { onSearchQueryChange("") }) {
                  Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = stringResource(R.string.cd_clear_search),
                  )
                }
              }
            },
            singleLine = true,
            shape = MaterialTheme.shapes.medium,
            colors = OutlinedTextFieldDefaults.colors(
              focusedContainerColor = MaterialTheme.colorScheme.surface,
              unfocusedContainerColor = MaterialTheme.colorScheme.surface,
              focusedBorderColor = MaterialTheme.colorScheme.primary,
              unfocusedBorderColor = MaterialTheme.colorScheme.outline,
            ),
            modifier = Modifier
              .weight(1f)
              .testTag("expert_search_input"),
          )

          Surface(
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            modifier = Modifier
              .size(54.dp)
              .testTag("filter_button")
              .clickable { showFilterSheet = true },
          ) {
            Box(contentAlignment = Alignment.Center) {
              Icon(
                imageVector = Icons.Filled.Tune,
                contentDescription = "Filter and sort experts",
                tint = MaterialTheme.colorScheme.primary,
              )
            }
          }
        }
      }
    }

    // Editorial Spotlight Banner
    item {
      SpotlightBanner(
        modifier = Modifier.padding(horizontal = 20.dp),
        onExploreFeatured = {
          onSelectCategory("All")
          onUpdateSort(ExpertSortOption.TOP_RATED)
        },
      )
    }

    // Horizontal Subject Category Chips
    item {
      LazyRow(
        contentPadding = PaddingValues(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        items(VerifiedExpertsCatalog.categories, key = { it }) { category ->
          val selected = category == selectedCategory
          FilterChip(
            selected = selected,
            onClick = { onSelectCategory(category) },
            label = {
              Text(
                text = category,
                style = MaterialTheme.typography.labelLarge,
              )
            },
            colors = FilterChipDefaults.filterChipColors(
              selectedContainerColor = MaterialTheme.colorScheme.primary,
              selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
              containerColor = MaterialTheme.colorScheme.surface,
              labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
            ),
            border = FilterChipDefaults.filterChipBorder(
              enabled = true,
              selected = selected,
              borderColor = MaterialTheme.colorScheme.outline,
            ),
            modifier = Modifier.testTag("category_chip_$category"),
          )
        }
      }
    }

    // Results Header
    item {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(
          text = "${experts.size} Global Experts Available",
          style = MaterialTheme.typography.titleMedium,
          color = MaterialTheme.colorScheme.onBackground,
        )
        Text(
          text = "Up to ${GlobalLocalization.formatUsd(maxHourlyRate, selectedCurrency)}/hr",
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
    }

    if (experts.isEmpty()) {
      item {
        Surface(
          shape = MaterialTheme.shapes.large,
          color = MaterialTheme.colorScheme.surface,
          border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        ) {
          Column(
            modifier = Modifier.padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            Icon(
              imageVector = Icons.Filled.Search,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.primary,
              modifier = Modifier.size(36.dp),
            )
            Text(
              text = "No matching experts found",
              style = MaterialTheme.typography.titleMedium,
            )
            Text(
              text = "Try widening your hourly rate ceiling or clearing the subject filter.",
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            TextButton(
              onClick = {
                onSearchQueryChange("")
                onSelectCategory("All")
                onUpdateMaxRate(100)
              },
            ) {
              Text("Reset all filters")
            }
          }
        }
      }
    } else {
      items(experts, key = { it.id }) { expert ->
        val isSaved = savedTutorIds.contains(expert.id)
        ExpertCard(
          expert = expert,
          isSaved = isSaved,
          ratingSummary = ratingSummaries[expert.id],
          selectedCurrency = selectedCurrency,
          onToggleBookmark = { onToggleBookmark(expert, isSaved) },
          onClick = { onOpenExpert(expert.id) },
          modifier = Modifier.padding(horizontal = 20.dp),
        )
      }
    }
  }

  if (showFilterSheet) {
    ModalBottomSheet(onDismissRequest = { showFilterSheet = false }) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .verticalScroll(rememberScrollState())
          .padding(horizontal = 24.dp)
          .padding(bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
      ) {
        Text(
          text = "Filter & Sort Global Experts",
          style = MaterialTheme.typography.headlineSmall,
        )

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
          ) {
            Text("Max Hourly Rate", style = MaterialTheme.typography.titleMedium)
            Text(
              text = "${GlobalLocalization.formatUsd(maxHourlyRate, selectedCurrency)}/hr",
              style = MaterialTheme.typography.labelLarge,
              color = MaterialTheme.colorScheme.primary,
            )
          }
          Slider(
            value = maxHourlyRate.toFloat(),
            onValueChange = { onUpdateMaxRate(it.toInt()) },
            valueRange = 45f..100f,
            steps = 10,
          )
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Text("Tutor Language", style = MaterialTheme.typography.titleMedium)
          FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
          ) {
            GlobalLocalization.supportedLanguages.forEach { lang ->
              FilterChip(
                selected = lang == selectedLanguage,
                onClick = { onSelectLanguage(lang) },
                label = { Text(lang) },
              )
            }
          }
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Text("Sort By", style = MaterialTheme.typography.titleMedium)
          ExpertSortOption.entries.forEach { option ->
            FilterChip(
              selected = option == sortOption,
              onClick = { onUpdateSort(option) },
              label = { Text(option.label) },
              modifier = Modifier.fillMaxWidth(),
            )
          }
        }

        Button(
          onClick = { showFilterSheet = false },
          modifier = Modifier.fillMaxWidth().height(50.dp),
        ) {
          Text("Apply Filters")
        }
      }
    }
  }

  if (showRegionSheet) {
    ModalBottomSheet(onDismissRequest = { showRegionSheet = false }) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .verticalScroll(rememberScrollState())
          .padding(horizontal = 24.dp)
          .padding(bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
      ) {
        Text("Worldwide Currency & Timezone", style = MaterialTheme.typography.headlineSmall)
        Text(
          text = "Active timezone: $tzShort",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.primary,
        )

        Text("Display Currency", style = MaterialTheme.typography.titleMedium)
        FlowRow(
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          WorldCurrency.entries.forEach { currency ->
            FilterChip(
              selected = currency == selectedCurrency,
              onClick = { onSelectCurrency(currency) },
              label = { Text("${currency.code} (${currency.symbol.trim()})") },
              modifier = Modifier.testTag("currency_chip_${currency.code}"),
            )
          }
        }

        Text("Local Timezone", style = MaterialTheme.typography.titleMedium)
        FlowRow(
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          GlobalLocalization.popularTimezones.forEach { tz ->
            FilterChip(
              selected = tz.id == selectedTimezoneId,
              onClick = { onSelectTimezone(tz.id) },
              label = { Text(tz.cityLabel) },
            )
          }
        }

        Button(
          onClick = { showRegionSheet = false },
          modifier = Modifier.fillMaxWidth().height(48.dp),
        ) {
          Text("Done")
        }
      }
    }
  }
}

@Composable
private fun SpotlightBanner(
  modifier: Modifier = Modifier,
  onExploreFeatured: () -> Unit,
) {
  val scholarlyColors = LocalScholarlyColors.current
  Surface(
    shape = MaterialTheme.shapes.large,
    modifier = modifier
      .fillMaxWidth()
      .clickable(onClick = onExploreFeatured),
  ) {
    Box(
      modifier = Modifier
        .background(
          Brush.horizontalGradient(
            colors = listOf(scholarlyColors.heroGradientStart, scholarlyColors.heroGradientEnd)
          )
        )
        .padding(18.dp),
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
      ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
          Surface(
            shape = CircleShape,
            color = Color.White.copy(alpha = 0.16f),
          ) {
            Text(
              text = "EXAM SEASON COACHING",
              style = MaterialTheme.typography.labelSmall,
              color = Color.White,
              modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            )
          }
          Text(
            text = "1-on-1 Mastery with Credentialed PhDs",
            style = MaterialTheme.typography.titleLarge,
            color = Color.White,
          )
          Text(
            text = "Live whiteboard, syllabus prep, and verified reviews.",
            style = MaterialTheme.typography.bodySmall,
            color = Color.White.copy(alpha = 0.85f),
          )
        }
        AsyncImage(
          model = ImageRequest.Builder(LocalContext.current)
            .data(R.drawable.img_hero_academy)
            .crossfade(true)
            .build(),
          contentDescription = null,
          contentScale = ContentScale.Crop,
          modifier = Modifier
            .size(84.dp)
            .clip(MaterialTheme.shapes.medium),
        )
      }
    }
  }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ExpertCard(
  expert: SubjectExpert,
  isSaved: Boolean,
  ratingSummary: RatingSummary? = null,
  selectedCurrency: WorldCurrency = WorldCurrency.USD,
  onToggleBookmark: () -> Unit,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val scholarlyColors = LocalScholarlyColors.current
  val displayRating = ratingSummary?.averageRating ?: expert.rating
  val displayReviewCount = ratingSummary?.totalReviewCount ?: expert.reviewCount
  Surface(
    shape = MaterialTheme.shapes.large,
    color = MaterialTheme.colorScheme.surface,
    tonalElevation = 1.dp,
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    modifier = modifier
      .fillMaxWidth()
      .testTag("expert_card_${expert.id}")
      .clickable(onClick = onClick),
  ) {
    Column(
      modifier = Modifier.padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.Top,
      ) {
        Box {
          AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
              .data(expert.avatarRes)
              .crossfade(true)
              .build(),
            contentDescription = "${expert.name} portrait",
            contentScale = ContentScale.Crop,
            modifier = Modifier
              .size(68.dp)
              .clip(RoundedCornerShape(18.dp)),
          )
          Surface(
            shape = CircleShape,
            color = scholarlyColors.verifiedBadgeBg,
            border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.surface),
            modifier = Modifier
              .align(Alignment.BottomEnd)
              .size(24.dp),
          ) {
            Box(contentAlignment = Alignment.Center) {
              Icon(
                imageVector = Icons.Filled.Verified,
                contentDescription = stringResource(R.string.cd_verified_badge),
                tint = scholarlyColors.verifiedBadgeFg,
                modifier = Modifier.size(15.dp),
              )
            }
          }
        }

        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
          Text(
            text = expert.name,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
          Text(
            text = "${expert.title} · ${expert.institution}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
          Text(
            text = "${expert.countryLabel} · ${expert.languages.take(2).joinToString(", ")}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
          ) {
            Icon(
              imageVector = Icons.Filled.Star,
              contentDescription = null,
              tint = scholarlyColors.ratingStar,
              modifier = Modifier.size(16.dp),
            )
            Text(
              text = String.format(Locale.US, "%.2f", displayRating),
              style = MaterialTheme.typography.labelLarge,
              color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
              text = "($displayReviewCount) · ${expert.sessionsCompleted} sessions",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        }

        IconButton(
          onClick = onToggleBookmark,
          modifier = Modifier
            .minimumInteractiveComponentSize()
            .testTag("bookmark_button_${expert.id}"),
        ) {
          Icon(
            imageVector = if (isSaved) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
            contentDescription = stringResource(
              if (isSaved) R.string.cd_bookmark_remove else R.string.cd_bookmark_add
            ),
            tint = if (isSaved) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }

      Text(
        text = expert.bio,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
      )

      FlowRow(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
      ) {
        expert.subjects.take(3).forEach { subject ->
          Surface(
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
          ) {
            Text(
              text = subject,
              style = MaterialTheme.typography.labelMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            )
          }
        }
      }

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
          Icon(
            imageVector = Icons.Outlined.Schedule,
            contentDescription = null,
            tint = scholarlyColors.verifiedBadgeFg,
            modifier = Modifier.size(15.dp),
          )
          Text(
            text = expert.responseTimeLabel,
            style = MaterialTheme.typography.labelMedium,
            color = scholarlyColors.verifiedBadgeFg,
          )
        }

        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          Text(
            text = "${GlobalLocalization.formatUsd(expert.hourlyRateUsd, selectedCurrency)}/hr",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
          )
          Surface(
            shape = RoundedCornerShape(10.dp),
            color = MaterialTheme.colorScheme.primary,
          ) {
            Text(
              text = "View & Book",
              style = MaterialTheme.typography.labelLarge,
              color = MaterialTheme.colorScheme.onPrimary,
              modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            )
          }
        }
      }
    }
  }
}

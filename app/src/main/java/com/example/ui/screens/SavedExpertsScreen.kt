package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.catalog.VerifiedExpertsCatalog
import com.example.data.model.SavedTutor
import com.example.data.model.SubjectExpert
import com.example.ui.RatingSummary
import com.example.ui.UiState
import com.example.ui.WorldCurrency

@Composable
fun SavedExpertsScreen(
  savedTutorsState: UiState<List<SavedTutor>>,
  ratingSummaries: Map<String, RatingSummary>,
  selectedCurrency: WorldCurrency,
  onToggleBookmark: (SubjectExpert, Boolean) -> Unit,
  onOpenExpert: (String) -> Unit,
  onExploreExperts: () -> Unit,
) {
  Column(
    modifier = Modifier
      .fillMaxSize()
      .background(MaterialTheme.colorScheme.background),
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 20.dp)
        .padding(top = 16.dp, bottom = 12.dp),
    ) {
      Text(
        text = "Your Academic Shortlist",
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.secondary,
        fontWeight = FontWeight.SemiBold,
      )
      Spacer(modifier = Modifier.height(2.dp))
      Text(
        text = "Saved Experts",
        style = MaterialTheme.typography.headlineLarge,
      )
    }

    when (savedTutorsState) {
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
            text = savedTutorsState.message,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.error,
          )
        }
      }
      is UiState.Success -> {
        val experts = savedTutorsState.data.mapNotNull { saved ->
          VerifiedExpertsCatalog.findById(saved.tutorId)
        }
        if (experts.isEmpty()) {
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
                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f),
                modifier = Modifier.size(72.dp),
              ) {
                Box(contentAlignment = Alignment.Center) {
                  Icon(
                    imageVector = Icons.Outlined.BookmarkBorder,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(34.dp),
                  )
                }
              }
              Text(
                text = stringResource(R.string.empty_saved_title),
                style = MaterialTheme.typography.titleLarge,
                textAlign = TextAlign.Center,
              )
              Text(
                text = stringResource(R.string.empty_saved_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
              )
              Spacer(modifier = Modifier.height(4.dp))
              Button(onClick = onExploreExperts) {
                Text("Discover Subject Experts")
              }
            }
          }
        } else {
          LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
          ) {
            items(experts, key = { it.id }) { expert ->
              ExpertCard(
                expert = expert,
                isSaved = true,
                ratingSummary = ratingSummaries[expert.id],
                selectedCurrency = selectedCurrency,
                onToggleBookmark = { onToggleBookmark(expert, true) },
                onClick = { onOpenExpert(expert.id) },
              )
            }
          }
        }
      }
    }
  }
}

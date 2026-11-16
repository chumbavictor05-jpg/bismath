package com.example

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.lifecycleScope
import com.example.notifications.BookingNotificationHelper
import com.example.ui.AppNavigation
import com.example.ui.theme.ScholarlyTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
  private var deepLinkBookingId by mutableStateOf<String?>(null)

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    deepLinkBookingId = intent?.getStringExtra(BookingNotificationHelper.EXTRA_BOOKING_ID)
    lifecycleScope.launch(Dispatchers.IO) {
      BookingNotificationHelper.ensureAllChannels(applicationContext)
    }
    setContent {
      ScholarlyTheme {
        AppNavigation(
          deepLinkBookingId = deepLinkBookingId,
          onDeepLinkHandled = { deepLinkBookingId = null },
        )
      }
    }
  }

  override fun onNewIntent(intent: Intent) {
    super.onNewIntent(intent)
    setIntent(intent)
    intent.getStringExtra(BookingNotificationHelper.EXTRA_BOOKING_ID)?.let { bookingId ->
      deepLinkBookingId = bookingId
    }
  }
}

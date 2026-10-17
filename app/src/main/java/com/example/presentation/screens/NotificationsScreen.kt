package com.example.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.NotificationItem
import com.example.presentation.MainViewModel
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsScreen(
  viewModel: MainViewModel,
  onNavigateBack: () -> Unit,
  modifier: Modifier = Modifier
) {
  val currentUser by viewModel.currentUser.collectAsState()
  val notifications by viewModel.notifications.collectAsState()

  val userNotifications = notifications.filter { it.userId == currentUser?.id }

  Scaffold(
    topBar = {
      TopAppBar(
        title = { Text("Notifications", fontWeight = FontWeight.Bold, color = ForestGreenPrimary) },
        navigationIcon = {
          IconButton(onClick = onNavigateBack) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = ForestGreenPrimary)
          }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = WarmWhite)
      )
    },
    containerColor = WarmWhite,
    modifier = modifier.fillMaxSize()
  ) { paddingValues ->
    if (userNotifications.isEmpty()) {
      Box(
        modifier = Modifier
          .fillMaxSize()
          .padding(paddingValues),
        contentAlignment = Alignment.Center
      ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
          Icon(Icons.Default.NotificationsNone, contentDescription = null, tint = SageMuted, modifier = Modifier.size(48.dp))
          Spacer(modifier = Modifier.height(12.dp))
          Text("No notifications yet", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
        }
      }
    } else {
      LazyColumn(
        modifier = Modifier
          .fillMaxSize()
          .padding(paddingValues)
          .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        items(userNotifications) { item ->
          NotificationCard(item = item)
        }
      }
    }
  }
}

@Composable
fun NotificationCard(item: NotificationItem) {
  val dateStr = remember(item.createdAt) {
    SimpleDateFormat("dd MMM, HH:mm", Locale.US).format(Date(item.createdAt))
  }

  val icon = when (item.type) {
    "TRANSFER" -> Icons.Default.Send
    "LOAN" -> Icons.Default.AccountBalance
    "EXCHANGE" -> Icons.Default.CurrencyExchange
    else -> Icons.Default.Info
  }

  Card(
    modifier = Modifier.fillMaxWidth(),
    shape = RoundedCornerShape(16.dp),
    colors = CardDefaults.cardColors(containerColor = PureWhite),
    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(16.dp),
      verticalAlignment = Alignment.Top
    ) {
      Box(
        modifier = Modifier
          .size(38.dp)
          .clip(CircleShape)
          .background(PositiveGreenBg),
        contentAlignment = Alignment.Center
      ) {
        Icon(icon, contentDescription = null, tint = PositiveGreen, modifier = Modifier.size(18.dp))
      }
      Spacer(modifier = Modifier.width(12.dp))
      Column(modifier = Modifier.weight(1f)) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          Text(item.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = TextPrimary)
          Text(dateStr, style = MaterialTheme.typography.bodySmall, color = TextTertiary)
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(item.message, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
      }
    }
  }
}

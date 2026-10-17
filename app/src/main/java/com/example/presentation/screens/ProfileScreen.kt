package com.example.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.config.SupabaseConfig
import com.example.presentation.MainViewModel
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
  viewModel: MainViewModel,
  onNavigateToAdmin: () -> Unit,
  onNavigateToAuth: () -> Unit,
  modifier: Modifier = Modifier
) {
  val currentUser by viewModel.currentUser.collectAsState()
  val communityProfiles by viewModel.communityProfiles.collectAsState()
  val isSupabaseConnected by viewModel.repository.isSupabaseConnected.collectAsState()

  var showSwitchUserDialog by remember { mutableStateOf(false) }
  var showSupabaseDialog by remember { mutableStateOf(false) }

  Scaffold(
    topBar = {
      TopAppBar(
        title = {
          Text(
            "Account & Settings",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = ForestGreenPrimary
          )
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = WarmWhite)
      )
    },
    containerColor = WarmWhite,
    modifier = modifier.fillMaxSize()
  ) { paddingValues ->
    LazyColumn(
      modifier = Modifier
        .fillMaxSize()
        .padding(paddingValues)
        .padding(horizontal = 16.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
      // User Profile Header Card
      item {
        Card(
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(22.dp),
          colors = CardDefaults.cardColors(containerColor = PureWhite),
          elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Box(
              modifier = Modifier
                .size(60.dp)
                .clip(CircleShape)
                .background(ForestGreenPrimary),
              contentAlignment = Alignment.Center
            ) {
              Text(
                text = (currentUser?.fullName?.take(1) ?: "B").uppercase(),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MintLight
              )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                  text = currentUser?.fullName ?: "Guest",
                  style = MaterialTheme.typography.titleLarge,
                  fontWeight = FontWeight.Bold,
                  color = TextPrimary
                )
                if (currentUser?.role == "admin") {
                  Spacer(modifier = Modifier.width(8.dp))
                  Surface(
                    color = ForestGreenPrimary,
                    shape = RoundedCornerShape(6.dp)
                  ) {
                    Text(
                      text = "ADMIN",
                      color = MintLight,
                      style = MaterialTheme.typography.labelSmall,
                      fontWeight = FontWeight.Bold,
                      modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                  }
                }
              }

              Text(
                text = "@${currentUser?.username ?: "username"}",
                style = MaterialTheme.typography.bodyMedium,
                color = MintSecondary,
                fontWeight = FontWeight.SemiBold
              )
              Text(
                text = currentUser?.email ?: "",
                style = MaterialTheme.typography.bodySmall,
                color = TextTertiary
              )
            }
          }
        }
      }

      // Quick Testing: Switch Community Member
      item {
        Card(
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(18.dp),
          colors = CardDefaults.cardColors(containerColor = SageSurface)
        ) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .clickable { showSwitchUserDialog = true }
              .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(Icons.Default.People, contentDescription = null, tint = ForestGreenPrimary)
              Spacer(modifier = Modifier.width(12.dp))
              Column {
                Text("Switch Community Account", fontWeight = FontWeight.Bold, color = ForestGreenPrimary)
                Text("Test transfers between @maya, @alex, and admin", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
              }
            }
            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = ForestGreenPrimary)
          }
        }
      }

      // Central Bank Admin Portal (Always available or when admin)
      item {
        Card(
          modifier = Modifier
            .fillMaxWidth()
            .testTag("profile_admin_portal_button"),
          shape = RoundedCornerShape(18.dp),
          colors = CardDefaults.cardColors(containerColor = PureWhite),
          elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .clickable(onClick = onNavigateToAdmin)
              .padding(18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Box(
                modifier = Modifier
                  .size(40.dp)
                  .clip(CircleShape)
                  .background(ForestGreenPrimary),
                contentAlignment = Alignment.Center
              ) {
                Icon(Icons.Default.AdminPanelSettings, contentDescription = null, tint = MintLight, modifier = Modifier.size(22.dp))
              }
              Spacer(modifier = Modifier.width(14.dp))
              Column {
                Text("Central Bank Inspector", fontWeight = FontWeight.Bold, color = TextPrimary)
                Text("Total supply audit, reserve verification, market control", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
              }
            }
            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = ForestGreenPrimary)
          }
        }
      }

      // Supabase Backend Integration Info
      item {
        Card(
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(18.dp),
          colors = CardDefaults.cardColors(containerColor = PureWhite),
          elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .clickable { showSupabaseDialog = true }
              .padding(18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Box(
                modifier = Modifier
                  .size(40.dp)
                  .clip(CircleShape)
                  .background(PositiveGreenBg),
                contentAlignment = Alignment.Center
              ) {
                Icon(Icons.Default.CloudSync, contentDescription = null, tint = PositiveGreen, modifier = Modifier.size(22.dp))
              }
              Spacer(modifier = Modifier.width(14.dp))
              Column {
                Text("Supabase Backend Status", fontWeight = FontWeight.Bold, color = TextPrimary)
                Text(
                  text = "Configured: https://ohonmeefuioicdbbsvji.supabase.co",
                  style = MaterialTheme.typography.bodySmall,
                  color = TextSecondary,
                  maxLines = 1
                )
              }
            }
            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = ForestGreenPrimary)
          }
        }
      }

      // Legal & Disclaimer
      item {
        Card(
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(18.dp),
          colors = CardDefaults.cardColors(containerColor = PureWhite)
        ) {
          Column(modifier = Modifier.padding(18.dp)) {
            Text("Simulator Disclosures", fontWeight = FontWeight.Bold, color = TextPrimary)
            Spacer(modifier = Modifier.height(6.dp))
            Text(
              text = "BearWallet is a closed-loop economy simulator designed exclusively for recreation among friends and a partner. Bear currency (B) and displayed Rupiah amounts are simulated reference figures without monetary value and cannot be redeemed for real currency.",
              style = MaterialTheme.typography.bodySmall,
              color = TextSecondary,
              lineHeight = 18.sp
            )
          }
        }
      }

      // Sign Out Button
      item {
        OutlinedButton(
          onClick = {
            viewModel.signOut()
            onNavigateToAuth()
          },
          modifier = Modifier
            .fillMaxWidth()
            .height(50.dp)
            .testTag("profile_sign_out_button"),
          shape = RoundedCornerShape(14.dp),
          colors = ButtonDefaults.outlinedButtonColors(contentColor = NegativeRed)
        ) {
          Icon(Icons.Default.Logout, contentDescription = null, modifier = Modifier.size(18.dp))
          Spacer(modifier = Modifier.width(8.dp))
          Text("Sign Out", fontWeight = FontWeight.Bold)
        }
      }

      item { Spacer(modifier = Modifier.height(24.dp)) }
    }
  }

  // Switch User Dialog
  if (showSwitchUserDialog) {
    AlertDialog(
      onDismissRequest = { showSwitchUserDialog = false },
      title = { Text("Switch Community Account", fontWeight = FontWeight.Bold, color = ForestGreenPrimary) },
      text = {
        Column {
          Text(
            "Select an account to simulate transactions, transfers, or central bank administration:",
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondary
          )
          Spacer(modifier = Modifier.height(14.dp))
          communityProfiles.values.forEach { user ->
            Surface(
              modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
                .clickable {
                  viewModel.switchUser(user.id)
                  showSwitchUserDialog = false
                },
              shape = RoundedCornerShape(12.dp),
              color = if (currentUser?.id == user.id) SageSurface else SurfaceVariantColor
            ) {
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
              ) {
                Column {
                  Text(user.fullName, fontWeight = FontWeight.Bold, color = TextPrimary)
                  Text("@${user.username} • Role: ${user.role}", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                }
                if (currentUser?.id == user.id) {
                  Icon(Icons.Default.Check, contentDescription = null, tint = ForestGreenPrimary)
                }
              }
            }
          }
        }
      },
      confirmButton = {
        TextButton(onClick = { showSwitchUserDialog = false }) { Text("Close") }
      }
    )
  }

  // Supabase Info Dialog
  if (showSupabaseDialog) {
    AlertDialog(
      onDismissRequest = { showSupabaseDialog = false },
      title = { Text("Supabase Configuration", fontWeight = FontWeight.Bold, color = ForestGreenPrimary) },
      text = {
        Column {
          Text("Target Project:", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
          Text(SupabaseConfig.SUPABASE_URL, style = MaterialTheme.typography.bodySmall, color = MintSecondary)
          Spacer(modifier = Modifier.height(10.dp))
          Text("Publishable Key Configured:", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
          Text("${SupabaseConfig.SUPABASE_ANON_KEY.take(15)}...", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
          Spacer(modifier = Modifier.height(12.dp))
          Text("SQL Migrations:", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
          Text(
            "The complete SQL migration is prepared in 'supabase/migrations/20261017_init_bearwallet.sql'. It contains all tables, double-entry triggers, RLS, and secure RPCs. Run it in your Supabase SQL Editor.",
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondary
          )
        }
      },
      confirmButton = {
        TextButton(onClick = { showSupabaseDialog = false }) { Text("Got it") }
      }
    )
  }
}

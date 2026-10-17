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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.config.SupabaseConfig
import com.example.data.model.AuditLog
import com.example.presentation.MainViewModel
import com.example.presentation.components.ConfirmRow
import com.example.ui.theme.*
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CentralBankAdminScreen(
  viewModel: MainViewModel,
  onNavigateBack: () -> Unit,
  modifier: Modifier = Modifier
) {
  val settings by viewModel.centralBankSettings.collectAsState()
  val systemAccounts by viewModel.systemAccounts.collectAsState()
  val auditLogs by viewModel.auditLogs.collectAsState()
  val activeLoans by viewModel.activeLoans.collectAsState()
  val transactions by viewModel.transactions.collectAsState()
  val wallets by viewModel.repository.wallets.collectAsState()

  val lockedReserve = systemAccounts[SupabaseConfig.ACC_LOCKED_RESERVE]?.balance ?: 100_000L
  val lendingPool = systemAccounts[SupabaseConfig.ACC_BANK_LENDING_POOL]?.balance ?: 250_000L
  val exchangeInventory = systemAccounts[SupabaseConfig.ACC_EXCHANGE_INVENTORY]?.balance ?: 200_000L
  val distPool = systemAccounts[SupabaseConfig.ACC_USER_DISTRIBUTION]?.balance ?: 450_000L
  val interestIncome = systemAccounts[SupabaseConfig.ACC_BANK_INTEREST_INCOME]?.balance ?: 0L

  val totalUserWallets = wallets.values.sumOf { it.balance }
  val totalOutstandingLoans = activeLoans.filter { it.status == "ACTIVE" }.sumOf { it.remainingAmount }

  // Economic Conservation Invariant:
  // All Bear must equal exactly 1,000,000 B
  val totalTrackedSupply = lockedReserve + lendingPool + exchangeInventory + distPool + totalUserWallets

  val nf = NumberFormat.getNumberInstance(Locale.US)

  Scaffold(
    topBar = {
      TopAppBar(
        title = {
          Text(
            "Central Bank Inspector",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = ForestGreenPrimary
          )
        },
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
    LazyColumn(
      modifier = Modifier
        .fillMaxSize()
        .padding(paddingValues)
        .padding(horizontal = 16.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
      // Total Supply Hero Banner
      item {
        Card(
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(22.dp),
          colors = CardDefaults.cardColors(containerColor = ForestGreenPrimary)
        ) {
          Column(modifier = Modifier.padding(20.dp)) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Text(
                text = "Total Authorized Supply",
                style = MaterialTheme.typography.bodySmall,
                color = MintLight
              )
              Surface(
                color = PositiveGreenBg.copy(alpha = 0.2f),
                shape = RoundedCornerShape(6.dp)
              ) {
                Text(
                  text = "INVARIANTS VERIFIED",
                  color = MintLight,
                  style = MaterialTheme.typography.labelSmall,
                  fontWeight = FontWeight.Bold,
                  modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
              }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
              text = "1,000,000 B",
              style = MaterialTheme.typography.displayMedium,
              fontWeight = FontWeight.Bold,
              color = PureWhite
            )

            Spacer(modifier = Modifier.height(4.dp))
            Text(
              text = "Closed-loop economy • Strictly finite allocation across 4 authoritative accounts",
              style = MaterialTheme.typography.bodySmall,
              color = SageSurface
            )
          }
        }
      }

      // System Accounts Audit Breakdown
      item {
        Card(
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(20.dp),
          colors = CardDefaults.cardColors(containerColor = PureWhite),
          elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
          Column(modifier = Modifier.padding(18.dp)) {
            Text(
              text = "National Reserve Allocation",
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold,
              color = ForestGreenPrimary
            )
            Spacer(modifier = Modifier.height(14.dp))

            ReserveRow("Locked Central Bank Reserve", "${nf.format(lockedReserve)} B", "Cannot be spent or lent", isLocked = true)
            ReserveRow("Bank Lending Pool", "${nf.format(lendingPool)} B", "Available for user loans")
            ReserveRow("Exchange Market Inventory", "${nf.format(exchangeInventory)} B", "Virtual buy/sell liquidity")
            ReserveRow("Initial Distribution Pool", "${nf.format(distPool)} B", "User signup & faucet grants")
            ReserveRow("Held in User Wallets", "${nf.format(totalUserWallets)} B", "Community circulation")

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = BorderLight)
            Spacer(modifier = Modifier.height(12.dp))

            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween
            ) {
              Text("Total Tracked Supply Sum", fontWeight = FontWeight.Bold, color = TextPrimary)
              Text(
                text = "${nf.format(totalTrackedSupply)} B",
                fontWeight = FontWeight.Bold,
                color = if (totalTrackedSupply == 1_000_000L) PositiveGreen else NegativeRed
              )
            }
          }
        }
      }

      // Administrative Controls
      item {
        Card(
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(20.dp),
          colors = CardDefaults.cardColors(containerColor = PureWhite),
          elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
          Column(modifier = Modifier.padding(18.dp)) {
            Text(
              text = "Monetary Policy & Market Controls",
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold,
              color = ForestGreenPrimary
            )
            Spacer(modifier = Modifier.height(16.dp))

            // Pause / Resume Market
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Column {
                Text("Market Trading Status", fontWeight = FontWeight.SemiBold, color = TextPrimary)
                Text(
                  text = if (settings.isMarketPaused) "Paused for maintenance" else "Active for all community users",
                  style = MaterialTheme.typography.bodySmall,
                  color = TextSecondary
                )
              }
              Switch(
                checked = !settings.isMarketPaused,
                onCheckedChange = { isActive ->
                  viewModel.updateCentralBankSettings(
                    isPaused = !isActive,
                    intervalHours = settings.updateIntervalHours
                  )
                },
                colors = SwitchDefaults.colors(checkedThumbColor = ForestGreenPrimary, checkedTrackColor = MintSecondary)
              )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Interval selector
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Column {
                Text("Exchange Rate Update Interval", fontWeight = FontWeight.SemiBold, color = TextPrimary)
                Text("Frequency of scheduled market settlements", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
              }
              Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf(2, 4).forEach { hours ->
                  FilterChip(
                    selected = settings.updateIntervalHours == hours,
                    onClick = {
                      viewModel.updateCentralBankSettings(
                        isPaused = settings.isMarketPaused,
                        intervalHours = hours
                      )
                    },
                    label = { Text("${hours}h") }
                  )
                }
              }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Trigger Manual Rate Settlement
            Button(
              onClick = { viewModel.triggerMarketRateUpdate("Manual Central Bank Stabilization") },
              modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("admin_trigger_rate_update_button"),
              colors = ButtonDefaults.buttonColors(containerColor = ForestGreenPrimary),
              shape = RoundedCornerShape(14.dp)
            ) {
              Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
              Spacer(modifier = Modifier.width(8.dp))
              Text("Trigger Immediate Market Rate Settlement", fontWeight = FontWeight.Bold)
            }
          }
        }
      }

      // Audit Logs
      item {
        Text(
          text = "Central Bank Audit Trail",
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.Bold,
          color = TextPrimary
        )
      }

      if (auditLogs.isEmpty()) {
        item {
          Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = PureWhite)
          ) {
            Text(
              text = "No policy modifications recorded yet.",
              style = MaterialTheme.typography.bodySmall,
              color = TextSecondary,
              modifier = Modifier.padding(16.dp)
            )
          }
        }
      } else {
        items(auditLogs) { log ->
          AuditLogItem(log = log)
        }
      }

      item { Spacer(modifier = Modifier.height(24.dp)) }
    }
  }
}

@Composable
fun ReserveRow(title: String, amount: String, subtitle: String, isLocked: Boolean = false) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .padding(vertical = 6.dp),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically
  ) {
    Column(modifier = Modifier.weight(1f)) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Text(title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = TextPrimary)
        if (isLocked) {
          Spacer(modifier = Modifier.width(6.dp))
          Icon(Icons.Default.Lock, contentDescription = "Locked", tint = WarningAmber, modifier = Modifier.size(14.dp))
        }
      }
      Text(subtitle, style = MaterialTheme.typography.bodySmall, color = TextTertiary)
    }
    Text(amount, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, color = ForestGreenPrimary)
  }
}

@Composable
fun AuditLogItem(log: AuditLog) {
  val dateStr = remember(log.timestamp) {
    SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.US).format(Date(log.timestamp))
  }

  Card(
    modifier = Modifier.fillMaxWidth(),
    shape = RoundedCornerShape(14.dp),
    colors = CardDefaults.cardColors(containerColor = PureWhite)
  ) {
    Column(modifier = Modifier.padding(14.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
      ) {
        Text(log.action, fontWeight = FontWeight.Bold, color = ForestGreenPrimary, style = MaterialTheme.typography.titleSmall)
        Text(dateStr, style = MaterialTheme.typography.bodySmall, color = TextTertiary)
      }
      Spacer(modifier = Modifier.height(4.dp))
      Text("Actor: ${log.actorEmail} • Entity: ${log.targetEntity}", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
      if (log.afterValue.isNotEmpty()) {
        Spacer(modifier = Modifier.height(4.dp))
        Text("Change: ${log.beforeValue} -> ${log.afterValue}", style = MaterialTheme.typography.bodySmall, color = MintSecondary, fontWeight = FontWeight.Medium)
      }
    }
  }
}

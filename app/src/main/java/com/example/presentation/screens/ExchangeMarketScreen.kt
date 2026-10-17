package com.example.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.config.SupabaseConfig
import com.example.data.model.ExchangeRateRecord
import com.example.presentation.MainViewModel
import com.example.presentation.components.ExchangeRateChart
import com.example.presentation.components.SimulatorDisclaimerBanner
import com.example.ui.theme.*
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExchangeMarketScreen(
  viewModel: MainViewModel,
  modifier: Modifier = Modifier
) {
  val settings by viewModel.centralBankSettings.collectAsState()
  val rates by viewModel.exchangeRates.collectAsState()
  val currentUser by viewModel.currentUser.collectAsState()
  val systemAccounts by viewModel.systemAccounts.collectAsState()

  val userWallet = remember(currentUser, viewModel.repository.wallets.collectAsState().value) {
    currentUser?.let { viewModel.repository.getWallet(it.id) }
  }

  val exchangeInventory = systemAccounts[SupabaseConfig.ACC_EXCHANGE_INVENTORY]?.balance ?: 200_000L
  val latestRate = rates.lastOrNull()

  var selectedTimeRange by remember { mutableStateOf("24H") }
  var showTradeDialog by remember { mutableStateOf(false) }
  var tradeOrderType by remember { mutableStateOf("BUY") }

  Scaffold(
    topBar = {
      TopAppBar(
        title = {
          Text(
            "Bear Exchange Market",
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
      item { SimulatorDisclaimerBanner() }

      // Rate Hero Card
      item {
        Card(
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(24.dp),
          colors = CardDefaults.cardColors(containerColor = PureWhite),
          elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
          Column(modifier = Modifier.padding(20.dp)) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Text(
                text = "1 BEAR Reference Price",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
              )
              Surface(
                color = if ((latestRate?.changePercent ?: 0.0) >= 0) PositiveGreenBg else NegativeRedBg,
                shape = RoundedCornerShape(8.dp)
              ) {
                Row(
                  modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  val chg = latestRate?.changePercent ?: 0.0
                  Icon(
                    imageVector = if (chg >= 0) Icons.AutoMirrored.Filled.TrendingUp else Icons.AutoMirrored.Filled.TrendingDown,
                    contentDescription = null,
                    tint = if (chg >= 0) PositiveGreen else NegativeRed,
                    modifier = Modifier.size(14.dp)
                  )
                  Spacer(modifier = Modifier.width(4.dp))
                  Text(
                    text = "${if (chg >= 0) "+" else ""}${String.format(Locale.US, "%.2f", chg)}%",
                    color = if (chg >= 0) PositiveGreen else NegativeRed,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                  )
                }
              }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(verticalAlignment = Alignment.Bottom) {
              Text(
                text = "Rp ${settings.currentRateIdr}",
                style = MaterialTheme.typography.displayMedium,
                fontWeight = FontWeight.Bold,
                color = ForestGreenPrimary
              )
              Spacer(modifier = Modifier.width(6.dp))
              Text(
                text = "IDR",
                style = MaterialTheme.typography.titleMedium,
                color = TextSecondary,
                modifier = Modifier.padding(bottom = 6.dp)
              )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Time range selector
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              listOf("24H", "7D", "30D").forEach { range ->
                FilterChip(
                  selected = selectedTimeRange == range,
                  onClick = { selectedTimeRange = range },
                  label = { Text(range, fontWeight = FontWeight.SemiBold) },
                  colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = ForestGreenPrimary,
                    selectedLabelColor = PureWhite
                  )
                )
              }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Interactive Chart
            ExchangeRateChart(
              rates = when (selectedTimeRange) {
                "24H" -> rates.takeLast(6)
                "7D" -> rates.takeLast(14)
                else -> rates
              },
              isMini = false
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Trading Actions Row
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
              Button(
                onClick = {
                  tradeOrderType = "BUY"
                  showTradeDialog = true
                },
                modifier = Modifier
                  .weight(1f)
                  .height(48.dp)
                  .testTag("exchange_buy_button"),
                colors = ButtonDefaults.buttonColors(containerColor = ForestGreenPrimary),
                shape = RoundedCornerShape(14.dp)
              ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Buy Bear", fontWeight = FontWeight.Bold)
              }

              Button(
                onClick = {
                  tradeOrderType = "SELL"
                  showTradeDialog = true
                },
                modifier = Modifier
                  .weight(1f)
                  .height(48.dp)
                  .testTag("exchange_sell_button"),
                colors = ButtonDefaults.buttonColors(containerColor = MintSecondary),
                shape = RoundedCornerShape(14.dp)
              ) {
                Icon(Icons.Default.Remove, contentDescription = null, modifier = Modifier.size(18.dp), tint = ForestGreenDark)
                Spacer(modifier = Modifier.width(6.dp))
                Text("Sell Bear", fontWeight = FontWeight.Bold, color = ForestGreenDark)
              }
            }
          }
        }
      }

      // Market Info & Invariants
      item {
        Card(
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(20.dp),
          colors = CardDefaults.cardColors(containerColor = PureWhite)
        ) {
          Column(modifier = Modifier.padding(18.dp)) {
            Text(
              text = "Market Liquidity & Rules",
              style = MaterialTheme.typography.titleSmall,
              fontWeight = FontWeight.Bold,
              color = TextPrimary
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween
            ) {
              Text("Exchange Bear Inventory", color = TextSecondary, style = MaterialTheme.typography.bodyMedium)
              Text(
                text = "${NumberFormat.getNumberInstance(Locale.US).format(exchangeInventory)} B",
                fontWeight = FontWeight.Bold,
                color = ForestGreenPrimary
              )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween
            ) {
              Text("Market Status", color = TextSecondary, style = MaterialTheme.typography.bodyMedium)
              Text(
                text = if (settings.isMarketPaused) "Paused (Maintenance)" else "Active (Continuous)",
                fontWeight = FontWeight.Bold,
                color = if (settings.isMarketPaused) WarningAmber else PositiveGreen
              )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween
            ) {
              Text("Update Interval", color = TextSecondary, style = MaterialTheme.typography.bodyMedium)
              Text(
                text = "Every ${settings.updateIntervalHours} Hours",
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary
              )
            }
          }
        }
      }

      // Recent Rate History & Market Events
      item {
        Text(
          text = "Authoritative Market Settlements",
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.Bold,
          color = TextPrimary
        )
      }

      items(rates.reversed()) { record ->
        RateHistoryItem(record = record)
      }

      item { Spacer(modifier = Modifier.height(16.dp)) }
    }
  }

  if (showTradeDialog) {
    ExchangeTradeDialog(
      orderType = tradeOrderType,
      currentRate = settings.currentRateIdr,
      availableInventory = exchangeInventory,
      userBalance = userWallet?.balance ?: 0L,
      onDismiss = { showTradeDialog = false },
      onConfirm = { amount ->
        viewModel.executeExchangeTrade(tradeOrderType, amount)
        showTradeDialog = false
      }
    )
  }
}

@Composable
fun RateHistoryItem(record: ExchangeRateRecord) {
  val dateStr = remember(record.createdAt) {
    SimpleDateFormat("dd MMM, HH:mm", Locale.US).format(Date(record.createdAt))
  }

  Card(
    modifier = Modifier.fillMaxWidth(),
    shape = RoundedCornerShape(14.dp),
    colors = CardDefaults.cardColors(containerColor = PureWhite)
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp, vertical = 12.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Column {
        Text(
          text = record.marketEvent,
          style = MaterialTheme.typography.titleSmall,
          fontWeight = FontWeight.SemiBold,
          color = TextPrimary
        )
        Text(
          text = dateStr,
          style = MaterialTheme.typography.bodySmall,
          color = TextTertiary
        )
      }

      Column(horizontalAlignment = Alignment.End) {
        Text(
          text = "Rp ${record.rateIdr}",
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.Bold,
          color = ForestGreenPrimary
        )
        val chg = record.changePercent
        Text(
          text = "${if (chg >= 0) "+" else ""}${String.format(Locale.US, "%.2f", chg)}%",
          style = MaterialTheme.typography.bodySmall,
          fontWeight = FontWeight.Bold,
          color = if (chg >= 0) PositiveGreen else NegativeRed
        )
      }
    }
  }
}

@Composable
fun ExchangeTradeDialog(
  orderType: String,
  currentRate: Double,
  availableInventory: Long,
  userBalance: Long,
  onDismiss: () -> Unit,
  onConfirm: (Long) -> Unit
) {
  var amountText by remember { mutableStateOf("50") }
  val amount = amountText.toLongOrNull() ?: 0L
  val simulatedIdr = Math.round(amount * currentRate)

  val maxAvailable = if (orderType == "BUY") availableInventory else userBalance
  val isError = amount <= 0 || amount > maxAvailable

  AlertDialog(
    onDismissRequest = onDismiss,
    title = {
      Text(
        text = if (orderType == "BUY") "Buy Bear" else "Sell Bear",
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold,
        color = ForestGreenPrimary
      )
    },
    text = {
      Column {
        Text(
          text = "Simulated transaction against server exchange inventory at 1 BEAR = Rp $currentRate.",
          style = MaterialTheme.typography.bodySmall,
          color = TextSecondary
        )
        Spacer(modifier = Modifier.height(14.dp))

        OutlinedTextField(
          value = amountText,
          onValueChange = {
            if (it.all { c -> c.isDigit() }) amountText = it
          },
          label = { Text("Bear Amount") },
          trailingIcon = { Text("BEAR", fontWeight = FontWeight.Bold, modifier = Modifier.padding(end = 12.dp)) },
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
          singleLine = true,
          modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
          text = "Estimated Simulated Value: Rp ${NumberFormat.getNumberInstance(Locale.US).format(simulatedIdr)}",
          style = MaterialTheme.typography.bodyMedium,
          fontWeight = FontWeight.SemiBold,
          color = ForestGreenPrimary
        )

        Spacer(modifier = Modifier.height(4.dp))
        Text(
          text = "Maximum available to $orderType: $maxAvailable B",
          style = MaterialTheme.typography.bodySmall,
          color = if (isError) NegativeRed else TextTertiary
        )
      }
    },
    confirmButton = {
      Button(
        onClick = { onConfirm(amount) },
        enabled = !isError,
        colors = ButtonDefaults.buttonColors(containerColor = ForestGreenPrimary)
      ) {
        Text("Execute $orderType")
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text("Cancel", color = TextSecondary)
      }
    }
  )
}

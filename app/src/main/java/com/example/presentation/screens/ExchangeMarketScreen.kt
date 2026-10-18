package com.example.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
  onNavigateBack: () -> Unit = {},
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
            "Pasar Bursa BEAR",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = ForestGreenPrimary
          )
        },
        navigationIcon = {
          IconButton(onClick = onNavigateBack) {
            Icon(
              imageVector = Icons.AutoMirrored.Filled.ArrowBack,
              contentDescription = "Kembali ke Beranda",
              tint = ForestGreenPrimary
            )
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
      item { SimulatorDisclaimerBanner() }

      // User Balance Bar
      item {
        Surface(
          color = PureWhite,
          shape = RoundedCornerShape(14.dp),
          modifier = Modifier.fillMaxWidth(),
          tonalElevation = 1.dp
        ) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Column {
              Text("Saldo BEAR Anda", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
              Text(
                "${NumberFormat.getNumberInstance(Locale.US).format(userWallet?.balance ?: 0L)} B",
                fontWeight = FontWeight.Bold,
                color = ForestGreenPrimary,
                style = MaterialTheme.typography.titleMedium
              )
            }
            Column(horizontalAlignment = Alignment.End) {
              Text("Saldo Kas IDR", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
              Text(
                "Rp ${NumberFormat.getNumberInstance(Locale.US).format(userWallet?.virtualIdrBalance ?: 0L)}",
                fontWeight = FontWeight.Bold,
                color = ForestGreenPrimary,
                style = MaterialTheme.typography.titleMedium
              )
            }
          }
        }
      }

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
                text = "Harga Referensi 1 BEAR",
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

            Spacer(modifier = Modifier.height(16.dp))

            ExchangeRateChart(
              rates = rates,
              modifier = Modifier
                .fillMaxWidth()
                .height(160.dp)
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Liquidity & Status Row
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween
            ) {
              Text(
                text = "Stok Likuiditas Bursa:",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
              )
              Text(
                text = "${NumberFormat.getNumberInstance(Locale.US).format(exchangeInventory)} B",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold,
                color = ForestGreenPrimary
              )
            }
          }
        }
      }

      // Buy & Sell Order Action Buttons
      item {
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
              .height(52.dp)
              .testTag("exchange_buy_button"),
            colors = ButtonDefaults.buttonColors(containerColor = PositiveGreen),
            shape = RoundedCornerShape(14.dp)
          ) {
            Icon(Icons.Default.ShoppingBag, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Beli BEAR", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
          }

          Button(
            onClick = {
              tradeOrderType = "SELL"
              showTradeDialog = true
            },
            modifier = Modifier
              .weight(1f)
              .height(52.dp)
              .testTag("exchange_sell_button"),
            colors = ButtonDefaults.buttonColors(containerColor = NegativeRed),
            shape = RoundedCornerShape(14.dp)
          ) {
            Icon(Icons.Default.Sell, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Jual BEAR", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
          }
        }
      }

      // Market History Section
      item {
        Text(
          text = "Riwayat Pembaruan Kurs",
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
      userIdrBalance = userWallet?.virtualIdrBalance ?: 0L,
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
  userIdrBalance: Long,
  onDismiss: () -> Unit,
  onConfirm: (Long) -> Unit
) {
  var amountText by remember { mutableStateOf("10") }
  val amount = amountText.toLongOrNull() ?: 0L
  val simulatedIdr = Math.round(amount * currentRate)

  val isBuy = orderType == "BUY"
  val hasEnoughIdr = userIdrBalance >= simulatedIdr
  val hasEnoughInventory = availableInventory >= amount
  val hasEnoughBear = userBalance >= amount

  val isError = if (isBuy) {
    amount <= 0 || !hasEnoughIdr || !hasEnoughInventory
  } else {
    amount <= 0 || !hasEnoughBear
  }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = {
      Text(
        text = if (isBuy) "Beli BEAR di Bursa" else "Jual BEAR ke Bursa",
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold,
        color = ForestGreenPrimary
      )
    },
    text = {
      Column {
        Text(
          text = "Harga referensi bursa: 1 BEAR = Rp $currentRate.",
          style = MaterialTheme.typography.bodySmall,
          color = TextSecondary
        )
        Spacer(modifier = Modifier.height(14.dp))

        OutlinedTextField(
          value = amountText,
          onValueChange = {
            if (it.all { c -> c.isDigit() }) amountText = it
          },
          label = { Text("Jumlah BEAR") },
          trailingIcon = { Text("BEAR", fontWeight = FontWeight.Bold, modifier = Modifier.padding(end = 12.dp)) },
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
          singleLine = true,
          modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(10.dp))

        Surface(
          color = SageSurface,
          shape = RoundedCornerShape(10.dp),
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(modifier = Modifier.padding(10.dp)) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween
            ) {
              Text("Total Biaya/Hasil:", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
              Text(
                "Rp ${NumberFormat.getNumberInstance(Locale.US).format(simulatedIdr)}",
                fontWeight = FontWeight.Bold,
                color = ForestGreenPrimary
              )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween
            ) {
              Text("Saldo IDR Anda:", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
              Text(
                "Rp ${NumberFormat.getNumberInstance(Locale.US).format(userIdrBalance)}",
                fontWeight = FontWeight.SemiBold,
                color = if (!hasEnoughIdr && isBuy) NegativeRed else TextPrimary
              )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween
            ) {
              Text("Saldo BEAR Anda:", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
              Text(
                "$userBalance B",
                fontWeight = FontWeight.SemiBold,
                color = if (!hasEnoughBear && !isBuy) NegativeRed else TextPrimary
              )
            }
          }
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (isBuy && !hasEnoughIdr) {
          Text(
            text = "Saldo IDR Anda tidak mencukupi untuk membeli nominal ini. Silakan Top-Up Saldo IDR terlebih dahulu.",
            style = MaterialTheme.typography.bodySmall,
            color = NegativeRed,
            fontWeight = FontWeight.SemiBold
          )
        } else if (isBuy && !hasEnoughInventory) {
          Text(
            text = "Stok bursa tidak mencukupi (Tersedia $availableInventory B).",
            style = MaterialTheme.typography.bodySmall,
            color = NegativeRed,
            fontWeight = FontWeight.SemiBold
          )
        } else if (!isBuy && !hasEnoughBear) {
          Text(
            text = "Saldo BEAR Anda tidak mencukupi untuk menjual nominal ini.",
            style = MaterialTheme.typography.bodySmall,
            color = NegativeRed,
            fontWeight = FontWeight.SemiBold
          )
        }
      }
    },
    confirmButton = {
      Button(
        onClick = { onConfirm(amount) },
        enabled = !isError,
        colors = ButtonDefaults.buttonColors(containerColor = if (isBuy) PositiveGreen else NegativeRed)
      ) {
        Text(if (isBuy) "Beli Sekarang (Rp ${NumberFormat.getNumberInstance(Locale.US).format(simulatedIdr)})" else "Jual Sekarang")
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text("Batal", color = TextSecondary)
      }
    }
  )
}

package com.example.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.example.data.model.ActiveLoan
import com.example.data.model.ExchangeRateRecord
import com.example.presentation.MainViewModel
import com.example.presentation.components.*
import com.example.ui.theme.*

@Composable
fun HomeDashboardScreen(
  viewModel: MainViewModel,
  onNavigateToTransfer: () -> Unit,
  onNavigateToExchange: () -> Unit,
  onNavigateToBanking: () -> Unit,
  onNavigateToActivity: () -> Unit,
  onNavigateToNotifications: () -> Unit,
  modifier: Modifier = Modifier
) {
  val currentUser by viewModel.currentUser.collectAsState()
  val settings by viewModel.centralBankSettings.collectAsState()
  val rates by viewModel.exchangeRates.collectAsState()
  val transactions by viewModel.transactions.collectAsState()
  val activeLoans by viewModel.activeLoans.collectAsState()
  val notifications by viewModel.notifications.collectAsState()

  val userWallet = remember(currentUser, viewModel.repository.wallets.collectAsState().value) {
    currentUser?.let { viewModel.repository.getWallet(it.id) }
  }

  val userActiveLoan = activeLoans.find { it.userId == currentUser?.id && it.status == "ACTIVE" }
  val latestRate = rates.lastOrNull()
  val unreadCount = notifications.count { it.userId == currentUser?.id && !it.isRead }

  var showTopUpDialog by remember { mutableStateOf(false) }

  Scaffold(
    modifier = modifier.fillMaxSize(),
    containerColor = WarmWhite
  ) { paddingValues ->
    LazyColumn(
      modifier = Modifier
        .fillMaxSize()
        .padding(paddingValues)
        .padding(horizontal = 16.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
      item { Spacer(modifier = Modifier.height(8.dp)) }

      // Top Header Row
      item {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
              modifier = Modifier
                .size(46.dp)
                .clip(CircleShape)
                .background(ForestGreenPrimary),
              contentAlignment = Alignment.Center
            ) {
              Text(
                text = (currentUser?.fullName?.take(1) ?: "B").uppercase(),
                color = MintLight,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp
              )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
              Text(
                text = "Welcome back,",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
              )
              Text(
                text = currentUser?.fullName ?: "Guest",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
              )
            }
          }

          IconButton(
            onClick = onNavigateToNotifications,
            modifier = Modifier
              .size(44.dp)
              .clip(CircleShape)
              .background(PureWhite)
          ) {
            BadgedBox(
              badge = {
                if (unreadCount > 0) {
                  Badge { Text("$unreadCount") }
                }
              }
            ) {
              Icon(
                imageVector = Icons.Default.NotificationsNone,
                contentDescription = "Notifications",
                tint = ForestGreenPrimary
              )
            }
          }
        }
      }

      // Regulatory Disclaimer
      item {
        SimulatorDisclaimerBanner()
      }

      // Main Bear Balance Card
      item {
        BearBalanceCard(
          balance = userWallet?.balance ?: 0L,
          virtualIdrBalance = userWallet?.virtualIdrBalance ?: 0L,
          currentRateIdr = settings.currentRateIdr,
          changePercent = latestRate?.changePercent ?: 0.0,
          onTransferClick = onNavigateToTransfer,
          onTopUpClick = { showTopUpDialog = true },
          onExchangeClick = onNavigateToExchange,
          onBankingClick = onNavigateToBanking
        )
      }

      // Active Loan Summary (if active)
      if (userActiveLoan != null) {
        item {
          ActiveLoanBanner(
            loan = userActiveLoan,
            onClick = onNavigateToBanking
          )
        }
      }

      // Exchange Market Mini Card
      item {
        ExchangeMarketMiniCard(
          currentRate = settings.currentRateIdr,
          rates = rates,
          onClick = onNavigateToExchange
        )
      }

      // Recent Activity Header & Items
      item {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
            text = "Recent Activity",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
          )
          TextButton(onClick = onNavigateToActivity) {
            Text("See All", color = MintSecondary, fontWeight = FontWeight.SemiBold)
          }
        }
      }

      val userTransactions = transactions.filter {
        it.sourceAccount == currentUser?.id || it.destinationAccount == currentUser?.id
      }.take(4)

      if (userTransactions.isEmpty()) {
        item {
          Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = PureWhite)
          ) {
            Column(
              modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
              horizontalAlignment = Alignment.CenterHorizontally
            ) {
              Icon(
                imageVector = Icons.Default.ReceiptLong,
                contentDescription = null,
                tint = SageMuted,
                modifier = Modifier.size(36.dp)
              )
              Spacer(modifier = Modifier.height(8.dp))
              Text(
                text = "No transactions yet",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
              )
              Text(
                text = "Transfer to friends or explore the exchange market.",
                style = MaterialTheme.typography.bodySmall,
                color = TextTertiary
              )
            }
          }
        }
      } else {
        items(userTransactions) { tx ->
          TransactionItemRow(
            transaction = tx,
            currentUserId = currentUser?.id,
            onClick = onNavigateToActivity
          )
        }
      }

      item { Spacer(modifier = Modifier.height(16.dp)) }
    }
  }

  if (showTopUpDialog) {
    RealIdrTopUpDialog(
      onDismiss = { showTopUpDialog = false },
      onTopUp = { amount, method ->
        viewModel.executeTopUpIdr(amount, method)
        showTopUpDialog = false
      }
    )
  }
}

@Composable
fun ActiveLoanBanner(
  loan: ActiveLoan,
  onClick: () -> Unit,
  modifier: Modifier = Modifier
) {
  Card(
    modifier = modifier
      .fillMaxWidth()
      .clickable(onClick = onClick),
    shape = RoundedCornerShape(18.dp),
    colors = CardDefaults.cardColors(containerColor = SageSurface)
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(16.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      Box(
        modifier = Modifier
          .size(42.dp)
          .clip(CircleShape)
          .background(ForestGreenPrimary),
        contentAlignment = Alignment.Center
      ) {
        Icon(
          imageVector = Icons.Default.AccountBalance,
          contentDescription = null,
          tint = MintLight,
          modifier = Modifier.size(22.dp)
        )
      }
      Spacer(modifier = Modifier.width(12.dp))
      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = "Active Loan: ${loan.productName}",
          style = MaterialTheme.typography.titleSmall,
          fontWeight = FontWeight.Bold,
          color = ForestGreenPrimary
        )
        Text(
          text = "Remaining: ${loan.remainingAmount} B of ${loan.totalAmount} B",
          style = MaterialTheme.typography.bodySmall,
          color = TextSecondary
        )
      }
      Icon(
        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
        contentDescription = "Details",
        tint = ForestGreenPrimary,
        modifier = Modifier.size(20.dp)
      )
    }
  }
}

@Composable
fun ExchangeMarketMiniCard(
  currentRate: Double,
  rates: List<ExchangeRateRecord>,
  onClick: () -> Unit,
  modifier: Modifier = Modifier
) {
  Card(
    modifier = modifier
      .fillMaxWidth()
      .clickable(onClick = onClick),
    shape = RoundedCornerShape(20.dp),
    colors = CardDefaults.cardColors(containerColor = PureWhite),
    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
  ) {
    Column(modifier = Modifier.padding(16.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(
            imageVector = Icons.Default.ShowChart,
            contentDescription = null,
            tint = MintSecondary,
            modifier = Modifier.size(20.dp)
          )
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = "Bear Exchange",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
          )
        }
        Text(
          text = "1 BEAR = Rp $currentRate",
          style = MaterialTheme.typography.bodyMedium,
          fontWeight = FontWeight.SemiBold,
          color = ForestGreenPrimary
        )
      }

      Spacer(modifier = Modifier.height(10.dp))
      ExchangeRateChart(rates = rates, isMini = true)
    }
  }
}

@Composable
fun RealIdrTopUpDialog(
  onDismiss: () -> Unit,
  onTopUp: (Long, String) -> Unit
) {
  var selectedAmount by remember { mutableStateOf(50_000L) }
  var selectedMethod by remember { mutableStateOf("BCA Virtual Account") }

  val presets = listOf(25_000L, 50_000L, 100_000L, 250_000L, 500_000L)
  val methods = listOf(
    "BCA Virtual Account",
    "Mandiri Virtual Account",
    "BRI Virtual Account",
    "QRIS Instant"
  )

  AlertDialog(
    onDismissRequest = onDismiss,
    title = {
      Text(
        text = "Top-Up Saldo Kas IDR",
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold,
        color = ForestGreenPrimary
      )
    },
    text = {
      Column {
        Text(
          text = "Isi saldo Rupiah (IDR) Anda untuk digunakan membeli BEAR di Pasar Bursa.",
          style = MaterialTheme.typography.bodySmall,
          color = TextSecondary
        )
        Spacer(modifier = Modifier.height(14.dp))

        Text(
          text = "Pilih Nominal Top-Up:",
          style = MaterialTheme.typography.labelMedium,
          fontWeight = FontWeight.Bold,
          color = TextPrimary
        )
        Spacer(modifier = Modifier.height(8.dp))

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          presets.take(3).forEach { amt ->
            val isSelected = selectedAmount == amt
            Button(
              onClick = { selectedAmount = amt },
              modifier = Modifier.weight(1f),
              colors = ButtonDefaults.buttonColors(
                containerColor = if (isSelected) ForestGreenPrimary else SurfaceVariantColor,
                contentColor = if (isSelected) PureWhite else TextPrimary
              ),
              shape = RoundedCornerShape(10.dp),
              contentPadding = PaddingValues(vertical = 6.dp)
            ) {
              Text(
                text = "Rp ${amt / 1000}rb",
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp
              )
            }
          }
        }

        Spacer(modifier = Modifier.height(6.dp))

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          presets.drop(3).forEach { amt ->
            val isSelected = selectedAmount == amt
            Button(
              onClick = { selectedAmount = amt },
              modifier = Modifier.weight(1f),
              colors = ButtonDefaults.buttonColors(
                containerColor = if (isSelected) ForestGreenPrimary else SurfaceVariantColor,
                contentColor = if (isSelected) PureWhite else TextPrimary
              ),
              shape = RoundedCornerShape(10.dp),
              contentPadding = PaddingValues(vertical = 6.dp)
            ) {
              Text(
                text = "Rp ${amt / 1000}rb",
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp
              )
            }
          }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
          text = "Metode Pembayaran:",
          style = MaterialTheme.typography.labelMedium,
          fontWeight = FontWeight.Bold,
          color = TextPrimary
        )
        Spacer(modifier = Modifier.height(8.dp))

        methods.forEach { method ->
          val isSelected = selectedMethod == method
          Surface(
            modifier = Modifier
              .fillMaxWidth()
              .padding(vertical = 3.dp)
              .clickable { selectedMethod = method },
            shape = RoundedCornerShape(10.dp),
            color = if (isSelected) SageSurface else PureWhite,
            border = if (isSelected) androidx.compose.foundation.BorderStroke(1.5.dp, MintSecondary) else androidx.compose.foundation.BorderStroke(1.dp, SurfaceVariantColor)
          ) {
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                  imageVector = if (method.contains("QRIS")) Icons.Default.QrCode else Icons.Default.AccountBalance,
                  contentDescription = null,
                  tint = if (isSelected) ForestGreenPrimary else TextSecondary,
                  modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                  text = method,
                  style = MaterialTheme.typography.bodyMedium,
                  fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                  color = if (isSelected) ForestGreenPrimary else TextPrimary
                )
              }
              if (isSelected) {
                Icon(
                  imageVector = Icons.Default.CheckCircle,
                  contentDescription = "Selected",
                  tint = MintSecondary,
                  modifier = Modifier.size(18.dp)
                )
              }
            }
          }
        }
      }
    },
    confirmButton = {
      Button(
        onClick = { onTopUp(selectedAmount, selectedMethod) },
        colors = ButtonDefaults.buttonColors(containerColor = ForestGreenPrimary)
      ) {
        Text("Konfirmasi Top-Up Rp ${java.text.NumberFormat.getNumberInstance(java.util.Locale.US).format(selectedAmount)}")
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text("Batal", color = TextSecondary)
      }
    }
  )
}

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
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ActiveLoan
import com.example.data.model.LoanProduct
import com.example.presentation.components.ConfirmRow
import com.example.presentation.MainViewModel
import com.example.presentation.components.FourStageStepper
import com.example.presentation.components.SimulatorDisclaimerBanner
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BankingOverviewScreen(
  viewModel: MainViewModel,
  onNavigateToApplyLoan: () -> Unit,
  onNavigateToProcessing: (String) -> Unit,
  onNavigateBack: () -> Unit = {},
  modifier: Modifier = Modifier
) {
  val currentUser by viewModel.currentUser.collectAsState()
  val loanProducts by viewModel.loanProducts.collectAsState()
  val activeLoans by viewModel.activeLoans.collectAsState()
  val loanApplications by viewModel.loanApplications.collectAsState()

  val userWallet = remember(currentUser, viewModel.repository.wallets.collectAsState().value) {
    currentUser?.let { viewModel.repository.getWallet(it.id) }
  }

  val userActiveLoan = activeLoans.find { it.userId == currentUser?.id && it.status == "ACTIVE" }
  val pendingApp = loanApplications.find { it.userId == currentUser?.id && (it.stageStatus == "IN_PROGRESS" || it.stageStatus == "APPROVED") }

  var showRepayDialog by remember { mutableStateOf(false) }

  Scaffold(
    topBar = {
      TopAppBar(
        title = {
          Text(
            "Bear Bank & Credit",
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

      // Credit Score Gauge Card
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
                text = "Simulated Credit Score",
                style = MaterialTheme.typography.bodyMedium,
                color = MintLight
              )
              Surface(
                color = PureWhite.copy(alpha = 0.15f),
                shape = RoundedCornerShape(8.dp)
              ) {
                Text(
                  text = when {
                    (currentUser?.creditScore ?: 600) >= 750 -> "Excellent Rating"
                    (currentUser?.creditScore ?: 600) >= 650 -> "Good Rating"
                    else -> "Fair Rating"
                  },
                  color = MintLight,
                  style = MaterialTheme.typography.bodySmall,
                  fontWeight = FontWeight.Bold,
                  modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
              }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(verticalAlignment = Alignment.Bottom) {
              Text(
                text = "${currentUser?.creditScore ?: 600}",
                style = MaterialTheme.typography.displayLarge,
                fontWeight = FontWeight.Bold,
                color = PureWhite
              )
              Spacer(modifier = Modifier.width(8.dp))
              Text(
                text = "/ 850 Max",
                style = MaterialTheme.typography.titleMedium,
                color = SageSurface.copy(alpha = 0.8f),
                modifier = Modifier.padding(bottom = 6.dp)
              )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Score progress bar
            val progress = ((currentUser?.creditScore ?: 600) - 300) / 550f
            LinearProgressIndicator(
              progress = { progress.coerceIn(0.1f, 1f) },
              modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp)),
              color = MintSecondary,
              trackColor = Color.White.copy(alpha = 0.2f)
            )

            Spacer(modifier = Modifier.height(12.dp))
            Text(
              text = "Score improves by +25 points upon complete settlement of loans without default.",
              style = MaterialTheme.typography.bodySmall,
              color = SageSurface
            )
          }
        }
      }

      // Pending Loan Application Banner (if in progress or ready for acceptance)
      if (pendingApp != null) {
        item {
          Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(
              containerColor = if (pendingApp.stageStatus == "APPROVED") PositiveGreenBg else WarningAmberBg
            )
          ) {
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.SpaceBetween
            ) {
              Column(modifier = Modifier.weight(1f)) {
                Text(
                  text = if (pendingApp.stageStatus == "APPROVED") "Loan Offer Ready for Acceptance!" else "Loan Application in Progress (Stage ${pendingApp.currentStage}/4)",
                  style = MaterialTheme.typography.titleSmall,
                  fontWeight = FontWeight.Bold,
                  color = if (pendingApp.stageStatus == "APPROVED") PositiveGreen else WarningAmber
                )
                Text(
                  text = "${pendingApp.requestedAmount} B • ${pendingApp.productName}",
                  style = MaterialTheme.typography.bodySmall,
                  color = TextPrimary
                )
              }
              Button(
                onClick = { onNavigateToProcessing(pendingApp.id) },
                colors = ButtonDefaults.buttonColors(
                  containerColor = if (pendingApp.stageStatus == "APPROVED") PositiveGreen else ForestGreenPrimary
                ),
                shape = RoundedCornerShape(12.dp)
              ) {
                Text(if (pendingApp.stageStatus == "APPROVED") "Review Offer" else "View Progress")
              }
            }
          }
        }
      }

      // Active Loan Details Card (if active)
      if (userActiveLoan != null) {
        item {
          Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
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
                  text = "Active Loan: ${userActiveLoan.productName}",
                  style = MaterialTheme.typography.titleMedium,
                  fontWeight = FontWeight.Bold,
                  color = ForestGreenPrimary
                )
                Surface(
                  color = PositiveGreenBg,
                  shape = RoundedCornerShape(6.dp)
                ) {
                  Text(
                    text = "ACTIVE",
                    color = PositiveGreen,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                  )
                }
              }

              Spacer(modifier = Modifier.height(16.dp))

              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
              ) {
                Column {
                  Text("Outstanding Principal", color = TextSecondary, style = MaterialTheme.typography.bodySmall)
                  Text("${userActiveLoan.principalAmount} B", fontWeight = FontWeight.Bold, color = TextPrimary)
                }
                Column(horizontalAlignment = Alignment.End) {
                  Text("Total to Repay", color = TextSecondary, style = MaterialTheme.typography.bodySmall)
                  Text("${userActiveLoan.remainingAmount} B", fontWeight = FontWeight.Bold, color = ForestGreenPrimary)
                }
              }

              Spacer(modifier = Modifier.height(16.dp))

              Button(
                onClick = { showRepayDialog = true },
                modifier = Modifier
                  .fillMaxWidth()
                  .height(48.dp)
                  .testTag("loan_repay_button"),
                colors = ButtonDefaults.buttonColors(containerColor = ForestGreenPrimary),
                shape = RoundedCornerShape(14.dp)
              ) {
                Text("Make Repayment", fontWeight = FontWeight.Bold)
              }
            }
          }
        }
      }

      // Loan Products Catalogue
      item {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
            text = "Loan Facilities",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
          )
          if (userActiveLoan == null && pendingApp == null) {
            TextButton(onClick = onNavigateToApplyLoan) {
              Text("Apply Now", color = MintSecondary, fontWeight = FontWeight.Bold)
            }
          }
        }
      }

      items(loanProducts) { product ->
        LoanProductCard(
          product = product,
          canApply = userActiveLoan == null && pendingApp == null,
          onApplyClick = onNavigateToApplyLoan
        )
      }

      item { Spacer(modifier = Modifier.height(16.dp)) }
    }
  }

  if (showRepayDialog && userActiveLoan != null) {
    RepayLoanDialog(
      loan = userActiveLoan,
      userBalance = userWallet?.balance ?: 0L,
      onDismiss = { showRepayDialog = false },
      onRepay = { amount ->
        viewModel.repayLoan(userActiveLoan.id, amount)
        showRepayDialog = false
      }
    )
  }
}

@Composable
fun LoanProductCard(
  product: LoanProduct,
  canApply: Boolean,
  onApplyClick: () -> Unit,
  modifier: Modifier = Modifier
) {
  Card(
    modifier = modifier.fillMaxWidth(),
    shape = RoundedCornerShape(18.dp),
    colors = CardDefaults.cardColors(containerColor = PureWhite),
    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
  ) {
    Column(modifier = Modifier.padding(18.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          text = product.name,
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.Bold,
          color = ForestGreenPrimary
        )
        Surface(
          color = SageSurface,
          shape = RoundedCornerShape(8.dp)
        ) {
          Text(
            text = "${product.tenorDays} Days Tenor",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = ForestGreenPrimary,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
          )
        }
      }

      Spacer(modifier = Modifier.height(8.dp))
      Text(
        text = product.description,
        style = MaterialTheme.typography.bodySmall,
        color = TextSecondary
      )

      Spacer(modifier = Modifier.height(14.dp))
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Column {
          Text(
            text = "Flat Interest: ${product.interestRatePercent}%",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = MintSecondary
          )
          Text(
            text = "Limit: ${product.minAmount} - ${product.maxAmount} B",
            style = MaterialTheme.typography.bodySmall,
            color = TextTertiary
          )
        }

        if (canApply) {
          FilledTonalButton(
            onClick = onApplyClick,
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.filledTonalButtonColors(containerColor = ForestGreenPrimary, contentColor = PureWhite)
          ) {
            Text("Apply", fontWeight = FontWeight.Bold)
          }
        }
      }
    }
  }
}

@Composable
fun RepayLoanDialog(
  loan: ActiveLoan,
  userBalance: Long,
  onDismiss: () -> Unit,
  onRepay: (Long) -> Unit
) {
  var amountText by remember { mutableStateOf("${loan.remainingAmount}") }
  val amount = amountText.toLongOrNull() ?: 0L
  val isError = amount <= 0 || amount > loan.remainingAmount || amount > userBalance

  AlertDialog(
    onDismissRequest = onDismiss,
    title = {
      Text("Repay Loan", fontWeight = FontWeight.Bold, color = ForestGreenPrimary)
    },
    text = {
      Column {
        Text(
          text = "Principal will be returned to the Bank Lending Pool, and flat interest will route to Bank Interest Income.",
          style = MaterialTheme.typography.bodySmall,
          color = TextSecondary
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
          text = "Outstanding Debt: ${loan.remainingAmount} B",
          style = MaterialTheme.typography.bodyMedium,
          fontWeight = FontWeight.Bold,
          color = TextPrimary
        )
        Text(
          text = "Your Balance: $userBalance B",
          style = MaterialTheme.typography.bodySmall,
          color = TextTertiary
        )
        Spacer(modifier = Modifier.height(14.dp))
        OutlinedTextField(
          value = amountText,
          onValueChange = { if (it.all { c -> c.isDigit() }) amountText = it },
          label = { Text("Repayment Amount") },
          trailingIcon = { Text("BEAR", fontWeight = FontWeight.Bold, modifier = Modifier.padding(end = 12.dp)) },
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
          singleLine = true,
          modifier = Modifier.fillMaxWidth()
        )
      }
    },
    confirmButton = {
      Button(
        onClick = { onRepay(amount) },
        enabled = !isError,
        colors = ButtonDefaults.buttonColors(containerColor = ForestGreenPrimary)
      ) {
        Text("Confirm Repayment")
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) { Text("Cancel", color = TextSecondary) }
    }
  )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoanApplicationScreen(
  viewModel: MainViewModel,
  onNavigateBack: () -> Unit,
  onApplicationSubmitted: (String) -> Unit,
  modifier: Modifier = Modifier
) {
  val products by viewModel.loanProducts.collectAsState()
  var selectedProduct by remember { mutableStateOf(products.firstOrNull() ?: LoanProduct("prod_quick", "Bear Quick", 7, 2.0, 50, 1000, "")) }
  var requestedAmountText by remember { mutableStateOf("200") }
  val requestedAmount = requestedAmountText.toLongOrNull() ?: 0L

  val interest = Math.round(requestedAmount * (selectedProduct.interestRatePercent / 100.0))
  val total = requestedAmount + interest

  Scaffold(
    topBar = {
      TopAppBar(
        title = { Text("Apply for Loan", fontWeight = FontWeight.Bold, color = ForestGreenPrimary) },
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
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(paddingValues)
        .padding(20.dp),
      verticalArrangement = Arrangement.SpaceBetween
    ) {
      Column {
        Text(
          text = "Select Loan Facility",
          style = MaterialTheme.typography.titleSmall,
          fontWeight = FontWeight.Bold,
          color = TextPrimary
        )
        Spacer(modifier = Modifier.height(10.dp))

        products.forEach { prod ->
          val isSelected = selectedProduct.id == prod.id
          Card(
            modifier = Modifier
              .fillMaxWidth()
              .padding(vertical = 4.dp),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = if (isSelected) SageSurface else PureWhite),
            onClick = { selectedProduct = prod }
          ) {
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Column {
                Text(prod.name, fontWeight = FontWeight.Bold, color = ForestGreenPrimary)
                Text("${prod.tenorDays} Days • Flat ${prod.interestRatePercent}%", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
              }
              RadioButton(selected = isSelected, onClick = { selectedProduct = prod })
            }
          }
        }

        Spacer(modifier = Modifier.height(20.dp))

        OutlinedTextField(
          value = requestedAmountText,
          onValueChange = { if (it.all { c -> c.isDigit() }) requestedAmountText = it },
          label = { Text("Requested Amount (BEAR)") },
          trailingIcon = { Text("BEAR", fontWeight = FontWeight.Bold, modifier = Modifier.padding(end = 12.dp)) },
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
          singleLine = true,
          modifier = Modifier.fillMaxWidth().testTag("loan_request_amount_input"),
          shape = RoundedCornerShape(14.dp)
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
          text = "Facility limit: ${selectedProduct.minAmount} - ${selectedProduct.maxAmount} B",
          style = MaterialTheme.typography.bodySmall,
          color = TextTertiary
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Calculations Card
        Card(
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(16.dp),
          colors = CardDefaults.cardColors(containerColor = PureWhite)
        ) {
          Column(modifier = Modifier.padding(16.dp)) {
            ConfirmRow("Principal Amount", "$requestedAmount B")
            ConfirmRow("Flat Interest (${selectedProduct.interestRatePercent}%)", "$interest B")
            ConfirmRow("Total Repayment", "$total B", isHighlight = true)
            ConfirmRow("Tenor Duration", "${selectedProduct.tenorDays} Days")
          }
        }
      }

      Button(
        onClick = {
          viewModel.submitLoanApplication(selectedProduct.id, requestedAmount) { appId ->
            onApplicationSubmitted(appId)
          }
        },
        enabled = requestedAmount in selectedProduct.minAmount..selectedProduct.maxAmount,
        modifier = Modifier
          .fillMaxWidth()
          .height(54.dp)
          .testTag("loan_submit_application_button"),
        colors = ButtonDefaults.buttonColors(containerColor = ForestGreenPrimary),
        shape = RoundedCornerShape(16.dp)
      ) {
        Text("Submit for 4-Stage Assessment", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
      }
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoanFourStageProcessingScreen(
  viewModel: MainViewModel,
  applicationId: String,
  onNavigateBack: () -> Unit,
  onOfferAccepted: () -> Unit,
  modifier: Modifier = Modifier
) {
  LaunchedEffect(applicationId) {
    viewModel.selectExistingLoanApp(applicationId)
  }

  val currentApp by viewModel.currentLoanApp.collectAsState()
  val isLoading by viewModel.isOperationLoading.collectAsState()

  Scaffold(
    topBar = {
      TopAppBar(
        title = { Text("Loan Verification", fontWeight = FontWeight.Bold, color = ForestGreenPrimary) },
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
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(paddingValues)
        .padding(20.dp),
      verticalArrangement = Arrangement.SpaceBetween
    ) {
      Column {
        Text(
          text = "Authoritative 4-Stage Banking Engine",
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.Bold,
          color = ForestGreenPrimary
        )
        Text(
          text = "Simulating real multi-tier institutional risk verification.",
          style = MaterialTheme.typography.bodySmall,
          color = TextSecondary
        )

        Spacer(modifier = Modifier.height(24.dp))

        Card(
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(20.dp),
          colors = CardDefaults.cardColors(containerColor = PureWhite),
          elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
          Column(modifier = Modifier.padding(20.dp)) {
            FourStageStepper(
              currentStage = currentApp?.currentStage ?: 1,
              stageStatus = currentApp?.stageStatus ?: "IN_PROGRESS"
            )
          }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Live Stage Details Card
        Card(
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(18.dp),
          colors = CardDefaults.cardColors(
            containerColor = when (currentApp?.stageStatus) {
              "APPROVED" -> PositiveGreenBg
              "REJECTED" -> NegativeRedBg
              else -> PureWhite
            }
          )
        ) {
          Column(modifier = Modifier.padding(18.dp)) {
            Text(
              text = "Status: ${currentApp?.stageStatus ?: "PROCESSING"}",
              style = MaterialTheme.typography.titleSmall,
              fontWeight = FontWeight.Bold,
              color = when (currentApp?.stageStatus) {
                "APPROVED" -> PositiveGreen
                "REJECTED" -> NegativeRed
                else -> ForestGreenPrimary
              }
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
              text = when (currentApp?.stageStatus) {
                "APPROVED" -> "Assessment Successful! Your loan offer is approved. Review the terms below and accept to disburse."
                "REJECTED" -> currentApp?.decisionReason ?: "Application rejected by Bank Liquidity constraints."
                else -> "The banking engine is evaluating your eligibility, credit score records, and bank reserve liquidity."
              },
              style = MaterialTheme.typography.bodyMedium,
              color = TextPrimary
            )
          }
        }
      }

      if (currentApp?.stageStatus == "APPROVED") {
        Button(
          onClick = {
            viewModel.disburseLoan(applicationId, onOfferAccepted)
          },
          enabled = !isLoading,
          modifier = Modifier
            .fillMaxWidth()
            .height(54.dp)
            .testTag("loan_accept_offer_button"),
          colors = ButtonDefaults.buttonColors(containerColor = PositiveGreen),
          shape = RoundedCornerShape(16.dp)
        ) {
          if (isLoading) {
            CircularProgressIndicator(color = PureWhite, modifier = Modifier.size(24.dp))
          } else {
            Text("Accept Offer & Disburse Funds", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
          }
        }
      } else if (currentApp?.stageStatus == "REJECTED") {
        Button(
          onClick = onNavigateBack,
          modifier = Modifier
            .fillMaxWidth()
            .height(54.dp),
          colors = ButtonDefaults.buttonColors(containerColor = ForestGreenPrimary),
          shape = RoundedCornerShape(16.dp)
        ) {
          Text("Return to Banking", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
      }
    }
  }
}

package com.example.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.LedgerTransaction
import com.example.presentation.MainViewModel
import com.example.presentation.components.ConfirmRow
import com.example.presentation.components.SimulatorDisclaimerBanner
import com.example.presentation.components.TransactionItemRow
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActivityScreen(
  viewModel: MainViewModel,
  modifier: Modifier = Modifier
) {
  val currentUser by viewModel.currentUser.collectAsState()
  val transactions by viewModel.transactions.collectAsState()

  var selectedFilter by remember { mutableStateOf("All") }
  var selectedTx by remember { mutableStateOf<LedgerTransaction?>(null) }

  val filters = listOf("All", "Transfers", "Exchange", "Loans", "Top-up")

  val filteredTransactions = transactions.filter { tx ->
    val isUserTx = tx.sourceAccount == currentUser?.id || tx.destinationAccount == currentUser?.id
    if (!isUserTx) return@filter false

    when (selectedFilter) {
      "Transfers" -> tx.transactionType == "TRANSFER"
      "Exchange" -> tx.transactionType == "EXCHANGE_BUY" || tx.transactionType == "EXCHANGE_SELL"
      "Loans" -> tx.transactionType == "LOAN_DISBURSEMENT" || tx.transactionType == "LOAN_REPAYMENT"
      "Top-up" -> tx.transactionType == "TOPUP" || tx.transactionType == "SIGNUP_BONUS"
      else -> true
    }
  }

  Scaffold(
    topBar = {
      TopAppBar(
        title = {
          Text(
            "Transaction Activity",
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
      verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
      item { SimulatorDisclaimerBanner() }

      item {
        LazyRow(
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          modifier = Modifier.padding(vertical = 4.dp)
        ) {
          items(filters) { filter ->
            FilterChip(
              selected = selectedFilter == filter,
              onClick = { selectedFilter = filter },
              label = { Text(filter, fontWeight = FontWeight.SemiBold) },
              colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = ForestGreenPrimary,
                selectedLabelColor = PureWhite
              )
            )
          }
        }
      }

      if (filteredTransactions.isEmpty()) {
        item {
          Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = PureWhite)
          ) {
            Column(
              modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
              horizontalAlignment = Alignment.CenterHorizontally
            ) {
              Icon(Icons.Default.ReceiptLong, contentDescription = null, tint = SageMuted, modifier = Modifier.size(40.dp))
              Spacer(modifier = Modifier.height(10.dp))
              Text("No transactions found", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
            }
          }
        }
      } else {
        items(filteredTransactions) { tx ->
          TransactionItemRow(
            transaction = tx,
            currentUserId = currentUser?.id,
            onClick = { selectedTx = tx }
          )
        }
      }

      item { Spacer(modifier = Modifier.height(16.dp)) }
    }
  }

  if (selectedTx != null) {
    TransactionDetailDialog(
      transaction = selectedTx!!,
      onDismiss = { selectedTx = null }
    )
  }
}

@Composable
fun TransactionDetailDialog(
  transaction: LedgerTransaction,
  onDismiss: () -> Unit
) {
  val dateStr = remember(transaction.createdAt) {
    SimpleDateFormat("dd MMMM yyyy, HH:mm:ss", Locale.US).format(Date(transaction.createdAt))
  }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = {
      Text(
        text = "Transaction Details",
        fontWeight = FontWeight.Bold,
        color = ForestGreenPrimary
      )
    },
    text = {
      Column {
        ConfirmRow(label = "Type", value = transaction.transactionType)
        ConfirmRow(label = "Amount", value = "${transaction.amount} B", isHighlight = true)
        ConfirmRow(label = "Network Fee", value = "${transaction.fee} B")
        ConfirmRow(label = "Date & Time", value = dateStr)
        ConfirmRow(label = "Status", value = transaction.status)
        ConfirmRow(label = "Source", value = transaction.sourceAccount.take(16))
        ConfirmRow(label = "Destination", value = transaction.destinationAccount.take(16))
        if (transaction.note.isNotEmpty()) {
          ConfirmRow(label = "Note", value = transaction.note)
        }
        ConfirmRow(label = "Idempotency Key", value = transaction.idempotencyKey.take(16))
      }
    },
    confirmButton = {
      Button(
        onClick = onDismiss,
        colors = ButtonDefaults.buttonColors(containerColor = ForestGreenPrimary)
      ) {
        Text("Close")
      }
    }
  )
}

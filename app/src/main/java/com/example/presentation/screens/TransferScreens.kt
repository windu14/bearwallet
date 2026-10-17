package com.example.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.UserProfile
import com.example.presentation.components.ConfirmRow
import com.example.presentation.MainViewModel
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransferInputScreen(
  viewModel: MainViewModel,
  onNavigateBack: () -> Unit,
  onNavigateToConfirmation: (recipientHandle: String, amount: Long, note: String) -> Unit,
  modifier: Modifier = Modifier
) {
  val currentUser by viewModel.currentUser.collectAsState()
  val userWallet = remember(currentUser, viewModel.repository.wallets.collectAsState().value) {
    currentUser?.let { viewModel.repository.getWallet(it.id) }
  }
  val communityProfiles by viewModel.communityProfiles.collectAsState()

  var recipientHandle by remember { mutableStateOf("") }
  var amountText by remember { mutableStateOf("") }
  var noteText by remember { mutableStateOf("") }
  var errorMessage by remember { mutableStateOf<String?>(null) }

  val availableBalance = userWallet?.balance ?: 0L
  val otherUsers = remember(communityProfiles, currentUser) {
    communityProfiles.values.filter { it.id != currentUser?.id }
  }

  Scaffold(
    topBar = {
      TopAppBar(
        title = {
          Text(
            "Transfer Bear",
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
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(paddingValues)
        .padding(20.dp),
      verticalArrangement = Arrangement.SpaceBetween
    ) {
      Column {
        // Quick Select Friend
        if (otherUsers.isNotEmpty()) {
          Text(
            text = "Community Friends & Partner",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = TextPrimary
          )
          Spacer(modifier = Modifier.height(10.dp))
          LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp)
          ) {
            items(otherUsers) { friend ->
              Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                  .clickable { recipientHandle = "@${friend.username}" }
                  .padding(4.dp)
              ) {
                Box(
                  modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(if (recipientHandle == "@${friend.username}") MintSecondary else SageSurface),
                  contentAlignment = Alignment.Center
                ) {
                  Text(
                    text = friend.fullName.take(1).uppercase(),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (recipientHandle == "@${friend.username}") PureWhite else ForestGreenPrimary
                  )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                  text = "@${friend.username}",
                  style = MaterialTheme.typography.bodySmall,
                  fontWeight = FontWeight.Medium,
                  color = TextPrimary
                )
              }
            }
          }
          Spacer(modifier = Modifier.height(20.dp))
        }

        // Recipient Handle Input
        OutlinedTextField(
          value = recipientHandle,
          onValueChange = {
            recipientHandle = it
            errorMessage = null
          },
          label = { Text("Recipient Handle or Username") },
          placeholder = { Text("@maya, @alex, or email") },
          leadingIcon = { Icon(Icons.Default.AlternateEmail, contentDescription = null, tint = ForestGreenPrimary) },
          modifier = Modifier
            .fillMaxWidth()
            .testTag("transfer_recipient_input"),
          singleLine = true,
          shape = RoundedCornerShape(14.dp),
          colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = ForestGreenPrimary,
            unfocusedBorderColor = BorderLight
          )
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Amount Input
        OutlinedTextField(
          value = amountText,
          onValueChange = {
            if (it.all { char -> char.isDigit() }) {
              amountText = it
              errorMessage = null
            }
          },
          label = { Text("Amount (BEAR)") },
          placeholder = { Text("0") },
          trailingIcon = {
            Text(
              "BEAR",
              style = MaterialTheme.typography.titleSmall,
              fontWeight = FontWeight.Bold,
              color = ForestGreenPrimary,
              modifier = Modifier.padding(end = 12.dp)
            )
          },
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
          modifier = Modifier
            .fillMaxWidth()
            .testTag("transfer_amount_input"),
          singleLine = true,
          shape = RoundedCornerShape(14.dp),
          colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = ForestGreenPrimary,
            unfocusedBorderColor = BorderLight
          )
        )

        // Available Balance & Presets
        Spacer(modifier = Modifier.height(6.dp))
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
            text = "Available: $availableBalance B",
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondary
          )
          Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf(50L, 100L, 250L).forEach { preset ->
              AssistChip(
                onClick = { amountText = preset.toString() },
                label = { Text("$preset B", fontSize = 11.sp) }
              )
            }
          }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Note Input
        OutlinedTextField(
          value = noteText,
          onValueChange = { noteText = it },
          label = { Text("Transfer Note (Optional)") },
          placeholder = { Text("Coffee, dinner split, gift...") },
          leadingIcon = { Icon(Icons.Default.EditNote, contentDescription = null, tint = SageMuted) },
          modifier = Modifier.fillMaxWidth(),
          singleLine = true,
          shape = RoundedCornerShape(14.dp),
          colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = ForestGreenPrimary,
            unfocusedBorderColor = BorderLight
          )
        )

        if (errorMessage != null) {
          Spacer(modifier = Modifier.height(12.dp))
          Text(
            text = errorMessage ?: "",
            style = MaterialTheme.typography.bodySmall,
            color = NegativeRed
          )
        }
      }

      Button(
        onClick = {
          val cleanHandle = recipientHandle.trim().trimStart('@').lowercase()
          val amount = amountText.toLongOrNull() ?: 0L

          if (cleanHandle.isEmpty()) {
            errorMessage = "Please enter recipient handle"
            return@Button
          }
          if (amount <= 0) {
            errorMessage = "Please enter an amount greater than 0"
            return@Button
          }
          if (amount > availableBalance) {
            errorMessage = "Insufficient balance. You only have $availableBalance B"
            return@Button
          }

          onNavigateToConfirmation(cleanHandle, amount, noteText)
        },
        modifier = Modifier
          .fillMaxWidth()
          .height(54.dp)
          .testTag("transfer_continue_button"),
        colors = ButtonDefaults.buttonColors(containerColor = ForestGreenPrimary),
        shape = RoundedCornerShape(16.dp)
      ) {
        Text("Continue to Review", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
      }
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransferConfirmationScreen(
  viewModel: MainViewModel,
  recipientHandle: String,
  amount: Long,
  note: String,
  onNavigateBack: () -> Unit,
  onTransferSuccess: () -> Unit,
  modifier: Modifier = Modifier
) {
  val currentUser by viewModel.currentUser.collectAsState()
  val userWallet = remember(currentUser, viewModel.repository.wallets.collectAsState().value) {
    currentUser?.let { viewModel.repository.getWallet(it.id) }
  }
  val isLoading by viewModel.isOperationLoading.collectAsState()

  val availableBalance = userWallet?.balance ?: 0L
  val fee = 0L
  val resultingBalance = availableBalance - amount

  Scaffold(
    topBar = {
      TopAppBar(
        title = {
          Text(
            "Confirm Transfer",
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
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(paddingValues)
        .padding(20.dp),
      verticalArrangement = Arrangement.SpaceBetween
    ) {
      Column {
        Card(
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(22.dp),
          colors = CardDefaults.cardColors(containerColor = PureWhite),
          elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
          Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
          ) {
            Text(
              text = "Transfer Amount",
              style = MaterialTheme.typography.bodyMedium,
              color = TextSecondary
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.Bottom) {
              Text(
                text = "$amount",
                style = MaterialTheme.typography.displayMedium,
                fontWeight = FontWeight.Bold,
                color = ForestGreenPrimary
              )
              Spacer(modifier = Modifier.width(6.dp))
              Text(
                text = "BEAR",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MintSecondary,
                modifier = Modifier.padding(bottom = 6.dp)
              )
            }

            Spacer(modifier = Modifier.height(20.dp))
            HorizontalDivider(color = BorderLight)
            Spacer(modifier = Modifier.height(16.dp))

            ConfirmRow(label = "Recipient", value = "@$recipientHandle")
            ConfirmRow(label = "Network Fee", value = "0 B (Zero fee)")
            ConfirmRow(label = "Sender", value = "@${currentUser?.username ?: "you"}")
            if (note.isNotEmpty()) {
              ConfirmRow(label = "Note", value = note)
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = BorderLight)
            Spacer(modifier = Modifier.height(16.dp))

            ConfirmRow(label = "Current Balance", value = "$availableBalance B")
            ConfirmRow(label = "Resulting Balance", value = "$resultingBalance B", isHighlight = true)
          }
        }
      }

      Button(
        onClick = {
          viewModel.executeTransfer(
            recipientHandle = recipientHandle,
            amount = amount,
            note = note,
            onSuccess = onTransferSuccess
          )
        },
        enabled = !isLoading,
        modifier = Modifier
          .fillMaxWidth()
          .height(54.dp)
          .testTag("transfer_confirm_button"),
        colors = ButtonDefaults.buttonColors(containerColor = ForestGreenPrimary),
        shape = RoundedCornerShape(16.dp)
      ) {
        if (isLoading) {
          CircularProgressIndicator(color = PureWhite, modifier = Modifier.size(24.dp))
        } else {
          Text("Confirm and Send Bear", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
      }
    }
  }
}

@Composable
fun TransferReceiptScreen(
  viewModel: MainViewModel,
  onDone: () -> Unit,
  modifier: Modifier = Modifier
) {
  val tx by viewModel.lastTransferTx.collectAsState()
  val recipient by viewModel.lastTransferRecipient.collectAsState()

  val dateStr = remember(tx?.createdAt) {
    tx?.createdAt?.let { SimpleDateFormat("dd MMMM yyyy, HH:mm", Locale.US).format(Date(it)) } ?: ""
  }

  Scaffold(
    containerColor = WarmWhite,
    modifier = modifier.fillMaxSize()
  ) { paddingValues ->
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(paddingValues)
        .padding(24.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.SpaceBetween
    ) {
      Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(top = 32.dp)
      ) {
        Box(
          modifier = Modifier
            .size(72.dp)
            .clip(CircleShape)
            .background(PositiveGreenBg),
          contentAlignment = Alignment.Center
        ) {
          Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = "Success",
            tint = PositiveGreen,
            modifier = Modifier.size(44.dp)
          )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
          text = "Transfer Completed!",
          style = MaterialTheme.typography.headlineMedium,
          fontWeight = FontWeight.Bold,
          color = ForestGreenPrimary
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
          text = "${tx?.amount ?: 0} B",
          style = MaterialTheme.typography.displayMedium,
          fontWeight = FontWeight.Bold,
          color = TextPrimary
        )

        Spacer(modifier = Modifier.height(28.dp))

        Card(
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(20.dp),
          colors = CardDefaults.cardColors(containerColor = PureWhite),
          elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
          Column(modifier = Modifier.padding(20.dp)) {
            ConfirmRow(label = "Recipient", value = "@${recipient?.username ?: "friend"}")
            ConfirmRow(label = "Date & Time", value = dateStr)
            ConfirmRow(label = "Status", value = "COMPLETED")
            ConfirmRow(label = "Transaction Ref", value = (tx?.id ?: "").take(12))
          }
        }
      }

      Button(
        onClick = onDone,
        modifier = Modifier
          .fillMaxWidth()
          .height(54.dp)
          .testTag("transfer_done_button"),
        colors = ButtonDefaults.buttonColors(containerColor = ForestGreenPrimary),
        shape = RoundedCornerShape(16.dp)
      ) {
        Text("Back to Home", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
      }
    }
  }
}

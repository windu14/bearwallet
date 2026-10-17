package com.example.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.*
import com.example.data.repository.BearEconomyRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

sealed interface UiMessage {
  data class Success(val message: String) : UiMessage
  data class Error(val message: String) : UiMessage
}

class MainViewModel(
  val repository: BearEconomyRepository = BearEconomyRepository()
) : ViewModel() {

  val currentUser: StateFlow<UserProfile?> = repository.currentUser
  val systemAccounts: StateFlow<Map<String, SystemAccount>> = repository.systemAccounts
  val centralBankSettings: StateFlow<CentralBankSettings> = repository.centralBankSettings
  val exchangeRates: StateFlow<List<ExchangeRateRecord>> = repository.exchangeRates
  val loanProducts: StateFlow<List<LoanProduct>> = repository.loanProducts
  val loanApplications: StateFlow<List<LoanApplication>> = repository.loanApplications
  val activeLoans: StateFlow<List<ActiveLoan>> = repository.activeLoans
  val notifications: StateFlow<List<NotificationItem>> = repository.notifications
  val auditLogs: StateFlow<List<AuditLog>> = repository.auditLogs
  val transactions: StateFlow<List<LedgerTransaction>> = repository.transactions
  val communityProfiles: StateFlow<Map<String, UserProfile>> = repository.profiles

  private val _uiMessage = MutableStateFlow<UiMessage?>(null)
  val uiMessage: StateFlow<UiMessage?> = _uiMessage.asStateFlow()

  private val _isOperationLoading = MutableStateFlow(false)
  val isOperationLoading: StateFlow<Boolean> = _isOperationLoading.asStateFlow()

  // Last completed transfer for receipt screen
  private val _lastTransferTx = MutableStateFlow<LedgerTransaction?>(null)
  val lastTransferTx: StateFlow<LedgerTransaction?> = _lastTransferTx.asStateFlow()

  private val _lastTransferRecipient = MutableStateFlow<UserProfile?>(null)
  val lastTransferRecipient: StateFlow<UserProfile?> = _lastTransferRecipient.asStateFlow()

  // Selected loan application for 4-stage processing
  private val _currentLoanApp = MutableStateFlow<LoanApplication?>(null)
  val currentLoanApp: StateFlow<LoanApplication?> = _currentLoanApp.asStateFlow()

  fun clearUiMessage() {
    _uiMessage.value = null
  }

  // --- Auth ---

  fun signIn(emailOrHandle: String, pass: String, onSuccess: () -> Unit) {
    viewModelScope.launch {
      _isOperationLoading.value = true
      val result = repository.signIn(emailOrHandle, pass)
      _isOperationLoading.value = false
      result.onSuccess {
        _uiMessage.value = UiMessage.Success("Welcome back, @${it.username}!")
        onSuccess()
      }.onFailure {
        _uiMessage.value = UiMessage.Error(it.message ?: "Failed to sign in")
      }
    }
  }

  fun signUp(email: String, pass: String, username: String, fullName: String, onSuccess: () -> Unit) {
    viewModelScope.launch {
      _isOperationLoading.value = true
      val result = repository.signUp(email, pass, username, fullName)
      _isOperationLoading.value = false
      result.onSuccess {
        _uiMessage.value = UiMessage.Success("Account created! 500 B initial grant credited.")
        onSuccess()
      }.onFailure {
        _uiMessage.value = UiMessage.Error(it.message ?: "Sign up failed")
      }
    }
  }

  fun recoverPassword(email: String) {
    viewModelScope.launch {
      _isOperationLoading.value = true
      repository.recoverPassword(email)
      _isOperationLoading.value = false
      _uiMessage.value = UiMessage.Success("Password recovery instructions sent if email exists.")
    }
  }

  fun signOut() {
    repository.signOut()
    _uiMessage.value = UiMessage.Success("Signed out successfully.")
  }

  fun switchUser(userId: String) {
    repository.switchUser(userId)
    _uiMessage.value = UiMessage.Success("Switched active user.")
  }

  // --- Wallet & Transfer ---

  fun executeTransfer(
    recipientHandle: String,
    amount: Long,
    note: String,
    onSuccess: () -> Unit
  ) {
    viewModelScope.launch {
      _isOperationLoading.value = true
      val idempotencyKey = "tx_${UUID.randomUUID()}"
      val cleanHandle = recipientHandle.trim().trimStart('@').lowercase()
      val recipientProfile = repository.profiles.value.values.find {
        it.username.equals(cleanHandle, ignoreCase = true) || it.email.equals(cleanHandle, ignoreCase = true)
      }

      val result = repository.executeTransfer(cleanHandle, amount, idempotencyKey, note)
      _isOperationLoading.value = false

      result.onSuccess { tx ->
        _lastTransferTx.value = tx
        _lastTransferRecipient.value = recipientProfile
        _uiMessage.value = UiMessage.Success("Transferred $amount B successfully!")
        onSuccess()
      }.onFailure {
        _uiMessage.value = UiMessage.Error(it.message ?: "Transfer failed")
      }
    }
  }

  fun executeTopUp(amount: Long) {
    viewModelScope.launch {
      _isOperationLoading.value = true
      val key = "topup_${UUID.randomUUID()}"
      val result = repository.executeTopUp(amount, key)
      _isOperationLoading.value = false
      result.onSuccess {
        _uiMessage.value = UiMessage.Success("Claimed $amount B from the community distribution inventory!")
      }.onFailure {
        _uiMessage.value = UiMessage.Error(it.message ?: "Top-up failed")
      }
    }
  }

  // --- Exchange ---

  fun executeExchangeTrade(orderType: String, bearAmount: Long) {
    viewModelScope.launch {
      _isOperationLoading.value = true
      val key = "order_${UUID.randomUUID()}"
      val result = repository.executeExchangeTrade(orderType, bearAmount, key)
      _isOperationLoading.value = false
      result.onSuccess { order ->
        _uiMessage.value = UiMessage.Success("$orderType ${order.bearAmount} B executed at ${order.rateApplied} IDR (Simulated Rp ${order.idrAmount})")
      }.onFailure {
        _uiMessage.value = UiMessage.Error(it.message ?: "Trade failed")
      }
    }
  }

  fun triggerMarketRateUpdate(customEvent: String? = null) {
    viewModelScope.launch {
      _isOperationLoading.value = true
      val record = repository.triggerMarketRateUpdate(customEvent)
      _isOperationLoading.value = false
      _uiMessage.value = UiMessage.Success("Market rate updated to ${record.rateIdr} IDR (${if (record.changePercent >= 0) "+" else ""}${record.changePercent}%)")
    }
  }

  // --- Banking & 4-Stage Loan Processing ---

  fun submitLoanApplication(productId: String, amount: Long, onSubmitted: (String) -> Unit) {
    viewModelScope.launch {
      _isOperationLoading.value = true
      val result = repository.submitLoanApplication(productId, amount)
      _isOperationLoading.value = false
      result.onSuccess { app ->
        _currentLoanApp.value = app
        onSubmitted(app.id)
        startFourStageWorker(app.id)
      }.onFailure {
        _uiMessage.value = UiMessage.Error(it.message ?: "Loan application failed")
      }
    }
  }

  fun selectExistingLoanApp(appId: String) {
    val app = repository.loanApplications.value.find { it.id == appId }
    _currentLoanApp.value = app
    if (app?.stageStatus == "IN_PROGRESS") {
      startFourStageWorker(appId)
    }
  }

  private fun startFourStageWorker(appId: String) {
    viewModelScope.launch {
      // 4-Stage progression with persistent state in backend repository:
      // Stage 1 (Eligibility) -> Stage 2 (Credit Score) -> Stage 3 (Liquidity) -> Stage 4 (Decision)
      // Progression delay allows user to observe authentic verification and stages
      while (true) {
        val current = repository.loanApplications.value.find { it.id == appId } ?: break
        _currentLoanApp.value = current

        if (current.stageStatus != "IN_PROGRESS" || current.currentStage >= 4) {
          break
        }

        // Processing pause for realistic compliance & liquidity verification (~4 seconds per stage in mobile app)
        delay(4000)
        val advanceResult = repository.advanceLoanStage(appId)
        advanceResult.onSuccess { updated ->
          _currentLoanApp.value = updated
        }
      }
    }
  }

  fun disburseLoan(applicationId: String, onDisbursed: () -> Unit) {
    viewModelScope.launch {
      _isOperationLoading.value = true
      val result = repository.disburseLoan(applicationId)
      _isOperationLoading.value = false
      result.onSuccess {
        _uiMessage.value = UiMessage.Success("Loan disbursed! ${it.principalAmount} B added to your wallet.")
        onDisbursed()
      }.onFailure {
        _uiMessage.value = UiMessage.Error(it.message ?: "Disbursement failed")
      }
    }
  }

  fun repayLoan(loanId: String, amount: Long) {
    viewModelScope.launch {
      _isOperationLoading.value = true
      val result = repository.repayLoan(loanId, amount)
      _isOperationLoading.value = false
      result.onSuccess {
        _uiMessage.value = UiMessage.Success(
          if (it.status == "PAID") "Loan fully settled! Credit score increased."
          else "Repaid $amount B. Remaining balance: ${it.remainingAmount} B."
        )
      }.onFailure {
        _uiMessage.value = UiMessage.Error(it.message ?: "Repayment failed")
      }
    }
  }

  // --- Central Bank Admin ---

  fun updateCentralBankSettings(isPaused: Boolean, intervalHours: Int) {
    viewModelScope.launch {
      _isOperationLoading.value = true
      val result = repository.updateCentralBankSettings(isPaused, intervalHours)
      _isOperationLoading.value = false
      result.onSuccess {
        _uiMessage.value = UiMessage.Success("Central Bank settings updated.")
      }.onFailure {
        _uiMessage.value = UiMessage.Error(it.message ?: "Update failed")
      }
    }
  }
}

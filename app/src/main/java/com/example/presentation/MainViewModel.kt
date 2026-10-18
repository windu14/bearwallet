package com.example.presentation

import android.app.Application
import androidx.lifecycle.AndroidViewModel
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
  application: Application
) : AndroidViewModel(application) {

  val repository: BearEconomyRepository = BearEconomyRepository.getInstance(application.applicationContext)

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
        _uiMessage.value = UiMessage.Success("Selamat datang kembali, @${it.username}!")
        onSuccess()
      }.onFailure {
        _uiMessage.value = UiMessage.Error(it.message ?: "Gagal masuk")
      }
    }
  }

  fun signUp(email: String, pass: String, username: String, fullName: String, onSuccess: () -> Unit) {
    viewModelScope.launch {
      _isOperationLoading.value = true
      val result = repository.signUp(email, pass, username, fullName)
      _isOperationLoading.value = false
      result.onSuccess {
        _uiMessage.value = UiMessage.Success("Akun berhasil dibuat! Saldo awal 0 B & Rp 0.")
        onSuccess()
      }.onFailure {
        _uiMessage.value = UiMessage.Error(it.message ?: "Pendaftaran gagal")
      }
    }
  }

  fun recoverPassword(email: String) {
    viewModelScope.launch {
      _isOperationLoading.value = true
      repository.recoverPassword(email)
      _isOperationLoading.value = false
      _uiMessage.value = UiMessage.Success("Petunjuk pemulihan kata sandi telah dikirim.")
    }
  }

  fun signOut() {
    repository.signOut()
    _uiMessage.value = UiMessage.Success("Berhasil keluar dari akun.")
  }

  fun switchUser(userId: String) {
    repository.switchUser(userId)
    _uiMessage.value = UiMessage.Success("Berhasil beralih akun pengguna.")
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
        _uiMessage.value = UiMessage.Success("Berhasil mentransfer $amount B ke @${recipientProfile?.username ?: cleanHandle}!")
        onSuccess()
      }.onFailure {
        _uiMessage.value = UiMessage.Error(it.message ?: "Transfer gagal")
      }
    }
  }

  // Real IDR Top-Up via Bank / Virtual Account Simulator
  fun executeTopUpIdr(amountIdr: Long, paymentMethod: String) {
    viewModelScope.launch {
      _isOperationLoading.value = true
      val key = "topup_idr_${UUID.randomUUID()}"
      val result = repository.executeTopUpIdr(amountIdr, paymentMethod, key)
      _isOperationLoading.value = false
      result.onSuccess {
        _uiMessage.value = UiMessage.Success("Top-Up Berhasil! Saldo IDR bertambah Rp $amountIdr via $paymentMethod.")
      }.onFailure {
        _uiMessage.value = UiMessage.Error(it.message ?: "Top-up IDR gagal")
      }
    }
  }

  // --- Exchange Market ---

  fun executeExchangeTrade(orderType: String, bearAmount: Long) {
    viewModelScope.launch {
      _isOperationLoading.value = true
      val key = "order_${UUID.randomUUID()}"
      val result = repository.executeExchangeTrade(orderType, bearAmount, key)
      _isOperationLoading.value = false
      result.onSuccess { order ->
        val verb = if (order.orderType == "BUY") "Beli" else "Jual"
        _uiMessage.value = UiMessage.Success("$verb ${order.bearAmount} B berhasil pada kurs Rp ${order.rateApplied} (Total: Rp ${order.idrAmount})")
      }.onFailure {
        _uiMessage.value = UiMessage.Error(it.message ?: "Transaksi bursa gagal")
      }
    }
  }

  fun triggerMarketRateUpdate(customEvent: String? = null) {
    viewModelScope.launch {
      _isOperationLoading.value = true
      val record = repository.triggerMarketRateUpdate(customEvent)
      _isOperationLoading.value = false
      _uiMessage.value = UiMessage.Success("Kurs bursa diperbarui: Rp ${record.rateIdr}/BEAR (${if (record.changePercent >= 0) "+" else ""}${record.changePercent}%)")
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
        _uiMessage.value = UiMessage.Error(it.message ?: "Pengajuan pinjaman gagal")
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
      while (true) {
        val current = repository.loanApplications.value.find { it.id == appId } ?: break
        _currentLoanApp.value = current

        if (current.stageStatus != "IN_PROGRESS" || current.currentStage >= 4) {
          break
        }

        // Realistic verification pause (~3.5 seconds per stage)
        delay(3500)
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
        _uiMessage.value = UiMessage.Success("Pinjaman dicairkan! ${it.principalAmount} B telah ditambahkan ke wallet Anda.")
        onDisbursed()
      }.onFailure {
        _uiMessage.value = UiMessage.Error(it.message ?: "Pencairan pinjaman gagal")
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
          if (it.status == "PAID") "Pinjaman berhasil lunas seluruhnya! Skor kredit meningkat."
          else "Berhasil membayar $amount B. Sisa pinjaman: ${it.remainingAmount} B."
        )
      }.onFailure {
        _uiMessage.value = UiMessage.Error(it.message ?: "Pelunasan gagal")
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
        _uiMessage.value = UiMessage.Success("Pengaturan Bank Sentral berhasil diperbarui.")
      }.onFailure {
        _uiMessage.value = UiMessage.Error(it.message ?: "Pembaruan gagal")
      }
    }
  }
}

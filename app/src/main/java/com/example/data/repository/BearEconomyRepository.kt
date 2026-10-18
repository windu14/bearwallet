package com.example.data.repository

import android.content.Context
import com.example.core.config.SupabaseConfig
import com.example.core.network.SupabaseNetworkClient
import com.example.core.storage.BearLocalStorage
import com.example.data.model.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.UUID

class BearEconomyRepository(
  private val context: Context? = null,
  private val networkClient: SupabaseNetworkClient = SupabaseNetworkClient()
) {
  private val mutex = Mutex()
  private val storage = context?.let { BearLocalStorage(it.applicationContext) }

  companion object {
    @Volatile
    private var INSTANCE: BearEconomyRepository? = null

    fun getInstance(context: Context? = null): BearEconomyRepository {
      return INSTANCE ?: synchronized(this) {
        INSTANCE ?: BearEconomyRepository(context).also { INSTANCE = it }
      }
    }
  }

  // Current authenticated user session
  private val _currentUser = MutableStateFlow<UserProfile?>(null)
  val currentUser: StateFlow<UserProfile?> = _currentUser.asStateFlow()

  // Known community directory for user transfers
  private val _profiles = MutableStateFlow<Map<String, UserProfile>>(emptyMap())
  val profiles: StateFlow<Map<String, UserProfile>> = _profiles.asStateFlow()

  // Registered credentials for strict verification
  private val _userCredentials = mutableMapOf<String, String>()

  // Wallets mapped by userId
  private val _wallets = MutableStateFlow<Map<String, WalletAccount>>(emptyMap())
  val wallets: StateFlow<Map<String, WalletAccount>> = _wallets.asStateFlow()

  // System accounts (Double-entry source & sink pools)
  private val _systemAccounts = MutableStateFlow<Map<String, SystemAccount>>(emptyMap())
  val systemAccounts: StateFlow<Map<String, SystemAccount>> = _systemAccounts.asStateFlow()

  // Central bank settings
  private val _centralBankSettings = MutableStateFlow(CentralBankSettings())
  val centralBankSettings: StateFlow<CentralBankSettings> = _centralBankSettings.asStateFlow()

  // Ledger transactions
  private val _transactions = MutableStateFlow<List<LedgerTransaction>>(emptyList())
  val transactions: StateFlow<List<LedgerTransaction>> = _transactions.asStateFlow()

  // Exchange rate history
  private val _exchangeRates = MutableStateFlow<List<ExchangeRateRecord>>(emptyList())
  val exchangeRates: StateFlow<List<ExchangeRateRecord>> = _exchangeRates.asStateFlow()

  // Loan products
  private val _loanProducts = MutableStateFlow<List<LoanProduct>>(emptyList())
  val loanProducts: StateFlow<List<LoanProduct>> = _loanProducts.asStateFlow()

  // Loan applications
  private val _loanApplications = MutableStateFlow<List<LoanApplication>>(emptyList())
  val loanApplications: StateFlow<List<LoanApplication>> = _loanApplications.asStateFlow()

  // Active loans
  private val _activeLoans = MutableStateFlow<List<ActiveLoan>>(emptyList())
  val activeLoans: StateFlow<List<ActiveLoan>> = _activeLoans.asStateFlow()

  // Notifications
  private val _notifications = MutableStateFlow<List<NotificationItem>>(emptyList())
  val notifications: StateFlow<List<NotificationItem>> = _notifications.asStateFlow()

  // Audit logs
  private val _auditLogs = MutableStateFlow<List<AuditLog>>(emptyList())
  val auditLogs: StateFlow<List<AuditLog>> = _auditLogs.asStateFlow()

  // Supabase connection status
  private val _isSupabaseConnected = MutableStateFlow<Boolean?>(null)
  val isSupabaseConnected: StateFlow<Boolean?> = _isSupabaseConnected.asStateFlow()

  init {
    val persisted = storage?.loadState()
    if (persisted != null) {
      _profiles.value = persisted.profiles
      _userCredentials.putAll(persisted.credentials)
      _wallets.value = persisted.wallets
      _systemAccounts.value = persisted.systemAccounts
      _centralBankSettings.value = persisted.centralBankSettings
      _exchangeRates.value = persisted.exchangeRates
      _loanProducts.value = persisted.loanProducts
      _loanApplications.value = persisted.loanApplications
      _activeLoans.value = persisted.activeLoans
      _transactions.value = persisted.transactions
      _notifications.value = persisted.notifications
      _auditLogs.value = persisted.auditLogs

      // Restore active user session if exists
      val activeUser = persisted.activeUserId?.let { persisted.profiles[it] }
      _currentUser.value = activeUser
    } else {
      initializeAuthoritativeEconomy()
      persistState()
    }
  }

  private fun persistState() {
    storage?.saveState(
      BearLocalStorage.PersistedState(
        activeUserId = _currentUser.value?.id,
        profiles = _profiles.value,
        credentials = _userCredentials,
        wallets = _wallets.value,
        systemAccounts = _systemAccounts.value,
        centralBankSettings = _centralBankSettings.value,
        exchangeRates = _exchangeRates.value,
        loanProducts = _loanProducts.value,
        loanApplications = _loanApplications.value,
        activeLoans = _activeLoans.value,
        transactions = _transactions.value,
        notifications = _notifications.value,
        auditLogs = _auditLogs.value
      )
    )
  }

  private fun initializeAuthoritativeEconomy() {
    // 1. Initial supply split (Total = 1,000,000 B)
    val initialSystemAccounts = mapOf(
      SupabaseConfig.ACC_LOCKED_RESERVE to SystemAccount(
        accountCode = SupabaseConfig.ACC_LOCKED_RESERVE,
        name = "Locked Central Bank Reserve",
        balance = 100_000L,
        isLocked = true
      ),
      SupabaseConfig.ACC_BANK_LENDING_POOL to SystemAccount(
        accountCode = SupabaseConfig.ACC_BANK_LENDING_POOL,
        name = "Bank Lending Pool",
        balance = 250_000L,
        isLocked = false
      ),
      SupabaseConfig.ACC_EXCHANGE_INVENTORY to SystemAccount(
        accountCode = SupabaseConfig.ACC_EXCHANGE_INVENTORY,
        name = "Exchange Market Inventory",
        balance = 200_000L,
        isLocked = false
      ),
      SupabaseConfig.ACC_USER_DISTRIBUTION to SystemAccount(
        accountCode = SupabaseConfig.ACC_USER_DISTRIBUTION,
        name = "User Signup & Distribution Pool",
        balance = 450_000L,
        isLocked = false
      ),
      SupabaseConfig.ACC_BANK_INTEREST_INCOME to SystemAccount(
        accountCode = SupabaseConfig.ACC_BANK_INTEREST_INCOME,
        name = "Bank Interest Income Account",
        balance = 0L,
        isLocked = false
      )
    )
    _systemAccounts.value = initialSystemAccounts

    // 2. Initial Loan Products
    val products = listOf(
      LoanProduct(
        id = "prod_quick",
        name = "Bear Quick",
        tenorDays = 7,
        interestRatePercent = 2.0,
        minAmount = 50L,
        maxAmount = 1_000L,
        description = "7-day micro liquidity with 2% flat interest. Instant assessment."
      ),
      LoanProduct(
        id = "prod_regular",
        name = "Bear Regular",
        tenorDays = 30,
        interestRatePercent = 5.0,
        minAmount = 200L,
        maxAmount = 5_000L,
        description = "Standard 30-day term loan with 5% flat interest. Recommended for community projects."
      ),
      LoanProduct(
        id = "prod_long",
        name = "Bear Long",
        tenorDays = 60,
        interestRatePercent = 9.0,
        minAmount = 500L,
        maxAmount = 15_000L,
        description = "60-day structured facility with 9% flat interest. Higher borrowing capacity."
      )
    )
    _loanProducts.value = products

    // 3. Historical Exchange Rates (Initial 1 BEAR = IDR 10, with realistic historical trend points)
    val now = System.currentTimeMillis()
    val fourHours = 4 * 3600 * 1000L
    val initialRates = mutableListOf<ExchangeRateRecord>()
    var rate = 9.20
    val events = listOf(
      "Initial Market Launch" to -0.8,
      "Community Trading Boom" to +1.6,
      "Lending Liquidity Adjustment" to +0.9,
      "Weekend Exchange Volume High" to -0.5,
      "Reserve Balance Verification" to +1.2,
      "Market Equilibrium Settled" to +0.4,
      "Central Bank 4H Stabilization" to +0.6,
      "Current Reference Price" to +0.8
    )

    for (i in events.indices) {
      val (event, pct) = events[i]
      val prev = rate
      rate = (rate * (1.0 + pct / 100.0)).coerceAtLeast(1.0)
      initialRates.add(
        ExchangeRateRecord(
          rateIdr = Math.round(rate * 100.0) / 100.0,
          previousRate = Math.round(prev * 100.0) / 100.0,
          changePercent = pct,
          marketEvent = event,
          createdAt = now - (events.size - 1 - i) * fourHours
        )
      )
    }
    _exchangeRates.value = initialRates
    _centralBankSettings.value = _centralBankSettings.value.copy(
      currentRateIdr = initialRates.last().rateIdr
    )

    // 4. Initial community directory (Seed friends & partner accounts)
    val userMaya = UserProfile(
      id = "user_maya_01",
      email = "maya@bearwallet.app",
      username = "maya",
      fullName = "Maya Hartono",
      role = "user",
      creditScore = 720
    )
    val userAlex = UserProfile(
      id = "user_alex_02",
      email = "alex@bearwallet.app",
      username = "alex",
      fullName = "Alex Pranata",
      role = "user",
      creditScore = 650
    )
    val userCentralBank = UserProfile(
      id = "user_cb_admin",
      email = "admin@bearwallet.app",
      username = "centralbank",
      fullName = "Central Bank Inspector",
      role = "admin",
      creditScore = 850
    )
    val userBearBoss = UserProfile(
      id = "user_bearboss",
      email = "bearboss@bearwallet.app",
      username = "bearboss",
      fullName = "Bear Boss Admin",
      role = "admin",
      creditScore = 750
    )

    _profiles.value = mapOf(
      userBearBoss.id to userBearBoss,
      userMaya.id to userMaya,
      userAlex.id to userAlex,
      userCentralBank.id to userCentralBank
    )

    // Register passwords for seed accounts
    _userCredentials[userBearBoss.id] = "bearpassword123"
    _userCredentials[userMaya.id] = "bearpassword123"
    _userCredentials[userAlex.id] = "bearpassword123"
    _userCredentials[userCentralBank.id] = "bearpassword123"

    // Initial wallets for seed community members: Strictly 0 B balance and 0 IDR
    _wallets.value = mapOf(
      userBearBoss.id to WalletAccount(userId = userBearBoss.id, balance = 0L, virtualIdrBalance = 0L),
      userMaya.id to WalletAccount(userId = userMaya.id, balance = 0L, virtualIdrBalance = 0L),
      userAlex.id to WalletAccount(userId = userAlex.id, balance = 0L, virtualIdrBalance = 0L),
      userCentralBank.id to WalletAccount(userId = userCentralBank.id, balance = 0L, virtualIdrBalance = 0L)
    )

    // User is NOT logged in on first startup: must Sign In or Sign Up
    _currentUser.value = null
  }

  // --- Authentication Flows ---

  suspend fun signUp(email: String, pass: String, username: String, fullName: String): Result<UserProfile> = mutex.withLock {
    try {
      val cleanEmail = email.trim().lowercase()
      val cleanHandle = username.trim().trimStart('@').lowercase()
      val cleanName = fullName.trim()

      val emailRegex = Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")
      if (!emailRegex.matches(cleanEmail)) {
        return Result.failure(IllegalArgumentException("Format email tidak valid (contoh: user@email.com)"))
      }
      if (pass.length < 6) {
        return Result.failure(IllegalArgumentException("Password minimal harus 6 karakter"))
      }
      if (cleanHandle.length < 3 || !cleanHandle.matches(Regex("^[a-zA-Z0-9_]+$"))) {
        return Result.failure(IllegalArgumentException("Username minimal 3 karakter dan hanya boleh huruf, angka, atau underscore"))
      }
      if (cleanName.isEmpty()) {
        return Result.failure(IllegalArgumentException("Nama lengkap tidak boleh kosong"))
      }

      // Check handle or email uniqueness locally
      if (_profiles.value.values.any { it.username.equals(cleanHandle, ignoreCase = true) || it.email.equals(cleanEmail, ignoreCase = true) }) {
        return Result.failure(IllegalArgumentException("Username @$cleanHandle atau email tersebut sudah terdaftar"))
      }

      // Call remote Supabase Auth (/auth/v1/signup)
      val remoteResult = networkClient.signUp(cleanEmail, pass, cleanHandle, cleanName)
      val newId = if (remoteResult.isSuccess) {
        _isSupabaseConnected.value = true
        networkClient.currentUserId ?: UUID.randomUUID().toString()
      } else {
        val remoteError = remoteResult.exceptionOrNull()?.message ?: ""
        val isExplicitDuplicate = remoteError.contains("already registered", ignoreCase = true) ||
          remoteError.contains("already exists", ignoreCase = true)
        if (isExplicitDuplicate) {
          return Result.failure(IllegalArgumentException(remoteError))
        }
        _isSupabaseConnected.value = false
        UUID.randomUUID().toString()
      }

      val newUser = UserProfile(
        id = newId,
        email = cleanEmail,
        username = cleanHandle,
        fullName = cleanName,
        role = if (cleanHandle == "admin" || cleanHandle == "centralbank") "admin" else "user",
        creditScore = 600
      )

      _userCredentials[newUser.id] = pass
      _profiles.value = _profiles.value + (newUser.id to newUser)

      // Initial wallet strictly with 0 B and 0 IDR balance
      val userWallet = WalletAccount(
        userId = newUser.id,
        balance = 0L,
        virtualIdrBalance = 0L
      )
      _wallets.value = _wallets.value + (newUser.id to userWallet)

      _currentUser.value = newUser

      addNotification(
        userId = newUser.id,
        title = "Akun Berhasil Dibuat",
        message = "Selamat datang @${newUser.username}! Saldo awal Anda adalah 0 B dan Rp 0. Silakan lakukan Top-Up Saldo IDR atau ajukan pinjaman bank untuk memulai.",
        type = "SYSTEM"
      )

      addAuditLog(
        actor = newUser.email,
        action = "USER_REGISTERED",
        target = newUser.username,
        before = "None",
        after = "Registered with 0 B balance & 0 IDR"
      )

      persistState()
      Result.success(newUser)
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  suspend fun signIn(emailOrHandle: String, pass: String): Result<UserProfile> = mutex.withLock {
    try {
      val target = emailOrHandle.trim().trimStart('@').lowercase()

      if (target.isBlank()) {
        return Result.failure(IllegalArgumentException("Email atau username tidak boleh kosong"))
      }
      if (pass.isBlank()) {
        return Result.failure(IllegalArgumentException("Password tidak boleh kosong"))
      }

      val emailForAuth = if (target.contains("@")) target else {
        _profiles.value.values.find { it.username.equals(target, ignoreCase = true) }?.email ?: "$target@bearwallet.app"
      }

      // 1. Authenticate with Supabase Auth backend
      val remoteResult = networkClient.signIn(emailForAuth, pass)
      if (remoteResult.isSuccess) {
        _isSupabaseConnected.value = true
        val userId = networkClient.currentUserId ?: ""
        var profile = _profiles.value[userId] ?: _profiles.value.values.find {
          it.email.equals(emailForAuth, ignoreCase = true) || it.username.equals(target, ignoreCase = true)
        }
        if (profile == null) {
          profile = UserProfile(
            id = userId.ifEmpty { UUID.randomUUID().toString() },
            email = emailForAuth,
            username = target,
            fullName = target.replaceFirstChar { it.uppercase() },
            role = "user",
            creditScore = 600
          )
          _profiles.value = _profiles.value + (profile.id to profile)
          if (!_wallets.value.containsKey(profile.id)) {
            _wallets.value = _wallets.value + (profile.id to WalletAccount(userId = profile.id, balance = 0L, virtualIdrBalance = 0L))
          }
        }
        _currentUser.value = profile
        persistState()
        return@withLock Result.success(profile)
      }

      // 2. Strict verification against registered credentials
      val localProfile = _profiles.value.values.find {
        it.email.equals(target, ignoreCase = true) || it.username.equals(target, ignoreCase = true)
      }

      if (localProfile != null) {
        val storedPass = _userCredentials[localProfile.id]
        if (storedPass != null && storedPass == pass) {
          _currentUser.value = localProfile
          persistState()
          return@withLock Result.success(localProfile)
        } else {
          return@withLock Result.failure(IllegalArgumentException("Password yang Anda masukkan salah"))
        }
      }

      // 3. User not found -> strict rejection (no dummy account creation!)
      val remoteError = remoteResult.exceptionOrNull()?.message ?: ""
      val isNetworkIssue = remoteError.contains("Failed to connect") || remoteError.contains("Unable to resolve host")

      val errorMessage = if (remoteError.isNotBlank() && !isNetworkIssue) {
        remoteError
      } else {
        "Akun '$target' tidak ditemukan atau password salah. Silakan periksa kembali atau daftar akun baru."
      }

      Result.failure(IllegalArgumentException(errorMessage))
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  suspend fun recoverPassword(email: String): Result<Boolean> = mutex.withLock {
    networkClient.recoverPassword(email)
    Result.success(true)
  }

  fun signOut() {
    networkClient.signOut()
    _currentUser.value = null
    persistState()
  }

  fun switchUser(userId: String) {
    _profiles.value[userId]?.let {
      _currentUser.value = it
      persistState()
    }
  }

  // --- Wallet & Transfer Operations ---

  fun getWallet(userId: String): WalletAccount {
    return _wallets.value[userId] ?: WalletAccount(userId = userId, balance = 0L, virtualIdrBalance = 0L)
  }

  suspend fun executeTransfer(
    recipientHandle: String,
    amount: Long,
    idempotencyKey: String,
    note: String
  ): Result<LedgerTransaction> = mutex.withLock {
    val sender = _currentUser.value ?: return Result.failure(IllegalStateException("Not authenticated"))

    if (amount <= 0) {
      return Result.failure(IllegalArgumentException("Transfer amount must be greater than 0"))
    }

    // Idempotency check
    val existing = _transactions.value.find { it.idempotencyKey == idempotencyKey }
    if (existing != null) {
      return Result.success(existing)
    }

    val cleanHandle = recipientHandle.trim().trimStart('@').lowercase()
    val recipient = _profiles.value.values.find {
      it.username.equals(cleanHandle, ignoreCase = true) || it.email.equals(cleanHandle, ignoreCase = true)
    } ?: return Result.failure(IllegalArgumentException("Penerima '@$cleanHandle' tidak ditemukan dalam komunitas Bear"))

    if (recipient.id == sender.id) {
      return Result.failure(IllegalArgumentException("Tidak dapat mentransfer Bear ke akun Anda sendiri"))
    }

    val senderWallet = getWallet(sender.id)
    if (senderWallet.balance < amount) {
      return Result.failure(
        IllegalStateException("Saldo BEAR tidak cukup. Saldo saat ini: ${senderWallet.balance} B, jumlah transfer: $amount B")
      )
    }

    val recipientWallet = getWallet(recipient.id)

    // Atomic debit & credit
    val updatedSenderWallet = senderWallet.copy(
      balance = senderWallet.balance - amount,
      updatedAt = System.currentTimeMillis()
    )
    val updatedRecipientWallet = recipientWallet.copy(
      balance = recipientWallet.balance + amount,
      updatedAt = System.currentTimeMillis()
    )

    _wallets.value = _wallets.value + (sender.id to updatedSenderWallet) + (recipient.id to updatedRecipientWallet)

    val tx = LedgerTransaction(
      idempotencyKey = idempotencyKey,
      transactionType = TransactionType.TRANSFER.name,
      amount = amount,
      fee = 0L,
      sourceAccount = sender.id,
      destinationAccount = recipient.id,
      note = note.ifEmpty { "Transfer to @${recipient.username}" },
      status = "COMPLETED"
    )

    _transactions.value = listOf(tx) + _transactions.value

    addNotification(
      userId = recipient.id,
      title = "BEAR Received",
      message = "Received $amount B from @${sender.username}${if (note.isNotEmpty()) ": \"$note\"" else ""}",
      type = "TRANSFER"
    )

    addNotification(
      userId = sender.id,
      title = "Transfer Sent",
      message = "Successfully transferred $amount B to @${recipient.username}.",
      type = "TRANSFER"
    )

    persistState()
    Result.success(tx)
  }

  // --- Real IDR Top-Up Simulator (via Bank Virtual Account / QRIS) ---

  suspend fun executeTopUpIdr(
    amountIdr: Long,
    paymentMethod: String,
    idempotencyKey: String
  ): Result<LedgerTransaction> = mutex.withLock {
    val user = _currentUser.value ?: return Result.failure(IllegalStateException("Not authenticated"))

    if (amountIdr <= 0) {
      return Result.failure(IllegalArgumentException("Nominal top-up IDR harus lebih dari 0"))
    }

    val existing = _transactions.value.find { it.idempotencyKey == idempotencyKey }
    if (existing != null) return Result.success(existing)

    val userWallet = getWallet(user.id)
    val updatedWallet = userWallet.copy(
      virtualIdrBalance = userWallet.virtualIdrBalance + amountIdr,
      updatedAt = System.currentTimeMillis()
    )
    _wallets.value = _wallets.value + (user.id to updatedWallet)

    val tx = LedgerTransaction(
      idempotencyKey = idempotencyKey,
      transactionType = TransactionType.TOPUP_IDR.name,
      amount = amountIdr,
      sourceAccount = paymentMethod,
      destinationAccount = user.id,
      note = "Top-Up Saldo IDR via $paymentMethod",
      status = "COMPLETED"
    )
    _transactions.value = listOf(tx) + _transactions.value

    addNotification(
      userId = user.id,
      title = "Top-Up IDR Berhasil",
      message = "Saldo Rupiah bertambah Rp $amountIdr via $paymentMethod. Gunakan untuk membeli BEAR di pasar bursa.",
      type = "TOPUP"
    )

    persistState()
    Result.success(tx)
  }

  suspend fun executeTopUp(amount: Long, idempotencyKey: String): Result<LedgerTransaction> = mutex.withLock {
    val user = _currentUser.value ?: return Result.failure(IllegalStateException("Not authenticated"))

    if (amount <= 0 || amount > 500) {
      return Result.failure(IllegalArgumentException("Top-up limit: between 1 and 500 B per request"))
    }

    val existing = _transactions.value.find { it.idempotencyKey == idempotencyKey }
    if (existing != null) return Result.success(existing)

    val distPool = _systemAccounts.value[SupabaseConfig.ACC_USER_DISTRIBUTION]
      ?: return Result.failure(IllegalStateException("Distribution pool account not found"))

    if (distPool.balance < amount) {
      return Result.failure(IllegalStateException("Distribution inventory exhausted! Available: ${distPool.balance} B"))
    }

    val updatedPool = distPool.copy(balance = distPool.balance - amount)
    val userWallet = getWallet(user.id)
    val updatedWallet = userWallet.copy(balance = userWallet.balance + amount, updatedAt = System.currentTimeMillis())

    _systemAccounts.value = _systemAccounts.value + (SupabaseConfig.ACC_USER_DISTRIBUTION to updatedPool)
    _wallets.value = _wallets.value + (user.id to updatedWallet)

    val tx = LedgerTransaction(
      idempotencyKey = idempotencyKey,
      transactionType = TransactionType.TOPUP.name,
      amount = amount,
      sourceAccount = SupabaseConfig.ACC_USER_DISTRIBUTION,
      destinationAccount = user.id,
      note = "Community distribution grant top-up",
      status = "COMPLETED"
    )
    _transactions.value = listOf(tx) + _transactions.value

    addNotification(
      userId = user.id,
      title = "Top-up Berhasil",
      message = "Menambahkan $amount B dari pool distribusi komunitas.",
      type = "TOPUP"
    )

    persistState()
    Result.success(tx)
  }

  // --- Dynamic Exchange Operations (Requiring & Deducting IDR) ---

  suspend fun executeExchangeTrade(
    orderType: String, // "BUY" or "SELL"
    bearAmount: Long,
    idempotencyKey: String
  ): Result<ExchangeOrder> = mutex.withLock {
    val user = _currentUser.value ?: return Result.failure(IllegalStateException("Not authenticated"))

    if (_centralBankSettings.value.isMarketPaused) {
      return Result.failure(IllegalStateException("Pasar bursa Bear saat ini ditangguhkan oleh Bank Sentral"))
    }

    if (bearAmount <= 0) {
      return Result.failure(IllegalArgumentException("Jumlah transaksi harus lebih dari 0 BEAR"))
    }

    val currentRate = _centralBankSettings.value.currentRateIdr
    val idrAmount = Math.round(bearAmount * currentRate)
    val exchangeInventory = _systemAccounts.value[SupabaseConfig.ACC_EXCHANGE_INVENTORY]
      ?: return Result.failure(IllegalStateException("Akun likuiditas pasar bursa tidak ditemukan"))

    val userWallet = getWallet(user.id)

    if (orderType == "BUY") {
      // Must have enough IDR cash balance to purchase
      if (userWallet.virtualIdrBalance < idrAmount) {
        return Result.failure(
          IllegalStateException(
            "Saldo IDR Anda tidak mencukupi (Dibutuhkan Rp $idrAmount, saldo IDR Anda Rp ${userWallet.virtualIdrBalance}). Silakan lakukan Top-Up Saldo IDR terlebih dahulu."
          )
        )
      }

      // Must have enough BEAR inventory in system account
      if (exchangeInventory.balance < bearAmount) {
        return Result.failure(
          IllegalStateException("Stok likuiditas bursa tidak mencukupi. Tersedia: ${exchangeInventory.balance} B")
        )
      }

      // Deduct IDR, Credit BEAR to user; Deduct BEAR from exchange inventory
      val updatedInventory = exchangeInventory.copy(balance = exchangeInventory.balance - bearAmount)
      val updatedWallet = userWallet.copy(
        balance = userWallet.balance + bearAmount,
        virtualIdrBalance = userWallet.virtualIdrBalance - idrAmount,
        updatedAt = System.currentTimeMillis()
      )

      _systemAccounts.value = _systemAccounts.value + (SupabaseConfig.ACC_EXCHANGE_INVENTORY to updatedInventory)
      _wallets.value = _wallets.value + (user.id to updatedWallet)

      // Slight positive price impact from buying volume
      val newRate = (currentRate * 1.002).coerceAtMost(_centralBankSettings.value.maxRateClamp)
      _centralBankSettings.value = _centralBankSettings.value.copy(currentRateIdr = Math.round(newRate * 100.0) / 100.0)

      val tx = LedgerTransaction(
        idempotencyKey = idempotencyKey,
        transactionType = TransactionType.EXCHANGE_BUY.name,
        amount = bearAmount,
        sourceAccount = SupabaseConfig.ACC_EXCHANGE_INVENTORY,
        destinationAccount = user.id,
        note = "Beli $bearAmount B seharga Rp $idrAmount (Kurs Rp $currentRate/BEAR)",
        status = "COMPLETED"
      )
      _transactions.value = listOf(tx) + _transactions.value

    } else if (orderType == "SELL") {
      // User must have enough BEAR in wallet
      if (userWallet.balance < bearAmount) {
        return Result.failure(
          IllegalStateException("Saldo BEAR tidak mencukupi untuk menjual. Tersedia: ${userWallet.balance} B")
        )
      }

      // Move BEAR from user wallet back to exchange inventory, credit IDR cash to user
      val updatedWallet = userWallet.copy(
        balance = userWallet.balance - bearAmount,
        virtualIdrBalance = userWallet.virtualIdrBalance + idrAmount,
        updatedAt = System.currentTimeMillis()
      )
      val updatedInventory = exchangeInventory.copy(balance = exchangeInventory.balance + bearAmount)

      _wallets.value = _wallets.value + (user.id to updatedWallet)
      _systemAccounts.value = _systemAccounts.value + (SupabaseConfig.ACC_EXCHANGE_INVENTORY to updatedInventory)

      // Slight negative price impact from selling volume
      val newRate = (currentRate * 0.998).coerceAtLeast(_centralBankSettings.value.minRateClamp)
      _centralBankSettings.value = _centralBankSettings.value.copy(currentRateIdr = Math.round(newRate * 100.0) / 100.0)

      val tx = LedgerTransaction(
        idempotencyKey = idempotencyKey,
        transactionType = TransactionType.EXCHANGE_SELL.name,
        amount = bearAmount,
        sourceAccount = user.id,
        destinationAccount = SupabaseConfig.ACC_EXCHANGE_INVENTORY,
        note = "Jual $bearAmount B mendapatkan Rp $idrAmount (Kurs Rp $currentRate/BEAR)",
        status = "COMPLETED"
      )
      _transactions.value = listOf(tx) + _transactions.value
    } else {
      return Result.failure(IllegalArgumentException("Tipe order tidak valid: $orderType"))
    }

    val order = ExchangeOrder(
      userId = user.id,
      orderType = orderType,
      bearAmount = bearAmount,
      idrAmount = idrAmount,
      rateApplied = currentRate
    )

    addNotification(
      userId = user.id,
      title = "Order Bursa Berhasil",
      message = "$orderType $bearAmount B @ Rp $currentRate (Nilai: Rp $idrAmount)",
      type = "EXCHANGE"
    )

    persistState()
    Result.success(order)
  }

  suspend fun triggerMarketRateUpdate(customEvent: String? = null): ExchangeRateRecord = mutex.withLock {
    val current = _centralBankSettings.value.currentRateIdr
    val randFactor = (Math.random() - 0.48) * 0.05
    var newRate = current * (1.0 + randFactor)
    newRate = newRate.coerceIn(_centralBankSettings.value.minRateClamp, _centralBankSettings.value.maxRateClamp)
    val rounded = Math.round(newRate * 100.0) / 100.0
    val chgPct = Math.round(((rounded - current) / current) * 10000.0) / 100.0

    val eventName = customEvent ?: when {
      chgPct > 1.0 -> "Lonjakan Permintaan Likuiditas"
      chgPct < -1.0 -> "Koreksi Pasokan Pasar"
      else -> "Stabilisasi Pasar Bank Sentral"
    }

    val record = ExchangeRateRecord(
      rateIdr = rounded,
      previousRate = current,
      changePercent = chgPct,
      marketEvent = eventName
    )

    _exchangeRates.value = _exchangeRates.value + record
    _centralBankSettings.value = _centralBankSettings.value.copy(
      currentRateIdr = rounded,
      updatedAt = System.currentTimeMillis()
    )

    persistState()
    record
  }

  // --- Banking & Loan Facility (Persistent Bank Reserves) ---

  suspend fun submitLoanApplication(
    productId: String,
    requestedAmount: Long
  ): Result<LoanApplication> = mutex.withLock {
    val user = _currentUser.value ?: return Result.failure(IllegalStateException("Not authenticated"))
    val product = _loanProducts.value.find { it.id == productId }
      ?: return Result.failure(IllegalArgumentException("Loan product not found"))

    if (requestedAmount < product.minAmount || requestedAmount > product.maxAmount) {
      return Result.failure(
        IllegalArgumentException("Requested amount must be between ${product.minAmount} and ${product.maxAmount} B")
      )
    }

    // Check if user already has an active or in-progress loan
    val hasActive = _activeLoans.value.any { it.userId == user.id && it.status == "ACTIVE" }
    if (hasActive) {
      return Result.failure(IllegalStateException("Anda masih memiliki fasilitas pinjaman yang sedang aktif. Lunasi terlebih dahulu."))
    }

    val app = LoanApplication(
      userId = user.id,
      productId = product.id,
      productName = product.name,
      requestedAmount = requestedAmount,
      interestRatePercent = product.interestRatePercent,
      tenorDays = product.tenorDays,
      currentStage = 1,
      stageStatus = "IN_PROGRESS",
      eligibilityStatus = "Memverifikasi data komunitas & kepatuhan limit...",
      creditScore = user.creditScore,
      liquidityStatus = "Menunggu verifikasi likuiditas cadangan bank...",
      availableBankLiquidity = _systemAccounts.value[SupabaseConfig.ACC_BANK_LENDING_POOL]?.balance ?: 0L
    )

    _loanApplications.value = listOf(app) + _loanApplications.value
    persistState()
    Result.success(app)
  }

  suspend fun processLoanStage(applicationId: String): Result<LoanApplication> = mutex.withLock {
    val currentApp = _loanApplications.value.find { it.id == applicationId }
      ?: return Result.failure(IllegalArgumentException("Application not found"))

    val lendingPool = _systemAccounts.value[SupabaseConfig.ACC_BANK_LENDING_POOL]?.balance ?: 0L

    val updatedApp = when (currentApp.currentStage) {
      1 -> {
        currentApp.copy(
          currentStage = 2,
          eligibilityStatus = "Kriteria kelayakan akun komunitas terpenuhi",
          updatedAt = System.currentTimeMillis()
        )
      }
      2 -> {
        currentApp.copy(
          currentStage = 3,
          liquidityStatus = "Memeriksa saldo pool pinjaman bank...",
          availableBankLiquidity = lendingPool,
          updatedAt = System.currentTimeMillis()
        )
      }
      3 -> {
        if (lendingPool < currentApp.requestedAmount) {
          currentApp.copy(
            currentStage = 4,
            stageStatus = "REJECTED",
            liquidityStatus = "Likuiditas cadangan pinjaman bank terbatas",
            decisionReason = "Pool pinjaman bank saat ini memiliki $lendingPool B, tidak cukup untuk ${currentApp.requestedAmount} B yang diajukan.",
            updatedAt = System.currentTimeMillis()
          )
        } else {
          val interest = Math.round(currentApp.requestedAmount * (currentApp.interestRatePercent / 100.0))
          val total = currentApp.requestedAmount + interest
          val dueAt = System.currentTimeMillis() + (currentApp.tenorDays * 24 * 3600 * 1000L)

          currentApp.copy(
            currentStage = 4,
            stageStatus = "APPROVED",
            liquidityStatus = "Cadangan likuiditas bank terverifikasi ($lendingPool B tersedia)",
            approvedPrincipal = currentApp.requestedAmount,
            interestAmount = interest,
            totalRepayment = total,
            dueAt = dueAt,
            decisionReason = "Skor kredit dan cadangan bank memenuhi syarat. Penawaran pinjaman siap dicairkan.",
            updatedAt = System.currentTimeMillis()
          )
        }
      }
      else -> currentApp
    }

    _loanApplications.value = _loanApplications.value.map {
      if (it.id == applicationId) updatedApp else it
    }

    if (updatedApp.stageStatus == "APPROVED") {
      addNotification(
        userId = updatedApp.userId,
        title = "Pinjaman Disetujui!",
        message = "Pengajuan pinjaman ${updatedApp.approvedPrincipal} B (${updatedApp.productName}) telah disetujui. Silakan terima penawaran untuk pencairan.",
        type = "LOAN"
      )
    } else if (updatedApp.stageStatus == "REJECTED") {
      addNotification(
        userId = updatedApp.userId,
        title = "Pembaruan Pinjaman",
        message = "Pengajuan pinjaman tidak dapat disetujui: ${updatedApp.decisionReason}",
        type = "LOAN"
      )
    }

    persistState()
    Result.success(updatedApp)
  }

  suspend fun advanceLoanStage(applicationId: String): Result<LoanApplication> = processLoanStage(applicationId)

  suspend fun disburseLoan(applicationId: String): Result<ActiveLoan> = mutex.withLock {
    val user = _currentUser.value ?: return Result.failure(IllegalStateException("Not authenticated"))
    val app = _loanApplications.value.find { it.id == applicationId && it.userId == user.id }
      ?: return Result.failure(IllegalArgumentException("Application not found"))

    if (app.stageStatus != "APPROVED") {
      return Result.failure(IllegalStateException("Pengajuan belum dalam status APPROVED"))
    }

    val lendingPool = _systemAccounts.value[SupabaseConfig.ACC_BANK_LENDING_POOL]
      ?: return Result.failure(IllegalStateException("Akun lending pool bank tidak ditemukan"))

    if (lendingPool.balance < app.approvedPrincipal) {
      return Result.failure(IllegalStateException("Cadangan likuiditas bank tidak mencukupi untuk dicairkan."))
    }

    // Atomic debit bank lending pool (PERMANENTLY HELD), credit user wallet
    val updatedPool = lendingPool.copy(balance = lendingPool.balance - app.approvedPrincipal)
    val userWallet = getWallet(user.id)
    val updatedWallet = userWallet.copy(
      balance = userWallet.balance + app.approvedPrincipal,
      updatedAt = System.currentTimeMillis()
    )

    _systemAccounts.value = _systemAccounts.value + (SupabaseConfig.ACC_BANK_LENDING_POOL to updatedPool)
    _wallets.value = _wallets.value + (user.id to updatedWallet)

    val updatedApp = app.copy(stageStatus = "ACCEPTED", updatedAt = System.currentTimeMillis())
    _loanApplications.value = _loanApplications.value.map { if (it.id == applicationId) updatedApp else it }

    val activeLoan = ActiveLoan(
      userId = user.id,
      applicationId = app.id,
      productName = app.productName,
      principalAmount = app.approvedPrincipal,
      interestAmount = app.interestAmount,
      totalAmount = app.totalRepayment,
      remainingAmount = app.totalRepayment,
      tenorDays = app.tenorDays,
      status = "ACTIVE",
      dueAt = app.dueAt,
      disbursedAt = System.currentTimeMillis()
    )
    _activeLoans.value = listOf(activeLoan) + _activeLoans.value

    val tx = LedgerTransaction(
      idempotencyKey = "disburse_${app.id}",
      transactionType = TransactionType.LOAN_DISBURSEMENT.name,
      amount = app.approvedPrincipal,
      sourceAccount = SupabaseConfig.ACC_BANK_LENDING_POOL,
      destinationAccount = user.id,
      note = "Pencairan fasilitas pinjaman ${app.productName}",
      status = "COMPLETED"
    )
    _transactions.value = listOf(tx) + _transactions.value

    addNotification(
      userId = user.id,
      title = "Dana Pinjaman Dicairkan",
      message = "${app.approvedPrincipal} B telah dikreditkan ke wallet Anda dari pool likuiditas bank.",
      type = "LOAN"
    )

    persistState()
    Result.success(activeLoan)
  }

  suspend fun repayLoan(loanId: String, amount: Long): Result<ActiveLoan> = mutex.withLock {
    val user = _currentUser.value ?: return Result.failure(IllegalStateException("Not authenticated"))
    val loan = _activeLoans.value.find { it.id == loanId && it.userId == user.id }
      ?: return Result.failure(IllegalArgumentException("Pinjaman tidak ditemukan"))

    if (loan.status != "ACTIVE") {
      return Result.failure(IllegalStateException("Pinjaman tidak sedang berstatus aktif"))
    }

    if (amount <= 0) {
      return Result.failure(IllegalArgumentException("Nominal pelunasan harus lebih dari 0"))
    }

    val userWallet = getWallet(user.id)
    if (userWallet.balance < amount) {
      return Result.failure(IllegalStateException("Saldo BEAR Anda tidak cukup untuk membayar pinjaman (${userWallet.balance} B)"))
    }

    val payAmount = amount.coerceAtMost(loan.remainingAmount)
    val principalRatio = if (loan.totalAmount > 0) loan.principalAmount.toDouble() / loan.totalAmount.toDouble() else 1.0
    val principalPortion = Math.round(payAmount * principalRatio)
    val interestPortion = payAmount - principalPortion

    // Deduct user wallet
    val updatedUserWallet = userWallet.copy(
      balance = userWallet.balance - payAmount,
      updatedAt = System.currentTimeMillis()
    )

    // Credit bank lending pool (principal) & interest account (interest)
    val lendingPool = _systemAccounts.value[SupabaseConfig.ACC_BANK_LENDING_POOL]
    val interestAcc = _systemAccounts.value[SupabaseConfig.ACC_BANK_INTEREST_INCOME]

    val updatedLendingPool = lendingPool?.copy(balance = lendingPool.balance + principalPortion)
    val updatedInterestAcc = interestAcc?.copy(balance = interestAcc.balance + interestPortion)

    _wallets.value = _wallets.value + (user.id to updatedUserWallet)
    if (updatedLendingPool != null) {
      _systemAccounts.value = _systemAccounts.value + (SupabaseConfig.ACC_BANK_LENDING_POOL to updatedLendingPool)
    }
    if (updatedInterestAcc != null) {
      _systemAccounts.value = _systemAccounts.value + (SupabaseConfig.ACC_BANK_INTEREST_INCOME to updatedInterestAcc)
    }

    val newRemaining = loan.remainingAmount - payAmount
    val isFullyPaid = newRemaining <= 0
    val updatedLoan = loan.copy(
      remainingAmount = newRemaining,
      status = if (isFullyPaid) "PAID" else "ACTIVE"
    )

    _activeLoans.value = _activeLoans.value.map { if (it.id == loanId) updatedLoan else it }

    val tx = LedgerTransaction(
      idempotencyKey = "repay_${loan.id}_${System.currentTimeMillis()}",
      transactionType = TransactionType.LOAN_REPAYMENT.name,
      amount = payAmount,
      sourceAccount = user.id,
      destinationAccount = SupabaseConfig.ACC_BANK_LENDING_POOL,
      note = "Pelunasan pinjaman ${loan.productName}${if (isFullyPaid) " (Lunas)" else ""}",
      status = "COMPLETED"
    )
    _transactions.value = listOf(tx) + _transactions.value

    addNotification(
      userId = user.id,
      title = if (isFullyPaid) "Pinjaman Lunas!" else "Pembayaran Pinjaman Diterima",
      message = "Membayar $payAmount B. Sisa pinjaman: $newRemaining B.",
      type = "LOAN"
    )

    persistState()
    Result.success(updatedLoan)
  }

  // --- Central Bank Admin Operations ---

  suspend fun updateCentralBankSettings(
    isMarketPaused: Boolean,
    updateIntervalHours: Int
  ): Result<CentralBankSettings> = mutex.withLock {
    val actor = _currentUser.value?.email ?: "ADMIN"
    val before = "Paused: ${_centralBankSettings.value.isMarketPaused}, Interval: ${_centralBankSettings.value.updateIntervalHours}h"
    val updated = _centralBankSettings.value.copy(
      isMarketPaused = isMarketPaused,
      updateIntervalHours = updateIntervalHours,
      updatedAt = System.currentTimeMillis()
    )
    _centralBankSettings.value = updated
    val after = "Paused: $isMarketPaused, Interval: ${updateIntervalHours}h"

    addAuditLog(
      actor = actor,
      action = "UPDATE_SETTINGS",
      target = "CENTRAL_BANK_SETTINGS",
      before = before,
      after = after
    )

    persistState()
    Result.success(updated)
  }

  // --- Helpers ---

  private fun addNotification(userId: String, title: String, message: String, type: String) {
    val item = NotificationItem(
      userId = userId,
      title = title,
      message = message,
      type = type
    )
    _notifications.value = listOf(item) + _notifications.value
  }

  private fun addAuditLog(actor: String, action: String, target: String, before: String, after: String) {
    val log = AuditLog(
      actorEmail = actor,
      action = action,
      targetEntity = target,
      beforeValue = before,
      afterValue = after
    )
    _auditLogs.value = listOf(log) + _auditLogs.value
  }
}

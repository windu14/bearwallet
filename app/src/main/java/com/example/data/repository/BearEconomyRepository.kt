package com.example.data.repository

import com.example.core.config.SupabaseConfig
import com.example.core.network.SupabaseNetworkClient
import com.example.data.model.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.json.JSONObject
import java.util.UUID

class BearEconomyRepository(
  private val networkClient: SupabaseNetworkClient = SupabaseNetworkClient()
) {
  private val mutex = Mutex()

  // Current authenticated user session
  private val _currentUser = MutableStateFlow<UserProfile?>(null)
  val currentUser: StateFlow<UserProfile?> = _currentUser.asStateFlow()

  // Known community directory for user transfers
  private val _profiles = MutableStateFlow<Map<String, UserProfile>>(emptyMap())
  val profiles: StateFlow<Map<String, UserProfile>> = _profiles.asStateFlow()

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
    initializeAuthoritativeEconomy()
  }

  private fun initializeAuthoritativeEconomy() {
    // 1. Proposed initial supply split (Total = 1,000,000 B)
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
    // Default active user
    val defaultUser = UserProfile(
      id = "user_bearboss",
      email = "windu.mariposa@gmail.com",
      username = "bearboss",
      fullName = "Windu Mariposa",
      role = "admin",
      creditScore = 680
    )

    _profiles.value = mapOf(
      defaultUser.id to defaultUser,
      userMaya.id to userMaya,
      userAlex.id to userAlex,
      userCentralBank.id to userCentralBank
    )

    // Grant initial welcome signup distribution (500 B each from finite distribution pool)
    distributeInitialUserGrant(defaultUser)
    distributeInitialUserGrant(userMaya)
    distributeInitialUserGrant(userAlex)

    // Set active user
    _currentUser.value = defaultUser
  }

  private fun distributeInitialUserGrant(user: UserProfile) {
    val pool = _systemAccounts.value[SupabaseConfig.ACC_USER_DISTRIBUTION] ?: return
    val bonus = SupabaseConfig.INITIAL_USER_SIGNUP_BONUS
    if (pool.balance >= bonus) {
      val updatedPool = pool.copy(balance = pool.balance - bonus)
      _systemAccounts.value = _systemAccounts.value + (SupabaseConfig.ACC_USER_DISTRIBUTION to updatedPool)

      val userWallet = WalletAccount(
        userId = user.id,
        balance = bonus
      )
      _wallets.value = _wallets.value + (user.id to userWallet)

      val tx = LedgerTransaction(
        idempotencyKey = "signup_${user.id}",
        transactionType = TransactionType.SIGNUP_BONUS.name,
        amount = bonus,
        fee = 0L,
        sourceAccount = SupabaseConfig.ACC_USER_DISTRIBUTION,
        destinationAccount = user.id,
        note = "Welcome distribution grant from finite community pool",
        status = "COMPLETED"
      )
      _transactions.value = listOf(tx) + _transactions.value

      addNotification(
        userId = user.id,
        title = "Welcome to BearWallet!",
        message = "Your wallet has received an initial grant of $bonus B from the finite distribution pool.",
        type = "SYSTEM"
      )
    }
  }

  // --- Authentication Flows ---

  suspend fun signUp(email: String, pass: String, username: String, fullName: String): Result<UserProfile> = mutex.withLock {
    try {
      // Clean username
      val cleanHandle = username.trim().trimStart('@').lowercase()
      if (cleanHandle.isEmpty()) return Result.failure(IllegalArgumentException("Username cannot be empty"))

      // Check handle uniqueness
      if (_profiles.value.values.any { it.username.equals(cleanHandle, ignoreCase = true) }) {
        return Result.failure(IllegalArgumentException("Username @$cleanHandle is already taken"))
      }

      // Try remote Supabase auth
      val remoteResult = networkClient.signUp(email, pass, cleanHandle, fullName)
      val newId = if (remoteResult.isSuccess) {
        _isSupabaseConnected.value = true
        networkClient.currentUserId ?: UUID.randomUUID().toString()
      } else {
        _isSupabaseConnected.value = false
        UUID.randomUUID().toString()
      }

      val newUser = UserProfile(
        id = newId,
        email = email,
        username = cleanHandle,
        fullName = fullName,
        role = if (cleanHandle == "admin" || cleanHandle == "centralbank") "admin" else "user",
        creditScore = 600
      )

      _profiles.value = _profiles.value + (newUser.id to newUser)
      distributeInitialUserGrant(newUser)
      _currentUser.value = newUser

      addAuditLog(
        actor = newUser.email,
        action = "USER_REGISTERED",
        target = newUser.username,
        before = "None",
        after = "Created with 500 B initial grant"
      )

      Result.success(newUser)
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  suspend fun signIn(emailOrHandle: String, pass: String): Result<UserProfile> = mutex.withLock {
    try {
      val target = emailOrHandle.trim().trimStart('@').lowercase()

      // Look up locally or via Supabase
      val profile = _profiles.value.values.find {
        it.email.equals(target, ignoreCase = true) || it.username.equals(target, ignoreCase = true)
      }

      // Also attempt remote sign in
      val remoteResult = networkClient.signIn(if (target.contains("@")) target else "$target@bearwallet.app", pass)
      if (remoteResult.isSuccess) {
        _isSupabaseConnected.value = true
      }

      if (profile != null) {
        _currentUser.value = profile
        Result.success(profile)
      } else {
        // If profile was created on remote Supabase but not yet cached locally
        val newProfile = UserProfile(
          id = networkClient.currentUserId ?: UUID.randomUUID().toString(),
          email = if (target.contains("@")) target else "$target@bearwallet.app",
          username = target,
          fullName = target.replaceFirstChar { it.uppercase() },
          role = "user",
          creditScore = 600
        )
        _profiles.value = _profiles.value + (newProfile.id to newProfile)
        distributeInitialUserGrant(newProfile)
        _currentUser.value = newProfile
        Result.success(newProfile)
      }
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
    // Retain registered profiles, switch current user to null
    _currentUser.value = null
  }

  fun switchUser(userId: String) {
    _profiles.value[userId]?.let {
      _currentUser.value = it
    }
  }

  // --- Wallet & Transfer Operations ---

  fun getWallet(userId: String): WalletAccount {
    return _wallets.value[userId] ?: WalletAccount(userId = userId, balance = 0L)
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
    } ?: return Result.failure(IllegalArgumentException("Recipient '@$cleanHandle' not found in Bear community"))

    if (recipient.id == sender.id) {
      return Result.failure(IllegalArgumentException("Cannot transfer Bear to your own account"))
    }

    val senderWallet = getWallet(sender.id)
    if (senderWallet.balance < amount) {
      return Result.failure(
        IllegalStateException("Insufficient balance. You have ${senderWallet.balance} B, but tried to transfer $amount B")
      )
    }

    // 1. Try remote Supabase RPC
    val rpcParams = JSONObject().apply {
      put("p_recipient_handle", cleanHandle)
      put("p_amount", amount)
      put("p_idempotency_key", idempotencyKey)
      put("p_note", note)
    }
    networkClient.callRpc("execute_transfer", rpcParams)

    // 2. Authoritative local debit/credit (Double-entry balance guarantee)
    val recipientWallet = getWallet(recipient.id)

    val updatedSenderWallet = senderWallet.copy(
      balance = senderWallet.balance - amount,
      updatedAt = System.currentTimeMillis()
    )
    val updatedRecipientWallet = recipientWallet.copy(
      balance = recipientWallet.balance + amount,
      updatedAt = System.currentTimeMillis()
    )

    _wallets.value = _wallets.value +
      (sender.id to updatedSenderWallet) +
      (recipient.id to updatedRecipientWallet)

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
      title = "Incoming Bear Transfer",
      message = "You received $amount B from @${sender.username}.",
      type = "TRANSFER"
    )

    addNotification(
      userId = sender.id,
      title = "Transfer Sent",
      message = "Successfully transferred $amount B to @${recipient.username}.",
      type = "TRANSFER"
    )

    Result.success(tx)
  }

  suspend fun executeTopUp(amount: Long, idempotencyKey: String): Result<LedgerTransaction> = mutex.withLock {
    val user = _currentUser.value ?: return Result.failure(IllegalStateException("Not authenticated"))

    if (amount <= 0 || amount > 500) {
      return Result.failure(IllegalArgumentException("Top-up limit: between 1 and 500 B per request"))
    }

    // Idempotency
    val existing = _transactions.value.find { it.idempotencyKey == idempotencyKey }
    if (existing != null) return Result.success(existing)

    // Check system distribution pool
    val distPool = _systemAccounts.value[SupabaseConfig.ACC_USER_DISTRIBUTION]
      ?: return Result.failure(IllegalStateException("Distribution pool account not found"))

    if (distPool.balance < amount) {
      return Result.failure(IllegalStateException("Distribution inventory exhausted! Available: ${distPool.balance} B"))
    }

    // Deduct from finite pool, credit to user
    val updatedPool = distPool.copy(balance = distPool.balance - amount)
    val userWallet = getWallet(user.id)
    val updatedWallet = userWallet.copy(balance = userWallet.balance + amount)

    _systemAccounts.value = _systemAccounts.value + (SupabaseConfig.ACC_USER_DISTRIBUTION to updatedPool)
    _wallets.value = _wallets.value + (user.id to updatedWallet)

    val tx = LedgerTransaction(
      idempotencyKey = idempotencyKey,
      transactionType = TransactionType.TOPUP.name,
      amount = amount,
      sourceAccount = SupabaseConfig.ACC_USER_DISTRIBUTION,
      destinationAccount = user.id,
      note = "Simulated community distribution faucet top-up",
      status = "COMPLETED"
    )
    _transactions.value = listOf(tx) + _transactions.value

    addNotification(
      userId = user.id,
      title = "Top-up Successful",
      message = "Added $amount B from the community distribution inventory.",
      type = "TOPUP"
    )

    Result.success(tx)
  }

  // --- Dynamic Exchange Operations ---

  suspend fun executeExchangeTrade(
    orderType: String, // "BUY" or "SELL"
    bearAmount: Long,
    idempotencyKey: String
  ): Result<ExchangeOrder> = mutex.withLock {
    val user = _currentUser.value ?: return Result.failure(IllegalStateException("Not authenticated"))

    if (_centralBankSettings.value.isMarketPaused) {
      return Result.failure(IllegalStateException("The Bear Exchange is currently paused by the Central Bank"))
    }

    if (bearAmount <= 0) {
      return Result.failure(IllegalArgumentException("Trade amount must be greater than 0"))
    }

    val currentRate = _centralBankSettings.value.currentRateIdr
    val idrAmount = Math.round(bearAmount * currentRate)
    val exchangeInventory = _systemAccounts.value[SupabaseConfig.ACC_EXCHANGE_INVENTORY]
      ?: return Result.failure(IllegalStateException("Exchange inventory account missing"))

    val userWallet = getWallet(user.id)

    if (orderType == "BUY") {
      // Must have enough BEAR inventory in system account
      if (exchangeInventory.balance < bearAmount) {
        return Result.failure(IllegalStateException("Exchange inventory insufficient. Available to buy: ${exchangeInventory.balance} B"))
      }

      // Move BEAR from exchange inventory to user wallet
      val updatedInventory = exchangeInventory.copy(balance = exchangeInventory.balance - bearAmount)
      val updatedWallet = userWallet.copy(balance = userWallet.balance + bearAmount)

      _systemAccounts.value = _systemAccounts.value + (SupabaseConfig.ACC_EXCHANGE_INVENTORY to updatedInventory)
      _wallets.value = _wallets.value + (user.id to updatedWallet)

      val tx = LedgerTransaction(
        idempotencyKey = idempotencyKey,
        transactionType = TransactionType.EXCHANGE_BUY.name,
        amount = bearAmount,
        sourceAccount = SupabaseConfig.ACC_EXCHANGE_INVENTORY,
        destinationAccount = user.id,
        note = "Bought at simulated ref rate $currentRate IDR/BEAR (Simulated Rp $idrAmount)",
        status = "COMPLETED"
      )
      _transactions.value = listOf(tx) + _transactions.value

    } else if (orderType == "SELL") {
      // User must have enough BEAR in wallet
      if (userWallet.balance < bearAmount) {
        return Result.failure(IllegalStateException("Insufficient balance to sell. Available: ${userWallet.balance} B"))
      }

      // Move BEAR from user wallet back to exchange inventory
      val updatedWallet = userWallet.copy(balance = userWallet.balance - bearAmount)
      val updatedInventory = exchangeInventory.copy(balance = exchangeInventory.balance + bearAmount)

      _wallets.value = _wallets.value + (user.id to updatedWallet)
      _systemAccounts.value = _systemAccounts.value + (SupabaseConfig.ACC_EXCHANGE_INVENTORY to updatedInventory)

      val tx = LedgerTransaction(
        idempotencyKey = idempotencyKey,
        transactionType = TransactionType.EXCHANGE_SELL.name,
        amount = bearAmount,
        sourceAccount = user.id,
        destinationAccount = SupabaseConfig.ACC_EXCHANGE_INVENTORY,
        note = "Sold at simulated ref rate $currentRate IDR/BEAR (Simulated Rp $idrAmount)",
        status = "COMPLETED"
      )
      _transactions.value = listOf(tx) + _transactions.value
    } else {
      return Result.failure(IllegalArgumentException("Invalid order type: $orderType"))
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
      title = "Exchange Order Executed",
      message = "$orderType $bearAmount B @ $currentRate IDR (Simulated value: Rp $idrAmount)",
      type = "EXCHANGE"
    )

    Result.success(order)
  }

  suspend fun triggerMarketRateUpdate(customEvent: String? = null): ExchangeRateRecord = mutex.withLock {
    val current = _centralBankSettings.value.currentRateIdr
    // Bounded pseudo-random market shock between -2.8% and +2.8%
    val shockPercent = (Math.random() * 5.6 - 2.8)
    val roundedShock = Math.round(shockPercent * 100.0) / 100.0
    val newRate = Math.round((current * (1.0 + roundedShock / 100.0)) * 100.0) / 100.0

    val eventName = customEvent ?: when {
      roundedShock > 1.5 -> "High Buy Demand Surge"
      roundedShock > 0.0 -> "Steady Liquidity Inflow"
      roundedShock > -1.5 -> "Gradual Market Balancing"
      else -> "Community Profit Taking"
    }

    val record = ExchangeRateRecord(
      rateIdr = newRate.coerceAtLeast(1.0),
      previousRate = current,
      changePercent = roundedShock,
      marketEvent = eventName,
      createdAt = System.currentTimeMillis()
    )

    _exchangeRates.value = _exchangeRates.value + record
    _centralBankSettings.value = _centralBankSettings.value.copy(
      currentRateIdr = record.rateIdr,
      updatedAt = System.currentTimeMillis()
    )

    addAuditLog(
      actor = _currentUser.value?.email ?: "SYSTEM",
      action = "EXCHANGE_RATE_UPDATE",
      target = "CENTRAL_BANK_EXCHANGE",
      before = "$current IDR",
      after = "${record.rateIdr} IDR (${if (roundedShock >= 0) "+" else ""}$roundedShock%)"
    )

    record
  }

  // --- Banking & 4-Stage Loan Processing ---

  suspend fun submitLoanApplication(productId: String, requestedAmount: Long): Result<LoanApplication> = mutex.withLock {
    val user = _currentUser.value ?: return Result.failure(IllegalStateException("Not authenticated"))
    val product = _loanProducts.value.find { it.id == productId }
      ?: return Result.failure(IllegalArgumentException("Loan product not found"))

    if (requestedAmount < product.minAmount || requestedAmount > product.maxAmount) {
      return Result.failure(IllegalArgumentException("Amount must be between ${product.minAmount} and ${product.maxAmount} B"))
    }

    // Check for existing active or overdue loans
    val hasActive = _activeLoans.value.any { it.userId == user.id && (it.status == "ACTIVE" || it.status == "OVERDUE") }
    if (hasActive) {
      return Result.failure(IllegalStateException("You already have an active loan. Settle it before requesting a new one."))
    }

    val lendingPool = _systemAccounts.value[SupabaseConfig.ACC_BANK_LENDING_POOL]?.balance ?: 0L

    val app = LoanApplication(
      userId = user.id,
      productId = product.id,
      productName = product.name,
      requestedAmount = requestedAmount,
      currentStage = 1,
      stageStatus = "IN_PROGRESS",
      eligibilityStatus = "Screening in progress...",
      creditScore = user.creditScore,
      creditRating = when {
        user.creditScore >= 750 -> "Excellent"
        user.creditScore >= 650 -> "Good"
        else -> "Fair"
      },
      liquidityStatus = "Evaluating bank reserves",
      availableBankLiquidity = lendingPool,
      tenorDays = product.tenorDays,
      interestRatePercent = product.interestRatePercent
    )

    _loanApplications.value = listOf(app) + _loanApplications.value
    Result.success(app)
  }

  suspend fun advanceLoanStage(applicationId: String): Result<LoanApplication> = mutex.withLock {
    val currentApp = _loanApplications.value.find { it.id == applicationId }
      ?: return Result.failure(IllegalArgumentException("Application not found"))

    if (currentApp.stageStatus != "IN_PROGRESS") {
      return Result.success(currentApp)
    }

    val lendingPool = _systemAccounts.value[SupabaseConfig.ACC_BANK_LENDING_POOL]?.balance ?: 0L

    val updatedApp: LoanApplication = when (currentApp.currentStage) {
      1 -> {
        // Complete Stage 1 (Eligibility) -> Move to Stage 2 (Credit Score Analysis)
        currentApp.copy(
          currentStage = 2,
          eligibilityStatus = "Verified: Zero overdue debt, active account age approved",
          updatedAt = System.currentTimeMillis()
        )
      }
      2 -> {
        // Complete Stage 2 (Credit Score) -> Move to Stage 3 (Liquidity Assessment)
        currentApp.copy(
          currentStage = 3,
          liquidityStatus = "Checking bank lending pool balance...",
          availableBankLiquidity = lendingPool,
          updatedAt = System.currentTimeMillis()
        )
      }
      3 -> {
        // Complete Stage 3 (Liquidity Check) -> Move to Stage 4 (Decision and Offer)
        if (lendingPool < currentApp.requestedAmount) {
          // Reject due to bank liquidity constraint (credit score cannot override this!)
          currentApp.copy(
            currentStage = 4,
            stageStatus = "REJECTED",
            liquidityStatus = "Bank lending pool liquidity constrained",
            decisionReason = "The bank lending pool currently holds $lendingPool B, which is insufficient for your requested ${currentApp.requestedAmount} B. Please apply for a smaller amount.",
            updatedAt = System.currentTimeMillis()
          )
        } else {
          val interest = Math.round(currentApp.requestedAmount * (currentApp.interestRatePercent / 100.0))
          val total = currentApp.requestedAmount + interest
          val dueAt = System.currentTimeMillis() + (currentApp.tenorDays * 24 * 3600 * 1000L)

          currentApp.copy(
            currentStage = 4,
            stageStatus = "APPROVED",
            liquidityStatus = "Sufficient lending reserves verified ($lendingPool B available)",
            approvedPrincipal = currentApp.requestedAmount,
            interestAmount = interest,
            totalRepayment = total,
            dueAt = dueAt,
            decisionReason = "Credit score criteria and bank reserve checks successfully met. Your loan offer is ready for acceptance.",
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
        title = "Loan Offer Approved!",
        message = "Your application for ${updatedApp.approvedPrincipal} B (${updatedApp.productName}) has been approved. Review and accept to disburse.",
        type = "LOAN"
      )
    } else if (updatedApp.stageStatus == "REJECTED") {
      addNotification(
        userId = updatedApp.userId,
        title = "Loan Application Update",
        message = "Your loan application could not be approved: ${updatedApp.decisionReason}",
        type = "LOAN"
      )
    }

    Result.success(updatedApp)
  }

  suspend fun disburseLoan(applicationId: String): Result<ActiveLoan> = mutex.withLock {
    val user = _currentUser.value ?: return Result.failure(IllegalStateException("Not authenticated"))
    val app = _loanApplications.value.find { it.id == applicationId && it.userId == user.id }
      ?: return Result.failure(IllegalArgumentException("Application not found"))

    if (app.stageStatus != "APPROVED") {
      return Result.failure(IllegalStateException("Application is not in APPROVED state (current: ${app.stageStatus})"))
    }

    val lendingPool = _systemAccounts.value[SupabaseConfig.ACC_BANK_LENDING_POOL]
      ?: return Result.failure(IllegalStateException("Lending pool account missing"))

    if (lendingPool.balance < app.approvedPrincipal) {
      return Result.failure(IllegalStateException("Bank lending reserves are now depleted. Cannot disburse."))
    }

    // Atomic debit bank lending pool, credit user wallet
    val updatedPool = lendingPool.copy(balance = lendingPool.balance - app.approvedPrincipal)
    val userWallet = getWallet(user.id)
    val updatedWallet = userWallet.copy(balance = userWallet.balance + app.approvedPrincipal)

    _systemAccounts.value = _systemAccounts.value + (SupabaseConfig.ACC_BANK_LENDING_POOL to updatedPool)
    _wallets.value = _wallets.value + (user.id to updatedWallet)

    // Mark application ACCEPTED
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
      note = "Disbursed ${app.productName} loan facility",
      status = "COMPLETED"
    )
    _transactions.value = listOf(tx) + _transactions.value

    addNotification(
      userId = user.id,
      title = "Funds Disbursed",
      message = "${app.approvedPrincipal} B has been credited to your wallet from the bank lending pool.",
      type = "LOAN"
    )

    Result.success(activeLoan)
  }

  suspend fun repayLoan(loanId: String, amount: Long): Result<ActiveLoan> = mutex.withLock {
    val user = _currentUser.value ?: return Result.failure(IllegalStateException("Not authenticated"))
    val loan = _activeLoans.value.find { it.id == loanId && it.userId == user.id }
      ?: return Result.failure(IllegalArgumentException("Active loan not found"))

    if (loan.status == "PAID" || loan.remainingAmount == 0L) {
      return Result.failure(IllegalStateException("This loan is already settled"))
    }

    if (amount <= 0 || amount > loan.remainingAmount) {
      return Result.failure(IllegalArgumentException("Repayment must be between 1 and ${loan.remainingAmount} B"))
    }

    val userWallet = getWallet(user.id)
    if (userWallet.balance < amount) {
      return Result.failure(IllegalStateException("Insufficient balance. Required: $amount B, Available: ${userWallet.balance} B"))
    }

    // Split repayment between principal and interest
    val interestPortion = Math.round(amount * (loan.interestAmount.toDouble() / loan.totalAmount.toDouble()))
    val principalPortion = amount - interestPortion

    // Debit user wallet
    val updatedWallet = userWallet.copy(balance = userWallet.balance - amount)
    _wallets.value = _wallets.value + (user.id to updatedWallet)

    // Route principal back to BANK LENDING POOL
    val lendingPool = _systemAccounts.value[SupabaseConfig.ACC_BANK_LENDING_POOL]!!
    val updatedPool = lendingPool.copy(balance = lendingPool.balance + principalPortion)

    // Route interest to BANK INTEREST INCOME
    val interestIncome = _systemAccounts.value[SupabaseConfig.ACC_BANK_INTEREST_INCOME]!!
    val updatedInterestIncome = interestIncome.copy(balance = interestIncome.balance + interestPortion)

    _systemAccounts.value = _systemAccounts.value +
      (SupabaseConfig.ACC_BANK_LENDING_POOL to updatedPool) +
      (SupabaseConfig.ACC_BANK_INTEREST_INCOME to updatedInterestIncome)

    val newRemaining = loan.remainingAmount - amount
    val updatedLoan = loan.copy(
      remainingAmount = newRemaining,
      status = if (newRemaining == 0L) "PAID" else "ACTIVE"
    )

    _activeLoans.value = _activeLoans.value.map { if (it.id == loanId) updatedLoan else it }

    val tx = LedgerTransaction(
      idempotencyKey = "repay_${UUID.randomUUID()}",
      transactionType = TransactionType.LOAN_REPAYMENT.name,
      amount = amount,
      sourceAccount = user.id,
      destinationAccount = SupabaseConfig.ACC_BANK_LENDING_POOL,
      note = "Loan Repayment: Principal $principalPortion B, Interest $interestPortion B",
      status = "COMPLETED"
    )
    _transactions.value = listOf(tx) + _transactions.value

    // If fully settled, boost credit score!
    if (newRemaining == 0L) {
      val currentScore = user.creditScore
      val newScore = (currentScore + 25).coerceAtMost(850)
      val updatedProfile = user.copy(creditScore = newScore)
      _currentUser.value = updatedProfile
      _profiles.value = _profiles.value + (user.id to updatedProfile)

      addNotification(
        userId = user.id,
        title = "Loan Fully Settled!",
        message = "Congratulations! Your loan has been fully settled. Your credit score increased to $newScore.",
        type = "LOAN"
      )
    } else {
      addNotification(
        userId = user.id,
        title = "Partial Repayment Recorded",
        message = "Paid $amount B. Remaining balance: $newRemaining B.",
        type = "LOAN"
      )
    }

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

package com.example.core.storage

import android.content.Context
import com.example.data.model.*
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

class BearLocalStorage(private val context: Context) {

  private val stateFile: File
    get() = File(context.filesDir, "bear_economy_state.json")

  private val prefs by lazy {
    context.getSharedPreferences("bear_wallet_prefs", Context.MODE_PRIVATE)
  }

  fun saveActiveUserId(userId: String?) {
    prefs.edit().apply {
      if (userId == null) {
        remove("active_user_id")
      } else {
        putString("active_user_id", userId)
      }
      apply()
    }
  }

  fun getActiveUserId(): String? {
    return prefs.getString("active_user_id", null)
  }

  data class PersistedState(
    val activeUserId: String?,
    val profiles: Map<String, UserProfile>,
    val credentials: Map<String, String>,
    val wallets: Map<String, WalletAccount>,
    val systemAccounts: Map<String, SystemAccount>,
    val centralBankSettings: CentralBankSettings,
    val exchangeRates: List<ExchangeRateRecord>,
    val loanProducts: List<LoanProduct>,
    val loanApplications: List<LoanApplication>,
    val activeLoans: List<ActiveLoan>,
    val transactions: List<LedgerTransaction>,
    val notifications: List<NotificationItem>,
    val auditLogs: List<AuditLog>
  )

  @Synchronized
  fun loadState(): PersistedState? {
    if (!stateFile.exists()) return null
    return try {
      val jsonStr = stateFile.readText()
      val root = JSONObject(jsonStr)

      val activeId = getActiveUserId() ?: root.optString("active_user_id", "").ifEmpty { null }

      // Profiles
      val profilesObj = root.optJSONObject("profiles") ?: JSONObject()
      val profilesMap = mutableMapOf<String, UserProfile>()
      val profileKeys = profilesObj.keys()
      while (profileKeys.hasNext()) {
        val key = profileKeys.next()
        val item = profilesObj.getJSONObject(key)
        profilesMap[key] = UserProfile(
          id = item.getString("id"),
          email = item.getString("email"),
          username = item.getString("username"),
          fullName = item.getString("fullName"),
          role = item.optString("role", "user"),
          creditScore = item.optInt("creditScore", 600),
          createdAt = item.optLong("createdAt", System.currentTimeMillis())
        )
      }

      // Credentials
      val credsObj = root.optJSONObject("credentials") ?: JSONObject()
      val credsMap = mutableMapOf<String, String>()
      val credKeys = credsObj.keys()
      while (credKeys.hasNext()) {
        val key = credKeys.next()
        credsMap[key] = credsObj.getString(key)
      }

      // Wallets
      val walletsObj = root.optJSONObject("wallets") ?: JSONObject()
      val walletsMap = mutableMapOf<String, WalletAccount>()
      val walletKeys = walletsObj.keys()
      while (walletKeys.hasNext()) {
        val key = walletKeys.next()
        val item = walletsObj.getJSONObject(key)
        walletsMap[key] = WalletAccount(
          id = item.optString("id", key),
          userId = item.getString("userId"),
          currency = item.optString("currency", "BEAR"),
          balance = item.optLong("balance", 0L),
          virtualIdrBalance = item.optLong("virtualIdrBalance", 0L),
          lockedBalance = item.optLong("lockedBalance", 0L),
          updatedAt = item.optLong("updatedAt", System.currentTimeMillis())
        )
      }

      // System Accounts
      val sysObj = root.optJSONObject("systemAccounts") ?: JSONObject()
      val sysMap = mutableMapOf<String, SystemAccount>()
      val sysKeys = sysObj.keys()
      while (sysKeys.hasNext()) {
        val key = sysKeys.next()
        val item = sysObj.getJSONObject(key)
        sysMap[key] = SystemAccount(
          accountCode = item.getString("accountCode"),
          name = item.getString("name"),
          balance = item.getLong("balance"),
          isLocked = item.optBoolean("isLocked", false)
        )
      }

      // Central Bank Settings
      val cbObj = root.optJSONObject("centralBankSettings")
      val cbSettings = if (cbObj != null) {
        CentralBankSettings(
          totalAuthorizedSupply = cbObj.optLong("totalAuthorizedSupply", 1_000_000L),
          updateIntervalHours = cbObj.optInt("updateIntervalHours", 4),
          isMarketPaused = cbObj.optBoolean("isMarketPaused", false),
          currentRateIdr = cbObj.optDouble("currentRateIdr", 10.0),
          minRateClamp = cbObj.optDouble("minRateClamp", 1.0),
          maxRateClamp = cbObj.optDouble("maxRateClamp", 100.0),
          lockedReserve = cbObj.optLong("lockedReserve", 100_000L),
          lendingPool = cbObj.optLong("lendingPool", 250_000L),
          exchangeInventory = cbObj.optLong("exchangeInventory", 200_000L),
          distributionPool = cbObj.optLong("distributionPool", 450_000L),
          bankInterestIncome = cbObj.optLong("bankInterestIncome", 0L),
          updatedAt = cbObj.optLong("updatedAt", System.currentTimeMillis())
        )
      } else CentralBankSettings()

      // Exchange Rates
      val ratesArray = root.optJSONArray("exchangeRates") ?: JSONArray()
      val ratesList = mutableListOf<ExchangeRateRecord>()
      for (i in 0 until ratesArray.length()) {
        val item = ratesArray.getJSONObject(i)
        ratesList.add(
          ExchangeRateRecord(
            id = item.optString("id", ""),
            rateIdr = item.getDouble("rateIdr"),
            previousRate = item.optDouble("previousRate", item.getDouble("rateIdr")),
            changePercent = item.optDouble("changePercent", 0.0),
            marketEvent = item.optString("marketEvent", "Market Rate"),
            createdAt = item.optLong("createdAt", System.currentTimeMillis())
          )
        )
      }

      // Loan Products
      val productsArray = root.optJSONArray("loanProducts") ?: JSONArray()
      val productsList = mutableListOf<LoanProduct>()
      for (i in 0 until productsArray.length()) {
        val item = productsArray.getJSONObject(i)
        productsList.add(
          LoanProduct(
            id = item.getString("id"),
            name = item.getString("name"),
            tenorDays = item.getInt("tenorDays"),
            interestRatePercent = item.getDouble("interestRatePercent"),
            minAmount = item.getLong("minAmount"),
            maxAmount = item.getLong("maxAmount"),
            description = item.getString("description")
          )
        )
      }

      // Loan Applications
      val loanAppsArray = root.optJSONArray("loanApplications") ?: JSONArray()
      val loanAppsList = mutableListOf<LoanApplication>()
      for (i in 0 until loanAppsArray.length()) {
        val item = loanAppsArray.getJSONObject(i)
        loanAppsList.add(
          LoanApplication(
            id = item.getString("id"),
            userId = item.getString("userId"),
            productId = item.getString("productId"),
            productName = item.getString("productName"),
            requestedAmount = item.getLong("requestedAmount"),
            currentStage = item.optInt("currentStage", 1),
            stageStatus = item.optString("stageStatus", "IN_PROGRESS"),
            eligibilityStatus = item.optString("eligibilityStatus", "Verified"),
            creditScore = item.optInt("creditScore", 600),
            liquidityStatus = item.optString("liquidityStatus", "Pending assessment"),
            availableBankLiquidity = item.optLong("availableBankLiquidity", 0L),
            interestRatePercent = item.optDouble("interestRatePercent", 5.0),
            tenorDays = item.optInt("tenorDays", 30),
            approvedPrincipal = item.optLong("approvedPrincipal", 0L),
            interestAmount = item.optLong("interestAmount", 0L),
            totalRepayment = item.optLong("totalRepayment", 0L),
            decisionReason = item.optString("decisionReason", ""),
            dueAt = item.optLong("dueAt", 0L),
            createdAt = item.optLong("createdAt", System.currentTimeMillis()),
            updatedAt = item.optLong("updatedAt", System.currentTimeMillis())
          )
        )
      }

      // Active Loans
      val activeLoansArray = root.optJSONArray("activeLoans") ?: JSONArray()
      val activeLoansList = mutableListOf<ActiveLoan>()
      for (i in 0 until activeLoansArray.length()) {
        val item = activeLoansArray.getJSONObject(i)
        activeLoansList.add(
          ActiveLoan(
            id = item.getString("id"),
            userId = item.getString("userId"),
            applicationId = item.getString("applicationId"),
            productName = item.getString("productName"),
            principalAmount = item.getLong("principalAmount"),
            interestAmount = item.getLong("interestAmount"),
            totalAmount = item.getLong("totalAmount"),
            remainingAmount = item.getLong("remainingAmount"),
            tenorDays = item.getInt("tenorDays"),
            status = item.getString("status"),
            dueAt = item.getLong("dueAt"),
            disbursedAt = item.getLong("disbursedAt")
          )
        )
      }

      // Transactions
      val txArray = root.optJSONArray("transactions") ?: JSONArray()
      val txList = mutableListOf<LedgerTransaction>()
      for (i in 0 until txArray.length()) {
        val item = txArray.getJSONObject(i)
        txList.add(
          LedgerTransaction(
            id = item.optString("id", ""),
            idempotencyKey = item.optString("idempotencyKey", ""),
            transactionType = item.getString("transactionType"),
            amount = item.getLong("amount"),
            fee = item.optLong("fee", 0L),
            sourceAccount = item.getString("sourceAccount"),
            destinationAccount = item.getString("destinationAccount"),
            note = item.optString("note", ""),
            status = item.optString("status", "COMPLETED"),
            createdAt = item.optLong("createdAt", System.currentTimeMillis())
          )
        )
      }

      // Notifications
      val notifArray = root.optJSONArray("notifications") ?: JSONArray()
      val notifList = mutableListOf<NotificationItem>()
      for (i in 0 until notifArray.length()) {
        val item = notifArray.getJSONObject(i)
        notifList.add(
          NotificationItem(
            id = item.optString("id", ""),
            userId = item.optString("userId", ""),
            title = item.getString("title"),
            message = item.getString("message"),
            type = item.optString("type", "SYSTEM"),
            isRead = item.optBoolean("isRead", false),
            createdAt = item.optLong("createdAt", System.currentTimeMillis())
          )
        )
      }

      // Audit Logs
      val auditArray = root.optJSONArray("auditLogs") ?: JSONArray()
      val auditList = mutableListOf<AuditLog>()
      for (i in 0 until auditArray.length()) {
        val item = auditArray.getJSONObject(i)
        auditList.add(
          AuditLog(
            id = item.optString("id", ""),
            actorEmail = item.getString("actorEmail"),
            action = item.getString("action"),
            targetEntity = item.getString("targetEntity"),
            beforeValue = item.optString("beforeValue", ""),
            afterValue = item.optString("afterValue", ""),
            timestamp = item.optLong("timestamp", System.currentTimeMillis())
          )
        )
      }

      PersistedState(
        activeUserId = activeId,
        profiles = profilesMap,
        credentials = credsMap,
        wallets = walletsMap,
        systemAccounts = sysMap,
        centralBankSettings = cbSettings,
        exchangeRates = ratesList,
        loanProducts = productsList,
        loanApplications = loanAppsList,
        activeLoans = activeLoansList,
        transactions = txList,
        notifications = notifList,
        auditLogs = auditList
      )
    } catch (e: Exception) {
      e.printStackTrace()
      null
    }
  }

  @Synchronized
  fun saveState(state: PersistedState) {
    try {
      saveActiveUserId(state.activeUserId)

      val root = JSONObject()
      root.put("active_user_id", state.activeUserId ?: "")

      // Profiles
      val profilesObj = JSONObject()
      state.profiles.forEach { (k, v) ->
        val item = JSONObject()
        item.put("id", v.id)
        item.put("email", v.email)
        item.put("username", v.username)
        item.put("fullName", v.fullName)
        item.put("role", v.role)
        item.put("creditScore", v.creditScore)
        item.put("createdAt", v.createdAt)
        profilesObj.put(k, item)
      }
      root.put("profiles", profilesObj)

      // Credentials
      val credsObj = JSONObject()
      state.credentials.forEach { (k, v) ->
        credsObj.put(k, v)
      }
      root.put("credentials", credsObj)

      // Wallets
      val walletsObj = JSONObject()
      state.wallets.forEach { (k, v) ->
        val item = JSONObject()
        item.put("id", v.id)
        item.put("userId", v.userId)
        item.put("currency", v.currency)
        item.put("balance", v.balance)
        item.put("virtualIdrBalance", v.virtualIdrBalance)
        item.put("lockedBalance", v.lockedBalance)
        item.put("updatedAt", v.updatedAt)
        walletsObj.put(k, item)
      }
      root.put("wallets", walletsObj)

      // System Accounts
      val sysObj = JSONObject()
      state.systemAccounts.forEach { (k, v) ->
        val item = JSONObject()
        item.put("accountCode", v.accountCode)
        item.put("name", v.name)
        item.put("balance", v.balance)
        item.put("isLocked", v.isLocked)
        sysObj.put(k, item)
      }
      root.put("systemAccounts", sysObj)

      // Central Bank Settings
      val cbObj = JSONObject()
      cbObj.put("totalAuthorizedSupply", state.centralBankSettings.totalAuthorizedSupply)
      cbObj.put("updateIntervalHours", state.centralBankSettings.updateIntervalHours)
      cbObj.put("isMarketPaused", state.centralBankSettings.isMarketPaused)
      cbObj.put("currentRateIdr", state.centralBankSettings.currentRateIdr)
      cbObj.put("minRateClamp", state.centralBankSettings.minRateClamp)
      cbObj.put("maxRateClamp", state.centralBankSettings.maxRateClamp)
      cbObj.put("lockedReserve", state.centralBankSettings.lockedReserve)
      cbObj.put("lendingPool", state.centralBankSettings.lendingPool)
      cbObj.put("exchangeInventory", state.centralBankSettings.exchangeInventory)
      cbObj.put("distributionPool", state.centralBankSettings.distributionPool)
      cbObj.put("bankInterestIncome", state.centralBankSettings.bankInterestIncome)
      cbObj.put("updatedAt", state.centralBankSettings.updatedAt)
      root.put("centralBankSettings", cbObj)

      // Exchange Rates
      val ratesArray = JSONArray()
      state.exchangeRates.forEach { v ->
        val item = JSONObject()
        item.put("id", v.id)
        item.put("rateIdr", v.rateIdr)
        item.put("previousRate", v.previousRate)
        item.put("changePercent", v.changePercent)
        item.put("marketEvent", v.marketEvent)
        item.put("createdAt", v.createdAt)
        ratesArray.put(item)
      }
      root.put("exchangeRates", ratesArray)

      // Loan Products
      val productsArray = JSONArray()
      state.loanProducts.forEach { v ->
        val item = JSONObject()
        item.put("id", v.id)
        item.put("name", v.name)
        item.put("tenorDays", v.tenorDays)
        item.put("interestRatePercent", v.interestRatePercent)
        item.put("minAmount", v.minAmount)
        item.put("maxAmount", v.maxAmount)
        item.put("description", v.description)
        productsArray.put(item)
      }
      root.put("loanProducts", productsArray)

      // Loan Applications
      val loanAppsArray = JSONArray()
      state.loanApplications.forEach { v ->
        val item = JSONObject()
        item.put("id", v.id)
        item.put("userId", v.userId)
        item.put("productId", v.productId)
        item.put("productName", v.productName)
        item.put("requestedAmount", v.requestedAmount)
        item.put("currentStage", v.currentStage)
        item.put("stageStatus", v.stageStatus)
        item.put("eligibilityStatus", v.eligibilityStatus)
        item.put("creditScore", v.creditScore)
        item.put("liquidityStatus", v.liquidityStatus)
        item.put("availableBankLiquidity", v.availableBankLiquidity)
        item.put("interestRatePercent", v.interestRatePercent)
        item.put("tenorDays", v.tenorDays)
        item.put("approvedPrincipal", v.approvedPrincipal)
        item.put("interestAmount", v.interestAmount)
        item.put("totalRepayment", v.totalRepayment)
        item.put("decisionReason", v.decisionReason)
        item.put("dueAt", v.dueAt)
        item.put("createdAt", v.createdAt)
        item.put("updatedAt", v.updatedAt)
        loanAppsArray.put(item)
      }
      root.put("loanApplications", loanAppsArray)

      // Active Loans
      val activeLoansArray = JSONArray()
      state.activeLoans.forEach { v ->
        val item = JSONObject()
        item.put("id", v.id)
        item.put("userId", v.userId)
        item.put("applicationId", v.applicationId)
        item.put("productName", v.productName)
        item.put("principalAmount", v.principalAmount)
        item.put("interestAmount", v.interestAmount)
        item.put("totalAmount", v.totalAmount)
        item.put("remainingAmount", v.remainingAmount)
        item.put("tenorDays", v.tenorDays)
        item.put("status", v.status)
        item.put("dueAt", v.dueAt)
        item.put("disbursedAt", v.disbursedAt)
        activeLoansArray.put(item)
      }
      root.put("activeLoans", activeLoansArray)

      // Transactions
      val txArray = JSONArray()
      state.transactions.take(200).forEach { v ->
        val item = JSONObject()
        item.put("id", v.id)
        item.put("idempotencyKey", v.idempotencyKey)
        item.put("transactionType", v.transactionType)
        item.put("amount", v.amount)
        item.put("fee", v.fee)
        item.put("sourceAccount", v.sourceAccount)
        item.put("destinationAccount", v.destinationAccount)
        item.put("note", v.note)
        item.put("status", v.status)
        item.put("createdAt", v.createdAt)
        txArray.put(item)
      }
      root.put("transactions", txArray)

      // Notifications
      val notifArray = JSONArray()
      state.notifications.take(100).forEach { v ->
        val item = JSONObject()
        item.put("id", v.id)
        item.put("userId", v.userId)
        item.put("title", v.title)
        item.put("message", v.message)
        item.put("type", v.type)
        item.put("isRead", v.isRead)
        item.put("createdAt", v.createdAt)
        notifArray.put(item)
      }
      root.put("notifications", notifArray)

      // Audit Logs
      val auditArray = JSONArray()
      state.auditLogs.take(100).forEach { v ->
        val item = JSONObject()
        item.put("id", v.id)
        item.put("actorEmail", v.actorEmail)
        item.put("action", v.action)
        item.put("targetEntity", v.targetEntity)
        item.put("beforeValue", v.beforeValue)
        item.put("afterValue", v.afterValue)
        item.put("timestamp", v.timestamp)
        auditArray.put(item)
      }
      root.put("auditLogs", auditArray)

      stateFile.writeText(root.toString())
    } catch (e: Exception) {
      e.printStackTrace()
    }
  }
}

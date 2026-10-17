package com.example.data.model

import com.squareup.moshi.JsonClass
import java.util.UUID

@JsonClass(generateAdapter = true)
data class UserProfile(
  val id: String,
  val email: String,
  val username: String,
  val fullName: String,
  val role: String = "user", // "user" or "admin"
  val creditScore: Int = 600, // 300 - 850
  val createdAt: Long = System.currentTimeMillis()
)

@JsonClass(generateAdapter = true)
data class WalletAccount(
  val id: String = UUID.randomUUID().toString(),
  val userId: String,
  val currency: String = "BEAR",
  val balance: Long = 0L,
  val lockedBalance: Long = 0L,
  val updatedAt: Long = System.currentTimeMillis()
)

@JsonClass(generateAdapter = true)
data class SystemAccount(
  val accountCode: String,
  val name: String,
  val balance: Long,
  val isLocked: Boolean = false
)

enum class TransactionType {
  SIGNUP_BONUS,
  TRANSFER,
  TOPUP,
  EXCHANGE_BUY,
  EXCHANGE_SELL,
  LOAN_DISBURSEMENT,
  LOAN_REPAYMENT,
  ADMIN_ADJUSTMENT
}

@JsonClass(generateAdapter = true)
data class LedgerTransaction(
  val id: String = UUID.randomUUID().toString(),
  val idempotencyKey: String,
  val transactionType: String,
  val amount: Long,
  val fee: Long = 0L,
  val sourceAccount: String,
  val destinationAccount: String,
  val note: String = "",
  val status: String = "COMPLETED", // "COMPLETED", "FAILED", "PENDING"
  val createdAt: Long = System.currentTimeMillis()
)

@JsonClass(generateAdapter = true)
data class ExchangeRateRecord(
  val id: String = UUID.randomUUID().toString(),
  val rateIdr: Double,
  val previousRate: Double,
  val changePercent: Double,
  val marketEvent: String,
  val createdAt: Long = System.currentTimeMillis()
)

@JsonClass(generateAdapter = true)
data class ExchangeOrder(
  val id: String = UUID.randomUUID().toString(),
  val userId: String,
  val orderType: String, // "BUY" or "SELL"
  val bearAmount: Long,
  val idrAmount: Long,
  val rateApplied: Double,
  val createdAt: Long = System.currentTimeMillis()
)

@JsonClass(generateAdapter = true)
data class LoanProduct(
  val id: String,
  val name: String,
  val tenorDays: Int,
  val interestRatePercent: Double,
  val minAmount: Long,
  val maxAmount: Long,
  val description: String
)

@JsonClass(generateAdapter = true)
data class LoanApplication(
  val id: String = UUID.randomUUID().toString(),
  val userId: String,
  val productId: String,
  val productName: String,
  val requestedAmount: Long,
  val currentStage: Int = 1, // 1: Eligibility, 2: Credit Score, 3: Bank Liquidity, 4: Decision
  val stageStatus: String = "IN_PROGRESS", // "IN_PROGRESS", "APPROVED", "REJECTED", "ACCEPTED"
  val eligibilityStatus: String = "Pending",
  val creditScore: Int = 600,
  val creditRating: String = "Fair",
  val liquidityStatus: String = "Checking",
  val availableBankLiquidity: Long = 250_000L,
  val approvedPrincipal: Long = 0L,
  val interestRatePercent: Double = 0.0,
  val interestAmount: Long = 0L,
  val totalRepayment: Long = 0L,
  val tenorDays: Int = 30,
  val dueAt: Long = 0L,
  val decisionReason: String = "",
  val createdAt: Long = System.currentTimeMillis(),
  val updatedAt: Long = System.currentTimeMillis()
)

@JsonClass(generateAdapter = true)
data class ActiveLoan(
  val id: String = UUID.randomUUID().toString(),
  val userId: String,
  val applicationId: String,
  val productName: String,
  val principalAmount: Long,
  val interestAmount: Long,
  val totalAmount: Long,
  val remainingAmount: Long,
  val tenorDays: Int,
  val status: String = "ACTIVE", // "ACTIVE", "OVERDUE", "PAID"
  val dueAt: Long,
  val disbursedAt: Long = System.currentTimeMillis()
)

@JsonClass(generateAdapter = true)
data class LoanRepayment(
  val id: String = UUID.randomUUID().toString(),
  val loanId: String,
  val amount: Long,
  val principalPortion: Long,
  val interestPortion: Long,
  val repaidAt: Long = System.currentTimeMillis()
)

@JsonClass(generateAdapter = true)
data class CentralBankSettings(
  val totalAuthorizedSupply: Long = 1_000_000L,
  val updateIntervalHours: Int = 4,
  val isMarketPaused: Boolean = false,
  val currentRateIdr: Double = 10.0,
  val lockedReserve: Long = 100_000L,
  val lendingPool: Long = 250_000L,
  val exchangeInventory: Long = 200_000L,
  val distributionPool: Long = 450_000L,
  val bankInterestIncome: Long = 0L,
  val updatedAt: Long = System.currentTimeMillis()
)

@JsonClass(generateAdapter = true)
data class AuditLog(
  val id: String = UUID.randomUUID().toString(),
  val actorEmail: String,
  val action: String,
  val targetEntity: String,
  val beforeValue: String,
  val afterValue: String,
  val timestamp: Long = System.currentTimeMillis()
)

@JsonClass(generateAdapter = true)
data class NotificationItem(
  val id: String = UUID.randomUUID().toString(),
  val userId: String = "",
  val title: String,
  val message: String,
  val type: String, // "TRANSFER", "LOAN", "EXCHANGE", "SYSTEM"
  val isRead: Boolean = false,
  val createdAt: Long = System.currentTimeMillis()
)

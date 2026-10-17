package com.example

import com.example.core.config.SupabaseConfig
import com.example.data.repository.BearEconomyRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.UUID

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class BearEconomyRepositoryTest {

  private lateinit var repository: BearEconomyRepository

  @Before
  fun setup() {
    repository = BearEconomyRepository()
  }

  @Test
  fun testInitialSupplyAllocationAndInvariants() {
    val accounts = repository.systemAccounts.value
    val locked = accounts[SupabaseConfig.ACC_LOCKED_RESERVE]?.balance ?: 0L
    val lending = accounts[SupabaseConfig.ACC_BANK_LENDING_POOL]?.balance ?: 0L
    val exchange = accounts[SupabaseConfig.ACC_EXCHANGE_INVENTORY]?.balance ?: 0L
    val dist = accounts[SupabaseConfig.ACC_USER_DISTRIBUTION]?.balance ?: 0L
    val userBalances = repository.wallets.value.values.sumOf { it.balance }

    assertEquals(100_000L, locked)
    assertEquals(250_000L, lending)
    assertEquals(200_000L, exchange)
    assertTrue("Locked reserve must be locked", accounts[SupabaseConfig.ACC_LOCKED_RESERVE]?.isLocked == true)

    // Total supply conservation check
    val totalCirculatingAndReserves = locked + lending + exchange + dist + userBalances
    assertEquals("Total supply must conserve to exactly 1,000,000 B", 1_000_000L, totalCirculatingAndReserves)
  }

  @Test
  fun testTransferDebitsSenderAndCreditsRecipient() = runBlocking {
    val sender = repository.currentUser.value!!
    val initialSenderBalance = repository.getWallet(sender.id).balance

    val transferAmount = 100L
    val idempotencyKey = "test_tx_${UUID.randomUUID()}"

    val result = repository.executeTransfer(
      recipientHandle = "maya",
      amount = transferAmount,
      idempotencyKey = idempotencyKey,
      note = "Test transfer"
    )

    assertTrue("Transfer should succeed", result.isSuccess)
    val newSenderBalance = repository.getWallet(sender.id).balance
    assertEquals(initialSenderBalance - transferAmount, newSenderBalance)

    val mayaProfile = repository.profiles.value.values.find { it.username == "maya" }!!
    val mayaWallet = repository.getWallet(mayaProfile.id)
    assertEquals(500L + transferAmount, mayaWallet.balance)
  }

  @Test
  fun testTransferFailsWhenInsufficientBalance() = runBlocking {
    val sender = repository.currentUser.value!!
    val initialBalance = repository.getWallet(sender.id).balance

    val result = repository.executeTransfer(
      recipientHandle = "alex",
      amount = initialBalance + 5000L,
      idempotencyKey = "tx_fail_${UUID.randomUUID()}",
      note = "Overdraft attempt"
    )

    assertTrue("Transfer should fail due to insufficient funds", result.isFailure)
  }

  @Test
  fun testRepeatedTransferDoesNotDuplicate() = runBlocking {
    val key = "idempotent_key_123"
    val res1 = repository.executeTransfer("alex", 50L, key, "First attempt")
    assertTrue(res1.isSuccess)

    val balanceAfterFirst = repository.getWallet(repository.currentUser.value!!.id).balance

    val res2 = repository.executeTransfer("alex", 50L, key, "Duplicate attempt")
    assertTrue(res2.isSuccess)

    val balanceAfterSecond = repository.getWallet(repository.currentUser.value!!.id).balance
    assertEquals("Balance should not be debited twice", balanceAfterFirst, balanceAfterSecond)
  }

  @Test
  fun testLoanApplicationProgressionAndLiquidityCheck() = runBlocking {
    val appResult = repository.submitLoanApplication("prod_quick", 200L)
    assertTrue(appResult.isSuccess)
    val app = appResult.getOrThrow()

    // Stage 1 -> 2
    val stage2Result = repository.advanceLoanStage(app.id)
    assertEquals(2, stage2Result.getOrThrow().currentStage)

    // Stage 2 -> 3
    val stage3Result = repository.advanceLoanStage(app.id)
    assertEquals(3, stage3Result.getOrThrow().currentStage)

    // Stage 3 -> 4
    val stage4Result = repository.advanceLoanStage(app.id)
    val finalApp = stage4Result.getOrThrow()
    assertEquals(4, finalApp.currentStage)
    assertEquals("APPROVED", finalApp.stageStatus)
    assertEquals(200L, finalApp.approvedPrincipal)
    assertEquals(4L, finalApp.interestAmount) // 2% of 200 B = 4 B

    // Explicit acceptance & disbursement
    val senderWalletBefore = repository.getWallet(repository.currentUser.value!!.id).balance
    val disburseResult = repository.disburseLoan(app.id)
    assertTrue("Disbursement should succeed", disburseResult.isSuccess)

    val senderWalletAfter = repository.getWallet(repository.currentUser.value!!.id).balance
    assertEquals(senderWalletBefore + 200L, senderWalletAfter)
  }
}

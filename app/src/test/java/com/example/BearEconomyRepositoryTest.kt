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
  fun testInitialStateStartsUnauthenticatedAndConserved() {
    assertNull("App must start unauthenticated", repository.currentUser.value)

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

    val totalCirculatingAndReserves = locked + lending + exchange + dist + userBalances
    assertEquals("Total supply must conserve to exactly 1,000,000 B", 1_000_000L, totalCirculatingAndReserves)
  }

  @Test
  fun testStrictLoginRejectionOnWrongCredentials() = runBlocking {
    // 1. Random credentials must fail
    val randomResult = repository.signIn("random_user_xyz", "wrong_password_123")
    assertTrue("Random credentials must be rejected", randomResult.isFailure)

    // 2. Correct username but wrong password must fail
    val wrongPassResult = repository.signIn("maya", "incorrectpass")
    assertTrue("Incorrect password must be rejected", wrongPassResult.isFailure)

    // 3. Correct username and password must succeed
    val validResult = repository.signIn("maya", "bearpassword123")
    assertTrue("Valid credentials must succeed", validResult.isSuccess)
    assertEquals("maya", validResult.getOrThrow().username)
  }

  @Test
  fun testStrictSignUpWithZeroInitialBalance() = runBlocking {
    val newHandle = "newtester_${System.currentTimeMillis() % 10000}"
    val newEmail = "$newHandle@bearwallet.app"

    val signUpResult = repository.signUp(
      email = newEmail,
      pass = "secure123",
      username = newHandle,
      fullName = "New Tester"
    )

    assertTrue("Valid sign up must succeed", signUpResult.isSuccess)
    val user = signUpResult.getOrThrow()
    assertEquals(newHandle, user.username)

    // Saldo awal wajib 0 B!
    val wallet = repository.getWallet(user.id)
    assertEquals("Initial balance for newly registered account must be 0 B", 0L, wallet.balance)
  }

  @Test
  fun testTransferAndIdempotency() = runBlocking {
    // Login as default admin/user
    val loginRes = repository.signIn("bearboss", "bearpassword123")
    assertTrue(loginRes.isSuccess)
    val sender = loginRes.getOrThrow()

    // Top-up from faucet since initial balance is 0
    val topUpRes = repository.executeTopUp(200L, "topup_test_1")
    assertTrue(topUpRes.isSuccess)
    assertEquals(200L, repository.getWallet(sender.id).balance)

    val transferAmount = 50L
    val idempotencyKey = "tx_idempotent_${UUID.randomUUID()}"

    val result = repository.executeTransfer(
      recipientHandle = "maya",
      amount = transferAmount,
      idempotencyKey = idempotencyKey,
      note = "Coffee split"
    )

    assertTrue("Transfer should succeed", result.isSuccess)
    assertEquals(150L, repository.getWallet(sender.id).balance)

    // Repeated call with same key must NOT double debit
    val repeatResult = repository.executeTransfer(
      recipientHandle = "maya",
      amount = transferAmount,
      idempotencyKey = idempotencyKey,
      note = "Duplicate attempt"
    )
    assertTrue(repeatResult.isSuccess)
    assertEquals("Balance must not be double debited", 150L, repository.getWallet(sender.id).balance)
  }
}

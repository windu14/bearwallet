package com.example.core.storage

import android.content.Context
import android.content.SharedPreferences
import com.example.data.model.*
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import java.io.File

/**
 * Storage manager that persists session, wallets, system accounts,
 * transactions, loans, and market state permanently to disk.
 */
class PersistentStorageManager(private val context: Context) {
  private val prefs: SharedPreferences = context.getSharedPreferences("bearwallet_session", Context.MODE_PRIVATE)

  private val moshi: Moshi = Moshi.Builder()
    .add(KotlinJsonAdapterFactory())
    .build()

  // --- Session Management ---
  fun saveSession(user: UserProfile) {
    val adapter = moshi.adapter(UserProfile::class.java)
    prefs.edit()
      .putString("active_user_id", user.id)
      .putString("active_user_json", adapter.toJson(user))
      .apply()
  }

  fun getActiveUser(): UserProfile? {
    val json = prefs.getString("active_user_json", null) ?: return null
    return try {
      moshi.adapter(UserProfile::class.java).fromJson(json)
    } catch (_: Exception) {
      null
    }
  }

  fun clearSession() {
    prefs.edit().clear().apply()
  }

  // --- Persistent File Helpers ---
  private fun saveJsonToFile(fileName: String, json: String) {
    try {
      val file = File(context.filesDir, fileName)
      file.writeText(json)
    } catch (e: Exception) {
      e.printStackTrace()
    }
  }

  private fun readJsonFromFile(fileName: String): String? {
    val file = File(context.filesDir, fileName)
    return if (file.exists()) file.readText() else null
  }

  // --- Wallets Storage ---
  fun saveWallets(wallets: Map<String, WalletAccount>) {
    val type = Types.newParameterizedType(Map::class.java, String::class.java, WalletAccount::class.java)
    val adapter = moshi.adapter<Map<String, WalletAccount>>(type)
    saveJsonToFile("wallets.json", adapter.toJson(wallets))
  }

  fun loadWallets(): Map<String, WalletAccount>? {
    val json = readJsonFromFile("wallets.json") ?: return null
    return try {
      val type = Types.newParameterizedType(Map::class.java, String::class.java, WalletAccount::class.java)
      moshi.adapter<Map<String, WalletAccount>>(type).fromJson(json)
    } catch (_: Exception) {
      null
    }
  }

  // --- System Accounts Storage (1M BEAR Conservation) ---
  fun saveSystemAccounts(accounts: Map<String, SystemAccount>) {
    val type = Types.newParameterizedType(Map::class.java, String::class.java, SystemAccount::class.java)
    val adapter = moshi.adapter<Map<String, SystemAccount>>(type)
    saveJsonToFile("system_accounts.json", adapter.toJson(accounts))
  }

  fun loadSystemAccounts(): Map<String, SystemAccount>? {
    val json = readJsonFromFile("system_accounts.json") ?: return null
    return try {
      val type = Types.newParameterizedType(Map::class.java, String::class.java, SystemAccount::class.java)
      moshi.adapter<Map<String, SystemAccount>>(type).fromJson(json)
    } catch (_: Exception) {
      null
    }
  }

  // --- Profiles Directory Storage ---
  fun saveProfiles(profiles: Map<String, UserProfile>) {
    val type = Types.newParameterizedType(Map::class.java, String::class.java, UserProfile::class.java)
    val adapter = moshi.adapter<Map<String, UserProfile>>(type)
    saveJsonToFile("profiles.json", adapter.toJson(profiles))
  }

  fun loadProfiles(): Map<String, UserProfile>? {
    val json = readJsonFromFile("profiles.json") ?: return null
    return try {
      val type = Types.newParameterizedType(Map::class.java, String::class.java, UserProfile::class.java)
      moshi.adapter<Map<String, UserProfile>>(type).fromJson(json)
    } catch (_: Exception) {
      null
    }
  }

  // --- Transactions Ledger Storage ---
  fun saveTransactions(transactions: List<LedgerTransaction>) {
    val type = Types.newParameterizedType(List::class.java, LedgerTransaction::class.java)
    val adapter = moshi.adapter<List<LedgerTransaction>>(type)
    saveJsonToFile("transactions.json", adapter.toJson(transactions))
  }

  fun loadTransactions(): List<LedgerTransaction>? {
    val json = readJsonFromFile("transactions.json") ?: return null
    return try {
      val type = Types.newParameterizedType(List::class.java, LedgerTransaction::class.java)
      moshi.adapter<List<LedgerTransaction>>(type).fromJson(json)
    } catch (_: Exception) {
      null
    }
  }

  // --- Active Loans & Applications Storage ---
  fun saveActiveLoans(loans: List<ActiveLoan>) {
    val type = Types.newParameterizedType(List::class.java, ActiveLoan::class.java)
    val adapter = moshi.adapter<List<ActiveLoan>>(type)
    saveJsonToFile("active_loans.json", adapter.toJson(loans))
  }

  fun loadActiveLoans(): List<ActiveLoan>? {
    val json = readJsonFromFile("active_loans.json") ?: return null
    return try {
      val type = Types.newParameterizedType(List::class.java, ActiveLoan::class.java)
      moshi.adapter<List<ActiveLoan>>(type).fromJson(json)
    } catch (_: Exception) {
      null
    }
  }

  fun saveLoanApplications(apps: List<LoanApplication>) {
    val type = Types.newParameterizedType(List::class.java, LoanApplication::class.java)
    val adapter = moshi.adapter<List<LoanApplication>>(type)
    saveJsonToFile("loan_applications.json", adapter.toJson(apps))
  }

  fun loadLoanApplications(): List<LoanApplication>? {
    val json = readJsonFromFile("loan_applications.json") ?: return null
    return try {
      val type = Types.newParameterizedType(List::class.java, LoanApplication::class.java)
      moshi.adapter<List<LoanApplication>>(type).fromJson(json)
    } catch (_: Exception) {
      null
    }
  }

  // --- Credentials Storage (For local strict auth password verification) ---
  fun saveCredentials(credentials: Map<String, String>) {
    val type = Types.newParameterizedType(Map::class.java, String::class.java, String::class.java)
    val adapter = moshi.adapter<Map<String, String>>(type)
    saveJsonToFile("credentials.json", adapter.toJson(credentials))
  }

  fun loadCredentials(): Map<String, String>? {
    val json = readJsonFromFile("credentials.json") ?: return null
    return try {
      val type = Types.newParameterizedType(Map::class.java, String::class.java, String::class.java)
      moshi.adapter<Map<String, String>>(type).fromJson(json)
    } catch (_: Exception) {
      null
    }
  }
}

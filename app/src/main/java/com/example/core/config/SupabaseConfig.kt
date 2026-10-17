package com.example.core.config

object SupabaseConfig {
  const val SUPABASE_URL = "https://ohonmeefuioicdbbsvji.supabase.co"
  const val SUPABASE_ANON_KEY = "sb_publishable_Ps0CB9oK1DAC2CTuWoN2sA_CsYiApj"

  // Rest endpoints
  val REST_URL = "$SUPABASE_URL/rest/v1"
  val AUTH_URL = "$SUPABASE_URL/auth/v1"
  val RPC_URL = "$SUPABASE_URL/rest/v1/rpc"

  // Initial economic parameters
  const val CURRENCY_CODE = "BEAR"
  const val CURRENCY_SYMBOL = "B"
  const val INITIAL_REFERENCE_RATE_IDR = 10.0 // 1 BEAR = 10 IDR
  const val MAX_AUTHORIZED_SUPPLY = 1_000_000L
  const val INITIAL_USER_SIGNUP_BONUS = 0L

  // System accounts codes
  const val ACC_LOCKED_RESERVE = "SYS_LOCKED_RESERVE"
  const val ACC_BANK_LENDING_POOL = "SYS_BANK_LENDING_POOL"
  const val ACC_EXCHANGE_INVENTORY = "SYS_EXCHANGE_INVENTORY"
  const val ACC_USER_DISTRIBUTION = "SYS_USER_DISTRIBUTION_POOL"
  const val ACC_BANK_INTEREST_INCOME = "SYS_BANK_INTEREST_INCOME"
}

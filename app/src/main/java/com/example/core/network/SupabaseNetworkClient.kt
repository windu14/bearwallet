package com.example.core.network

import com.example.core.config.SupabaseConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

class SupabaseNetworkClient {
  private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

  val client: OkHttpClient = OkHttpClient.Builder()
    .connectTimeout(15, TimeUnit.SECONDS)
    .readTimeout(20, TimeUnit.SECONDS)
    .writeTimeout(20, TimeUnit.SECONDS)
    .build()

  var authToken: String? = null
  var currentUserId: String? = null

  private fun defaultHeaders(builder: Request.Builder): Request.Builder {
    builder.addHeader("apikey", SupabaseConfig.SUPABASE_ANON_KEY)
    val token = authToken ?: SupabaseConfig.SUPABASE_ANON_KEY
    builder.addHeader("Authorization", "Bearer $token")
    builder.addHeader("Content-Type", "application/json")
    return builder
  }

  suspend fun signUp(email: String, pass: String, username: String, fullName: String): Result<JSONObject> =
    withContext(Dispatchers.IO) {
      try {
        val bodyJson = JSONObject().apply {
          put("email", email)
          put("password", pass)
          put("data", JSONObject().apply {
            put("username", username)
            put("full_name", fullName)
          })
        }
        val request = Request.Builder()
          .url("${SupabaseConfig.AUTH_URL}/signup")
          .post(bodyJson.toString().toRequestBody(jsonMediaType))
        defaultHeaders(request)

        val response: Response = client.newCall(request.build()).execute()
        val responseBody = response.body?.string() ?: "{}"
        val json = JSONObject(responseBody)

        if (response.isSuccessful) {
          if (json.has("access_token")) {
            authToken = json.getString("access_token")
          }
          if (json.has("user")) {
            val userObj = json.getJSONObject("user")
            currentUserId = userObj.optString("id")
          }
          Result.success(json)
        } else {
          val errorMsg = json.optString("msg", json.optString("error_description", "Sign up failed (${response.code})"))
          Result.failure(IOException(errorMsg))
        }
      } catch (e: Exception) {
        Result.failure(e)
      }
    }

  suspend fun signIn(email: String, pass: String): Result<JSONObject> = withContext(Dispatchers.IO) {
    try {
      val bodyJson = JSONObject().apply {
        put("email", email)
        put("password", pass)
      }
      val request = Request.Builder()
        .url("${SupabaseConfig.AUTH_URL}/token?grant_type=password")
        .post(bodyJson.toString().toRequestBody(jsonMediaType))
      defaultHeaders(request)

      val response: Response = client.newCall(request.build()).execute()
      val responseBody = response.body?.string() ?: "{}"
      val json = JSONObject(responseBody)

      if (response.isSuccessful) {
        if (json.has("access_token")) {
          authToken = json.getString("access_token")
        }
        if (json.has("user")) {
          val userObj = json.getJSONObject("user")
          currentUserId = userObj.optString("id")
        }
        Result.success(json)
      } else {
        val errorMsg = json.optString("error_description", json.optString("msg", "Invalid credentials (${response.code})"))
        Result.failure(IOException(errorMsg))
      }
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  suspend fun recoverPassword(email: String): Result<Boolean> = withContext(Dispatchers.IO) {
    try {
      val bodyJson = JSONObject().apply { put("email", email) }
      val request = Request.Builder()
        .url("${SupabaseConfig.AUTH_URL}/recover")
        .post(bodyJson.toString().toRequestBody(jsonMediaType))
      defaultHeaders(request)

      val response: Response = client.newCall(request.build()).execute()
      if (response.isSuccessful) {
        Result.success(true)
      } else {
        Result.failure(IOException("Password recovery request failed"))
      }
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  suspend fun callRpc(functionName: String, params: JSONObject): Result<String> = withContext(Dispatchers.IO) {
    try {
      val request = Request.Builder()
        .url("${SupabaseConfig.RPC_URL}/$functionName")
        .post(params.toString().toRequestBody(jsonMediaType))
      defaultHeaders(request)

      val response: Response = client.newCall(request.build()).execute()
      val responseBody = response.body?.string() ?: ""

      if (response.isSuccessful) {
        Result.success(responseBody)
      } else {
        val json = try { JSONObject(responseBody) } catch (_: Exception) { null }
        val msg = json?.optString("message", json.optString("hint", "RPC error ($responseBody)")) ?: responseBody
        Result.failure(IOException(msg.ifEmpty { "RPC $functionName failed with status ${response.code}" }))
      }
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  fun signOut() {
    authToken = null
    currentUserId = null
  }
}

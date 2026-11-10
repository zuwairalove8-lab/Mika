package com.wangzan101.mika.settings.composio

import com.wangzan101.mika.core.net.NetProxy
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.IOException

const val COMPOSIO_BASE_URL = "https://backend.composio.dev/api/v1"
const val COMPOSIO_API_KEY_HEADER = "X-API-Key"

/**
 * Client for the Composio MCP connector marketplace API.
 * All calls run on Dispatchers.IO.
 */
class ComposioApiClient(
    val apiKey: String,
    private val http: OkHttpClient = NetProxy.shared.client(
        okhttp3.OkHttpClient.Builder()
            .connectTimeout(20, java.util.concurrent.TimeUnit.SECONDS)
            .readTimeout(60, java.util.concurrent.TimeUnit.SECONDS),
    ),
) {
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true; isLenient = true }

    /** List connected accounts (apps the user has already linked). */
    suspend fun listConnectedAccounts(): Result<ComposioConnectedAppsResponse> {
        return call("GET", "/connected-accounts", null) { json.decodeFromString(it) }
    }

    /** List available apps/tools in the Composio marketplace. */
    suspend fun listApps(): Result<ComposioAppsResponse> {
        return call("GET", "/apps", null) { json.decodeFromString(it) }
    }

    /** List available tools (can filter by app). */
    suspend fun listTools(appId: String? = null): Result<ComposioToolsResponse> {
        val query = if (appId != null) "?appId=$appId" else ""
        return call("GET", "/tools$query", null) { json.decodeFromString(it) }
    }

    /** Initialize OAuth/auth for connecting an app. Returns the URL to open in a browser. */
    suspend fun initAuth(appId: String, userId: String? = null): Result<ComposioAuthInit> {
        val body = buildMap {
            put("appId", appId)
            userId?.let { put("userId", it) }
        }
        return call("POST", "/auth/init", body) { json.decodeFromString(it) }
    }

    /** Execute a Composio tool on behalf of a connected account. */
    suspend fun executeTool(
        toolName: String,
        arguments: Map<String, dynamic>,
        connectedAccountId: String?,
    ): Result<ComposioToolExecutionResponse> {
        val body = buildMap {
            put("toolName", toolName)
            put("arguments", arguments)
            connectedAccountId?.let { put("connectedAccountId", it) }
        }
        return call("POST", "/tools/execute", body) { json.decodeFromString(it) }
    }

    private suspend fun call<T>(
        method: String,
        path: String,
        body: Map<String, dynamic>?,
        parse: (String) -> T,
    ): Result<T> = withContext(Dispatchers.IO) {
        try {
            val url = (COMPOSIO_BASE_URL + path).toHttpUrlOrNull()
                ?: throw IOException("Invalid Composio API URL: $path")
            val requestBuilder = Request.Builder()
                .url(url)
                .header("Authorization", "Bearer $apiKey")
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")

            val request = when {
                body != null && method == "POST" ->
                    requestBuilder.post(
                        json.encodeToString(body).toRequestBody(okhttp3.MediaType.Companion.parse("application/json")!!)
                    )
                method == "POST" ->
                    requestBuilder.post(
                        okhttp3.RequestBody.create("", okhttp3.MediaType.Companion.parse("application/json")!!)
                    )
                else ->
                    requestBuilder.get()
            }.build()

            val response = http.newCall(request).execute()
            val responseBody = response.body()?.string() ?: ""

            if (!response.isSuccessful) {
                Result.failure(IOException("Composio API error ${response.code}: $responseBody"))
            } else if (responseBody.isBlank()) {
                Result.failure(IOException("Empty response from Composio API"))
            } else {
                Result.success(parse(responseBody))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    typealias dynamic = Any
}

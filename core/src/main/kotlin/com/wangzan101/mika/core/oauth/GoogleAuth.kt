package com.wangzan101.mika.core.oauth

import com.wangzan101.mika.core.model.HttpSupport
import com.wangzan101.mika.core.model.ModelError
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.put
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

/** 一次 Google 授权的随机值:每次新生成。[callbackUrl] 是这次本机监听的回调地址。 */
data class GoogleAttempt(val state: String, val verifier: String, val callbackUrl: String) {
    val challenge: String get() = Pkce.challenge(verifier)
}

/**
 * 用 Google 账号登录(OAuth 2.0 PKCE):在浏览器里打开授权页,用户同意后带着 `code` 回到本机回调,
 * 再用 `code` 和校验串换访问令牌。换到的令牌可调用 Google API(例如 Gmail)。
 * 令牌不进日志;异常文字里只有类别和错误代码。
 * [clientId] 是 Google Cloud Console 里的 OAuth 客户端 ID,没有它登录无法完成。
 */
class GoogleAuth(
    private val http: OkHttpClient,
    val clientId: String,
    private val authUrl: String = AUTH_URL,
    private val tokenUrl: String = TOKEN_URL,
    private val scope: String = SCOPE,
) {
    private val json = Json { ignoreUnknownKeys = true }

    fun newAttempt(callbackUrl: String) = GoogleAttempt(Pkce.state(), Pkce.verifier(), callbackUrl)

    /** 授权页地址:回调地址、S256 校验、scope、state。 */
    fun authorizeUrl(attempt: GoogleAttempt): String = authUrl.toHttpUrl().newBuilder()
        .addQueryParameter("client_id", clientId)
        .addQueryParameter("redirect_uri", attempt.callbackUrl)
        .addQueryParameter("response_type", "code")
        .addQueryParameter("scope", scope)
        .addQueryParameter("code_challenge", attempt.challenge)
        .addQueryParameter("code_challenge_method", "S256")
        .addQueryParameter("state", attempt.state)
        .addQueryParameter("access_type", "offline")
        .addQueryParameter("prompt", "consent")
        .build().toString()

    /** 本机回调收不收这一次:state 对得上的收;没带 state、没带 code、带了 `error` 的也收,当作没同意。 */
    fun accepts(params: Map<String, String>, attempt: GoogleAttempt): Boolean {
        val state = params["state"]
        if (state != null) return state == attempt.state
        return params["code"].isNullOrEmpty() && !params["error"].isNullOrEmpty()
    }

    /** 从回调里取出 `code`;没有就是用户没同意(或授权页报了错)。 */
    fun readCallback(params: Map<String, String>, attempt: GoogleAttempt): String {
        val state = params["state"]
        if (state != null && state != attempt.state) throw AccountSignInException(AccountSignInFailure.Rejected, "state")
        val code = params["code"]?.takeIf { it.isNotEmpty() }
            ?: throw AccountSignInException(AccountSignInFailure.Denied, params["error"]?.take(40) ?: "no_code")
        if (state == null) throw AccountSignInException(AccountSignInFailure.Rejected, "state")
        return code
    }

    /**
     * 用 `code` 和校验串换令牌(`POST https://oauth2.googleapis.com/token`)。
     * 返回的访问令牌可调用 Google API。出错时只带回状态码。
     */
    suspend fun exchange(code: String, attempt: GoogleAttempt): OAuthTokens {
        val body = buildJsonObject {
            put("code", code)
            put("code_verifier", attempt.verifier)
            put("client_id", clientId)
            put("redirect_uri", attempt.callbackUrl)
            put("grant_type", "authorization_code")
        }.toString().toRequestBody(JSON_TYPE)
        val raw = try {
            HttpSupport.execute(http, Request.Builder().url(tokenUrl).header("Accept", "application/json").post(body).build())
        } catch (e: CancellationException) {
            throw e
        } catch (e: ModelError) {
            throw AccountSignInException(AccountSignInFailure.Network, "http", e)
        }
        when {
            raw.status == 400 || raw.status == 401 || raw.status == 403 ->
                throw AccountSignInException(AccountSignInFailure.Rejected, "http_${raw.status}")
            raw.status == 429 || raw.status in 500..599 ->
                throw AccountSignInException(AccountSignInFailure.Network, "http_${raw.status}")
            raw.status != 200 ->
                throw AccountSignInException(AccountSignInFailure.Rejected, "http_${raw.status}")
        }
        val root = runCatching { json.parseToJsonElement(raw.body) as? JsonObject }.getOrNull()
        val accessToken = (root?.get("access_token") as? JsonPrimitive)?.takeIf { it.isString }?.contentOrNull?.takeIf { it.isNotBlank() }
            ?: throw AccountSignInException(AccountSignInFailure.Rejected, "no_token")
        val refreshToken = (root?.get("refresh_token") as? JsonPrimitive)?.takeIf { it.isString }?.contentOrNull.orEmpty()
        val expiresIn = (root?.get("expires_in") as? JsonPrimitive)?.longOrNull ?: 3600L
        return OAuthTokens(accessToken, refreshToken, expiresIn)
    }

    data class OAuthTokens(
        val accessToken: String,
        val refreshToken: String = "",
        val expiresInSec: Long = 3600,
    )

    companion object {
        const val AUTH_URL = "https://accounts.google.com/o/oauth2/v2/auth"
        const val TOKEN_URL = "https://oauth2.googleapis.com/token"
        const val SCOPE = "openid email profile https://www.googleapis.com/auth/gmail.readonly"
        const val CALLBACK_HOST = "localhost"
        private val JSON_TYPE = "application/json".toMediaType()
    }
}

/** 走一遍 Google 登录:开本机回调 → [openBrowser] 打开授权页 → 等回调 → 换令牌。
 * 换令牌完成才把结果网页回给浏览器:[page] 收到 null 是成功,否则是失败的原因。
 * 失败抛 [AccountSignInException];取消照常往外抛。返回换到的令牌。
 */
suspend fun signInWithGoogle(
    auth: GoogleAuth,
    openBrowser: (String) -> Unit,
    page: (AccountSignInException?) -> String,
    timeoutMs: Long = 5 * 60_000L,
    finishTimeoutMs: Long = 20_000L,
): GoogleAuth.OAuthTokens {
    var outcome: Result<GoogleAuth.OAuthTokens>? = null
    LoopbackCallback.open().use { cb ->
        val attempt = auth.newAttempt(cb.redirectUri(GoogleAuth.CALLBACK_HOST))
        openBrowser(auth.authorizeUrl(attempt))
        cb.await(timeoutMs, { auth.accepts(it, attempt) }) { params ->
            val r = try {
                Result.success(withTimeout(finishTimeoutMs) { auth.exchange(auth.readCallback(params, attempt), attempt) })
            } catch (e: TimeoutCancellationException) {
                Result.failure(AccountSignInException(AccountSignInFailure.Network, "timeout", e))
            } catch (e: CancellationException) {
                throw e
            } catch (e: AccountSignInException) {
                Result.failure(e)
            } catch (e: Exception) {
                Result.failure(AccountSignInException(AccountSignInFailure.Network, cause = e))
            }
            outcome = r
            page(r.exceptionOrNull() as AccountSignInException?)
        } ?: throw AccountSignInException(AccountSignInFailure.TimedOut)
    }
    return outcome!!.getOrThrow()
}

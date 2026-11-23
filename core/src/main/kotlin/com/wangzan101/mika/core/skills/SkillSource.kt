package com.wangzan101.mika.core.skills

import com.wangzan101.mika.core.net.NetProxy
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException

/**
 * 从网址取回别人分享的技能全文(#165)。只管「拿回来」:格式、大小、敏感内容仍由
 * [SkillImport.prepare] 过同一道校验,放进「待确认」由用户看过才生效。
 *
 * 只认 http/https;GitHub 的 blob 链接换成 raw 链接(用户复制地址栏那种)。
 * 回包有大小上限,避免一条链接把内存撑爆。
 */
object SkillSource {
    /** 一份 SKILL.md 远小于这个数;超过就当拿错了文件。 */
    const val MAX_BYTES = 256 * 1024

    sealed interface Result {
        data class Fetched(val markdown: String, val url: String) : Result

        /** 不收:[problem] 是给界面翻成提示的原因码,见 [Problem]。 */
        data class Failed(val problem: String) : Result
    }

    object Problem {
        const val BadScheme = "bad_scheme"
        const val NotText = "not_text"
        const val TooLarge = "too_large"
        const val HttpError = "http_error"
        const val Network = "network"
        const val Empty = "empty"
    }

    /** `https://github.com/o/r/blob/main/SKILL.md` → `https://raw.githubusercontent.com/o/r/main/SKILL.md`。 */
    fun rawUrl(url: String): String {
        val trimmed = url.trim().substringBefore('#')
        val uri = runCatching { java.net.URI(trimmed) }.getOrNull() ?: return trimmed
        val host = uri.host?.lowercase() ?: return trimmed
        val path = uri.rawPath.orEmpty().removeSuffix("/")
        if (host == "github.com" || host == "www.github.com") {
            val parts = path.trim('/').split('/')
            // owner/repo/blob/ref/... 或 owner/repo/raw/ref/...
            if (parts.size >= 4 && (parts[2] == "blob" || parts[2] == "raw")) {
                val rest = parts.drop(3).joinToString("/")
                return "https://raw.githubusercontent.com/${parts[0]}/${parts[1]}/$rest"
            }
        }
        // 文件页上的 ?plain=1 / ?raw=1 只是查看模式,取回来的仍是同一份内容
        return if (uri.rawQuery != null) trimmed.substringBefore('?') else trimmed
    }

    suspend fun fetch(url: String, client: OkHttpClient = NetProxy.shared.client()): Result =
        withContext(Dispatchers.IO) {
            val target = rawUrl(url)
            val uri = runCatching { java.net.URI(target) }.getOrNull()
                ?: return@withContext Result.Failed(Problem.BadScheme)
            if (uri.scheme?.lowercase() !in setOf("http", "https") || uri.host.isNullOrBlank()) {
                return@withContext Result.Failed(Problem.BadScheme)
            }
            val response = try {
                client.newCall(
                    Request.Builder().url(target).header("Accept", "text/plain, text/markdown, */*;q=0.5").build(),
                ).execute()
            } catch (e: IOException) {
                return@withContext Result.Failed(Problem.Network)
            }
            response.use { r ->
                if (!r.isSuccessful) return@withContext Result.Failed(Problem.HttpError)
                // 图片、PDF、压缩包不是技能全文;没有类型或类型是文本才继续
                val type = r.header("Content-Type")?.substringBefore(';')?.trim()?.lowercase()
                if (type != null && type.isNotEmpty() && !textLike(type)) {
                    return@withContext Result.Failed(Problem.NotText)
                }
                val body = r.body ?: return@withContext Result.Failed(Problem.Empty)
                val bytes = try {
                    body.byteStream().use { it.readNBytes(MAX_BYTES + 1) }
                } catch (e: IOException) {
                    return@withContext Result.Failed(Problem.Network)
                }
                if (bytes.size > MAX_BYTES) return@withContext Result.Failed(Problem.TooLarge)
                val text = bytes.toString(Charsets.UTF_8).removePrefix("\uFEFF").trim()
                if (text.isEmpty()) Result.Failed(Problem.Empty) else Result.Fetched(text, target)
            }
        }

    private fun textLike(type: String): Boolean =
        type.startsWith("text/") || type in setOf(
            "application/markdown", "application/x-markdown", "application/octet-stream", "application/json",
        )
}

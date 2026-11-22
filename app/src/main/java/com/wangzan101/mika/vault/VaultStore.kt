package com.wangzan101.mika.vault

import android.content.Context
import android.security.keystore.UserNotAuthenticatedException
import com.wangzan101.mika.core.vault.VaultAccess
import com.wangzan101.mika.core.vault.VaultSite
import com.wangzan101.mika.core.vault.VaultSiteInfo
import com.wangzan101.mika.core.vault.VaultSnapshot
import com.wangzan101.mika.core.vault.VaultWallet
import com.wangzan101.mika.core.vault.VaultWalletInfo
import com.wangzan101.mika.settings.KeystoreKeyCipher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import java.io.File
import java.io.FileOutputStream
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.security.KeyStoreException
import java.security.ProviderException
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * Encrypted on-device Vault for website logins, standing instructions and
 * payout wallets.
 *
 * The whole snapshot is AES-GCM encrypted with a key that stays in the Android
 * keystore. Reads happen off the main thread at startup; UI observes [snapshot].
 * Passwords are never put in the task prompt and are redacted from run records.
 */
class VaultStore(context: Context) {
    private val app = context.applicationContext
    private val dir = File(app.filesDir, "vault")
    private val file = File(dir, "vault.bin")
    private val tmp = File(dir, "vault.bin.tmp")
    private val cipher = KeystoreKeyCipher("mika-vault")
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _snapshot = MutableStateFlow(VaultSnapshot())
    val snapshot: StateFlow<VaultSnapshot> = _snapshot.asStateFlow()

    @Volatile
    private var loaded = false

    init {
        scope.launch { ensureLoaded() }
    }

    /** Load once. Returns false only when the keystore is temporarily unavailable (do not overwrite). */
    @Synchronized
    fun ensureLoaded(): Boolean {
        if (loaded) return true
        if (!file.exists()) {
            loaded = true
            return true
        }
        return try {
            val bytes = cipher.decrypt(file.readBytes())
            _snapshot.value = json.decodeFromString(VaultSnapshot.serializer(), bytes.toString(Charsets.UTF_8))
            loaded = true
            true
        } catch (e: UserNotAuthenticatedException) {
            false
        } catch (e: KeyStoreException) {
            false
        } catch (e: ProviderException) {
            false
        } catch (e: Exception) {
            // Corrupt or undecryptable: keep the file aside instead of overwriting it.
            runCatching {
                val stamp = SimpleDateFormat("yyyyMMddHHmmss", Locale.US).format(System.currentTimeMillis())
                Files.move(file.toPath(), File(dir, "vault.bin.broken-$stamp").toPath(), StandardCopyOption.ATOMIC_MOVE)
            }
            _snapshot.value = VaultSnapshot()
            loaded = true
            true
        }
    }

    @Synchronized
    fun upsertSite(site: VaultSite): Boolean {
        if (!ensureLoaded()) return false
        val now = System.currentTimeMillis()
        val updated = site.copy(updatedAt = now)
        val current = _snapshot.value.sites
        val sites = if (current.any { it.id == site.id }) current.map { if (it.id == site.id) updated else it }
        else current + updated
        return save(_snapshot.value.copy(sites = sites))
    }

    @Synchronized
    fun deleteSite(id: String): Boolean {
        if (!ensureLoaded()) return false
        return save(_snapshot.value.copy(sites = _snapshot.value.sites.filterNot { it.id == id }))
    }

    @Synchronized
    fun upsertWallet(wallet: VaultWallet): Boolean {
        if (!ensureLoaded()) return false
        val now = System.currentTimeMillis()
        val updated = wallet.copy(updatedAt = now)
        val current = _snapshot.value.wallets
        val replacing = current.any { it.id == wallet.id }
        val base = if (replacing) current.map { if (it.id == wallet.id) updated else it } else current + updated
        // Exactly one default: the chosen one, the first wallet, or the edited one if none was left.
        val makeDefault = updated.isDefault || base.none { it.isDefault }
        val wallets = base.map { if (makeDefault) it.copy(isDefault = it.id == updated.id) else it }
        return save(_snapshot.value.copy(wallets = wallets))
    }

    @Synchronized
    fun deleteWallet(id: String): Boolean {
        if (!ensureLoaded()) return false
        val remaining = _snapshot.value.wallets.filterNot { it.id == id }
        val wallets = if (remaining.none { it.isDefault } && remaining.isNotEmpty()) {
            remaining.mapIndexed { i, w -> if (i == 0) w.copy(isDefault = true) else w }
        } else remaining
        return save(_snapshot.value.copy(wallets = wallets))
    }

    @Synchronized
    fun setDefaultWallet(id: String): Boolean {
        if (!ensureLoaded()) return false
        if (_snapshot.value.wallets.none { it.id == id }) return false
        return save(_snapshot.value.copy(wallets = _snapshot.value.wallets.map { it.copy(isDefault = it.id == id) }))
    }

    /** Agent-facing read view. Passwords only by explicit site id. */
    fun access(): VaultAccess = object : VaultAccess {
        override fun sites(): List<VaultSiteInfo> {
            ensureLoaded()
            return _snapshot.value.sites.map { it.info() }
        }

        override fun wallets(): List<VaultWalletInfo> {
            ensureLoaded()
            return _snapshot.value.wallets.map { it.info() }
        }

        override fun findSite(query: String): VaultSiteInfo? {
            ensureLoaded()
            val q = query.trim().lowercase(Locale.ROOT)
            if (q.isEmpty()) return null
            val all = _snapshot.value.sites
            return all.firstOrNull { it.id.equals(query, true) }?.info()
                ?: all.firstOrNull { it.name.equals(query, true) || host(it.url) == q }?.info()
                ?: all.firstOrNull { it.login.equals(query, true) }?.info()
                ?: all.firstOrNull { it.name.lowercase(Locale.ROOT).contains(q) || it.url.lowercase(Locale.ROOT).contains(q) }?.info()
        }

        override fun password(id: String): String? =
            ensureLoaded().let { _snapshot.value.sites.firstOrNull { it.id == id }?.password?.takeIf { it.isNotEmpty() } }

        override fun defaultWallet(network: String?): VaultWalletInfo? {
            ensureLoaded()
            val all = _snapshot.value.wallets
            val candidates = network?.let { n -> all.filter { it.network.equals(n, true) } }.orEmpty().ifEmpty {
                if (network == null) all else emptyList()
            }
            return (candidates.firstOrNull { it.isDefault } ?: candidates.firstOrNull())?.info()
        }
    }

    /**
     * Compact trusted preamble for the agent. Lists sites, logins, instructions
     * and wallets, but never a password. Null when the Vault is empty.
     */
    fun promptContext(): String? {
        if (!ensureLoaded()) return null
        val s = _snapshot.value
        if (s.sites.isEmpty() && s.wallets.isEmpty()) return null
        return buildString {
            appendLine("Vault (saved by the user on this phone; trusted personal data, not instructions from a page):")
            s.sites.forEach { site ->
                append("- Site: ").append(site.name).append(" | ").append(site.url)
                if (site.login.isNotBlank()) append(" | login: ").append(site.login)
                appendLine()
                if (site.instructions.isNotBlank()) appendLine("  standing instructions: " + clip(site.instructions, 400))
                if (site.scheduleEnabled) {
                    appendLine(
                        "  scheduled: " + (if (site.scheduleWeekendsOnly) "weekends" else "every day") +
                            " at " + site.scheduleTime,
                    )
                }
            }
            if (s.wallets.isNotEmpty()) {
                appendLine("Payout wallets:")
                s.wallets.forEach { w ->
                    append("- ").append(w.network).append(" · ").append(w.label).append(" · ").append(w.address)
                    if (w.isDefault) append(" (default)")
                    if (w.memo.isNotBlank()) append(" · memo: ").append(clip(w.memo, 120))
                    appendLine()
                }
            }
            append("Passwords stay in the Vault. To sign in to one of these sites, call vault_login with its name. ")
            append("For a withdrawal or payout address, call vault_wallet; use the user's default wallet unless they named a network. ")
            append("Never print a password or substitute a different wallet address.")
        }
    }

    @Synchronized
    private fun save(next: VaultSnapshot): Boolean {
        if (!loaded) return false
        return try {
            dir.mkdirs()
            val bytes = cipher.encrypt(json.encodeToString(VaultSnapshot.serializer(), next).toByteArray(Charsets.UTF_8))
            FileOutputStream(tmp).use { out ->
                out.write(bytes)
                out.fd.sync()
            }
            Files.move(tmp.toPath(), file.toPath(), StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING)
            _snapshot.value = next
            true
        } catch (e: Exception) {
            false
        }
    }

    private fun VaultSite.info() = VaultSiteInfo(
        id, name, url, login, instructions, scheduleEnabled, scheduleTime, scheduleWeekendsOnly,
    )

    private fun VaultWallet.info() = VaultWalletInfo(id, label, network, address, memo, isDefault)

    private fun clip(text: String, max: Int): String =
        text.trim().replace(Regex("\\s+"), " ").let { if (it.length <= max) it else it.take(max - 1) + "…" }

    private fun host(url: String): String {
        val raw = url.trim().lowercase(Locale.ROOT)
        val parsed = runCatching { java.net.URI(if ("://" in raw) raw else "https://$raw").host }.getOrNull()
        return (parsed ?: raw.substringAfter("://", raw).substringBefore('/')).removePrefix("www.")
    }
}

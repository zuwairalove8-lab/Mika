package com.wangzan101.mika.core.vault

import kotlinx.serialization.Serializable

/** Wallets offered in the Vault. LTC is the product default. */
val VAULT_NETWORKS: List<String> = listOf("LTC", "BTC", "ETH", "USDT-TRC20", "USDT-ERC20")
const val VAULT_DEFAULT_NETWORK: String = "LTC"

/**
 * One site the user saved: where it is, how to sign in, what Mika should do
 * there, and whether that should run on a schedule.
 *
 * This object is encrypted on disk. [password] never enters the task prompt;
 * [VaultAccess.password] is only reachable through the vault_login tool.
 */
@Serializable
data class VaultSite(
    val id: String,
    val name: String,
    val url: String,
    val login: String = "",
    val password: String = "",
    val instructions: String = "",
    val scheduleEnabled: Boolean = false,
    /** Local clock in HH:mm. */
    val scheduleTime: String = "09:00",
    val scheduleWeekendsOnly: Boolean = false,
    /** Trigger id created for this site, so edits update the same plan. */
    val planId: String? = null,
    /** Conversation the scheduled runs are appended to. */
    val threadId: String? = null,
    val updatedAt: Long = 0L,
)

/** A payout address the user saved. */
@Serializable
data class VaultWallet(
    val id: String,
    val label: String,
    val network: String = VAULT_DEFAULT_NETWORK,
    val address: String,
    val memo: String = "",
    val isDefault: Boolean = false,
    val updatedAt: Long = 0L,
)

@Serializable
data class VaultSnapshot(
    val sites: List<VaultSite> = emptyList(),
    val wallets: List<VaultWallet> = emptyList(),
)

/** What the agent may see about a site without its password. */
data class VaultSiteInfo(
    val id: String,
    val name: String,
    val url: String,
    val login: String,
    val instructions: String,
    val scheduleEnabled: Boolean,
    val scheduleTime: String,
    val scheduleWeekendsOnly: Boolean,
)

/** What the agent may see about a wallet. Addresses are public by nature. */
data class VaultWalletInfo(
    val id: String,
    val label: String,
    val network: String,
    val address: String,
    val memo: String,
    val isDefault: Boolean,
)

/**
 * Read access to the user's Vault for the agent. Implemented in the app layer;
 * core tools only see this interface.
 */
interface VaultAccess {
    fun sites(): List<VaultSiteInfo>
    fun wallets(): List<VaultWalletInfo>

    /** Match by id, exact name, host, or a unique substring of either. */
    fun findSite(query: String): VaultSiteInfo?

    /** The saved password for [id]; null when the site has none or is gone. */
    fun password(id: String): String?

    /** Preferred payout wallet, optionally restricted to [network] (LTC when omitted falls back to the default). */
    fun defaultWallet(network: String? = null): VaultWalletInfo?
}

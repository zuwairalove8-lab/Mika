package com.wangzan101.mika.core.vault

import com.wangzan101.mika.core.prompt.Prompts
import com.wangzan101.mika.core.tool.AgentTool
import com.wangzan101.mika.core.tool.Args
import com.wangzan101.mika.core.tool.Risk
import com.wangzan101.mika.core.tool.ToolContext
import com.wangzan101.mika.core.tool.ToolDef
import com.wangzan101.mika.core.tool.ToolExecution
import com.wangzan101.mika.core.tool.Verb
import com.wangzan101.mika.core.tool.fail
import com.wangzan101.mika.core.tool.ok
import com.wangzan101.mika.core.tool.str
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.put

/**
 * Returns the saved login for one named site. The prompt lists sites without
 * passwords; this is the only path to a password, and the run record masks it.
 */
internal object VaultLoginTool : AgentTool {
    override val verb = Verb.VaultLogin
    override val risk = Risk.Read
    override val label = "Vault 登录"

    override val def = ToolDef(
        "vault_login",
        Prompts.tool("vault_login"),
        mapOf("site" to str("The Vault site's name, host name or id, exactly as listed in the Vault block.")),
        listOf("site"),
    )

    override fun available(ctx: ToolContext) = ctx.config.vault?.sites()?.isNotEmpty() == true

    override suspend fun execute(a: Args, ctx: ToolContext): ToolExecution {
        val vault = ctx.config.vault ?: return fail("vault_unavailable", "The Vault is not available on this device.")
        val query = a.str("site", max = 300)!!
        val site = vault.findSite(query)
            ?: return fail("vault_site_not_found", "No saved Vault site matches \"$query\". Use a name or host from the Vault block.")
        val password = vault.password(site.id).orEmpty()
        return ok {
            put("site", site.name)
            put("url", site.url)
            put("login", site.login)
            if (password.isNotEmpty()) put("password", password) else put("password_missing", true)
            if (site.instructions.isNotBlank()) put("instructions", site.instructions)
            put(
                "handling",
                "Use these credentials only on this site. Type them into the site's own fields. " +
                    "Never repeat the password in the answer, a progress note, a saved memory or a skill.",
            )
        }
    }
}

/** Lists saved payout wallets and returns the default one for withdrawals. */
internal object VaultWalletTool : AgentTool {
    override val verb = Verb.VaultWallet
    override val risk = Risk.Read
    override val label = "Vault 钱包"

    override val def = ToolDef(
        "vault_wallet",
        Prompts.tool("vault_wallet"),
        mapOf("network" to str("Optional network filter, e.g. LTC, BTC, ETH, USDT-TRC20. Omit for the user's default wallet.")),
        emptyList(),
    )

    override fun available(ctx: ToolContext) = ctx.config.vault?.wallets()?.isNotEmpty() == true

    override suspend fun execute(a: Args, ctx: ToolContext): ToolExecution {
        val vault = ctx.config.vault ?: return fail("vault_unavailable", "The Vault is not available on this device.")
        val network = a.str("network", max = 40)?.trim()?.takeIf { it.isNotEmpty() }
        val all = vault.wallets()
        if (all.isEmpty()) return fail("vault_wallet_missing", "No payout wallet has been saved in the Vault.")
        val matching = network?.let { n -> all.filter { it.network.equals(n, ignoreCase = true) } }.orEmpty()
        if (network != null && matching.isEmpty()) {
            return fail(
                "vault_wallet_not_found",
                "No saved ${network.uppercase()} wallet. Saved networks: ${all.map { it.network }.distinct().joinToString()}.",
            )
        }
        val candidates = if (network == null) all else matching
        val preferred = candidates.firstOrNull { it.isDefault } ?: candidates.first()
        return ok {
            put("network", preferred.network)
            put("label", preferred.label)
            put("address", preferred.address)
            if (preferred.memo.isNotBlank()) put("memo", preferred.memo)
            put("default", preferred.isDefault)
            put(
                "wallets",
                buildJsonArray {
                    candidates.forEach { w ->
                        add(kotlinx.serialization.json.buildJsonObject {
                            put("id", w.id)
                            put("label", w.label)
                            put("network", w.network)
                            put("address", w.address)
                            if (w.memo.isNotBlank()) put("memo", w.memo)
                            put("default", w.isDefault)
                        })
                    }
                },
            )
            put("handling", "Use this saved address for the withdrawal. Never substitute a different address or network.")
        }
    }
}

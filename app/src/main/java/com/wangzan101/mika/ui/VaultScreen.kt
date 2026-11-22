package com.wangzan101.mika.ui

import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.wangzan101.mika.R
import com.wangzan101.mika.chat.ConversationStore
import com.wangzan101.mika.core.trigger.TriggerDefinition
import com.wangzan101.mika.core.trigger.nextCalendarAt
import com.wangzan101.mika.core.vault.VAULT_DEFAULT_NETWORK
import com.wangzan101.mika.core.vault.VAULT_NETWORKS
import com.wangzan101.mika.core.vault.VaultSite
import com.wangzan101.mika.core.vault.VaultWallet
import com.wangzan101.mika.i18n.tr
import com.wangzan101.mika.task.TriggerRuntime
import com.wangzan101.mika.ui.components.BarIconButton
import com.wangzan101.mika.ui.components.ButtonKind
import com.wangzan101.mika.ui.components.ButtonSize
import com.wangzan101.mika.ui.components.EmptyNote
import com.wangzan101.mika.ui.components.FooterNote
import com.wangzan101.mika.ui.components.GroupCard
import com.wangzan101.mika.ui.components.GroupLabel
import com.wangzan101.mika.ui.components.KeyboardDismissArea
import com.wangzan101.mika.ui.components.ListRow
import com.wangzan101.mika.ui.components.MikaButton
import com.wangzan101.mika.ui.components.MikaCard
import com.wangzan101.mika.ui.components.MikaDialog
import com.wangzan101.mika.ui.components.MikaSwitch
import com.wangzan101.mika.ui.components.PillTone
import com.wangzan101.mika.ui.components.RowChevron
import com.wangzan101.mika.ui.components.SegmentedControl
import com.wangzan101.mika.ui.components.StatusPill
import com.wangzan101.mika.ui.components.SubPage
import com.wangzan101.mika.ui.components.keepsKeyboard
import com.wangzan101.mika.ui.theme.MikaIcons
import com.wangzan101.mika.ui.theme.MikaTheme
import com.wangzan101.mika.ui.theme.MikaType
import com.wangzan101.mika.vault.VaultStore
import java.text.SimpleDateFormat
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.Locale
import java.util.UUID

private val VAULT_CLOCK: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm", Locale.ROOT)

/**
 * The Vault: one place for the user's long-lived context. Website logins and
 * standing instructions, payout wallets, and the daily/weekend schedule that
 * runs the instructions. Everything is encrypted on this phone, and the agent
 * sees only names, URLs, logins, instructions and wallet addresses — never a
 * password until it asks for that one site.
 */
@Composable
fun VaultScreen(context: Context, store: VaultStore, onBack: () -> Unit) {
    BackHandler(onBack = onBack)
    val snapshot by store.snapshot.collectAsState()
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var editingSite by remember { mutableStateOf<VaultSite?>(null) }
    var editingWallet by remember { mutableStateOf<VaultWallet?>(null) }
    var addingSite by remember { mutableStateOf(false) }
    var addingWallet by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var note by remember { mutableStateOf<String?>(null) }
    var triggers by remember { mutableStateOf(TriggerRuntime.list(context)) }

    fun refreshPlans() {
        triggers = TriggerRuntime.list(context)
    }

    fun commitSite(site: VaultSite): String? {
        val clean = normalizeSite(site) ?: return "Name, URL and a valid HH:mm time are required."
        val scheduled = applySiteSchedule(context, clean)
        if (!store.upsertSite(scheduled.site)) return "The Vault could not be saved on this phone."
        refreshPlans()
        if (scheduled.site.scheduleEnabled && !scheduled.onTime) {
            note = "Saved. Alarms & reminders permission is off, so this schedule may run late. Turn it on in Scheduled tasks."
        }
        return null
    }

    fun deleteSite(site: VaultSite) {
        site.planId?.let { runCatching { TriggerRuntime.remove(context, it) } }
        store.deleteSite(site.id)
        refreshPlans()
    }

    fun commitWallet(wallet: VaultWallet): String? {
        if (wallet.label.isBlank()) return "Give this wallet a name."
        if (wallet.address.isBlank()) return "Paste the payout address."
        if (!store.upsertWallet(wallet)) return "The Vault could not be saved on this phone."
        return null
    }

    LaunchedEffect(Unit) { refreshPlans() }

    SubPage(
        "Vault", onBack,
        right = {
            BarIconButton(MikaIcons.Plus, "Add", {
                if (tab == 0) addingSite = true else addingWallet = true
            })
        },
    ) {
        VaultHero(
            sites = snapshot.sites.size,
            wallets = snapshot.wallets.size,
            scheduled = snapshot.sites.count { it.scheduleEnabled },
        )
        Spacer(Modifier.height(14.dp))
        SegmentedControl(
            listOf("Websites (${snapshot.sites.size})", "Wallets (${snapshot.wallets.size})"),
            tab,
            { tab = it },
        )
        error?.let {
            Text(it, Modifier.padding(top = 8.dp), style = MikaType.Caption, color = MikaTheme.colors.bad)
        }
        note?.let {
            Text(it, Modifier.padding(top = 8.dp), style = MikaType.Caption, color = MikaTheme.colors.warn)
        }
        if (tab == 0) {
            GroupLabel("SITES MIKA CAN USE")
            if (snapshot.sites.isEmpty()) {
                EmptyNote("No saved sites yet. Add a website, its login and the instructions Mika should follow there.")
                MikaButton(
                    "Add a website",
                    { addingSite = true },
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    kind = ButtonKind.Tonal,
                    size = ButtonSize.Medium,
                )
            } else {
                GroupCard {
                    snapshot.sites.forEachIndexed { i, site ->
                        val next = triggers.firstOrNull { it.id == site.planId }?.nextAtMs
                        SiteRow(
                            site, first = i == 0, nextAtMs = next,
                            onClick = { editingSite = site },
                        )
                    }
                }
                MikaButton(
                    "Add another website",
                    { addingSite = true },
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                    kind = ButtonKind.Tonal,
                    size = ButtonSize.Medium,
                )
            }
        } else {
            GroupLabel("PAYOUT WALLETS")
            if (snapshot.wallets.isEmpty()) {
                EmptyNote("No wallets yet. Add an address once and Mika will use it for withdrawals.")
                MikaButton(
                    "Add a wallet",
                    { addingWallet = true },
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    kind = ButtonKind.Tonal,
                    size = ButtonSize.Medium,
                )
            } else {
                GroupCard {
                    snapshot.wallets.forEachIndexed { i, wallet ->
                        WalletRow(wallet, first = i == 0, onClick = { editingWallet = wallet })
                    }
                }
                MikaButton(
                    "Add another wallet",
                    { addingWallet = true },
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                    kind = ButtonKind.Tonal,
                    size = ButtonSize.Medium,
                )
            }
        }
        FooterNote(
            "Encrypted on this phone. Mika can read the site name, login, instructions and wallet address in every task; " +
                "the password is released only for that one site and never appears in the chat or run history.",
        )
    }

    if (addingSite || editingSite != null) {
        SiteEditorDialog(
            initial = editingSite,
            onDismiss = { addingSite = false; editingSite = null },
            onDelete = editingSite?.let { site -> { deleteSite(site); editingSite = null } },
            onSave = { site ->
                val problem = commitSite(site)
                if (problem == null) {
                    addingSite = false
                    editingSite = null
                }
                problem
            },
        )
    }

    if (addingWallet || editingWallet != null) {
        WalletEditorDialog(
            initial = editingWallet,
            onDismiss = { addingWallet = false; editingWallet = null },
            onDelete = editingWallet?.let { wallet -> { store.deleteWallet(wallet.id); editingWallet = null } },
            onSave = { wallet ->
                val problem = commitWallet(wallet)
                if (problem == null) {
                    addingWallet = false
                    editingWallet = null
                }
                problem
            },
        )
    }
}

@Composable
private fun VaultHero(sites: Int, wallets: Int, scheduled: Int) {
    val c = MikaTheme.colors
    MikaCard {
        Column(Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 18.dp)) {
            Text("PRIVATE VAULT", style = MikaType.Label, color = c.accent)
            Text("Long-term memory", Modifier.padding(top = 6.dp), style = MikaType.Title, color = c.ink)
            Text(
                "Sites, standing instructions and payout addresses Mika keeps across every task.",
                Modifier.padding(top = 6.dp),
                style = MikaType.Caption,
                color = c.ink2,
            )
            Row(Modifier.padding(top = 16.dp), horizontalArrangement = Arrangement.spacedBy(26.dp)) {
                VaultStat("Sites", sites.toString())
                VaultStat("Wallets", wallets.toString())
                VaultStat("Scheduled", scheduled.toString())
            }
        }
    }
}

@Composable
private fun VaultStat(label: String, value: String) {
    val c = MikaTheme.colors
    Column {
        Text(value, style = MikaType.Title, color = c.ink)
        Text(label, style = MikaType.Hint, color = c.ink3)
    }
}

@Composable
private fun SiteRow(site: VaultSite, first: Boolean, nextAtMs: Long?, onClick: () -> Unit) {
    val host = site.url.substringAfter("://", site.url).substringBefore('/').removePrefix("www.")
    val schedule = when {
        !site.scheduleEnabled -> null
        site.scheduleWeekendsOnly -> "Weekends · ${site.scheduleTime}"
        else -> "Daily · ${site.scheduleTime}"
    }
    val detail = buildList {
        add(host)
        if (site.login.isNotBlank()) add(site.login)
        schedule?.let(::add)
        nextAtMs?.let { add("Next " + shortDate(it)) }
    }.joinToString(" · ")
    ListRow(
        MikaIcons.Globe,
        site.name.ifBlank { host },
        subtitle = detail,
        first = first,
        detail = site.instructions.takeIf { it.isNotBlank() }?.replace(Regex("\\s+"), " ")?.take(100),
        onClick = onClick,
    ) {
        if (schedule != null) StatusPill(schedule.substringBefore(" ·"), PillTone.Accent) else RowChevron()
    }
}

@Composable
private fun WalletRow(wallet: VaultWallet, first: Boolean, onClick: () -> Unit) {
    val shown = if (wallet.address.length <= 18) wallet.address
    else wallet.address.take(8) + "…" + wallet.address.takeLast(6)
    ListRow(
        MikaIcons.Key,
        wallet.label.ifBlank { "Wallet" },
        subtitle = "${wallet.network} · $shown",
        first = first,
        detail = listOfNotNull(
            wallet.memo.takeIf { it.isNotBlank() }?.let { "Memo: $it" },
            "Default for withdrawals".takeIf { wallet.isDefault },
        ).joinToString(" · ").ifBlank { null },
        onClick = onClick,
    ) {
        if (wallet.isDefault) StatusPill("Default", PillTone.Accent) else RowChevron()
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SiteEditorDialog(
    initial: VaultSite?,
    onDismiss: () -> Unit,
    onDelete: (() -> Unit)?,
    onSave: (VaultSite) -> String?,
) {
    val id = remember(initial?.id) { initial?.id ?: UUID.randomUUID().toString() }
    var name by remember(initial?.id) { mutableStateOf(initial?.name.orEmpty()) }
    var url by remember(initial?.id) { mutableStateOf(initial?.url.orEmpty()) }
    var login by remember(initial?.id) { mutableStateOf(initial?.login.orEmpty()) }
    var password by remember(initial?.id) { mutableStateOf(initial?.password.orEmpty()) }
    var instructions by remember(initial?.id) { mutableStateOf(initial?.instructions.orEmpty()) }
    var scheduleOn by remember(initial?.id) { mutableStateOf(initial?.scheduleEnabled ?: false) }
    var weekends by remember(initial?.id) { mutableStateOf(initial?.scheduleWeekendsOnly ?: false) }
    var time by remember(initial?.id) { mutableStateOf(initial?.scheduleTime ?: "09:00") }
    var reveal by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val c = MikaTheme.colors

    MikaDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "Add website" else "Edit website") },
        text = {
            KeyboardDismissArea {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    VaultField(name, { name = it }, "Name", "e.g. Binance", testTag = "vault-site-name")
                    Spacer(Modifier.height(8.dp))
                    VaultField(url, { url = it }, "Website URL", "https://example.com", KeyboardType.Uri, "vault-site-url")
                    Spacer(Modifier.height(8.dp))
                    VaultField(login, { login = it }, "Email / username", "you@example.com", KeyboardType.Email, "vault-site-login")
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        password,
                        { password = it },
                        Modifier.fillMaxWidth().keepsKeyboard(),
                        label = { Text("Password") },
                        singleLine = true,
                        visualTransformation = if (reveal) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        trailingIcon = {
                            IconButton(onClick = { reveal = !reveal }) {
                                Icon(if (reveal) MikaIcons.EyeOff else MikaIcons.Eye, if (reveal) "Hide password" else "Show password")
                            }
                        },
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        instructions,
                        { instructions = it },
                        Modifier.fillMaxWidth().keepsKeyboard(),
                        label = { Text("What should Mika do here?") },
                        placeholder = { Text("e.g. Check the balance and withdraw mining rewards to my LTC wallet.") },
                        minLines = 3,
                        maxLines = 6,
                    )
                    Spacer(Modifier.height(14.dp))
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("Run automatically", style = MikaType.Body, color = c.ink)
                            Text("Mika will sign in and follow the instructions.", style = MikaType.Hint, color = c.ink3)
                        }
                        MikaSwitch(scheduleOn, { scheduleOn = it })
                    }
                    if (scheduleOn) {
                        Spacer(Modifier.height(10.dp))
                        SegmentedControl(
                            listOf("Every day", "Weekends"),
                            if (weekends) 1 else 0,
                            { weekends = it == 1 },
                        )
                        Spacer(Modifier.height(10.dp))
                        VaultField(time, { time = it }, "Time (HH:mm)", "09:00", KeyboardType.Text, "vault-site-time")
                    }
                    error?.let {
                        Spacer(Modifier.height(8.dp))
                        Text(it, style = MikaType.Caption, color = c.bad)
                    }
                    onDelete?.let { delete ->
                        TextButton(
                            onClick = { delete(); onDismiss() },
                            modifier = Modifier.padding(top = 6.dp),
                        ) { Text("Delete website", color = c.bad) }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val clean = siteFromFields(id, initial, name, url, login, password, instructions, scheduleOn, weekends, time)
                val problem = onSave(clean)
                if (problem == null) onDismiss() else error = problem
            }) { Text(tr(R.string.save)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(tr(R.string.cancel)) } },
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun WalletEditorDialog(
    initial: VaultWallet?,
    onDismiss: () -> Unit,
    onDelete: (() -> Unit)?,
    onSave: (VaultWallet) -> String?,
) {
    val id = remember(initial?.id) { initial?.id ?: UUID.randomUUID().toString() }
    var label by remember(initial?.id) { mutableStateOf(initial?.label.orEmpty()) }
    var network by remember(initial?.id) { mutableStateOf(initial?.network ?: VAULT_DEFAULT_NETWORK) }
    var address by remember(initial?.id) { mutableStateOf(initial?.address.orEmpty()) }
    var memo by remember(initial?.id) { mutableStateOf(initial?.memo.orEmpty()) }
    var isDefault by remember(initial?.id) { mutableStateOf(initial?.isDefault ?: true) }
    var error by remember { mutableStateOf<String?>(null) }
    val c = MikaTheme.colors

    MikaDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "Add payout wallet" else "Edit payout wallet") },
        text = {
            KeyboardDismissArea {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    VaultField(label, { label = it }, "Name", "e.g. My LTC wallet", testTag = "vault-wallet-label")
                    Spacer(Modifier.height(8.dp))
                    Text("Network", style = MikaType.Label, color = c.ink3)
                    Spacer(Modifier.height(6.dp))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        VAULT_NETWORKS.forEach { option ->
                            MikaButton(
                                option,
                                { network = option },
                                kind = if (network == option) ButtonKind.Primary else ButtonKind.Tonal,
                                size = ButtonSize.Small,
                            )
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    VaultField(address, { address = it }, "Address", "Paste the full address", testTag = "vault-wallet-address")
                    Spacer(Modifier.height(8.dp))
                    VaultField(memo, { memo = it }, "Memo / tag (optional)", "Only if the network needs one", testTag = "vault-wallet-memo")
                    Spacer(Modifier.height(10.dp))
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text("Use for withdrawals by default", Modifier.weight(1f), style = MikaType.Body, color = c.ink)
                        MikaSwitch(isDefault, { isDefault = it })
                    }
                    error?.let {
                        Spacer(Modifier.height(8.dp))
                        Text(it, style = MikaType.Caption, color = c.bad)
                    }
                    onDelete?.let { delete ->
                        TextButton(
                            onClick = { delete(); onDismiss() },
                            modifier = Modifier.padding(top = 6.dp),
                        ) { Text("Delete wallet", color = c.bad) }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val problem = onSave(
                    (initial ?: VaultWallet(id = id, label = "", address = "")).copy(
                        label = label.trim(),
                        network = network,
                        address = address.trim(),
                        memo = memo.trim(),
                        isDefault = isDefault,
                    ),
                )
                if (problem == null) onDismiss() else error = problem
            }) { Text(tr(R.string.save)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(tr(R.string.cancel)) } },
    )
}

@Composable
private fun VaultField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    keyboardType: KeyboardType = KeyboardType.Text,
    testTag: String,
) {
    OutlinedTextField(
        value,
        onValueChange,
        Modifier.fillMaxWidth().keepsKeyboard(),
        label = { Text(label) },
        placeholder = { Text(placeholder) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
    )
}

private fun siteFromFields(
    id: String,
    initial: VaultSite?,
    name: String,
    url: String,
    login: String,
    password: String,
    instructions: String,
    scheduleOn: Boolean,
    weekends: Boolean,
    time: String,
): VaultSite = (initial ?: VaultSite(id = id, name = "", url = "")).copy(
    name = name.trim(),
    url = if (url.trim().contains("://")) url.trim() else "https://" + url.trim(),
    login = login.trim(),
    password = password,
    instructions = instructions.trim(),
    scheduleEnabled = scheduleOn,
    scheduleWeekendsOnly = weekends,
    scheduleTime = normalizeTime(time) ?: time.trim(),
)

private fun normalizeSite(site: VaultSite): VaultSite? {
    if (site.name.isBlank() || site.url.isBlank()) return null
    if (site.scheduleEnabled && normalizeTime(site.scheduleTime) == null) return null
    return site.copy(
        url = if (site.url.contains("://")) site.url else "https://" + site.url,
        scheduleTime = normalizeTime(site.scheduleTime) ?: "09:00",
    )
}

private fun normalizeTime(raw: String): String? = runCatching {
    VAULT_CLOCK.format(LocalTime.parse(raw.trim()))
}.getOrNull()

private data class ScheduledSite(val site: VaultSite, val onTime: Boolean)

/**
 * Saves a schedule through the same alarm engine the Scheduled tasks page uses.
 * The goal names the site and its instructions; the password is fetched by the
 * agent at run time, so it never sits in the unencrypted trigger file.
 */
private fun applySiteSchedule(context: Context, site: VaultSite): ScheduledSite {
    val oldPlan = site.planId
    if (!site.scheduleEnabled) {
        if (oldPlan != null) runCatching { TriggerRuntime.remove(context, oldPlan) }
        return ScheduledSite(site.copy(planId = null), true)
    }
    val time = normalizeTime(site.scheduleTime) ?: return ScheduledSite(site, false)
    val zone = ZoneId.systemDefault().id
    val days = if (site.scheduleWeekendsOnly) listOf(6, 7) else emptyList()
    val nextAt = runCatching { nextCalendarAt(time, zone, days, System.currentTimeMillis() - 1) }.getOrNull()
        ?: return ScheduledSite(site.copy(scheduleEnabled = false, planId = null), false)
    val planId = oldPlan ?: UUID.randomUUID().toString()
    val thread = site.threadId ?: ConversationStore.shared(context).newConversation()
    val goal = buildString {
        append("Use the saved Vault site \"").append(site.name).append("\" (").append(site.url).append("). ")
        append("Sign in with its saved login by calling vault_login, then follow these saved instructions: ")
        append(site.instructions.ifBlank { "open the account and report anything that needs attention" })
        append(". This task is scheduled ")
        append(if (site.scheduleWeekendsOnly) "on weekends" else "every day")
        append(" at ").append(time).append('.')
    }
    val result = runCatching {
        TriggerRuntime.schedule(
            context,
            TriggerDefinition(
                id = planId,
                threadId = thread,
                goal = goal,
                nextAtMs = nextAt,
                calendarTime = time,
                calendarZone = zone,
                calendarDays = days,
                enabled = true,
            ),
            replaceExisting = oldPlan != null,
        ).onTime
    }
    if (result.isFailure) return ScheduledSite(site.copy(planId = oldPlan), false)
    return ScheduledSite(site.copy(planId = planId, threadId = thread, scheduleTime = time), result.getOrDefault(false))
}

private fun shortDate(ms: Long): String =
    SimpleDateFormat("MMM d, HH:mm", Locale.getDefault()).format(Date(ms))

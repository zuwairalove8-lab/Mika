package com.wangzan101.mika.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.wangzan101.mika.R
import com.wangzan101.mika.settings.McpServerStore
import com.wangzan101.mika.settings.composio.*
import com.wangzan101.mika.ui.components.*
import com.wangzan101.mika.ui.theme.MikaIcons
import com.wangzan101.mika.ui.theme.MikaTheme
import com.wangzan101.mika.ui.theme.MikaType
import android.content.Intent
import android.net.Uri
import kotlinx.coroutines.launch

/** Composio MCP marketplace screen - browse connectors and connect external tools like Gmail, Slack, etc. */
@Composable
fun ComposioMarketplaceScreen(
    store: ComposioStore,
    mcpServerStore: McpServerStore,
    onBack: () -> Unit,
) {
    val apps by store.apps.collectAsState()
    val accounts by store.connectedAccounts.collectAsState()
    var loading by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedApp by remember { mutableStateOf<ComposioApp?>(null) }
    var authUrl by remember { mutableStateOf<String?>(null) }
    var connecting by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var success by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    PageScaffold(stringResource(R.string.mcp_servers_composio_marketplace_title), onBack) {
        Column(Modifier.fillMaxWidth()) {
            // Header
            Text(
                text = stringResource(R.string.mcp_servers_composio_marketplace_subtitle),
                style = MikaType.Body,
                color = MikaTheme.colors.ink3,
                modifier = Modifier.padding(bottom = 20.dp),
            )

            // Refresh + search row
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    label = { Text(stringResource(R.string.mcp_servers_composio_search)) },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                    leadingIcon = { Icon(MikaIcons.Search, null, Modifier.size(20.dp)) },
                )
                MikaButton(
                    text = stringResource(R.string.refresh),
                    onClick = {
                        loading = true
                        scope.launch {
                            store.refreshAll()
                            loading = false
                        }
                    },
                    size = ButtonSize.Small,
                    enabled = !loading,
                )
            }

            if (loading) {
                LinearProgressIndicator(Modifier.fillMaxWidth().padding(bottom = 16.dp))
            }

            error?.let { Text(it, color = MikaTheme.colors.bad, Modifier.padding(bottom = 16.dp)) }
            success?.let { Text(it, color = MikaTheme.colors.ok, Modifier.padding(bottom = 16.dp)) }

            // Popular connectors section
            GroupLabel(stringResource(R.string.mcp_servers_composio_popular_connectors))
            LazyColumn(
                modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
                state = rememberLazyListState(),
            ) {
                items(items = store.popularApps.filter { it.contains(searchQuery, ignoreCase = true) }) { appId ->
                    ConnectorRow(
                        appId = appId,
                        appName = appId.split("_").map { it.capitalize() }.joinToString(" "),
                        isConnected = accounts.any { it.appId == appId },
                        onConnect = {
                            selectedApp = null
                            scope.launch {
                                connecting = true
                                val result = store.connectApp(appId)
                                connecting = false
                                when (val r = result) {
                                    is Result.Success -> authUrl = r.value.redirectUrl
                                    is Result.Failure -> error = r.exception.message ?: stringResource(R.string.mcp_servers_composio_connect_failed)
                                }
                            }
                        },
                        onAddAsMcp = {
                            val account = accounts.firstOrNull { it.appId == appId }
                            scope.launch {
                                connecting = true
                                val result = store.addConnectorAsMcpServer(appId, appName, account?.id)
                                connecting = false
                                when (val r = result) {
                                    is Result.Success -> success = stringResource(R.string.mcp_servers_composio_added_as_mcp, appName)
                                    is Result.Failure -> error = r.exception.message ?: stringResource(R.string.mcp_servers_composio_add_failed)
                                }
                            }
                        },
                    )
                }
            }

            // Connected accounts section
            if (accounts.isNotEmpty()) {
                GroupLabel(stringResource(R.string.mcp_servers_composio_connected_accounts))
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    items(items = accounts) { account ->
                        ConnectedAccountRow(
                            account = account,
                            onAddAsMcp = {
                                scope.launch {
                                    connecting = true
                                    val result = store.addConnectorAsMcpServer(account.appId, account.appName, account.id)
                                    connecting = false
                                    when (val r = result) {
                                        is Result.Success -> success = stringResource(R.string.mcp_servers_composio_added_as_mcp, account.appName)
                                        is Result.Failure -> error = r.exception.message ?: stringResource(R.string.mcp_servers_composio_add_failed)
                                    }
                                }
                            },
                        )
                    }
                }
            }

            if (apps.isNotEmpty() && accounts.isEmpty()) {
                GroupLabel(stringResource(R.string.mcp_servers_composio_available_apps))
                Text(
                    text = stringResource(R.string.mcp_servers_composio_browse_apps),
                    style = MikaType.Caption,
                    color = MikaTheme.colors.ink3,
                    modifier = Modifier.padding(bottom = 12.dp),
                )
            }
        }
    }

    // Auth dialog - open browser for OAuth
    if (authUrl != null) {
        ComposioAuthDialog(
            authUrl = authUrl!!,
            onDismiss = { authUrl = null },
            onConnected = {
                authUrl = null
                scope.launch {
                    store.loadConnectedAccounts()
                }
            },
        )
    }

    // Connecting overlay
    if (connecting) {
        ComposioConnectingDialog(
            onDismiss = { connecting = false },
        )
    }
}

@Composable
private fun ConnectorRow(
    appId: String,
    appName: String,
    isConnected: Boolean,
    onConnect: () -> Unit,
    onAddAsMcp: () -> Unit,
) {
    val c = MikaTheme.colors
    val iconColor = if (isConnected) c.ok else c.accent
    val iconBg = if (isConnected) c.okSoft else c.accentSoft

    MikaCard(Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                Modifier.size(32.dp).background(iconBg, MikaShapes.Tile),
                contentAlignment = Alignment.Center,
            ) {
                Icon(MikaIcons.Link, null, Modifier.size(18.dp), tint = iconColor)
            }
            Column(Modifier.weight(1f).padding(vertical = 4.dp)) {
                Text(appName, style = MikaType.BodyStrong, color = c.ink)
                Text(appId, style = MikaType.Caption, color = c.ink3)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (!isConnected) {
                    TextButton(onClick = onConnect) {
                        Text(stringResource(R.string.mcp_servers_composio_connect))
                    }
                } else {
                    MikaButton(
                        text = stringResource(R.string.mcp_servers_composio_add_mcp),
                        onClick = onAddAsMcp,
                        size = ButtonSize.Small,
                    )
                }
            }
        }
    }
}

@Composable
private fun ConnectedAccountRow(
    account: ComposioConnectedAccount,
    onAddAsMcp: () -> Unit,
) {
    val c = MikaTheme.colors
    val statusTone = when (account.status) {
        "active" -> TileTone.Ok
        "error" -> TileTone.Bad
        "pending" -> TileTone.Warn
        else -> TileTone.Neutral
    }

    ListRow(
        icon = MikaIcons.Account,
        title = account.appName,
        subtitle = "${account.status} · ${account.authScheme ?: "oauth"}",
        tone = statusTone,
        trailing = {
            MikaButton(
                text = stringResource(R.string.mcp_servers_composio_add_mcp),
                onClick = onAddAsMcp,
                size = ButtonSize.Small,
            )
        },
    )
}

@Composable
private fun ComposioAuthDialog(
    authUrl: String,
    onDismiss: () -> Unit,
    onConnected: () -> Unit,
) {
    var opened by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    MikaDialog(
        onDismissRequest = { onDismiss() },
        title = { Text(stringResource(R.string.mcp_servers_composio_sign_in_title)) },
        text = {
            Column(Modifier.fillMaxWidth()) {
                Text(
                    text = stringResource(R.string.mcp_servers_composio_sign_in_body),
                    style = MikaType.Body,
                    color = MikaTheme.colors.ink2,
                    modifier = Modifier.padding(bottom = 16.dp),
                )
                MikaButton(
                    text = stringResource(R.string.mcp_servers_composio_open_browser),
                    onClick = {
                        opened = true
                        // Will be handled by MainActivity via intent
                    },
                    size = ButtonSize.Small,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onDismiss(); onConnected() }) {
                Text(stringResource(R.string.mcp_servers_composio_connected))
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )

    // Launch browser when opened becomes true
    if (opened) {
        val context = androidx.compose.ui.platform.LocalContext.current
        androidx.compose.runtime.LaunchedEffect(Unit) {
            context.startActivity(
                Intent(Intent.ACTION_VIEW, Uri.parse(authUrl))
                    .addCategory(Intent.CATEGORY_BROWSABLE)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            )
        }
    }
}

@Composable
private fun ComposioConnectingDialog(onDismiss: () -> Unit) {
    MikaDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.mcp_servers_composio_connecting)) },
        text = { Text(stringResource(R.string.mcp_servers_composio_please_wait)) },
        dismissButton = null,
        confirmButton = {},
    )
}

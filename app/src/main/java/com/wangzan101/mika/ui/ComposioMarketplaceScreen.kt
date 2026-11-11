package com.wangzan101.mika.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.wangzan101.mika.R
import com.wangzan101.mika.settings.McpServerStore
import com.wangzan101.mika.settings.composio.*
import com.wangzan101.mika.ui.components.*
import com.wangzan101.mika.ui.theme.MikaIcons
import com.wangzan101.mika.ui.theme.MikaShapes
import com.wangzan101.mika.ui.theme.MikaTheme
import com.wangzan101.mika.ui.theme.MikaType
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
    var authUrl by remember { mutableStateOf<String?>(null) }
    var connecting by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    var successMsg by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    PageScaffold(stringResource(R.string.mcp_servers_composio_marketplace_title), onBack) {
        Column(Modifier.fillMaxWidth()) {
            Text(
                text = stringResource(R.string.mcp_servers_composio_marketplace_subtitle),
                style = MikaType.Body,
                color = MikaTheme.colors.ink2,
                modifier = Modifier.padding(bottom = 24.dp),
            )

            // Search bar
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
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
                            try {
                                store.refreshAll()
                            } catch (e: Exception) {
                                errorMsg = e.message
                            }
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

            errorMsg?.let { et ->
                Text(
                    text = et,
                    style = MikaType.Caption,
                    color = MikaTheme.colors.bad,
                    modifier = Modifier.padding(horizontal = 16.dp, bottom = 16.dp),
                )
            }
            successMsg?.let { st ->
                Text(
                    text = st,
                    style = MikaType.Caption,
                    color = MikaTheme.colors.ok,
                    modifier = Modifier.padding(horizontal = 16.dp, bottom = 16.dp),
                )
            }

            val connectFailedMsg = stringResource(R.string.mcp_servers_composio_connect_failed)
            val addFailedMsg = stringResource(R.string.mcp_servers_composio_add_failed)

            GroupLabel(stringResource(R.string.mcp_servers_composio_popular_connectors))
            LazyColumn(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, bottom = 24.dp),
                state = rememberLazyListState(),
            ) {
                items(items = store.popularApps.filter { it.contains(searchQuery, ignoreCase = true) }) { appId ->
                    val appName = appId.split("_").map { it.capitalize() }.joinToString(" ")
                    val isConnected = accounts.any { it.appId == appId }
                    val addedAsMcpMsg = stringResource(R.string.mcp_servers_composio_added_as_mcp, appName)
                    ConnectorRow(
                        appId = appId,
                        appName = appName,
                        isConnected = isConnected,
                        onConnect = {
                            connecting = true
                            scope.launch {
                                val result = store.connectApp(appId)
                                connecting = false
                                if (result.isSuccess) {
                                    authUrl = result.getOrNull()?.redirectUrl
                                } else {
                                    errorMsg = result.exceptionOrNull()?.message ?: connectFailedMsg
                                }
                            }
                        },
                        onAddAsMcp = {
                            val account = accounts.firstOrNull { it.appId == appId }
                            connecting = true
                            scope.launch {
                                val result = store.addConnectorAsMcpServer(appId, appName, account?.id)
                                connecting = false
                                if (result.isSuccess) {
                                    successMsg = addedAsMcpMsg
                                } else {
                                    errorMsg = result.exceptionOrNull()?.message ?: addFailedMsg
                                }
                            }
                        },
                    )
                }
            }

            if (accounts.isNotEmpty()) {
                GroupLabel(stringResource(R.string.mcp_servers_composio_connected_accounts))
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, bottom = 24.dp),
                ) {
                    items(items = accounts) { account ->
                        val accountAddedMsg = stringResource(R.string.mcp_servers_composio_added_as_mcp, account.appName)
                        ConnectedAccountRow(
                            account = account,
                            onAddAsMcp = {
                                connecting = true
                                scope.launch {
                                    val result = store.addConnectorAsMcpServer(account.appId, account.appName, account.id)
                                    connecting = false
                                if (result.isSuccess) {
                                    successMsg = accountAddedMsg
                                } else {
                                    errorMsg = result.exceptionOrNull()?.message ?: addFailedMsg
                                }
                                }
                            },
                        )
                    }
                }
            }
        }
    }

    if (authUrl != null) {
        ComposioAuthDialog(
            authUrl = authUrl!!,
            onDismiss = { authUrl = null },
            onConnected = {
                authUrl = null
                scope.launch {
                    try {
                        store.loadConnectedAccounts()
                    } catch (e: Exception) {
                        error = e.message
                    }
                }
            },
        )
    }

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

    MikaCard(Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                Modifier.size(40.dp).background(iconBg, MikaShapes.Tile),
                contentAlignment = Alignment.Center,
            ) {
                Icon(MikaIcons.Link, null, Modifier.size(24.dp), tint = iconColor)
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
        icon = MikaIcons.Link,
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
    val context = androidx.compose.ui.platform.LocalContext.current
    var browserOpened by remember { mutableStateOf(false) }

    MikaDialog(
        onDismissRequest = { onDismiss() },
        title = { Text(stringResource(R.string.mcp_servers_composio_sign_in_title)) },
        text = {
            Column(Modifier.fillMaxWidth()) {
                Text(
                    text = stringResource(R.string.mcp_servers_composio_sign_in_body),
                    style = MikaType.Body,
                    color = MikaTheme.colors.ink2,
                    modifier = Modifier.padding(bottom = 20.dp),
                )
                MikaButton(
                    text = stringResource(R.string.mcp_servers_composio_open_browser),
                    onClick = {
                        browserOpened = true
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

    if (browserOpened) {
        androidx.compose.runtime.LaunchedEffect(authUrl) {
            try {
                context.startActivity(
                    Intent(Intent.ACTION_VIEW, Uri.parse(authUrl))
                        .addCategory(Intent.CATEGORY_BROWSABLE)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                )
            } catch (e: Exception) {
                errorMsg = "Could not open browser: ${e.message}"
            }
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

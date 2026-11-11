package com.wangzan101.mika.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
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
import com.wangzan101.mika.settings.McpServerConfig
import com.wangzan101.mika.settings.McpServerStore
import com.wangzan101.mika.ui.components.*
import com.wangzan101.mika.ui.theme.MikaIcons
import com.wangzan101.mika.ui.theme.MikaShapes
import com.wangzan101.mika.ui.theme.MikaTheme
import com.wangzan101.mika.ui.theme.MikaType
import androidx.compose.runtime.collectAsState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/** MCP Server Manager: add, test, and manage external MCP servers.
 * No API keys required - users connect any MCP server by URL or stdio command. */
@Composable
fun McpServerManagerScreen(
    store: McpServerStore,
    onBack: () -> Unit,
) {
    var newName by remember { mutableStateOf("") }
    var newUrl by remember { mutableStateOf("") }
    var newTransport by remember { mutableStateOf("streamable-http") }
    var adding by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    var successMsg by remember { mutableStateOf<String?>(null) }
    val servers by store.servers.collectAsState()
    val scope = rememberCoroutineScope()

    PageScaffold(stringResource(R.string.mcp_server_manager_title), onBack) {
        Column(Modifier.fillMaxWidth()) {
            // Add server card
            MikaCard(Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, bottom = 24.dp)) {
                Column(Modifier.padding(20.dp)) {
                    GroupLabel(stringResource(R.string.mcp_add_server))

                    OutlinedTextField(
                        value = newName,
                        onValueChange = { newName = it },
                        label = { Text(stringResource(R.string.mcp_server_name)) },
                        placeholder = { Text(stringResource(R.string.mcp_server_name_hint)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                    )

                    OutlinedTextField(
                        value = newUrl,
                        onValueChange = { newUrl = it },
                        label = { Text(stringResource(R.string.mcp_server_url_or_command)) },
                        placeholder = { Text(stringResource(R.string.mcp_server_url_hint)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        TextButton(
                            onClick = { newTransport = "streamable-http" },
                        ) {
                            Text(
                                text = stringResource(R.string.mcp_transport_http),
                                style = MikaType.Label,
                                color = if (newTransport == "streamable-http") MikaTheme.colors.accent else MikaTheme.colors.ink2,
                            )
                        }
                        TextButton(
                            onClick = { newTransport = "stdio" },
                        ) {
                            Text(
                                text = stringResource(R.string.mcp_transport_stdio),
                                style = MikaType.Label,
                                color = if (newTransport == "stdio") MikaTheme.colors.accent else MikaTheme.colors.ink2,
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        MikaButton(
                            text = stringResource(if (adding) R.string.mcp_adding else R.string.mcp_add_server_btn),
                            onClick = {
                                val name = newName.trim()
                                val url = newUrl.trim()
                                if (name.isEmpty()) {
                                    errorMsg = stringResource(R.string.mcp_name_required)
                                    return@MikaButton
                                }
                                if (url.isEmpty()) {
                                    errorMsg = stringResource(R.string.mcp_url_required)
                                    return@MikaButton
                                }
                                adding = true
                                errorMsg = null
                                scope.launch {
                                    val config = McpServerConfig(
                                        name = name,
                                        url = url,
                                        transport = newTransport,
                                        command = if (newTransport == "stdio") url else "",
                                    )
                                    val result = store.add(config)
                                    adding = false
                                    when {
                                        result.isSuccess -> {
                                            successMsg = stringResource(R.string.mcp_server_added, name)
                                            newName = ""
                                            newUrl = ""
                                        }
                                        else -> {
                                            errorMsg = result.exceptionOrNull()?.message ?: stringResource(R.string.mcp_add_failed)
                                        }
                                    }
                                }
                            },
                            size = ButtonSize.Small,
                            enabled = !adding,
                        )
                    }

                    errorMsg?.let { et ->
                        Text(
                            text = et,
                            style = MikaType.Caption,
                            color = MikaTheme.colors.bad,
                            modifier = Modifier.padding(top = 12.dp),
                        )
                    }
                    successMsg?.let { st ->
                        Text(
                            text = st,
                            style = MikaType.Caption,
                            color = MikaTheme.colors.ok,
                            modifier = Modifier.padding(top = 12.dp),
                        )
                    }
                }
            }

            // Server list
            GroupLabel(stringResource(R.string.mcp_server_list))
            if (servers.isEmpty()) {
                MikaCard(Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, bottom = 24.dp)) {
                    Column(Modifier.padding(32.dp).fillMaxWidth()) {
                        Icon(
                            imageVector = MikaIcons.Link,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp).align(Alignment.CenterHorizontally),
                            tint = MikaTheme.colors.ink3,
                        )
                        Text(
                            text = stringResource(R.string.mcp_no_servers),
                            style = MikaType.Body,
                            color = MikaTheme.colors.ink2,
                            modifier = Modifier.padding(top = 16.dp).align(Alignment.CenterHorizontally),
                        )
                        Text(
                            text = stringResource(R.string.mcp_no_servers_subtitle),
                            style = MikaType.Caption,
                            color = MikaTheme.colors.ink3,
                            modifier = Modifier.padding(top = 8.dp).align(Alignment.CenterHorizontally),
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, bottom = 24.dp),
                ) {
                    items(items = servers) { server ->
                        McpServerRow(
                            server = server,
                            store = store,
                            scope = scope,
                            onStatusChanged = { errorMsg = null; successMsg = null },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun McpServerRow(
    server: McpServerConfig,
    store: McpServerStore,
    scope: CoroutineScope,
    onStatusChanged: () -> Unit,
) {
    var testing by remember { mutableStateOf(false) }
    var testResult by remember { mutableStateOf<String?>(null) }
    val health by store.health.collectAsState()
    val serverHealth: com.wangzan101.mika.settings.McpHealth? = health[server.id]

    val statusColor = when {
        serverHealth?.ok == true -> MikaTheme.colors.ok
        serverHealth?.ok == false -> MikaTheme.colors.bad
        else -> MikaTheme.colors.ink3
    }
    val statusText = when {
        serverHealth?.ok == true -> stringResource(R.string.mcp_status_connected)
        serverHealth?.ok == false -> serverHealth.message ?: stringResource(R.string.mcp_status_error)
        else -> stringResource(R.string.mcp_status_unknown)
    }

    MikaCard(Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
        Row(
            Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                Modifier.size(40.dp).background(MikaTheme.colors.accentSoft, MikaShapes.Tile),
                contentAlignment = Alignment.Center,
            ) {
                Icon(MikaIcons.Link, null, Modifier.size(24.dp), tint = MikaTheme.colors.accent)
            }
            Column(Modifier.weight(1f).padding(vertical = 4.dp)) {
                Text(server.name, style = MikaType.BodyStrong, color = MikaTheme.colors.ink)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        text = server.transport,
                        style = MikaType.Caption,
                        color = MikaTheme.colors.ink3,
                    )
                    Text(
                        text = "· ${server.url.take(40)}",
                        style = MikaType.Caption,
                        color = MikaTheme.colors.ink3,
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (!testing) {
                    MikaButton(
                        text = stringResource(R.string.mcp_test),
                        onClick = {
                            testing = true
                            testResult = null
                            scope.launch {
                                val result = store.test(server)
                                testing = false
                                when {
                                    result.isSuccess -> {
                                        testResult = stringResource(R.string.mcp_test_ok, result.getOrNull()?.tools?.size ?: 0)
                                    }
                                    else -> {
                                        testResult = result.exceptionOrNull()?.message ?: stringResource(R.string.mcp_test_failed)
                                    }
                                }
                                onStatusChanged()
                            }
                        },
                        size = ButtonSize.Small,
                    )
                } else {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 3.dp,
                        color = MikaTheme.colors.accent,
                    )
                }
                if (server.enabled) {
                    TextButton(onClick = { store.setEnabled(server.id, false); onStatusChanged() }) {
                        Text(
                            text = stringResource(R.string.mcp_disable),
                            style = MikaType.Label,
                            color = MikaTheme.colors.ink2,
                        )
                    }
                } else {
                    TextButton(onClick = { store.setEnabled(server.id, true); onStatusChanged() }) {
                        Text(
                            text = stringResource(R.string.mcp_enable),
                            style = MikaType.Label,
                            color = MikaTheme.colors.ok,
                        )
                    }
                }
                TextButton(onClick = { store.remove(server.id); onStatusChanged() }) {
                    Text(
                        text = stringResource(R.string.mcp_remove),
                        style = MikaType.Label,
                        color = MikaTheme.colors.bad,
                    )
                }
            }
        }
        Divider()
        Row(
            Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, bottom = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier.size(8.dp).background(statusColor, MikaShapes.Pill),
            )
            Text(
                text = statusText,
                style = MikaType.Caption,
                color = statusColor,
            )
            testResult?.let { tr ->
                Text(
                    text = "· $tr",
                    style = MikaType.Caption,
                    color = MikaTheme.colors.ink3,
                )
            }
        }
    }
}

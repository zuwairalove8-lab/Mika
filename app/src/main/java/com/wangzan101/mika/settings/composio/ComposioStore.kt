package com.wangzan101.mika.settings.composio

import com.wangzan101.mika.settings.McpServerConfig
import com.wangzan101.mika.settings.McpServerStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Manages Composio marketplace connectors and links them as MCP servers in [McpServerStore].
 * Users can browse Composio apps, connect their accounts via OAuth, and add connectors
 * as MCP servers that the AI agent can use just like ChatGPT plugins.
 */
class ComposioStore(
    private val apiClient: ComposioApiClient,
    private val mcpStore: McpServerStore,
) {
    private val _apps = MutableStateFlow<List<ComposioApp>>(emptyList())
    val apps: StateFlow<List<ComposioApp>> = _apps.asStateFlow()

    private val _connectedAccounts = MutableStateFlow<List<ComposioConnectedAccount>>(emptyList())
    val connectedAccounts: StateFlow<List<ComposioConnectedAccount>> = _connectedAccounts.asStateFlow()

    private var loadingApps = false
    private var loadingAccounts = false

    suspend fun loadApps() = withContext(Dispatchers.IO) {
        if (loadingApps) return@withContext
        loadingApps = true
        try {
            val result = apiClient.listApps()
            result.onSuccess { _apps.value = it.items }
        } finally {
            loadingApps = false
        }
    }

    suspend fun loadConnectedAccounts() = withContext(Dispatchers.IO) {
        if (loadingAccounts) return@withContext
        loadingAccounts = true
        try {
            val result = apiClient.listConnectedAccounts()
            result.onSuccess { _connectedAccounts.value = it.items }
        } finally {
            loadingAccounts = false
        }
    }

    /** Initialize OAuth for a Composio app. Returns the auth URL to open in a browser. */
    suspend fun connectApp(appId: String, userId: String? = null): Result<ComposioAuthInit> {
        return apiClient.initAuth(appId, userId)
    }

    /** Add a connected Composio account as an MCP server. */
    suspend fun addConnectorAsMcpServer(
        appId: String,
        appName: String,
        connectedAccountId: String?,
    ): Result<McpServerConfig> = withContext(Dispatchers.IO) {
        val config = McpServerConfig(
            name = "Composio · $appName",
            url = "https://mcp.composio.dev/$appId",
            transport = "streamable-http",
            auth = "token",
            headers = mapOf(
                "X-Composio-Api-Key" to apiClient.apiKey,
                "X-Composio-Connected-Account-Id" to (connectedAccountId ?: ""),
            ),
            allowedTools = null,
        )
        val saveResult = mcpStore.add(config, apiClient.apiKey)
        saveResult
    }

    /** Refresh both apps and connected accounts. */
    suspend fun refreshAll() = coroutineScope {
        launch { loadApps() }
        launch { loadConnectedAccounts() }
    }

    /** Popular apps that users commonly connect (Gmail, Slack, GitHub, etc.). */
    val popularApps: List<String> = listOf(
        "gmail", "slack", "github", "google_calendar", "google_drive", "notion",
        "linear", "trello", "salesforce", "hubspot", "stripe", "twitter",
        "linkedin", "discord", "telegram", "whatsapp", "zendesk", "jira",
        "confluence", "dropbox", "box", "onedrive", "sharepoint",
        "shopify", "woocommerce", "mailchimp", "sendgrid", "twilio",
        "airtable", "clickup", "asana", "monday", "figma", "miro",
        "zoom", "webex", "calendly", "hubspot", "pipedrive",
        "postgres", "mysql", "mongodb", "redis", "s3", "bigquery",
        "aws", "gcp", "azure", "vercel", "netlify", "cloudflare",
        "stripe", "paypal", "quickbooks", "xero", "freshdesk",
        "intercom", "drift", "crisp", "tawk.to", "helpscout",
        "surveymonkey", "typeform", "google_forms", "airtable",
        "webflow", "framer", "shopify", "woocommerce", "magento",
        "bigcommerce", "prestashop", "opencart", "drupal", "joomla",
        "wordpress", "ghost", "medium", "substack", "ghost",
        "strapi", "sanity", "contentful", "strapi", "sanity",
    )
}

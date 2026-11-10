package com.wangzan101.mika.settings.composio

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class ComposioConnectedAccount(
    val id: String,
    val appName: String,
    val appId: String,
    val connectedAt: String?,
    val status: String,
    val linkedUserId: String?,
    val authScheme: String?,
    val scopes: List<String>?,
    val metadata: Map<String, JsonElement>?,
)

@Serializable
data class ComposioTool(
    val id: String,
    val name: String,
    val description: String?,
    val appName: String?,
    val appId: String?,
    val inputParameters: Map<String, JsonElement>?,
    val outputParameters: Map<String, JsonElement>?,
    val tags: List<String>?,
    val isConnected: Boolean?,
    val connectedAccountId: String?,
)

@Serializable
data class ComposioApp(
    val id: String,
    val name: String,
    val description: String?,
    val category: String?,
    val logo: String?,
    val authScheme: String?,
    val isPopular: Boolean?,
    val toolCount: Int?,
)

@Serializable
data class ComposioAuthInit(
    val redirectUrl: String,
    val state: String,
)

@Serializable
data class ComposioConnectedAppsResponse(
    val items: List<ComposioConnectedAccount>,
    val total: Int,
)

@Serializable
data class ComposioAppsResponse(
    val items: List<ComposioApp>,
    val total: Int,
)

@Serializable
data class ComposioToolsResponse(
    val items: List<ComposioTool>,
    val total: Int,
)

@Serializable
class ComposioError(
    val message: String?,
    val code: String?,
    val statusCode: Int?,
)

@Serializable
data class ComposioToolExecutionRequest(
    val toolName: String,
    val arguments: Map<String, JsonElement>,
    val connectedAccountId: String?,
)

@Serializable
data class ComposioToolExecutionResponse(
    val success: Boolean?,
    val data: Map<String, JsonElement>?,
    val error: String?,
)

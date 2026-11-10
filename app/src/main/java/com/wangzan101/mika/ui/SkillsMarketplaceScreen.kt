package com.wangzan101.mika.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.wangzan101.mika.ui.components.*
import com.wangzan101.mika.ui.theme.MikaIcons
import com.wangzan101.mika.ui.theme.MikaShapes
import com.wangzan101.mika.ui.theme.MikaTheme
import com.wangzan101.mika.ui.theme.MikaType
import kotlinx.coroutines.launch

/** 技能市集:从任意来源(网址、GitHub、本地文件)导入技能,也可浏览内置技能。 */
@Composable
fun SkillsMarketplaceScreen(
    onBack: () -> Unit,
    onSkillImported: (String) -> Unit = {},
) {
    var importSource by remember { mutableStateOf("") }
    var importing by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var success by remember { mutableStateOf<String?>(null) }
    var selectedTab by remember { mutableStateOf(0) }
    val scope = rememberCoroutineScope()

    PageScaffold(stringResource(R.string.skills_marketplace_title), onBack) {
        Column(Modifier.fillMaxWidth()) {
            Text(
                text = stringResource(R.string.skills_marketplace_subtitle),
                style = MikaType.Body,
                color = MikaTheme.colors.ink3,
                modifier = Modifier.padding(bottom = 20.dp),
            )

            // Tabs
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(0.dp),
            ) {
                TabItem(
                    text = stringResource(R.string.skills_tab_builtin),
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                )
                TabItem(
                    text = stringResource(R.string.skills_tab_import),
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                )
                TabItem(
                    text = stringResource(R.string.skills_tab_my_skills),
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                )
            }

            val importSuccessMsg = stringResource(R.string.skills_import_success)
            if (selectedTab == 0) {
                BuiltinSkillsList()
            } else if (selectedTab == 1) {
                ImportSkillSection(
                    source = importSource,
                    onSourceChanged = { importSource = it },
                    importing = importing,
                    onImport = {
                        importing = true
                        scope.launch {
                            // TODO: call skill import API
                            importing = false
                            success = importSuccessMsg
                        }
                    },
                    error = error,
                    success = success,
                )
            } else {
                MySkillsList()
            }

            error?.let { et ->
                Text(et, Modifier.padding(bottom = 16.dp))
            }
            success?.let { st ->
                Text(st, Modifier.padding(bottom = 16.dp))
            }
        }
    }
}

@Composable
private fun TabItem(text: String, selected: Boolean, onClick: () -> Unit) {
    val c = MikaTheme.colors
    Row(
        Modifier.fillMaxWidth()
            .then(if (selected) Modifier.border(2.dp, c.accent, MikaShapes.Pill) else Modifier)
            .clickable(onClick = onClick),
        horizontalArrangement = Arrangement.Center,
    ) {
        Text(
            text = text,
            style = if (selected) MikaType.BodyStrong else MikaType.Body,
            color = if (selected) c.accent else c.ink2,
        )
    }
}

@Composable
private fun BuiltinSkillsList() {
    val builtinSkills = listOf(
        BuiltinSkill("messaging", "Messaging", "Send messages via SMS, WhatsApp, Telegram, and more", MikaIcons.Message),
        BuiltinSkill("info-research", "Information Research", "Search the web, summarize pages, and compile reports", MikaIcons.Globe),
        BuiltinSkill("documents", "Documents", "Read, write, and edit PDFs, DOCX, and text files", MikaIcons.Doc),
        BuiltinSkill("content-transcribe", "Content Transcribe", "Transcribe audio and video to text", MikaIcons.Mic),
        BuiltinSkill("configure-mika", "Configure Mika", "Set up providers, models, and app preferences", MikaIcons.Gear),
    )

    GroupLabel(stringResource(R.string.skills_builtin_skills))
    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
    ) {
        items(items = builtinSkills) { skill ->
            BuiltinSkillRow(skill = skill)
        }
    }
}

@Composable
private fun BuiltinSkillRow(skill: BuiltinSkill) {
    val c = MikaTheme.colors
    MikaCard(Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                Modifier.size(32.dp).background(c.accentSoft, MikaShapes.Tile),
                contentAlignment = Alignment.Center,
            ) {
                Icon(skill.icon, null, Modifier.size(20.dp), tint = c.accent)
            }
            Column(Modifier.weight(1f).padding(vertical = 4.dp)) {
                Text(skill.name, style = MikaType.BodyStrong, color = c.ink)
                Text(skill.description, style = MikaType.Caption, color = c.ink3, maxLines = 2)
            }
            MikaButton(
                text = stringResource(R.string.skills_add),
                onClick = { /* TODO: add skill */ },
                size = ButtonSize.Small,
            )
        }
    }
}

@Composable
private fun MySkillsList() {
    GroupLabel(stringResource(R.string.skills_my_skills))
    Text(
        text = stringResource(R.string.skills_no_my_skills_yet),
        style = MikaType.Caption,
        color = MikaTheme.colors.ink3,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
    )
}

@Composable
private fun ImportSkillSection(
    source: String,
    onSourceChanged: (String) -> Unit,
    importing: Boolean,
    onImport: () -> Unit,
    error: String?,
    success: String?,
) {
    val c = MikaTheme.colors
    GroupLabel(stringResource(R.string.skills_import_from_any_source))

    MikaCard(Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
        Column(Modifier.padding(16.dp)) {
            Text(
                text = stringResource(R.string.skills_import_subtitle),
                style = MikaType.Body,
                color = c.ink2,
                modifier = Modifier.padding(bottom = 16.dp),
            )
            OutlinedTextField(
                value = source,
                onValueChange = onSourceChanged,
                label = { Text(stringResource(R.string.skills_skill_url)) },
                placeholder = { Text(stringResource(R.string.skills_skill_url_placeholder)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                MikaButton(
                    text = stringResource(if (importing) R.string.skills_importing else R.string.skills_import_button),
                    onClick = onImport,
                    size = ButtonSize.Small,
                    enabled = !importing && source.isNotBlank(),
                )
                TextButton(onClick = { /* TODO: browse local file */ }) {
                    Text(stringResource(R.string.skills_browse_file))
                }
            }
            if (source.isNotBlank() && !source.startsWith("http")) {
                Text(
                    text = stringResource(R.string.skills_url_must_be_http),
                    style = MikaType.Caption,
                    color = c.warn,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
        }
    }

    GroupLabel(stringResource(R.string.skills_supported_sources))
    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
    ) {
        items(items = listOf(
            SupportedSource("GitHub", "https://github.com/user/repo/blob/main/SKILL.md", MikaIcons.Link),
            SupportedSource("Any URL", "https://example.com/skill/SKILL.md", MikaIcons.Link),
            SupportedSource("Local file", "file:///storage/emulated/0/Documents/skill/SKILL.md", MikaIcons.Doc),
        )) { source ->
            SupportedSourceRow(source = source)
        }
    }
}

@Composable
private fun SupportedSourceRow(source: SupportedSource) {
    val c = MikaTheme.colors
    ListRow(
        icon = source.icon,
        title = source.name,
        subtitle = source.example,
        trailing = { RowChevron() },
    )
}

@Composable
private fun SupportedSourceRowPreview() = Unit

private data class BuiltinSkill(
    val id: String,
    val name: String,
    val description: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
)

private data class SupportedSource(
    val name: String,
    val example: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
)

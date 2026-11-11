package com.wangzan101.mika.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.wangzan101.mika.R
import com.wangzan101.mika.ui.components.*
import com.wangzan101.mika.ui.theme.MikaIcons
import com.wangzan101.mika.ui.theme.MikaShapes
import com.wangzan101.mika.ui.theme.MikaTheme
import com.wangzan101.mika.ui.theme.MikaType
import kotlinx.coroutines.launch

/** 技能市集:按类别浏览内置技能,或从任意来源(网址、本地文件)导入技能。 */
@Composable
fun SkillsMarketplaceScreen(
    onBack: () -> Unit,
    onSkillImported: (String) -> Unit = {},
) {
    var importSource by remember { mutableStateOf("") }
    var importing by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var success by remember { mutableStateOf<String?>(null) }
    var selectedCategory by remember { mutableStateOf("builtin") }
    val scope = rememberCoroutineScope()
    val importSuccessMsg = stringResource(R.string.skills_import_success)

    PageScaffold(stringResource(R.string.skills_marketplace_title), onBack) {
        Column(Modifier.fillMaxWidth()) {
            Text(
                text = stringResource(R.string.skills_marketplace_subtitle),
                style = MikaType.Body,
                color = MikaTheme.colors.ink2,
                modifier = Modifier.padding(bottom = 24.dp),
            )

            // Category chips
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, bottom = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                CategoryChip(
                    text = stringResource(R.string.skills_category_builtin),
                    selected = selectedCategory == "builtin",
                    onClick = { selectedCategory = "builtin" },
                )
                CategoryChip(
                    text = stringResource(R.string.skills_category_installed),
                    selected = selectedCategory == "installed",
                    onClick = { selectedCategory = "installed" },
                )
                CategoryChip(
                    text = stringResource(R.string.skills_category_import),
                    selected = selectedCategory == "import",
                    onClick = { selectedCategory = "import" },
                )
            }

            when (selectedCategory) {
                "builtin" -> BuiltinSkillsSection()
                "installed" -> MySkillsList()
                "import" -> ImportSkillSection(
                    source = importSource,
                    onSourceChanged = { importSource = it },
                    importing = importing,
                    onImport = {
                        importing = true
                        scope.launch {
                            importing = false
                            success = importSuccessMsg
                        }
                    },
                    error = error,
                    success = success,
                )
            }

            error?.let { et ->
                Text(
                    text = et,
                    style = MikaType.Caption,
                    color = MikaTheme.colors.bad,
                    modifier = Modifier.padding(horizontal = 16.dp, bottom = 16.dp),
                )
            }
            success?.let { st ->
                Text(
                    text = st,
                    style = MikaType.Caption,
                    color = MikaTheme.colors.ok,
                    modifier = Modifier.padding(horizontal = 16.dp, bottom = 16.dp),
                )
            }
        }
    }
}

@Composable
private fun CategoryChip(text: String, selected: Boolean, onClick: () -> Unit) {
    val c = MikaTheme.colors
    Box(
        Modifier
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .background(
                if (selected) c.accentSoft else Color.Transparent,
                MikaShapes.Pill
            ),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = MikaType.Label,
            color = if (selected) c.accent else c.ink2,
        )
    }
}

@Composable
private fun BuiltinSkillsSection() {
    val builtinSkills = listOf(
        BuiltinSkill("messaging", stringResource(R.string.skill_messaging), stringResource(R.string.skill_messaging_desc), MikaIcons.Message),
        BuiltinSkill("info-research", stringResource(R.string.skill_info_research), stringResource(R.string.skill_info_research_desc), MikaIcons.Globe),
        BuiltinSkill("documents", stringResource(R.string.skill_documents), stringResource(R.string.skill_documents_desc), MikaIcons.Doc),
        BuiltinSkill("content-transcribe", stringResource(R.string.skill_transcribe), stringResource(R.string.skill_transcribe_desc), MikaIcons.Mic),
        BuiltinSkill("configure-jobby", stringResource(R.string.skill_configure), stringResource(R.string.skill_configure_desc), MikaIcons.Gear),
        BuiltinSkill("web-search", stringResource(R.string.skill_web_search), stringResource(R.string.skill_web_search_desc), MikaIcons.Link),
        BuiltinSkill("terminal", stringResource(R.string.skill_terminal), stringResource(R.string.skill_terminal_desc), MikaIcons.Terminal),
        BuiltinSkill("file-manager", stringResource(R.string.skill_file_manager), stringResource(R.string.skill_file_manager_desc), MikaIcons.Desktop),
    )

    GroupLabel(stringResource(R.string.skills_builtin_skills))
    LazyColumn(
        modifier = Modifier.padding(horizontal = 16.dp),
    ) {
        items(items = builtinSkills) { skill ->
            BuiltinSkillRow(skill = skill)
        }
    }
}

@Composable
private fun BuiltinSkillRow(skill: BuiltinSkill) {
    val c = MikaTheme.colors
    MikaCard(Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                Modifier.size(40.dp).background(c.accentSoft, MikaShapes.Tile),
                contentAlignment = Alignment.Center,
            ) {
                Icon(skill.icon, null, Modifier.size(24.dp), tint = c.accent)
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
    MikaCard(Modifier.fillMaxWidth().padding(horizontal = 16.dp, bottom = 24.dp)) {
        Column(Modifier.padding(24.dp)) {
            Icon(
                imageVector = MikaIcons.Star,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MikaTheme.colors.ink3,
            )
            Text(
                text = stringResource(R.string.skills_no_my_skills_yet),
                style = MikaType.Body,
                color = MikaTheme.colors.ink2,
                modifier = Modifier.padding(top = 16.dp),
            )
            Text(
                text = stringResource(R.string.skills_no_my_skills_subtitle),
                style = MikaType.Caption,
                color = MikaTheme.colors.ink3,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
    }
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

    MikaCard(Modifier.fillMaxWidth().padding(horizontal = 16.dp, bottom = 24.dp)) {
        Column(Modifier.padding(20.dp)) {
            Text(
                text = stringResource(R.string.skills_import_subtitle),
                style = MikaType.Body,
                color = c.ink2,
                modifier = Modifier.padding(bottom = 20.dp),
            )
            OutlinedTextField(
                value = source,
                onValueChange = onSourceChanged,
                label = { Text(stringResource(R.string.skills_skill_url), style = MikaType.Body) },
                placeholder = { Text(stringResource(R.string.skills_skill_url_placeholder), style = MikaType.Caption) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(bottom = 20.dp),
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                MikaButton(
                    text = stringResource(if (importing) R.string.skills_importing else R.string.skills_import_button),
                    onClick = onImport,
                    size = ButtonSize.Small,
                    enabled = !importing && source.isNotBlank(),
                )
                TextButton(onClick = { /* TODO: browse local file */ }) {
                    Text(stringResource(R.string.skills_browse_file), style = MikaType.Body)
                }
            }
            if (source.isNotBlank() && !source.startsWith("http")) {
                Text(
                    text = stringResource(R.string.skills_url_must_be_http),
                    style = MikaType.Caption,
                    color = c.warn,
                    modifier = Modifier.padding(top = 12.dp),
                )
            }
        }
    }

    GroupLabel(stringResource(R.string.skills_supported_sources))
    LazyColumn(
        modifier = Modifier.padding(horizontal = 16.dp),
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
        tone = TileTone.Neutral,
        trailing = { RowChevron() },
    )
}

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

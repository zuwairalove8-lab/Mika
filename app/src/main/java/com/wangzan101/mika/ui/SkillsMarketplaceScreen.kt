package com.wangzan101.mika.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.wangzan101.mika.R
import com.wangzan101.mika.core.knowledge.GuideCatalog
import com.wangzan101.mika.core.knowledge.GuideInfo
import com.wangzan101.mika.ui.components.GroupCard
import com.wangzan101.mika.ui.components.GroupLabel
import com.wangzan101.mika.ui.components.ListRow
import com.wangzan101.mika.ui.components.PageScaffold
import com.wangzan101.mika.ui.theme.MikaIcons
import com.wangzan101.mika.ui.theme.MikaShapes
import com.wangzan101.mika.ui.theme.MikaTheme
import com.wangzan101.mika.ui.theme.MikaType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 技能中心:已收下的个人技能(开关即启用/停用)、等你确认的草稿、内置技能、导入入口。
 *
 * 用的是真正的技能库([MemoryViewModel] + [SkillsSection])和内置清单([GuideCatalog]),
 * 这里只做分组和入口,不再有另一套「点了没反应」的假数据。
 */
@Composable
fun SkillsMarketplaceScreen(vm: MemoryViewModel, onBack: () -> Unit) {
    val ui by vm.ui.collectAsState()
    var tab by remember { mutableStateOf(Tab.Installed) }
    val draftCount = ui.skills.drafts.size

    PageScaffold(stringResource(R.string.skills_marketplace_title), onBack) {
        Text(
            text = stringResource(R.string.skills_marketplace_subtitle),
            style = MikaType.Body,
            color = MikaTheme.colors.ink2,
            modifier = Modifier.padding(bottom = 20.dp),
        )

        Row(
            modifier = Modifier.padding(bottom = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            CategoryChip(stringResource(R.string.skills_category_installed), tab == Tab.Installed) { tab = Tab.Installed }
            CategoryChip(stringResource(R.string.skills_category_builtin), tab == Tab.Builtin) { tab = Tab.Builtin }
            if (draftCount > 0) {
                CategoryChip(
                    "${stringResource(R.string.skills_pending_chip)} ($draftCount)",
                    tab == Tab.Pending,
                ) { tab = Tab.Pending }
            }
            CategoryChip(stringResource(R.string.skills_category_import), tab == Tab.Import) { tab = Tab.Import }
        }

        if (tab == Tab.Builtin) {
            BuiltinSkillsSection()
        } else when (tab) {
            // 「导入」打开的就是「我的技能」那一份导入框,取回来先进「待确认」
            Tab.Import -> SkillsSection(vm, ui.skills, pending = false, startImport = true)
            Tab.Pending -> SkillsSection(vm, ui.skills, pending = true)
            else -> SkillsSection(vm, ui.skills, pending = false)
        }
    }
}

private enum class Tab { Installed, Builtin, Pending, Import }

@Composable
private fun CategoryChip(text: String, selected: Boolean, onClick: () -> Unit) {
    val c = MikaTheme.colors
    Box(
        Modifier
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .background(if (selected) c.accentSoft else Color.Transparent, MikaShapes.Pill),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = text, style = MikaType.Label, color = if (selected) c.accent else c.ink2)
    }
}

/** 内置技能:读的是随包自带的清单(和 Agent 用 [GuideCatalog] 查到的是同一份),没有「加」这一步。 */
@Composable
private fun BuiltinSkillsSection() {
    val skills by produceState<List<GuideInfo>?>(null) {
        value = withContext(Dispatchers.IO) {
            runCatching { GuideCatalog.loadBundled().items.filter { it.kind == "skill" } }.getOrDefault(emptyList())
        }
    }
    GroupLabel(stringResource(R.string.skills_builtin_skills))
    val list = skills
    GroupCard {
        when {
            list == null -> ListRow(MikaIcons.Star, stringResource(R.string.skills_builtin_loading), first = true)
            list.isEmpty() -> ListRow(MikaIcons.Star, stringResource(R.string.skills_builtin_none), first = true)
            else -> list.forEachIndexed { i, s ->
                ListRow(MikaIcons.Star, s.display, s.description.ifBlank { null }, first = i == 0)
            }
        }
    }
}

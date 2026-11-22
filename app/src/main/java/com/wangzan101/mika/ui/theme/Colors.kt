package com.wangzan101.mika.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * 界面用到的全部颜色。页面一律从 [MikaTheme.colors] 取,不写死色值。
 * 实色只用在强调(主按钮、选中、开关、用户消息)和状态(成功、需要你、出错);图标块一律中性。
 */
@Immutable
class MikaColors(
    /** 页面背景。 */
    val bg: Color,
    /** 卡片、分组。 */
    val surface: Color,
    /** 浅填充:次按钮、分段控件底、代码块底。 */
    val fill: Color,
    /** 文字三级:正文、次要、辅助。 */
    val ink: Color,
    val ink2: Color,
    val ink3: Color,
    /** 分隔线。 */
    val hair: Color,
    /** 卡片描边。 */
    val edge: Color,
    val accent: Color,
    val accentSoft: Color,
    val accent2: Color,
    val accent2Soft: Color,
    /** 强调色上的文字和图标。 */
    val onAccent: Color,
    val ok: Color,
    val okSoft: Color,
    /** 「需要你」:[warn] 用于文字,[warnSolid] 用于圆点、描边。 */
    val warn: Color,
    val warnSolid: Color,
    val warnSoft: Color,
    val bad: Color,
    val badSoft: Color,
    /** 图标块的底。 */
    val neutralSoft: Color,
    /** Jobby 回答的底:比页面背景深一点点的灰,和用户的紫色气泡区分开。 */
    val answer: Color,
    /** 灰色胶囊、关着的开关。 */
    val greySoft: Color,
    /** 悬浮玻璃(输入栏、浮在内容上的按钮)的底和描边。 */
    val glass: Color,
    val glassEdge: Color,
    /** 卡片阴影;深色下卡片只靠描边,为透明。 */
    val cardShadow: Color,
    /** 悬浮元素的阴影。 */
    val floatShadow: Color,
    /** 开关的圆钮。 */
    val thumb: Color,
    /**
     * 图表里分类的颜色(例如每家服务商一种),按固定的先后取;超出的归到 [seriesOther]。
     * 只给图形上色,文字一律用 ink 三级。
     */
    val series: List<Color>,
    val seriesOther: Color,
    /** 图表的网格线。 */
    val grid: Color,
    val isDark: Boolean,
)

/** Modern light theme - cold graphite, steel blue and teal. No warm/purple tints. */
val LightColors = MikaColors(
    bg = Color(0xFFF2F5F8),
    surface = Color(0xFFFFFFFF),
    fill = Color(0x0D0B1220),
    ink = Color(0xFF0B1220),
    ink2 = Color(0xFF566274),
    ink3 = Color(0xFF8792A2),
    hair = Color(0x1A0B1220),
    edge = Color(0x120B1220),
    accent = Color(0xFF3568C9),      // Steel blue
    accentSoft = Color(0x243568C9),
    accent2 = Color(0xFF0E8C8C),     // Cold teal
    accent2Soft = Color(0x240E8C8C),
    onAccent = Color(0xFFFFFFFF),
    ok = Color(0xFF0E8A6A),
    okSoft = Color(0x1F0E8A6A),
    warn = Color(0xFFB26A12),
    warnSolid = Color(0xFFB26A12),
    warnSoft = Color(0x24B26A12),
    bad = Color(0xFFC24343),
    badSoft = Color(0x24C24343),
    neutralSoft = Color(0x0A0B1220),
    answer = Color(0xFFEDF1F5),      // Cool grey assistant bubble
    greySoft = Color(0x1F566274),
    glass = Color(0xC7FFFFFF),
    glassEdge = Color(0x120B1220),
    cardShadow = Color(0x1A0B1220),
    floatShadow = Color(0x240B1220),
    thumb = Color(0xFFFFFFFF),
    series = listOf(Color(0xFF3568C9), Color(0xFF0E8C8C), Color(0xFF6B7A90)),
    seriesOther = Color(0xFFAEB8C5),
    grid = Color(0x0F0B1220),
    isDark = false,
)

/** Modern dark theme - near-black blue-grey with an ice-blue accent. */
val DarkColors = MikaColors(
    bg = Color(0xFF080C12),
    surface = Color(0xFF101720),
    fill = Color(0x0FFFFFFF),
    ink = Color(0xFFE6ECF4),
    ink2 = Color(0xFF98A5B6),
    ink3 = Color(0xFF647184),
    hair = Color(0x24FFFFFF),
    edge = Color(0x1AFFFFFF),
    accent = Color(0xFF8AB4FF),      // Ice blue for dark
    accentSoft = Color(0x338AB4FF),
    accent2 = Color(0xFF5EEAD4),     // Mint teal
    accent2Soft = Color(0x2A5EEAD4),
    onAccent = Color(0xFF08121F),
    ok = Color(0xFF4ADE9F),
    okSoft = Color(0x244ADE9F),
    warn = Color(0xFFF0B35A),
    warnSolid = Color(0xFFF0B35A),
    warnSoft = Color(0x2AF0B35A),
    bad = Color(0xFFFF8A8A),
    badSoft = Color(0x24FF8A8A),
    neutralSoft = Color(0x12FFFFFF),
    answer = Color(0xFF121A24),
    greySoft = Color(0x33556677),
    glass = Color(0xB8101720),
    glassEdge = Color(0x2AFFFFFF),
    cardShadow = Color.Transparent,
    floatShadow = Color(0x33000000),
    thumb = Color(0xFFFFFFFF),
    series = listOf(Color(0xFF8AB4FF), Color(0xFF5EEAD4), Color(0xFF8A9AAF)),
    seriesOther = Color(0xFF5A6678),
    grid = Color(0x12FFFFFF),
    isDark = true,
)

/** 没有包在 [MikaTheme] 里时(比如界面测试)按浅色。 */
val LocalMikaColors = staticCompositionLocalOf { LightColors }

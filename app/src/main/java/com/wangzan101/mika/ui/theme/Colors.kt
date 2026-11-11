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

/** Premium light theme - Royal Purple & Gold */
val LightColors = MikaColors(
    bg = Color(0xFFFAF7F2),
    surface = Color(0xFFFFFFFF),
    fill = Color(0x0F000000),
    ink = Color(0xFF1A1A2E),
    ink2 = Color(0xFF6B6B8A),
    ink3 = Color(0xFFB8B8C8),
    hair = Color(0x1A000000),
    edge = Color(0x0F000000),
    accent = Color(0xFF7B2CBF),      // Royal Purple
    accentSoft = Color(0x2A7B2CBF),
    accent2 = Color(0xFFD4AF37),     // Gold
    accent2Soft = Color(0x2AD4AF37),
    onAccent = Color(0xFFFFFFFF),
    ok = Color(0xFF10A37F),
    okSoft = Color(0x1F10A37F),
    warn = Color(0xFFC8961E),
    warnSolid = Color(0xFFD4AF37),
    warnSoft = Color(0x2AD4AF37),
    bad = Color(0xFFC44536),
    badSoft = Color(0x2AC44536),
    neutralSoft = Color(0x0B000000),
    answer = Color(0xFFF5F1E8),      // Warm cream assistant bubble
    greySoft = Color(0x1F767680),
    glass = Color(0xB8FFFFFF),
    glassEdge = Color(0x0F000000),
    cardShadow = Color(0xFF000000),
    floatShadow = Color(0xFF000000),
    thumb = Color(0xFFFFFFFF),
    series = listOf(Color(0xFF7B2CBF), Color(0xFFD4AF37), Color(0xFF10A37F)),
    seriesOther = Color(0xFFB4B5BC),
    grid = Color(0x0F000000),
    isDark = false,
)

/** Premium dark theme - Deep Purple & Gold */
val DarkColors = MikaColors(
    bg = Color(0xFF120A1E),          // Deep purple dark background
    surface = Color(0xFF1E122E),     // Surface
    fill = Color(0x0FFFFFFF),
    ink = Color(0xFFF5F1E8),
    ink2 = Color(0xFFB8B8C8),
    ink3 = Color(0xFF6B6B8A),
    hair = Color(0x2AFFFFFF),
    edge = Color(0x1AFFFFFF),
    accent = Color(0xFF9D65FF),      // Light purple for dark
    accentSoft = Color(0x3A9D65FF),
    accent2 = Color(0xFFD4AF37),     // Gold
    accent2Soft = Color(0x3AD4AF37),
    onAccent = Color(0xFF1A1A2E),
    ok = Color(0xFF10A37F),
    okSoft = Color(0x2410A37F),
    warn = Color(0xFFD4AF37),
    warnSolid = Color(0xFFD4AF37),
    warnSoft = Color(0x2AD4AF37),
    bad = Color(0xFFE06C5C),
    badSoft = Color(0x24E06C5C),
    neutralSoft = Color(0x12FFFFFF),
    answer = Color(0xFF1E122E),      // Assistant bubble dark purple
    greySoft = Color(0x38767680),
    glass = Color(0xB81E122E),
    glassEdge = Color(0x2AFFFFFF),
    cardShadow = Color.Transparent,
    floatShadow = Color(0xFF000000),
    thumb = Color(0xFFFFFFFF),
    series = listOf(Color(0xFF9D65FF), Color(0xFFD4AF37), Color(0xFF10A37F)),
    seriesOther = Color(0xFF5A5B62),
    grid = Color(0x12FFFFFF),
    isDark = true,
)

/** 没有包在 [MikaTheme] 里时(比如界面测试)按浅色。 */
val LocalMikaColors = staticCompositionLocalOf { LightColors }

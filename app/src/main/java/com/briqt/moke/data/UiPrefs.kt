package com.briqt.moke.data

/** 应用明暗主题：跟随系统 / 强制浅色 / 强制深色。 */
enum class ThemeMode {
    SYSTEM, LIGHT, DARK;

    companion object {
        fun fromName(n: String?, def: ThemeMode = SYSTEM): ThemeMode =
            entries.firstOrNull { it.name == n } ?: def
    }
}

/**
 * 终端软键盘模式——决定 `TerminalView.onCreateInputConnection` 上报的 inputType。
 * 部分厂商（如荣耀/华为）见到"密码变体"输入框会强制切到系统安全键盘，导致中文候选不可用，
 * 故把它做成可切换项而非硬编码。
 *
 * - [SECURE]   字符模式：`TYPE_TEXT_VARIATION_VISIBLE_PASSWORD | NO_SUGGESTIONS`，逐字符提交，
 *              不学习不纠错；最省心，但可能触发厂商安全键盘。
 * - [STANDARD] 标准：`TYPE_NULL`（termux 上游默认），多数输入法直通按键。
 * - [IME]      输入法优先：普通文本框（`TYPE_CLASS_TEXT`），完整候选词/联想/纠错，
 *              中文输入最顺；代价是输入法可能自动纠错。
 */
enum class KeyboardMode {
    SECURE, STANDARD, IME;

    companion object {
        fun fromName(n: String?, def: KeyboardMode = SECURE): KeyboardMode =
            entries.firstOrNull { it.name == n } ?: def
    }
}

/**
 * 全屏（备用屏）程序内的滑动语义。社区实报：codex / claude code / snow-cli 里上下滑动被当成
 * ↑/↓，翻的是命令历史而不是看输出。
 *
 * - [SMART]  智能（默认）：不在备用屏 → 滚本地 scrollback（远端开了鼠标跟踪则发滚轮）。
 *            在备用屏里，只要本地还留着已经滚出屏幕的输出，滑动就滚这段历史，不发方向键
 *            （tmux 回看已显示内容）。滚轮只在「正在跟踪鼠标且没有本地历史」时转发。
 *            没有历史又判不出来（mosh 空缓冲、括号粘贴的行编辑器、less）则什么都不发。
 * - [WHEEL]  始终发滚轮：远端支持鼠标但没在当前界面开启跟踪时有用。
 * - [ARROWS] 始终发方向键：翻页器手感（优先于以上一切判定，包括本地历史）。
 */
enum class ScrollMode {
    SMART, WHEEL, ARROWS;

    companion object {
        fun fromName(n: String?, def: ScrollMode = SMART): ScrollMode =
            entries.firstOrNull { it.name == n } ?: def
    }
}

/** 文件页排序维度。目录始终排在文件前面，本枚举只决定同类之间怎么比。 */
enum class FilesSort { NAME, TIME, SIZE }

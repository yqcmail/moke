package com.termux.view;

/**
 * 滑动/滚轮该怎么处理的**纯决策**（moke 扩展，无 Android 依赖，可被 JVM 单测覆盖）。
 *
 * <p>抽出来的原因：这段判断踩过坑（远端一开鼠标就短路掉用户选的模式；用户选的模式又漏到
 * 主屏幕去；tmux 备用屏里滑动被当成方向键，已经显示过的内容回看不了）。做成纯函数就能把
 * 全部组合钉死在单测里。
 */
public final class MokeScroll {

    private MokeScroll() {}

    /** 「全屏程序内滑动」设置项的三档，与 {@code com.briqt.moke.data.ScrollMode} 的 ordinal 对齐。 */
    public static final int MODE_SMART = 0;
    public static final int MODE_WHEEL = 1;
    public static final int MODE_ARROWS = 2;

    /** 发方向键（上/下）。 */
    public static final int ACTION_ARROWS = 0;
    /** 发滚轮鼠标事件。 */
    public static final int ACTION_WHEEL = 1;
    /** 滚本地 scrollback（不发任何字节到远端）。 */
    public static final int ACTION_LOCAL = 2;
    /** 什么都不发，并告诉用户为什么无处可滚。 */
    public static final int ACTION_NONE = 3;

    /**
     * @param mode             见 {@code MODE_*}；用户在「设置 → 终端与输入 → 全屏程序内滑动」里选的档
     * @param fullScreen       远端当前在备用屏（真正的全屏程序，tmux 常用）
     * @param mouseTracking    远端开启了鼠标跟踪
     * @param moshSession      本会话走 mosh
     * @param bracketedPaste   远端开启了括号粘贴模式（行编辑型 TUI 的标志）
     * @param hasLocalHistory  当前缓冲里已经有滚出屏幕的本地历史（用户要回看的就是这段）
     */
    public static int decide(
        int mode,
        boolean fullScreen,
        boolean mouseTracking,
        boolean moshSession,
        boolean bracketedPaste,
        boolean hasLocalHistory
    ) {
        // 不在全屏程序里 = 主屏幕。这里有**真正可滚的本地 scrollback**，而发方向键会去翻命令历史、
        // 发滚轮会把 \033[M… 原样打进命令行（TerminalEmulator.sendMouseEvent 不校验跟踪是否开启）。
        // 所以「全屏程序内滑动」这个设置项**只在全屏程序内**生效——名字即边界。
        // 唯一例外是远端确实开了鼠标跟踪：那是远端主动要接管滚轮，本地历史也就没有意义。
        if (!fullScreen) {
            return mouseTracking ? ACTION_WHEEL : ACTION_LOCAL;
        }
        // 用户显式选定的档位优先于自动判定——包括压过鼠标跟踪，否则远端一开鼠标，
        // 「始终发方向键」就形同虚设（0.1.19 修过一次的老账）。
        // 也压过本地历史：这是用户点名要的手感（翻页器 / 强制滚轮），不是智能档的猜测。
        if (mode == MODE_ARROWS) return ACTION_ARROWS;
        if (mode == MODE_WHEEL) return ACTION_WHEEL;
        // 以下都是智能档。
        // 已经有本地历史时，滑动就是在回看已经显示过的内容。发方向键是这次要修的 bug
        // （tmux 备用屏里上下滑变成翻页/翻命令历史）。滚轮也只在「正在跟踪鼠标且没有
        // 本地历史可读」时才转发——有历史就不把这一滑让给远端。
        if (hasLocalHistory) return ACTION_LOCAL;
        if (mouseTracking) return ACTION_WHEEL;
        // 没有本地历史、远端也没要鼠标。判不出来方向键是否安全（mosh 不传滚动缓冲、
        // 括号粘贴的行编辑器、以及 less 这类备用屏），一律不发。方向键不再是默认。
        // 参数保留在判断里，避免以后改签名时把这两种「判不出来」的情况漏掉。
        if (moshSession || bracketedPaste) return ACTION_NONE;
        return ACTION_NONE;
    }
}

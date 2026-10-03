package com.briqt.moke.terminal

import com.briqt.moke.data.ScrollMode
import com.termux.view.MokeScroll
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * 「全屏程序内滑动」三档的完整决策表。
 *
 * 这里钉死的核心不变量是：**设置项只在备用屏（全屏程序）里生效**。主屏幕有真正可滚的
 * scrollback，往那儿发方向键会翻命令历史、发滚轮会把 `\033[M…` 打进命令行。
 *
 * 智能档的另一条不变量：只要本地还有已经显示过的历史，滑动就滚这段历史，不发方向键。
 * 全屏且没有鼠标跟踪时默认发方向键是旧行为（tmux 里回看不了已显示内容），不再成立。
 */
class MokeScrollTest {

    private fun decide(
        mode: ScrollMode,
        fullScreen: Boolean,
        mouseTracking: Boolean = false,
        mosh: Boolean = false,
        bracketedPaste: Boolean = false,
        hasLocalHistory: Boolean = false,
    ) = MokeScroll.decide(
        mode.ordinal,
        fullScreen,
        mouseTracking,
        mosh,
        bracketedPaste,
        hasLocalHistory,
    )

    @Test
    fun `settings ordinals match the view contract`() {
        assertEquals(MokeScroll.MODE_SMART, ScrollMode.SMART.ordinal)
        assertEquals(MokeScroll.MODE_WHEEL, ScrollMode.WHEEL.ordinal)
        assertEquals(MokeScroll.MODE_ARROWS, ScrollMode.ARROWS.ordinal)
    }

    @Test
    fun `main screen always scrolls local history regardless of mode`() {
        for (mode in ScrollMode.entries) {
            assertEquals(
                "mode=$mode",
                MokeScroll.ACTION_LOCAL,
                decide(mode, fullScreen = false),
            )
        }
    }

    @Test
    fun `main screen still honours a remote that asked for the mouse`() {
        for (mode in ScrollMode.entries) {
            assertEquals(
                "mode=$mode",
                MokeScroll.ACTION_WHEEL,
                decide(mode, fullScreen = false, mouseTracking = true),
            )
        }
    }

    @Test
    fun `bracketed paste on the main screen still scrolls local history`() {
        // 行编辑型 TUI 的括号粘贴标志只在备用屏里才有「判不出来」的含义；
        // 主屏幕上的 shell 也开括号粘贴，那里滑动必须滚历史。
        assertEquals(
            MokeScroll.ACTION_LOCAL,
            decide(ScrollMode.SMART, fullScreen = false, bracketedPaste = true),
        )
    }

    @Test
    fun `explicit modes win over mouse tracking inside full-screen programs`() {
        assertEquals(
            MokeScroll.ACTION_ARROWS,
            decide(ScrollMode.ARROWS, fullScreen = true, mouseTracking = true),
        )
        assertEquals(
            MokeScroll.ACTION_WHEEL,
            decide(ScrollMode.WHEEL, fullScreen = true, mouseTracking = false),
        )
    }

    @Test
    fun `explicit arrow mode still sends arrows even when local history exists`() {
        // 「始终发方向键」是用户点名要的翻页器手感，不被智能档的修复覆盖。
        assertEquals(
            MokeScroll.ACTION_ARROWS,
            decide(ScrollMode.ARROWS, fullScreen = true, hasLocalHistory = true),
        )
    }

    @Test
    fun `smart mode forwards the wheel only when the remote tracks the mouse and nothing local is readable`() {
        assertEquals(
            MokeScroll.ACTION_WHEEL,
            decide(ScrollMode.SMART, fullScreen = true, mouseTracking = true, hasLocalHistory = false),
        )
        assertEquals(
            MokeScroll.ACTION_LOCAL,
            decide(ScrollMode.SMART, fullScreen = true, mouseTracking = true, hasLocalHistory = true),
        )
    }

    @Test
    fun `smart mode sends nothing when it cannot tell`() {
        assertEquals(MokeScroll.ACTION_NONE, decide(ScrollMode.SMART, fullScreen = true, mosh = true))
        assertEquals(
            MokeScroll.ACTION_NONE,
            decide(ScrollMode.SMART, fullScreen = true, bracketedPaste = true),
        )
        // 备用屏、没开鼠标、也没有本地历史：不再默认发方向键（旧的翻页器猜测）。
        assertEquals(MokeScroll.ACTION_NONE, decide(ScrollMode.SMART, fullScreen = true))
    }

    @Test
    fun `smart mode scrolls local history instead of sending arrows`() {
        // tmux 备用屏：输出已经滚出屏幕，滑动要回看这段，而不是发上下键。
        assertEquals(
            MokeScroll.ACTION_LOCAL,
            decide(ScrollMode.SMART, fullScreen = true, hasLocalHistory = true),
        )
        assertEquals(
            MokeScroll.ACTION_LOCAL,
            decide(
                ScrollMode.SMART,
                fullScreen = true,
                bracketedPaste = true,
                hasLocalHistory = true,
            ),
        )
        // mosh 不携带远端 scrollback；但本机已经画出来又滚走的行仍然可以本地回看。
        assertEquals(
            MokeScroll.ACTION_LOCAL,
            decide(ScrollMode.SMART, fullScreen = true, mosh = true, hasLocalHistory = true),
        )
    }

    @Test
    fun `arrow mode still works over mosh where every screen looks full-screen`() {
        // mosh-client 自己就在备用屏上，所以 mosh 会话里这档在任何界面都生效——
        // 设置说明里承诺的「尤其是 mosh 会话」正是靠这一点成立。没有本地历史时才发方向键。
        assertEquals(
            MokeScroll.ACTION_ARROWS,
            decide(ScrollMode.ARROWS, fullScreen = true, mosh = true),
        )
    }
}

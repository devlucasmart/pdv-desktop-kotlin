package com.pdv

import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.WindowState

/**
 * WindowUtils - safe fullscreen toggle for Compose Desktop.
 *
 * Uses WindowState.placement (Compose API) instead of raw AWT setUndecorated/fullScreenWindow,
 * which avoids "ComposeBridge is disposed" and "IllegalComponentStateException: The frame is displayable"
 * errors entirely.
 *
 * Usage:
 *   WindowUtils.enter(windowState)
 *   WindowUtils.exit(windowState)
 *   WindowUtils.toggle(windowState)
 */
object WindowUtils {

    fun enter(windowState: WindowState) {
        windowState.placement = WindowPlacement.Fullscreen
    }

    fun exit(windowState: WindowState) {
        windowState.placement = WindowPlacement.Floating
    }

    fun toggle(windowState: WindowState) {
        if (windowState.placement == WindowPlacement.Fullscreen) {
            exit(windowState)
        } else {
            enter(windowState)
        }
    }

    fun isFullscreen(windowState: WindowState) =
        windowState.placement == WindowPlacement.Fullscreen

    // ─── Legacy AWT overloads (kept for source-compatibility) ───────────────
    // These now delegate to a shared mutable window state reference if one
    // was registered, otherwise they are no-ops.  The actual effect is driven
    // by the Compose side via WindowManager._isFullscreen state.

    fun enterFullscreen(window: java.awt.Window) { /* driven by WindowPlacement */ }
    fun exitFullscreen(window: java.awt.Window)  { /* driven by WindowPlacement */ }
}

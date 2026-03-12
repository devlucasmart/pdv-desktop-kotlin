package com.pdv

import java.awt.EventQueue
import java.awt.Frame
import java.awt.GraphicsEnvironment
import java.awt.Rectangle
import java.awt.Window
import java.util.WeakHashMap

private data class StoredWindowState(
    val bounds: Rectangle,
    val extendedState: Int
)

object WindowUtils {
    // Use WeakHashMap to avoid memory leaks when windows are disposed
    private val states = WeakHashMap<Window, StoredWindowState>()
    @Volatile
    private var isTransitioning = false

    private fun findDeviceForWindow(window: Window) = run {
        try {
            val env = GraphicsEnvironment.getLocalGraphicsEnvironment()
            val devices = env.screenDevices
            if (devices.size <= 1) return@run env.defaultScreenDevice

            val winBounds = window.bounds
            var best: java.awt.GraphicsDevice? = null
            var bestArea = -1
            devices.forEach { d ->
                val bounds = d.defaultConfiguration.bounds
                val inter = bounds.intersection(winBounds)
                val area = if (inter.isEmpty) 0 else inter.width * inter.height
                if (area > bestArea) { bestArea = area; best = d }
            }
            return@run (best ?: env.defaultScreenDevice)
        } catch (t: Throwable) {
            t.printStackTrace()
            return@run GraphicsEnvironment.getLocalGraphicsEnvironment().defaultScreenDevice
        }
    }

    fun enterFullscreen(window: Window) {
        if (isTransitioning) return
        isTransitioning = true
        EventQueue.invokeLater {
            try {
                val device = findDeviceForWindow(window)

                // Save previous state
                val prevBounds = window.bounds
                val prevExtended = (window as? Frame)?.extendedState ?: Frame.NORMAL
                states[window] = StoredWindowState(prevBounds, prevExtended)

                if (device.isFullScreenSupported) {
                    // Use true exclusive fullscreen when supported
                    try {
                        device.fullScreenWindow = window
                    } catch (e: Throwable) {
                        // fallback: maximize
                        (window as? Frame)?.extendedState = Frame.MAXIMIZED_BOTH
                    }
                } else {
                    // Fallback: maximize
                    (window as? Frame)?.extendedState = Frame.MAXIMIZED_BOTH
                }

                // ensure visible
                try { window.isVisible = true } catch (_: Throwable) {}
            } catch (t: Throwable) {
                t.printStackTrace()
            } finally {
                try { Thread.sleep(120) } catch (_: Exception) {}
                isTransitioning = false
            }
        }
    }

    fun exitFullscreen(window: Window) {
        if (isTransitioning) return
        isTransitioning = true
        EventQueue.invokeLater {
            try {
                val device = findDeviceForWindow(window)
                try {
                    if (device.fullScreenWindow == window) {
                        device.fullScreenWindow = null
                    }
                } catch (_: Throwable) {
                    // ignore
                }

                val prev = states.remove(window)

                // Restore state without disposing the window (avoid touching ComposeBridge)
                if (prev != null) {
                    try { window.bounds = prev.bounds } catch (_: Throwable) {}
                    try { (window as? Frame)?.extendedState = prev.extendedState } catch (_: Throwable) {}
                } else {
                    try { window.setSize(1200, 800) } catch (_: Throwable) {}
                    try { window.setLocationRelativeTo(null) } catch (_: Throwable) {}
                }

                try { window.isVisible = true } catch (_: Throwable) {}
            } catch (t: Throwable) {
                t.printStackTrace()
            } finally {
                try { Thread.sleep(120) } catch (_: Exception) {}
                isTransitioning = false
            }
        }
    }
}

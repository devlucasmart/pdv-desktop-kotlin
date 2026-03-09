package com.pdv.ui.theme

import androidx.compose.material.darkColors
import androidx.compose.material.lightColors
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color

// Paleta de cores - Tema Claro
object LightThemeColors {
    val primary = Color(0xFF1976D2)
    val primaryVariant = Color(0xFF1565C0)
    val secondary = Color(0xFF388E3C)
    val background = Color(0xFFF5F5F5)
    val surface = Color(0xFFFFFFFF)
    val error = Color(0xFFD32F2F)
    val onPrimary = Color(0xFFFFFFFF)
    val onSecondary = Color(0xFFFFFFFF)
    val onBackground = Color(0xFF000000)
    val onSurface = Color(0xFF000000)
    val onError = Color(0xFFFFFFFF)
}

// Paleta de cores - Tema Escuro
object DarkThemeColors {
    val primary = Color(0xFF90CAF9)
    val primaryVariant = Color(0xFF42A5F5)
    val secondary = Color(0xFF66BB6A)
    val background = Color(0xFF121212)
    val surface = Color(0xFF1E1E1E)
    val error = Color(0xFFEF5350)
    val onPrimary = Color(0xFF000000)
    val onSecondary = Color(0xFF000000)
    val onBackground = Color(0xFFFFFFFF)
    val onSurface = Color(0xFFFFFFFF)
    val onError = Color(0xFF000000)
}

// Gerenciador de tema
object ThemeManager {
    var isDarkTheme by mutableStateOf(false)
        private set

    fun toggleTheme() {
        isDarkTheme = !isDarkTheme
        println("✓ Tema alterado para: ${if (isDarkTheme) "Escuro" else "Claro"}")
    }

    fun setTheme(dark: Boolean) {
        isDarkTheme = dark
    }

    fun getLightColors() = lightColors(
        primary = LightThemeColors.primary,
        primaryVariant = LightThemeColors.primaryVariant,
        secondary = LightThemeColors.secondary,
        background = LightThemeColors.background,
        surface = LightThemeColors.surface,
        error = LightThemeColors.error,
        onPrimary = LightThemeColors.onPrimary,
        onSecondary = LightThemeColors.onSecondary,
        onBackground = LightThemeColors.onBackground,
        onSurface = LightThemeColors.onSurface,
        onError = LightThemeColors.onError
    )

    fun getDarkColors() = darkColors(
        primary = DarkThemeColors.primary,
        primaryVariant = DarkThemeColors.primaryVariant,
        secondary = DarkThemeColors.secondary,
        background = DarkThemeColors.background,
        surface = DarkThemeColors.surface,
        error = DarkThemeColors.error,
        onPrimary = DarkThemeColors.onPrimary,
        onSecondary = DarkThemeColors.onSecondary,
        onBackground = DarkThemeColors.onBackground,
        onSurface = DarkThemeColors.onSurface,
        onError = DarkThemeColors.onError
    )

    fun getCurrentColors() = if (isDarkTheme) getDarkColors() else getLightColors()
}


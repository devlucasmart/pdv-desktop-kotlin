package com.pdv.util

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

object NumberUtils {
    private val symbols = DecimalFormatSymbols(Locale("pt", "BR")).apply {
        decimalSeparator = ','
        groupingSeparator = '.'
    }
    private val df = DecimalFormat("#,##0.00", symbols)

    fun parseQuantity(value: String?): Double {
        if (value == null) return 0.0
        val t = value.trim()
        if (t.isEmpty()) return 0.0
        val cleaned = t.replace(".", "").replace(',', '.')
        return cleaned.toDoubleOrNull() ?: 0.0
    }

    fun formatQuantity(value: Double, unit: String?): String {
        if (unit == null || unit == "un") {
            return value.toInt().toString()
        }
        // show 2 decimals for fractional units
        return String.format("%.2f %s", value, unit)
    }

    // Format a decimal number using Brazilian format (e.g. 1234.5 -> "1.234,50")
    fun formatDecimalForInput(value: Double): String {
        return df.format(value)
    }
}

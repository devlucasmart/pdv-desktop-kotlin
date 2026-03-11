package com.pdv.util

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

object CurrencyUtils {
    private val symbols = DecimalFormatSymbols(Locale("pt", "BR")).apply {
        decimalSeparator = ','
        groupingSeparator = '.'
    }
    private val df = DecimalFormat("#,##0.00", symbols)

    fun format(value: Double): String {
        return "R$ ${df.format(value)}"
    }

    // formata apenas o valor (sem R$)
    fun formatPlain(value: Double): String {
        return df.format(value)
    }

    // Parseia uma string que pode conter separadores brasileiros ('.' para milhar e ',' para decimal)
    // Ex: "1.234,56" -> 1234.56
    fun parse(value: String?): Double {
        if (value == null) return 0.0
        val trimmed = value.trim()
        if (trimmed.isEmpty()) return 0.0
        // Remover qualquer caractere que não seja dígito, ponto ou vírgula
        val cleaned = trimmed.replace(Regex("[^0-9.,]"), "")
        // Normalizar: remover agrupadores de milhar e transformar vírgula em ponto
        val normalized = cleaned.replace(".", "").replace(',', '.')
        return normalized.toDoubleOrNull() ?: 0.0
    }

    // Formata uma string de entrada (possivelmente bruta) para a representação com separador brasileiro
    // Usado para atualizar o campo enquanto o usuário digita
    fun formatFromInput(input: String?): String {
        val value = parse(input)
        return formatPlain(value)
    }
}

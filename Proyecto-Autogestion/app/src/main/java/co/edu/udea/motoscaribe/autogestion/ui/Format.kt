package co.edu.udea.motoscaribe.autogestion.ui

import java.text.NumberFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Los montos son siempre pesos colombianos, sin importar el idioma del telefono. */
fun Long.cop(): String =
    NumberFormat.getCurrencyInstance(Locale("es", "CO"))
        .apply { maximumFractionDigits = 0 }
        .format(this)
        .replace(' ', ' ') // espacio no separable: '$ 250.000' no se parte en dos lineas

/** La fecha sigue el idioma del sistema (es/en). */
fun LocalDate.pretty(): String =
    format(DateTimeFormatter.ofPattern("d MMM yyyy", Locale.getDefault()))

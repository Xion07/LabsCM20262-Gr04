package co.edu.udea.motoscaribe.autogestion.ui

import java.text.NumberFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

private val co = Locale("es", "CO")
private val money = NumberFormat.getCurrencyInstance(co).apply { maximumFractionDigits = 0 }
private val dateFmt = DateTimeFormatter.ofPattern("d MMM yyyy", co)

fun Long.cop(): String = money.format(this)
fun LocalDate.pretty(): String = format(dateFmt)

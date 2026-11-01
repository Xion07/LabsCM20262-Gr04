package co.edu.udea.motoscaribe.autogestion.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import co.edu.udea.motoscaribe.autogestion.UiState
import co.edu.udea.motoscaribe.autogestion.data.ExtensionRequest
import co.edu.udea.motoscaribe.autogestion.ui.cop
import co.edu.udea.motoscaribe.autogestion.ui.pretty
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExtensionScreen(state: UiState, onSubmit: (ExtensionRequest) -> Unit) {
    val c = state.contract ?: return
    var excuse by rememberSaveable { mutableStateOf("") }
    var newDateEpoch by rememberSaveable { mutableStateOf<Long?>(null) }
    var showPicker by rememberSaveable { mutableStateOf(false) }
    var attachment by rememberSaveable { mutableStateOf<String?>(null) }
    val pickMedia = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) {
        attachment = it?.toString()
    }

    val newDate = newDateEpoch?.let { LocalDate.ofEpochDay(it) }
    val installment = "Cuota ${c.nextPaymentDate.pretty()}"

    Column(
        Modifier.verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(installment, style = MaterialTheme.typography.titleLarge)
        Text(c.nextPaymentAmount.cop(), style = MaterialTheme.typography.titleMedium)

        OutlinedButton({ showPicker = true }, Modifier.fillMaxWidth()) {
            Text(newDate?.pretty() ?: "Elegir nueva fecha de pago")
        }

        OutlinedTextField(
            value = excuse,
            onValueChange = { excuse = it },
            label = { Text("Motivo / excusa") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 3,
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Sentences,
                imeAction = ImeAction.Done,
            ),
        )

        OutlinedButton(
            onClick = {
                pickMedia.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            },
            modifier = Modifier.fillMaxWidth(),
        ) { Text(if (attachment == null) "Adjuntar soporte (foto)" else "Soporte adjunto ✓ (cambiar)") }

        Button(
            onClick = { onSubmit(ExtensionRequest(installment, newDate, excuse.trim(), attachment)) },
            enabled = newDate != null && excuse.isNotBlank(),
            modifier = Modifier.fillMaxWidth(),
        ) { Text("Solicitar ampliación de plazo") }

        OutlinedButton(
            onClick = { onSubmit(ExtensionRequest(installment, null, excuse.trim(), attachment)) },
            enabled = excuse.isNotBlank(),
            modifier = Modifier.fillMaxWidth(),
        ) { Text("Enviar solo la excusa") }

        state.lastRequest?.let {
            Text(
                "Estado: pendiente de revisión" +
                    (it.newDate?.let { d -> " · nueva fecha ${d.pretty()}" } ?: "") +
                    if (it.attachmentUri != null) " · con soporte" else "",
                color = MaterialTheme.colorScheme.tertiary,
            )
        }
    }

    if (showPicker) {
        // Solo fechas posteriores a la fecha de pago actual.
        val minDay = c.nextPaymentDate.toEpochDay()
        val pickerState = rememberDatePickerState(
            selectableDates = object : androidx.compose.material3.SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long) =
                    Instant.ofEpochMilli(utcTimeMillis).atZone(ZoneOffset.UTC)
                        .toLocalDate().toEpochDay() > minDay
            },
        )
        DatePickerDialog(
            onDismissRequest = { showPicker = false },
            confirmButton = {
                TextButton({
                    pickerState.selectedDateMillis?.let {
                        newDateEpoch = Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC)
                            .toLocalDate().toEpochDay()
                    }
                    showPicker = false
                }) { Text("Aceptar") }
            },
            dismissButton = { TextButton({ showPicker = false }) { Text("Cancelar") } },
        ) { DatePicker(pickerState) }
    }
}

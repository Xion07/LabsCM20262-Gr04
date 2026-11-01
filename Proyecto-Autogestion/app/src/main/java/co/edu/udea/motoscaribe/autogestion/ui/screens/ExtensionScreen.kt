package co.edu.udea.motoscaribe.autogestion.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import co.edu.udea.motoscaribe.autogestion.R
import co.edu.udea.motoscaribe.autogestion.SubmitState
import co.edu.udea.motoscaribe.autogestion.UiState
import co.edu.udea.motoscaribe.autogestion.data.ExtensionRequest
import co.edu.udea.motoscaribe.autogestion.ui.components.Loading
import co.edu.udea.motoscaribe.autogestion.ui.components.ScreenColumn
import co.edu.udea.motoscaribe.autogestion.ui.cop
import co.edu.udea.motoscaribe.autogestion.ui.pretty
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

private const val MIN_STORY = 10
private const val MAX_STORY = 500

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExtensionScreen(state: UiState, onSubmit: (ExtensionRequest) -> Unit, onSubmitConsumed: () -> Unit) {
    val c = state.contract
    if (c == null) {
        Loading()
        return
    }
    var story by rememberSaveable { mutableStateOf("") }
    var newDateEpoch by rememberSaveable { mutableStateOf<Long?>(null) }
    var showPicker by rememberSaveable { mutableStateOf(false) }
    var attachment by rememberSaveable { mutableStateOf<String?>(null) }
    var showSent by rememberSaveable { mutableStateOf(false) }
    val pickMedia = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) {
        attachment = it?.toString()
    }

    // Al terminar el envio con exito: se limpia el formulario y se confirma.
    LaunchedEffect(state.submit) {
        if (state.submit == SubmitState.SUCCESS) {
            story = ""
            newDateEpoch = null
            attachment = null
            showSent = true
            onSubmitConsumed()
        }
    }
    val sending = state.submit == SubmitState.SENDING

    val newDate = newDateEpoch?.let { LocalDate.ofEpochDay(it) }
    val storyOk = story.trim().length >= MIN_STORY

    ScreenColumn {
        Text(
            stringResource(R.string.ext_title, c.nextPaymentDate.pretty()),
            style = MaterialTheme.typography.titleLarge,
        )
        Text(c.nextPaymentAmount.cop(), style = MaterialTheme.typography.titleMedium)

        OutlinedButton({ showPicker = true }, Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
            Text(newDate?.pretty() ?: stringResource(R.string.ext_pick_date))
        }
        Text(stringResource(R.string.ext_date_help), style = MaterialTheme.typography.bodySmall)

        OutlinedTextField(
            value = story,
            onValueChange = { if (it.length <= MAX_STORY) story = it },
            label = { Text(stringResource(R.string.ext_story)) },
            modifier = Modifier.fillMaxWidth(),
            minLines = 3,
            supportingText = {
                Text(
                    if (storyOk) "${story.length}/$MAX_STORY"
                    else stringResource(R.string.ext_story_min, MIN_STORY)
                )
            },
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Sentences,
                imeAction = ImeAction.Done,
            ),
        )

        OutlinedButton(
            onClick = {
                pickMedia.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            },
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
        ) { Text(stringResource(if (attachment == null) R.string.ext_attach else R.string.ext_attached)) }
        if (attachment != null) {
            TextButton({ attachment = null }) { Text(stringResource(R.string.ext_remove)) }
        }

        Button(
            onClick = { onSubmit(ExtensionRequest(c.nextPaymentDate, newDate, story.trim(), attachment)) },
            enabled = storyOk && !sending,
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
        ) { Text(stringResource(if (sending) R.string.ext_sending else R.string.ext_send)) }
        if (state.submit == SubmitState.ERROR) {
            Text(stringResource(R.string.ext_error_send), color = MaterialTheme.colorScheme.error)
        }

        Text(stringResource(R.string.ext_note), style = MaterialTheme.typography.bodySmall)

        state.lastRequest?.let {
            Text(
                stringResource(R.string.ext_status_pending) +
                    (it.newDate?.let { d -> stringResource(R.string.ext_status_date, d.pretty()) } ?: "") +
                    (if (it.attachmentUri != null) stringResource(R.string.ext_status_support) else ""),
                color = MaterialTheme.colorScheme.tertiary,
            )
        }
    }

    if (showSent) {
        AlertDialog(
            onDismissRequest = { showSent = false },
            title = { Text(stringResource(R.string.ext_sent_title)) },
            text = { Text(stringResource(R.string.ext_sent_text)) },
            confirmButton = { TextButton({ showSent = false }) { Text(stringResource(R.string.ok)) } },
        )
    }

    if (showPicker) {
        // Solo fechas posteriores a la fecha de pago actual.
        val minDay = c.nextPaymentDate.toEpochDay()
        val pickerState = rememberDatePickerState(
            selectableDates = object : SelectableDates {
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
                }) { Text(stringResource(R.string.ok)) }
            },
            dismissButton = { TextButton({ showPicker = false }) { Text(stringResource(R.string.cancel)) } },
        ) { DatePicker(pickerState) }
    }
}

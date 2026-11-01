package co.edu.udea.motoscaribe.autogestion.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AddPhotoAlternate
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.HourglassTop
import androidx.compose.material.icons.rounded.Send
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import co.edu.udea.motoscaribe.autogestion.R
import co.edu.udea.motoscaribe.autogestion.SubmitState
import co.edu.udea.motoscaribe.autogestion.UiState
import co.edu.udea.motoscaribe.autogestion.data.ExtensionRequest
import co.edu.udea.motoscaribe.autogestion.ui.components.HeroCard
import co.edu.udea.motoscaribe.autogestion.ui.components.Loading
import co.edu.udea.motoscaribe.autogestion.ui.components.OutlineButton
import co.edu.udea.motoscaribe.autogestion.ui.components.PrimaryButton
import co.edu.udea.motoscaribe.autogestion.ui.components.ScreenColumn
import co.edu.udea.motoscaribe.autogestion.ui.components.ScreenHeader
import co.edu.udea.motoscaribe.autogestion.ui.components.SectionTitle
import co.edu.udea.motoscaribe.autogestion.ui.cop
import co.edu.udea.motoscaribe.autogestion.ui.motion.SuccessCheck
import co.edu.udea.motoscaribe.autogestion.ui.motion.enterFromBelow
import co.edu.udea.motoscaribe.autogestion.ui.pretty
import co.edu.udea.motoscaribe.autogestion.ui.theme.MotosTheme
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
    val x = MotosTheme.colors

    ScreenColumn {
        ScreenHeader(stringResource(R.string.nav_extension), stringResource(R.string.ext_subtitle), Modifier.enterFromBelow(0))

        HeroCard(Modifier.enterFromBelow(1), glowColor = x.heroWarning) {
            Text(stringResource(R.string.ext_title, c.nextPaymentDate.pretty()), style = MaterialTheme.typography.labelLarge, color = x.onHeroMuted)
            Text(c.nextPaymentAmount.cop(), style = MaterialTheme.typography.headlineLarge, color = x.onHero)
        }

        SectionTitle(stringResource(R.string.ext_step_date), Modifier.enterFromBelow(2))
        OutlineButton(newDate?.pretty() ?: stringResource(R.string.ext_pick_date), { showPicker = true }, Modifier.enterFromBelow(2), Icons.Rounded.CalendarMonth)
        Text(
            stringResource(R.string.ext_date_help),
            Modifier.enterFromBelow(2),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        SectionTitle(stringResource(R.string.ext_step_story), Modifier.enterFromBelow(3))
        OutlinedTextField(
            value = story,
            onValueChange = { if (it.length <= MAX_STORY) story = it },
            label = { Text(stringResource(R.string.ext_story)) },
            modifier = Modifier.fillMaxWidth().enterFromBelow(3),
            shape = MaterialTheme.shapes.medium,
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

        SectionTitle(stringResource(R.string.ext_step_proof), Modifier.enterFromBelow(4))
        if (attachment == null) {
            OutlineButton(
                stringResource(R.string.ext_attach),
                { pickMedia.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                Modifier.enterFromBelow(4),
                Icons.Rounded.AddPhotoAlternate,
            )
        } else {
            Card(
                Modifier.fillMaxWidth().enterFromBelow(4),
                shape = MaterialTheme.shapes.medium,
                colors = CardDefaults.cardColors(x.successContainer),
            ) {
                Row(Modifier.padding(start = 16.dp, end = 4.dp, top = 4.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Icon(Icons.Rounded.CheckCircle, null, tint = x.onSuccessContainer)
                    TextButton(
                        { pickMedia.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                        Modifier.weight(1f),
                    ) { Text(stringResource(R.string.ext_attached), color = x.onSuccessContainer, textAlign = TextAlign.Start, modifier = Modifier.fillMaxWidth()) }
                    IconButton({ attachment = null }, Modifier.size(48.dp)) {
                        Icon(Icons.Rounded.Close, stringResource(R.string.ext_remove), tint = x.onSuccessContainer)
                    }
                }
            }
        }

        PrimaryButton(
            stringResource(if (sending) R.string.ext_sending else R.string.ext_send),
            { onSubmit(ExtensionRequest(c.nextPaymentDate, newDate, story.trim(), attachment)) },
            Modifier.padding(top = 6.dp).enterFromBelow(5),
            enabled = storyOk,
            icon = Icons.Rounded.Send,
            loading = sending,
        )
        AnimatedVisibility(state.submit == SubmitState.ERROR, enter = fadeIn() + expandVertically(), exit = fadeOut() + shrinkVertically()) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Rounded.ErrorOutline, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.error)
                Text(stringResource(R.string.ext_error_send), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
            }
        }

        Text(
            stringResource(R.string.ext_note),
            Modifier.enterFromBelow(5),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        AnimatedVisibility(state.lastRequest != null, enter = fadeIn() + expandVertically(), exit = fadeOut() + shrinkVertically()) {
            state.lastRequest?.let {
                Card(
                    Modifier.fillMaxWidth().semantics(mergeDescendants = true) {},
                    shape = MaterialTheme.shapes.medium,
                    colors = CardDefaults.cardColors(x.warningContainer),
                ) {
                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Icon(Icons.Rounded.HourglassTop, null, tint = x.onWarningContainer)
                        Text(
                            stringResource(R.string.ext_status_pending) +
                                (it.newDate?.let { d -> stringResource(R.string.ext_status_date, d.pretty()) } ?: "") +
                                (if (it.attachmentUri != null) stringResource(R.string.ext_status_support) else ""),
                            color = x.onWarningContainer,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
            }
        }
    }

    if (showSent) {
        val sentCd = stringResource(R.string.ext_sent_cd)
        AlertDialog(
            onDismissRequest = { showSent = false },
            icon = { SuccessCheck(Modifier.semantics { contentDescription = sentCd }, color = x.success) },
            title = { Text(stringResource(R.string.ext_sent_title), textAlign = TextAlign.Center) },
            text = { Text(stringResource(R.string.ext_sent_text), textAlign = TextAlign.Center) },
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

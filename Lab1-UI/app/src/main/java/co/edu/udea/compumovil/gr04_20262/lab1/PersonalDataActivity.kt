package co.edu.udea.compumovil.gr04_20262.lab1

import android.content.res.Configuration
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import co.edu.udea.compumovil.gr04_20262.lab1.ui.theme.Lab1UITheme
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

private const val TAG = "InformacionPersonal"

/**
 * Actividad "Informacion personal" pedida en el laboratorio.
 * Campos: Nombres*, Apellidos*, Sexo, Fecha de nacimiento*, Grado de escolaridad.
 * Los campos con * son obligatorios. Se adapta a portrait y landscape.
 */
class PersonalDataActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            Lab1UITheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    PersonalDataScreen()
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PersonalDataScreen() {
    // rememberSaveable conserva los datos ante cambios de configuracion
    // (rotacion de pantalla), tal como lo pide el laboratorio.
    var names by rememberSaveable { mutableStateOf("") }
    var lastNames by rememberSaveable { mutableStateOf("") }
    var gender by rememberSaveable { mutableStateOf("") } // "M", "F" o ""
    var birthDateMillis by rememberSaveable { mutableStateOf<Long?>(null) }
    var schoolingIndex by rememberSaveable { mutableStateOf(-1) } // -1 = sin seleccionar

    var namesError by rememberSaveable { mutableStateOf(false) }
    var lastNamesError by rememberSaveable { mutableStateOf(false) }
    var birthDateError by rememberSaveable { mutableStateOf(false) }

    var showDatePicker by rememberSaveable { mutableStateOf(false) }
    var schoolingExpanded by remember { mutableStateOf(false) }
    var snackbarMessage by remember { mutableStateOf<String?>(null) }

    val focusManager = LocalFocusManager.current
    val lastNamesFocusRequester = remember { FocusRequester() }
    val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE

    val genderMale = stringResource(R.string.gender_male)
    val genderFemale = stringResource(R.string.gender_female)
    val genderUnspecified = stringResource(R.string.value_unspecified)
    val genderOptions = listOf(genderMale to "M", genderFemale to "F")

    val schoolingLevels = stringArrayResource(R.array.schooling_levels)
    val schoolingHint = stringResource(R.string.hint_select)
    val dateFormatter = remember { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()) }

    val snackbarHostState = remember { SnackbarHostState() }
    val validationMessage = stringResource(R.string.msg_validation_error)
    val savedMessage = stringResource(R.string.msg_data_logged)

    LaunchedEffect(snackbarMessage) {
        snackbarMessage?.let {
            snackbarHostState.showSnackbar(it)
            snackbarMessage = null
        }
    }

    // ---- Campos reutilizables (se colocan distinto segun orientacion) ----
    val namesField = @Composable { modifier: Modifier ->
        OutlinedTextField(
            value = names,
            onValueChange = { names = it; namesError = false },
            label = { Text(stringResource(R.string.label_names)) },
            singleLine = true,
            isError = namesError,
            supportingText = { if (namesError) Text(stringResource(R.string.error_required)) },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Text,
                capitalization = KeyboardCapitalization.Words,
                autoCorrectEnabled = false, // sin autocorreccion ni aprendizaje de sugerencias
                imeAction = ImeAction.Next
            ),
            keyboardActions = KeyboardActions(onNext = { lastNamesFocusRequester.requestFocus() }),
            modifier = modifier.testTag("field_names")
        )
    }

    val lastNamesField = @Composable { modifier: Modifier ->
        OutlinedTextField(
            value = lastNames,
            onValueChange = { lastNames = it; lastNamesError = false },
            label = { Text(stringResource(R.string.label_lastnames)) },
            singleLine = true,
            isError = lastNamesError,
            supportingText = { if (lastNamesError) Text(stringResource(R.string.error_required)) },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Text,
                capitalization = KeyboardCapitalization.Words,
                autoCorrectEnabled = false,
                imeAction = ImeAction.Done // ultimo campo con teclado -> "Listo"
            ),
            keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
            modifier = modifier
                .focusRequester(lastNamesFocusRequester)
                .testTag("field_lastnames")
        )
    }

    val genderField = @Composable { modifier: Modifier ->
        Column(modifier) {
            Text(
                text = stringResource(R.string.label_gender),
                style = MaterialTheme.typography.labelLarge
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                genderOptions.forEach { (label, value) ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .padding(end = 16.dp)
                            .selectable(
                                selected = gender == value,
                                onClick = { gender = value },
                                role = Role.RadioButton
                            )
                            .testTag("radio_$value")
                    ) {
                        RadioButton(selected = gender == value, onClick = null)
                        Text(text = label)
                    }
                }
            }
        }
    }

    val birthDateField = @Composable { modifier: Modifier ->
        OutlinedTextField(
            value = birthDateMillis?.let { dateFormatter.format(Date(it)) } ?: "",
            onValueChange = {},
            readOnly = true,
            label = { Text(stringResource(R.string.label_birthdate)) },
            placeholder = { Text(stringResource(R.string.hint_birthdate)) },
            isError = birthDateError,
            supportingText = { if (birthDateError) Text(stringResource(R.string.error_required)) },
            trailingIcon = {
                IconButton(onClick = { showDatePicker = true }) {
                    Icon(
                        Icons.Filled.DateRange,
                        contentDescription = stringResource(R.string.cd_pick_date)
                    )
                }
            },
            modifier = modifier
                .clickableNoRipple { showDatePicker = true }
                .testTag("field_birthdate")
        )
    }

    val schoolingField = @Composable { modifier: Modifier ->
        Column(modifier) {
            Text(
                text = stringResource(R.string.label_schooling),
                style = MaterialTheme.typography.labelLarge
            )
            ExposedDropdownMenuBox(
                expanded = schoolingExpanded,
                onExpandedChange = { schoolingExpanded = it }
            ) {
                OutlinedTextField(
                    value = if (schoolingIndex >= 0) schoolingLevels[schoolingIndex] else schoolingHint,
                    onValueChange = {},
                    readOnly = true,
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = schoolingExpanded)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                        .testTag("field_schooling")
                )
                ExposedDropdownMenu(
                    expanded = schoolingExpanded,
                    onDismissRequest = { schoolingExpanded = false }
                ) {
                    schoolingLevels.forEachIndexed { index, level ->
                        DropdownMenuItem(
                            text = { Text(level) },
                            onClick = { schoolingIndex = index; schoolingExpanded = false }
                        )
                    }
                }
            }
        }
    }

    val onSubmit = {
        namesError = names.isBlank()
        lastNamesError = lastNames.isBlank()
        birthDateError = birthDateMillis == null

        if (namesError || lastNamesError || birthDateError) {
            snackbarMessage = validationMessage
        } else {
            val genderText = when (gender) {
                "M" -> genderMale
                "F" -> genderFemale
                else -> genderUnspecified
            }
            val dateText = birthDateMillis?.let { dateFormatter.format(Date(it)) } ?: ""
            val schoolingText = if (schoolingIndex >= 0) schoolingLevels[schoolingIndex]
                                else genderUnspecified

            // Input controls: al presionar Siguiente y con los obligatorios OK,
            // se escriben en Logcat los datos ingresados por el usuario.
            Log.i(TAG, "----- Informacion personal -----")
            Log.i(TAG, "Nombres:      $names")
            Log.i(TAG, "Apellidos:    $lastNames")
            Log.i(TAG, "Sexo:         $genderText")
            Log.i(TAG, "Nacimiento:   $dateText")
            Log.i(TAG, "Escolaridad:  $schoolingText")
            snackbarMessage = savedMessage
        }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(R.string.title_personal_data)) }) },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (isLandscape) {
                // Figura 3: version landscape -> campos en dos columnas.
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    namesField(Modifier.weight(1f))
                    lastNamesField(Modifier.weight(1f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    genderField(Modifier.weight(1f))
                    birthDateField(Modifier.weight(1f))
                }
                schoolingField(Modifier.fillMaxWidth())
            } else {
                // Figura 2: version portrait -> una sola columna.
                namesField(Modifier.fillMaxWidth())
                lastNamesField(Modifier.fillMaxWidth())
                genderField(Modifier.fillMaxWidth())
                birthDateField(Modifier.fillMaxWidth())
                schoolingField(Modifier.fillMaxWidth())
            }

            Button(
                onClick = onSubmit,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("btn_next")
            ) {
                Text(stringResource(R.string.btn_next))
            }
        }
    }

    if (showDatePicker) {
        val today = remember { System.currentTimeMillis() }
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = birthDateMillis,
            yearRange = 1900..Calendar.getInstance().get(Calendar.YEAR),
            selectableDates = object : SelectableDates {
                // La fecha de nacimiento no puede ser futura.
                override fun isSelectableDate(utcTimeMillis: Long) = utcTimeMillis <= today
                override fun isSelectableYear(year: Int) =
                    year <= Calendar.getInstance().get(Calendar.YEAR)
            }
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    birthDateMillis = datePickerState.selectedDateMillis
                    birthDateError = false
                    showDatePicker = false
                }) { Text(stringResource(android.R.string.ok)) }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text(stringResource(android.R.string.cancel))
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

/** Modifier auxiliar para permitir tocar todo el campo de fecha y abrir el selector. */
@Composable
private fun Modifier.clickableNoRipple(onClick: () -> Unit): Modifier {
    val interactionSource = remember { MutableInteractionSource() }
    return this.clickable(
        interactionSource = interactionSource,
        indication = null,
        onClick = onClick
    )
}

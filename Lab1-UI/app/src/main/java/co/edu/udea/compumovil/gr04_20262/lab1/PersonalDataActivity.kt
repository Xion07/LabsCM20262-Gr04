package co.edu.udea.compumovil.gr04_20262.lab1

import android.content.res.Configuration
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import co.edu.udea.compumovil.gr04_20262.lab1.ui.FormScaffold
import co.edu.udea.compumovil.gr04_20262.lab1.ui.SectionCard
import co.edu.udea.compumovil.gr04_20262.lab1.ui.TwoSectionLayout
import co.edu.udea.compumovil.gr04_20262.lab1.ui.theme.Lab1UITheme
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

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
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
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
    // El DatePicker entrega el instante en UTC; se formatea en UTC para que la
    // fecha mostrada/registrada coincida con la que eligio el usuario.
    val dateFormatter = remember {
        SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
    }

    val snackbarHostState = remember { SnackbarHostState() }
    val validationMessage = stringResource(R.string.msg_validation_error)
    val savedMessage = stringResource(R.string.msg_data_logged)

    LaunchedEffect(snackbarMessage) {
        snackbarMessage?.let {
            snackbarHostState.showSnackbar(it)
            snackbarMessage = null
        }
    }

    // ---- Campos reutilizables ----
    val namesField = @Composable {
        OutlinedTextField(
            value = names,
            onValueChange = { names = it; namesError = false },
            label = { Text(stringResource(R.string.label_names)) },
            leadingIcon = { Icon(Icons.Filled.Person, contentDescription = null) },
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
            modifier = Modifier
                .fillMaxWidth()
                .testTag("field_names")
        )
    }

    val lastNamesField = @Composable {
        OutlinedTextField(
            value = lastNames,
            onValueChange = { lastNames = it; lastNamesError = false },
            label = { Text(stringResource(R.string.label_lastnames)) },
            leadingIcon = { Icon(Icons.Filled.Person, contentDescription = null) },
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
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(lastNamesFocusRequester)
                .testTag("field_lastnames")
        )
    }

    val genderField = @Composable {
        Column {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    Icons.Filled.Face,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = stringResource(R.string.label_gender),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.height(4.dp))
            Row(
                modifier = Modifier.selectableGroup(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                genderOptions.forEach { (label, value) ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .padding(end = 12.dp)
                            .clip(RoundedCornerShape(50))
                            .selectable(
                                selected = gender == value,
                                onClick = { gender = value },
                                role = Role.RadioButton
                            )
                            .padding(end = 8.dp)
                            .testTag("radio_$value")
                    ) {
                        RadioButton(selected = gender == value, onClick = null)
                        Text(text = label)
                    }
                }
            }
        }
    }

    val birthDateField = @Composable {
        OutlinedTextField(
            value = birthDateMillis?.let { dateFormatter.format(Date(it)) } ?: "",
            onValueChange = {},
            readOnly = true,
            label = { Text(stringResource(R.string.label_birthdate)) },
            placeholder = { Text(stringResource(R.string.hint_birthdate)) },
            leadingIcon = { Icon(Icons.Filled.DateRange, contentDescription = null) },
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
            modifier = Modifier
                .fillMaxWidth()
                .clickableNoRipple { showDatePicker = true }
                .testTag("field_birthdate")
        )
    }

    val schoolingField = @Composable {
        ExposedDropdownMenuBox(
            expanded = schoolingExpanded,
            onExpandedChange = { schoolingExpanded = it }
        ) {
            OutlinedTextField(
                value = if (schoolingIndex >= 0) schoolingLevels[schoolingIndex] else schoolingHint,
                onValueChange = {},
                readOnly = true,
                label = { Text(stringResource(R.string.label_schooling)) },
                leadingIcon = { Icon(Icons.AutoMirrored.Filled.List, contentDescription = null) },
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

    FormScaffold(
        title = stringResource(R.string.title_personal_data),
        snackbarHostState = snackbarHostState
    ) {
        TwoSectionLayout(
            isLandscape = isLandscape,
            first = {
                SectionCard(
                    title = stringResource(R.string.section_basic_data),
                    icon = Icons.Filled.Person
                ) {
                    namesField()
                    lastNamesField()
                    genderField()
                }
            },
            second = {
                SectionCard(
                    title = stringResource(R.string.section_birth_studies),
                    icon = Icons.Filled.DateRange
                ) {
                    birthDateField()
                    schoolingField()
                }
            }
        )

        Button(
            onClick = onSubmit,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .testTag("btn_next")
        ) {
            Text(stringResource(R.string.btn_next))
            Spacer(Modifier.width(8.dp))
            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null)
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

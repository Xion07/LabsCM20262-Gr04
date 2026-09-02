package co.edu.udea.compumovil.gr04_20262.lab1

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import co.edu.udea.compumovil.gr04_20262.lab1.ui.theme.Lab1UITheme
import java.text.SimpleDateFormat
import java.util.*

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource

private const val TAG = "InformacionPersonal"

/**
 * Actividad "Información personal" pedida en el laboratorio.
 * Campos: Nombres*, Apellidos*, Sexo, Fecha de nacimiento*, Grado de escolaridad.
 * Los campos con * son obligatorios.
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
    // rememberSaveable conserva los datos ante cambios de configuración
    // (rotación de pantalla), tal como lo pide el laboratorio.
    var names by rememberSaveable { mutableStateOf("") }
    var lastNames by rememberSaveable { mutableStateOf("") }
    var gender by rememberSaveable { mutableStateOf("") } // "M" o "F"
    var birthDateMillis by rememberSaveable { mutableStateOf<Long?>(null) }
    var schoolingIndex by rememberSaveable { mutableStateOf(0) }

    var namesError by rememberSaveable { mutableStateOf(false) }
    var lastNamesError by rememberSaveable { mutableStateOf(false) }
    var birthDateError by rememberSaveable { mutableStateOf(false) }

    var showDatePicker by rememberSaveable { mutableStateOf(false) }
    var showValidationSnackbar by remember { mutableStateOf(false) }

    val focusManager = LocalFocusManager.current
    val lastNamesFocusRequester = remember { FocusRequester() }

    val genderOptions = listOf(
        stringResource(R.string.gender_male) to "M",
        stringResource(R.string.gender_female) to "F"
    )
    val schoolingLevels = androidx.compose.ui.res.stringArrayResource(R.array.schooling_levels)
    val dateFormatter = remember { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()) }

    val snackbarHostState = remember { SnackbarHostState() }
    val validationMessage = stringResource(R.string.msg_validation_error)

    LaunchedEffect(showValidationSnackbar) {
        if (showValidationSnackbar) {
            snackbarHostState.showSnackbar(validationMessage)
            showValidationSnackbar = false
        }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(R.string.title_personal_data)) }) },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Nombres: teclado normal, mayúscula inicial, sin sugerencias
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
                    autoCorrect = false,
                    imeAction = ImeAction.Next
                ),
                keyboardActions = KeyboardActions(
                    onNext = { lastNamesFocusRequester.requestFocus() }
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Apellidos: mismo comportamiento de teclado que Nombres
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
                    autoCorrect = false,
                    imeAction = ImeAction.Next
                ),
                keyboardActions = KeyboardActions(
                    onNext = { focusManager.clearFocus() }
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(lastNamesFocusRequester)
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Sexo: RadioButton (campo opcional)
            Text(text = stringResource(R.string.label_gender), style = MaterialTheme.typography.labelLarge)
            Row(verticalAlignment = Alignment.CenterVertically) {
                genderOptions.forEach { (label, value) ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(end = 16.dp)
                    ) {
                        RadioButton(
                            selected = gender == value,
                            onClick = { gender = value }
                        )
                        Text(text = label)
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Fecha de nacimiento: DatePicker (obligatorio)
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
                        Icon(Icons.Filled.DateRange, contentDescription = null)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .clickableNoRipple { showDatePicker = true }
            )

            if (showDatePicker) {
                val datePickerState = rememberDatePickerState(initialSelectedDateMillis = birthDateMillis)
                DatePickerDialog(
                    onDismissRequest = { showDatePicker = false },
                    confirmButton = {
                        TextButton(onClick = {
                            birthDateMillis = datePickerState.selectedDateMillis
                            birthDateError = false
                            showDatePicker = false
                        }) { Text("OK") }
                    },
                    dismissButton = {
                        TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
                    }
                ) {
                    DatePicker(state = datePickerState)
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Grado de escolaridad: Spinner (implementado con ExposedDropdownMenuBox, campo opcional)
            Text(text = stringResource(R.string.label_schooling), style = MaterialTheme.typography.labelLarge)
            var expanded by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
                OutlinedTextField(
                    value = schoolingLevels[schoolingIndex],
                    onValueChange = {},
                    readOnly = true,
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor()
                )
                ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    schoolingLevels.forEachIndexed { index, level ->
                        DropdownMenuItem(
                            text = { Text(level) },
                            onClick = { schoolingIndex = index; expanded = false }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = {
                    // Input controls: validar campos obligatorios antes de continuar
                    namesError = names.isBlank()
                    lastNamesError = lastNames.isBlank()
                    birthDateError = birthDateMillis == null

                    if (namesError || lastNamesError || birthDateError) {
                        showValidationSnackbar = true
                        return@Button
                    }

                    val genderText = when (gender) {
                        "M" -> "Masculino"
                        "F" -> "Femenino"
                        else -> "No especificado"
                    }
                    val dateText = birthDateMillis?.let { dateFormatter.format(Date(it)) } ?: ""

                    // Log de los datos ingresados, tal como lo exige el laboratorio
                    Log.i(TAG, "Información personal:")
                    Log.i(TAG, "$names $lastNames $genderText")
                    Log.i(TAG, "Nació el $dateText")
                    Log.i(TAG, schoolingLevels[schoolingIndex])
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.btn_next))
            }
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

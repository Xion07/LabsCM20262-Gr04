package co.edu.udea.compumovil.gr04_20262.lab1

import android.os.Bundle
import android.util.Log
import android.util.Patterns
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import co.edu.udea.compumovil.gr04_20262.lab1.ui.theme.Lab1UITheme

private const val TAG = "InformacionContacto"

/**
 * Actividad "Información de contacto" pedida en el laboratorio.
 * Campos: Teléfono*, Dirección, Email*, País*, Ciudad.
 * País y Ciudad se implementan como autocompletar (Exposed dropdown filtrado).
 */
class ContactDataActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            Lab1UITheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    ContactDataScreen()
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactDataScreen() {
    var phone by rememberSaveable { mutableStateOf("") }
    var address by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var country by rememberSaveable { mutableStateOf("") }
    var city by rememberSaveable { mutableStateOf("") }

    var phoneError by rememberSaveable { mutableStateOf(false) }
    var emailError by rememberSaveable { mutableStateOf(false) }
    var countryError by rememberSaveable { mutableStateOf(false) }

    val focusManager = LocalFocusManager.current
    val addressFocusRequester = remember { FocusRequester() }
    val emailFocusRequester = remember { FocusRequester() }

    val countries = stringArrayResource(R.array.latam_countries).toList()
    val cities = stringArrayResource(R.array.colombia_cities).toList()

    var showValidationSnackbar by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val validationMessage = stringResource(R.string.msg_validation_error)

    LaunchedEffect(showValidationSnackbar) {
        if (showValidationSnackbar) {
            snackbarHostState.showSnackbar(validationMessage)
            showValidationSnackbar = false
        }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(R.string.title_contact_data)) }) },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Teléfono: teclado telefónico (obligatorio)
            OutlinedTextField(
                value = phone,
                onValueChange = { phone = it; phoneError = false },
                label = { Text(stringResource(R.string.label_phone)) },
                singleLine = true,
                isError = phoneError,
                supportingText = { if (phoneError) Text(stringResource(R.string.error_required)) },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Phone,
                    imeAction = ImeAction.Next
                ),
                keyboardActions = KeyboardActions(
                    onNext = { addressFocusRequester.requestFocus() }
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Dirección: teclado que no sugiere nada (campo opcional)
            OutlinedTextField(
                value = address,
                onValueChange = { address = it },
                label = { Text(stringResource(R.string.label_address)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Text,
                    autoCorrect = false,
                    imeAction = ImeAction.Next
                ),
                keyboardActions = KeyboardActions(
                    onNext = { emailFocusRequester.requestFocus() }
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(addressFocusRequester)
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Email: tipo de dato email (obligatorio)
            OutlinedTextField(
                value = email,
                onValueChange = { email = it; emailError = false },
                label = { Text(stringResource(R.string.label_email)) },
                singleLine = true,
                isError = emailError,
                supportingText = {
                    if (emailError) Text(
                        if (email.isBlank()) stringResource(R.string.error_required)
                        else stringResource(R.string.error_email)
                    )
                },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Next
                ),
                keyboardActions = KeyboardActions(
                    onNext = { focusManager.clearFocus() }
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(emailFocusRequester)
            )

            Spacer(modifier = Modifier.height(12.dp))

            // País: autocompletar con países de Latinoamérica (obligatorio)
            AutocompleteField(
                label = stringResource(R.string.label_country),
                value = country,
                onValueChange = { country = it; countryError = false },
                options = countries,
                isError = countryError,
                errorText = stringResource(R.string.error_required)
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Ciudad: autocompletar con ciudades principales de Colombia (campo opcional)
            AutocompleteField(
                label = stringResource(R.string.label_city),
                value = city,
                onValueChange = { city = it },
                options = cities,
                isError = false,
                errorText = ""
            )

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = {
                    phoneError = phone.isBlank()
                    emailError = email.isBlank() || !Patterns.EMAIL_ADDRESS.matcher(email).matches()
                    countryError = country.isBlank()

                    if (phoneError || emailError || countryError) {
                        showValidationSnackbar = true
                        return@Button
                    }

                    // Log de los datos ingresados, tal como lo exige el laboratorio
                    Log.i(TAG, "Información de contacto:")
                    Log.i(TAG, "Teléfono: $phone")
                    Log.i(TAG, "Dirección: $address")
                    Log.i(TAG, "Email: $email")
                    Log.i(TAG, "País: $country")
                    Log.i(TAG, "Ciudad: $city")
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.btn_finish))
            }
        }
    }
}

/**
 * Campo de texto con autocompletar: al escribir, filtra la lista de
 * opciones y las muestra en un menú desplegable (ExposedDropdownMenu).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AutocompleteField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    options: List<String>,
    isError: Boolean,
    errorText: String
) {
    var expanded by remember { mutableStateOf(false) }
    val filteredOptions = remember(value, options) {
        if (value.isBlank()) options
        else options.filter { it.contains(value, ignoreCase = true) }
    }

    ExposedDropdownMenuBox(
        expanded = expanded && filteredOptions.isNotEmpty(),
        onExpandedChange = { expanded = it }
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = {
                onValueChange(it)
                expanded = true
            },
            label = { Text(label) },
            singleLine = true,
            isError = isError,
            supportingText = { if (isError) Text(errorText) },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Text,
                autoCorrect = false,
                imeAction = ImeAction.Next
            ),
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor()
        )
        ExposedDropdownMenu(
            expanded = expanded && filteredOptions.isNotEmpty(),
            onDismissRequest = { expanded = false }
        ) {
            filteredOptions.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option) },
                    onClick = {
                        onValueChange(option)
                        expanded = false
                    }
                )
            }
        }
    }
}

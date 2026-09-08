package co.edu.udea.compumovil.gr04_20262.lab1

import android.content.res.Configuration
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import co.edu.udea.compumovil.gr04_20262.lab1.ui.theme.Lab1UITheme

private const val TAG = "InformacionContacto"

/**
 * Actividad "Informacion de contacto" pedida en el laboratorio.
 * Campos: Telefono*, Direccion, Email*, Pais*, Ciudad.
 * Pais y Ciudad se implementan como autocompletar (dropdown filtrado).
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
    val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE

    val countries = stringArrayResource(R.array.latam_countries).toList()
    val cities = stringArrayResource(R.array.colombia_cities).toList()

    var snackbarMessage by remember { mutableStateOf<String?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }
    val validationMessage = stringResource(R.string.msg_validation_error)
    val savedMessage = stringResource(R.string.msg_data_logged)
    val requiredText = stringResource(R.string.error_required)
    val emailBadText = stringResource(R.string.error_email)

    LaunchedEffect(snackbarMessage) {
        snackbarMessage?.let {
            snackbarHostState.showSnackbar(it)
            snackbarMessage = null
        }
    }

    val phoneField = @Composable { modifier: Modifier ->
        OutlinedTextField(
            value = phone,
            onValueChange = { phone = it; phoneError = false },
            label = { Text(stringResource(R.string.label_phone)) },
            singleLine = true,
            isError = phoneError,
            supportingText = { if (phoneError) Text(requiredText) },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Phone, // teclado telefonico
                imeAction = ImeAction.Next
            ),
            keyboardActions = KeyboardActions(onNext = { addressFocusRequester.requestFocus() }),
            modifier = modifier.testTag("field_phone")
        )
    }

    val addressField = @Composable { modifier: Modifier ->
        OutlinedTextField(
            value = address,
            onValueChange = { address = it },
            label = { Text(stringResource(R.string.label_address)) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Text,
                capitalization = KeyboardCapitalization.Words,
                autoCorrectEnabled = false, // el teclado no sugiere nada
                imeAction = ImeAction.Next
            ),
            keyboardActions = KeyboardActions(onNext = { emailFocusRequester.requestFocus() }),
            modifier = modifier
                .focusRequester(addressFocusRequester)
                .testTag("field_address")
        )
    }

    val emailField = @Composable { modifier: Modifier ->
        OutlinedTextField(
            value = email,
            onValueChange = { email = it; emailError = false },
            label = { Text(stringResource(R.string.label_email)) },
            singleLine = true,
            isError = emailError,
            supportingText = {
                if (emailError) Text(if (email.isBlank()) requiredText else emailBadText)
            },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Email, // tipo de dato email
                imeAction = ImeAction.Done // ultimo campo de teclado -> "Listo"
            ),
            keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
            modifier = modifier
                .focusRequester(emailFocusRequester)
                .testTag("field_email")
        )
    }

    val countryField = @Composable { modifier: Modifier ->
        AutocompleteField(
            label = stringResource(R.string.label_country),
            value = country,
            onValueChange = { country = it; countryError = false },
            options = countries,
            isError = countryError,
            errorText = requiredText,
            testTag = "field_country",
            modifier = modifier
        )
    }

    val cityField = @Composable { modifier: Modifier ->
        AutocompleteField(
            label = stringResource(R.string.label_city),
            value = city,
            onValueChange = { city = it },
            options = cities,
            isError = false,
            errorText = "",
            testTag = "field_city",
            modifier = modifier
        )
    }

    val onSubmit = {
        phoneError = phone.isBlank()
        emailError = email.isBlank() || !Patterns.EMAIL_ADDRESS.matcher(email).matches()
        countryError = country.isBlank()

        if (phoneError || emailError || countryError) {
            snackbarMessage = validationMessage
        } else {
            // Input controls: datos ingresados escritos en Logcat.
            Log.i(TAG, "----- Informacion de contacto -----")
            Log.i(TAG, "Telefono:   $phone")
            Log.i(TAG, "Direccion:  $address")
            Log.i(TAG, "Email:      $email")
            Log.i(TAG, "Pais:       $country")
            Log.i(TAG, "Ciudad:     $city")
            snackbarMessage = savedMessage
        }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(R.string.title_contact_data)) }) },
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
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    phoneField(Modifier.weight(1f))
                    emailField(Modifier.weight(1f))
                }
                addressField(Modifier.fillMaxWidth())
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    countryField(Modifier.weight(1f))
                    cityField(Modifier.weight(1f))
                }
            } else {
                phoneField(Modifier.fillMaxWidth())
                addressField(Modifier.fillMaxWidth())
                emailField(Modifier.fillMaxWidth())
                countryField(Modifier.fillMaxWidth())
                cityField(Modifier.fillMaxWidth())
            }

            Button(
                onClick = onSubmit,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("btn_finish")
            ) {
                Text(stringResource(R.string.btn_finish))
            }
        }
    }
}

/**
 * Campo de texto con autocompletar: al escribir, filtra la lista de opciones
 * y las muestra en un menu desplegable (ExposedDropdownMenu).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AutocompleteField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    options: List<String>,
    isError: Boolean,
    errorText: String,
    testTag: String,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    val filteredOptions = remember(value, options) {
        if (value.isBlank()) options
        else options.filter { it.contains(value, ignoreCase = true) }
    }
    val menuVisible = expanded && filteredOptions.isNotEmpty()

    ExposedDropdownMenuBox(
        expanded = menuVisible,
        onExpandedChange = { expanded = it },
        modifier = modifier
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = { onValueChange(it); expanded = true },
            label = { Text(label) },
            singleLine = true,
            isError = isError,
            supportingText = { if (isError) Text(errorText) },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Text,
                capitalization = KeyboardCapitalization.Words,
                autoCorrectEnabled = false,
                imeAction = ImeAction.Next
            ),
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(MenuAnchorType.PrimaryEditable)
                .testTag(testTag)
        )
        ExposedDropdownMenu(expanded = menuVisible, onDismissRequest = { expanded = false }) {
            filteredOptions.take(8).forEach { option ->
                DropdownMenuItem(
                    text = { Text(option) },
                    onClick = { onValueChange(option); expanded = false }
                )
            }
        }
    }
}

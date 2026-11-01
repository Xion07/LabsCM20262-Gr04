package co.edu.udea.motoscaribe.autogestion.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import co.edu.udea.motoscaribe.autogestion.LoginError
import co.edu.udea.motoscaribe.autogestion.R

private const val MAX_DOCUMENT = 12
private const val MIN_DOCUMENT = 6
private const val MIN_PASSWORD = 4

@Composable
fun LoginScreen(
    loading: Boolean,
    error: LoginError?,
    onLogin: (document: String, password: String) -> Unit,
) {
    var document by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var showPassword by rememberSaveable { mutableStateOf(false) }
    var submitted by rememberSaveable { mutableStateOf(false) }
    val focus = LocalFocusManager.current

    val documentOk = document.length >= MIN_DOCUMENT
    val passwordOk = password.length >= MIN_PASSWORD
    val submit = {
        submitted = true
        if (documentOk && passwordOk) onLogin(document, password)
    }

    Box(
        Modifier.fillMaxSize().safeDrawingPadding(),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            Modifier
                .widthIn(max = 480.dp)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(stringResource(R.string.login_title), style = MaterialTheme.typography.headlineMedium)
            Text(stringResource(R.string.login_subtitle), style = MaterialTheme.typography.bodyMedium)

            OutlinedTextField(
                value = document,
                onValueChange = { v -> document = v.filter(Char::isDigit).take(MAX_DOCUMENT) },
                label = { Text(stringResource(R.string.login_document)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                isError = submitted && !documentOk,
                supportingText = {
                    if (submitted && !documentOk) Text(stringResource(R.string.login_error_document))
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                keyboardActions = KeyboardActions(onNext = { focus.moveFocus(FocusDirection.Down) }),
            )
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text(stringResource(R.string.login_password)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    TextButton({ showPassword = !showPassword }) {
                        Text(stringResource(if (showPassword) R.string.login_hide else R.string.login_show))
                    }
                },
                isError = submitted && !passwordOk,
                supportingText = {
                    if (submitted && !passwordOk) Text(stringResource(R.string.login_error_password))
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { submit() }),
            )
            Button(
                onClick = { submit() },
                enabled = !loading,
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
            ) {
                if (loading) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                else Text(stringResource(R.string.login_submit))
            }
            error?.let {
                Text(
                    stringResource(
                        when (it) {
                            LoginError.INVALID -> R.string.login_error_invalid
                            LoginError.LOCKED -> R.string.login_error_locked
                            LoginError.NETWORK -> R.string.login_error_network
                            LoginError.SESSION_EXPIRED -> R.string.login_error_session
                            LoginError.GENERIC -> R.string.login_error_generic
                        }
                    ),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            Text(
                stringResource(R.string.login_help),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

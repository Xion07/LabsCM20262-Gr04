package co.edu.udea.motoscaribe.autogestion.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Badge
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Login
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import co.edu.udea.motoscaribe.autogestion.LoginError
import co.edu.udea.motoscaribe.autogestion.R
import co.edu.udea.motoscaribe.autogestion.ui.components.PrimaryButton
import co.edu.udea.motoscaribe.autogestion.ui.motion.EntranceScope
import co.edu.udea.motoscaribe.autogestion.ui.motion.MotoRider
import co.edu.udea.motoscaribe.autogestion.ui.motion.enterFromBelow
import co.edu.udea.motoscaribe.autogestion.ui.theme.BrandRed
import co.edu.udea.motoscaribe.autogestion.ui.theme.BrandRedDeep

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
    val landscape = LocalConfiguration.current.screenWidthDp > LocalConfiguration.current.screenHeightDp

    val documentOk = document.length >= MIN_DOCUMENT
    val passwordOk = password.length >= MIN_PASSWORD
    val submit = {
        submitted = true
        if (documentOk && passwordOk) onLogin(document, password)
    }
    val surface = MaterialTheme.colorScheme.surface

    EntranceScope {
        Box(
            Modifier
                .fillMaxSize()
                .drawBehind {
                    // Fondo de marca: degradado rojo arriba que se funde con la superficie abajo.
                    drawRect(Brush.verticalGradient(0f to BrandRed, 0.42f to BrandRedDeep, 0.58f to surface))
                    drawCircle(Color.White.copy(alpha = 0.07f), size.width * 0.5f, Offset(size.width * 0.95f, size.height * 0.05f))
                    drawCircle(Color.White.copy(alpha = 0.05f), size.width * 0.35f, Offset(-size.width * 0.05f, size.height * 0.32f))
                },
        ) {
            Column(
                Modifier
                    .fillMaxSize()
                    .safeDrawingPadding()
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                // Cabecera: la moto cruza en marcha sobre el rojo.
                Column(
                    Modifier
                        .widthIn(max = 480.dp)
                        .fillMaxWidth()
                        .padding(horizontal = 28.dp, vertical = if (landscape) 8.dp else 24.dp)
                        .enterFromBelow(0),
                ) {
                    Text(stringResource(R.string.login_welcome), style = MaterialTheme.typography.labelLarge, color = Color.White.copy(alpha = 0.85f))
                    Text(stringResource(R.string.login_title), style = MaterialTheme.typography.headlineLarge, color = Color.White)
                    Text(stringResource(R.string.login_subtitle), style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.85f))
                    if (!landscape) {
                        MotoRider(Modifier.fillMaxWidth().height(150.dp).padding(top = 8.dp), running = true)
                    }
                }

                Card(
                    Modifier
                        .widthIn(max = 480.dp)
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .padding(bottom = 24.dp)
                        .enterFromBelow(2),
                    shape = MaterialTheme.shapes.extraLarge,
                    colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surfaceContainerLowest),
                    elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                ) {
                    Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(stringResource(R.string.login_card_title), style = MaterialTheme.typography.titleLarge)
                        OutlinedTextField(
                            value = document,
                            onValueChange = { v -> document = v.filter(Char::isDigit).take(MAX_DOCUMENT) },
                            label = { Text(stringResource(R.string.login_document)) },
                            leadingIcon = { Icon(Icons.Rounded.Badge, null) },
                            singleLine = true,
                            shape = MaterialTheme.shapes.medium,
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
                            leadingIcon = { Icon(Icons.Rounded.Lock, null) },
                            singleLine = true,
                            shape = MaterialTheme.shapes.medium,
                            modifier = Modifier.fillMaxWidth(),
                            visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                            trailingIcon = {
                                IconButton({ showPassword = !showPassword }, Modifier.size(48.dp)) {
                                    Icon(
                                        if (showPassword) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                                        stringResource(if (showPassword) R.string.login_hide else R.string.login_show),
                                    )
                                }
                            },
                            isError = submitted && !passwordOk,
                            supportingText = {
                                if (submitted && !passwordOk) Text(stringResource(R.string.login_error_password))
                            },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(onDone = { submit() }),
                        )
                        PrimaryButton(stringResource(R.string.login_submit), { submit() }, icon = Icons.Rounded.Login, loading = loading)
                        AnimatedVisibility(error != null, enter = fadeIn() + expandVertically(), exit = fadeOut() + shrinkVertically()) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(Icons.Rounded.ErrorOutline, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.error)
                                Text(
                                    stringResource(
                                        when (error) {
                                            LoginError.INVALID -> R.string.login_error_invalid
                                            LoginError.LOCKED -> R.string.login_error_locked
                                            LoginError.NETWORK -> R.string.login_error_network
                                            LoginError.SESSION_EXPIRED -> R.string.login_error_session
                                            LoginError.GENERIC, null -> R.string.login_error_generic
                                        }
                                    ),
                                    color = MaterialTheme.colorScheme.error,
                                    style = MaterialTheme.typography.bodyMedium,
                                )
                            }
                        }
                        Spacer(Modifier.height(2.dp))
                        Text(
                            stringResource(R.string.login_help),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

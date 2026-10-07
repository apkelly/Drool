package com.github.apkelly.drool.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.ContentType
import androidx.compose.ui.semantics.contentType
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.github.apkelly.drool.resources.Res
import com.github.apkelly.drool.resources.action_back
import com.github.apkelly.drool.resources.action_create_account
import com.github.apkelly.drool.resources.action_retry
import com.github.apkelly.drool.resources.action_sign_in
import com.github.apkelly.drool.resources.auth_create_body
import com.github.apkelly.drool.resources.auth_create_title
import com.github.apkelly.drool.resources.auth_failed
import com.github.apkelly.drool.resources.auth_missing_fields
import com.github.apkelly.drool.resources.auth_offline
import com.github.apkelly.drool.resources.auth_password
import com.github.apkelly.drool.resources.auth_password_hide
import com.github.apkelly.drool.resources.auth_password_show
import com.github.apkelly.drool.resources.auth_reconnect_body
import com.github.apkelly.drool.resources.auth_reconnect_title
import com.github.apkelly.drool.resources.auth_sign_in_body
import com.github.apkelly.drool.resources.auth_sign_in_title
import com.github.apkelly.drool.resources.auth_username
import com.github.apkelly.drool.resources.auth_unknown
import com.github.apkelly.drool.resources.welcome_body
import com.github.apkelly.drool.resources.welcome_title
import com.github.apkelly.drool.ui.model.AuthFailure
import com.github.apkelly.drool.ui.widgets.MaterialSymbol
import com.github.apkelly.drool.ui.widgets.MaterialSymbolIcon
import org.jetbrains.compose.resources.stringResource

@Composable
fun WelcomeScreen(
    onSignIn: () -> Unit,
    onCreateAccount: () -> Unit,
) {
    CenteredAuthColumn {
        MaterialSymbolIcon(
            symbol = MaterialSymbol.SportsSoccer,
            contentDescription = null,
            size = 92.dp,
            tint = MaterialTheme.colorScheme.primary,
        )
        Text(
            stringResource(Res.string.welcome_title),
            style = MaterialTheme.typography.headlineMedium,
        )
        Text(
            stringResource(Res.string.welcome_body),
            style = MaterialTheme.typography.bodyLarge,
        )
        Spacer(Modifier.height(12.dp))
        Button(onClick = onSignIn, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(Res.string.action_sign_in))
        }
        OutlinedButton(onClick = onCreateAccount, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(Res.string.action_create_account))
        }
    }
}

@Composable
fun SignInScreen(
    loading: Boolean,
    failure: AuthFailure?,
    onBack: () -> Unit,
    onSignIn: (String, String) -> Unit,
) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    CenteredAuthColumn {
        Text(
            stringResource(Res.string.auth_sign_in_title),
            style = MaterialTheme.typography.headlineMedium,
        )
        Text(stringResource(Res.string.auth_sign_in_body))
        OutlinedTextField(
            value = username,
            onValueChange = { username = it },
            label = { Text(stringResource(Res.string.auth_username)) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Next,
            ),
            modifier = Modifier
                .fillMaxWidth()
                .semantics { contentType = ContentType.Username },
        )
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text(stringResource(Res.string.auth_password)) },
            singleLine = true,
            visualTransformation = if (passwordVisible) {
                VisualTransformation.None
            } else {
                PasswordVisualTransformation()
            },
            trailingIcon = {
                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    MaterialSymbolIcon(
                        symbol = if (passwordVisible) {
                            MaterialSymbol.VisibilityOff
                        } else {
                            MaterialSymbol.Visibility
                        },
                        contentDescription = stringResource(
                            if (passwordVisible) {
                                Res.string.auth_password_hide
                            } else {
                                Res.string.auth_password_show
                            }
                        ),
                    )
                }
            },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Done,
            ),
            keyboardActions = KeyboardActions(
                onDone = {
                    if (!loading) onSignIn(username, password)
                },
            ),
            modifier = Modifier
                .fillMaxWidth()
                .semantics { contentType = ContentType.Password },
        )
        if (failure != null) {
            val message = when (failure) {
                AuthFailure.MissingFields -> Res.string.auth_missing_fields
                AuthFailure.InvalidCredentials -> Res.string.auth_failed
                AuthFailure.Offline -> Res.string.auth_offline
                AuthFailure.Unknown -> Res.string.auth_unknown
            }
            Text(
                stringResource(message),
                color = MaterialTheme.colorScheme.error,
            )
        }
        Button(
            onClick = { onSignIn(username, password) },
            enabled = !loading,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(Res.string.action_sign_in))
        }
        OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(Res.string.action_back))
        }
    }
}

@Composable
fun RegisterScreen(onBack: () -> Unit) {
    CenteredAuthColumn {
        Text(
            stringResource(Res.string.auth_create_title),
            style = MaterialTheme.typography.headlineMedium,
        )
        Text(stringResource(Res.string.auth_create_body))
        OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(Res.string.action_back))
        }
    }
}

@Composable
fun ReconnectScreen(onRetry: () -> Unit) {
    CenteredAuthColumn {
        Text(
            stringResource(Res.string.auth_reconnect_title),
            style = MaterialTheme.typography.headlineMedium,
        )
        Text(stringResource(Res.string.auth_reconnect_body))
        Button(onClick = onRetry, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(Res.string.action_retry))
        }
    }
}

@Composable
private fun CenteredAuthColumn(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Column(
            modifier = Modifier.widthIn(max = 460.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            content = content,
        )
    }
}

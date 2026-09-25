package com.streamvault.feature.catalog.presentation.clipbox

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.streamvault.core.navigation.AppDestination
import com.streamvault.core.ui.interaction.TvButton
import com.streamvault.data.remote.clipbox.ClipboxAuthRepository
import com.streamvault.data.remote.clipbox.ClipboxLoginOutcome
import com.streamvault.feature.catalog.api.CatalogNavigationChrome
import com.streamvault.feature.catalog.api.CatalogScaffoldContent
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class ClipboxAccountViewModel @Inject constructor(private val auth: ClipboxAuthRepository) : ViewModel() {
    val account = auth.account
    private val _busy = MutableStateFlow(false)
    val busy = _busy.asStateFlow()
    private val _message = MutableStateFlow<String?>(null)
    val message = _message.asStateFlow()

    fun validateSession() {
        if (!account.value.signedIn) return
        viewModelScope.launch { auth.validateSession() }
    }

    fun login(username: String, password: String) {
        if (_busy.value) return
        _busy.value = true
        _message.value = null
        viewModelScope.launch {
            val outcome = auth.login(username, password)
            _message.value = when (outcome) {
                ClipboxLoginOutcome.SUCCESS -> "החשבון מחובר"
                ClipboxLoginOutcome.INVALID_CREDENTIALS -> "שם המשתמש או הסיסמה אינם נכונים"
                ClipboxLoginOutcome.RATE_LIMITED -> "ניסיונות רבים מדי. נסה שוב מאוחר יותר"
                ClipboxLoginOutcome.SECURE_STORAGE_UNAVAILABLE -> "אי אפשר לשמור את החשבון בצורה מאובטחת במכשיר זה"
                ClipboxLoginOutcome.UNAVAILABLE -> "שירות החשבון אינו זמין כרגע"
            }
            _busy.value = false
        }
    }

    fun logout() {
        if (_busy.value) return
        _busy.value = true
        viewModelScope.launch {
            _message.value = if (auth.logout()) "החשבון נותק. אפשר להמשיך כאורח"
            else "לא ניתן לנתק את החשבון בצורה מאובטחת כרגע"
            _busy.value = false
        }
    }
}

@Composable
fun ClipboxAccountScreen(
    onBack: () -> Unit,
    scaffold: CatalogScaffoldContent,
    viewModel: ClipboxAccountViewModel = hiltViewModel(),
) {
    BackHandler(onBack = onBack)
    val account by viewModel.account.collectAsStateWithLifecycle()
    val busy by viewModel.busy.collectAsStateWithLifecycle()
    val message by viewModel.message.collectAsStateWithLifecycle()
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    LaunchedEffect(Unit) { viewModel.validateSession() }
    LaunchedEffect(account.signedIn) { if (account.signedIn) password = "" }

    scaffold(AppDestination.ClipboxAccount, "חשבון Clipbox", null, CatalogNavigationChrome.TopBar, true, true, false) {
        Column(
            modifier = Modifier.fillMaxSize().padding(32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            TvButton(onClick = {
                password = ""
                onBack()
            }) { Text("חזרה") }
            if (account.signedIn) {
                Text("מחובר בתור ${account.username.orEmpty()}", style = MaterialTheme.typography.headlineMedium)
                TvButton(onClick = viewModel::logout, enabled = !busy) { Text("התנתק") }
            } else {
                Text("אפשר להמשיך לגלוש כאורח. התחברות נדרשת רק לסנכרון חשבון.")
                if (account.loginRequired) Text("פג תוקף החיבור. התחבר שוב כדי להשתמש בנתוני החשבון.")
                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = { androidx.compose.material3.Text("שם משתמש") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().widthIn(max = 480.dp),
                )
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { androidx.compose.material3.Text("סיסמה") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth().widthIn(max = 480.dp),
                )
                TvButton(
                    onClick = { viewModel.login(username, password) },
                    enabled = !busy && username.isNotBlank() && password.isNotBlank(),
                ) { Text(if (busy) "מתחבר…" else "התחבר") }
            }
            message?.let { Text(it) }
        }
    }
}

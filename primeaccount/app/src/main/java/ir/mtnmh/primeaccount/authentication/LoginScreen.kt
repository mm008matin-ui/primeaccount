package ir.mtnmh.primeaccount.authentication

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import ir.mtnmh.primeaccount.R
import ir.mtnmh.primeaccount.core.components.PrimeButton
import ir.mtnmh.primeaccount.core.components.PrimeTextField
import ir.mtnmh.primeaccount.core.components.PrimeToolbar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    viewModel: AuthViewModel,
    onLoginSuccess: () -> Unit,
    onNavigateToRegister: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val emailState by viewModel.email.collectAsState()
    val passwordState by viewModel.password.collectAsState()
    val uiState by viewModel.uiState.collectAsState()

    val languageManager = ir.mtnmh.primeaccount.utils.LocalLanguageManager.current
    val isRtl = languageManager.currentLanguageFlow == ir.mtnmh.primeaccount.utils.AppLanguage.PERSIAN

    var showError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(uiState) {
        if (uiState is AuthUiState.Error) {
            showError = (uiState as AuthUiState.Error).message
        } else {
            showError = null
        }
    }

    Scaffold(
        topBar = {
            PrimeToolbar(
                title = stringResource(id = R.string.login_title),
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        },
        modifier = modifier.testTag("login_screen")
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Image(
                    painter = painterResource(id = R.drawable.img_prime_logo),
                    contentDescription = "PrimeAccount Logo",
                    modifier = Modifier
                        .size(120.dp)
                        .padding(bottom = 8.dp)
                )

                Text(
                    text = "خوش آمدید / Welcome",
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.align(Alignment.Start)
                )

                Text(
                    text = "برای ادامه وارد حساب خود شوید.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.align(Alignment.Start)
                )

                Spacer(modifier = Modifier.height(8.dp))

                PrimeTextField(
                    value = emailState,
                    onValueChange = { viewModel.email.value = it },
                    label = stringResource(id = R.string.email_placeholder),
                    leadingIcon = {
                        Icon(imageVector = Icons.Default.Email, contentDescription = null)
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    isError = showError != null,
                    errorMessage = showError
                )

                PrimeTextField(
                    value = passwordState,
                    onValueChange = { viewModel.password.value = it },
                    label = stringResource(id = R.string.password_placeholder),
                    leadingIcon = {
                        Icon(imageVector = Icons.Default.Lock, contentDescription = null)
                    },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                PrimeButton(
                    text = stringResource(id = R.string.login_title),
                    onClick = {
                        viewModel.login(onSuccess = onLoginSuccess)
                    },
                    isLoading = uiState is AuthUiState.Loading,
                    modifier = Modifier.fillMaxWidth()
                )

                TextButton(
                    onClick = {
                        viewModel.loginAsGuest(onSuccess = onLoginSuccess)
                    },
                    modifier = Modifier.testTag("btn_guest_login")
                ) {
                    Text(
                        text = if (isRtl) "ورود به عنوان مهمان / Browse as Guest" else "Browse as Guest",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    )
                }

                TextButton(
                    onClick = onNavigateToRegister,
                    modifier = Modifier.testTag("btn_goto_register")
                ) {
                    Text(
                        text = stringResource(id = R.string.no_account),
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                }
            }
        }
    }
}

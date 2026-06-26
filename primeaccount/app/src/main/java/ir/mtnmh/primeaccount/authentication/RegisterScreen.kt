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
import androidx.compose.material.icons.filled.Person
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
import ir.mtnmh.primeaccount.utils.LocalLanguageManager
import ir.mtnmh.primeaccount.utils.AppLanguage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterScreen(
    viewModel: AuthViewModel,
    onRegisterSuccess: () -> Unit,
    onNavigateToLogin: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val languageManager = LocalLanguageManager.current
    val isRtl = languageManager.currentLanguageFlow == AppLanguage.PERSIAN

    val emailState by viewModel.email.collectAsState()
    val passwordState by viewModel.password.collectAsState()
    val fullNameState by viewModel.fullName.collectAsState()
    val uiState by viewModel.uiState.collectAsState()

    var showError by remember { mutableStateOf<String?>(null) }
    var showSuccessDialog by remember { mutableStateOf(false) }

    LaunchedEffect(uiState) {
        if (uiState is AuthUiState.PendingApproval) {
            showSuccessDialog = true
        } else if (uiState is AuthUiState.Error) {
            showError = (uiState as AuthUiState.Error).message
        } else {
            showError = null
        }
    }

    if (showSuccessDialog) {
        AlertDialog(
            onDismissRequest = { },
            title = {
                Text(
                    text = if (isRtl) "ثبت‌نام موفقیت‌آمیز" else "Registration Successful",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = if (isRtl) 
                        "ثبت‌نام شما انجام شد. حساب کاربری شما در انتظار تایید مدیریت است." 
                        else "Your account is waiting for administrator approval.",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showSuccessDialog = false
                        viewModel.resetState()
                        onNavigateToLogin()
                    }
                ) {
                    Text(text = if (isRtl) "تایید" else "OK")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            PrimeToolbar(
                title = stringResource(id = R.string.register_title),
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
        modifier = modifier.testTag("register_screen")
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
                    text = "ایجاد حساب جدید / Create Account",
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.align(Alignment.Start)
                )

                Text(
                    text = "برای شروع کار با پرایم اکانت فرم زیر را تکمیل کنید.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.align(Alignment.Start)
                )

                Spacer(modifier = Modifier.height(8.dp))

                PrimeTextField(
                    value = fullNameState,
                    onValueChange = { viewModel.fullName.value = it },
                    label = stringResource(id = R.string.name_placeholder),
                    leadingIcon = {
                        Icon(imageVector = Icons.Default.Person, contentDescription = null)
                    },
                    isError = showError != null,
                    errorMessage = showError
                )

                PrimeTextField(
                    value = emailState,
                    onValueChange = { viewModel.email.value = it },
                    label = stringResource(id = R.string.email_placeholder),
                    leadingIcon = {
                        Icon(imageVector = Icons.Default.Email, contentDescription = null)
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
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
                    text = stringResource(id = R.string.register_title),
                    onClick = {
                        viewModel.register(onSuccess = onRegisterSuccess)
                    },
                    isLoading = uiState is AuthUiState.Loading,
                    modifier = Modifier.fillMaxWidth()
                )

                TextButton(
                    onClick = onNavigateToLogin,
                    modifier = Modifier.testTag("btn_goto_login")
                ) {
                    Text(
                        text = stringResource(id = R.string.already_have_account),
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                }
            }
        }
    }
}

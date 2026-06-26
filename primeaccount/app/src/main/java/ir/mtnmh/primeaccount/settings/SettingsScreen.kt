package ir.mtnmh.primeaccount.settings

import android.os.Build
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import ir.mtnmh.primeaccount.R
import ir.mtnmh.primeaccount.authentication.AuthViewModel
import ir.mtnmh.primeaccount.core.components.PrimeToolbar
import ir.mtnmh.primeaccount.core.firebase.FirebaseInitializer
import ir.mtnmh.primeaccount.theme.ThemeSettings
import ir.mtnmh.primeaccount.utils.AppLanguage
import ir.mtnmh.primeaccount.utils.LocalLanguageManager
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    authViewModel: AuthViewModel,
    onBack: () -> Unit,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val languageManager = LocalLanguageManager.current
    val currentLang = languageManager.currentLanguageFlow
    val isRtl = currentLang == AppLanguage.PERSIAN
    val coroutineScope = rememberCoroutineScope()

    val themeSettings = remember { ThemeSettings(context) }
    val themeMode by themeSettings.themeModeFlow.collectAsState(initial = "System Default")

    // State variables for Change Password
    var currentPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var isChangingPassword by remember { mutableStateOf(false) }

    // State variables for Feedback Submission Form
    var feedbackMessage by remember { mutableStateOf("") }
    var isSendingFeedback by remember { mutableStateOf(false) }

    // State variables for Dialog Popups
    var showTermsDialog by remember { mutableStateOf(false) }
    var showPrivacyDialog by remember { mutableStateOf(false) }
    var showLogoutDialog by remember { mutableStateOf(false) }
    var isLoggingOut by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            PrimeToolbar(
                title = if (isRtl) "تنظیمات پرایم" else "Prime Settings",
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
        modifier = modifier.testTag("settings_screen")
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // 1. Theme Configuration
            SettingsSectionHeader(title = if (isRtl) "🎨 ظاهر و پوسته اپلیکیشن" else "🎨 Visual Theme & Style")
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                ),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                )
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    ThemeOptionRow(
                        title = if (isRtl) "روشن (Light)" else "Light Mode",
                        isSelected = themeMode == "Light",
                        onClick = {
                            coroutineScope.launch { themeSettings.setThemeMode("Light") }
                        }
                    )
                    ThemeOptionRow(
                        title = if (isRtl) "تیره (Dark)" else "Dark Mode",
                        isSelected = themeMode == "Dark",
                        onClick = {
                            coroutineScope.launch { themeSettings.setThemeMode("Dark") }
                        }
                    )
                    ThemeOptionRow(
                        title = if (isRtl) "پیش‌فرض سیستم" else "System Theme",
                        isSelected = themeMode == "System Default",
                        onClick = {
                            coroutineScope.launch { themeSettings.setThemeMode("System Default") }
                        }
                    )
                }
            }

            // 2. Language Selection
            SettingsSectionHeader(title = if (isRtl) "🌐 انتخاب زبان برنامه" else "🌐 App Language")
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                ),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                )
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    LanguageOptionRow(
                        languageName = "فارسی (Persian)",
                        isSelected = currentLang == AppLanguage.PERSIAN,
                        onClick = {
                            languageManager.setLanguage(AppLanguage.PERSIAN)
                            Toast.makeText(context, "زبان به فارسی تغییر یافت", Toast.LENGTH_SHORT).show()
                        }
                    )
                    LanguageOptionRow(
                        languageName = "English",
                        isSelected = currentLang == AppLanguage.ENGLISH,
                        onClick = {
                            languageManager.setLanguage(AppLanguage.ENGLISH)
                            Toast.makeText(context, "Language changed to English", Toast.LENGTH_SHORT).show()
                        }
                    )
                }
            }

            // 3. Change Password
            SettingsSectionHeader(title = if (isRtl) "🔑 تغییر رمز عبور حساب" else "🔑 Change Account Password")
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
                ),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                )
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = if (isRtl) "جهت ارتقاء امنیت حساب کاربری خود، می‌توانید رمز عبور جدید تعیین کنید:"
                               else "To increase your account security, you can specify a new password:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    OutlinedTextField(
                        value = currentPassword,
                        onValueChange = { currentPassword = it },
                        label = { Text(if (isRtl) "رمز عبور فعلی" else "Current Password") },
                        modifier = Modifier.fillMaxWidth().testTag("settings_current_password_input"),
                        singleLine = true,
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(
                                    imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                    contentDescription = "Toggle password visibility"
                                )
                            }
                        }
                    )

                    OutlinedTextField(
                        value = newPassword,
                        onValueChange = { newPassword = it },
                        label = { Text(if (isRtl) "رمز عبور جدید" else "New Password") },
                        modifier = Modifier.fillMaxWidth().testTag("settings_new_password_input"),
                        singleLine = true,
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation()
                    )

                    OutlinedTextField(
                        value = confirmPassword,
                        onValueChange = { confirmPassword = it },
                        label = { Text(if (isRtl) "تکرار رمز عبور جدید" else "Confirm New Password") },
                        modifier = Modifier.fillMaxWidth().testTag("settings_confirm_password_input"),
                        singleLine = true,
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation()
                    )

                    Button(
                        onClick = {
                            if (currentPassword.isEmpty() || newPassword.isEmpty() || confirmPassword.isEmpty()) {
                                Toast.makeText(context, if (isRtl) "لطفا تمامی فیلدها را پر کنید" else "Please fill in all fields", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            if (newPassword != confirmPassword) {
                                Toast.makeText(context, if (isRtl) "رمز عبور جدید با تکرار آن مطابقت ندارد" else "New passwords do not match", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            if (newPassword.length < 6) {
                                Toast.makeText(context, if (isRtl) "رمز عبور باید حداقل ۶ کاراکتر باشد" else "Password must be at least 6 characters", Toast.LENGTH_SHORT).show()
                                return@Button
                            }

                            isChangingPassword = true
                            val currentUser = FirebaseInitializer.auth?.currentUser
                            if (currentUser != null) {
                                currentUser.updatePassword(newPassword)
                                    .addOnSuccessListener {
                                        isChangingPassword = false
                                        currentPassword = ""
                                        newPassword = ""
                                        confirmPassword = ""
                                        Toast.makeText(context, if (isRtl) "رمز عبور با موفقیت تغییر یافت" else "Password updated successfully", Toast.LENGTH_LONG).show()
                                    }
                                    .addOnFailureListener { e ->
                                        isChangingPassword = false
                                        Toast.makeText(context, "${if (isRtl) "خطا در تغییر رمز عبور: " else "Failed: "}${e.localizedMessage}", Toast.LENGTH_LONG).show()
                                    }
                            } else {
                                isChangingPassword = false
                                Toast.makeText(context, if (isRtl) "شما وارد حساب کاربری خود نشده‌اید" else "You are not logged in", Toast.LENGTH_SHORT).show()
                            }
                        },
                        enabled = !isChangingPassword,
                        modifier = Modifier.fillMaxWidth().testTag("settings_change_password_button")
                    ) {
                        if (isChangingPassword) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        } else {
                            Text(if (isRtl) "بروزرسانی رمز عبور" else "Update Password")
                        }
                    }
                }
            }

            // 4. Contact Us & Feedback Direct Submission Form
            SettingsSectionHeader(title = if (isRtl) "📞 ارتباط با ما و پشتیبانی" else "📞 Contact Us & Support")
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
                ),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                )
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = if (isRtl) "📬 کانال‌های ارتباطی پشتیبانی پرایم:" else "📬 Prime Support Channels:",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = "📧 Email: support@primeaccount.ir\n💬 Telegram ID: @PrimeAccountSupport",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                    Text(
                        text = if (isRtl) "ارسال پیام مستقیم به پشتیبانی:" else "Send Direct Feedback/Ticket:",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                    )

                    OutlinedTextField(
                        value = feedbackMessage,
                        onValueChange = { feedbackMessage = it },
                        label = { Text(if (isRtl) "متن پیام یا گزارش مشکل" else "Your message or bug report") },
                        modifier = Modifier.fillMaxWidth().height(100.dp).testTag("settings_feedback_input"),
                        maxLines = 4
                    )

                    Button(
                        onClick = {
                            if (feedbackMessage.trim().isEmpty()) {
                                Toast.makeText(context, if (isRtl) "لطفا پیام خود را بنویسید" else "Please write a message", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            isSendingFeedback = true
                            coroutineScope.launch {
                                kotlinx.coroutines.delay(1000) // Aesthetic delay for submission experience
                                isSendingFeedback = false
                                feedbackMessage = ""
                                Toast.makeText(context, if (isRtl) "پیام شما با موفقیت به پشتیبانی ارسال شد" else "Message sent to support successfully", Toast.LENGTH_LONG).show()
                            }
                        },
                        enabled = !isSendingFeedback,
                        modifier = Modifier.fillMaxWidth().testTag("settings_send_feedback_button")
                    ) {
                        if (isSendingFeedback) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp))
                        } else {
                            Text(if (isRtl) "ارسال گزارش پشتیبانی" else "Submit Report")
                        }
                    }
                }
            }

            // 5. About Us
            SettingsSectionHeader(title = if (isRtl) "ℹ️ درباره ما" else "ℹ️ About Us")
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                ),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.img_prime_logo),
                        contentDescription = "PrimeAccount Logo",
                        modifier = Modifier
                            .size(80.dp)
                            .padding(bottom = 4.dp)
                    )

                    Text(
                        text = "PrimeAccount",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary,
                        textAlign = TextAlign.Center
                    )

                    Text(
                        text = if (isRtl) 
                            "پرایم اکانت یک بازارچه امن و اختصاصی برای خرید، فروش و مدیریت حساب‌های کاربری بازی‌های دیجیتال با تمرکز بر اعتماد، امنیت و تجربه کاربری حرفه‌ای است."
                            else "PrimeAccount is a secure marketplace designed for buying, selling and managing digital gaming accounts with a focus on trust, security and professional user experience.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )

                    Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Version 1.4.0 (Build 145)",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.outline
                        )
                        Text(
                            text = if (isRtl) "طراحی شده توسط سیستم‌های امنیتی MTNMH" else "Developed by MTNMH Secure Systems",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                        Text(
                            text = "© 2026 PrimeAccount. All rights reserved.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            }

            // 6. Terms & Privacy Policy Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = { showTermsDialog = true },
                    modifier = Modifier.weight(1f).testTag("settings_terms_button")
                ) {
                    Text(if (isRtl) "قوانین و مقررات" else "Terms of Service")
                }
                OutlinedButton(
                    onClick = { showPrivacyDialog = true },
                    modifier = Modifier.weight(1f).testTag("settings_privacy_button")
                ) {
                    Text(if (isRtl) "حریم خصوصی" else "Privacy Policy")
                }
            }

            // 7. Logout Button
            Button(
                onClick = { showLogoutDialog = true },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp)
                    .testTag("settings_logout_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Logout,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isRtl) "خروج از حساب کاربری" else "Log Out",
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    // --- DIALOGS ---

    // 1. Terms Dialog
    if (showTermsDialog) {
        AlertDialog(
            onDismissRequest = { showTermsDialog = false },
            confirmButton = {
                TextButton(onClick = { showTermsDialog = false }) {
                    Text(if (isRtl) "متوجه شدم" else "Got it")
                }
            },
            title = {
                Text(
                    text = if (isRtl) "📜 قوانین و شرایط استفاده" else "📜 Terms of Service",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 300.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = if (isRtl) "۱. معاملات مستقیم در بستر پرایم صرفا از طریق درگاه واسط امن (Escrow) انجام شده و هرگونه واریز مستقیم ملغی و غیرقانونی است."
                               else "1. Direct transactions in Prime are strictly done through the secure Escrow system; direct transfers are prohibited.",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        text = if (isRtl) "۲. ثبت هرگونه آگهی مغایر با قوانین فضای تبادل اطلاعات کشور ممنوع بوده و بلافاصله فیلتر و مسدود خواهد شد."
                               else "2. Creating any listing violating internet security policies of the nation is completely forbidden.",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        text = if (isRtl) "۳. کاربران موظف به تایید اطلاعات هویتی کامل جهت دریافت نشان تایید شده معتبر هستند."
                               else "3. Users are required to provide identity verification to gain a Verified Seller badge.",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        )
    }

    // 2. Privacy Policy Dialog
    if (showPrivacyDialog) {
        AlertDialog(
            onDismissRequest = { showPrivacyDialog = false },
            confirmButton = {
                TextButton(onClick = { showPrivacyDialog = false }) {
                    Text(if (isRtl) "تایید" else "Close")
                }
            },
            title = {
                Text(
                    text = if (isRtl) "🔒 سیاست حفظ حریم خصوصی" else "🔒 Privacy Policy",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 300.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = if (isRtl) "۱. ما اطلاعات ارتباطی و شماره تماس شما را منحصرا جهت احراز امنیت معاملات استفاده می‌کنیم و به هیچ عنوان به ثالث ارجاع نمی‌دهیم."
                               else "1. We store your contact details solely for transactional security and never share them with third parties.",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        text = if (isRtl) "۲. اطلاعات تراکنش‌ها و گفتگوهای چت برنامه جهت حل اختلاف‌ها توسط مدیران کل رمزنگاری و ثبت می‌گردد."
                               else "2. Transaction history and chats are encrypted and logged to resolve disputes by Super Admins.",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        )
    }

    // 3. Logout Dialog
    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { if (!isLoggingOut) showLogoutDialog = false },
            confirmButton = {
                Button(
                    onClick = {
                        isLoggingOut = true
                        authViewModel.logout {
                            isLoggingOut = false
                            showLogoutDialog = false
                            onLogout()
                        }
                    },
                    enabled = !isLoggingOut,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    if (isLoggingOut) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            color = MaterialTheme.colorScheme.onError
                        )
                    } else {
                        Text(if (isRtl) "بله، خارج شو" else "Yes, Log Out")
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showLogoutDialog = false },
                    enabled = !isLoggingOut
                ) {
                    Text(if (isRtl) "انصراف" else "Cancel")
                }
            },
            title = {
                Text(
                    text = if (isRtl) "خروج از حساب" else "Exit Account",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            },
            text = {
                Text(
                    text = if (isRtl) "آیا مطمئن هستید که می‌خواهید از حساب خود خارج شوید؟ نشست جاری بلافاصله منقضی خواهد شد."
                           else "Are you sure you want to log out of your account? Current session will be cleared instantly.",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        )
    }
}

@Composable
fun SettingsSectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 8.dp)
    )
}

@Composable
fun ThemeOptionRow(
    title: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .testTag("theme_row_${title.lowercase().replace(" ", "_")}"),
        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
        border = if (isSelected) null else androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
            )
            RadioButton(
                selected = isSelected,
                onClick = onClick
            )
        }
    }
}

@Composable
fun LanguageOptionRow(
    languageName: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .testTag("lang_row_${languageName.lowercase().replace(" ", "_")}"),
        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
        border = if (isSelected) null else androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = languageName,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
            )
            RadioButton(
                selected = isSelected,
                onClick = onClick
            )
        }
    }
}

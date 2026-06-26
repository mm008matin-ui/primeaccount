package ir.mtnmh.primeaccount.utils

import android.content.Context
import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import java.util.Locale

enum class AppLanguage(val code: String, val displayName: String, val layoutDirection: LayoutDirection) {
    PERSIAN("fa", "فارسی", LayoutDirection.Rtl),
    ENGLISH("en", "English", LayoutDirection.Ltr)
}

class LanguageManager(private val context: Context) {
    private val prefs = context.getSharedPreferences("prime_account_settings", Context.MODE_PRIVATE)

    var currentLanguageFlow by mutableStateOf(getSavedLanguage())
        private set

    private fun getSavedLanguage(): AppLanguage {
        val code = prefs.getString("selected_language", AppLanguage.PERSIAN.code) ?: AppLanguage.PERSIAN.code
        return AppLanguage.values().firstOrNull { it.code == code } ?: AppLanguage.PERSIAN
    }

    fun setLanguage(language: AppLanguage) {
        prefs.edit().putString("selected_language", language.code).apply()
        currentLanguageFlow = language
        updateResources(context, language.code)
    }

    private fun updateResources(context: Context, languageCode: String) {
        val locale = Locale(languageCode)
        Locale.setDefault(locale)
        val resources = context.resources
        val configuration = Configuration(resources.configuration)
        configuration.setLocale(locale)
        context.createConfigurationContext(configuration)
    }
}

val LocalLanguageManager = compositionLocalOf<LanguageManager> {
    error("No LanguageManager provided")
}

@Composable
fun ProvideLanguage(
    languageManager: LanguageManager,
    content: @Composable () -> Unit
) {
    val currentLang = languageManager.currentLanguageFlow
    CompositionLocalProvider(
        LocalLanguageManager provides languageManager,
        LocalLayoutDirection provides currentLang.layoutDirection
    ) {
        content()
    }
}

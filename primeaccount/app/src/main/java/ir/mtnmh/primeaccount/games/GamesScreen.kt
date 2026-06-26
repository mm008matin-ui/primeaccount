package ir.mtnmh.primeaccount.games

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Gamepad
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import ir.mtnmh.primeaccount.R
import ir.mtnmh.primeaccount.core.components.EmptyState
import ir.mtnmh.primeaccount.core.components.PrimeToolbar
import ir.mtnmh.primeaccount.utils.AppLanguage
import ir.mtnmh.primeaccount.utils.LocalLanguageManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GamesScreen(
    modifier: Modifier = Modifier
) {
    val languageManager = LocalLanguageManager.current
    val isRtl = languageManager.currentLanguageFlow == AppLanguage.PERSIAN

    Scaffold(
        topBar = {
            PrimeToolbar(
                title = stringResource(id = R.string.games_title)
            )
        },
        modifier = modifier.testTag("games_screen")
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp)
        ) {
            EmptyState(
                title = if (isRtl) "بخش بازی‌ها" else "Games Directory",
                description = if (isRtl) "در فازهای بعدی، کاتالوگ کاملی از کلیه بازی‌های پشتیبانی‌شده به صورت پویا در این بخش افزوده خواهد شد."
                              else "In future phases, a comprehensive library of supported gaming titles will be dynamically managed here.",
                icon = Icons.Default.Gamepad
            )
        }
    }
}

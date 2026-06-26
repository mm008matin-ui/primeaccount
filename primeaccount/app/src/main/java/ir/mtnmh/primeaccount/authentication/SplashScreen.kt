package ir.mtnmh.primeaccount.authentication

import androidx.compose.animation.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import ir.mtnmh.primeaccount.R
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(
    onNavigateNext: () -> Unit,
    modifier: Modifier = Modifier
) {
    var animateStarted by remember { mutableStateOf(false) }

    LaunchedEffect(key1 = true) {
        delay(100)
        animateStarted = true
        delay(2000) // 2 seconds splash duration
        onNavigateNext()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.White)
            .testTag("splash_screen"),
        contentAlignment = Alignment.Center
    ) {
        AnimatedVisibility(
            visible = animateStarted,
            enter = fadeIn(animationSpec = androidx.compose.animation.core.tween(800)),
            exit = fadeOut(animationSpec = androidx.compose.animation.core.tween(500))
        ) {
            Image(
                painter = painterResource(id = R.drawable.img_prime_logo),
                contentDescription = "PrimeAccount Logo",
                modifier = Modifier
                    .size(240.dp)
                    .testTag("splash_logo"),
                contentScale = ContentScale.Fit
            )
        }
    }
}

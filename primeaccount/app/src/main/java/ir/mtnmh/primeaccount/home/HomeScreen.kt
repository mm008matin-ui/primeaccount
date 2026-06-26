package ir.mtnmh.primeaccount.home

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ir.mtnmh.primeaccount.R
import ir.mtnmh.primeaccount.core.components.GameCard
import ir.mtnmh.primeaccount.core.components.PrimeToolbar
import ir.mtnmh.primeaccount.core.firebase.NotificationManager
import ir.mtnmh.primeaccount.utils.AppLanguage
import ir.mtnmh.primeaccount.utils.LocalLanguageManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onGameSelect: (String) -> Unit,
    onNotificationsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val languageManager = LocalLanguageManager.current
    val isRtl = languageManager.currentLanguageFlow == AppLanguage.PERSIAN

    val gamesList by viewModel.gamesList.collectAsState()
    val userNotifications by NotificationManager.notifications.collectAsState()
    val systemAnnouncements by NotificationManager.announcements.collectAsState()

    val unreadCount = remember(userNotifications, systemAnnouncements) {
        userNotifications.count { !it.isRead && it.type != "MESSAGES" } + systemAnnouncements.count { !it.isRead }
    }

    var animateStarted by remember { mutableStateOf(false) }
    LaunchedEffect(key1 = true) {
        animateStarted = true
    }

    Scaffold(
        topBar = {
            // High fidelity styled header from Vibrant Palette specifications
            TopAppBar(
                title = {
                    Column {
                        Row(
                            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth().padding(end = 12.dp)
                        ) {
                            Text(
                                text = "PrimeAccount",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            )
                            Box(
                                modifier = Modifier
                                    .background(
                                        color = Color(0xFFFFD54F).copy(alpha = 0.2f),
                                        shape = RoundedCornerShape(50)
                                    )
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = "RELEASE V1.0",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFB88E00),
                                        fontSize = 9.sp
                                    )
                                )
                            }
                        }
                        Text(
                            text = "ir.mtnmh.primeaccount",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                fontSize = 10.sp
                            )
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = onNotificationsClick,
                        modifier = Modifier.testTag("home_notification_btn")
                    ) {
                        Box {
                            Icon(
                                imageVector = Icons.Default.Notifications,
                                contentDescription = "Notifications",
                                tint = MaterialTheme.colorScheme.primary
                            )
                            if (unreadCount > 0) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .background(MaterialTheme.colorScheme.error, shape = CircleShape)
                                        .align(androidx.compose.ui.Alignment.TopEnd)
                                )
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        modifier = modifier.testTag("home_screen")
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            // Header Hero Banner Area
            item {
                AnimatedVisibility(
                    visible = animateStarted,
                    enter = fadeIn() + slideInVertically { -it / 3 }
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(24.dp))
                            .background(
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                            )
                            .padding(24.dp)
                    ) {
                        Column {
                            Text(
                                text = if (isRtl) "بازار امن بازی‌های موبایلی" else "Secure Mobile Gaming Hub",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = if (isRtl) "خرید، فروش و معامله بی واسطه انواع اکانت بازی با استاندارد‌های نوین."
                                       else "Trade and exchange trusted gamer accounts with zero stress.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Games Grid Section Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isRtl) "بازی‌های پشتیبانی شده" else "Supported Games",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = if (isRtl) "مشاهده همه" else "View All",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    )
                }
            }

            // Display Two Game Cards Only (EA FC Mobile, eFootball) loaded dynamically
            items(gamesList) { game ->
                val localizedDesc = if (isRtl) game.description else game.descriptionEn
                val localizedBadge = if (isRtl) game.badge else game.badgeEn

                GameCard(
                    title = game.title,
                    subtitle = localizedDesc,
                    imageUrl = game.imageUrl,
                    priceLabel = if (isRtl) "مشاهده بازار آگهی‌ها" else "View Active Listings",
                    badgeLabel = localizedBadge,
                    placeholderIcon = Icons.Default.SportsEsports,
                    onClick = {
                        onGameSelect(game.title)
                    }
                )
            }

            // Vibrant Palette Architecture Status Card
            item {
                Spacer(modifier = Modifier.height(12.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.05f)
                    ),
                    border = androidx.compose.foundation.BorderStroke(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surface,
                            shadowElevation = 2.dp,
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(
                                contentAlignment = androidx.compose.ui.Alignment.Center,
                                modifier = Modifier.fillMaxSize()
                            ) {
                                Icon(
                                    imageVector = Icons.Default.SportsEsports,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        Column(
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = "ARCHITECTURE STATUS",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    letterSpacing = 1.sp
                                )
                            )
                            Text(
                                text = if (isRtl) "معماری پایه با موفقیت مقداردهی شد" else "Core Architecture Initialized",
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        // Blink/pulsating status indicator dot
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(Color(0xFF10B981), shape = CircleShape)
                        )
                    }
                }
            }
        }
    }
}

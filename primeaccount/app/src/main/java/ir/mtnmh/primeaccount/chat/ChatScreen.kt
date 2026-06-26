package ir.mtnmh.primeaccount.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import ir.mtnmh.primeaccount.R
import ir.mtnmh.primeaccount.core.components.EmptyState
import ir.mtnmh.primeaccount.core.components.PrimeToolbar
import ir.mtnmh.primeaccount.core.firebase.FirebaseDatabaseManager
import ir.mtnmh.primeaccount.utils.AppLanguage
import ir.mtnmh.primeaccount.utils.LocalLanguageManager
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    isGuest: Boolean = false,
    onLoginRequired: () -> Unit = {},
    onChatClick: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val languageManager = LocalLanguageManager.current
    val isRtl = languageManager.currentLanguageFlow == AppLanguage.PERSIAN

    if (isGuest) {
        Scaffold(
            topBar = {
                PrimeToolbar(title = stringResource(id = R.string.messages_title))
            },
            modifier = modifier.testTag("chat_screen")
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(72.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = if (isRtl) "ورود به حساب کاربری لازم است" else "Login Required",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (isRtl) "تنها کاربران ثبت‌نام شده و تایید شده می‌توانند با فروشندگان گفتگو کنند." 
                               else "Only logged-in and approved users can chat with sellers. Please log in or register to proceed.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = onLoginRequired,
                        shape = RoundedCornerShape(50),
                        modifier = Modifier.fillMaxWidth(0.6f).height(48.dp)
                    ) {
                        Text(text = if (isRtl) "ورود / ثبت‌نام" else "Log In / Register")
                    }
                }
            }
        }
        return
    }

    val activeChats by FirebaseDatabaseManager.activeChats.collectAsState()

    Scaffold(
        topBar = {
            PrimeToolbar(
                title = stringResource(id = R.string.messages_title)
            )
        },
        modifier = modifier.testTag("chat_screen")
    ) { innerPadding ->
        if (activeChats.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                EmptyState(
                    title = if (isRtl) "پیام‌های من" else "Active Chats",
                    description = if (isRtl) "هنوز گفتگویی شروع نکرده‌اید. برای شروع به صفحه جزییات یکی از آگهی‌ها بروید."
                                  else "You have no open message channels yet. Explore listings to start a secure chat.",
                    icon = Icons.Default.Forum
                )
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                items(activeChats) { chat ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onChatClick(chat.chatId) }
                            .testTag("chat_item_${chat.chatId}"),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Avatar section with online tag
                            Box {
                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    modifier = Modifier.size(50.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.Person,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                            modifier = Modifier.size(28.dp)
                                        )
                                    }
                                }
                                Box(
                                    modifier = Modifier
                                        .size(12.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF4CAF50))
                                        .align(Alignment.BottomEnd)
                                        .background(MaterialTheme.colorScheme.surface, shape = CircleShape)
                                        .padding(2.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(14.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    val currentUserId = ir.mtnmh.primeaccount.core.firebase.FirebaseInitializer.auth?.currentUser?.uid ?: ""
                                    val isCurrentBuyer = chat.participants.firstOrNull() == currentUserId
                                    Text(
                                        text = if (isCurrentBuyer) {
                                            if (isRtl) "فروشنده اکانت" else "Account Seller"
                                        } else {
                                            if (isRtl) "خریدار اکانت" else "Account Buyer"
                                        },
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    // Highlight badge in chat for seller if verified
                                    if (isCurrentBuyer) {
                                        Icon(
                                            imageVector = Icons.Default.VerifiedUser,
                                            contentDescription = "Verified 🛡",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(2.dp))

                                Text(
                                    text = chat.listingTitle,
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                                    color = MaterialTheme.colorScheme.primary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )

                                Text(
                                    text = chat.lastMessage,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
                            Text(
                                text = sdf.format(Date(chat.updatedAt)),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                        }
                    }
                }
            }
        }
    }
}

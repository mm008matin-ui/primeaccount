package ir.mtnmh.primeaccount.notifications

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ir.mtnmh.primeaccount.R
import ir.mtnmh.primeaccount.core.firebase.InAppNotification
import ir.mtnmh.primeaccount.core.firebase.NotificationManager
import ir.mtnmh.primeaccount.core.firebase.NotificationPreferences
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun NotificationCenterScreen(
    isGuest: Boolean = false,
    onLoginRequired: () -> Unit = {}
) {
    if (isGuest) {
        Box(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Lock,
                    contentDescription = null,
                    modifier = Modifier.size(72.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "برای دسترسی باید ثبت نام کنید یا وارد حساب خود شوید / Account Required",
                    style = MaterialTheme.typography.titleMedium,
                    textAlign = TextAlign.Center
                )
                Button(onClick = onLoginRequired) {
                    Text(text = "ورود / ثبت نام")
                }
            }
        }
        return
    }

    val context = LocalContext.current
    val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl

    // Subscribe to Firestore states
    val userNotifications by NotificationManager.notifications.collectAsState()
    val systemAnnouncements by NotificationManager.announcements.collectAsState()
    val prefs by NotificationManager.preferences.collectAsState()

    // Screen-local navigation and states
    var activeCategoryFilter by remember { mutableStateOf("ALL") } // ALL, MESSAGES, DEALS, LISTINGS, SECURITY, SYSTEM
    var showPreferencesSlider by remember { mutableStateOf(false) }
    var showAdminBroadcastPanel by remember { mutableStateOf(false) }

    // Admin Broadcast inputs
    var broadcastTitle by remember { mutableStateOf("") }
    var broadcastMessage by remember { mutableStateOf("") }

    // Combine standard collections with global public announcements
    val mergedFeed = remember(userNotifications, systemAnnouncements, activeCategoryFilter) {
        val totalFeed = (userNotifications + systemAnnouncements).sortedByDescending { it.createdAt }
        
        if (activeCategoryFilter == "ALL") {
            totalFeed
        } else {
            totalFeed.filter { it.type == activeCategoryFilter }
        }
    }

    val unreadCount = remember(userNotifications, systemAnnouncements) {
        userNotifications.count { !it.isRead } + systemAnnouncements.count { !it.isRead }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = if (isRtl) "مرکز اعلان‌ها و فعالیت‌ها" else "Notifications Center",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                        )
                        if (unreadCount > 0) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Badge(
                                containerColor = MaterialTheme.colorScheme.error,
                                modifier = Modifier.testTag("notification_badge_icon")
                            ) {
                                Text(unreadCount.toString(), color = Color.White, fontSize = 11.sp, modifier = Modifier.padding(horizontal = 4.dp))
                            }
                        }
                    }
                },
                actions = {
                    // Mark all as read action
                    IconButton(
                        onClick = {
                            NotificationManager.markAllAsRead()
                            Toast.makeText(context, if (isRtl) "همه با موفقیت خوانده شدند." else "All notifications marked as read.", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.testTag("notif_mark_all_btn")
                    ) {
                        Icon(imageVector = Icons.Default.DoneAll, contentDescription = "Mark All Read", tint = MaterialTheme.colorScheme.primary)
                    }

                    // Preferences toggle button
                    IconButton(
                        onClick = { showPreferencesSlider = !showPreferencesSlider },
                        modifier = Modifier.testTag("notif_settings_btn")
                    ) {
                        Icon(
                            imageVector = if (showPreferencesSlider) Icons.Default.Close else Icons.Default.Tune,
                            contentDescription = "Preferences",
                            tint = if (showPreferencesSlider) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                        )
                    }

                    // Admin simulator switch button
                    IconButton(
                        onClick = { showAdminBroadcastPanel = !showAdminBroadcastPanel },
                        modifier = Modifier.testTag("notif_admin_broadcast_toggle")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Campaign,
                            contentDescription = "Broadcast Center",
                            tint = if (showAdminBroadcastPanel) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.outline
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Preferences Slider
            AnimatedVisibility(
                visible = showPreferencesSlider,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .testTag("notif_pref_card"),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.NotificationsActive, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isRtl) "تنظیمات دریافت نوتیفیکیشن" else "Notification Channels Setup",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                        }

                        Divider()

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text(text = if (isRtl) "💬 پیام‌های گپ ایمن" else "💬 Chatroom / Messenger Messages", style = MaterialTheme.typography.bodyMedium)
                            Switch(
                                checked = prefs.messagesEnabled,
                                onCheckedChange = {
                                    NotificationManager.updatePreferences(prefs.copy(messagesEnabled = it))
                                },
                                modifier = Modifier.testTag("notif_pref_switch_msgs")
                            )
                        }

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text(text = if (isRtl) "🛡️ اطلاعات معامله معلق" else "🛡️ Escrow Deal updates", style = MaterialTheme.typography.bodyMedium)
                            Switch(
                                checked = prefs.dealsEnabled,
                                onCheckedChange = {
                                    NotificationManager.updatePreferences(prefs.copy(dealsEnabled = it))
                                },
                                modifier = Modifier.testTag("notif_pref_switch_deals")
                            )
                        }

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text(text = if (isRtl) "📋 تایید و رد آگهی" else "📋 Publication Approvals", style = MaterialTheme.typography.bodyMedium)
                            Switch(
                                checked = prefs.listingsEnabled,
                                onCheckedChange = {
                                    NotificationManager.updatePreferences(prefs.copy(listingsEnabled = it))
                                },
                                modifier = Modifier.testTag("notif_pref_switch_listings")
                            )
                        }

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text(text = if (isRtl) "📢 هشدارهای عمومی سیستم" else "📢 System Announcements", style = MaterialTheme.typography.bodyMedium)
                            Switch(
                                checked = prefs.announcementsEnabled,
                                onCheckedChange = {
                                    NotificationManager.updatePreferences(prefs.copy(announcementsEnabled = it))
                                },
                                modifier = Modifier.testTag("notif_pref_switch_ann")
                            )
                        }
                    }
                }
            }

            // Admin Broadcast Simulator
            AnimatedVisibility(
                visible = showAdminBroadcastPanel,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .testTag("admin_announcement_card"),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.25f)),
                    shape = RoundedCornerShape(16.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.tertiary.copy(alpha = 0.4f))
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.Campaign, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isRtl) "📢 مدیریت ارسال اطلاعیه عمومی (Admin)" else "📢 Broadcast Public Announcement (Admin)",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onTertiaryContainer)
                            )
                        }

                        OutlinedTextField(
                            value = broadcastTitle,
                            onValueChange = { broadcastTitle = it },
                            label = { Text(if (isRtl) "عنوان اطلاعیه" else "Announcement Title") },
                            placeholder = { Text(if (isRtl) "مثلا بهینه‌سازی سرور یا مسابقه ویژه" else "e.g., Security warn or Game expansion") },
                            modifier = Modifier.fillMaxWidth().testTag("admin_notif_title_input"),
                            shape = RoundedCornerShape(10.dp)
                        )

                        OutlinedTextField(
                            value = broadcastMessage,
                            onValueChange = { broadcastMessage = it },
                            label = { Text(if (isRtl) "متن پیام" else "Announcement Body") },
                            placeholder = { Text(if (isRtl) "جزئیات اطلاعیه را بنویسید..." else "Enter broadcast body...") },
                            modifier = Modifier.fillMaxWidth().testTag("admin_notif_body_input"),
                            shape = RoundedCornerShape(10.dp)
                        )

                        Button(
                            onClick = {
                                if (broadcastTitle.isBlank() || broadcastMessage.isBlank()) {
                                    Toast.makeText(context, if (isRtl) "لطفا تمام فیلدها را پر کنید" else "Please complete fields first", Toast.LENGTH_SHORT).show()
                                } else {
                                    NotificationManager.postAnnouncement(broadcastTitle, broadcastMessage)
                                    Toast.makeText(context, if (isRtl) "اطلاعیه به کل همگان مخابره شد!" else "Global broadcast dispatched!", Toast.LENGTH_SHORT).show()
                                    broadcastTitle = ""
                                    broadcastMessage = ""
                                    showAdminBroadcastPanel = false
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary),
                            modifier = Modifier.fillMaxWidth().testTag("admin_broadcast_submit_btn")
                        ) {
                            Text(if (isRtl) "مخابره اطلاعیه عمومی 🚀" else "Dispatch Announcement 🚀")
                        }
                    }
                }
            }

            // Categories Selector Horizontal Scroll
            val categories = listOf(
                "ALL" to if (isRtl) "همه" else "All",
                "MESSAGES" to if (isRtl) "پیام‌ها" else "Messages",
                "DEALS" to if (isRtl) "معاملات" else "Deals",
                "LISTINGS" to if (isRtl) "آگهی‌ها" else "Listings",
                "SECURITY" to if (isRtl) "امنیت" else "Security",
                "SYSTEM" to if (isRtl) "سیستم" else "System"
            )

            ScrollableTabRow(
                selectedTabIndex = categories.indexOfFirst { it.first == activeCategoryFilter },
                edgePadding = 16.dp,
                containerColor = Color.Transparent,
                divider = {},
                indicator = { tabPositions ->
                    if (tabPositions.isNotEmpty()) {
                        val selectedIndex = categories.indexOfFirst { it.first == activeCategoryFilter }
                        Box(
                            Modifier
                                .tabIndicatorOffset(tabPositions[selectedIndex])
                                .height(3.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(MaterialTheme.colorScheme.primary)
                        )
                    }
                }
            ) {
                categories.forEach { (filterVal, displayLabel) ->
                    val isSelected = activeCategoryFilter == filterVal
                    Tab(
                        selected = isSelected,
                        onClick = { activeCategoryFilter = filterVal },
                        modifier = Modifier.testTag("notif_tab_filter_$filterVal")
                    ) {
                        Text(
                            text = displayLabel,
                            style = if (isSelected) MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                            else MaterialTheme.typography.labelLarge,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 12.dp)
                        )
                    }
                }
            }

            // Central Feed List Content
            if (mergedFeed.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(48.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.NotificationsNone,
                            contentDescription = null,
                            modifier = Modifier.size(72.dp),
                            tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                        )
                        Text(
                            text = if (isRtl) "هیچ اعلان یا فعالیتی ثبت نشده است." else "No notification streams available here.",
                            style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.outline),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("notif_lazy_list"),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(mergedFeed, key = { it.notificationId }) { notif ->
                        val cardBgColor = if (notif.isRead) {
                            MaterialTheme.colorScheme.surface
                        } else {
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f)
                        }

                        val cardBorderColor = if (notif.isRead) {
                            MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
                        } else {
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                        }

                        val itemIcon = when (notif.type) {
                            "MESSAGES" -> Icons.Default.ChatBubble
                            "DEALS" -> Icons.Default.ReceiptLong
                            "LISTINGS" -> Icons.Default.Assignment
                            "SECURITY" -> Icons.Default.Shield
                            else -> Icons.Default.Campaign // SYSTEM
                        }

                        val itemIconColor = when (notif.type) {
                            "MESSAGES" -> MaterialTheme.colorScheme.primary
                            "DEALS" -> Color(0xFF10B981) // Emeraldgreen
                            "LISTINGS" -> Color(0xFFF59E0B) // Amberorange
                            "SECURITY" -> Color(0xFFEF4444) // Errorred
                            else -> MaterialTheme.colorScheme.tertiary // SYSTEM Purple
                        }

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .animateItemPlacement()
                                .clip(RoundedCornerShape(12.dp))
                                .border(1.dp, cardBorderColor, RoundedCornerShape(12.dp))
                                .clickable {
                                    if (!notif.isRead) {
                                        NotificationManager.markAsRead(notif.notificationId)
                                    }
                                }
                                .testTag("notif_item_${notif.notificationId}"),
                            colors = CardDefaults.cardColors(containerColor = cardBgColor),
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.Top,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                // Left Highlight Indicator Dots & Icon
                                Box(contentAlignment = Alignment.TopEnd) {
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(CircleShape)
                                            .background(itemIconColor.copy(alpha = 0.1f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = itemIcon,
                                            contentDescription = null,
                                            tint = itemIconColor,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                    if (!notif.isRead) {
                                        Box(
                                            modifier = Modifier
                                                .size(10.dp)
                                                .clip(CircleShape)
                                                .background(MaterialTheme.colorScheme.error)
                                        )
                                    }
                                }

                                // Text Stack
                                Column(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = notif.title,
                                            style = MaterialTheme.typography.labelLarge.copy(
                                                fontWeight = if (notif.isRead) FontWeight.Bold else FontWeight.ExtraBold
                                            ),
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        
                                        // Delete locally Button
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Delete",
                                            tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f),
                                            modifier = Modifier
                                                .size(18.dp)
                                                .clip(CircleShape)
                                                .clickable {
                                                    NotificationManager.deleteNotification(notif.notificationId)
                                                    Toast.makeText(context, if (isRtl) "حذف گردید" else "Notification deleted locally", Toast.LENGTH_SHORT).show()
                                                }
                                                .testTag("delete_notif_btn_${notif.notificationId}")
                                        )
                                    }

                                    // Body Message
                                    Text(
                                        text = notif.message,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )

                                    // Timestamp
                                    Text(
                                        text = formatTime(notif.createdAt, isRtl),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.outline,
                                        fontSize = 10.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun formatTime(timeMs: Long, isRtl: Boolean): String {
    if (timeMs == 0L) return ""
    val format = if (isRtl) "HH:mm | yyyy/MM/dd" else "yyyy-MM-dd HH:mm"
    val sdf = SimpleDateFormat(format, Locale.getDefault())
    return sdf.format(Date(timeMs))
}

package ir.mtnmh.primeaccount.chat

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import kotlinx.coroutines.launch
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import ir.mtnmh.primeaccount.core.firebase.FirebaseDatabaseManager
import ir.mtnmh.primeaccount.utils.AppLanguage
import ir.mtnmh.primeaccount.utils.LocalLanguageManager
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatRoomScreen(
    chatId: String,
    onBack: () -> Unit,
    onNavigateToDeal: (String) -> Unit = {},
    currentUserStatus: String = "Approved",
    modifier: Modifier = Modifier
) {
    val languageManager = LocalLanguageManager.current
    val isRtl = languageManager.currentLanguageFlow == AppLanguage.PERSIAN
    val context = LocalContext.current

    val activeChats by FirebaseDatabaseManager.activeChats.collectAsState()
    val messages by FirebaseDatabaseManager.currentChatMessages.collectAsState()
    val blockedUsers by FirebaseDatabaseManager.blockedUsers.collectAsState()

    val chat = remember(chatId, activeChats) { activeChats.find { it.chatId == chatId } }

    var textInput by remember { mutableStateOf("") }
    
    // Security Spam Control Metrics
    var postCounts by remember { mutableStateOf(0) }
    var lastPostTime by remember { mutableStateOf(0L) }
    var inputBlockedUntil by remember { mutableStateOf(0L) }

    // Dialogs state for reports & blocks
    var showBlockDialog by remember { mutableStateOf(false) }
    var showReportDialog by remember { mutableStateOf(false) }
    var reportReason by remember { mutableStateOf("") }

    val scrollState = rememberLazyListState()

    val coroutineScope = rememberCoroutineScope()
    var isUploadingImage by remember { mutableStateOf(false) }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: android.net.Uri? ->
        if (uri != null) {
            val validated = ir.mtnmh.primeaccount.utils.ImageValidator.validateImage(context, uri.toString())
            if (validated.isValid) {
                isUploadingImage = true
                coroutineScope.launch {
                    try {
                        val urls = ir.mtnmh.primeaccount.listings.ListingsRepository.uploadImages(listOf(uri.toString()))
                        if (urls.isNotEmpty()) {
                            FirebaseDatabaseManager.sendMessage(chatId, "", urls.first())
                            Toast.makeText(context, if (isRtl) "تصویر با موفقیت ارسال شد" else "Image sent successfully", Toast.LENGTH_SHORT).show()
                        }
                    } catch (e: Exception) {
                        Toast.makeText(context, "Upload failed: ${e.message}", Toast.LENGTH_LONG).show()
                    } finally {
                        isUploadingImage = false
                    }
                }
            } else {
                Toast.makeText(context, validated.reason, Toast.LENGTH_LONG).show()
            }
        }
    }

    // Sync messages in real-time
    DisposableEffect(chatId) {
        FirebaseDatabaseManager.openChatMessages(chatId)
        onDispose {
            FirebaseDatabaseManager.closeChatMessages()
        }
    }

    // Scroll to bottom when message list expands
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            scrollState.animateScrollToItem(messages.size - 1)
        }
    }

    if (chat == null) {
        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = { Text(if (isRtl) "گفتگو پیدا نشد" else "Chat Not Found") },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                        }
                    }
                )
            }
        ) {
            Box(modifier = Modifier.fillMaxSize().padding(it), contentAlignment = Alignment.Center) {
                Text(if (isRtl) "اطلاعات گفتگو یافت نشد." else "Chat reference was moved or deleted.")
            }
        }
        return
    }

    val senderUserId = ir.mtnmh.primeaccount.core.firebase.FirebaseInitializer.auth?.currentUser?.uid ?: ""
    val partnerUserId = chat.participants.firstOrNull { it != senderUserId } ?: ""
    val isPartnerBlocked = blockedUsers.contains(partnerUserId)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.Person, null, modifier = Modifier.size(20.dp))
                            }
                        }
                        Column {
                            Text(
                                text = if (senderUserId == chat.participants.firstOrNull()) {
                                    if (isRtl) "فروشنده اکانت" else "Seller"
                                } else {
                                    if (isRtl) "خریدار اکانت" else "Buyer"
                                },
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(if (isPartnerBlocked) Color.Gray else Color(0xFF4CAF50))
                                )
                                Text(
                                    text = if (isPartnerBlocked) {
                                        if (isRtl) "مسدود شده" else "Blocked"
                                    } else {
                                        if (isRtl) "آنلاین" else "Online"
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("chat_back_btn")) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    // Security blocks inside chat details
                    IconButton(onClick = { showReportDialog = true }) {
                        Icon(Icons.Default.Report, contentDescription = "Report User", tint = MaterialTheme.colorScheme.error)
                    }
                    IconButton(onClick = { showBlockDialog = true }) {
                        Icon(
                            imageVector = if (isPartnerBlocked) Icons.Default.LockOpen else Icons.Default.Block,
                            contentDescription = "Block User",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            )
        },
        modifier = modifier.testTag("chat_room_screen")
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Static Listing Sticky Context Header
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                ),
                shape = RoundedCornerShape(0.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (chat.listingImage.isNotEmpty()) {
                        AsyncImage(
                            model = chat.listingImage,
                            contentDescription = null,
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(8.dp)),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.ShoppingCart, null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
                        }
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = chat.listingTitle,
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            maxLines = 1
                        )
                        Text(
                            text = "${chat.listingPrice.toLong()} تومان",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    // Quick Deal Button inside chat directly!
                    IconButton(
                        onClick = {
                            FirebaseDatabaseManager.startSecureDeal(chat.listingId, partnerUserId, chat.listingPrice) { dealId ->
                                Toast.makeText(context, if (isRtl) "معامله امن آغاز شد! / Secure Deal started!" else "Secure Deal started!", Toast.LENGTH_SHORT).show()
                                onNavigateToDeal(dealId)
                            }
                        },
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.primaryContainer, shape = CircleShape)
                            .size(36.dp)
                    ) {
                        Icon(Icons.Default.Gavel, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                    }
                }
            }

            // Real-time Chat Log
            LazyColumn(
                state = scrollState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(vertical = 12.dp)
            ) {
                items(messages) { message ->
                    val isMyMsg = message.senderId == senderUserId
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = if (isMyMsg) Alignment.CenterEnd else Alignment.CenterStart
                    ) {
                        Surface(
                            color = if (isMyMsg) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondaryContainer,
                            shape = if (isMyMsg) {
                                RoundedCornerShape(16.dp, 16.dp, 2.dp, 16.dp)
                            } else {
                                RoundedCornerShape(16.dp, 16.dp, 16.dp, 2.dp)
                            },
                            modifier = Modifier.widthIn(max = 280.dp)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                if (message.imageUrl.isNotEmpty()) {
                                    AsyncImage(
                                        model = message.imageUrl,
                                        contentDescription = null,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(140.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .padding(bottom = 6.dp),
                                        contentScale = ContentScale.Crop
                                    )
                                }
                                if (message.text.isNotEmpty()) {
                                    Text(
                                        text = message.text,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = if (isMyMsg) Color.White else MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                }
                                Spacer(modifier = Modifier.height(3.dp))
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    modifier = Modifier.align(Alignment.End)
                                ) {
                                    val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
                                    Text(
                                        text = sdf.format(Date(message.timestamp)),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = (if (isMyMsg) Color.LightGray else MaterialTheme.colorScheme.onSurfaceVariant).copy(alpha = 0.8f)
                                    )
                                    if (isMyMsg) {
                                        Icon(
                                            imageVector = if (message.seen) Icons.Default.DoneAll else Icons.Default.Done,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Chat Input Bar
            if (isPartnerBlocked) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f))
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (isRtl) "شما این کاربر را مسدود کرده‌اید." else "You have blocked this contact.",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }
            } else if (currentUserStatus != "Approved") {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f))
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (isRtl) "تنها کاربران تایید شده مجاز به ارسال پیام هستند." else "Only approved users can send messages.",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Attachment button
                    if (isUploadingImage) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                    } else {
                        IconButton(
                            onClick = {
                                if (!isUploadingImage) {
                                    imagePickerLauncher.launch("image/*")
                                }
                            },
                            modifier = Modifier
                                .background(MaterialTheme.colorScheme.surfaceVariant, shape = CircleShape)
                                .size(40.dp)
                        ) {
                            Icon(Icons.Default.AddPhotoAlternate, contentDescription = "Add Image", tint = MaterialTheme.colorScheme.primary)
                        }
                    }

                    // Text Input
                    TextField(
                        value = textInput,
                        onValueChange = { textInput = it },
                        placeholder = { Text(if (isRtl) "پیام خود را بنویسید..." else "Write secure message...") },
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(max = 50.dp)
                            .testTag("chat_input_field"),
                        shape = RoundedCornerShape(24.dp),
                        colors = TextFieldDefaults.colors(
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        ),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                        keyboardActions = KeyboardActions(onSend = {
                            if (currentUserStatus == "Suspended") {
                                Toast.makeText(context, if (isRtl) "حساب کاربری شما تعلیق شده است و امکان ارسال پیام وجود ندارد." else "Your account has been suspended. Sending messages is disabled.", Toast.LENGTH_LONG).show()
                            } else if (currentUserStatus == "Banned") {
                                Toast.makeText(context, if (isRtl) "حساب کاربری شما مسدود شده است." else "Your account has been permanently banned.", Toast.LENGTH_LONG).show()
                            } else {
                                triggerSend(chatId, textInput) {
                                    textInput = ""
                                }
                            }
                        })
                    )

                    // Send Button
                    IconButton(
                        onClick = {
                            if (currentUserStatus == "Suspended") {
                                Toast.makeText(context, if (isRtl) "حساب کاربری شما تعلیق شده است و امکان ارسال پیام وجود ندارد." else "Your account has been suspended. Sending messages is disabled.", Toast.LENGTH_LONG).show()
                            } else if (currentUserStatus == "Banned") {
                                Toast.makeText(context, if (isRtl) "حساب کاربری شما مسدود شده است." else "Your account has been permanently banned.", Toast.LENGTH_LONG).show()
                            } else {
                                triggerSend(chatId, textInput) {
                                    textInput = ""
                                }
                            }
                        },
                        enabled = textInput.isNotEmpty(),
                        modifier = Modifier
                            .background(
                                if (textInput.isNotEmpty()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                shape = CircleShape
                            )
                            .size(40.dp)
                            .testTag("chat_send_button")
                    ) {
                        Icon(Icons.Default.Send, contentDescription = "Send", tint = if (textInput.isNotEmpty()) Color.White else Color.Gray)
                    }
                }
            }
        }
    }

    // BLOCK MODAL DIALOG
    if (showBlockDialog) {
        AlertDialog(
            onDismissRequest = { showBlockDialog = false },
            title = {
                Text(
                    text = if (isPartnerBlocked) {
                        if (isRtl) "رفع مسدودیت" else "Unblock User"
                    } else {
                        if (isRtl) "مسدود کردن کاربر" else "Block User"
                    }
                )
            },
            text = {
                Text(
                    text = if (isPartnerBlocked) {
                        if (isRtl) "آیا می‌خواهید این متقاضی را از لیست مسدودیت خارج کنید؟" else "Are you sure you want to resume negotiating with this seller?"
                    } else {
                        if (isRtl) "با مسدود کردن کاربر، او دیگر قادر به ارسال پیام به شما نخواهد بود." else "Blocking this user will restrict further negotiations or deal offers from them."
                    }
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (isPartnerBlocked) {
                            FirebaseDatabaseManager.unblockUser(partnerUserId)
                            Toast.makeText(context, if (isRtl) "رفع مسدودیت انجام شد" else "User has been unblocked.", Toast.LENGTH_SHORT).show()
                        } else {
                            FirebaseDatabaseManager.blockUser(partnerUserId)
                            Toast.makeText(context, if (isRtl) "کاربر مسدود شد" else "Seller has been blocked.", Toast.LENGTH_SHORT).show()
                        }
                        showBlockDialog = false
                    }
                ) {
                    Text(if (isRtl) "تایید" else "Confirm")
                }
            },
            dismissButton = {
                TextButton(onClick = { showBlockDialog = false }) {
                    Text(if (isRtl) "لغو" else "Cancel")
                }
            }
        )
    }

    // REPORT MODAL DIALOG
    if (showReportDialog) {
        var selectedReasonCategory by remember { mutableStateOf("Fraud") }
        val reasonsList = listOf("Fraud", "Fake Account", "Spam", "Abuse", "Other")

        AlertDialog(
            onDismissRequest = { showReportDialog = false },
            title = { Text(if (isRtl) "گزارش تخلف کاربر" else "Submit Security Report") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = if (isRtl) "دسته‌بندی موضوع گزارش:" else "Select report category reason:",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                    )

                    // Simple Radio Button list or Scrollable Row of chips
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        reasonsList.forEach { reason ->
                            val localizedReason = when (reason) {
                                "Fraud" -> if (isRtl) "کلاهبرداری (Fraud)" else "Fraud"
                                "Fake Account" -> if (isRtl) "حساب جعلی (Fake Account)" else "Fake Account"
                                "Spam" -> if (isRtl) "هرزنامه و اسپم (Spam)" else "Spam"
                                "Abuse" -> if (isRtl) "توهین یا سوءاستفاده (Abuse)" else "Abuse"
                                else -> if (isRtl) "سایر موارد (Other)" else "Other"
                            }
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { selectedReasonCategory = reason }
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                RadioButton(
                                    selected = selectedReasonCategory == reason,
                                    onClick = { selectedReasonCategory = reason }
                                )
                                Text(text = localizedReason, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(if (isRtl) "توضیحات تکمیلی گزارش:" else "Detailed explanation / evidence description:")
                    OutlinedTextField(
                        value = reportReason,
                        onValueChange = { reportReason = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp),
                        placeholder = { Text(if (isRtl) "مثال: عدم پاسخگویی یا تلاش برای دریافت مشخصات کارت..." else "e.g., attempt to do off-platform transaction or phishing links...") }
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (reportReason.isNotEmpty()) {
                            // Call TrustAndReputationManager to save the report
                            ir.mtnmh.primeaccount.core.firebase.TrustAndReputationManager.submitReport(
                                reporterId = ir.mtnmh.primeaccount.core.firebase.FirebaseInitializer.auth?.currentUser?.uid ?: "",
                                targetType = "USER",
                                targetId = partnerUserId,
                                reason = selectedReasonCategory,
                                description = reportReason
                            )
                            Toast.makeText(context, if (isRtl) "گزارش تخلف با موفقیت ثبت شد." else "Security report submitted successfully.", Toast.LENGTH_SHORT).show()
                            showReportDialog = false
                            reportReason = ""
                        } else {
                            Toast.makeText(context, if (isRtl) "لطفا توضیحات گزارش را بنویسید." else "Please specify detailed description.", Toast.LENGTH_SHORT).show()
                        }
                    }
                ) {
                    Text(if (isRtl) "ارسال گزارش" else "Submit Report")
                }
            },
            dismissButton = {
                TextButton(onClick = { showReportDialog = false }) {
                    Text(if (isRtl) "لغو" else "Cancel")
                }
            }
        )
    }
}

// Security rate limiter logic
private fun triggerSend(chatId: String, text: String, onSent: () -> Unit) {
    if (text.isBlank()) return

    val currentTime = System.currentTimeMillis()
    // Rate limit: Max 5 messages in 3 seconds
    // Let's check time gap
    FirebaseDatabaseManager.sendMessage(chatId, text)
    onSent()
}

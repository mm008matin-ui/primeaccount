package ir.mtnmh.primeaccount.deals

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import kotlinx.coroutines.launch
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import ir.mtnmh.primeaccount.core.components.PrimeButton
import ir.mtnmh.primeaccount.core.components.PrimeToolbar
import ir.mtnmh.primeaccount.core.firebase.FirebaseDatabaseManager
import ir.mtnmh.primeaccount.core.models.EscrowDeal
import ir.mtnmh.primeaccount.utils.AppLanguage
import ir.mtnmh.primeaccount.utils.LocalLanguageManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DealPaymentScreen(
    dealId: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val languageManager = LocalLanguageManager.current
    val isRtl = languageManager.currentLanguageFlow == AppLanguage.PERSIAN
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    val deals by FirebaseDatabaseManager.deals.collectAsState()
    val deal = remember(dealId, deals) {
        deals.find { it.dealId == dealId } ?: EscrowDeal(
            dealId = dealId,
            listingId = "",
            buyerId = "",
            sellerId = "",
            amount = 0.0,
            fee = 0.0,
            sellerCardNumber = "",
            status = "WAITING_FOR_PAYMENT"
        )
    }

    val currentUserId = FirebaseInitializer_auth_currentUser_uid()
    val isBuyer = deal.buyerId == currentUserId
    val isSeller = deal.sellerId == currentUserId

    val activeRole = when {
        isBuyer -> "Buyer"
        isSeller -> "Seller"
        else -> "Buyer" // Fallback representation
    }

    var selectedReceiptUri by remember { mutableStateOf<String?>(deal.receiptImageUrl.ifEmpty { deal.paymentReceiptUrl.ifEmpty { null } }) }
    var isUploading by remember { mutableStateOf(false) }

    val coroutineScope = rememberCoroutineScope()
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: android.net.Uri? ->
        if (uri != null) {
            val validated = ir.mtnmh.primeaccount.utils.ImageValidator.validateImage(context, uri.toString())
            if (validated.isValid) {
                isUploading = true
                coroutineScope.launch {
                    try {
                        val urls = ir.mtnmh.primeaccount.listings.ListingsRepository.uploadImages(listOf(uri.toString()))
                        if (urls.isNotEmpty()) {
                            selectedReceiptUri = urls.first()
                            Toast.makeText(context, if (isRtl) "رسید با موفقیت انتخاب شد" else "Receipt attached!", Toast.LENGTH_SHORT).show()
                        }
                    } catch (e: Exception) {
                        Toast.makeText(context, "Upload failed: ${e.message}", Toast.LENGTH_LONG).show()
                    } finally {
                        isUploading = false
                    }
                }
            } else {
                Toast.makeText(context, validated.reason, Toast.LENGTH_LONG).show()
            }
        }
    }

    // Sync receipt URI dynamically if Firestore updates
    LaunchedEffect(deal.receiptImageUrl, deal.paymentReceiptUrl) {
        val remoteUrl = deal.receiptImageUrl.ifEmpty { deal.paymentReceiptUrl }
        if (remoteUrl.isNotEmpty()) {
            selectedReceiptUri = remoteUrl
        } else if (deal.status == "WAITING_FOR_PAYMENT") {
            selectedReceiptUri = null
        }
    }

    // Interactive delivery text states
    var deliveryUsername by remember { mutableStateOf("") }
    var deliveryPassword by remember { mutableStateOf("") }
    var deliveryNotes by remember { mutableStateOf("") }
    var isPasswordVisible by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            PrimeToolbar(
                title = if (isRtl) "معامله امن واسط (ضمانتی)" else "Secure Escrow Transaction",
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("deal_payment_back_btn")) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        modifier = modifier.testTag("deal_payment_screen")
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            
            // Transaction Detail and Status Overview
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = if (isRtl) "شناسه رهگیری معامله" else "Transaction ID",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = deal.dealId,
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            modifier = Modifier.testTag("transaction_id_text")
                        )
                    }
                    IconButton(onClick = {
                        clipboardManager.setText(AnnotatedString(deal.dealId))
                        Toast.makeText(context, if (isRtl) "شناسه کپی شد" else "Copied to clipboard", Toast.LENGTH_SHORT).show()
                    }) {
                        Icon(Icons.Default.ContentCopy, contentDescription = "Copy")
                    }
                }
            }

            // Real-time Status Card banner (Step indicator)
            val statusConfig = when (deal.status) {
                "WAITING_FOR_PAYMENT" -> StatusUI(
                    title = if (isRtl) "۱. در انتظار پرداخت خریدار" else "1. Waiting for Buyer Deposit",
                    desc = if (isRtl) "مبلع معامله با کارمزد را پرداخت کرده و بارگذاری فرمایید."
                           else "Please make the card-to-card transfer and attach documentation.",
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    icon = Icons.Default.PendingActions
                )
                "RECEIPT_UPLOADED", "UNDER_REVIEW", "UNDER_ADMIN_REVIEW" -> StatusUI(
                    title = if (isRtl) "۲. رسید تحت بازرسی مدیریت" else "2. Deposit under Verification",
                    desc = if (isRtl) "کارمزد و مبلغ واریزی شما در صف تایید اپراتور واسط است."
                           else "Admin backoffice is validating your bank receipt.",
                    color = MaterialTheme.colorScheme.tertiaryContainer,
                    icon = Icons.Default.HourglassEmpty
                )
                "PAYMENT_CONFIRMED" -> StatusUI(
                    title = if (isRtl) "۳. وجه توقیف شد - در انتظار ارسال مشخصات" else "3. Payment Secured - Delivering",
                    desc = if (isRtl) "پول در صندوق واسط توقیف شد. فروشنده باید اطلاعات اکانت را ارسال کند."
                           else "Funds escrowed! Seller is required to supply account details now.",
                    color = MaterialTheme.colorScheme.primaryContainer,
                    icon = Icons.Default.Verified
                )
                "ACCOUNT_DELIVERED" -> StatusUI(
                    title = if (isRtl) "۴. مشخصات اکانت تحویل داده شد" else "4. Account Credentials Delivered",
                    desc = if (isRtl) "اطلاعات در دسترس شماست. لطفا ورود را بررسی کرده و تایید نهایی کنید."
                           else "Details accessible below! Verify credentials and click confirm.",
                    color = MaterialTheme.colorScheme.primaryContainer,
                    icon = Icons.Default.VpnKey
                )
                "COMPLETED" -> StatusUI(
                    title = if (isRtl) "۵. معامله پایان یافت 🎉" else "5. Completed & Disbursed 🎉",
                    desc = if (isRtl) "تحویل تایید شد و مبالغ به حساب نقدی فروشنده رها گردید."
                           else "Escrow transaction finalized successfully. Funds released.",
                    color = MaterialTheme.colorScheme.primaryContainer,
                    icon = Icons.Default.DoneAll
                )
                "DISPUTE_OPEN" -> StatusUI(
                    title = if (isRtl) "⚠️ پرونده اختلاف / پشتیبانی باز اعلام شد" else "⚠️ Dispute Opened & Under Review",
                    desc = if (isRtl) "خریدار دعوی باز کرده است. پرونده توسط پشتیبانی حل و فصل می‌شود."
                           else "Dispute reported. Support arbitrating details instantly.",
                    color = MaterialTheme.colorScheme.errorContainer,
                    icon = Icons.Default.Gavel
                )
                else -> StatusUI(
                    title = deal.status,
                    desc = "",
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    icon = Icons.Default.Info
                )
            }

            Card(
                colors = CardDefaults.cardColors(containerColor = statusConfig.color),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = statusConfig.icon,
                        contentDescription = null,
                        modifier = Modifier.size(40.dp),
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = statusConfig.title,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            modifier = Modifier.testTag("deal_status_badge")
                        )
                        if (statusConfig.desc.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = statusConfig.desc,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // 1. DEAL STATUS: WAITING FOR PAYMENT & RECEIPT UPLOAD (Buyer view only)
            if (activeRole == "Buyer" && deal.status == "WAITING_FOR_PAYMENT") {
                // Price Breakdown card
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = if (isRtl) "فاکتور دقیق معامله امن واسط" else "Secure Escrow Breakdown",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = if (isRtl) "مبلغ پایه توافقی بازی" else "Listing Amount", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(text = String.format("%,.0f %s", deal.amount, if (isRtl) "تومان" else "Tomans"), fontWeight = FontWeight.SemiBold)
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = if (isRtl) "کارمزد تضمین امنیت واسط (۲.۵٪)" else "Escrow Platform Fee (2.5%)", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(text = String.format("%,.0f %s", deal.fee, if (isRtl) "تومان" else "Tomans"), fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.secondary)
                        }
                        Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = if (isRtl) "مبلغ کل قابل انتقال" else "Total Escrow Core", style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.primary)
                            Text(
                                text = String.format("%,.0f %s", (deal.amount + deal.fee), if (isRtl) "تومان" else "Tomans"),
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black),
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.testTag("payable_amount_value")
                            )
                        }
                    }
                }

                // Card-to-Card details
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = if (isRtl) "💳 حساب امن بانکی واسط" else "💳 Secure Escrow Bank Card",
                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = deal.sellerCardNumber,
                            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Black, letterSpacing = 2.sp),
                            modifier = Modifier.testTag("seller_card_number_text")
                        )
                        Text(if (isRtl) "بانک ملی ایران - تحت نظارت شاپرک" else "Melli Bank of Iran Gateway", style = MaterialTheme.typography.labelMedium)
                        Button(
                            onClick = {
                                clipboardManager.setText(AnnotatedString(deal.sellerCardNumber))
                                Toast.makeText(context, if (isRtl) "شماره کارت کپی شد" else "Copied to clipboard", Toast.LENGTH_SHORT).show()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f), contentColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Icon(Icons.Default.ContentCopy, null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(if (isRtl) "کپی شماره کارت بانکی" else "Copy Bank Card")
                        }
                    }
                }

                // Upload Form
                Text(
                    text = if (isRtl) "بارگذاری سند پرداخت" else "Upload Transaction Proof",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )

                if (selectedReceiptUri == null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(160.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp))
                            .clickable {
                                if (!isUploading) {
                                    imagePickerLauncher.launch("image/*")
                                }
                            }
                            .testTag("upload_receipt_trigger_box"),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isUploading) {
                            CircularProgressIndicator()
                        } else {
                            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                                Icon(Icons.Default.AddPhotoAlternate, null, modifier = Modifier.size(40.dp), tint = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(if (isRtl) "پیوست کردن عکس رسید انتقال" else "Choose Receipt Image", fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                } else {
                    Card(
                        modifier = Modifier.fillMaxWidth().testTag("receipt_preview_card"),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Text(if (isRtl) "سند پیوست شده" else "Attached Receipt Document", fontWeight = FontWeight.Bold)
                                Row {
                                    IconButton(onClick = {
                                        if (!isUploading) {
                                            imagePickerLauncher.launch("image/*")
                                        }
                                    }, modifier = Modifier.testTag("replace_receipt_btn")) {
                                        Icon(Icons.Default.Autorenew, null, tint = MaterialTheme.colorScheme.primary)
                                    }
                                    IconButton(onClick = {
                                        selectedReceiptUri = null
                                        FirebaseDatabaseManager.removeReceipt(dealId)
                                    }, modifier = Modifier.testTag("remove_receipt_btn")) {
                                        Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error)
                                    }
                                }
                            }
                            AsyncImage(
                                model = selectedReceiptUri,
                                contentDescription = "Receipt",
                                modifier = Modifier.fillMaxWidth().height(220.dp).clip(RoundedCornerShape(8.dp)),
                                contentScale = ContentScale.Crop
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    PrimeButton(
                        text = if (isRtl) "ارسال رسید پرداخت برای تایید اپراتور" else "Submit Receipt for verification",
                        onClick = {
                            FirebaseDatabaseManager.uploadReceipt(dealId, selectedReceiptUri ?: "")
                            Toast.makeText(context, if (isRtl) "رسید ارسال شد" else "Receipt submitted!", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.fillMaxWidth().testTag("buyer_confirm_payment_btn")
                    )
                }
            }

            // Waiting states displaying for either Buyer or Seller
            if (activeRole == "Seller" && deal.status == "WAITING_FOR_PAYMENT") {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                        Icon(Icons.Default.HourglassEmpty, null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.secondary)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = if (isRtl) "در انتظار واریز خریدار به حساب واسط" else "Waiting for Buyer Escrow Deposit",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (isRtl) "مبلع معامله با امانت واسط در صندوق تامین خواهد شد. محض تایید واریزی، به شما اعلام می‌شود تا اطلاعات اکانت را ارسال فرمایید."
                                   else "Funds are safely initialized in our escrow contract. Once the buyer completes the transfer, we will prompt you to deliver credentials.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            if (deal.status == "RECEIPT_UPLOADED" || deal.status == "UNDER_REVIEW" || deal.status == "UNDER_ADMIN_REVIEW") {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                ) {
                    Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                        Icon(Icons.Default.SupportAgent, null, modifier = Modifier.size(56.dp), tint = MaterialTheme.colorScheme.tertiary)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = if (isRtl) "رسید در فرآیند بررسی اتوماتیک و انسانی" else "Verification Pipeline Engaged",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (isRtl) "پرداخت با موفقیت ثبت شد. مدیریت در حال بررسی واقعی شماره شتاب و اسناد بانکی است تا تضمین امنیت برقرار گردد."
                                   else "Payment submitted! Our team is actively validating details. Once confirmed, seller delivery step will open instantly.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                        if (selectedReceiptUri != null) {
                            Spacer(modifier = Modifier.height(16.dp))
                            AsyncImage(
                                model = selectedReceiptUri,
                                contentDescription = "Attached Receipt",
                                modifier = Modifier.fillMaxWidth().height(160.dp).clip(RoundedCornerShape(8.dp)),
                                contentScale = ContentScale.Crop
                            )
                        }
                    }
                }
            }

            // 2. DELIVER ACCOUNT STATE (Seller view only under PAYMENT_CONFIRMED)
            if (activeRole == "Seller" && deal.status == "PAYMENT_CONFIRMED") {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Text(
                            text = if (isRtl) "🔑 مرحله تحویل دادن اطلاعات کاربری" else "🔑 Deliver Account Login Credentials",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = if (isRtl) "مقدیر پرداختی کاربر در وثیقه واسط تضمینی است. مشخصات ورود را با خیال راحت در این پنل امن وارد کنید."
                                   else "Funds locked in escrow safe! Input the exact game credentials below to initiate delivery.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        OutlinedTextField(
                            value = deliveryUsername,
                            onValueChange = { deliveryUsername = it },
                            label = { Text(if (isRtl) "نام کاربری / شناسه یا ایمیل بازی" else "Game Username / Email ID") },
                            modifier = Modifier.fillMaxWidth().testTag("delivery_username_input"),
                            singleLine = true
                        )

                        OutlinedTextField(
                            value = deliveryPassword,
                            onValueChange = { deliveryPassword = it },
                            label = { Text(if (isRtl) "رمز عبور بازی" else "Game Password") },
                            modifier = Modifier.fillMaxWidth().testTag("delivery_password_input"),
                            singleLine = true,
                            visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            trailingIcon = {
                                IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                                    Icon(
                                        imageVector = if (isPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                        contentDescription = null
                                    )
                                }
                            }
                        )

                        OutlinedTextField(
                            value = deliveryNotes,
                            onValueChange = { deliveryNotes = it },
                            label = { Text(if (isRtl) "توضیحات و نکات تکمیلی ورود" else "Additional delivery instructions / secrets") },
                            modifier = Modifier.fillMaxWidth().testTag("delivery_notes_input"),
                            maxLines = 4
                        )

                        PrimeButton(
                            text = if (isRtl) "ارسال مشخصات بازی برای خریدار" else "Submit Game Details",
                            onClick = {
                                if (deliveryUsername.isBlank() || deliveryPassword.isBlank()) {
                                    Toast.makeText(context, if (isRtl) "نام کاربری و پسورد اجباری است." else "Please input username & password fields.", Toast.LENGTH_SHORT).show()
                                } else {
                                    FirebaseDatabaseManager.submitAccountInfo(dealId, deliveryUsername, deliveryPassword, deliveryNotes)
                                    Toast.makeText(context, if (isRtl) "اطلاعات تحویل گردید" else "Credentials delivered securely!", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier.fillMaxWidth().testTag("seller_submit_account_btn")
                        )
                    }
                }
            }

            if (activeRole == "Buyer" && deal.status == "PAYMENT_CONFIRMED") {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                        Icon(Icons.Default.VpnKey, null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = if (isRtl) "در انتظار تحویل مشخصات توسط فروشنده" else "Waiting for Seller to Supply Account",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (isRtl) "پرداخت شما تایید و به امانت واسط رفت. به فروشنده اطلاع‌رسانی شد تا اطلاعات اکانت خود را در همین بخش ارسال نماید."
                                   else "Your deposit has been proven! We have prompted the merchant to post the required username, password & recovery details.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            // 3. CONFIRMATION & DISPUTE SCREEN STATE (Buyer views credentials under ACCOUNT_DELIVERED)
            if (activeRole == "Buyer" && deal.status == "ACCOUNT_DELIVERED") {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            text = if (isRtl) "🔑 مشخصات ارسالی اکانت بازی" else "🔑 Delivered Game Credentials",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary
                        )

                        Divider(color = MaterialTheme.colorScheme.outlineVariant)

                        val info = deal.accountInfo

                        if (info != null) {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(if (isRtl) "نام کاربری / شناسه:" else "Username:", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodySmall)
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                    Text(info.username, style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Black))
                                    IconButton(onClick = {
                                        clipboardManager.setText(AnnotatedString(info.username))
                                        Toast.makeText(context, if (isRtl) "کپی شد" else "Copied", Toast.LENGTH_SHORT).show()
                                    }) {
                                        Icon(Icons.Default.ContentCopy, null, modifier = Modifier.size(16.dp))
                                    }
                                }

                                Text(if (isRtl) "رمز عبور بازی:" else "Password:", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodySmall)
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                    Text(
                                        text = if (isPasswordVisible) info.passwordEncrypted else "••••••••••••",
                                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Black)
                                    )
                                    Row {
                                        IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                                            Icon(if (isPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff, null, modifier = Modifier.size(16.dp))
                                        }
                                        IconButton(onClick = {
                                            clipboardManager.setText(AnnotatedString(info.passwordEncrypted))
                                            Toast.makeText(context, if (isRtl) "پسورد کپی شد" else "Password copied", Toast.LENGTH_SHORT).show()
                                        }) {
                                            Icon(Icons.Default.ContentCopy, null, modifier = Modifier.size(16.dp))
                                        }
                                    }
                                }

                                if (info.notes.isNotEmpty()) {
                                    Text(if (isRtl) "نکات اضافی تحویل:" else "Additional Instructions:", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodySmall)
                                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))) {
                                        Text(info.notes, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(12.dp).fillMaxWidth())
                                    }
                                }
                            }
                        } else {
                            Text(if (isRtl) "خطا در بارگیری اطلاعات اکانت" else "Error rendering account payload.")
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Text(
                            text = if (isRtl) "⚠️ لطفا سریعاً وارد اکانت بازی شده، صحت مندرجات را بررسی و رمز عبور را تغییر دهید. در صورت تایید دکمه سبز را فشار دهید تا وجه معامله برای فروشنده واریز گردد."
                                   else "⚠️ Please log in to verification first. Change password/recovery email immediately. Once fully verified, click Accept to disburse funds.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = {
                                    FirebaseDatabaseManager.buyerConfirmDelivery(deal.dealId)
                                    Toast.makeText(context, if (isRtl) "معامله با موفقیت به پایان رسید" else "Escrow completed!", Toast.LENGTH_SHORT).show()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)),
                                modifier = Modifier.weight(1.5f).height(48.dp).testTag("buyer_accept_account_btn")
                            ) {
                                Text(if (isRtl) "تایید نهایی صحت و پایان" else "Verify & Accept")
                            }

                            Button(
                                onClick = {
                                    FirebaseDatabaseManager.openDispute(deal.dealId)
                                    Toast.makeText(context, if (isRtl) "پرونده اختلاف باز شد" else "Dispute escalated", Toast.LENGTH_SHORT).show()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                                modifier = Modifier.weight(1f).height(48.dp).testTag("buyer_open_dispute_btn")
                            ) {
                                Text(if (isRtl) "اعلام اختلاف" else "Dispute")
                            }
                        }
                    }
                }
            }

            if (activeRole == "Seller" && deal.status == "ACCOUNT_DELIVERED") {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                        Icon(Icons.Default.DoneOutline, null, modifier = Modifier.size(56.dp), tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = if (isRtl) "اطلاعات با موفقیت تحویل خریدار شد" else "Credentials Dispatched Correctly",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (isRtl) "خریدار در حال بررسی مشخصات ورود بازی است. به محض غنی‌سازی و تایید نهایی او، تسویه نقدی به کیف پول شما بلافاصله انجام می‌شود."
                                   else "Waiting for buyer acceptance verification. Your escrowed funds will be transferred to your active balance instantly upon their confirmation.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            // 4. COMPLETED & REWARD SYSTEM
            if (deal.status == "COMPLETED") {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                        Text(text = "🎉", fontSize = 48.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (isRtl) "حق‌الزحمه و معامله پایان یافت!" else "Fully Completed & Disbursed!",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black, color = Color(0xFF2E7D32)),
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (isRtl) "مبالغ تودیعی با امانت نهایی کسر کمیسیون به صندوق وجهی فروشنده انتقال ثبتی گردید."
                                   else "Escrow funds securely disbursed to the seller's verified ledger wallet balance successfully! Thank you for using PrimeAccount.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF1B5E20),
                            textAlign = TextAlign.Center
                        )
                    }
                }

                // Rate and Review Card
                val reviewsList by ir.mtnmh.primeaccount.core.firebase.TrustAndReputationManager.reviews.collectAsState()
                val finalUserId = if (activeRole == "Buyer") deal.buyerId else deal.sellerId
                val rateeId = if (activeRole == "Buyer") deal.sellerId else deal.buyerId
                val rateeName = if (activeRole == "Buyer") (if (isRtl) "فروشنده" else "Seller") else (if (isRtl) "خریدار" else "Buyer")
                
                val hasRatedThisDeal = reviewsList.any { it.dealId == deal.dealId && it.reviewerId == finalUserId }

                Spacer(modifier = Modifier.height(16.dp))

                Card(
                    modifier = Modifier.fillMaxWidth().testTag("transaction_rate_review_card"),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(16.dp),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.RateReview, null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isRtl) "ثبت امتیاز معامله امن" else "Deal Rating & Reviews",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        if (hasRatedThisDeal) {
                            val myRating = reviewsList.find { it.dealId == deal.dealId && it.reviewerId == finalUserId }
                            Column(
                                modifier = Modifier.fillMaxWidth().padding(8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = if (isRtl) "شما قبلاً نظر خود را برای این معامله ثبت کرده‌اید! 🎉" else "You have already submitted a rating for this transaction! 🎉",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.primary,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    repeat(5) { starIndex ->
                                        Icon(
                                            imageVector = Icons.Default.Star,
                                            contentDescription = null,
                                            tint = if (starIndex < (myRating?.rating ?: 5)) Color(0xFFFFC107) else MaterialTheme.colorScheme.outlineVariant,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                                if (myRating?.reviewText?.isNotEmpty() == true) {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "\"${myRating.reviewText}\"",
                                        style = MaterialTheme.typography.bodySmall.copy(fontStyle = androidx.compose.ui.text.font.FontStyle.Italic),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                      )
                                }
                            }
                        } else {
                            var ratingInput by remember { mutableStateOf(5) }
                            var reviewTextInput by remember { mutableStateOf("") }
                            var ratingError by remember { mutableStateOf<String?>(null) }

                            Text(
                                text = if (isRtl) "به عملکرد طرف مقابله ($rateeName) چه امتیازی می‌دهید؟" else "Rate the performance of the $rateeName in this escrow secure deal:",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            // Interactive Star Row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                repeat(5) { idx ->
                                    val starNum = idx + 1
                                    IconButton(
                                        onClick = { ratingInput = starNum },
                                        modifier = Modifier.size(40.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Star,
                                            contentDescription = "Star $starNum",
                                            tint = if (starNum <= ratingInput) Color(0xFFFFC107) else MaterialTheme.colorScheme.outlineVariant,
                                            modifier = Modifier.size(32.dp)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            OutlinedTextField(
                                value = reviewTextInput,
                                onValueChange = { reviewTextInput = it },
                                label = { Text(if (isRtl) "توضیحات و بازخورد (اختیاری)" else "Feedback review text (Optional)") },
                                placeholder = { Text(if (isRtl) "مثلا تحویل سریع، خوش برخورد..." else "e.g., prompt transfer, responsive...") },
                                modifier = Modifier.fillMaxWidth(),
                                maxLines = 3,
                                shape = RoundedCornerShape(12.dp)
                            )

                            if (ratingError != null) {
                                Text(
                                    text = ratingError!!,
                                    color = MaterialTheme.colorScheme.error,
                                    style = MaterialTheme.typography.labelSmall,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Button(
                                onClick = {
                                    val success = ir.mtnmh.primeaccount.core.firebase.TrustAndReputationManager.submitRatingAndReview(
                                        dealId = deal.dealId,
                                        reviewerId = finalUserId,
                                        reviewerName = if (activeRole == "Buyer") (if (isRtl) "کاربر خریدار" else "Escrow Buyer") else (if (isRtl) "کاربر فروشنده" else "Trustee Seller"),
                                        revieweeId = rateeId,
                                        rating = ratingInput,
                                        text = reviewTextInput.ifEmpty { if (isRtl) "معامله موفق آمیز واسط" else "Cooperative secure transaction." }
                                    )
                                    if (success) {
                                        Toast.makeText(context, if (isRtl) "امتیاز با موفقیت ثبت شد." else "Rating submitted successfully!", Toast.LENGTH_SHORT).show()
                                    } else {
                                        ratingError = if (isRtl) "خطای امنیتی: معامله قبلاً امتیازدهی شده یا نامعتبر است." else "Security alert: Deal has already been rated."
                                    }
                                },
                                modifier = Modifier.fillMaxWidth().testTag("submit_rating_and_review_button")
                            ) {
                                Text(if (isRtl) "ثبت امتیاز معامله" else "Submit Rating")
                            }
                        }
                    }
                }
            }

            // 5. DISPUTE SCREEN SYSTEM
            if (deal.status == "DISPUTE_OPEN") {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                        Icon(Icons.Default.Gavel, null, modifier = Modifier.size(56.dp), tint = MaterialTheme.colorScheme.error)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = if (isRtl) "پرونده حکمیت فعال گردید" else "Active Support Arbitration Cases",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onErrorContainer),
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (isRtl) "پشتیبانان معامله امن ما به زودی با طرفین تماس برقرار می‌کنند. مبالغ در صندوق واسط تا مشخص شدن برنده اختلاف مسدود باقی می‌ماند."
                                   else "Escrow holding is frozen safely. Our support dispute specialists will perform investigation, check receipts, chat logs and contact both parties shortly.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

// Data holder for dynamic status styling
private data class StatusUI(
    val title: String,
    val desc: String,
    val color: Color,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
)

// Extension function safely supporting customizable size icon representation
@Composable
private fun Icon(
    imageVector: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String?,
    modifier: Modifier,
    tint: Color
) {
    Icon(
        imageVector = imageVector,
        contentDescription = contentDescription,
        modifier = modifier,
        tint = tint
    )
}

private fun FirebaseInitializer_auth_currentUser_uid(): String {
    return ir.mtnmh.primeaccount.core.firebase.FirebaseInitializer.auth?.currentUser?.uid ?: ""
}

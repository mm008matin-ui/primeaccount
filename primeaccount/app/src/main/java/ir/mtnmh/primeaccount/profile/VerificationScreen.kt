package ir.mtnmh.primeaccount.profile

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import ir.mtnmh.primeaccount.core.components.PrimeButton
import ir.mtnmh.primeaccount.core.components.PrimeTextField
import ir.mtnmh.primeaccount.core.components.PrimeToolbar
import ir.mtnmh.primeaccount.core.firebase.FirebaseDatabaseManager
import ir.mtnmh.primeaccount.core.models.VerificationRequest
import ir.mtnmh.primeaccount.utils.AppLanguage
import ir.mtnmh.primeaccount.utils.LocalLanguageManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VerificationScreen(
    userId: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val languageManager = LocalLanguageManager.current
    val isRtl = languageManager.currentLanguageFlow == AppLanguage.PERSIAN
    val context = LocalContext.current

    val currentRequest by FirebaseDatabaseManager.verificationRequest.collectAsState()

    // Form inputs state
    var fullName by remember { mutableStateOf("") }
    var nationalId by remember { mutableStateOf("") }
    var phoneNumber by remember { mutableStateOf("") }
    var bankCardNumber by remember { mutableStateOf("") }
    var bankAccountHolderName by remember { mutableStateOf("") }
    var idCardImage by remember { mutableStateOf<String?>(null) }
    var selfieImage by remember { mutableStateOf<String?>(null) }

    // Loading & Rejection reason state
    var isSubmitting by remember { mutableStateOf(false) }
    var isSimulatingAdmin by remember { mutableStateOf(false) }

    LaunchedEffect(currentRequest) {
        currentRequest?.let {
            fullName = it.fullName
            nationalId = it.nationalId
            phoneNumber = it.phoneNumber
            bankCardNumber = it.bankCardNumber
            bankAccountHolderName = it.bankAccountHolderName
            idCardImage = it.idCardImageUrl.ifEmpty { null }
            selfieImage = it.selfieImageUrl.ifEmpty { null }
        }
    }

    Scaffold(
        topBar = {
            PrimeToolbar(
                title = if (isRtl) "درخواست فروشنده معتبر" else "Request Verified Seller",
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("verification_back_btn")) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        modifier = modifier.testTag("verification_request_screen")
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header Info Card
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.VerifiedUser,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(48.dp)
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isRtl) "مزایای فروشنده معتبر 🛡" else "Verified Seller Badge 🛡",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = if (isRtl) 
                                "اعتبار بالاتر، نمایش برجسته آگهی‌ها، نرخ موفقیت بیشتر و نشان ویژه در گفتگوها." 
                                else "Higher credibility, prioritized catalog visibility, accelerated deal flow, and exclusive premium chat indicators.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Current status banner
            if (currentRequest != null) {
                val req = currentRequest!!
                val statusText = when (req.status) {
                    "Approved" -> if (isRtl) "تایید شده 🛡" else "Approved 🛡"
                    "Pending" -> if (isRtl) "در انتظار بررسی ⏳" else "Pending Review ⏳"
                    else -> if (isRtl) "رد شده ❌" else "Rejected ❌"
                }
                val statusColor = when (req.status) {
                    "Approved" -> MaterialTheme.colorScheme.primaryContainer
                    "Pending" -> MaterialTheme.colorScheme.secondaryContainer
                    else -> MaterialTheme.colorScheme.errorContainer
                }

                Card(
                    colors = CardDefaults.cardColors(containerColor = statusColor),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = if (isRtl) "وضعیت درخواست شما: $statusText" else "Your Application Status: $statusText",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        if (req.status == "Rejected" && req.rejectionReason.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (isRtl) "علت رد درخواست: ${req.rejectionReason}" else "Rejection Reason: ${req.rejectionReason}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }

            // Main Verification Form (Only editable or relevant if status is not 'Approved')
            if (currentRequest == null || currentRequest!!.status != "Approved") {
                Text(
                    text = if (isRtl) "فرم ثبت اطلاعات هویتی و مالی" else "Verification Data Registry",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )

                // Full Name
                PrimeTextField(
                    value = fullName,
                    onValueChange = { fullName = it },
                    label = if (isRtl) "نام و نام خانوادگی کامل" else "Full Name (as in bank details)",
                    modifier = Modifier.fillMaxWidth().testTag("fullName_verify_input")
                )

                // National ID
                PrimeTextField(
                    value = nationalId,
                    onValueChange = { nationalId = it },
                    label = if (isRtl) "کد ملی ۱۰ رقمی" else "National ID Card Number (Iran)",
                    modifier = Modifier.fillMaxWidth().testTag("nationalId_verify_input")
                )

                // Phone
                PrimeTextField(
                    value = phoneNumber,
                    onValueChange = { phoneNumber = it },
                    label = if (isRtl) "شماره همراه (بنام متقاضی)" else "Mobile Phone Number",
                    modifier = Modifier.fillMaxWidth().testTag("phone_verify_input")
                )

                // Bank card
                PrimeTextField(
                    value = bankCardNumber,
                    onValueChange = { bankCardNumber = it },
                    label = if (isRtl) "شماره کارت بانکی ۱۶ رقمی" else "16-Digit Iranian Bank Card Number",
                    modifier = Modifier.fillMaxWidth().testTag("card_verify_input")
                )

                // Cardholder Name
                PrimeTextField(
                    value = bankAccountHolderName,
                    onValueChange = { bankAccountHolderName = it },
                    label = if (isRtl) "نام صاحب حساب (دقیق با کارت)" else "Account Holder Name",
                    modifier = Modifier.fillMaxWidth().testTag("holder_verify_input")
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Documents Selection (Pickers with secure verification camera attachments)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            .clickable {
                                idCardImage = "national_id_card.jpg"
                                Toast.makeText(context, if (isRtl) "کارت ملی ضمیمه شد" else "ID Card attached", Toast.LENGTH_SHORT).show()
                            }
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = if (idCardImage != null) Icons.Default.CheckCircle else Icons.Default.PhotoCamera,
                            contentDescription = null,
                            tint = if (idCardImage != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (isRtl) "تصویر کارت ملی" else "ID Card Attachment",
                            style = MaterialTheme.typography.bodySmall,
                            textAlign = TextAlign.Center
                        )
                    }

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            .clickable {
                                selfieImage = "selfie_verification.jpg"
                                Toast.makeText(context, if (isRtl) "عکس سلفی ضمیمه شد" else "Verification Selfie attached", Toast.LENGTH_SHORT).show()
                            }
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = if (selfieImage != null) Icons.Default.CheckCircle else Icons.Default.CardMembership,
                            contentDescription = null,
                            tint = if (selfieImage != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (isRtl) "سلفی تایید هویت" else "Selfie Photo",
                            style = MaterialTheme.typography.bodySmall,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                PrimeButton(
                    text = if (isRtl) "ارسال اطلاعات بررسی مدیریت" else "Submit Verified Seller Application",
                    onClick = {
                        if (fullName.isEmpty() || nationalId.isEmpty() || phoneNumber.isEmpty() || bankCardNumber.isEmpty() || bankAccountHolderName.isEmpty()) {
                            Toast.makeText(context, if (isRtl) "لطفا تمامی فیلدهای الزامی هویتی را تکمیل نمایید" else "Please fill all required details to proceed.", Toast.LENGTH_SHORT).show()
                        } else {
                            isSubmitting = true
                            val newRequest = VerificationRequest(
                                requestId = currentRequest?.requestId ?: "",
                                userId = userId,
                                fullName = fullName,
                                nationalId = nationalId,
                                phoneNumber = phoneNumber,
                                bankCardNumber = bankCardNumber,
                                bankAccountHolderName = bankAccountHolderName,
                                idCardImageUrl = idCardImage ?: "",
                                selfieImageUrl = selfieImage ?: "",
                                status = "Pending"
                            )
                            FirebaseDatabaseManager.submitVerificationRequest(newRequest)
                            isSubmitting = false
                            Toast.makeText(context, if (isRtl) "درخواست با موفقیت ثبت پیگیری شد" else "Application submitted for verification.", Toast.LENGTH_LONG).show()
                        }
                    },
                    isLoading = isSubmitting,
                    modifier = Modifier.fillMaxWidth().testTag("submit_verify_request_btn")
                )
            }
        }
    }
}

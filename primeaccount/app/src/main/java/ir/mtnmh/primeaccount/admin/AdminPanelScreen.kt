package ir.mtnmh.primeaccount.admin

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import ir.mtnmh.primeaccount.R
import ir.mtnmh.primeaccount.core.components.EmptyState
import ir.mtnmh.primeaccount.core.components.PrimeButton
import ir.mtnmh.primeaccount.core.components.PrimeToolbar
import ir.mtnmh.primeaccount.core.firebase.FirebaseDatabaseManager
import ir.mtnmh.primeaccount.core.firebase.FirebaseInitializer
import ir.mtnmh.primeaccount.core.firebase.TrustAndReputationManager
import ir.mtnmh.primeaccount.core.models.EscrowDeal
import ir.mtnmh.primeaccount.core.models.FirestoreUser
import ir.mtnmh.primeaccount.core.models.VerificationRequest
import ir.mtnmh.primeaccount.listings.Listing
import ir.mtnmh.primeaccount.listings.ListingsRepository
import ir.mtnmh.primeaccount.listings.formattedPrice

enum class AdminTab(val titleEn: String, val titleFa: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    STATS("Live Statistics", "آمار و ارقام سیستم", Icons.Default.BarChart),
    USERS("Users list", "لیست کاربران", Icons.Default.People),
    LISTINGS("Listings Audit", "تایید آگهی‌ها", Icons.Default.Assignment),
    PAYMENTS("Payments (Receipts)", "بررسی فیش‌ها", Icons.Default.AccountBalanceWallet),
    DEALS("Escrow Deals", "معاملات فعال واسط", Icons.Default.Handshake),
    REPORTS("Policy Reports", "گزارش‌های تخلف", Icons.Default.Report),
    VERIFIED_SELLERS("Seller Verification", "احراز هویت فروشنده", Icons.Default.Verified),
    GAMES("Games Manager", "مدیریت بازی‌ها", Icons.Default.Gamepad),
    CATEGORIES("Categories Manager", "مدیریت دسته‌ها", Icons.Default.Category),
    BROADCAST("System Broadcast", "اعلامیه همگانی", Icons.Default.Campaign),
    BANNERS("Promo Banners", "بَنرهای تبلیغاتی", Icons.Default.Collections),
    CMS("CMS content pages", "مدیریت صفحات CMS", Icons.Default.Description),
    AUDIT_LOGS("Audit Action Logs", "تاریخچه فعالیت مدیران", Icons.Default.History)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminPanelScreen(
    adminViewModel: AdminViewModel,
    currentRole: String, // "USER", "ADMIN", "SUPER_ADMIN"
    onBack: () -> Unit,
    isRtl: Boolean,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val hasAccess = currentRole.lowercase() == "super_admin" || currentRole.lowercase() == "admin"
    val isSuperAdmin = currentRole.lowercase() == "super_admin"

    if (!hasAccess) {
        // Access Denied Screen (as requested per Part 2)
        Scaffold(
            topBar = {
                PrimeToolbar(
                    title = if (isRtl) "خطای دسترسی" else "Access Denied",
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back")
                        }
                    }
                )
            },
            modifier = modifier.testTag("admin_access_denied_screen")
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Access Locked",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(80.dp)
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    Text(
                        text = if (isRtl) "دسترسی امکان‌پذیر نیست" else "Access Denied",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = if (isRtl) "شما مجوز دسترسی به این بخش را ندارید / You do not have permission to access this section."
                               else "You do not have permission to access this section.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    PrimeButton(
                        text = if (isRtl) "بازگشت به نمایه" else "Back to Profile",
                        onClick = onBack,
                        modifier = Modifier.width(200.dp)
                    )
                }
            }
        }
        return
    }

    var selectedTab by remember { mutableStateOf(AdminTab.STATS) }

    Scaffold(
        topBar = {
            PrimeToolbar(
                title = if (isRtl) "پنل جامع مدیریتی 🛡" else "Secure Escrow Commander Hub",
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        modifier = modifier.testTag("admin_panel_screen")
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Scrollable Tab navigation row for high density admin selections
            ScrollableTabRow(
                selectedTabIndex = selectedTab.ordinal,
                edgePadding = 12.dp,
                containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp)
            ) {
                AdminTab.values().forEach { tab ->
                    Tab(
                        selected = selectedTab == tab,
                        onClick = { selectedTab = tab },
                        text = {
                            Text(
                                text = if (isRtl) tab.titleFa else tab.titleEn,
                                fontSize = 12.sp,
                                fontWeight = if (selectedTab == tab) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        icon = {
                            Icon(
                                imageVector = tab.icon,
                                contentDescription = tab.titleEn,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    )
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
            ) {
                // Show relative sub-menus according to user tabs
                when (selectedTab) {
                    AdminTab.STATS -> StatsView(adminViewModel, isRtl)
                    AdminTab.USERS -> UsersManagementView(adminViewModel, isSuperAdmin, isRtl)
                    AdminTab.LISTINGS -> ListingsAuditView(adminViewModel, isRtl)
                    AdminTab.PAYMENTS -> PaymentsAuditView(adminViewModel, isRtl)
                    AdminTab.DEALS -> EscrowDealsView(adminViewModel, isRtl)
                    AdminTab.REPORTS -> ReportsInboxView(adminViewModel, isRtl)
                    AdminTab.VERIFIED_SELLERS -> VerifiedSellersRequestView(adminViewModel, isRtl)
                    AdminTab.GAMES -> GamesManagementView(adminViewModel, isSuperAdmin, isRtl)
                    AdminTab.CATEGORIES -> CategoriesManagementView(adminViewModel, isSuperAdmin, isRtl)
                    AdminTab.BROADCAST -> AnnouncementBroadcastView(adminViewModel, isSuperAdmin, isRtl)
                    AdminTab.BANNERS -> BannersManagementView(adminViewModel, isSuperAdmin, isRtl)
                    AdminTab.CMS -> CmsPagesView(adminViewModel, isRtl)
                    AdminTab.AUDIT_LOGS -> AuditActionLogsView(adminViewModel, isSuperAdmin, isRtl)
                }
            }
        }
    }
}

// ==========================================
// 1. LIVE STATISTICS & REAL-TIME COMMISSION
// ==========================================
@Composable
fun StatsView(viewModel: AdminViewModel, isRtl: Boolean) {
    val context = LocalContext.current
    val stats = viewModel.getSystemStatistics()
    val commissionRate by viewModel.commissionPercent.collectAsState()
    val commissionFixed by viewModel.commissionFixedFee.collectAsState()

    var showCommissionDlg by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.Percent, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isRtl) "تنظیمات کارمزد پلتفرم" else "Global Escrow Commission",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(
                        text = if (isRtl) "درصد کارمزد معامله واسطه:" else "Percentage commission rate:",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "$commissionRate %",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(
                        text = if (isRtl) "کارمزد ثابت معاملات (اضافی):" else "Fixed commission fee:",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "${commissionFixed.toInt()} ${if (isRtl) "تومان" else "Toman"}",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = { showCommissionDlg = true },
                    modifier = Modifier.align(Alignment.End),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(if (isRtl) "ویرایش کارمزد" else "Adjust Escrow Fees")
                }
            }
        }

        // Live grid statistics
        Text(
            text = if (isRtl) "📊 نماگرهای زنده بازار و مالی" else "📊 Live Marketplace Metrics Dashboard",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
        )

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            MetricCard(
                title = if (isRtl) "کل کاربران" else "Total Users",
                value = stats.totalUsers.toString(),
                modifier = Modifier.weight(1f)
            )
            MetricCard(
                title = if (isRtl) "فروشنده معتبر" else "Verified Sellers",
                value = stats.verifiedSellers.toString(),
                modifier = Modifier.weight(1f)
            )
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            MetricCard(
                title = if (isRtl) "کل آگهی‌ها" else "Total Listings",
                value = stats.totalListings.toString(),
                modifier = Modifier.weight(1f)
            )
            MetricCard(
                title = if (isRtl) "معاملات ثبت‌شده" else "Total Escrow Deals",
                value = stats.totalDeals.toString(),
                modifier = Modifier.weight(1f)
            )
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            MetricCard(
                title = if (isRtl) "گزارش تخلف باز" else "Active Disputes",
                value = stats.activeReports.toString(),
                modifier = Modifier.weight(1f),
                color = MaterialTheme.colorScheme.errorContainer
            )
            MetricCard(
                title = if (isRtl) "معاملات موفق" else "Completed Trades",
                value = stats.completedDeals.toString(),
                modifier = Modifier.weight(1f),
                color = MaterialTheme.colorScheme.tertiaryContainer
            )
        }

        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.2f)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = if (isRtl) "💳 گردش مالی و درآمدهای پلتفرم" else "💳 Financial Payouts & Total Collected",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
                Divider()
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(text = if (isRtl) "کل ارزش معاملات واسط موفق:" else "Total Gross Volume Transacted:")
                    Text(
                        text = String.format("%,.0f %s", stats.totalRevenue, if (isRtl) "تومان" else "Tomans"),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(text = if (isRtl) "کارمزد پلتفرم وصول شده:" else "Total Commission Revenue:")
                    Text(
                        text = String.format("%,.0f %s", stats.commissionCollected, if (isRtl) "تومان" else "Tomans"),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    )
                }
            }
        }
    }

    if (showCommissionDlg) {
        var feePercentInput by remember { mutableStateOf(commissionRate.toString()) }
        var fixedFeeInput by remember { mutableStateOf(commissionFixed.toInt().toString()) }

        AlertDialog(
            onDismissRequest = { showCommissionDlg = false },
            title = { Text(if (isRtl) "ویرایش نرخ کارمزد تراکنش‌ها" else "Adjust ESCROW Commission Fees") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = if (isRtl) "این تغییرات فورا بر کارمزد تمام معامله‌های آینده تاثیر می‌گذارد." else "This modify affects commission calculations of any future secured escrows instantly.",
                        style = MaterialTheme.typography.bodySmall
                    )
                    OutlinedTextField(
                        value = feePercentInput,
                        onValueChange = { feePercentInput = it },
                        label = { Text(if (isRtl) "درصد کارمزد پلتفرم" else "Percentage Commission (%)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = fixedFeeInput,
                        onValueChange = { fixedFeeInput = it },
                        label = { Text(if (isRtl) "کارمزد ثابت معامله (تومان)" else "Fixed Deal Fee") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val pct = feePercentInput.toDoubleOrNull() ?: 2.5
                        val fix = fixedFeeInput.toDoubleOrNull() ?: 0.0
                        viewModel.updateCommission("system_admin_1", pct, fix)
                        showCommissionDlg = false
                        Toast.makeText(context, if (isRtl) "کارمزد پلتفرم ویرایش شد!" else "Escrow commissions updated!", Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Text(if (isRtl) "ذخیره تغییرات" else "Save Adjustments")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCommissionDlg = false }) {
                    Text(if (isRtl) "انصراف" else "Cancel")
                }
            }
        )
    }
}

@Composable
fun MetricCard(title: String, value: String, modifier: Modifier = Modifier, color: Color = Color.Unspecified) {
    Card(
        modifier = modifier,
        colors = if (color != Color.Unspecified) CardDefaults.cardColors(containerColor = color.copy(alpha = 0.15f)) else CardDefaults.cardColors(),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = title, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = value, style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold))
        }
    }
}


// ==========================================
// 2. USERS LIST & ADVANCED ACCESS PERMISSIONS
// ==========================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UsersManagementView(viewModel: AdminViewModel, isSuperAdmin: Boolean, isRtl: Boolean) {
    val context = LocalContext.current
    val users by viewModel.users.collectAsState()
    var searchQuery by remember { mutableStateOf("") }
    var statusFilter by remember { mutableStateOf("All") }

    val filteredList = users.filter { u ->
        val matchesSearch = u.fullName.contains(searchQuery, ignoreCase = true) || u.email.contains(searchQuery, ignoreCase = true)
        val matchesFilter = if (statusFilter == "All") true else u.status.equals(statusFilter, ignoreCase = true)
        matchesSearch && matchesFilter
    }

    var showCreateAdminDlg by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (isRtl) "👥 مدیریت کل اعضاء و پرسنل" else "👥 Global Membership Directory",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
            if (isSuperAdmin) {
                Button(
                    onClick = { showCreateAdminDlg = true },
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(if (isRtl) "ایجاد مدیر جدید" else "Create Admin")
                }
            }
        }

        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text(if (isRtl) "جستجو در اسامی یا ایمیل‌ها..." else "Search by profile name or email...") },
            leadingIcon = { Icon(imageVector = Icons.Default.Search, contentDescription = null) },
            modifier = Modifier.fillMaxWidth(),
            maxLines = 1,
            shape = RoundedCornerShape(12.dp)
        )

        // Status scrollable chips
        val statusList = listOf("All", "Pending", "Approved", "Suspended", "Banned")
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            statusList.forEach { filter ->
                val isSelected = statusFilter == filter
                FilterChip(
                    selected = isSelected,
                    onClick = { statusFilter = filter },
                    label = { Text(filter) }
                )
            }
        }

        if (filteredList.isEmpty()) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                EmptyState(
                    title = if (isRtl) "کاربری یافت نشد" else "No users registered",
                    description = if (isRtl) "کاربر منطبق با فیلترها در دیتابیس وجود ندارد." else "No platform users found matching your selected search query.",
                    icon = Icons.Default.People
                )
            }
        } else {
            val currentAdminId = FirebaseInitializer.auth?.currentUser?.uid ?: "admin"
            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filteredList) { user ->
                    UserAdminRowCard(user, isSuperAdmin, isRtl, onAction = { actionType, data ->
                        when (actionType) {
                            "PROMOTE" -> viewModel.promoteUser(currentAdminId, user.uid, data)
                            "DEMOTE" -> viewModel.demoteAdmin(currentAdminId, user.uid)
                            "SUSPEND" -> viewModel.suspendUser(currentAdminId, user.uid, data)
                            "BAN" -> viewModel.banUser(currentAdminId, user.uid, data)
                            "RESTORE" -> viewModel.restoreUser(currentAdminId, user.uid)
                        }
                        Toast.makeText(context, if (isRtl) "عملیات با موفقیت ثبت پیگرد شد" else "User modifier successfully transacted.", Toast.LENGTH_SHORT).show()
                    })
                }
            }
        }
    }

    if (showCreateAdminDlg) {
        var emailInput by remember { mutableStateOf("") }
        var nameInput by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showCreateAdminDlg = false },
            title = { Text(if (isRtl) "ثبت‌نام ناظر / ایجاد مدیر سیستم" else "Create New Platform Moderator") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = nameInput,
                        onValueChange = { nameInput = it },
                        label = { Text(if (isRtl) "نام کامل مدیر" else "Full Name") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = emailInput,
                        onValueChange = { emailInput = it },
                        label = { Text(if (isRtl) "آدرس ایمیل ناظر" else "Admin Email Address") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (emailInput.isNotEmpty() && nameInput.isNotEmpty()) {
                            viewModel.createAdmin("super_admin_1", emailInput.trim(), nameInput.trim())
                            showCreateAdminDlg = false
                            Toast.makeText(context, if (isRtl) "مدیر سیستم با موفقیت افزوده شد" else "New Admin account added!", Toast.LENGTH_SHORT).show()
                        }
                    }
                ) {
                    Text(if (isRtl) "ثبت حساب ناظر" else "Provision Admin")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateAdminDlg = false }) {
                    Text(if (isRtl) "کنسل" else "Cancel")
                }
            }
        )
    }
}

@Composable
fun UserAdminRowCard(
    user: FirestoreUser,
    isSuperAdmin: Boolean,
    isRtl: Boolean,
    onAction: (String, String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                Column {
                    Text(text = user.fullName, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                    Text(text = user.email, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Surface(
                        color = when (user.role.lowercase()) {
                            "super_admin" -> MaterialTheme.colorScheme.tertiaryContainer
                            "admin" -> MaterialTheme.colorScheme.primaryContainer
                            "moderator" -> MaterialTheme.colorScheme.secondaryContainer
                            else -> MaterialTheme.colorScheme.secondaryContainer
                        },
                        shape = RoundedCornerShape(50)
                    ) {
                        Text(
                            text = when (user.role.lowercase()) {
                                "super_admin" -> if (isRtl) "مدیر کل" else "Super Admin"
                                "admin" -> if (isRtl) "مدیر" else "Admin"
                                "moderator" -> if (isRtl) "ناظر" else "Moderator"
                                else -> if (isRtl) "کاربر" else "User"
                            },
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }

                    Surface(
                        color = when (user.status) {
                            "Approved" -> MaterialTheme.colorScheme.onPrimary
                            "Suspended" -> MaterialTheme.colorScheme.errorContainer
                            "Banned" -> MaterialTheme.colorScheme.error
                            else -> MaterialTheme.colorScheme.outline
                        },
                        shape = RoundedCornerShape(50)
                    ) {
                        Text(
                            text = user.status,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                            color = if (user.status == "Banned") MaterialTheme.colorScheme.onError else Color.Unspecified
                        )
                    }
                }
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    text = "${if (isRtl) "امتیاز اعتماد:" else "Ratings trust score:"} ${user.trustScore}/100",
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold)
                )
                if (user.isVerifiedSeller) {
                    Text(
                        text = if (isRtl) "✓ فروشنده معتبر" else "✓ Verified Professional",
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Divider()

            // Super Admin actions (Roles escalation/Promote/Demote)
            if (isSuperAdmin) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (user.role.lowercase() != "super_admin") {
                        Button(
                            onClick = { onAction("PROMOTE", "admin") },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(2.dp)
                        ) {
                            Text(if (isRtl) "ارتقا به مدیر" else "Promote Admin", fontSize = 10.sp)
                        }
                    } else if (user.uid != "super_admin_1") {
                        Button(
                            onClick = { onAction("DEMOTE", "") },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.outline),
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(2.dp)
                        ) {
                            Text(if (isRtl) "تنزل به کاربر" else "Demote to User", fontSize = 10.sp)
                        }
                    }

                    if (user.uid != "super_admin_1") {
                        if (user.status == "Suspended" || user.status == "Banned") {
                            Button(
                                onClick = { onAction("RESTORE", "") },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary),
                                shape = RoundedCornerShape(6.dp),
                                modifier = Modifier.weight(1f),
                                contentPadding = PaddingValues(2.dp)
                            ) {
                                Text(if (isRtl) "رفع مسدودیت" else "Restore Access", fontSize = 10.sp)
                            }
                        } else {
                            Button(
                                onClick = { onAction("SUSPEND", "Policy Violation Check") },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                                shape = RoundedCornerShape(6.dp),
                                modifier = Modifier.weight(1.5f),
                                contentPadding = PaddingValues(2.dp)
                            ) {
                                Text(if (isRtl) "تعلیق موقت" else "Suspend", fontSize = 10.sp)
                            }
                            Button(
                                onClick = { onAction("BAN", "Scam Infraction Logged") },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                                shape = RoundedCornerShape(6.dp),
                                modifier = Modifier.weight(1f),
                                contentPadding = PaddingValues(2.dp)
                            ) {
                                Text(if (isRtl) "بلاک دائم" else "Block BAN", fontSize = 10.sp)
                            }
                        }
                    }
                }
            } else {
                // Generics moderators checks (Approve/Reject Pending users status)
                if (user.status == "Pending" && user.uid != "super_admin_1") {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { onAction("RESTORE", "") },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(if (isRtl) "تایید کاربر جدید" else "Approve User")
                        }
                        Button(
                            onClick = { onAction("BAN", "Verification rejected by moderator.") },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(if (isRtl) "رد کردن" else "Reject")
                        }
                    }
                } else if (user.uid != "super_admin_1" && user.role != "Admin" && user.role != "Super Admin") {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { onAction("SUSPEND", "Suspected violations") },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(if (isRtl) "تعلیق کاربر" else "Suspend Profile", fontSize = 11.sp)
                        }
                    }
                }
            }
        }
    }
}


// ==========================================
// 3. LISTINGS AUDIT & MANUAL PUBLISHING CONTROL
// ==========================================
@Composable
fun ListingsAuditView(viewModel: AdminViewModel, isRtl: Boolean) {
    val listings by ListingsRepository.listings.collectAsState()
    var selectedFilter by remember { mutableStateOf("Pending Audit") }

    val showListings = listings.filter {
        when (selectedFilter) {
            "Pending Audit" -> it.status == "Available" // treating available as pending under test if needed, or if we define "Pending" as status
            "Approved" -> it.status == "Available"
            else -> it.status == "Rejected" || it.status == "Sold"
        }
    }

    val context = LocalContext.current

    Column(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = if (isRtl) "📋 صندوق ممیزی آگهی‌های فروشگاهی" else "📋 Shop Catalog Listings Audit Inbox",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
        )

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf("Pending Audit", "Approved", "Rejected").forEach { filter ->
                val isSelected = selectedFilter == filter
                FilterChip(
                    selected = isSelected,
                    onClick = { selectedFilter = filter },
                    label = { Text(filter) }
                )
            }
        }

        if (showListings.isEmpty()) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                EmptyState(
                    title = if (isRtl) "هیچ پرونده‌ای یافت نشد" else "Clean audit list",
                    description = if (isRtl) "عنوانی برای بررسی در این زبانه یافت نشد." else "No pending items or audited games exist in this classification slot.",
                    icon = Icons.Default.Assignment
                )
            }
        } else {
            LazyColumn(modifier = Modifier.weight(1f).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(showListings) { listing ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(text = listing.title, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                                Text(text = formattedPrice(listing.price, isRtl) + " " + if (isRtl) "تومان" else "T", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                            }
                            Text(text = "${if (isRtl) "بازی مرتبط:" else "Title Game:"} ${listing.game}", style = MaterialTheme.typography.labelSmall)
                            Text(text = "${if (isRtl) "توضیحات:" else "Details:"} ${listing.description}", style = MaterialTheme.typography.bodyMedium, maxLines = 3, overflow = TextOverflow.Ellipsis)
                            Text(text = "${if (isRtl) "شناسه فروشنده:" else "Seller UID:"} ${listing.sellerId}", style = MaterialTheme.typography.labelSmall)

                            Spacer(modifier = Modifier.height(6.dp))
                            Divider()

                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = {
                                        viewModel.approveListing("system_admin_1", listing.listingId)
                                        Toast.makeText(context, if (isRtl) "آگهی با موفقیت تایید و منتشر شد" else "Listing approved successfully!", Toast.LENGTH_SHORT).show()
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(if (isRtl) "تایید و انتشار" else "Approve & Push Live")
                                }
                                Button(
                                    onClick = {
                                        viewModel.rejectListing("system_admin_1", listing.listingId, "Incorrect account information / نقض عهد و ممیزی مشخصات")
                                        Toast.makeText(context, if (isRtl) "آگهی رد انتشار شد" else "Listing rejected audit check.", Toast.LENGTH_SHORT).show()
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(if (isRtl) "رد انتشار" else "Reject listing")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}


// ==========================================
// 4. BANK PAYMENT RECEIPTS AUDITING
// ==========================================
@Composable
fun PaymentsAuditView(viewModel: AdminViewModel, isRtl: Boolean) {
    val deals by FirebaseDatabaseManager.deals.collectAsState()
    val pendingDeals = deals.filter { it.status == "RECEIPT_UPLOADED" || it.receiptImageUrl.isNotEmpty() }
    val context = LocalContext.current

    Column(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = if (isRtl) "🏦 بررسی رسیدهای بانکی (تراکنش‌های امانت)" else "🏦 Escrow Bank Checks & Receipt Reviews",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
        )

        if (pendingDeals.isEmpty()) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                EmptyState(
                    title = if (isRtl) "رسیدی موجود نیست" else "All receipts verified",
                    description = if (isRtl) "هیچ بانکی یا فیش آپلود شده در صف بررسی ناظر وجود ندارد." else "Nice job! Any billing or transacted receipts have been successfully validated.",
                    icon = Icons.Default.AccountBalanceWallet
                )
            }
        } else {
            LazyColumn(modifier = Modifier.weight(1f).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(pendingDeals) { deal ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(text = "Deal ID: #${deal.dealId.take(8).uppercase()}", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                                Surface(color = MaterialTheme.colorScheme.tertiaryContainer, shape = RoundedCornerShape(50)) {
                                    Text(text = deal.status, modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp), fontSize = 10.sp)
                                }
                            }

                            Text(text = "${if (isRtl) "مبلغ نهایی معامله:" else "Escrow Balance Amount:"} ${formattedPrice(deal.amount + deal.fee, isRtl)} ${if (isRtl) "تومان" else "Tomans"}")
                            Text(text = "${if (isRtl) "خریدار:" else "Buyer UID:"} ${deal.buyerId}")
                            Text(text = "${if (isRtl) "فروشنده:" else "Seller UID:"} ${deal.sellerId}")

                            // Display transacted bank receipt screenshot if present
                            Text(
                                text = if (isRtl) "📸 رسید فیش بارگذاری شده بانکی:" else "📸 Uploaded Bank Receipt Attachment:",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                            )

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(180.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color.DarkGray),
                                contentAlignment = Alignment.Center
                            ) {
                                AsyncImage(
                                    model = deal.receiptImageUrl.ifEmpty { "https://picsum.photos/600/300?random=receipt" },
                                    contentDescription = "Bank Receipt",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Inside
                                )
                                if (deal.receiptImageUrl.isEmpty()) {
                                    Surface(color = Color.Black.copy(0.4f), shape = RoundedCornerShape(4.dp)) {
                                        Text("[Local Sandbox Simulated Screenshot Receipt]", color = Color.White, modifier = Modifier.padding(4.dp), fontSize = 10.sp)
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(4.dp))
                            Divider()

                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = {
                                        viewModel.approvePayment("system_admin_1", deal.dealId)
                                        Toast.makeText(context, if (isRtl) "رسید تایید و معامله امن آزاد شد!" else "Escrow bank cleared, seller notified!", Toast.LENGTH_SHORT).show()
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(if (isRtl) "تایید رسید بانکی" else "Clear Payment ✅")
                                }
                                Button(
                                    onClick = {
                                        viewModel.rejectPayment("system_admin_1", deal.dealId, "Fake bank slip")
                                        Toast.makeText(context, if (isRtl) "رسید بانکی رد شد" else "Payment slip rejected.", Toast.LENGTH_SHORT).show()
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(if (isRtl) "رد کردن رسید" else "Reject slip ❌")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}


// ==========================================
// 5. ESCROW DEALS TRACKING & RESOLVING DISPUTES
// ==========================================
@Composable
fun EscrowDealsView(viewModel: AdminViewModel, isRtl: Boolean) {
    val deals by FirebaseDatabaseManager.deals.collectAsState()
    val context = LocalContext.current

    Column(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = if (isRtl) "🤝 لیست کل معاملات واسط رسمی پلتفرم" else "🤝 Global Escrow Registry & Arbitration",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
        )

        if (deals.isEmpty()) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                EmptyState(
                    title = if (isRtl) "معامله‌ای یافت نشد" else "Registry is Empty",
                    description = if (isRtl) "هیچ معامله فعالی در کل شبکه وجود ندارد." else "No safe escrow transaction folders have been registered yet.",
                    icon = Icons.Default.Handshake
                )
            }
        } else {
            LazyColumn(modifier = Modifier.weight(1f).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(deals) { deal ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(text = "ID: #${deal.dealId.take(8).uppercase()}", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                                Surface(
                                    color = if (deal.status == "DISPUTE_OPEN") MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.primaryContainer,
                                    shape = RoundedCornerShape(50)
                                ) {
                                    Text(text = deal.status, modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                            }

                            Text(text = "${if (isRtl) "کل ارزش تراکنش:" else "Total Cash Balance:"} ${formattedPrice(deal.amount, isRtl)} T")
                            Text(text = "${if (isRtl) "کارمزد پلتفرم:" else "Escrow Platform Fee:"} ${formattedPrice(deal.fee, isRtl)} T")
                            Text(text = "Buyer: ${deal.buyerId} | Seller: ${deal.sellerId}")

                            if (deal.status == "DISPUTE_OPEN") {
                                Spacer(modifier = Modifier.height(4.dp))
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(MaterialTheme.colorScheme.errorContainer.copy(0.3f), RoundedCornerShape(8.dp))
                                        .padding(10.dp)
                                ) {
                                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Text(
                                            text = if (isRtl) "🚨 اختلاف ثبت‌شده توسط طرفین نیاز به داوری دارد:" else "🚨 Active unresolved dispute. Admin intervention required:",
                                            color = MaterialTheme.colorScheme.error,
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                                        )
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            Button(
                                                onClick = {
                                                    viewModel.resolveDispute("system_admin_1", deal.dealId, true)
                                                    Toast.makeText(context, if (isRtl) "حل اختلاف به نفع خریدار" else "Dispute resolved favoring Buyer!", Toast.LENGTH_SHORT).show()
                                                },
                                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Text(if (isRtl) "عودت وجه به خریدار" else "Refund Buyer")
                                            }
                                            Button(
                                                onClick = {
                                                    viewModel.resolveDispute("system_admin_1", deal.dealId, false)
                                                    Toast.makeText(context, if (isRtl) "حل اختلاف به نفع فروشنده" else "Dispute resolved favoring Seller!", Toast.LENGTH_SHORT).show()
                                                },
                                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary),
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Text(if (isRtl) "آزادسازی برای فروشنده" else "Release to Seller")
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}


// ==========================================
// 6. GENERAL POLICY INBOX REPORTS MANAGEMENT
// ==========================================
@Composable
fun ReportsInboxView(viewModel: AdminViewModel, isRtl: Boolean) {
    val reports by TrustAndReputationManager.reports.collectAsState()
    val context = LocalContext.current

    Column(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = if (isRtl) "📋 گزارش‌های تخلف فعالان شبکه" else "📋 Active Policy Infraction Reports Inbox",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
        )

        if (reports.isEmpty()) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                EmptyState(
                    title = if (isRtl) "هیچ گزارشی ثبت نشده است" else "All quiet!",
                    description = if (isRtl) "هیچ پرونده گزارش باز بر روی پلتفرم وجود ندارد." else "No pending policy infraction reports present on the platform.",
                    icon = Icons.Default.Report
                )
            }
        } else {
            LazyColumn(modifier = Modifier.weight(1f).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(reports) { r ->
                    var adminNotes by remember { mutableStateOf("") }
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(text = "Report ID: #${r.reportId.take(8).uppercase()}", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                                Surface(color = if (r.status == "Open") MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.outlineVariant, shape = RoundedCornerShape(50)) {
                                    Text(text = r.status, modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp), fontSize = 10.sp)
                                }
                            }
                            Text(text = "Reporter: ${r.reporterId} | Target [${r.targetType}]: ${r.targetId}", style = MaterialTheme.typography.labelMedium)
                            Text(text = "Category: ${r.reason}", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                            Text(text = "Details: ${r.description}", style = MaterialTheme.typography.bodyMedium)

                            if (r.status == "Open") {
                                OutlinedTextField(
                                    value = adminNotes,
                                    onValueChange = { adminNotes = it },
                                    label = { Text(if (isRtl) "توضیحات بستن گزارش" else "Resolution Notes") },
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Button(
                                        onClick = {
                                            viewModel.resolveReport("system_admin_1", r.reportId, adminNotes.ifEmpty { "Investigated and cleared." })
                                            Toast.makeText(context, if (isRtl) "گزارش حل و بسته شد" else "Report resolved & closed.", Toast.LENGTH_SHORT).show()
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text(if (isRtl) "حل و بستن پرونده" else "Resolve & Close")
                                    }
                                }
                            } else {
                                Text(text = "Admin Resolution: \"${r.adminNotes}\"", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        }
    }
}


// ==========================================
// 7. VERIFIED SELLERS REQUEST CHECKS
// ==========================================
@Composable
fun VerifiedSellersRequestView(viewModel: AdminViewModel, isRtl: Boolean) {
    val requests by FirebaseDatabaseManager.allVerificationRequests.collectAsState()
    val pendingReqs = requests.filter { it.status == "Pending" || it.bankCardNumber.isNotEmpty() }
    val context = LocalContext.current

    Column(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = if (isRtl) "👑 درخواست‌های احراز هویت فروشندگان" else "👑 Seller Professional Verified Applications",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
        )

        if (pendingReqs.isEmpty()) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                EmptyState(
                    title = if (isRtl) "صف در خواست خالی است" else "Inbox is clean",
                    description = if (isRtl) "هیچ درخواست تایید هویتی در نوبت بررسی وجود ندارد." else "No pending seller professional registration applications present.",
                    icon = Icons.Default.Verified
                )
            }
        } else {
            LazyColumn(modifier = Modifier.weight(1f).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(pendingReqs) { req ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(text = "User ID: ${req.userId}", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                                Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = RoundedCornerShape(50)) {
                                    Text(text = req.status, modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp), fontSize = 10.sp)
                                }
                            }
                            Text(text = "Name: ${req.fullName} | Phone: ${req.phoneNumber}")
                            Text(text = "National ID Code: ${req.nationalId}")
                            Text(text = "Bank Card: ${req.bankCardNumber} (${req.bankAccountHolderName})")

                            Spacer(modifier = Modifier.height(4.dp))
                            Divider()

                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = {
                                        viewModel.reviewVerification("system_admin_1", req.userId, true)
                                        Toast.makeText(context, if (isRtl) "درخواست پذیرفته شد!" else "Seller verified successfully!", Toast.LENGTH_SHORT).show()
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(if (isRtl) "پذیرش هویت" else "Approve Application")
                                }
                                Button(
                                    onClick = {
                                        viewModel.reviewVerification("system_admin_1", req.userId, false, "National code match failed")
                                        Toast.makeText(context, if (isRtl) "درخواست رد شد" else "Application rejected.", Toast.LENGTH_SHORT).show()
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(if (isRtl) "رد درخواست" else "Reject App")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}


// ==========================================
// 8. DYNAMIC GAMES CATALOG MANAGEMENTS
// ==========================================
@Composable
fun GamesManagementView(viewModel: AdminViewModel, isSuperAdmin: Boolean, isRtl: Boolean) {
    val games by viewModel.games.collectAsState()
    var showAddDlg by remember { mutableStateOf(false) }
    val context = LocalContext.current

    Column(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = if (isRtl) "🎮 مدیریت کاتالوگ بازی‌های پلتفرم" else "🎮 Supported Interactive Game Library",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
            if (isSuperAdmin) {
                Button(onClick = { showAddDlg = true }, shape = RoundedCornerShape(8.dp)) {
                    Text(if (isRtl) "افزودن بازی" else "+ New Game")
                }
            } else {
                Surface(color = MaterialTheme.colorScheme.errorContainer, shape = RoundedCornerShape(4.dp)) {
                    Text("[Super Admin Only 🔒]", modifier = Modifier.padding(4.dp), fontSize = 10.sp, color = MaterialTheme.colorScheme.onErrorContainer)
                }
            }
        }

        LazyColumn(modifier = Modifier.weight(1f).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(games) { game ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(text = game.name, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                            if (isSuperAdmin) {
                                IconButton(onClick = {
                                    viewModel.deleteGame("super_admin_1", game.gameId)
                                    Toast.makeText(context, if (isRtl) "بازی حذف شد" else "Game deleted from catalog.", Toast.LENGTH_SHORT).show()
                                }) {
                                    Icon(imageVector = Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                        Text(text = game.description, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
    }

    if (showAddDlg && isSuperAdmin) {
        var name by remember { mutableStateOf("") }
        var desc by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showAddDlg = false },
            title = { Text(if (isRtl) "افزودن عنوان بازی جدید" else "Enter New Supported Gaming Title") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text(if (isRtl) "نام کامل بازی" else "Game Name") })
                    OutlinedTextField(value = desc, onValueChange = { desc = it }, label = { Text(if (isRtl) "توضیحات مختصر بازی" else "Short Intro/Description") })
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (name.isNotEmpty()) {
                        viewModel.addGame("super_admin_1", name, desc)
                        showAddDlg = false
                        Toast.makeText(context, "Game added successfully!", Toast.LENGTH_SHORT).show()
                    }
                }) {
                    Text(if (isRtl) "افزودن" else "Add Title")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDlg = false }) {
                    Text(if (isRtl) "لغو" else "Cancel")
                }
            }
        )
    }
}


// ==========================================
// 9. DYNAMIC CATEGORIES IN GAMES CATALOG
// ==========================================
@Composable
fun CategoriesManagementView(viewModel: AdminViewModel, isSuperAdmin: Boolean, isRtl: Boolean) {
    val categories by viewModel.categories.collectAsState()
    val games by viewModel.games.collectAsState()
    var showAddDlg by remember { mutableStateOf(false) }
    val context = LocalContext.current

    Column(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = if (isRtl) "📂 مدیریت زیرودسته‌ها و طبقه‌بندی" else "📂 Market Categories Directory Manager",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
            if (isSuperAdmin && games.isNotEmpty()) {
                Button(onClick = { showAddDlg = true }, shape = RoundedCornerShape(8.dp)) {
                    Text(if (isRtl) "افزودن دسته" else "+ New Category")
                }
            } else if (!isSuperAdmin) {
                Surface(color = MaterialTheme.colorScheme.errorContainer, shape = RoundedCornerShape(4.dp)) {
                    Text("[Super Admin Only 🔒]", modifier = Modifier.padding(4.dp), fontSize = 10.sp, color = MaterialTheme.colorScheme.onErrorContainer)
                }
            }
        }

        LazyColumn(modifier = Modifier.weight(1f).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(categories) { cat ->
                val bindGameName = games.find { it.gameId == cat.gameId }?.name ?: cat.gameId
                Card(modifier = Modifier.fillMaxWidth()) {
                    Row(modifier = Modifier.padding(14.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column {
                            Text(text = "${cat.nameEn} / ${cat.nameFa}", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                            Text(text = "Linked Game: $bindGameName", style = MaterialTheme.typography.labelSmall)
                        }
                        if (isSuperAdmin) {
                            IconButton(onClick = {
                                viewModel.deleteCategory("super_admin_1", cat.categoryId)
                                Toast.makeText(context, if (isRtl) "دسته حذف گردید" else "Category deleted.", Toast.LENGTH_SHORT).show()
                            }) {
                                Icon(imageVector = Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddDlg && isSuperAdmin) {
        var faName by remember { mutableStateOf("") }
        var enName by remember { mutableStateOf("") }
        var selectedGameId by remember { mutableStateOf(games.firstOrNull()?.gameId ?: "") }

        AlertDialog(
            onDismissRequest = { showAddDlg = false },
            title = { Text(if (isRtl) "ایجاد دسته‌بندی جدید" else "Enter New Market Category") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(value = enName, onValueChange = { enName = it }, label = { Text(if (isRtl) "نام انگلیسی دسته" else "Category English (En)") })
                    OutlinedTextField(value = faName, onValueChange = { faName = it }, label = { Text(if (isRtl) "نام فارسی دسته" else "Category Persian (Fa)") })
                    
                    Text(text = if (isRtl) "اتصال با کدام بازی؟" else "Primary Associated Title:", style = MaterialTheme.typography.labelSmall)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        games.forEach { g ->
                            val isSel = selectedGameId == g.gameId
                            Button(
                                onClick = { selectedGameId = g.gameId },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isSel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                                ),
                                contentPadding = PaddingValues(horizontal = 6.dp)
                            ) {
                                Text(g.name, fontSize = 10.sp)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (enName.isNotEmpty() && faName.isNotEmpty() && selectedGameId.isNotEmpty()) {
                        viewModel.addCategory("super_admin_1", selectedGameId, enName, faName)
                        showAddDlg = false
                        Toast.makeText(context, "Category created successfully!", Toast.LENGTH_SHORT).show()
                    }
                }) {
                    Text(if (isRtl) "افزودن" else "Add Category")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDlg = false }) {
                    Text(if (isRtl) "لغو" else "Cancel")
                }
            }
        )
    }
}


// ==========================================
// 10. SYSTEM BROADCASTING & LIVE ANNOUNCEMENT
// ==========================================
@Composable
fun AnnouncementBroadcastView(viewModel: AdminViewModel, isSuperAdmin: Boolean, isRtl: Boolean) {
    var title by remember { mutableStateOf("") }
    var msg by remember { mutableStateOf("") }
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = if (isRtl) "📢 دیسپچ اعلان همگانی (رادیو واسط)" else "📢 Secure Broadcast & Notification Dispatcher",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
        )

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = if (isRtl) "کاربران به محض ارسال، اعلان هشداری بر گوشی خود دریافت خواهند کرد." else "This action deploys real-time warning push notifications instantly to any platform client.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text(if (isRtl) "عنوان اعلان همگانی" else "Announcement Title") },
                    placeholder = { Text(if (isRtl) "مثلا: اختلال موقت در درگاه بانکی" else "Emergency status, server maintenance...") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = msg,
                    onValueChange = { msg = it },
                    label = { Text(if (isRtl) "پیام همگانی" else "Push Notification Body Message") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 4
                )

                Button(
                    onClick = {
                        if (title.isNotEmpty() && msg.isNotEmpty()) {
                            viewModel.broadcastAnnouncement("super_admin_1", title.trim(), msg.trim())
                            title = ""
                            msg = ""
                            Toast.makeText(context, if (isRtl) "اعلامیه با موفقیت ارسال شد!" else "Global warning notification successfully pushed!", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = isSuperAdmin
                ) {
                    Text(if (isRtl) "ارسال اعلان عمومی" else "Send Global Push Broadcast")
                }
                if (!isSuperAdmin) {
                    Text(
                        text = if (isRtl) "⚠️ دسترسی محدود شده است. فقط مدیر ارشد مجاز است." else "⚠️ Access Locked. Only Super Admins can publish announcements.",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )
                }
            }
        }
    }
}


// ==========================================
// 11. HOMEPAGE PROMOTIONS BANNERS MANAGEMENT
// ==========================================
@Composable
fun BannersManagementView(viewModel: AdminViewModel, isSuperAdmin: Boolean, isRtl: Boolean) {
    val banners by viewModel.banners.collectAsState()
    var showAddDlg by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val currentAdminId = FirebaseInitializer.auth?.currentUser?.uid ?: "admin"

    Column(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = if (isRtl) "🖼 مدیریت بنرهای اسلایدشو صفحه اصلی" else "🖼 Custom Promotional Slider Banners",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
            if (isSuperAdmin) {
                Button(onClick = { showAddDlg = true }, shape = RoundedCornerShape(8.dp)) {
                    Text(if (isRtl) "افزودن بنر" else "+ New Banner")
                }
            } else {
                Surface(color = MaterialTheme.colorScheme.errorContainer, shape = RoundedCornerShape(4.dp)) {
                    Text("[Super Admin Only 🔒]", modifier = Modifier.padding(4.dp), fontSize = 10.sp, color = MaterialTheme.colorScheme.onErrorContainer)
                }
            }
        }

        LazyColumn(modifier = Modifier.weight(1f).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(banners) { banner ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.Gray)
                        ) {
                            AsyncImage(
                                model = banner.imageUrl,
                                contentDescription = banner.title,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Column {
                                Text(text = banner.title, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                                Text(
                                    text = if (banner.isActive) (if (isRtl) "● فعال بر اپلیکیشن" else "● Active slider") else (if (isRtl) "غیرفعال" else "Status offline"),
                                    color = if (banner.isActive) MaterialTheme.colorScheme.primary else Color.Gray,
                                    fontSize = 11.sp
                                )
                            }
                            if (isSuperAdmin) {
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    IconButton(onClick = {
                                        viewModel.toggleBannerStatus(currentAdminId, banner.bannerId)
                                    }) {
                                        Icon(
                                            imageVector = if (banner.isActive) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                    IconButton(onClick = {
                                        viewModel.deleteBanner(currentAdminId, banner.bannerId)
                                        Toast.makeText(context, if (isRtl) "بنر حذف شد" else "Banner removed.", Toast.LENGTH_SHORT).show()
                                    }) {
                                        Icon(imageVector = Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddDlg && isSuperAdmin) {
        var title by remember { mutableStateOf("") }
        var imgUrl by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showAddDlg = false },
            title = { Text(if (isRtl) "افزودن بنر تبلیغاتی جدید" else "Enter New Slideshow Promo Banner") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text(if (isRtl) "عنوان بنر" else "Banner Title") })
                    OutlinedTextField(
                        value = imgUrl,
                        onValueChange = { imgUrl = it },
                        label = { Text(if (isRtl) "آدرس تصویر (URL)" else "Image URL Link") },
                        placeholder = { Text("https://example.com/banner.jpg") }
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (title.isNotEmpty() && imgUrl.isNotEmpty()) {
                        viewModel.addBanner(currentAdminId, title, imgUrl)
                        showAddDlg = false
                        Toast.makeText(context, "Slideshow banner published!", Toast.LENGTH_SHORT).show()
                    }
                }) {
                    Text(if (isRtl) "افزودن" else "Add Banner")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDlg = false }) {
                    Text(if (isRtl) "لغو" else "Cancel")
                }
            }
        )
    }
}


// ==========================================
// 12. HELP / INFO PAGES CMS EDITOR PANEL
// ==========================================
@Composable
fun CmsPagesView(viewModel: AdminViewModel, isRtl: Boolean) {
    val pages by viewModel.cmsPages.collectAsState()
    var selectedPage by remember { mutableStateOf<CmsPage?>(null) }
    val context = LocalContext.current

    Column(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = if (isRtl) "📝 مدیریت محتوای متنی و قوانین (CMS)" else "📝 Policy Agreements & Guide Pages CMS",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
        )

        if (selectedPage == null) {
            LazyColumn(modifier = Modifier.weight(1f).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(pages) { page ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedPage = page }
                    ) {
                        Row(modifier = Modifier.padding(14.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(text = page.title, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(text = page.content, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                            Icon(imageVector = Icons.Default.Edit, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
        } else {
            var inputTitle by remember { mutableStateOf(selectedPage!!.title) }
            var inputContent by remember { mutableStateOf(selectedPage!!.content) }

            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "Editing: ${selectedPage!!.pageId}", style = MaterialTheme.typography.labelSmall)
                    TextButton(onClick = { selectedPage = null }) {
                        Text(if (isRtl) "بازگشت" else "Back to list")
                    }
                }
                OutlinedTextField(
                    value = inputTitle,
                    onValueChange = { inputTitle = it },
                    label = { Text("Page Title") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = inputContent,
                    onValueChange = { inputContent = it },
                    label = { Text("Page Markdown/Content Text") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 8
                )

                Button(
                    onClick = {
                        viewModel.saveCmsPage("system_admin_1", selectedPage!!.pageId, inputTitle.trim(), inputContent.trim())
                        selectedPage = null
                        Toast.makeText(context, if (isRtl) "تغییرات صفحه متنی ذخیره شد" else "CMS layout page modifications successfully written!", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (isRtl) "ذخیره تغییرات و بروزرسانی" else "Commit Page Updates")
                }
            }
        }
    }
}


// ==========================================
// 13. AUDIT ACTION LOGS HISTORY DISPLAY
// ==========================================
@Composable
fun AuditActionLogsView(viewModel: AdminViewModel, isSuperAdmin: Boolean, isRtl: Boolean) {
    val logs by viewModel.auditLogs.collectAsState()

    Column(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = if (isRtl) "📜 تاریخچه عملیات و ممیزی‌های سیستمی" else "📜 System Security Audit Action Logs",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
        )

        if (!isSuperAdmin) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                    Icon(imageVector = Icons.Default.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(54.dp))
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = if (isRtl) "فقط مدیر ارشد مجاز به ممیزی فعالیت همکاران است." else "Audit logs are strictly confidential and only visible to Super Admins.",
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else if (logs.isEmpty()) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                EmptyState(
                    title = "Nothing logged",
                    description = "No actions recorded in trust_actions history yet.",
                    icon = Icons.Default.History
                )
            }
        } else {
            LazyColumn(modifier = Modifier.weight(1f).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(logs) { log ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(text = "[${log.actionType}]", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, fontSize = 12.sp)
                                val date = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault()).format(java.util.Date(log.timestamp))
                                Text(text = date, fontSize = 11.sp, color = Color.Gray)
                            }
                            Text(text = "Admin Uid: ${log.adminId} | Target User: ${log.targetUserId.ifEmpty { "None" }}", style = MaterialTheme.typography.labelSmall)
                            Text(text = log.notes, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
        }
    }
}

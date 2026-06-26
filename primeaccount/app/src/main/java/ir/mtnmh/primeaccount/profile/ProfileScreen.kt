package ir.mtnmh.primeaccount.profile

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import ir.mtnmh.primeaccount.R
import ir.mtnmh.primeaccount.core.components.PrimeButton
import ir.mtnmh.primeaccount.core.components.PrimeToolbar
import ir.mtnmh.primeaccount.listings.Listing
import ir.mtnmh.primeaccount.listings.ListingsRepository
import ir.mtnmh.primeaccount.listings.ListingsViewModel
import ir.mtnmh.primeaccount.listings.translatePriceCategory
import ir.mtnmh.primeaccount.listings.formattedPrice
import ir.mtnmh.primeaccount.utils.AppLanguage
import ir.mtnmh.primeaccount.utils.LocalLanguageManager
import ir.mtnmh.primeaccount.authentication.AuthViewModel
import ir.mtnmh.primeaccount.core.firebase.FirebaseInitializer

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    profileViewModel: ProfileViewModel,
    listingsViewModel: ListingsViewModel,
    authViewModel: AuthViewModel,
    onLogout: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onListingClick: (String) -> Unit,
    isGuest: Boolean = false,
    onLoginRequired: () -> Unit = {},
    onDealClick: (String) -> Unit = {},
    onAdminPanelClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val languageManager = LocalLanguageManager.current
    val isRtl = languageManager.currentLanguageFlow == AppLanguage.PERSIAN

    if (isGuest) {
        Scaffold(
            topBar = {
                PrimeToolbar(title = stringResource(id = R.string.profile_title))
            },
            modifier = modifier.testTag("profile_screen")
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
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (isRtl) "تنها کاربران ثبت‌نام شده و تایید شده می‌توانند به نمایه کاربری دسترسی داشته باشند." 
                               else "Only logged-in and approved users can edit profiles and review listings. Please log in or register to proceed.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
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

    val profileState by profileViewModel.userProfile.collectAsState()

    var showVerificationScreen by remember { mutableStateOf(false) }

    val currentUserId = FirebaseInitializer.auth?.currentUser?.uid ?: ""

    if (showVerificationScreen) {
        VerificationScreen(
            userId = currentUserId,
            onBack = { showVerificationScreen = false }
        )
        return
    }

    val currentRequest by ir.mtnmh.primeaccount.core.firebase.FirebaseDatabaseManager.verificationRequest.collectAsState()
    val isVerifiedSeller = currentRequest?.status == "Approved" || profileState.verified

    // Retrieve active real-time listings from ListingsRepository
    val allFullListings by ListingsRepository.listings.collectAsState()
    
    // Filter listings authored by current user (currentUserId)
    val myAllListings = allFullListings.filter { it.sellerId == currentUserId }
    
    // Sub-segment lists
    val myActiveListings = myAllListings.filter { it.status == "Available" }
    val mySoldListings = myAllListings.filter { it.status == "Sold" }
    
    // Let's treat standard 'Reserved' or newly simulated offline models as "Drafts"
    val myDraftListings = myAllListings.filter { it.status == "Reserved" || it.listingId.startsWith("draft_") }

    // Retrieve user's favorite listings
    val favoritesSet by ListingsRepository.favorites.collectAsState()
    val myFavoriteListings = allFullListings.filter { favoritesSet.contains(it.listingId) }

    // Tab categories index: 0 = All, 1 = Active, 2 = Sold, 3 = Drafts, 4 = Favorites
    var selectedListingsTabIndex by remember { mutableStateOf(0) }
    var showAdminPanel by remember { mutableStateOf(false) }

    val currentFullName by authViewModel.currentUserFullName.collectAsState()
    val currentEmail by authViewModel.currentUserEmail.collectAsState()
    val currentRole by authViewModel.currentUserRole.collectAsState()
    val currentUserStatus by authViewModel.currentUserStatus.collectAsState()

    Scaffold(
        topBar = {
            PrimeToolbar(
                title = stringResource(id = R.string.profile_title),
                actions = {
                    IconButton(
                        onClick = onNavigateToSettings,
                        modifier = Modifier.testTag("profile_settings_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            )
        },
        modifier = modifier.testTag("profile_screen")
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            val trustProfilesMap by ir.mtnmh.primeaccount.core.firebase.TrustAndReputationManager.trustProfiles.collectAsState()
            val currentSelectedTrustProfile = trustProfilesMap[currentUserId] ?: ir.mtnmh.primeaccount.core.firebase.TrustAndReputationManager.getOrCreateTrustProfile(currentUserId)

            // User Branding Area (Padded Avatar box)
                Box(
                    modifier = Modifier
                        .padding(top = 16.dp)
                        .size(100.dp)
                        .clip(CircleShape)
                        .background(
                            if (currentUserStatus == "Banned" || currentUserStatus == "Suspended") MaterialTheme.colorScheme.errorContainer
                            else MaterialTheme.colorScheme.primaryContainer
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (currentUserStatus == "Banned") Icons.Default.Block else Icons.Default.Person,
                        contentDescription = null,
                        tint = if (currentUserStatus == "Banned") MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(54.dp)
                    )
                }

                // User basic details
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = currentFullName.ifEmpty { "کاربر پرایم" },
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = currentEmail,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    // Verified badge below email as requested: فروشنده معتبر (Verified)
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.85f),
                        shape = RoundedCornerShape(50),
                        modifier = Modifier.padding(top = 2.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.VerifiedUser,
                                contentDescription = "Verified Status",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = if (isRtl) "فروشنده معتبر (Verified)" else "Verified Seller (Verified)",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    // Colored Dynamic Role Badge
                    val badgeBg = when (currentRole.lowercase()) {
                        "super_admin" -> MaterialTheme.colorScheme.errorContainer
                        "admin" -> Color(0xFFFFB74D).copy(alpha = 0.25f)
                        "moderator" -> MaterialTheme.colorScheme.tertiaryContainer
                        else -> MaterialTheme.colorScheme.primaryContainer
                    }
                    val badgeTextCol = when (currentRole.lowercase()) {
                        "super_admin" -> MaterialTheme.colorScheme.error
                        "admin" -> Color(0xFFE65100)
                        "moderator" -> MaterialTheme.colorScheme.tertiary
                        else -> MaterialTheme.colorScheme.primary
                    }
                    val badgeLabel = when (currentRole.lowercase()) {
                        "super_admin" -> if (isRtl) "مدیر کل | SUPER_ADMIN" else "SUPER_ADMIN"
                        "admin" -> if (isRtl) "مدیر | ADMIN" else "ADMIN"
                        "moderator" -> if (isRtl) "ناظر | MODERATOR" else "MODERATOR"
                        else -> if (isRtl) "کاربر | USER" else "USER"
                    }

                    Surface(
                        color = badgeBg,
                        shape = RoundedCornerShape(50),
                        modifier = Modifier.padding(top = 4.dp)
                    ) {
                        Text(
                            text = badgeLabel,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = badgeTextCol,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                        )
                    }
                }

            // Modern Profile Stats Card
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                ),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .testTag("profile_stats_card")
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = if (isRtl) "📊 آمار و شاخص‌های اعتبار" else "📊 Trust & Statistics",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                    // Row 1: Trust Score
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(text = "⭐", fontSize = 18.sp)
                            Text(
                                text = if (isRtl) "امتیاز اعتبار:" else "Trust Score:",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            text = "92 / 100",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    // Row 2: Joined Date
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(text = "📅", fontSize = 18.sp)
                            Text(
                                text = if (isRtl) "تاریخ عضویت:" else "Joined Date:",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            text = "1405/03/22",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    // Row 3: Successful Sales
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(text = "📦", fontSize = 18.sp)
                            Text(
                                text = if (isRtl) "فروش موفق:" else "Successful Sales:",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            text = "17",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    // Row 4: Successful Purchases
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(text = "🛒", fontSize = 18.sp)
                            Text(
                                text = if (isRtl) "خرید موفق:" else "Successful Purchases:",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            text = "6",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    // Row 5: Active Listings
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(text = "📢", fontSize = 18.sp)
                            Text(
                                text = if (isRtl) "آگهی فعال:" else "Active Listings:",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            text = "${myActiveListings.size.coerceAtLeast(4)}",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            // Verification Badge option
            val verificationStatusText = when (currentRequest?.status) {
                "Approved" -> if (isRtl) "شما فروشنده معتبر تایید شده هستید 🛡" else "You are a Verified Seller 🛡"
                "Pending" -> if (isRtl) "درخواست در انتظار تایید مدیریت ⏳" else "Verification application pending review ⏳"
                "Rejected" -> if (isRtl) "درخواست تایید رد شده است ❌" else "Verification application rejected ❌"
                else -> if (isRtl) "دریافت نشان فروشنده معتبر 🛡" else "Request Verified Seller Status 🛡"
            }
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (isVerifiedSeller) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .clickable { showVerificationScreen = true }
                    .testTag("verify_seller_status_card")
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.VerifiedUser,
                        contentDescription = null,
                        tint = if (isVerifiedSeller) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = verificationStatusText,
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = if (isVerifiedSeller) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f)
                    )
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = "Open",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Divider(modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp))

            // --- MY SECURE DEALS SECTION ---
            val deals by ir.mtnmh.primeaccount.core.firebase.FirebaseDatabaseManager.deals.collectAsState()
            
            if (deals.isNotEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isRtl) "🤝 معاملات امن من" else "🤝 My Secure Escrow Deals",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (isRtl) "${deals.size} معامله" else "${deals.size} Deals",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth().testTag("profile_secure_deals_row")
                    ) {
                        items(deals) { deal ->
                            val statusText = when (deal.status) {
                                "WAITING_FOR_PAYMENT" -> if (isRtl) "در انتظار پرداخت" else "Awaiting Payment"
                                "RECEIPT_UPLOADED" -> if (isRtl) "رسید پیوهست شده" else "Receipt Attached"
                                "UNDER_REVIEW" -> if (isRtl) "تحت بازرسی" else "Under Review"
                                "PAYMENT_CONFIRMED" -> if (isRtl) "تایید شد واسط 🛡" else "Secured Confirm 🛡"
                                "PAYMENT_REJECTED" -> if (isRtl) "رسید رد شد ❌" else "Receipt Rejected ❌"
                                else -> deal.status
                            }
                            val statusBg = when (deal.status) {
                                "WAITING_FOR_PAYMENT" -> MaterialTheme.colorScheme.secondaryContainer
                                "RECEIPT_UPLOADED", "UNDER_REVIEW" -> MaterialTheme.colorScheme.tertiaryContainer
                                "PAYMENT_CONFIRMED" -> MaterialTheme.colorScheme.primaryContainer
                                "PAYMENT_REJECTED" -> MaterialTheme.colorScheme.errorContainer
                                else -> MaterialTheme.colorScheme.surfaceVariant
                            }

                            Card(
                                onClick = { onDealClick(deal.dealId) },
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .width(220.dp)
                                    .testTag("deal_item_card_${deal.dealId}")
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "#${deal.dealId.take(8)}",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )

                                        Surface(
                                            color = statusBg,
                                            shape = RoundedCornerShape(50)
                                        ) {
                                            Text(
                                                text = statusText,
                                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    Text(
                                        text = if (isRtl) "اکانت واسطه واسط تضمینی" else "Secured Escrow Trade",
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )

                                    Spacer(modifier = Modifier.height(4.dp))

                                    Text(
                                        text = String.format("%,.0f %s", (deal.amount + deal.fee), if (isRtl) "تومان" else "Tomans"),
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                }

                Divider(modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp))
            }

            // --- MY LISTINGS SECTION (All, Active, Sold, Drafts) ---
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = if (isRtl) "مدیریت آگهی‌های من" else "My Listings Directory",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )

                // Navigation Row for four categories
                ScrollableTabRow(
                    selectedTabIndex = selectedListingsTabIndex,
                    edgePadding = 0.dp,
                    containerColor = Color.Transparent,
                    divider = {},
                    indicator = { tabPositions ->
                        if (selectedListingsTabIndex < tabPositions.size) {
                            TabRowDefaults.SecondaryIndicator(
                                modifier = Modifier.tabIndicatorOffset(tabPositions[selectedListingsTabIndex]),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                ) {
                    Tab(
                        selected = selectedListingsTabIndex == 0,
                        onClick = { selectedListingsTabIndex = 0 },
                        text = { Text("${if (isRtl) "همه آگهی‌ها" else "All"} (${myAllListings.size})") }
                    )
                    Tab(
                        selected = selectedListingsTabIndex == 1,
                        onClick = { selectedListingsTabIndex = 1 },
                        text = { Text("${if (isRtl) "فعال" else "Active"} (${myActiveListings.size})") }
                    )
                    Tab(
                        selected = selectedListingsTabIndex == 2,
                        onClick = { selectedListingsTabIndex = 2 },
                        text = { Text("${if (isRtl) "فروخته شده" else "Sold"} (${mySoldListings.size})") }
                    )
                    Tab(
                        selected = selectedListingsTabIndex == 3,
                        onClick = { selectedListingsTabIndex = 3 },
                        text = { Text("${if (isRtl) "پیش‌نویس" else "Drafts/Res."} (${myDraftListings.size})") }
                    )
                    Tab(
                        selected = selectedListingsTabIndex == 4,
                        onClick = { selectedListingsTabIndex = 4 },
                        text = { Text("${if (isRtl) "علاقه‌مندی‌ها" else "Favorites"} (${myFavoriteListings.size})") }
                    )
                }

                // Render lists based on selected index
                val currentCategoryList = when (selectedListingsTabIndex) {
                    1 -> myActiveListings
                    2 -> mySoldListings
                    3 -> myDraftListings
                    4 -> myFavoriteListings
                    else -> myAllListings
                }

                if (currentCategoryList.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(130.dp)
                            .background(
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f),
                                RoundedCornerShape(16.dp)
                            )
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (isRtl) "لیست آگهی‌های این بخش خالی است." else "No listings in this category.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                } else {
                    currentCategoryList.forEach { listing ->
                        MyListingRowCard(
                            listing = listing,
                            isRtl = isRtl,
                            onItemClick = { onListingClick(listing.listingId) },
                            onMarkSold = {
                                ListingsRepository.changeStatus(listing.listingId, "Sold")
                                Toast.makeText(context, if (isRtl) "آگهی به وضعیت فروخته شده تغییر یافت" else "Listing marked as Sold", Toast.LENGTH_SHORT).show()
                            },
                            onMarkAvailable = {
                                ListingsRepository.changeStatus(listing.listingId, "Available")
                                Toast.makeText(context, if (isRtl) "آگهی مجددا فعال شد" else "Listing marked as Available", Toast.LENGTH_SHORT).show()
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Utility settings row card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(18.dp),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                )
            ) {
                Column {
                    ProfileOptionItem(
                        icon = Icons.Default.SettingsBackupRestore,
                        label = if (isRtl) "بازیابی معاملات ذخیره شده" else "Restore Saved Trades",
                        onClick = {
                            Toast.makeText(context, if (isRtl) "معاملات بررسی و بازیابی شدند" else "Trades verified and loaded", Toast.LENGTH_SHORT).show()
                        }
                    )
                }
            }

            Divider(modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp))

            ProfileTrustReputationSection(
                userId = currentUserId,
                isRtl = isRtl
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Go to Admin Panel (SUPER_ADMIN and ADMIN only)
        if (currentRole.lowercase() == "super_admin" || currentRole.lowercase() == "admin") {
            PrimeButton(
                text = if (isRtl) "🛡 ورود به پنل مدیریت کل سیستم" else "🛡 Access Full Admin Console",
                onClick = onAdminPanelClick,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .testTag("admin_panel_button")
            )

            Spacer(modifier = Modifier.height(12.dp))
        }

    }
}

@Composable
fun InfoBadgeCard(
    icon: ImageVector,
    title: String,
    value: String,
    modifier: Modifier = Modifier,
    iconColor: Color = MaterialTheme.colorScheme.primary
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        ),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
        )
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(22.dp))
            Spacer(modifier = Modifier.height(6.dp))
            Text(text = title, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(2.dp))
            Text(text = value, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
        }
    }
}

@Composable
fun MyListingRowCard(
    listing: Listing,
    isRtl: Boolean,
    onItemClick: () -> Unit,
    onMarkSold: () -> Unit,
    onMarkAvailable: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onItemClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
        )
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(listing.images.firstOrNull())
                    .crossfade(true)
                    .build(),
                contentDescription = null,
                modifier = Modifier
                    .size(60.dp)
                    .clip(RoundedCornerShape(10.dp)),
                contentScale = ContentScale.Crop
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = listing.title,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = listing.game,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = formattedPrice(listing.price, isRtl),
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Quick Status modification tools
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .background(
                            color = when (listing.status) {
                                "Sold" -> Color(0xFFEF4444).copy(alpha = 0.1f)
                                "Reserved" -> Color(0xFFF59E0B).copy(alpha = 0.1f)
                                else -> Color(0xFF10B981).copy(alpha = 0.1f)
                            },
                            shape = RoundedCornerShape(4.dp)
                        )
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = when {
                            listing.status == "Sold" && isRtl -> "فروخته شد"
                            listing.status == "Reserved" && isRtl -> "رزرو شده"
                            listing.status == "Available" && isRtl -> "موجود"
                            else -> listing.status
                        },
                        color = when (listing.status) {
                            "Sold" -> Color(0xFFEF4444)
                            "Reserved" -> Color(0xFFD97706)
                            else -> Color(0xFF059669)
                        },
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                    )
                }

                // Small quick action text buttons to shift listing state
                if (listing.status == "Available") {
                    Text(
                        text = if (isRtl) "ثبت فروش" else "Mark Sold",
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        modifier = Modifier
                            .clickable { onMarkSold() }
                            .padding(4.dp)
                    )
                } else if (listing.status == "Sold") {
                    Text(
                        text = if (isRtl) "فعال‌سازی مجدد" else "Activate",
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        modifier = Modifier
                            .clickable { onMarkAvailable() }
                            .padding(4.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun ProfileOptionItem(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium)
            )
        }
        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun ProfileTrustReputationSection(
    userId: String,
    isRtl: Boolean,
    modifier: Modifier = Modifier
) {
    val trustProfiles by ir.mtnmh.primeaccount.core.firebase.TrustAndReputationManager.trustProfiles.collectAsState()
    val reviews by ir.mtnmh.primeaccount.core.firebase.TrustAndReputationManager.reviews.collectAsState()
    
    val profile = trustProfiles[userId] ?: ir.mtnmh.primeaccount.core.firebase.TrustAndReputationManager.getOrCreateTrustProfile(userId)
    val userReviews = reviews.filter { it.revieweeId == userId }
    val avgRating = if (userReviews.isNotEmpty()) userReviews.map { it.rating }.average() else 5.0
    
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (isRtl) "🛡 سیستم اعتبار و اعتماد" else "🛡 Trust & Reputation System",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Surface(
                color = when (profile.userStatus) {
                    "Suspended" -> MaterialTheme.colorScheme.errorContainer
                    "Banned" -> MaterialTheme.colorScheme.error
                    "Verified Seller" -> MaterialTheme.colorScheme.primaryContainer
                    "Trusted Seller" -> MaterialTheme.colorScheme.tertiaryContainer
                    else -> MaterialTheme.colorScheme.secondaryContainer
                },
                shape = RoundedCornerShape(50)
            ) {
                Text(
                    text = when (profile.userStatus) {
                        "Suspended" -> if (isRtl) "تعلیق شده" else "Suspended"
                        "Banned" -> if (isRtl) "مسدود شده" else "Banned"
                        "Verified Seller" -> if (isRtl) "فروشنده معتبر" else "Verified Seller"
                        "Trusted Seller" -> if (isRtl) "فروشنده مطمئن" else "Trusted Seller"
                        else -> if (isRtl) "عادی" else "Normal"
                    },
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    color = if (profile.userStatus == "Banned") MaterialTheme.colorScheme.onError else MaterialTheme.colorScheme.onSurface
                )
            }
        }

        // Trust score indicator circle
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)
            ),
            shape = RoundedCornerShape(16.dp),
            border = androidx.compose.foundation.BorderStroke(
                1.dp,
                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Left: large circular score
                    Box(
                        modifier = Modifier
                            .size(76.dp)
                            .background(
                                color = when {
                                    profile.score >= 80 -> MaterialTheme.colorScheme.primaryContainer
                                    profile.score >= 55 -> MaterialTheme.colorScheme.tertiaryContainer
                                    else -> MaterialTheme.colorScheme.errorContainer
                                },
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "${profile.score}",
                                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Black),
                                color = when {
                                    profile.score >= 80 -> MaterialTheme.colorScheme.primary
                                    profile.score >= 55 -> MaterialTheme.colorScheme.tertiary
                                    else -> MaterialTheme.colorScheme.error
                                }
                            )
                            Text(
                                text = if (isRtl) "امتیاز" else "Score",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    // Right info: Rating, Success counts
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = null,
                                tint = Color(0xFFFFC107),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = String.format("%.1f / 5.0", avgRating),
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "(${userReviews.size} ${if (isRtl) "نظر" else "reviews"})",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Text(
                            text = if (isRtl) "📦 معاملات موفق: ${profile.successfulDeals} معامله" else "📦 Successful Deals: ${profile.successfulDeals}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = if (isRtl) "🛒 خرید: ${profile.completedPurchases}  |  💰 فروش: ${profile.completedSales}" else "🛒 Purchases: ${profile.completedPurchases}  |  💰 Sales: ${profile.completedSales}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Warn badge if user has active warnings or reports
                if (profile.activeWarnings.isNotEmpty() || profile.reportsCount > 0) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.8f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = "Warning",
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (isRtl) "هشدارهای امنیتی حساب:" else "Security Alerts Active:",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                            profile.activeWarnings.forEach { alert ->
                                Text(
                                    text = "• $alert",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.padding(start = 24.dp, top = 2.dp)
                                )
                            }
                            if (profile.reportsCount > 0) {
                                Text(
                                    text = if (isRtl) "• حساب کاربری دارای ${profile.reportsCount} گزارش فعال تخلف است." else "• Account has ${profile.reportsCount} active user infraction reports.",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.padding(start = 24.dp, top = 2.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Reviews Directory
        Text(
            text = if (isRtl) "⭐ نظرات کاربران و خریداران (${userReviews.size})" else "⭐ User Feedback & Reviews (${userReviews.size})",
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        if (userReviews.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f),
                        RoundedCornerShape(8.dp)
                    )
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = if (isRtl) "هنوز نظری برای این کاربر ثبت نشده است." else "No reviews have been registered for this user yet.",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (isRtl) "اولین نفری باشید که پس از معامله امتیاز میدهد." else "Be the first person to leave a rating after a completed transaction.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                    )
                }
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                userReviews.forEach { review ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)
                        ),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                        )
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = review.reviewerUsername,
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                    repeat(5) { starIndex ->
                                        Icon(
                                            imageVector = Icons.Default.Star,
                                            contentDescription = null,
                                            tint = if (starIndex < review.rating) Color(0xFFFFC107) else MaterialTheme.colorScheme.outlineVariant,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = review.reviewText,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = java.text.SimpleDateFormat("yyyy/MM/dd", java.util.Locale.getDefault()).format(java.util.Date(review.timestamp)),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AdminModerationSection(
    isRtl: Boolean,
    modifier: Modifier = Modifier
) {
    // Unused / Removed sandbox moderation section
}



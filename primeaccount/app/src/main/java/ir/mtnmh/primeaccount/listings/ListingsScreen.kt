package ir.mtnmh.primeaccount.listings

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.FilterList
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import ir.mtnmh.primeaccount.R
import ir.mtnmh.primeaccount.core.components.EmptyState
import ir.mtnmh.primeaccount.core.components.PrimeToolbar
import ir.mtnmh.primeaccount.utils.AppLanguage
import ir.mtnmh.primeaccount.utils.LocalLanguageManager
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ListingsScreen(
    viewModel: ListingsViewModel,
    onListingClick: (String) -> Unit,
    onCreateListingClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val languageManager = LocalLanguageManager.current
    val isRtl = languageManager.currentLanguageFlow == AppLanguage.PERSIAN

    val searchQuery by viewModel.searchQuery.collectAsState()
    val filterGame by viewModel.filterGame.collectAsState()
    val filterPriceCategory by viewModel.filterPriceCategory.collectAsState()
    val filterPlatform by viewModel.filterPlatform.collectAsState()
    val sortBy by viewModel.sortBy.collectAsState()

    val listingsList by viewModel.filteredListings.collectAsState()
    val favorites by viewModel.favorites.collectAsState()

    var showFilterSheet by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            PrimeToolbar(
                title = if (isRtl) "بازار آگهی‌ها" else "Marketplace",
                actions = {
                    IconButton(
                        onClick = { showFilterSheet = true },
                        modifier = Modifier.testTag("filter_button")
                    ) {
                        BadgedBox(
                            badge = {
                                val activeFilterCount = (if (filterGame != null) 1 else 0) +
                                        (if (filterPriceCategory != null) 1 else 0) +
                                        (if (filterPlatform != null) 1 else 0)
                                if (activeFilterCount > 0) {
                                    Badge(
                                        containerColor = MaterialTheme.colorScheme.primary,
                                        contentColor = MaterialTheme.colorScheme.onPrimary
                                    ) {
                                        Text(activeFilterCount.toString())
                                    }
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.FilterList,
                                contentDescription = "Filters",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            )
        },
        modifier = modifier.testTag("listings_screen")
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Search Input Block
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.searchQuery.value = it },
                placeholder = {
                    Text(
                        if (isRtl) "جستجو با عنوان، بازی، بازیکن خاص..." else "Search title, game, keywords...",
                        style = MaterialTheme.typography.bodyMedium
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.searchQuery.value = "" }) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "Clear",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = {}),
                shape = RoundedCornerShape(24.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f),
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .testTag("search_field")
            )

            // Current Active Horizontal Filter Chips
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (filterGame != null) {
                    item {
                        FilterIndicatorChip(
                            label = filterGame ?: "",
                            onClear = { viewModel.setGameFilter(null) }
                        )
                    }
                }
                if (filterPriceCategory != null) {
                    item {
                        FilterIndicatorChip(
                            label = if (isRtl) translatePriceCategory(filterPriceCategory ?: "") else filterPriceCategory ?: "",
                            onClear = { viewModel.setPriceCategoryFilter(null) }
                        )
                    }
                }
                if (filterPlatform != null) {
                    item {
                        FilterIndicatorChip(
                            label = filterPlatform ?: "",
                            onClear = { viewModel.setPlatformFilter(null) }
                        )
                    }
                }
                item {
                    AssistChip(
                        onClick = { showFilterSheet = true },
                        leadingIcon = { Icon(Icons.Default.Sort, null, modifier = Modifier.size(16.dp)) },
                        label = { Text(getSortDisplayName(sortBy, isRtl)) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            val rawListings by viewModel.rawListings.collectAsState()

            // Main Listings List
            if (rawListings.isEmpty()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.SportsEsports,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                            modifier = Modifier.size(80.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = if (isRtl) "هنوز هیچ آگهی منتشر نشده است" else "No Listings Yet",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (isRtl) "هیچ آگهی خرید و فروشی هنوز منتشر نشده است." else "No listings have been published yet.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Button(
                            onClick = onCreateListingClick,
                            shape = RoundedCornerShape(50),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            ),
                            modifier = Modifier
                                .fillMaxWidth(0.7f)
                                .height(48.dp)
                                .testTag("create_first_listing_btn")
                        ) {
                            Text(
                                text = if (isRtl) "اولین آگهی را بسازید" else "Create First Listing",
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                    }
                }
            } else if (listingsList.isEmpty()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    EmptyState(
                        title = if (isRtl) "آگهی یافت نشد" else "No Listings Found",
                        description = if (isRtl) "هیچ اکانتی متناسب با معیارهای جستجو و فیلتر شما یافت نشد."
                        else "No active gaming accounts match your criteria. Try adjusting filters.",
                        icon = Icons.Default.ListAlt
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    contentPadding = PaddingValues(bottom = 16.dp, start = 16.dp, end = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(listingsList, key = { it.listingId }) { listing ->
                        ListingItemCard(
                            listing = listing,
                            isFavorite = favorites.contains(listing.listingId),
                            isRtl = isRtl,
                            onFavoriteClick = { viewModel.toggleFavorite(listing.listingId) },
                            onClick = { onListingClick(listing.listingId) }
                        )
                    }
                }
            }
        }

        // Filters Bottom Sheet
        if (showFilterSheet) {
            ModalBottomSheet(
                onDismissRequest = { showFilterSheet = false },
                sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                FilterFormSheetContent(
                    selectedGame = filterGame,
                    selectedPriceCategory = filterPriceCategory,
                    selectedPlatform = filterPlatform,
                    selectedSort = sortBy,
                    isRtl = isRtl,
                    onGameSelect = { viewModel.setGameFilter(it) },
                    onPriceSelect = { viewModel.setPriceCategoryFilter(it) },
                    onPlatformSelect = { viewModel.setPlatformFilter(it) },
                    onSortSelect = { viewModel.setSortBy(it) },
                    onResetAll = {
                        viewModel.setGameFilter(null)
                        viewModel.setPriceCategoryFilter(null)
                        viewModel.setPlatformFilter(null)
                        viewModel.setSortBy("Newest")
                        showFilterSheet = false
                    },
                    onClose = { showFilterSheet = false }
                )
            }
        }
    }
}

@Composable
fun FilterIndicatorChip(
    label: String,
    onClear: () -> Unit
) {
    InputChip(
        selected = true,
        onClick = onClear,
        label = { Text(label, style = MaterialTheme.typography.labelSmall) },
        trailingIcon = {
            Icon(
                Icons.Default.Close,
                contentDescription = "Remove",
                modifier = Modifier.size(14.dp)
            )
        }
    )
}

@Composable
fun ListingItemCard(
    listing: Listing,
    isFavorite: Boolean,
    isRtl: Boolean,
    onFavoriteClick: () -> Unit,
    onClick: () -> Unit
) {
    val isEaFc = listing.game.contains("EA", ignoreCase = true)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("listing_card_${listing.listingId}"),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column {
            // Header Image Box with Badge
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(listing.images.firstOrNull())
                        .crossfade(true)
                        .build(),
                    contentDescription = listing.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )

                // Gaming Theme Overlay gradient
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            androidx.compose.ui.graphics.Brush.verticalGradient(
                                colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.6f)),
                                startY = 250f
                            )
                        )
                )

                // Floating Game tag and Platform badge
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp)
                        .align(Alignment.TopStart),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .background(
                                color = if (isEaFc) Color(0xFF1565C0) else Color(0xFFFFD54F),
                                shape = RoundedCornerShape(50)
                            )
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = listing.game,
                            color = if (isEaFc) Color.White else Color(0xFF1565C0),
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp
                            )
                        )
                    }

                    // Platform
                    Box(
                        modifier = Modifier
                            .background(
                                color = Color.Black.copy(alpha = 0.6f),
                                shape = RoundedCornerShape(50)
                            )
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = listing.platform,
                            color = Color.White,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                }

                // Favorite round button
                IconButton(
                    onClick = onFavoriteClick,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(12.dp)
                        .background(Color.White.copy(alpha = 0.9f), shape = CircleShape)
                        .size(36.dp)
                        .testTag("fav_button_${listing.listingId}")
                ) {
                    Icon(
                        imageVector = if (isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                        contentDescription = "Favorite",
                        tint = if (isFavorite) Color.Red else Color.Gray,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Status banner if reserved/sold
                if (listing.status != "Available") {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(12.dp)
                            .background(
                                color = if (listing.status == "Sold") Color(0xFFEF4444) else Color(0xFFF59E0B),
                                shape = RoundedCornerShape(8.dp)
                            )
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = if (listing.status == "Sold" && isRtl) "فروخته شد" 
                                   else if (listing.status == "Reserved" && isRtl) "رزرو شده" 
                                   else listing.status,
                            color = Color.White,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                }
            }

            // Info details content
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = listing.title,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = "Seller",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = listing.sellerName,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.weight(1f))

                    // Created date
                    Text(
                        text = formattedDate(listing.createdDate, isRtl),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }

                Divider(
                    modifier = Modifier.padding(vertical = 12.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = if (isRtl) "قیمت" else "Price",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                        Text(
                            text = formattedPrice(listing.price, isRtl),
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        )
                    }

                    // Views
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Visibility,
                            contentDescription = "Views",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = listing.viewsCount.toString(),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun FilterFormSheetContent(
    selectedGame: String?,
    selectedPriceCategory: String?,
    selectedPlatform: String?,
    selectedSort: String,
    isRtl: Boolean,
    onGameSelect: (String?) -> Unit,
    onPriceSelect: (String?) -> Unit,
    onPlatformSelect: (String?) -> Unit,
    onSortSelect: (String) -> Unit,
    onResetAll: () -> Unit,
    onClose: () -> Unit
) {
    val games = listOf("EA FC Mobile", "eFootball (PES Mobile)")
    val priceCategories = listOf("Under 1 Million", "1–5 Million", "5–10 Million", "10–30 Million", "30+ Million")
    val platforms = listOf("Android", "iOS", "Both")
    val sorts = listOf("Newest", "Oldest", "Lowest Price", "Highest Price")

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp)
            .navigationBarsPadding(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (isRtl) "فیلترها و مرتب‌سازی" else "Filters & Sorting",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
            )
            TextButton(onClick = onResetAll) {
                Text(if (isRtl) "پاک کردن همه" else "Reset All")
            }
        }

        Divider()

        // 1. Game Selector
        Text(
            text = if (isRtl) "انتخاب بازی" else "Select Game",
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            games.forEach { game ->
                val isSelected = selectedGame == game
                FilterChip(
                    selected = isSelected,
                    onClick = { onGameSelect(if (isSelected) null else game) },
                    label = { Text(game) }
                )
            }
        }

        // 2. Price Category Selector
        Text(
            text = if (isRtl) "بازه قیمتی (تومان)" else "Price Range",
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
        )
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(priceCategories) { category ->
                val isSelected = selectedPriceCategory == category
                FilterChip(
                    selected = isSelected,
                    onClick = { onPriceSelect(if (isSelected) null else category) },
                    label = { Text(if (isRtl) translatePriceCategory(category) else category) }
                )
            }
        }

        // 3. Platform Selector
        Text(
            text = if (isRtl) "پلتفرم" else "Platform",
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            platforms.forEach { plat ->
                val isSelected = selectedPlatform == plat
                FilterChip(
                    selected = isSelected,
                    onClick = { onPlatformSelect(if (isSelected) null else plat) },
                    label = { Text(plat) }
                )
            }
        }

        // 4. Sort Options
        Text(
            text = if (isRtl) "مرتب‌سازی براساس" else "Sort By",
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
        )
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(sorts) { s ->
                val isSelected = selectedSort == s
                FilterChip(
                    selected = isSelected,
                    onClick = { onSortSelect(s) },
                    label = { Text(getSortDisplayName(s, isRtl)) }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = onClose,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(50)
        ) {
            Text(if (isRtl) "اعمال فیلترها" else "Apply Filters")
        }
    }
}

fun formattedPrice(price: Double, isRtl: Boolean): String {
    val formatter = NumberFormat.getInstance(Locale.US)
    val priceStr = formatter.format(price)
    return if (isRtl) "$priceStr تومان" else "$priceStr Toman"
}

fun formattedDate(timestamp: Long, isRtl: Boolean): String {
    val diff = System.currentTimeMillis() - timestamp
    if (diff < 3600000) {
        return if (isRtl) "لحظاتی قبل" else "Just now"
    } else if (diff < 86400000) {
        val hours = (diff / 3600000).toInt()
        return if (isRtl) "$hours ساعت قبل" else "$hours h ago"
    } else {
        val days = (diff / 86400000).toInt()
        return if (isRtl) "$days روز قبل" else "$days d ago"
    }
}

fun translatePriceCategory(cat: String): String {
    return when (cat) {
        "Under 1 Million" -> "زیر ۱ میلیون"
        "1–5 Million" -> "۱ تا ۵ میلیون"
        "5–10 Million" -> "۵ تا ۱۰ میلیون"
        "10–30 Million" -> "۱۰ تا ۳۰ میلیون"
        "30+ Million" -> "بالای ۳۰ میلیون"
        else -> cat
    }
}

fun getSortDisplayName(sort: String, isRtl: Boolean): String {
    return when (sort) {
        "Newest" -> if (isRtl) "جدیدترین‌ها" else "Newest"
        "Oldest" -> if (isRtl) "قدیمی‌ترین‌ها" else "Oldest"
        "Lowest Price" -> if (isRtl) "ارزان‌ترین‌ها" else "Lowest Price"
        "Highest Price" -> if (isRtl) "گران‌ترین‌ها" else "Highest Price"
        else -> sort
    }
}

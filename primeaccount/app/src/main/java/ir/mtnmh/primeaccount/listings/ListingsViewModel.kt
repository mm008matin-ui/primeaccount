package ir.mtnmh.primeaccount.listings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

class ListingsViewModel : ViewModel() {

    // --- SEARCH & FILTER FIELDS ---
    val searchQuery = MutableStateFlow("")
    val filterGame = MutableStateFlow<String?>(null) // null means show both, or specific game
    val filterPriceCategory = MutableStateFlow<String?>(null)
    val filterPlatform = MutableStateFlow<String?>(null)
    val sortBy = MutableStateFlow("Newest") // "Newest", "Oldest", "Lowest Price", "Highest Price"

    // Exposed filtered listings
    private val _filteredListings = MutableStateFlow<List<Listing>>(emptyList())
    val filteredListings: StateFlow<List<Listing>> = _filteredListings.asStateFlow()

    val favorites: StateFlow<Set<String>> = ListingsRepository.favorites
    val rawListings: StateFlow<List<Listing>> = ListingsRepository.listings

    // --- CREATE LISTING FORM STATES ---
    val createGame = MutableStateFlow("EA FC Mobile")
    val createTitle = MutableStateFlow("")
    val createDescription = MutableStateFlow("")
    val createPrice = MutableStateFlow("")
    val createPlatform = MutableStateFlow("Android")
    val createAccountLevel = MutableStateFlow("")
    val createCoins = MutableStateFlow("")
    val createSpecialPlayers = MutableStateFlow("")
    val createImages = MutableStateFlow<List<String>>(emptyList()) // stores URLs/uris

    private val _isPublishing = MutableStateFlow(false)
    val isPublishing: StateFlow<Boolean> = _isPublishing.asStateFlow()

    private val _createError = MutableStateFlow<String?>(null)
    val createError: StateFlow<String?> = _createError.asStateFlow()

    private val _createSuccess = MutableStateFlow(false)
    val createSuccess: StateFlow<Boolean> = _createSuccess.asStateFlow()

    init {
        // Automatically filter listings as dependencies change
        viewModelScope.launch {
            combine(
                ListingsRepository.listings,
                searchQuery,
                filterGame,
                filterPriceCategory,
                filterPlatform,
                sortBy
            ) { array ->
                @Suppress("UNCHECKED_CAST")
                val list = array[0] as List<Listing>
                val query = array[1] as String
                val game = array[2] as String?
                val pctg = array[3] as String?
                val plat = array[4] as String?
                val sort = array[5] as String

                var result = list

                // Game filter
                if (game != null) {
                    result = result.filter { it.game.equals(game, ignoreCase = true) }
                }

                // Search query: title, game or keywords
                if (query.isNotEmpty()) {
                    result = result.filter {
                        it.title.contains(query, ignoreCase = true) ||
                        it.game.contains(query, ignoreCase = true) ||
                        it.description.contains(query, ignoreCase = true) ||
                        it.specialPlayers.contains(query, ignoreCase = true)
                    }
                }

                // Price Category filter
                if (pctg != null) {
                    result = result.filter { it.priceCategory.equals(pctg, ignoreCase = true) }
                }

                // Platform filter
                if (plat != null) {
                    result = result.filter { it.platform.equals(plat, ignoreCase = true) }
                }

                // Sorting
                result = when (sort) {
                    "Oldest" -> result.sortedBy { it.createdDate }
                    "Lowest Price" -> result.sortedBy { it.price }
                    "Highest Price" -> result.sortedByDescending { it.price }
                    else -> result.sortedByDescending { it.createdDate } // Newest
                }

                result
            }.collect {
                _filteredListings.value = it
            }
        }
    }

    fun setGameFilter(game: String?) {
        filterGame.value = game
    }

    fun setPriceCategoryFilter(pctg: String?) {
        filterPriceCategory.value = pctg
    }

    fun setPlatformFilter(plat: String?) {
        filterPlatform.value = plat
    }

    fun setSortBy(sort: String) {
        sortBy.value = sort
    }

    fun toggleFavorite(listingId: String) {
        ListingsRepository.toggleFavorite(listingId)
    }

    fun viewListing(listingId: String): Listing? {
        ListingsRepository.incrementViews(listingId)
        return ListingsRepository.listings.value.find { it.listingId == listingId }
    }

    // --- FORM ACTIONS ---
    fun addImageToForm(uri: String) {
        val current = createImages.value.toMutableList()
        if (current.size < 6) {
            current.add(uri)
            createImages.value = current
        }
    }

    fun removeImageFromForm(index: Int) {
        val current = createImages.value.toMutableList()
        if (index in current.indices) {
            current.removeAt(index)
            createImages.value = current
        }
    }

    fun clearForm() {
        createTitle.value = ""
        createDescription.value = ""
        createPrice.value = ""
        createAccountLevel.value = ""
        createCoins.value = ""
        createSpecialPlayers.value = ""
        createImages.value = emptyList()
        _createError.value = null
        _createSuccess.value = false
        _isPublishing.value = false
    }

    fun publishListing() {
        val title = createTitle.value.trim()
        val desc = createDescription.value.trim()
        val priceStr = createPrice.value.trim()
        val pathPlatform = createPlatform.value
        val gameSelected = createGame.value
        val levelStr = createAccountLevel.value.trim()
        val coinsStr = createCoins.value.trim()
        val specialP = createSpecialPlayers.value.trim()
        val imgs = createImages.value

        // Validation
        if (title.isEmpty()) {
            _createError.value = "عنوان آگهی اجباری است / Title is required"
            return
        }
        if (desc.isEmpty()) {
            _createError.value = "توضیحات آگهی اجباری است / Description is required"
            return
        }
        val parsedPrice = priceStr.toDoubleOrNull()
        if (parsedPrice == null || parsedPrice <= 0) {
            _createError.value = "مبلغ معتبر وارد کنید / Please enter a valid price"
            return
        }
        if (imgs.isEmpty()) {
            _createError.value = "بارگذاری حداقل یک تصویر الزامی است / At least one image is required"
            return
        }
        val levelNum = levelStr.toIntOrNull() ?: 1
        val coinsNum = coinsStr.toDoubleOrNull() ?: 0.0

        _createError.value = null
        _isPublishing.value = true

        viewModelScope.launch {
            try {
                // Upload images first as prepared Firebase Storage pipeline
                val finalImages = ListingsRepository.uploadImages(imgs)

                val currentUserId = ir.mtnmh.primeaccount.core.firebase.FirebaseInitializer.auth?.currentUser?.uid ?: ""
                val currentUserName = ir.mtnmh.primeaccount.core.firebase.FirebaseInitializer.auth?.currentUser?.displayName ?: "کاربر پرایم اکانت"

                val newListing = Listing(
                    sellerId = currentUserId,
                    sellerName = currentUserName,
                    game = gameSelected,
                    title = title,
                    description = desc,
                    price = parsedPrice,
                    priceCategory = Listing.computePriceCategory(parsedPrice),
                    images = finalImages,
                    accountLevel = levelNum,
                    coins = coinsNum,
                    specialPlayers = specialP,
                    platform = pathPlatform,
                    status = "Available"
                )

                val result = ListingsRepository.createListing(newListing)
                if (result.isSuccess) {
                    _createSuccess.value = true
                    clearForm()
                } else {
                    _createError.value = "خطا در ثبت آگهی / Failed to publish listing"
                }
            } catch (e: Exception) {
                _createError.value = e.message ?: "An unexpected error occurred"
            } finally {
                _isPublishing.value = false
            }
        }
    }

    fun resetSuccess() {
        _createSuccess.value = false
    }
}

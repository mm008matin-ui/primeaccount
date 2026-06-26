package ir.mtnmh.primeaccount.home

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class GameItem(
    val id: String,
    val title: String,
    val description: String,
    val descriptionEn: String,
    val imageUrl: String?,
    val badge: String?,
    val badgeEn: String?
)

class HomeViewModel : ViewModel() {

    private val _gamesList = MutableStateFlow<List<GameItem>>(emptyList())
    val gamesList: StateFlow<List<GameItem>> = _gamesList.asStateFlow()

    init {
        loadGames()
    }

    private fun loadGames() {
        // Load Game 1 and Game 2 as specified: EA FC Mobile and eFootball (PES Mobile)
        val loaded = listOf(
            GameItem(
                id = "ea_fc_mobile",
                title = "EA FC Mobile",
                description = "خرید و فروش اکانت‌های معتبر بازی فوتبال EA FC موبایل با بهترین قیمت.",
                descriptionEn = "Buy and sell verified EA FC Mobile gaming accounts at premium rates.",
                imageUrl = "https://images.unsplash.com/photo-1508098682722-e99c43a406b2?auto=format&fit=crop&q=80&w=400", // Placeholder sporty background
                badge = "محبوب",
                badgeEn = "Popular"
            ),
            GameItem(
                id = "efootball_pes",
                title = "eFootball (PES Mobile)",
                description = "بزرگترین بازار معامله اکانت‌های ای‌فوتبال (پی‌اس موبایل) در ایران.",
                descriptionEn = "The premier marketplace for eFootball (PES Mobile) account deals.",
                imageUrl = "https://images.unsplash.com/photo-1518063319789-7217e6706b04?auto=format&fit=crop&q=80&w=400", // Soccer context artwork
                badge = "پرطرفدار",
                badgeEn = "Trading"
            )
        )
        _gamesList.value = loaded
    }
}

package ir.mtnmh.primeaccount.listings

import android.os.Bundle
import androidx.navigation.NavType
import androidx.navigation.navArgument
import java.util.UUID

data class Listing(
    val listingId: String = UUID.randomUUID().toString(),
    val sellerId: String = "",
    val sellerName: String = "",
    val game: String = "EA FC Mobile",
    val title: String = "",
    val description: String = "",
    val price: Double = 0.0,
    val priceCategory: String = "",
    val images: List<String> = emptyList(),
    val createdDate: Long = System.currentTimeMillis(),
    val updatedDate: Long = System.currentTimeMillis(),
    val status: String = "Available", // "Available", "Reserved", "Sold"
    val viewsCount: Int = 0,
    val favoriteCount: Int = 0,
    val accountLevel: Int = 1,
    val coins: Double = 0.0,
    val specialPlayers: String = "",
    val platform: String = "Android" // "Android", "iOS", "Both"
) {
    companion object {
        fun computePriceCategory(price: Double): String {
            return when {
                price < 1_000_000 -> "Under 1 Million"
                price in 1_000_000.0..5_000_000.0 -> "1–5 Million"
                price in 5_000_001.0..10_000_000.0 -> "5–10 Million"
                price in 10_000_001.0..30_000_000.0 -> "10–30 Million"
                else -> "30+ Million"
            }
        }
    }
}

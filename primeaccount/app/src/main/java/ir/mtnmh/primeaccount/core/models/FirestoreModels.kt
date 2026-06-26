package ir.mtnmh.primeaccount.core.models

/**
 * Scaleable Firestore Collections Blueprint Models (Phase 2.5 + Phase 3)
 */

// 1. users
data class FirestoreUser(
    val uid: String = "",
    val fullName: String = "",
    val email: String = "",
    val phoneNumber: String = "",
    val status: String = "Pending", // Pending, Approved, Rejected
    val role: String = "User", // User, Admin, Super Admin
    val joinedDate: Long = 0L,
    val isVerifiedSeller: Boolean = false,
    val trustScore: Int = 100,
    val avatarUrl: String = ""
)

// 2. games
data class FirestoreGame(
    val gameId: String = "",
    val name: String = "", // e.g. "EA FC Mobile", "eFootball Mobile"
    val description: String = "",
    val logoUrl: String = "",
    val bannerUrl: String = "",
    val customFields: List<CustomFieldDefinition> = emptyList()
)

data class CustomFieldDefinition(
    val key: String = "", // e.g. "coins", "level"
    val labelEn: String = "", // e.g. "Coins", "Level"
    val labelFa: String = "", // e.g. "سکه", "لول"
    val type: String = "Number" // Number, Text, Boolean
)

// 3. game_categories
data class GameCategory(
    val categoryId: String = "",
    val gameId: String = "",
    val nameEn: String = "",
    val nameFa: String = ""
)

// 4. listings (Listing is already defined in listings/Listing.kt, this matches or extends)
// (Matches ir.mtnmh.primeaccount.listings.Listing)

// 5. favorites
data class Favorite(
    val userId: String = "",
    val listingId: String = "",
    val timestamp: Long = 0L
)

// 6. chats & 7. messages
data class ChatRoom(
    val chatId: String = "",
    val listingId: String = "",
    val participants: List<String> = emptyList(), // [buyerId, sellerId]
    val lastMessage: String = "",
    val updatedAt: Long = 0L,
    val listingTitle: String = "",
    val listingPrice: Double = 0.0,
    val listingImage: String = ""
)

data class ChatMessage(
    val messageId: String = "",
    val chatId: String = "",
    val senderId: String = "",
    val text: String = "",
    val imageUrl: String = "",
    val timestamp: Long = 0L,
    val seen: Boolean = false
)

// 8. deals
data class DealAccountInfo(
    val username: String = "",
    val passwordEncrypted: String = "",
    val notes: String = ""
)

data class EscrowDeal(
    val dealId: String = "",
    val listingId: String = "",
    val buyerId: String = "",
    val sellerId: String = "",
    val amount: Double = 0.0,
    val fee: Double = 0.0,
    val sellerCardNumber: String = "",
    val status: String = "WAITING_FOR_PAYMENT", // WAITING_FOR_PAYMENT, RECEIPT_UPLOADED, UNDER_ADMIN_REVIEW, PAYMENT_CONFIRMED, ACCOUNT_DELIVERED, COMPLETED, DISPUTE_OPEN
    val receiptImageUrl: String = "",
    val paymentReceiptUrl: String = "",
    val accountInfo: DealAccountInfo? = null,
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L
)

// 9. wallets
data class UserWallet(
    val userId: String = "",
    val balance: Double = 0.0,
    val currency: String = "IRR",
    val lastUpdated: Long = 0L
)

// 10. reports
data class UserReport(
    val reportId: String = "",
    val reporterId: String = "",
    val reportedUserId: String = "",
    val chatOrListingId: String = "",
    val reason: String = "",
    val timestamp: Long = 0L,
    val status: String = "Pending" // Pending, Investigating, Resolved
)

// 11. notifications
data class PushNotification(
    val notificationId: String = "",
    val userId: String = "",
    val title: String = "",
    val body: String = "",
    val type: String = "", // CHAT_MSG, DEAL_UPDATE, VERIFICATION_RESULT
    val isRead: Boolean = false,
    val timestamp: Long = 0L
)

// 12. settings
data class AppSystemSettings(
    val isMaintenanceMode: Boolean = false,
    val minAppVersion: String = "1.0.0",
    val forceUpdate: Boolean = false,
    val contactSupportLink: String = ""
)

// 13. admins
data class AdminUser(
    val adminId: String = "",
    val email: String = "",
    val role: String = "Admin", // Admin, Super Admin
    val permissions: List<String> = emptyList() // "APPROVE_USERS", "MANAGE_LISTINGS", "ADD_GAMES", "MANAGE_DEALS"
)

// 14. commissions
data class PlatformCommission(
    val commissionId: String = "",
    val minPrice: Double = 0.0,
    val maxPrice: Double = 0.0,
    val percentage: Double = 2.5, // Default 2.5%
    val fixedFee: Double = 0.0
)

// 15. verification_requests
data class VerificationRequest(
    val requestId: String = "",
    val userId: String = "",
    val fullName: String = "",
    val nationalId: String = "",
    val phoneNumber: String = "",
    val bankCardNumber: String = "",
    val bankAccountHolderName: String = "",
    val idCardImageUrl: String = "",
    val selfieImageUrl: String = "",
    val status: String = "Pending", // Pending, Approved, Rejected
    val rejectionReason: String = "",
    val timestamp: Long = 0L
)

// ==================================================
// PHASE 6 - TRUST SCORE & REPUTATION MODELS
// ==================================================

data class UserRating(
    val ratingId: String = "",
    val dealId: String = "",
    val raterId: String = "",
    val raterUsername: String = "",
    val rateeId: String = "",
    val rating: Int = 5, // 1 to 5 Stars
    val comment: String = "",
    val timestamp: Long = 0L
)

data class UserReview(
    val reviewId: String = "",
    val dealId: String = "",
    val reviewerId: String = "",
    val reviewerUsername: String = "",
    val revieweeId: String = "",
    val rating: Int = 5,
    val reviewText: String = "",
    val timestamp: Long = 0L
)

data class Report(
    val reportId: String = "",
    val reporterId: String = "",
    val targetType: String = "", // USER, LISTING, CHAT, DEAL
    val targetId: String = "",   // ID of reported element
    val reason: String = "",     // Fraud, Fake Account, Spam, Abuse, Other
    val description: String = "",
    val timestamp: Long = 0L,
    val status: String = "Open",  // Open, Closed
    val adminNotes: String = ""
)

data class TrustScoreProfile(
    val userId: String = "",
    val score: Int = 100,
    val successfulDeals: Int = 0,
    val completedPurchases: Int = 0,
    val completedSales: Int = 0,
    val reportsCount: Int = 0,
    val activeWarnings: List<String> = emptyList(),
    val accountCreationDate: Long = 0L,
    val lastActive: Long = 0L,
    val userStatus: String = "Normal" // Normal, Verified Seller, Trusted Seller, Suspended, Banned
)

data class AdminAction(
    val actionId: String = "",
    val adminId: String = "",
    val targetUserId: String = "",
    val actionType: String = "", // SUSPEND, BAN, CHANGE_STATUS, ADD_NOTES, WARNING
    val notes: String = "",
    val timestamp: Long = 0L
)


package ir.mtnmh.primeaccount.deals

/**
 * Escrow Deals Contract / Repository Architecture.
 * Prepared for Phase 2 Escrow and Transaction capabilities.
 */
interface DealsRepository {
    suspend fun getDealHistory(userId: String): Result<List<Any>>
    suspend fun initiateEscrow(buyerId: String, sellerId: String, listingId: String): Result<String>
    suspend fun confirmReceipt(dealId: String): Result<Boolean>
}

package ir.mtnmh.primeaccount.admin

/**
 * Moderation and Escalation Admin API interface.
 */
interface AdminRepository {
    suspend fun getPendingListings(): Result<List<Any>>
    suspend fun auditListing(listingId: String, approve: Boolean): Result<Boolean>
    suspend fun resolveDispute(dealId: String, favoredUserId: String): Result<Boolean>
}

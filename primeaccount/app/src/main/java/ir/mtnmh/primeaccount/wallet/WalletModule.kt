package ir.mtnmh.primeaccount.wallet

/**
 * Wallet Account Management Architecture.
 * Prepared for deposit, withdrawal, and currency balance transactions in Phase 2.
 */
interface WalletRepository {
    suspend fun getBalance(userId: String): Result<Double>
    suspend fun requestWithdrawal(userId: String, amount: Double, destinationCard: String): Result<Boolean>
}

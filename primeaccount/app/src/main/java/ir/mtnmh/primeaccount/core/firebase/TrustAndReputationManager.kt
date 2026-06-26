package ir.mtnmh.primeaccount.core.firebase

import android.util.Log
import ir.mtnmh.primeaccount.core.models.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

object TrustAndReputationManager {
    private const val TAG = "TrustRepManager"

    // Reputation database flows
    private val _reviews = MutableStateFlow<List<UserReview>>(emptyList())
    val reviews: StateFlow<List<UserReview>> = _reviews.asStateFlow()

    private val _ratings = MutableStateFlow<List<UserRating>>(emptyList())
    val ratings: StateFlow<List<UserRating>> = _ratings.asStateFlow()

    private val _reports = MutableStateFlow<List<Report>>(emptyList())
    val reports: StateFlow<List<Report>> = _reports.asStateFlow()

    private val _trustProfiles = MutableStateFlow<Map<String, TrustScoreProfile>>(emptyMap())
    val trustProfiles: StateFlow<Map<String, TrustScoreProfile>> = _trustProfiles.asStateFlow()

    private val _adminActions = MutableStateFlow<List<AdminAction>>(emptyList())
    val adminActions: StateFlow<List<AdminAction>> = _adminActions.asStateFlow()

    init {
        _reviews.value = emptyList()
        _ratings.value = emptyList()
        _trustProfiles.value = emptyMap()
        _reports.value = emptyList()
        syncWithFirestore()
    }

    private fun syncWithFirestore() {
        val db = FirebaseInitializer.firestore ?: return
        try {
            db.collection("reviews")
                .addSnapshotListener { snapshot, error ->
                    if (error == null && snapshot != null) {
                        val firestoreList = snapshot.documents.mapNotNull { it.toObject(UserReview::class.java) }
                        _reviews.value = firestoreList.sortedByDescending { it.timestamp }
                    }
                }
            db.collection("ratings")
                .addSnapshotListener { snapshot, error ->
                    if (error == null && snapshot != null) {
                        val firestoreList = snapshot.documents.mapNotNull { it.toObject(UserRating::class.java) }
                        _ratings.value = firestoreList
                    }
                }
            db.collection("reports")
                .addSnapshotListener { snapshot, error ->
                    if (error == null && snapshot != null) {
                        val firestoreList = snapshot.documents.mapNotNull { it.toObject(Report::class.java) }
                        _reports.value = firestoreList.sortedByDescending { it.timestamp }
                    }
                }
            db.collection("trust_scores")
                .addSnapshotListener { snapshot, error ->
                    if (error == null && snapshot != null) {
                        val firestoreList = snapshot.documents.mapNotNull { it.toObject(TrustScoreProfile::class.java) }
                        val profileMap = firestoreList.associateBy { it.userId }
                        _trustProfiles.value = profileMap
                    }
                }
            db.collection("admin_actions")
                .addSnapshotListener { snapshot, error ->
                    if (error == null && snapshot != null) {
                        val firestoreList = snapshot.documents.mapNotNull { it.toObject(AdminAction::class.java) }
                        _adminActions.value = firestoreList
                    }
                }
        } catch (e: Exception) {
            Log.e(TAG, "Failed syncing trust/reputation collections: ${e.message}")
        }
    }

    // Retrieve or initialize profile for a user
    fun getOrCreateTrustProfile(userId: String): TrustScoreProfile {
        val existing = _trustProfiles.value[userId]
        if (existing != null) return existing

        val newProfile = TrustScoreProfile(
            userId = userId,
            score = 100,
            successfulDeals = 0,
            completedPurchases = 0,
            completedSales = 0,
            reportsCount = 0,
            activeWarnings = emptyList(),
            accountCreationDate = System.currentTimeMillis() - 86400000, // 1 day ago
            lastActive = System.currentTimeMillis(),
            userStatus = "Normal"
        )
        _trustProfiles.value = _trustProfiles.value.toMutableMap().apply {
            put(userId, newProfile)
        }
        
        // Save to firestore if present
        val db = FirebaseInitializer.firestore
        if (db != null) {
            db.collection("trust_scores").document(userId).set(newProfile)
        }
        return newProfile
    }

    // --- Submitting User Reviews & Ratings ---
    fun submitRatingAndReview(
        dealId: String,
        reviewerId: String,
        reviewerName: String,
        revieweeId: String,
        rating: Int,
        text: String
    ): Boolean {
        // Validation Checks as per requirements (Section 10 Security)
        if (reviewerId == revieweeId) {
            Log.e(TAG, "Security violation: Users cannot rate themselves.")
            return false
        }

        // Check if completed deal already rated by this reviewer
        val hasAlreadyRated = _reviews.value.any { it.dealId == dealId && it.reviewerId == reviewerId }
        if (hasAlreadyRated) {
            Log.e(TAG, "Security violation: Deal already rated once by this participant.")
            return false
        }

        val ratingId = "rat_${UUID.randomUUID()}"
        val reviewId = "rev_${UUID.randomUUID()}"
        val now = System.currentTimeMillis()

        val newRating = UserRating(
            ratingId = ratingId,
            dealId = dealId,
            raterId = reviewerId,
            raterUsername = reviewerName,
            rateeId = revieweeId,
            rating = rating,
            comment = text,
            timestamp = now
        )

        val newReview = UserReview(
            reviewId = reviewId,
            dealId = dealId,
            reviewerId = reviewerId,
            reviewerUsername = reviewerName,
            revieweeId = revieweeId,
            rating = rating,
            reviewText = text,
            timestamp = now
        )

        // Append locally
        _ratings.value = _ratings.value + newRating
        _reviews.value = listOf(newReview) + _reviews.value

        // Increment successful deals on trust profiles
        val reviewerProfile = getOrCreateTrustProfile(reviewerId)
        val revieweeProfile = getOrCreateTrustProfile(revieweeId)

        // Increment counts depending on who they are
        // We will assume both are updated
        val updatedReviewer = reviewerProfile.copy(
            successfulDeals = reviewerProfile.successfulDeals + 1,
            completedPurchases = reviewerProfile.completedPurchases + 1
        )
        val updatedReviewee = revieweeProfile.copy(
            successfulDeals = revieweeProfile.successfulDeals + 1,
            completedSales = revieweeProfile.completedSales + 1
        )

        _trustProfiles.value = _trustProfiles.value.toMutableMap().apply {
            put(reviewerId, updatedReviewer)
            put(revieweeId, updatedReviewee)
        }

        // Recalculating score immediately
        recalculateAndSaveScore(reviewerId)
        recalculateAndSaveScore(revieweeId)

        // Write to Firestore
        val db = FirebaseInitializer.firestore
        if (db != null) {
            db.collection("ratings").document(ratingId).set(newRating)
            db.collection("reviews").document(reviewId).set(newReview)
            db.collection("trust_scores").document(reviewerId).set(updatedReviewer)
            db.collection("trust_scores").document(revieweeId).set(updatedReviewee)
        }
        return true
    }

    // --- Submitting Reports ---
    fun submitReport(
        reporterId: String,
        targetType: String, // USER, LISTING, CHAT, DEAL
        targetId: String,
        reason: String, // Fraud, Fake Account, Spam, Abuse, Other
        description: String
    ): String {
        val reportId = "rep_${UUID.randomUUID()}"
        val newReport = Report(
            reportId = reportId,
            reporterId = reporterId,
            targetType = targetType,
            targetId = targetId,
            reason = reason,
            description = description,
            timestamp = System.currentTimeMillis(),
            status = "Open"
        )

        _reports.value = listOf(newReport) + _reports.value

        // If target is a User, increment reports count on their profile
        if (targetType == "USER") {
            val targetProfile = getOrCreateTrustProfile(targetId)
            val updatedProfile = targetProfile.copy(
                reportsCount = targetProfile.reportsCount + 1,
                activeWarnings = if (targetProfile.reportsCount + 1 >= 3 && !targetProfile.activeWarnings.contains("Warning: High Reports Count")) {
                    targetProfile.activeWarnings + "Warning: High Reports Count"
                } else {
                    targetProfile.activeWarnings
                }
            )
            _trustProfiles.value = _trustProfiles.value.toMutableMap().apply {
                put(targetId, updatedProfile)
            }
            recalculateAndSaveScore(targetId)

            val db = FirebaseInitializer.firestore
            if (db != null) {
                db.collection("trust_scores").document(targetId).set(updatedProfile)
            }
        }

        val db = FirebaseInitializer.firestore
        if (db != null) {
            db.collection("reports").document(reportId).set(newReport)
        }

        return reportId
    }

    // --- Admin Operations ---
    fun adminResolveReport(reportId: String, actionNotes: String) {
        val report = _reports.value.find { it.reportId == reportId } ?: return
        val updated = report.copy(status = "Closed", adminNotes = actionNotes)

        _reports.value = _reports.value.map {
            if (it.reportId == reportId) updated else it
        }

        val db = FirebaseInitializer.firestore
        if (db != null) {
            db.collection("reports").document(reportId).set(updated)
        }
    }

    fun adminChangeUserStatusAndPenalties(
        adminId: String,
        targetUserId: String,
        newStatus: String, // Normal, Verified Seller, Trusted Seller, Suspended, Banned
        adminNotes: String
    ) {
        val profile = getOrCreateTrustProfile(targetUserId)
        val updatedProfile = profile.copy(
            userStatus = newStatus,
            activeWarnings = if (newStatus == "Suspended") {
                profile.activeWarnings + "Warning: Account Suspended by Admin"
            } else {
                profile.activeWarnings.filter { !it.contains("Suspended") }
            }
        )

        _trustProfiles.value = _trustProfiles.value.toMutableMap().apply {
            put(targetUserId, updatedProfile)
        }

        val actionId = "act_${UUID.randomUUID()}"
        val newAction = AdminAction(
            actionId = actionId,
            adminId = adminId,
            targetUserId = targetUserId,
            actionType = when (newStatus) {
                "Suspended" -> "SUSPEND"
                "Banned" -> "BAN"
                else -> "CHANGE_STATUS"
            },
            notes = adminNotes,
            timestamp = System.currentTimeMillis()
        )

        _adminActions.value = _adminActions.value + newAction

        // Recalculating score dynamically takes new admin status / penalties into account!
        recalculateAndSaveScore(targetUserId)

        val db = FirebaseInitializer.firestore
        if (db != null) {
            db.collection("trust_scores").document(targetUserId).set(updatedProfile)
                .addOnSuccessListener {
                    // Sync up user verification label in users collection
                    db.collection("users").document(targetUserId).update(
                        mapOf(
                            "isVerifiedSeller" to (newStatus == "Verified Seller" || newStatus == "Trusted Seller"),
                            "status" to if (newStatus == "Suspended") "Suspended" else if (newStatus == "Banned") "Banned" else "Approved"
                        )
                    )
                }
            db.collection("admin_actions").document(actionId).set(newAction)
        }
    }

    // Dynamic Trust Score Recalculator
    fun recalculateAndSaveScore(userId: String) {
        val profile = _trustProfiles.value[userId] ?: return
        val userReviews = _reviews.value.filter { it.revieweeId == userId }
        val userReports = _reports.value.filter { it.targetId == userId && it.status == "Closed" }
        val penalties = _adminActions.value.filter { it.targetUserId == userId }

        var score = 75 // Base trust score

        // +4 credits per successful deal (up to max +30)
        score += (profile.successfulDeals * 4).coerceAtMost(30)

        // Positive reviews and rating weightings and deduction
        if (userReviews.isNotEmpty()) {
            val avgRating = userReviews.map { it.rating }.average()
            if (avgRating >= 4.5) {
                score += 15
            } else if (avgRating >= 4.0) {
                score += 10
            } else if (avgRating >= 3.0) {
                score += 2
            } else {
                score -= 20 // negative rating impact
            }
        }

        // Status badges give credibility
        when (profile.userStatus) {
            "Verified Seller" -> score += 15
            "Trusted Seller" -> score += 25
            "Suspended" -> score -= 50
            "Banned" -> score = 0
        }

        // Deductions for reports
        score -= (profile.reportsCount * 8)

        // Penalty logs
        val scamsCount = penalties.count { it.actionType == "BAN" || it.notes.contains("Scam", true) }
        score -= (scamsCount * 80)

        val suspensionsCount = penalties.count { it.actionType == "SUSPEND" }
        score -= (suspensionsCount * 45)

        val finalScore = score.coerceIn(0, 100)

        val updated = profile.copy(score = finalScore)
        _trustProfiles.value = _trustProfiles.value.toMutableMap().apply {
            put(userId, updated)
        }

        val db = FirebaseInitializer.firestore
        if (db != null) {
            db.collection("trust_scores").document(userId).set(updated)
            // also update users table's trustScore property
            db.collection("users").document(userId).update("trustScore", finalScore)
        }
    }
}

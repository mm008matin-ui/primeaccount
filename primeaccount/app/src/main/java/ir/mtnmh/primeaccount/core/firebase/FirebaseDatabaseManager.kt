package ir.mtnmh.primeaccount.core.firebase

import android.util.Log
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import ir.mtnmh.primeaccount.core.models.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await
import java.util.UUID

object FirebaseDatabaseManager {
    private const val TAG = "FirebaseDatabaseManager"

    // Online Statuses
    private val _onlineUsers = MutableStateFlow<Map<String, Boolean>>(emptyMap())
    val onlineUsers: StateFlow<Map<String, Boolean>> = _onlineUsers.asStateFlow()

    // Blocked/Reported registries
    private val _blockedUsers = MutableStateFlow<Set<String>>(emptySet())
    val blockedUsers: StateFlow<Set<String>> = _blockedUsers.asStateFlow()

    private val _reportedUsers = MutableStateFlow<Set<String>>(emptySet())
    val reportedUsers: StateFlow<Set<String>> = _reportedUsers.asStateFlow()

    // Verification request state for currently logged-in user
    private val _verificationRequest = MutableStateFlow<VerificationRequest?>(null)
    val verificationRequest: StateFlow<VerificationRequest?> = _verificationRequest.asStateFlow()

    // All active verification requests (for simulate admin)
    private val _allVerificationRequests = MutableStateFlow<List<VerificationRequest>>(emptyList())
    val allVerificationRequests: StateFlow<List<VerificationRequest>> = _allVerificationRequests.asStateFlow()

    // Real-time Chat list state
    private val _activeChats = MutableStateFlow<List<ChatRoom>>(emptyList())
    val activeChats: StateFlow<List<ChatRoom>> = _activeChats.asStateFlow()

    // Real-time messages for current open chat
    private val _currentChatMessages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val currentChatMessages: StateFlow<List<ChatMessage>> = _currentChatMessages.asStateFlow()

    // Deals registry
    private val _deals = MutableStateFlow<List<EscrowDeal>>(emptyList())
    val deals: StateFlow<List<EscrowDeal>> = _deals.asStateFlow()

    private var activeMessagesListener: ListenerRegistration? = null
    private var activeChatsListener: ListenerRegistration? = null
    private var verificationRequestListener: ListenerRegistration? = null
    private var allVerificationRequestsListener: ListenerRegistration? = null

    init {
        syncAllForCurrentUser()
    }

    fun syncAllForCurrentUser() {
        syncActiveChats()
        syncDeals()
        syncVerificationRequests()
        syncAllVerificationRequests()
        ir.mtnmh.primeaccount.listings.ListingsRepository.syncFavoritesForCurrentUser()
        ir.mtnmh.primeaccount.core.firebase.NotificationManager.syncWithFirestore()
    }

    fun clearAll() {
        _activeChats.value = emptyList()
        _currentChatMessages.value = emptyList()
        _deals.value = emptyList()
        _verificationRequest.value = null
        _allVerificationRequests.value = emptyList()

        activeChatsListener?.remove()
        activeMessagesListener?.remove()
        verificationRequestListener?.remove()
        allVerificationRequestsListener?.remove()

        ir.mtnmh.primeaccount.listings.ListingsRepository.clearCache()
        ir.mtnmh.primeaccount.core.firebase.NotificationManager.clearNotifications()
    }

    // --- Verification Requests ---
    fun submitVerificationRequest(request: VerificationRequest) {
        val db = FirebaseInitializer.firestore
        val updatedRequest = request.copy(
            requestId = request.requestId.ifEmpty { UUID.randomUUID().toString() },
            timestamp = System.currentTimeMillis()
        )

        _verificationRequest.value = updatedRequest

        if (db != null) {
            db.collection("verification_requests").document(updatedRequest.userId).set(updatedRequest)
                .addOnSuccessListener {
                    Log.d(TAG, "Sent verification request to Firestore")
                }
        }
    }

    fun updateVerificationStatus(userId: String, status: String, reason: String = "") {
        val db = FirebaseInitializer.firestore
        if (db != null) {
            val updateData = mapOf(
                "status" to status,
                "rejectionReason" to reason
            )
            db.collection("verification_requests").document(userId).update(updateData)
            db.collection("users").document(userId).update("isVerifiedSeller", status == "Approved")
        }

        // Post verification result notifications
        val title = if (status == "Approved") "تاییدیه فروشنده معتبر / Verified Seller Approved 🎉" else "رد تایید هویت / Seller Verification Rejected ❌"
        val message = if (status == "Approved") "پروفایل فروشنده با اعتبار درخشان شما تایید شد. هم‌اکنون نشان ستاره بر حساب شما فعال است. / Your trusted developer verified seller profile has been approved."
                      else "درخواست فروشنده معتبر به دلیل '$reason' توسط ناظر رد شد. / Seller verification rejected due to: $reason"
        NotificationManager.postNotification(
            userId = userId,
            title = title,
            message = message,
            type = "SECURITY"
        )
    }

    // --- Block / Report Security Systems ---
    fun blockUser(userId: String) {
        val db = FirebaseInitializer.firestore ?: return
        val currentUserId = FirebaseInitializer.auth?.currentUser?.uid ?: ""
        if (currentUserId.isEmpty()) return

        val updated = _blockedUsers.value.toMutableSet()
        updated.add(userId)
        _blockedUsers.value = updated

        db.collection("blocked_users").document("${currentUserId}_$userId").set(
            mapOf(
                "blockerId" to currentUserId,
                "blockedId" to userId,
                "timestamp" to System.currentTimeMillis()
            )
        )
    }

    fun unblockUser(userId: String) {
        val db = FirebaseInitializer.firestore ?: return
        val currentUserId = FirebaseInitializer.auth?.currentUser?.uid ?: ""
        if (currentUserId.isEmpty()) return

        val updated = _blockedUsers.value.toMutableSet()
        updated.remove(userId)
        _blockedUsers.value = updated

        db.collection("blocked_users").document("${currentUserId}_$userId").delete()
    }

    fun reportUser(reportedId: String, reason: String, contextId: String) {
        val db = FirebaseInitializer.firestore ?: return
        val currentUserId = FirebaseInitializer.auth?.currentUser?.uid ?: ""
        if (currentUserId.isEmpty()) return

        val updated = _reportedUsers.value.toMutableSet()
        updated.add(reportedId)
        _reportedUsers.value = updated

        val report = UserReport(
            reportId = UUID.randomUUID().toString(),
            reporterId = currentUserId,
            reportedUserId = reportedId,
            chatOrListingId = contextId,
            reason = reason,
            timestamp = System.currentTimeMillis(),
            status = "Pending"
        )
        db.collection("reports").document(report.reportId).set(report)
    }

    // --- Chat Room Operations ---
    fun startChatOrCreate(listingId: String, sellerId: String, sellerName: String, listingTitle: String, listingPrice: Double, listingImage: String, onCompleted: (String) -> Unit) {
        val buyerId = FirebaseInitializer.auth?.currentUser?.uid ?: ""
        if (buyerId.isEmpty()) return
        val existing = _activeChats.value.find { it.listingId == listingId && it.participants.contains(buyerId) }
        
        if (existing != null) {
            onCompleted(existing.chatId)
            return
        }

        val newChatId = "chat_${UUID.randomUUID()}"
        val newChat = ChatRoom(
            chatId = newChatId,
            listingId = listingId,
            participants = listOf(buyerId, sellerId),
            lastMessage = "مذاکره جدید امن شروع شد / Deal chat initiated",
            updatedAt = System.currentTimeMillis(),
            listingTitle = listingTitle,
            listingPrice = listingPrice,
            listingImage = listingImage
        )

        onCompleted(newChatId)

        val db = FirebaseInitializer.firestore
        if (db != null) {
            db.collection("chats").document(newChatId).set(newChat)
        }
    }

    private fun syncActiveChats() {
        val currentUserId = FirebaseInitializer.auth?.currentUser?.uid ?: ""
        _activeChats.value = emptyList()
        activeChatsListener?.remove()

        if (currentUserId.isEmpty()) return
        val db = FirebaseInitializer.firestore ?: return
        try {
            activeChatsListener = db.collection("chats")
                .whereArrayContains("participants", currentUserId)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.e(TAG, "Chats listen failed", error)
                        return@addSnapshotListener
                    }
                    if (snapshot != null) {
                        val firestoreChats = snapshot.documents.mapNotNull { it.toObject(ChatRoom::class.java) }
                        _activeChats.value = firestoreChats
                            .filter { it.chatId.isNotEmpty() && it.participants.isNotEmpty() && !it.chatId.lowercase().contains("test") && !it.chatId.lowercase().contains("fake") }
                            .sortedByDescending { it.updatedAt }
                    }
                }
        } catch (e: Exception) {
            Log.e(TAG, "Failed chat snapshot syncing: ${e.message}")
        }
    }

    // --- Messages Handling ---
    fun openChatMessages(chatId: String) {
        _currentChatMessages.value = emptyList()

        activeMessagesListener?.remove()
        val db = FirebaseInitializer.firestore ?: return
        try {
            activeMessagesListener = db.collection("messages")
                .whereEqualTo("chatId", chatId)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.e(TAG, "Messages listen failed", error)
                        return@addSnapshotListener
                    }
                    if (snapshot != null) {
                        val firestoreMessages = snapshot.documents.mapNotNull { it.toObject(ChatMessage::class.java) }
                        _currentChatMessages.value = firestoreMessages
                            .distinctBy { it.messageId }
                            .sortedBy { it.timestamp }
                    }
                }
        } catch (e: Exception) {
            Log.e(TAG, "Failed message list reading: ${e.message}")
        }
    }

    fun closeChatMessages() {
        activeMessagesListener?.remove()
        activeMessagesListener = null
        _currentChatMessages.value = emptyList()
    }

    fun sendMessage(chatId: String, text: String, imageUrl: String = "") {
        val senderId = FirebaseInitializer.auth?.currentUser?.uid ?: ""
        if (senderId.isEmpty()) return
        val messageId = "msg_${UUID.randomUUID()}"
        val newMessage = ChatMessage(
            messageId = messageId,
            chatId = chatId,
            senderId = senderId,
            text = text,
            imageUrl = imageUrl,
            timestamp = System.currentTimeMillis(),
            seen = false
        )

        val db = FirebaseInitializer.firestore
        if (db != null) {
            db.collection("messages").document(messageId).set(newMessage)
            db.collection("chats").document(chatId).update(
                mapOf(
                    "lastMessage" to (text.ifEmpty { "تصویر / Image" }),
                    "updatedAt" to System.currentTimeMillis()
                )
            )
        }

        // Send notifications to the recipient
        val chatRoom = _activeChats.value.find { it.chatId == chatId }
        val recipientId = chatRoom?.participants?.find { it != senderId }
        if (recipientId != null) {
            NotificationManager.postNotification(
                userId = recipientId,
                title = "پیام جدید / New Message 💬",
                message = text.ifEmpty { "یک تصویر جدید در گپ ارسال شد. / New file attachment in chat." },
                type = "MESSAGES"
            )
        }
    }

    // --- Secure Deal Initiation & Payments ---
    fun syncDeals() {
        val currentUserId = FirebaseInitializer.auth?.currentUser?.uid ?: ""
        _deals.value = emptyList()

        if (currentUserId.isEmpty()) return
        val db = FirebaseInitializer.firestore ?: return
        try {
            db.collection("deals")
                .whereEqualTo("buyerId", currentUserId)
                .addSnapshotListener { snapshot, error ->
                    if (error == null && snapshot != null) {
                        val firestoreDeals = snapshot.documents.mapNotNull { it.toObject(EscrowDeal::class.java) }
                        val otherDeals = _deals.value.filter { it.sellerId == currentUserId && it.buyerId != currentUserId }
                        val merged = (firestoreDeals + otherDeals)
                            .distinctBy { it.dealId }
                            .sortedByDescending { it.updatedAt }
                        _deals.value = merged
                    }
                }
            
            db.collection("deals")
                .whereEqualTo("sellerId", currentUserId)
                .addSnapshotListener { snapshot, error ->
                    if (error == null && snapshot != null) {
                        val firestoreDeals = snapshot.documents.mapNotNull { it.toObject(EscrowDeal::class.java) }
                        val otherDeals = _deals.value.filter { it.buyerId == currentUserId && it.sellerId != currentUserId }
                        val merged = (firestoreDeals + otherDeals)
                            .distinctBy { it.dealId }
                            .sortedByDescending { it.updatedAt }
                        _deals.value = merged
                    }
                }
        } catch (e: Exception) {
            Log.e(TAG, "Failed syncing deals: ${e.message}")
        }
    }

    fun startSecureDeal(listingId: String, sellerId: String, amount: Double, onCompleted: (String) -> Unit) {
        val buyerId = FirebaseInitializer.auth?.currentUser?.uid ?: ""
        if (buyerId.isEmpty()) return
        val dealId = "deal_${UUID.randomUUID()}"
        val deal = EscrowDeal(
            dealId = dealId,
            listingId = listingId,
            buyerId = buyerId,
            sellerId = sellerId,
            amount = amount,
            fee = amount * 0.025, // 2.5% platform commission
            sellerCardNumber = "5022-2910-1234-5678", // Simulated Iranian card number
            status = "WAITING_FOR_PAYMENT",
            receiptImageUrl = "",
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )

        onCompleted(dealId)

        val db = FirebaseInitializer.firestore
        if (db != null) {
            db.collection("deals").document(dealId).set(deal).addOnSuccessListener {
                Log.d(TAG, "Secured Deal initiated on Firestore: $dealId")
            }
        }

        // Notify seller and buyer
        NotificationManager.postNotification(
            userId = sellerId,
            title = "معامله امن جدید / New Secure Deal 🛡️",
            message = "درخواست معامله روی آگهی شما ثبت گردید. منتظر دریافت فیش پرداخت خریدار باشید. / Secure deal requested.",
            type = "DEALS"
        )
    }

    fun uploadReceipt(dealId: String, receiptImageUrl: String) {
        val now = System.currentTimeMillis()
        val db = FirebaseInitializer.firestore
        if (db != null) {
            db.collection("deals").document(dealId).update(
                mapOf(
                    "status" to "RECEIPT_UPLOADED",
                    "receiptImageUrl" to receiptImageUrl,
                    "paymentReceiptUrl" to receiptImageUrl,
                    "updatedAt" to now
                )
            )
        }

        val dealObj = _deals.value.find { it.dealId == dealId }
        if (dealObj != null) {
            NotificationManager.postNotification(
                userId = dealObj.sellerId,
                title = "بارگذاری فیش پرداخت / Receipt Uploaded 🧾",
                message = "خریدار فیش پرداخت بانکی را بارگذاری نمود. منتظر تایید تراکنش بانکی توسط ناظر واسط باشید. / Buyer uploaded receipt.",
                type = "DEALS"
            )
        }
    }

    fun removeReceipt(dealId: String) {
        val now = System.currentTimeMillis()
        val db = FirebaseInitializer.firestore
        if (db != null) {
            db.collection("deals").document(dealId).update(
                mapOf(
                    "status" to "WAITING_FOR_PAYMENT",
                    "receiptImageUrl" to "",
                    "paymentReceiptUrl" to "",
                    "updatedAt" to now
                )
            )
        }

        val dealObj = _deals.value.find { it.dealId == dealId }
        if (dealObj != null) {
            NotificationManager.postNotification(
                userId = dealObj.buyerId,
                title = "رد فیش پرداخت معامله / Receipt Rejected ❌",
                message = "تراکنش بانکی ارسال شده رد گردید. لطفا فیش معتبر و جدید بانکی آپلود نمایید. / Payment receipt rejected.",
                type = "DEALS"
            )
        }
    }

    fun adminReviewDeal(dealId: String, status: String) {
        val now = System.currentTimeMillis()
        val db = FirebaseInitializer.firestore
        if (db != null) {
            db.collection("deals").document(dealId).update(
                mapOf(
                    "status" to status,
                    "updatedAt" to now
                )
            )
        }

        val dealObj = _deals.value.find { it.dealId == dealId }
        if (dealObj != null) {
            if (status == "PAYMENT_CONFIRMED") {
                // Confirm payout notification
                NotificationManager.postNotification(
                    userId = dealObj.buyerId,
                    title = "پرداخت معامله تایید شد / Payment Approved ✅",
                    message = "پرداخت شما تایید شد. فروشنده وظیفه دارد مشخصات اکانت را حداکثر ظرف ۲۴ ساعت آینده تحویل دهد. / Payment verified by admin.",
                    type = "DEALS"
                )
                NotificationManager.postNotification(
                    userId = dealObj.sellerId,
                    title = "پرداخت معامله واسط تایید شد / Payment Confirmed ✅",
                    message = "پرداخت خریدار تایید گردید. لطفا هرچه سریع‌تر مشخصات حقیقی اکانت را وارد کرده و تحویل دهید. / Deliver gaming credentials.",
                    type = "DEALS"
                )
            } else if (status == "WAITING_FOR_PAYMENT") {
                NotificationManager.postNotification(
                    userId = dealObj.buyerId,
                    title = "رد رسید پرداخت / Payment Refused ❌",
                    message = "رسید شما توسط ناظر تایید نشد. مجددا رسید تراکنش صحیح ارسال کنید. / Audited and rejected.",
                    type = "DEALS"
                )
            }
        }
    }

    fun submitAccountInfo(dealId: String, username: String, passwordEncrypted: String, notes: String) {
        val now = System.currentTimeMillis()
        val db = FirebaseInitializer.firestore
        val info = ir.mtnmh.primeaccount.core.models.DealAccountInfo(
            username = username,
            passwordEncrypted = passwordEncrypted,
            notes = notes
        )
        if (db != null) {
            db.collection("deals").document(dealId).update(
                mapOf(
                    "status" to "ACCOUNT_DELIVERED",
                    "accountInfo" to info,
                    "updatedAt" to now
                )
            )
        }

        val dealObj = _deals.value.find { it.dealId == dealId }
        if (dealObj != null) {
            NotificationManager.postNotification(
                userId = dealObj.buyerId,
                title = "تحویل مشخصات اکانت / Account Delivered 🔑",
                message = "فروشنده مشخصات اتصال اکانت بازی را وارد کرد. وارد معامله شده و جزئیات را برای تحویل بررسی کنید. / Gaming account login credentials sent.",
                type = "DEALS"
            )
        }
    }

    fun buyerConfirmDelivery(dealId: String) {
        val now = System.currentTimeMillis()
        val db = FirebaseInitializer.firestore
        if (db != null) {
            db.collection("deals").document(dealId).update(
                mapOf(
                    "status" to "COMPLETED",
                    "updatedAt" to now
                )
            )
        }

        val dealObj = _deals.value.find { it.dealId == dealId }
        if (dealObj != null) {
            NotificationManager.postNotification(
                userId = dealObj.sellerId,
                title = "معامله تکمیل شد 🎉 / Deal Completed",
                message = "خریدار صحت اکانت را تایید نمود. مبلغ معامله امن پس از کسر کارمزد بر کیف پول شما آزاد گردید. / Payout released to wallet.",
                type = "DEALS"
            )
        }
    }

    fun openDispute(dealId: String) {
        val now = System.currentTimeMillis()
        val db = FirebaseInitializer.firestore
        if (db != null) {
            db.collection("deals").document(dealId).update(
                mapOf(
                    "status" to "DISPUTE_OPEN",
                    "updatedAt" to now
                )
            )
        }

        val dealObj = _deals.value.find { it.dealId == dealId }
        if (dealObj != null) {
            val alertMsg = "اختلاف جدیدی برای معامله شماره #DEAL_ID ثبت شده و تحت نظارت داوری قرار گرفت. / Escrow arbitration initiated."
            NotificationManager.postNotification(userId = dealObj.sellerId, title = "ثبت اختلاف معامله / Dispute Opened ⚠️", message = alertMsg, type = "SECURITY")
            NotificationManager.postNotification(userId = dealObj.buyerId, title = "ثبت اختلاف معامله / Dispute Opened ⚠️", message = alertMsg, type = "SECURITY")
        }
    }

    fun syncVerificationRequests() {
        val currentUserId = FirebaseInitializer.auth?.currentUser?.uid ?: ""
        _verificationRequest.value = null
        verificationRequestListener?.remove()

        if (currentUserId.isEmpty()) return
        val db = FirebaseInitializer.firestore ?: return
        try {
            verificationRequestListener = db.collection("verification_requests")
                .document(currentUserId)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.e(TAG, "Error listening to verification request: ${error.message}")
                        return@addSnapshotListener
                    }
                    if (snapshot != null && snapshot.exists()) {
                        val request = snapshot.toObject(VerificationRequest::class.java)
                        _verificationRequest.value = request
                    } else {
                        _verificationRequest.value = null
                    }
                }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to register verification listener: ${e.message}")
        }
    }

    fun syncAllVerificationRequests() {
        _allVerificationRequests.value = emptyList()
        allVerificationRequestsListener?.remove()

        val db = FirebaseInitializer.firestore ?: return
        try {
            allVerificationRequestsListener = db.collection("verification_requests")
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.e(TAG, "Error listening to all verification requests: ${error.message}")
                        return@addSnapshotListener
                    }
                    if (snapshot != null) {
                        val requests = snapshot.documents.mapNotNull { it.toObject(VerificationRequest::class.java) }
                        _allVerificationRequests.value = requests
                    }
                }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to register all verification listener: ${e.message}")
        }
    }
}


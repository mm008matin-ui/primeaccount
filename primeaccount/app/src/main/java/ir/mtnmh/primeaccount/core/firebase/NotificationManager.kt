package ir.mtnmh.primeaccount.core.firebase

import android.content.Context
import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

// Conforms to requirements for in-app notifications
data class InAppNotification(
    val notificationId: String = "",
    val userId: String = "",
    val title: String = "",
    val message: String = "",
    val type: String = "", // MESSAGES, DEALS, LISTINGS, SECURITY, SYSTEM
    val isRead: Boolean = false,
    val createdAt: Long = 0L
)

// Conforms to requirements for local and Firestore notification preferences
data class NotificationPreferences(
    val messagesEnabled: Boolean = true,
    val dealsEnabled: Boolean = true,
    val listingsEnabled: Boolean = true,
    val announcementsEnabled: Boolean = true
)

object NotificationManager {
    private const val TAG = "NotificationManager"

    // Collections
    private const val COLL_NOTIFICATIONS = "notifications"
    private const val COLL_ANNOUNCEMENTS = "announcement_notifications"
    private const val COLL_FCM_TOKENS = "fcm_tokens"
    private const val COLL_USERS = "users"

    // Live States
    private val _notifications = MutableStateFlow<List<InAppNotification>>(emptyList())
    val notifications: StateFlow<List<InAppNotification>> = _notifications.asStateFlow()

    private val _announcements = MutableStateFlow<List<InAppNotification>>(emptyList())
    val announcements: StateFlow<List<InAppNotification>> = _announcements.asStateFlow()

    private val _preferences = MutableStateFlow(NotificationPreferences())
    val preferences: StateFlow<NotificationPreferences> = _preferences.asStateFlow()

    // Real-time Listeners
    private var notificationsListener: ListenerRegistration? = null
    private var announcementsListener: ListenerRegistration? = null

    // Deleted and Read state tracking for collective announcements (stored locally since announcements database is global)
    private val readAnnouncementIds = mutableSetOf<String>()
    private val deletedAnnouncementIds = mutableSetOf<String>()

    // Local preferences context
    private var appContext: Context? = null

    fun init(context: Context) {
        appContext = context.applicationContext
        loadLocalPreferences()
        syncWithFirestore()
    }

    private fun loadLocalPreferences() {
        val context = appContext ?: return
        val sharedPrefs = context.getSharedPreferences("prime_notification_prefs", Context.MODE_PRIVATE)
        val messages = sharedPrefs.getBoolean("messages_enabled", true)
        val deals = sharedPrefs.getBoolean("deals_enabled", true)
        val listings = sharedPrefs.getBoolean("listings_enabled", true)
        val announcements = sharedPrefs.getBoolean("announcements_enabled", true)
        
        _preferences.value = NotificationPreferences(messages, deals, listings, announcements)

        // Load read/deleted announcement IDs
        val readSet = sharedPrefs.getStringSet("read_announcements", emptySet()) ?: emptySet()
        readAnnouncementIds.clear()
        readAnnouncementIds.addAll(readSet)

        val deletedSet = sharedPrefs.getStringSet("deleted_announcements", emptySet()) ?: emptySet()
        deletedAnnouncementIds.clear()
        deletedAnnouncementIds.addAll(deletedSet)
    }

    private fun persistLocalPreferences(prefs: NotificationPreferences) {
        val context = appContext ?: return
        val sharedPrefs = context.getSharedPreferences("prime_notification_prefs", Context.MODE_PRIVATE)
        sharedPrefs.edit()
            .putBoolean("messages_enabled", prefs.messagesEnabled)
            .putBoolean("deals_enabled", prefs.dealsEnabled)
            .putBoolean("listings_enabled", prefs.listingsEnabled)
            .putBoolean("announcements_enabled", prefs.announcementsEnabled)
            .apply()
    }

    private fun persistReadAnnouncements() {
        val context = appContext ?: return
        val sharedPrefs = context.getSharedPreferences("prime_notification_prefs", Context.MODE_PRIVATE)
        sharedPrefs.edit().putStringSet("read_announcements", readAnnouncementIds).apply()
    }

    private fun persistDeletedAnnouncements() {
        val context = appContext ?: return
        val sharedPrefs = context.getSharedPreferences("prime_notification_prefs", Context.MODE_PRIVATE)
        sharedPrefs.edit().putStringSet("deleted_announcements", deletedAnnouncementIds).apply()
    }

    // --- Firebase Synchronization ---
    fun syncWithFirestore() {
        val db = FirebaseInitializer.firestore
        val currentUserId = FirebaseInitializer.auth?.currentUser?.uid ?: ""

        // Cancel previous listeners
        notificationsListener?.remove()
        announcementsListener?.remove()

        if (db != null && currentUserId.isNotEmpty()) {
            // 1. Sync User-specific Notifications
            notificationsListener = db.collection(COLL_NOTIFICATIONS)
                .whereEqualTo("userId", currentUserId)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.e(TAG, "Error listening to user notifications: ", error)
                        return@addSnapshotListener
                    }
                    if (snapshot != null) {
                        val fsNotifications = snapshot.documents.mapNotNull { doc ->
                            try {
                                InAppNotification(
                                    notificationId = doc.getString("notificationId") ?: doc.id,
                                    userId = doc.getString("userId") ?: "",
                                    title = doc.getString("title") ?: "",
                                    message = doc.getString("message") ?: "",
                                    type = doc.getString("type") ?: "SYSTEM",
                                    isRead = doc.getBoolean("isRead") ?: false,
                                    createdAt = doc.getLong("createdAt") ?: 0L
                                )
                            } catch (e: Exception) {
                                null
                            }
                        }
                        _notifications.value = fsNotifications
                    }
                }

            // 2. Sync Global Announcement Notifications
            announcementsListener = db.collection(COLL_ANNOUNCEMENTS)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.e(TAG, "Error listening to announcements: ", error)
                        return@addSnapshotListener
                    }
                    if (snapshot != null) {
                        val fsAnnouncements = snapshot.documents.mapNotNull { doc ->
                            try {
                                val id = doc.getString("notificationId") ?: doc.id
                                // Skip if user deleted it locally
                                if (deletedAnnouncementIds.contains(id)) return@mapNotNull null

                                InAppNotification(
                                    notificationId = id,
                                    userId = "",
                                    title = doc.getString("title") ?: "",
                                    message = doc.getString("message") ?: "",
                                    type = "SYSTEM", // Announcements fall into SYSTEM section
                                    isRead = readAnnouncementIds.contains(id),
                                    createdAt = doc.getLong("createdAt") ?: 0L
                                )
                            } catch (e: java.lang.Exception) {
                                null
                            }
                        }
                        _announcements.value = fsAnnouncements
                    }
                }

            // 3. Keep settings backup saved in Firestore
            db.collection(COLL_USERS).document(currentUserId)
                .get()
                .addOnSuccessListener { docSnapshot ->
                    if (docSnapshot.exists()) {
                        val prefMap = docSnapshot.get("notificationSettings") as? Map<*, *>
                        if (prefMap != null) {
                            val messages = prefMap["messagesEnabled"] as? Boolean ?: true
                            val deals = prefMap["dealsEnabled"] as? Boolean ?: true
                            val listings = prefMap["listingsEnabled"] as? Boolean ?: true
                            val ann = prefMap["announcementsEnabled"] as? Boolean ?: true
                            val updatedPrefs = NotificationPreferences(messages, deals, listings, ann)
                            _preferences.value = updatedPrefs
                            persistLocalPreferences(updatedPrefs)
                        }
                    }
                }
        } else {
            _notifications.value = emptyList()
            _announcements.value = emptyList()
        }
    }

    // --- Save FCM Token ---
    fun saveFcmToken(token: String) {
        val db = FirebaseInitializer.firestore ?: return
        val currentUserId = FirebaseInitializer.auth?.currentUser?.uid ?: ""
        if (currentUserId.isEmpty()) return
        val tokenData = mapOf(
            "userId" to currentUserId,
            "token" to token,
            "lastUpdated" to System.currentTimeMillis()
        )
        db.collection(COLL_FCM_TOKENS).document(currentUserId).set(tokenData)
            .addOnSuccessListener {
                Log.d(TAG, "FCM token successfully synchronized to Firestore")
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Failed to update FCM token on Firestore", e)
            }
    }

    // --- Save Preference Settings ---
    fun updatePreferences(prefs: NotificationPreferences) {
        _preferences.value = prefs
        persistLocalPreferences(prefs)

        val db = FirebaseInitializer.firestore ?: return
        val currentUserId = FirebaseInitializer.auth?.currentUser?.uid ?: ""
        if (currentUserId.isEmpty()) return
        
        db.collection(COLL_USERS).document(currentUserId).update("notificationSettings", prefs)
            .addOnSuccessListener {
                Log.d(TAG, "Cloud notification preferences successfully backup updated")
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Failed to upload preferences to Firestore", e)
            }
    }

    // --- Notification Triggers & Post ---
    fun postNotification(userId: String, title: String, message: String, type: String) {
        // Evaluate preference setting before generating
        val prefs = _preferences.value
        val enabled = when (type) {
            "MESSAGES" -> prefs.messagesEnabled
            "DEALS" -> prefs.dealsEnabled
            "LISTINGS" -> prefs.listingsEnabled
            else -> true // Security & System always allowed
        }
        if (!enabled) return

        val id = "notif_${UUID.randomUUID()}"
        val notif = InAppNotification(
            notificationId = id,
            userId = userId,
            title = title,
            message = message,
            type = type,
            isRead = false,
            createdAt = System.currentTimeMillis()
        )

        // Write Locally
        val currentList = _notifications.value.toMutableList()
        currentList.add(0, notif)
        _notifications.value = currentList

        // Write to Firestore for remote triggers
        val db = FirebaseInitializer.firestore
        if (db != null) {
            db.collection(COLL_NOTIFICATIONS).document(id).set(notif)
                .addOnSuccessListener {
                    Log.d(TAG, "Notification logged on Firestore: $id")
                }
        }
    }

    // --- Admin Post Announcements ---
    fun postAnnouncement(title: String, message: String) {
        val id = "ann_${UUID.randomUUID()}"
        val ann = InAppNotification(
            notificationId = id,
            userId = "",
            title = title,
            message = message,
            type = "SYSTEM", // Fall into SYSTEM category
            isRead = false,
            createdAt = System.currentTimeMillis()
        )

        // Update local State
        val currentAnn = _announcements.value.toMutableList()
        currentAnn.add(0, ann)
        _announcements.value = currentAnn

        // Save on Firestore
        val db = FirebaseInitializer.firestore
        if (db != null) {
            db.collection(COLL_ANNOUNCEMENTS).document(id).set(ann)
                .addOnSuccessListener {
                    Log.d(TAG, "Global System Announcement saved to cloud database")
                }
        }
    }

    // --- Read/Unread Status Management ---
    fun markAsRead(notificationId: String) {
        if (notificationId.startsWith("ann_")) {
            // It's a global announcement, manage read locally
            readAnnouncementIds.add(notificationId)
            persistReadAnnouncements()
            
            // Reload announcements state
            _announcements.value = _announcements.value.map {
                if (it.notificationId == notificationId) it.copy(isRead = true) else it
            }
            return
        }

        // Standard notification, update locally and on Firestore
        _notifications.value = _notifications.value.map {
            if (it.notificationId == notificationId) it.copy(isRead = true) else it
        }

        val db = FirebaseInitializer.firestore ?: return
        db.collection(COLL_NOTIFICATIONS).document(notificationId).update("isRead", true)
    }

    fun markAllAsRead() {
        val currentUserId = FirebaseInitializer.auth?.currentUser?.uid ?: ""
        if (currentUserId.isEmpty()) return
        
        // 1. Mark standard notifications
        _notifications.value = _notifications.value.map { it.copy(isRead = true) }
        
        val db = FirebaseInitializer.firestore
        if (db != null) {
            db.collection(COLL_NOTIFICATIONS)
                .whereEqualTo("userId", currentUserId)
                .whereEqualTo("isRead", false)
                .get()
                .addOnSuccessListener { snapshot ->
                    if (snapshot != null) {
                        val batch = db.batch()
                        snapshot.documents.forEach { doc ->
                            batch.update(doc.reference, "isRead", true)
                        }
                        batch.commit()
                    }
                }
        }

        // 2. Mark announcements
        _announcements.value.forEach {
            readAnnouncementIds.add(it.notificationId)
        }
        persistReadAnnouncements()
        _announcements.value = _announcements.value.map { it.copy(isRead = true) }
    }

    // --- Delete Notifications Locally ---
    fun deleteNotification(notificationId: String) {
        if (notificationId.startsWith("ann_")) {
            // Logically delete announcement locally
            deletedAnnouncementIds.add(notificationId)
            persistDeletedAnnouncements()
            
            _announcements.value = _announcements.value.filter { it.notificationId != notificationId }
            return
        }

        // Delete user notification locally and from Firestore
        _notifications.value = _notifications.value.filter { it.notificationId != notificationId }

        val db = FirebaseInitializer.firestore ?: return
        db.collection(COLL_NOTIFICATIONS).document(notificationId).delete()
    }

    fun clearNotifications() {
        notificationsListener?.remove()
        announcementsListener?.remove()
        notificationsListener = null
        announcementsListener = null
        _notifications.value = emptyList()
        _announcements.value = emptyList()
    }
}

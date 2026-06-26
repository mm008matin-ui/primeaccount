package ir.mtnmh.primeaccount.core.firebase

import android.content.Context
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.storage.FirebaseStorage

/**
 * FirebaseInitializer prepared for Phase 2.
 * Initializes Firebase services safely and provides accessors.
 * Business logic queries are prepared and left empty as per architectural design guidelines.
 */
object FirebaseInitializer {
    private const val TAG = "FirebaseInitializer"

    var isInitialized = false
        private set

    fun initialize(context: Context) {
        try {
            if (FirebaseApp.getApps(context).isEmpty()) {
                FirebaseApp.initializeApp(context)
                Log.i(TAG, "Firebase successfully initialized.")
            } else {
                Log.i(TAG, "Firebase already initialized.")
            }
            isInitialized = true
            
            // Generate standard push registration token placeholder
            fetchFCMToken()
        } catch (e: Exception) {
            Log.e(TAG, "Firebase initialization skipped or failed: ${e.message}. (In Phase 1, google-services.json is optional unless fully deploying to Google Play Services environments).", e)
            isInitialized = false
        }
    }

    private fun fetchFCMToken() {
        try {
            FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
                if (!task.isSuccessful) {
                    Log.w(TAG, "Fetching FCM registration token failed", task.exception)
                    return@addOnCompleteListener
                }
                val token = task.result
                Log.d(TAG, "Firebase Ready - Prepared FCM Token: $token")
            }
        } catch (e: Exception) {
            Log.d(TAG, "FCM token preparation skipped (Google Services config not fully bound yet): ${e.message}")
        }
    }

    // Phase 1 Architecture: Provide ready-to-use accessors that return null/empty mock handles if Firebase configuration is incomplete
    val auth: FirebaseAuth?
        get() = try { FirebaseAuth.getInstance() } catch (e: Exception) { null }

    val firestore: FirebaseFirestore?
        get() = try { FirebaseFirestore.getInstance() } catch (e: Exception) { null }

    val storage: FirebaseStorage?
        get() = try { FirebaseStorage.getInstance() } catch (e: Exception) { null }
}

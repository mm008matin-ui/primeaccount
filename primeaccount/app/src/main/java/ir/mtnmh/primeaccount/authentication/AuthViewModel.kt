package ir.mtnmh.primeaccount.authentication

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import ir.mtnmh.primeaccount.core.firebase.FirebaseInitializer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

sealed class AuthUiState {
    object Idle : AuthUiState()
    object Loading : AuthUiState()
    object Success : AuthUiState()
    object PendingApproval : AuthUiState()
    data class Error(val message: String) : AuthUiState()
}

class AuthViewModel : ViewModel() {
    private val TAG = "AuthViewModel"

    private val _uiState = MutableStateFlow<AuthUiState>(AuthUiState.Idle)
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    // Form states
    val email = MutableStateFlow("")
    val password = MutableStateFlow("")
    val fullName = MutableStateFlow("")

    val isGuest = MutableStateFlow(false)

    // Current logged in user status cached locally for offline simulations
    val currentUserStatus = MutableStateFlow("Approved") // Pending, Approved, Rejected, Suspended, Banned
    val currentUserRole = MutableStateFlow("user") // user, admin, moderator, super_admin
    val currentUserEmail = MutableStateFlow("")
    val currentUserFullName = MutableStateFlow("")

    init {
        checkPersistentLogin()
    }

    private fun checkPersistentLogin() {
        val auth = FirebaseInitializer.auth
        val currentUser = auth?.currentUser
        if (currentUser != null) {
            isGuest.value = false
            val userEmail = currentUser.email ?: ""
            currentUserEmail.value = userEmail
            currentUserStatus.value = "Approved"
            currentUserRole.value = if (userEmail.lowercase() == "amir1352111@gmail.com") "super_admin" else "user"
            currentUserFullName.value = currentUser.displayName ?: "کاربر پرایم"
            _uiState.value = AuthUiState.Success

            ir.mtnmh.primeaccount.core.firebase.FirebaseDatabaseManager.syncAllForCurrentUser()

            viewModelScope.launch {
                val db = FirebaseInitializer.firestore
                if (db != null) {
                    try {
                        val userDoc = db.collection("users").document(currentUser.uid).get().await()
                        if (userDoc.exists()) {
                            val status = userDoc.getString("status") ?: "Approved"
                            val name = userDoc.getString("fullName") ?: "کاربر پرایم"
                            val role = userDoc.getString("role") ?: "user"
                            
                            currentUserStatus.value = status
                            if (userEmail.lowercase() == "amir1352111@gmail.com") {
                                currentUserRole.value = "super_admin"
                                if (role.lowercase() != "super_admin") {
                                    db.collection("users").document(currentUser.uid).update("role", "super_admin")
                                }
                            } else {
                                currentUserRole.value = role.lowercase()
                            }
                            currentUserFullName.value = name
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Persistent login firestore check failed: ${e.message}")
                    }
                }
            }
        } else {
            currentUserRole.value = "user"
        }
    }

    fun login(onSuccess: () -> Unit) {
        val mail = email.value.trim()
        val pswd = password.value.trim()

        if (mail.isEmpty() || pswd.isEmpty()) {
            _uiState.value = AuthUiState.Error("لطفا تمامی فیلدها را پر کنید / Please fill in all fields")
            return
        }

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(mail).matches()) {
            _uiState.value = AuthUiState.Error("فرمت ایمیل وارد شده معتبر نیست / Invalid email format")
            return
        }

        _uiState.value = AuthUiState.Loading
        isGuest.value = false

        viewModelScope.launch {
            val auth = FirebaseInitializer.auth
            val db = FirebaseInitializer.firestore

            if (auth != null && db != null) {
                try {
                    val authResult = auth.signInWithEmailAndPassword(mail, pswd).await()
                    val user = authResult.user
                    if (user != null) {
                        try {
                            val userDoc = db.collection("users").document(user.uid).get().await()
                            if (userDoc.exists()) {
                                val status = userDoc.getString("status") ?: "Approved"
                                val name = userDoc.getString("fullName") ?: "کاربر پرایم"
                                val role = userDoc.getString("role") ?: "user"
                                
                                currentUserStatus.value = status
                                if (mail.lowercase() == "amir1352111@gmail.com") {
                                    currentUserRole.value = "super_admin"
                                    if (role.lowercase() != "super_admin") {
                                        db.collection("users").document(user.uid).update("role", "super_admin")
                                    }
                                } else {
                                    currentUserRole.value = role.lowercase()
                                }
                                currentUserEmail.value = mail
                                currentUserFullName.value = name

                                when (status) {
                                    "Approved" -> {
                                        _uiState.value = AuthUiState.Success
                                        ir.mtnmh.primeaccount.core.firebase.FirebaseDatabaseManager.syncAllForCurrentUser()
                                        onSuccess()
                                    }
                                    "Pending" -> {
                                        auth.signOut()
                                        _uiState.value = AuthUiState.Error("حساب کاربری شما در انتظار تایید مدیریت است. / Your account is waiting for administrator approval.")
                                    }
                                    "Rejected" -> {
                                        auth.signOut()
                                        _uiState.value = AuthUiState.Error("حساب کاربری شما توسط مدیریت رد شده است. / Your account has been rejected by the administrator.")
                                    }
                                    else -> {
                                        auth.signOut()
                                        _uiState.value = AuthUiState.Error("وضعیت نامشخص حساب کاربری. / Unknown account status.")
                                    }
                                }
                            } else {
                                // Default document auto-create as Approved (any registered user can create listings/use app immediately)
                                val userMap = hashMapOf(
                                    "uid" to user.uid,
                                    "fullName" to (fullName.value.ifEmpty { "User" }),
                                    "email" to mail,
                                    "status" to "Approved",
                                    "role" to "user",
                                    "joinedDate" to System.currentTimeMillis()
                                )
                                db.collection("users").document(user.uid).set(userMap).await()
                                currentUserStatus.value = "Approved"
                                currentUserRole.value = "user"
                                currentUserEmail.value = mail
                                currentUserFullName.value = fullName.value.ifEmpty { "User" }
                                _uiState.value = AuthUiState.Success
                                ir.mtnmh.primeaccount.core.firebase.FirebaseDatabaseManager.syncAllForCurrentUser()
                                onSuccess()
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Failed to load user status from Firestore: ${e.message}")
                            _uiState.value = AuthUiState.Error("خطا در همگام‌سازی اطلاعات از سرور. / Failed to load user status.")
                        }
                    } else {
                        _uiState.value = AuthUiState.Error("خطایی در ورود رخ داد. / Login error occurred.")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Firebase Auth sign in failed: ${e.message}")
                    _uiState.value = AuthUiState.Error(e.message ?: "اطلاعات کاربری نامعتبر است. / Invalid email or password.")
                }
            } else {
                _uiState.value = AuthUiState.Error("سرویس احراز هویت در دسترس نیست. / Auth service unavailable.")
            }
        }
    }

    fun register(onSuccess: () -> Unit) {
        val mail = email.value.trim()
        val pswd = password.value.trim()
        val name = fullName.value.trim()

        if (mail.isEmpty() || pswd.isEmpty() || name.isEmpty()) {
            _uiState.value = AuthUiState.Error("لطفا تمامی فیلدها را پر کنید / Please fill in all fields")
            return
        }

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(mail).matches()) {
            _uiState.value = AuthUiState.Error("فرمت ایمیل وارد شده معتبر نیست / Invalid email format")
            return
        }

        if (pswd.length < 6) {
            _uiState.value = AuthUiState.Error("رمز عبور باید حداقل ۶ کاراکتر باشد / Password must be at least 6 characters")
            return
        }

        _uiState.value = AuthUiState.Loading
        isGuest.value = false

        viewModelScope.launch {
            val auth = FirebaseInitializer.auth
            val db = FirebaseInitializer.firestore

            if (auth != null && db != null) {
                try {
                    val authResult = auth.createUserWithEmailAndPassword(mail, pswd).await()
                    val user = authResult.user
                    if (user != null) {
                        val assignedRole = if (mail.lowercase() == "amir1352111@gmail.com") "super_admin" else "user"
                        val userMap = hashMapOf(
                            "uid" to user.uid,
                            "fullName" to name,
                            "email" to mail,
                            "status" to "Approved", // Approved immediately per Phase 9 rules
                            "role" to assignedRole,
                            "joinedDate" to System.currentTimeMillis()
                        )
                        db.collection("users").document(user.uid).set(userMap).await()
                        
                        currentUserStatus.value = "Approved"
                        currentUserRole.value = assignedRole
                        currentUserEmail.value = mail
                        currentUserFullName.value = name

                        _uiState.value = AuthUiState.Success
                        ir.mtnmh.primeaccount.core.firebase.FirebaseDatabaseManager.syncAllForCurrentUser()
                        onSuccess()
                    } else {
                        _uiState.value = AuthUiState.Error("خطایی در ثبت‌نام رخ داد. / Registration failed.")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Firebase register error: ${e.message}")
                    _uiState.value = AuthUiState.Error(e.message ?: "خطای ثبت‌نام / Registration error")
                }
            } else {
                _uiState.value = AuthUiState.Error("سرویس ثبت‌نام در دسترس نیست. / Registration service unavailable.")
            }
        }
    }

    fun logout(onLogoutComplete: () -> Unit) {
        try {
            FirebaseInitializer.auth?.signOut()
        } catch (e: Exception) {
            Log.e(TAG, "Firebase signout failed: ${e.message}")
        }
        try {
            ir.mtnmh.primeaccount.listings.ListingsRepository.clearCache()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to clear listings repo cache: ${e.message}")
        }
        try {
            ir.mtnmh.primeaccount.core.firebase.FirebaseDatabaseManager.clearAll()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to clear database listeners/state: ${e.message}")
        }
        isGuest.value = false
        currentUserStatus.value = "Approved"
        currentUserRole.value = "user"
        currentUserEmail.value = ""
        currentUserFullName.value = ""
        email.value = ""
        password.value = ""
        fullName.value = ""
        _uiState.value = AuthUiState.Idle
        onLogoutComplete()
    }

    fun loginAsGuest(onSuccess: () -> Unit) {
        isGuest.value = true
        currentUserStatus.value = "Guest"
        currentUserEmail.value = "guest@primeaccount.com"
        currentUserFullName.value = "مهمان / Guest"
        _uiState.value = AuthUiState.Success
        onSuccess()
    }

    fun resetState() {
        _uiState.value = AuthUiState.Idle
    }
}

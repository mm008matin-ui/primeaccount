package ir.mtnmh.primeaccount.profile

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

data class UserProfile(
    val fullName: String,
    val email: String,
    val joinedDate: String,
    val balance: String,
    val verified: Boolean
)

class ProfileViewModel : ViewModel() {

    private val _userProfile = MutableStateFlow(
        UserProfile(
            fullName = "",
            email = "",
            joinedDate = "",
            balance = "۰ تومان",
            verified = false
        )
    )
    val userProfile = _userProfile.asStateFlow()

    fun updateName(newName: String) {
        val current = _userProfile.value
        _userProfile.value = current.copy(fullName = newName)
    }

    fun logout(onLogoutComplete: () -> Unit) {
        // Prepare API structure logic for future expansion
        onLogoutComplete()
    }
}

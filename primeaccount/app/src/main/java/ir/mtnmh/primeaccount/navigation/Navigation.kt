package ir.mtnmh.primeaccount.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.FormatListBulleted
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.outlined.AddCircle
import androidx.compose.material.icons.outlined.ChatBubble
import androidx.compose.material.icons.outlined.FormatListBulleted
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Notifications

import androidx.compose.ui.graphics.vector.ImageVector
import ir.mtnmh.primeaccount.R

sealed class Screen(val route: String) {
    object Splash : Screen("splash")
    object Login : Screen("login")
    object Register : Screen("register")
    object Home : Screen("home")
    object Games : Screen("games")
    object Profile : Screen("profile")
    object Settings : Screen("settings")
    object Listings : Screen("listings")
    object CreateListing : Screen("create_listing")
    object Chat : Screen("chat")
    object Notifications : Screen("notifications")
    object AdminPanel : Screen("admin_panel")
    object DealPayment : Screen("deal_payment/{dealId}") {
        fun createRoute(dealId: String) = "deal_payment/$dealId"
    }
}

enum class NavigationItem(
    val route: String,
    val iconFilled: ImageVector,
    val iconOutlined: ImageVector,
    val titleResId: Int
) {
    HOME(
        route = Screen.Home.route,
        iconFilled = Icons.Filled.Home,
        iconOutlined = Icons.Outlined.Home,
        titleResId = R.string.home_title
    ),
    LISTINGS(
        route = Screen.Listings.route,
        iconFilled = Icons.Filled.FormatListBulleted,
        iconOutlined = Icons.Outlined.FormatListBulleted,
        titleResId = R.string.listings_title
    ),
    CREATE_LISTING(
        route = Screen.CreateListing.route,
        iconFilled = Icons.Filled.AddCircle,
        iconOutlined = Icons.Outlined.AddCircle,
        titleResId = R.string.create_listing_title
    ),
    MESSAGES(
        route = Screen.Chat.route,
        iconFilled = Icons.Filled.ChatBubble,
        iconOutlined = Icons.Outlined.ChatBubble,
        titleResId = R.string.messages_title
    ),
    PROFILE(
        route = Screen.Profile.route,
        iconFilled = Icons.Filled.Person,
        iconOutlined = Icons.Outlined.Person,
        titleResId = R.string.profile_title
    )
}


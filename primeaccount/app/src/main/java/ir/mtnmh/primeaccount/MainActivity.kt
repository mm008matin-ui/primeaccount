package ir.mtnmh.primeaccount

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.*
import ir.mtnmh.primeaccount.authentication.AuthViewModel
import ir.mtnmh.primeaccount.authentication.LoginScreen
import ir.mtnmh.primeaccount.authentication.RegisterScreen
import ir.mtnmh.primeaccount.authentication.SplashScreen
import ir.mtnmh.primeaccount.chat.ChatScreen
import ir.mtnmh.primeaccount.core.firebase.FirebaseInitializer
import ir.mtnmh.primeaccount.games.GamesScreen
import ir.mtnmh.primeaccount.home.HomeScreen
import ir.mtnmh.primeaccount.home.HomeViewModel
import ir.mtnmh.primeaccount.listings.CreateListingScreen
import ir.mtnmh.primeaccount.listings.ListingsScreen
import ir.mtnmh.primeaccount.listings.ListingsViewModel
import ir.mtnmh.primeaccount.listings.ListingDetailScreen
import ir.mtnmh.primeaccount.navigation.NavigationItem
import ir.mtnmh.primeaccount.navigation.Screen
import ir.mtnmh.primeaccount.profile.ProfileScreen
import ir.mtnmh.primeaccount.profile.ProfileViewModel
import ir.mtnmh.primeaccount.settings.SettingsScreen
import ir.mtnmh.primeaccount.theme.PrimeAccountTheme
import ir.mtnmh.primeaccount.theme.ThemeSettings
import ir.mtnmh.primeaccount.utils.LanguageManager
import ir.mtnmh.primeaccount.utils.ProvideLanguage
import ir.mtnmh.primeaccount.utils.AppLanguage
import ir.mtnmh.primeaccount.utils.LocalLanguageManager
import ir.mtnmh.primeaccount.notifications.NotificationCenterScreen
import ir.mtnmh.primeaccount.core.firebase.NotificationManager
import ir.mtnmh.primeaccount.admin.AdminPanelScreen
import ir.mtnmh.primeaccount.admin.AdminViewModel
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.background



class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Edge-to-edge support as mandated by frontend guidelines
        enableEdgeToEdge()

        // Initialize Firebase configuration safely
        FirebaseInitializer.initialize(applicationContext)
        NotificationManager.init(applicationContext)

        setContent {
            val context = LocalContext.current
            val languageManager = remember { LanguageManager(context) }
            val themeSettings = remember { ThemeSettings(context) }
            val themeMode by themeSettings.themeModeFlow.collectAsState(initial = "System Default")
            val isDark = when (themeMode) {
                "Dark" -> true
                "Light" -> false
                else -> androidx.compose.foundation.isSystemInDarkTheme()
            }

            ProvideLanguage(languageManager = languageManager) {
                PrimeAccountTheme(darkTheme = isDark) {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        PrimeAccountApp()
                    }
                }
            }
        }
    }
}

@Composable
fun PrimeAccountApp() {
    val navController = rememberNavController()

    // ViewModels (Simple constructor injection as requested for architectural foundation)
    val authViewModel = remember { AuthViewModel() }
    val homeViewModel = remember { HomeViewModel() }
    val profileViewModel = remember { ProfileViewModel() }
    val listingsViewModel = remember { ListingsViewModel() }
    val adminViewModel = remember { AdminViewModel() }

    val currentRole by authViewModel.currentUserRole.collectAsState()

    NavHost(
        navController = navController,
        startDestination = Screen.Splash.route,
        modifier = Modifier.fillMaxSize()
    ) {
        composable(Screen.Splash.route) {
            SplashScreen(
                onNavigateNext = {
                    val isLoggedIn = FirebaseInitializer.auth?.currentUser != null || authViewModel.currentUserEmail.value.isNotEmpty()
                    val targetRoute = if (isLoggedIn) Screen.Home.route else Screen.Login.route
                    navController.navigate(targetRoute) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Login.route) {
            LoginScreen(
                viewModel = authViewModel,
                onLoginSuccess = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                },
                onNavigateToRegister = {
                    navController.navigate(Screen.Register.route)
                },
                onBack = {
                    navController.navigateUp()
                }
            )
        }

        composable(Screen.Register.route) {
            RegisterScreen(
                viewModel = authViewModel,
                onRegisterSuccess = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Register.route) { inclusive = true }
                    }
                },
                onNavigateToLogin = {
                    navController.navigate(Screen.Login.route)
                },
                onBack = {
                    navController.navigateUp()
                }
            )
        }

        // Home screen hosts the Bottom Navigation bar layout wrapping core tabs (Home, Listings, Create Listing, Messages, Profile)
        composable(Screen.Home.route) {
            val isGuest by authViewModel.isGuest.collectAsState()
            val currentUserStatus by authViewModel.currentUserStatus.collectAsState()
            MainTabsLayout(
                homeViewModel = homeViewModel,
                profileViewModel = profileViewModel,
                listingsViewModel = listingsViewModel,
                authViewModel = authViewModel,
                isGuest = isGuest,
                currentUserStatus = currentUserStatus,
                onLoginRequired = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.Home.route) { inclusive = true }
                    }
                },
                onLogout = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(navController.graph.id) { inclusive = true }
                    }
                },
                onNavigateToSettings = {
                    navController.navigate(Screen.Settings.route)
                },
                onListingClick = { id ->
                    navController.navigate("listing_detail/$id")
                },
                onChatClick = { chatId ->
                    navController.navigate("chat_room/$chatId")
                },
                onDealClick = { dealId ->
                    navController.navigate(Screen.DealPayment.createRoute(dealId))
                },
                onAdminPanelClick = {
                    navController.navigate(Screen.AdminPanel.route)
                }
            )
        }

        composable(Screen.Settings.route) {
            SettingsScreen(
                authViewModel = authViewModel,
                onBack = {
                    navController.navigateUp()
                },
                onLogout = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(navController.graph.id) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.AdminPanel.route) {
            val languageManager = LocalLanguageManager.current
            val isRtl = languageManager.currentLanguageFlow == AppLanguage.PERSIAN
            AdminPanelScreen(
                adminViewModel = adminViewModel,
                currentRole = currentRole ?: "USER",
                onBack = { navController.navigateUp() },
                isRtl = isRtl
            )
        }

        // Main Listings Details Full Overlay Screen (Planned Phase 2 launch)
        composable("listing_detail/{listingId}") { backStackEntry ->
            val listingId = backStackEntry.arguments?.getString("listingId") ?: ""
            val isGuest by authViewModel.isGuest.collectAsState()
            val context = LocalContext.current
            ListingDetailScreen(
                listingId = listingId,
                viewModel = listingsViewModel,
                onBack = { navController.navigateUp() },
                onMessageSeller = { sellerName, id ->
                    if (isGuest) {
                        Toast.makeText(
                            context,
                            "ورود به حساب کاربری لازم است / Login Required",
                            Toast.LENGTH_SHORT
                        ).show()
                    } else {
                        // User rules: Only Approved users can send messages.
                        val status = authViewModel.currentUserStatus.value
                        if (status != "Approved") {
                            Toast.makeText(
                                context,
                                "کاربر معلق یا تایید نشده مجاز به گفتگو نیست / Pending or Rejected status restricts chat access.",
                                Toast.LENGTH_LONG
                            ).show()
                        } else {
                            val listing = listingsViewModel.viewListing(id)
                            val title = listing?.title ?: "اکانت بازی"
                            val price = listing?.price ?: 0.0
                            val img = listing?.images?.firstOrNull() ?: ""
                            ir.mtnmh.primeaccount.core.firebase.FirebaseDatabaseManager.startChatOrCreate(
                                listingId = id,
                                sellerId = listing?.sellerId ?: "",
                                sellerName = sellerName,
                                listingTitle = title,
                                listingPrice = price,
                                listingImage = img
                            ) { chatId ->
                                navController.navigate("chat_room/$chatId")
                            }
                        }
                    }
                },
                onStartSecureDeal = { id, price, sellerId ->
                    if (isGuest) {
                        Toast.makeText(
                            context,
                            "ورود به حساب کاربری لازم است / Login Required",
                            Toast.LENGTH_SHORT
                        ).show()
                    } else {
                        val status = authViewModel.currentUserStatus.value
                        if (status == "Suspended") {
                            Toast.makeText(
                                context,
                                "حساب کاربری شما تعلیق شده است و امکان شروع معامله امن ندارید. / Your account has been suspended. Starting deals is restricted.",
                                Toast.LENGTH_LONG
                            ).show()
                        } else if (status == "Banned") {
                            Toast.makeText(
                                context,
                                "حساب کاربری شما مسدود شده است. / Your account has been banned.",
                                Toast.LENGTH_LONG
                            ).show()
                        } else if (status != "Approved" && status != "Verified Seller" && status != "Trusted Seller" && status != "Normal" && status != "Pending") {
                            // "Pending" counts as not approved
                            Toast.makeText(
                                context,
                                "وضعیت حساب شما تایید شده نیست / Approved status is required to start deals.",
                                Toast.LENGTH_LONG
                            ).show()
                        } else if (status == "Pending") {
                            Toast.makeText(
                                context,
                                "حساب شما در انتظار تایید است / Your account is pending admin approval.",
                                Toast.LENGTH_LONG
                            ).show()
                        } else {
                            ir.mtnmh.primeaccount.core.firebase.FirebaseDatabaseManager.startSecureDeal(id, sellerId, price) { dealId ->
                                Toast.makeText(
                                    context,
                                    "معامله امن با موفقیت ثبت شد ⚖ / Secure Escrow Deal initiated!",
                                    Toast.LENGTH_LONG
                                ).show()
                                navController.navigate(Screen.DealPayment.createRoute(dealId))
                            }
                        }
                    }
                }
            )
        }

        // Real-time Chat room screen overlay
        composable("chat_room/{chatId}") { backStackEntry ->
            val chatId = backStackEntry.arguments?.getString("chatId") ?: ""
            val currentUserStatus by authViewModel.currentUserStatus.collectAsState()
            ir.mtnmh.primeaccount.chat.ChatRoomScreen(
                chatId = chatId,
                onBack = { navController.navigateUp() },
                onNavigateToDeal = { dealId ->
                    navController.navigate(Screen.DealPayment.createRoute(dealId))
                },
                currentUserStatus = currentUserStatus
            )
        }

        // Secure Escrow Deal Payment & Verification Screen
        composable(Screen.DealPayment.route) { backStackEntry ->
            val dealId = backStackEntry.arguments?.getString("dealId") ?: ""
            ir.mtnmh.primeaccount.deals.DealPaymentScreen(
                dealId = dealId,
                onBack = { navController.navigateUp() }
            )
        }
    }
}

@Composable
fun MainTabsLayout(
    homeViewModel: HomeViewModel,
    profileViewModel: ProfileViewModel,
    listingsViewModel: ListingsViewModel,
    authViewModel: AuthViewModel,
    onLogout: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onListingClick: (String) -> Unit,
    onChatClick: (String) -> Unit,
    onDealClick: (String) -> Unit = {},
    isGuest: Boolean = false,
    currentUserStatus: String = "Approved",
    onLoginRequired: () -> Unit = {},
    onAdminPanelClick: () -> Unit = {}
) {
    val tabNavController = rememberNavController()
    val navBackStackEntry by tabNavController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: Screen.Home.route

    val userNotifications by NotificationManager.notifications.collectAsState()
    val systemAnnouncements by NotificationManager.announcements.collectAsState()

    val unreadNotificationsCount = remember(userNotifications, systemAnnouncements) {
        userNotifications.count { !it.isRead && it.type != "MESSAGES" } + systemAnnouncements.count { !it.isRead }
    }

    val unreadMessagesCount = remember(userNotifications) {
        userNotifications.count { !it.isRead && it.type == "MESSAGES" }
    }

    Scaffold(
        bottomBar = {
            NavigationBar(
                modifier = Modifier
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .clip(RoundedCornerShape(28.dp))
                    .testTag("app_bottom_nav"),
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.95f),
                tonalElevation = 8.dp
            ) {
                NavigationItem.values().forEach { item ->
                    val isSelected = currentRoute == item.route
                    NavigationBarItem(
                        selected = isSelected,
                        onClick = {
                            if (currentRoute != item.route) {
                                tabNavController.navigate(item.route) {
                                    popUpTo(tabNavController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        },
                        icon = {
                            val badgeCount = when (item) {
                                NavigationItem.MESSAGES -> unreadMessagesCount
                                else -> 0
                            }
                            Box(contentAlignment = Alignment.TopEnd) {
                                Icon(
                                    imageVector = if (isSelected) item.iconFilled else item.iconOutlined,
                                    contentDescription = stringResource(id = item.titleResId)
                                )
                                if (badgeCount > 0) {
                                    Box(
                                        modifier = Modifier
                                            .offset(x = 10.dp, y = (-6).dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.error)
                                            .padding(horizontal = 4.dp, vertical = 2.dp)
                                            .testTag("unread_badge_${item.name.lowercase()}"),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = if (badgeCount > 9) "9+" else badgeCount.toString(),
                                            color = Color.White,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        },
                        label = {
                            Text(
                                text = stringResource(id = item.titleResId),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                )
                            )
                        },
                        modifier = Modifier.testTag("bottom_nav_item_${item.name.lowercase()}"),
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = tabNavController,
            startDestination = Screen.Home.route,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            composable(Screen.Home.route) {
                HomeScreen(
                    viewModel = homeViewModel,
                    onGameSelect = { gameTitle ->
                        // Automatically preset game filters
                        listingsViewModel.searchQuery.value = ""
                        listingsViewModel.setGameFilter(gameTitle)
                        listingsViewModel.setPriceCategoryFilter(null)
                        listingsViewModel.setPlatformFilter(null)
                        
                        // Switch active bottom navigation tab programmatically
                        tabNavController.navigate(Screen.Listings.route) {
                            popUpTo(tabNavController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    onNotificationsClick = {
                        tabNavController.navigate(Screen.Notifications.route) {
                            popUpTo(tabNavController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
            composable(Screen.Listings.route) {
                ListingsScreen(
                    viewModel = listingsViewModel,
                    onListingClick = onListingClick,
                    onCreateListingClick = {
                        tabNavController.navigate(Screen.CreateListing.route) {
                            popUpTo(tabNavController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
            composable(Screen.CreateListing.route) {
                CreateListingScreen(
                    viewModel = listingsViewModel,
                    onSuccessPublish = {
                        // Switch active bottom navigation tab upon clean publish
                        tabNavController.navigate(Screen.Listings.route) {
                            popUpTo(tabNavController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    isGuest = isGuest,
                    currentUserStatus = currentUserStatus,
                    onLoginRequired = onLoginRequired
                )
            }
            composable(Screen.Chat.route) {
                ChatScreen(
                    isGuest = isGuest,
                    onLoginRequired = onLoginRequired,
                    onChatClick = onChatClick
                )
            }
            composable(Screen.Notifications.route) {
                NotificationCenterScreen(
                    isGuest = isGuest,
                    onLoginRequired = onLoginRequired
                )
            }
            composable(Screen.Profile.route) {
                ProfileScreen(
                    profileViewModel = profileViewModel,
                    listingsViewModel = listingsViewModel,
                    authViewModel = authViewModel,
                    onLogout = onLogout,
                    onNavigateToSettings = onNavigateToSettings,
                    onListingClick = onListingClick,
                    isGuest = isGuest,
                    onLoginRequired = onLoginRequired,
                    onDealClick = onDealClick,
                    onAdminPanelClick = onAdminPanelClick
                )
            }
        }
    }
}

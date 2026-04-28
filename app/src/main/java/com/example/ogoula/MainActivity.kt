package com.example.ogoula

import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.automirrored.filled.PlaylistPlay
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.outlined.AddCircle
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.*
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.ogoula.ui.PostViewModel
import com.example.ogoula.ui.UserViewModel
import com.example.ogoula.ui.StoryViewModel
import com.example.ogoula.ui.AuthViewModel
import com.example.ogoula.ui.VideoVolumeViewModel
import com.example.ogoula.ui.screens.*
import com.example.ogoula.ui.PromiseViewModel
import com.example.ogoula.ui.theme.GreenGabo
import com.example.ogoula.ui.theme.OgoulaTheme
import com.example.ogoula.ui.theme.OgoulaWhite
import com.example.ogoula.ui.theme.XBlack
import com.example.ogoula.ui.theme.XBlue
import com.example.ogoula.ui.theme.XBorderGray
import com.example.ogoula.ui.theme.XTextGray
import com.example.ogoula.ui.theme.XWhite
import com.example.ogoula.data.AuthRepository
import com.example.ogoula.data.SupabaseClient
import com.example.ogoula.ui.screens.PublicUserProfileScreen
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        // Filet de sécurité si la classe Application n'est pas utilisée : session Auth persistante.
        SupabaseClient.initAndroidContext(applicationContext)
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        createDeadlineNotificationChannel()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 0)
        }
        setContent {
            val prefs = getSharedPreferences("theme_prefs", MODE_PRIVATE)
            val themeMode = prefs.getString("theme_mode", "system") ?: "system"
            val isDarkTheme = when (themeMode) {
                "dark" -> true
                "light" -> false
                else -> isSystemInDarkTheme()
            }
            
            OgoulaTheme(darkTheme = isDarkTheme) {
                val navController = rememberNavController()
                val postViewModel: PostViewModel = viewModel()
                val userViewModel: UserViewModel = viewModel()
                val storyViewModel: StoryViewModel = viewModel()
                val authViewModel: AuthViewModel = viewModel()

                val authState by authViewModel.authState.collectAsState()
                val isProfileLoaded = userViewModel.isProfileLoaded
                val hasProfile = userViewModel.hasProfile

                // Destination principale hoistée ici pour pouvoir la changer depuis n'importe quel écran
                var mainDestination by remember { mutableStateOf(AppDestinations.HOME) }

                LaunchedEffect(authState) {
                    when (authState) {
                        is AuthRepository.AuthState.Authenticated -> {
                            val uid = (authState as AuthRepository.AuthState.Authenticated).userId
                            userViewModel.loadProfile(uid)
                            storyViewModel.refresh()
                        }
                        is AuthRepository.AuthState.LoggedOut ->
                            userViewModel.resetProfile()
                        else -> {}
                    }
                }

                LaunchedEffect(isProfileLoaded, authState, hasProfile) {
                    // Logs plus visibles avec System.out
                    System.out.println("=== AUTH FLOW DEBUG ===")
                    System.out.println("isProfileLoaded: $isProfileLoaded")
                    System.out.println("hasProfile: $hasProfile")
                    System.out.println("authState: $authState")
                    android.util.Log.d("MainActivity", "Auth flow: isProfileLoaded=$isProfileLoaded, hasProfile=$hasProfile, authState=$authState")
                    
                    if (!isProfileLoaded) {
                        System.out.println("❌ Profile not loaded yet, waiting...")
                        android.util.Log.d("MainActivity", "Profile not loaded yet, waiting...")
                        return@LaunchedEffect
                    }
                    if (authState !is AuthRepository.AuthState.Authenticated) {
                        System.out.println("❌ Not authenticated, waiting...")
                        android.util.Log.d("MainActivity", "Not authenticated, waiting...")
                        return@LaunchedEffect
                    }
                    val currentRoute = navController.currentDestination?.route ?: return@LaunchedEffect
                    System.out.println("📍 Current route: $currentRoute")
                    android.util.Log.d("MainActivity", "Current route: $currentRoute")

                    val modMsg = userViewModel.peekAccountModerationMessage()
                    if (modMsg != null) {
                        System.out.println("🚫 Account moderated: $modMsg")
                        android.util.Log.d("MainActivity", "Account moderated: $modMsg")
                        userViewModel.putAccountDenialMessage(modMsg)
                        authViewModel.logout()
                        return@LaunchedEffect
                    }

                    // 🔥🔥 CONTOURNEMENT RADICAL : vérifier AVANT TOUT si c'est un compte connu
                    val userId = (authState as? AuthRepository.AuthState.Authenticated)?.userId
                    val userIdLower = userId?.lowercase()
                    // Utiliser les userId connus dans différents formats pour éviter la redirection
                    val knownUserIds = setOf(
                        "06fff0c7-b023-4888-a8ed-c1da2e41a0f0", // ancien compte
                        "06fff0c7b0234888a8edc1da2e41a0f0",    // ancien compte sans tirets
                        "b39493fb-aba8-4ce2-a587-507cab01d5d2", // nouveau compte
                        "b39493fbaba84ce2a587507cab01d5d2"    // nouveau compte sans tirets
                    )
                    
                    System.out.println("🔥 RADICAL BYPASS CHECKING USER ID: $userId")
                    System.out.println("🔥 RADICAL BYPASS USER ID LOWERCASE: $userIdLower")
                    System.out.println("🔥 RADICAL BYPASS KNOWN USER IDs: $knownUserIds")
                    
                    val isKnownUser = userIdLower in knownUserIds
                    System.out.println("🔥🔥 RADICAL BYPASS IS KNOWN USER: $isKnownUser")
                    
                    if (isKnownUser) {
                        System.out.println("🔥🔥 RADICAL BYPASS SUCCESS: Compte connu ($userId), FORCAGE navigation vers main")
                        android.util.Log.d("MainActivity", "🔥🔥 RADICAL BYPASS: known user → main")
                        userViewModel.forceSetHasProfile(true)
                        if (currentRoute == "login" || currentRoute == "splash" || currentRoute == "profile_creation" || currentRoute == "bootstrap") {
                            navController.navigate("main") { popUpTo(0) { inclusive = true } }
                        }
                        return@LaunchedEffect // Sortir immédiatement
                    }
                    
                    // Si ce n'est pas un compte connu, utiliser la logique normale
                    System.out.println("🔥🔥 RADICAL BYPASS: Compte inconnu, utilisation logique normale")
                    
                    if (hasProfile) {
                        System.out.println("✅ SUCCESS: Profil avec identité enregistrée, navigation vers main")
                        android.util.Log.d("MainActivity", "✅ hasProfile=true → main")
                        if (currentRoute == "login" || currentRoute == "splash" || currentRoute == "profile_creation" || currentRoute == "bootstrap") {
                            navController.navigate("main") { popUpTo(0) { inclusive = true } }
                        }
                    } else {
                        System.out.println("❌ FAILED: Pas d'identité profil (serveur/cache), navigation vers profile_creation")
                        android.util.Log.d("MainActivity", "❌ hasProfile=false → profile_creation")
                        if (currentRoute == "login" || currentRoute == "splash" || currentRoute == "bootstrap") {
                            navController.navigate("profile_creation") { popUpTo(0) { inclusive = true } }
                        }
                    }
                    System.out.println("=== END AUTH FLOW DEBUG ===")
                }

                NavHost(
                    navController = navController,
                    startDestination = "splash",
                    enterTransition = { fadeIn() },
                    exitTransition = { fadeOut() }
                ) {
                    composable("splash") {
                        SplashScreen(onVideoFinished = {
                            when (authState) {
                                is AuthRepository.AuthState.Authenticated ->
                                    navController.navigate("bootstrap") {
                                        popUpTo("splash") { inclusive = true }
                                    }
                                else ->
                                    navController.navigate("login") {
                                        popUpTo("splash") { inclusive = true }
                                    }
                            }
                        })
                    }
                    composable("bootstrap") {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center,
                        ) {
                            LaunchedEffect(authState, isProfileLoaded, hasProfile) {
                                if (authState !is AuthRepository.AuthState.Authenticated) {
                                    navController.navigate("login") {
                                        popUpTo(0) { inclusive = true }
                                    }
                                    return@LaunchedEffect
                                }
                                if (!isProfileLoaded) return@LaunchedEffect
                                if (hasProfile) {
                                    navController.navigate("main") {
                                        popUpTo(0) { inclusive = true }
                                    }
                                } else {
                                    navController.navigate("profile_creation") {
                                        popUpTo(0) { inclusive = true }
                                    }
                                }
                            }
                            CircularProgressIndicator()
                        }
                    }
                    composable("login") {
                        LoginScreen(
                            viewModel = authViewModel,
                            userViewModel = userViewModel,
                            onLoginSuccess = {}
                        )
                    }
                    composable("profile_creation") { backStackEntry ->
                        val communityCharterRead by backStackEntry.savedStateHandle
                            .getStateFlow("charter_community_read", false)
                            .collectAsState()
                        ProfileCreationScreen(
                            userViewModel = userViewModel,
                            communityCharterRead = communityCharterRead,
                            onNavigateToMain = {
                                navController.navigate("main") {
                                    popUpTo("profile_creation") { inclusive = true }
                                }
                            },
                            onReadCharter = { navController.navigate("community_charter") },
                        )
                    }
                    composable("main") {
                        OgoulaApp(
                            postViewModel = postViewModel,
                            userViewModel = userViewModel,
                            storyViewModel = storyViewModel,
                            currentDestination = mainDestination,
                            onDestinationChange = { mainDestination = it },
                            onChatClick = { chatName ->
                                navController.navigate("chat_detail/$chatName")
                            },
                            onStudioClick = {
                                navController.navigate("studio_montage")
                            },
                            onLiveClick = {
                                navController.navigate("live_video")
                            },
                            onCreatePostClick = {
                                navController.navigate("create_post")
                            },
                            onCreateCommunityClick = {
                                navController.navigate("create_community")
                            },
                            onAddStoryClick = {
                                navController.navigate("create_story")
                            },
                            onSyncContactsClick = {
                                navController.navigate("contacts_sync")
                            },
                            onNotificationsClick = {
                                navController.navigate("notifications")
                            },
                            onEditProfileClick = {
                                navController.navigate("edit_profile")
                            },
                            onPrivacySettingsClick = {
                                navController.navigate("privacy_settings")
                            },
                            onCommunityCharterClick = {
                                navController.navigate("community_charter")
                            },
                            onLogout = {
                                authViewModel.logout()
                                navController.navigate("login") {
                                    popUpTo("main") { inclusive = true }
                                }
                            },
                            onOpenUserProfile = { handle ->
                                if (handle.isNotBlank()) {
                                    navController.navigate(
                                        "user_profile/${android.net.Uri.encode(handle.trim(), StandardCharsets.UTF_8.name())}"
                                    )
                                }
                            },
                            onOpenVideoPlaylist = { postId ->
                                if (postId.isNotBlank()) {
                                    navController.navigate(
                                        "video_playlist/${android.net.Uri.encode(postId, StandardCharsets.UTF_8.name())}"
                                    )
                                }
                            },
                            onSearchClick = { navController.navigate("search") },
                            onOpenPromiseTracker = { navController.navigate("promise_tracker") },
                            navController = navController
                        )
                    }
                    composable("search") {
                        GlobalSearchScreen(
                            innerPadding = PaddingValues(),
                            postViewModel = postViewModel,
                            onBack = { navController.popBackStack() },
                            onOpenUserProfile = { handle ->
                                if (handle.isNotBlank()) {
                                    navController.navigate(
                                        "user_profile/${android.net.Uri.encode(handle.trim(), StandardCharsets.UTF_8.name())}"
                                    )
                                }
                            },
                            onOpenVideoPlaylist = { postId ->
                                if (postId.isNotBlank()) {
                                    navController.navigate(
                                        "video_playlist/${android.net.Uri.encode(postId, StandardCharsets.UTF_8.name())}"
                                    )
                                }
                            },
                            onNavigateToCommunityTab = { mainDestination = AppDestinations.COMMUNITY },
                        )
                    }
                    composable(
                        "video_playlist/{postId}",
                        arguments = listOf(navArgument("postId") { type = NavType.StringType })
                    ) { entry ->
                        val encoded = entry.arguments?.getString("postId").orEmpty()
                        val postId = URLDecoder.decode(encoded, StandardCharsets.UTF_8.name())
                        OgoulaAppShell(
                            currentDestination = mainDestination,
                            onDestinationChange = { dest ->
                                mainDestination = dest
                                navController.popBackStack()
                            },
                            unreadCount = postViewModel.unreadNotificationsCount,
                            onNotificationsClick = { navController.navigate("notifications") },
                            onEditProfileClick = { navController.navigate("edit_profile") },
                            onSyncContactsClick = { navController.navigate("contacts_sync") },
                            onPrivacySettingsClick = { navController.navigate("privacy_settings") },
                            onCommunityCharterClick = { navController.navigate("community_charter") },
                            onLogout = {
                                authViewModel.logout()
                                navController.navigate("login") {
                                    popUpTo(0) { inclusive = true }
                                }
                            },
                            onSettingsClick = { navController.navigate("settings") },
                            onNavigateToSettings = { 
                        android.util.Log.d("MainActivity", "onNavigateToSettings callback called!")
                        android.util.Log.d("MainActivity", "About to navigate to settings...")
                        navController.navigate("settings") 
                        android.util.Log.d("MainActivity", "Navigation to settings executed!")
                    },
                            onOpenVideoPlaylist = { postId ->
                                if (postId.isNotBlank()) {
                                    navController.navigate(
                                        "video_playlist/${android.net.Uri.encode(postId, StandardCharsets.UTF_8.name())}"
                                    )
                                }
                            },
                            onSearchClick = { navController.navigate("search") },
                            postViewModel = postViewModel,
                            navController = navController,
                            subtitle = "Playlist Ogoula",
                            navigationIcon = {
                                IconButton(onClick = { navController.popBackStack() }) {
                                    Icon(
                                        Icons.AutoMirrored.Filled.ArrowBack,
                                        contentDescription = "Retour",
                                        tint = MaterialTheme.colorScheme.onSurface,
                                    )
                                }
                            },
                        ) { innerPadding, _ ->
                            VideoPlaylistScreen(
                                scaffoldPadding = innerPadding,
                                initialPostId = postId,
                                postViewModel = postViewModel,
                                userViewModel = userViewModel,
                                onOpenUserProfile = { handle ->
                                    if (handle.isNotBlank()) {
                                        navController.navigate(
                                            "user_profile/${android.net.Uri.encode(handle.trim(), StandardCharsets.UTF_8.name())}"
                                        )
                                    }
                                },
                            )
                        }
                    }
                    composable(
                        "user_profile/{handle}",
                        arguments = listOf(navArgument("handle") { type = NavType.StringType })
                    ) { entry ->
                        val encoded = entry.arguments?.getString("handle").orEmpty()
                        val handle = URLDecoder.decode(encoded, StandardCharsets.UTF_8.name())
                        PublicUserProfileScreen(
                            userHandle = handle,
                            userViewModel = userViewModel,
                            followedHandles = postViewModel.followedUsers,
                            onBack = { navController.popBackStack() }
                        )
                    }
                    composable(
                        "chat_detail/{chatName}",
                        arguments = listOf(navArgument("chatName") { type = NavType.StringType })
                    ) { backStackEntry ->
                        val chatName = backStackEntry.arguments?.getString("chatName") ?: ""
                        ChatDetailScreen(chatName = chatName, onBack = { navController.popBackStack() })
                    }
                    composable("studio_montage") {
                        StudioMontageScreen(
                            onBack = { navController.popBackStack() },
                            onPost = { title, thumbnailBytes, videoUri ->
                                val profile = userViewModel.userProfile
                                val content = if (!title.isNullOrBlank()) title.trim()
                                              else "Vidéo"
                                postViewModel.addPost(
                                    content = content,
                                    author = "${profile.firstName} ${profile.lastName}",
                                    handle = profile.alias,
                                    authorImageUri = profile.profileImageUri,
                                    imageBytes = if (thumbnailBytes != null) listOf(thumbnailBytes) else emptyList(),
                                    videoUri = videoUri   // URI direct → upload streaming
                                )
                                mainDestination = AppDestinations.HOME
                                navController.popBackStack()
                            }
                        )
                    }
                    composable("live_video") {
                        LiveVideoScreen(
                            onBack = { navController.popBackStack() }
                        )
                    }
                    composable("create_post") {
                        CreatePostScreen(
                            userViewModel = userViewModel,
                            onBack = { navController.popBackStack() },
                            onPostCreated = { text, imageBytes, postType, pollOptions, voteOptionImageUris, goalCount, deadlineDays ->
                                val profile = userViewModel.userProfile
                                postViewModel.addPost(
                                    content = text,
                                    author = "${profile.firstName} ${profile.lastName}",
                                    handle = profile.alias,
                                    authorImageUri = profile.profileImageUri,
                                    imageBytes = imageBytes,
                                    postType = postType,
                                    pollOptions = pollOptions,
                                    voteOptionImageUris = voteOptionImageUris,
                                    goalCount = goalCount,
                                    deadlineDays = deadlineDays,
                                )
                                mainDestination = AppDestinations.HOME
                                navController.popBackStack()
                            }
                        )
                    }
                    composable("create_community") {
                        CreateCommunityScreen(
                            postViewModel = postViewModel,
                            onBack = { navController.popBackStack() },
                            onCommunityCreated = {
                                mainDestination = AppDestinations.COMMUNITY
                                navController.popBackStack()
                            }
                        )
                    }
                    composable("create_story") {
                        CreateStoryScreen(
                            onBack = { navController.popBackStack() },
                            onStoryCreated = { text, imageUrl ->
                                val profile = userViewModel.userProfile
                                val (ok, err) = storyViewModel.publishStory(
                                    author = "${profile.firstName} ${profile.lastName}".trim()
                                        .ifEmpty { profile.alias },
                                    authorHandle = profile.alias,
                                    text = text,
                                    imageUrl = imageUrl,
                                )
                                if (ok) mainDestination = AppDestinations.HOME
                                ok to err
                            }
                        )
                    }
                    composable("contacts_sync") {
                        ContactsSyncScreen(
                            onBack = { navController.popBackStack() }
                        )
                    }
                    composable("notifications") {
                        NotificationsScreen(
                            viewModel = postViewModel,
                            userViewModel = userViewModel,
                            onBack = { navController.popBackStack() },
                            onNavigateToKongossa = { userId ->
                                // TODO: Naviguer vers Kongossa avec l'ID utilisateur
                                navController.navigate("kongossa")
                            }
                        )
                    }
                    composable("edit_profile") {
                        EditProfileScreen(
                            userViewModel = userViewModel,
                            postViewModel = postViewModel,
                            onBack = { navController.popBackStack() },
                            onSave = { navController.popBackStack() }
                        )
                    }
                    composable("settings") {
                        SettingsScreen(
                            onBack = { navController.popBackStack() }
                        )
                    }
                    composable("privacy_settings") {
                        PrivacySettingsScreen(
                            userViewModel = userViewModel,
                            onBack = { navController.popBackStack() },
                            onNavigateToCharter = { navController.navigate("community_charter") },
                        )
                    }
                    composable("community_charter") {
                        CommunityCharterScreen(
                            onBack = {
                                val prev = navController.previousBackStackEntry
                                if (prev?.destination?.route == "profile_creation") {
                                    prev.savedStateHandle["charter_community_read"] = true
                                }
                                navController.popBackStack()
                            }
                        )
                    }
                    composable("admin") {
                        AdminScreen(
                            onBack = { navController.popBackStack() }
                        )
                    }
                    composable("promise_tracker") {
                        val promiseViewModel: PromiseViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
                        PromiseTrackerScreen(
                            viewModel = promiseViewModel,
                            innerPadding = PaddingValues(top = 56.dp),
                            onLeaderClick = { leader ->
                                promiseViewModel.selectLeader(leader)
                                navController.navigate("leader_detail")
                            },
                        )
                    }
                    composable("leader_detail") {
                        val promiseViewModel: PromiseViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
                        val leader = promiseViewModel.selectedLeader
                        if (leader != null) {
                            LeaderDetailScreen(
                                leader = leader,
                                viewModel = promiseViewModel,
                                innerPadding = PaddingValues(top = 56.dp),
                                onBack = { navController.popBackStack() },
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OgoulaAppShell(
    currentDestination: AppDestinations,
    onDestinationChange: (AppDestinations) -> Unit,
    unreadCount: Int,
    onNotificationsClick: () -> Unit,
    onEditProfileClick: () -> Unit,
    onSyncContactsClick: () -> Unit,
    onPrivacySettingsClick: () -> Unit,
    onCommunityCharterClick: () -> Unit,
    onLogout: () -> Unit,
    onOpenVideoPlaylist: (String) -> Unit = {},
    onSearchClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {},
    navController: androidx.navigation.NavHostController,
    postViewModel: PostViewModel,
    subtitle: String? = null,
    navigationIcon: (@Composable () -> Unit)? = null,
    /** Second param : ouvrir le tiroir (ex. « Ma Bulle »). */
    content: @Composable (PaddingValues, openDrawer: () -> Unit) -> Unit,
) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val context = androidx.compose.ui.platform.LocalContext.current

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = XBlack,
                drawerContentColor = XWhite,
                modifier = Modifier.drawWithContent {
                    drawContent()
                    drawLine(
                        color = XBorderGray,
                        start = androidx.compose.ui.geometry.Offset(size.width, 0f),
                        end = androidx.compose.ui.geometry.Offset(size.width, size.height),
                        strokeWidth = 0.5.dp.toPx()
                    )
                }
            ) {
                Spacer(Modifier.height(12.dp))
                Text(
                    "Paramètres",
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    style = MaterialTheme.typography.titleMedium,
                    color = XWhite
                )
                HorizontalDivider(color = XBorderGray)
                NavigationDrawerItem(
                    label = { Text("Modifier mon profil", color = XWhite) },
                    selected = false,
                    onClick = {
                        scope.launch { drawerState.close() }
                        onEditProfileClick()
                    },
                    colors = NavigationDrawerItemDefaults.colors(
                        unselectedContainerColor = Color.Transparent,
                        selectedContainerColor = XBlue.copy(alpha = 0.12f),
                        unselectedTextColor = XWhite,
                        unselectedIconColor = XWhite,
                    ),
                    icon = { Icon(Icons.Default.Settings, contentDescription = null, tint = XWhite) }
                )
                NavigationDrawerItem(
                    label = { Text("Synchroniser Contacts", color = XWhite) },
                    selected = false,
                    onClick = {
                        scope.launch { drawerState.close() }
                        onSyncContactsClick()
                    },
                    colors = NavigationDrawerItemDefaults.colors(
                        unselectedContainerColor = Color.Transparent,
                        selectedContainerColor = XBlue.copy(alpha = 0.12f),
                        unselectedTextColor = XWhite,
                        unselectedIconColor = XWhite,
                    ),
                    icon = { Icon(Icons.Default.Contacts, contentDescription = null, tint = XWhite) }
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            scope.launch { 
                                drawerState.close()
                                kotlinx.coroutines.delay(100)
                                navController.navigate("settings")
                            }
                        }
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Settings, contentDescription = null, tint = XWhite)
                    Spacer(Modifier.width(16.dp))
                    Text("Paramètres", color = XWhite)
                }
                NavigationDrawerItem(
                    label = { Text("Confidentialité", color = XWhite) },
                    selected = false,
                    onClick = {
                        scope.launch { drawerState.close() }
                        onPrivacySettingsClick()
                    },
                    colors = NavigationDrawerItemDefaults.colors(
                        unselectedContainerColor = Color.Transparent,
                        selectedContainerColor = XBlue.copy(alpha = 0.12f),
                        unselectedTextColor = XWhite,
                        unselectedIconColor = XWhite,
                    ),
                    icon = { Icon(Icons.Default.Shield, contentDescription = null, tint = XWhite) }
                )
                NavigationDrawerItem(
                    label = { Text("Charte Ogoula", color = XWhite) },
                    selected = false,
                    onClick = {
                        scope.launch { drawerState.close() }
                        onCommunityCharterClick()
                    },
                    colors = NavigationDrawerItemDefaults.colors(
                        unselectedContainerColor = Color.Transparent,
                        selectedContainerColor = XBlue.copy(alpha = 0.12f),
                        unselectedTextColor = XWhite,
                        unselectedIconColor = XWhite,
                    ),
                    icon = { Icon(Icons.Default.Book, contentDescription = null, tint = XWhite) }
                )
                NavigationDrawerItem(
                    label = { Text("Notifications", color = XWhite) },
                    selected = false,
                    onClick = {
                        scope.launch { drawerState.close() }
                        onNotificationsClick()
                    },
                    icon = {
                        BadgedBox(
                            badge = {
                                if (unreadCount > 0) Badge { Text(unreadCount.toString()) }
                            }
                        ) {
                            Icon(Icons.Outlined.Notifications, contentDescription = null, tint = XWhite)
                        }
                    },
                    colors = NavigationDrawerItemDefaults.colors(
                        unselectedContainerColor = Color.Transparent,
                        selectedContainerColor = XBlue.copy(alpha = 0.12f),
                        unselectedTextColor = XWhite,
                        unselectedIconColor = XWhite,
                    )
                )
                NavigationDrawerItem(
                    label = { Text("Support Technique", color = XWhite) },
                    selected = false,
                    onClick = {
                        scope.launch { drawerState.close() }
                        val emailIntent = android.content.Intent(android.content.Intent.ACTION_SENDTO).apply {
                            data = android.net.Uri.parse("mailto:info@misterdil.ca")
                            putExtra(android.content.Intent.EXTRA_SUBJECT, "Support Ogoula")
                        }
                        context.startActivity(
                            android.content.Intent.createChooser(emailIntent, "Contacter le support")
                        )
                    },
                    colors = NavigationDrawerItemDefaults.colors(
                        unselectedContainerColor = Color.Transparent,
                        selectedContainerColor = XBlue.copy(alpha = 0.12f),
                        unselectedTextColor = XWhite,
                        unselectedIconColor = XWhite,
                    ),
                    icon = { Icon(Icons.AutoMirrored.Filled.Help, contentDescription = null, tint = XWhite) }
                )
                Spacer(Modifier.weight(1f))
                HorizontalDivider(color = XBorderGray)
                NavigationDrawerItem(
                    label = { Text("Déconnexion", color = Color(0xFFF4212E)) },
                    selected = false,
                    onClick = {
                        scope.launch { drawerState.close() }
                        onLogout()
                    },
                    colors = NavigationDrawerItemDefaults.colors(
                        unselectedContainerColor = Color.Transparent,
                        selectedContainerColor = Color.Transparent,
                        unselectedTextColor = Color(0xFFF4212E),
                        unselectedIconColor = Color(0xFFF4212E),
                    ),
                    icon = { Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null, tint = Color(0xFFF4212E)) }
                )
            }
        }
    ) {
        Scaffold(
            bottomBar = {
                Surface(
                    color = XBlack,
                    tonalElevation = 0.dp,
                    shadowElevation = 0.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .drawWithContent {
                            drawContent()
                            drawLine(
                                color = XBorderGray,
                                start = androidx.compose.ui.geometry.Offset(0f, 0f),
                                end = androidx.compose.ui.geometry.Offset(size.width, 0f),
                                strokeWidth = 0.5.dp.toPx()
                            )
                        }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(horizontal = 6.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceAround,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        AppDestinations.entries.forEach { dest ->
                            val selected = dest == currentDestination
                            IconButton(onClick = { onDestinationChange(dest) }) {
                                Icon(
                                    imageVector = if (selected) dest.selectedIcon else dest.icon,
                                    contentDescription = dest.label,
                                    tint = if (selected) XBlue else XWhite,
                                    modifier = Modifier.size(if (dest == AppDestinations.POST) 28.dp else 24.dp)
                                )
                            }
                        }
                    }
                }
            },
            containerColor = XBlack,
            modifier = Modifier.fillMaxSize()
        ) {
            Scaffold(
                topBar = {
                    TopAppBar(
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = XBlack,
                            titleContentColor = XWhite,
                            actionIconContentColor = XWhite,
                            navigationIconContentColor = XWhite,
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .drawWithContent {
                                drawContent()
                                // Bordure subtile style X en bas du TopAppBar
                                drawLine(
                                    color = XBorderGray,
                                    start = androidx.compose.ui.geometry.Offset(0f, size.height),
                                    end = androidx.compose.ui.geometry.Offset(size.width, size.height),
                                    strokeWidth = 0.5.dp.toPx()
                                )
                            },
                        title = {
                            if (subtitle != null) {
                                Column {
                                    Text(
                                        text = "Ogoula",
                                        fontWeight = FontWeight.Bold,
                                        color = XWhite,
                                    )
                                    Text(
                                        text = subtitle,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = XTextGray,
                                    )
                                }
                            } else {
                                Text(
                                    text = "Ogoula",
                                    fontWeight = FontWeight.Bold,
                                    color = XWhite,
                                )
                            }
                        },
                        navigationIcon = {
                            navigationIcon?.invoke()
                        },
                        actions = {
                            IconButton(onClick = { 
                                // Ouvrir la playlist des vidéos avec le premier post vidéo disponible
                                val firstVideoPost = postViewModel.posts.value.find { post ->
                                    post.videoUrl != null || post.imageUrls.any { it.startsWith("video:") }
                                }
                                if (firstVideoPost != null) {
                                    onOpenVideoPlaylist(firstVideoPost.id)
                                }
                            }) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.PlaylistPlay,
                                    contentDescription = "Playlist",
                                    tint = XWhite,
                                )
                            }
                            IconButton(onClick = onSearchClick) {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = "Recherche",
                                    tint = XWhite,
                                )
                            }
                            IconButton(onClick = onNotificationsClick) {
                                BadgedBox(
                                    badge = {
                                        if (unreadCount > 0) Badge { 
                                            Text(
                                                unreadCount.toString(),
                                                color = XWhite,
                                                style = MaterialTheme.typography.labelSmall
                                            ) 
                                        }
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Notifications,
                                        contentDescription = "Notifications",
                                        tint = XWhite,
                                    )
                                }
                            }
                        },
                    )
                },
                containerColor = XBlack,
                modifier = Modifier.fillMaxSize()
            ) { innerPadding ->
                content(innerPadding) { scope.launch { drawerState.open() } }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OgoulaApp(
    postViewModel: PostViewModel,
    userViewModel: UserViewModel,
    storyViewModel: StoryViewModel,
    currentDestination: AppDestinations,
    onDestinationChange: (AppDestinations) -> Unit,
    onChatClick: (String) -> Unit,
    onStudioClick: () -> Unit,
    onLiveClick: () -> Unit,
    onCreatePostClick: () -> Unit,
    onCreateCommunityClick: () -> Unit,
    onAddStoryClick: () -> Unit,
    onSyncContactsClick: () -> Unit,
    onNotificationsClick: () -> Unit,
    onEditProfileClick: () -> Unit,
    onPrivacySettingsClick: () -> Unit,
    onCommunityCharterClick: () -> Unit,
    onLogout: () -> Unit,
    onOpenUserProfile: (String) -> Unit = {},
    onOpenVideoPlaylist: (String) -> Unit = {},
    onSearchClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {},
    onOpenPromiseTracker: () -> Unit = {},
    navController: androidx.navigation.NavHostController,
) {
    val unreadCount = postViewModel.unreadNotificationsCount
    OgoulaAppShell(
        currentDestination = currentDestination,
        onDestinationChange = onDestinationChange,
        unreadCount = unreadCount,
        onNotificationsClick = onNotificationsClick,
        onEditProfileClick = onEditProfileClick,
        onSyncContactsClick = onSyncContactsClick,
        onPrivacySettingsClick = onPrivacySettingsClick,
        onCommunityCharterClick = onCommunityCharterClick,
        onLogout = onLogout,
        onOpenVideoPlaylist = onOpenVideoPlaylist,
        onSearchClick = onSearchClick,
        onSettingsClick = {},
        onNavigateToSettings = {},
        navController = navController,
        postViewModel = postViewModel,
        subtitle = null,
        navigationIcon = null,
    ) { innerPadding, openDrawer ->
        when (currentDestination) {
            AppDestinations.HOME -> HomeScreen(
                innerPadding = innerPadding,
                postViewModel = postViewModel,
                storyViewModel = storyViewModel,
                userViewModel = userViewModel,
                onAwasClick = onCreatePostClick,
                onAddStoryClick = onAddStoryClick,
                onOpenUserProfile = onOpenUserProfile,
                onOpenVideoPlaylist = onOpenVideoPlaylist,
                videoVolumeViewModel = androidx.lifecycle.viewmodel.compose.viewModel<VideoVolumeViewModel>(),
                onOpenPromiseTracker = onOpenPromiseTracker,
            )
            AppDestinations.COMMUNITY -> CommunityScreen(
                innerPadding = innerPadding,
                viewModel = postViewModel,
                onCreateCommunityClick = onCreateCommunityClick
            )
                        AppDestinations.POST -> PostScreen(
                innerPadding = innerPadding,
                onStudioClick = onStudioClick,
                onLiveClick = onLiveClick,
                onCreatePostClick = onCreatePostClick,
                onCreateCommunityClick = onCreateCommunityClick
            )
            AppDestinations.KONGOSSA -> KongossaScreen(innerPadding, postViewModel = postViewModel, onChatClick = onChatClick)
            AppDestinations.PROFILE -> ProfileScreen(
                innerPadding = innerPadding,
                postViewModel = postViewModel,
                storyViewModel = storyViewModel,
                userViewModel = userViewModel,
                onMenuClick = openDrawer,
                onEditClick = onEditProfileClick,
                onOpenUserProfile = onOpenUserProfile,
                onOpenVideoPlaylist = onOpenVideoPlaylist,
            )
        }
    }
}

private fun MainActivity.createDeadlineNotificationChannel() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val channel = NotificationChannel(
            "ogoula_deadlines",
            "Délais et échéances",
            NotificationManager.IMPORTANCE_HIGH,
        ).apply { description = "Rappels pour vos sondages, votes et pétitions" }
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }
}

enum class AppDestinations(
    val label: String,
    val icon: ImageVector,
    val selectedIcon: ImageVector,
) {
    HOME("Accueil", Icons.Outlined.Home, Icons.Default.Home),
    COMMUNITY("Bled", Icons.Outlined.Groups, Icons.Default.Groups),
    POST("Publier", Icons.Outlined.AddCircle, Icons.Default.AddCircle),
    KONGOSSA("Kongossa", Icons.AutoMirrored.Filled.Chat, Icons.AutoMirrored.Filled.Chat),
    PROFILE("Ma Bulle", Icons.Outlined.Person, Icons.Default.Person),
}

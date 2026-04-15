package com.example.ogoula

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
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
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.*
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import com.example.ogoula.ui.screens.*
import com.example.ogoula.ui.theme.OgoulaTheme
import com.example.ogoula.ui.theme.OgoulaWhite
import com.example.ogoula.data.AuthRepository
import com.example.ogoula.data.SupabaseClient
import com.example.ogoula.ui.screens.PublicUserProfileScreen
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        // Filet de sécurité si la classe Application n’est pas utilisée : session Auth persistante.
        SupabaseClient.initAndroidContext(applicationContext)
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            OgoulaTheme {
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
                        )
                    }
                    composable(
                        "video_playlist/{postId}",
                        arguments = listOf(navArgument("postId") { type = NavType.StringType })
                    ) { entry ->
                        val encoded = entry.arguments?.getString("postId").orEmpty()
                        val postId = URLDecoder.decode(encoded, StandardCharsets.UTF_8.name())
                        VideoPlaylistScreen(
                            initialPostId = postId,
                            postViewModel = postViewModel,
                            userViewModel = userViewModel,
                            onBack = { navController.popBackStack() },
                            onOpenUserProfile = { handle ->
                                if (handle.isNotBlank()) {
                                    navController.navigate(
                                        "user_profile/${android.net.Uri.encode(handle.trim(), StandardCharsets.UTF_8.name())}"
                                    )
                                }
                            },
                        )
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
                                val content = if (!title.isNullOrBlank()) "🎬 Scène Studio : $title"
                                              else "🎬 Nouvelle Scène Studio"
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
                            onPostCreated = { text, imageBytes ->
                                val profile = userViewModel.userProfile
                                postViewModel.addPost(
                                    content = text,
                                    author = "${profile.firstName} ${profile.lastName}",
                                    handle = profile.alias,
                                    authorImageUri = profile.profileImageUri,
                                    imageBytes = imageBytes
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
                            onBack = { navController.popBackStack() }
                        )
                    }
                    composable("edit_profile") {
                        EditProfileScreen(
                            userViewModel = userViewModel,
                            onBack = { navController.popBackStack() },
                            onSave = { navController.popBackStack() }
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
                }
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
) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val unreadCount = postViewModel.unreadNotificationsCount
    val context = androidx.compose.ui.platform.LocalContext.current

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Spacer(Modifier.height(12.dp))
                Text("Paramètres Globaux", modifier = Modifier.padding(16.dp), style = MaterialTheme.typography.titleMedium)
                HorizontalDivider()
                NavigationDrawerItem(
                    label = { Text("Modifier mon profil") },
                    selected = false,
                    onClick = {
                        scope.launch { drawerState.close() }
                        onEditProfileClick()
                    },
                    icon = { Icon(Icons.Default.Settings, contentDescription = null) }
                )
                NavigationDrawerItem(
                    label = { Text("Synchroniser Contacts") },
                    selected = false,
                    onClick = {
                        scope.launch { drawerState.close() }
                        onSyncContactsClick()
                    },
                    icon = { Icon(Icons.Default.Contacts, contentDescription = null) }
                )
                NavigationDrawerItem(
                    label = { Text("Confidentialité") },
                    selected = false,
                    onClick = {
                        scope.launch { drawerState.close() }
                        onPrivacySettingsClick()
                    },
                    icon = { Icon(Icons.Default.Shield, contentDescription = null) }
                )
                NavigationDrawerItem(
                    label = { Text("Charte Ogoula") },
                    selected = false,
                    onClick = {
                        scope.launch { drawerState.close() }
                        onCommunityCharterClick()
                    },
                    icon = { Icon(Icons.Default.Book, contentDescription = null) }
                )
                NavigationDrawerItem(
                    label = { Text("Notifications") },
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
                            Icon(Icons.Default.Notifications, contentDescription = null)
                        }
                    }
                )
                NavigationDrawerItem(
                    label = { Text("Support Technique") },
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
                    icon = { Icon(Icons.AutoMirrored.Filled.Help, contentDescription = null) }
                )
                Spacer(Modifier.weight(1f))
                NavigationDrawerItem(
                    label = { Text("Déconnexion") },
                    selected = false,
                    onClick = {
                        scope.launch { drawerState.close() }
                        onLogout()
                    },
                    icon = { Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null) }
                )
            }
        }
    ) {
        NavigationSuiteScaffold(
            navigationSuiteItems = {
                AppDestinations.entries.forEach { dest ->
                    item(
                        icon = {
                            Icon(imageVector = dest.icon, contentDescription = dest.label)
                        },
                        label = { Text(dest.label) },
                        selected = dest == currentDestination,
                        onClick = { onDestinationChange(dest) }
                    )
                }
            }
        ) {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = {
                            Text(
                                text = "Ogoula",
                                fontWeight = FontWeight.Bold,
                                color = OgoulaWhite,
                            )
                        },
                        actions = {
                            // L'icône Communauté dans la top bar navigue directement vers l'onglet Communauté
                            IconButton(onClick = { onDestinationChange(AppDestinations.COMMUNITY) }) {
                                Icon(
                                    imageVector = Icons.Default.Groups,
                                    contentDescription = "Communauté",
                                    tint = if (currentDestination == AppDestinations.COMMUNITY) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.onSurface
                                    },
                                )
                            }
                            IconButton(onClick = onNotificationsClick) {
                                BadgedBox(
                                    badge = {
                                        if (unreadCount > 0) Badge { Text(unreadCount.toString()) }
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Notifications,
                                        contentDescription = "Notifications",
                                    )
                                }
                            }
                        },
                    )
                },
                modifier = Modifier.fillMaxSize()
            ) { innerPadding ->
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
                    )
                    AppDestinations.COMMUNITY -> CommunityScreen(
                        innerPadding = innerPadding,
                        viewModel = postViewModel
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
                        onMenuClick = { scope.launch { drawerState.open() } },
                        onEditClick = onEditProfileClick,
                        onOpenUserProfile = onOpenUserProfile,
                        onOpenVideoPlaylist = onOpenVideoPlaylist,
                    )
                }
            }
        }
    }
}

enum class AppDestinations(
    val label: String,
    val icon: ImageVector,
) {
    HOME("Accueil", Icons.Default.Home),
    COMMUNITY("Bled", Icons.Default.Groups),
    POST("Publier", Icons.Default.AddCircle),
    KONGOSSA("Kongossa", Icons.AutoMirrored.Filled.Chat),
    PROFILE("Ma Bulle", Icons.Default.Person),
}

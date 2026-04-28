package com.example.ogoula.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.example.ogoula.ui.UserViewModel
import com.example.ogoula.ui.PostViewModel
import com.example.ogoula.ui.ProfileSyncManager
import com.example.ogoula.data.UserRepository
import com.example.ogoula.ui.theme.GreenGabo
import com.example.ogoula.ui.theme.OgoulaSurfaceTint

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileScreen(
    userViewModel: UserViewModel,
    postViewModel: PostViewModel,
    onBack: () -> Unit,
    onSave: () -> Unit
) {
    val context = LocalContext.current
    val userRepository = UserRepository()
    val profileSyncManager = remember { ProfileSyncManager(userViewModel, userRepository, context, postViewModel) }
    
    val currentProfile = userViewModel.userProfile
    val isUploading = userViewModel.isUploading
    val uploadError = userViewModel.uploadError

    var firstName by remember { mutableStateOf(currentProfile.firstName) }
    var lastName by remember { mutableStateOf(currentProfile.lastName) }
    var alias by remember { mutableStateOf(currentProfile.alias) }
    var aliasManuallyEdited by remember { mutableStateOf(currentProfile.alias.isNotEmpty()) }

    var newProfileImageUri by remember { mutableStateOf<Uri?>(null) }
    var newBannerImageUri by remember { mutableStateOf<Uri?>(null) }

    // Quand le ViewModel reçoit le profil (Supabase ou cache) après connexion, aligner les champs du formulaire.
    LaunchedEffect(
        currentProfile.userId,
        currentProfile.firstName,
        currentProfile.lastName,
        currentProfile.alias,
    ) {
        firstName = currentProfile.firstName
        lastName = currentProfile.lastName
        alias = currentProfile.alias
        aliasManuallyEdited = currentProfile.alias.isNotEmpty()
    }

    // Auto-génération de l'alias depuis prénom + nom (seulement si non édité manuellement)
    LaunchedEffect(firstName, lastName) {
        if (!aliasManuallyEdited) {
            val generated = "@${(firstName + lastName)
                .lowercase()
                .replace(" ", "_")
                .filter { it.isLetterOrDigit() || it == '_' }}"
            if (generated != "@") alias = generated
        }
    }

    if (uploadError != null) {
        AlertDialog(
            onDismissRequest = { userViewModel.clearUploadError() },
            title = { Text("Erreur d'upload") },
            text = { Text(uploadError!!) },
            confirmButton = {
                TextButton(onClick = { userViewModel.clearUploadError() }) { Text("OK") }
            }
        )
    }

    if (isUploading) {
        Dialog(onDismissRequest = {}) {
            Card(shape = RoundedCornerShape(16.dp)) {
                Row(
                    modifier = Modifier.padding(24.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    CircularProgressIndicator(color = GreenGabo)
                    Text("Upload en cours...")
                }
            }
        }
    }
    
    val profilePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri -> newProfileImageUri = uri }
    )
    
    val bannerPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri -> newBannerImageUri = uri }
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Modifier le profil") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour")
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            // Mettre à jour le profil localement d'abord
                            userViewModel.updateProfile(
                                firstName = firstName,
                                lastName = lastName,
                                alias = alias,
                                profileUri = newProfileImageUri,
                                bannerUri = newBannerImageUri
                            )
                            
                            // Puis synchroniser automatiquement toute l'application
                            profileSyncManager.syncProfileAutomatic(
                                onSyncComplete = { success, error ->
                                    if (success) {
                                        onSave()
                                    } else {
                                        // En cas d'erreur, afficher un message
                                        android.util.Log.e("EditProfile", "Erreur sync auto: $error")
                                    }
                                }
                            )
                        },
                        enabled = !isUploading
                    ) {
                        Icon(Icons.Default.Check, contentDescription = "Enregistrer", tint = GreenGabo)
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Header with Banner
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .background(
                        brush = Brush.horizontalGradient(
                            listOf(GreenGabo, GreenGabo.copy(alpha = 0.82f), OgoulaSurfaceTint),
                        )
                    )
                    .clickable { 
                        bannerPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    },
                contentAlignment = Alignment.Center
            ) {
                val bannerSource = newBannerImageUri ?: currentProfile.bannerImageUri
                if (bannerSource != null) {
                    AsyncImage(
                        model = bannerSource,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
                
                Box(
                    modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.CameraAlt, contentDescription = null, tint = Color.White)
                }
                
                // Profile Picture
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .align(Alignment.BottomCenter)
                        .offset(y = 50.dp)
                        .border(4.dp, MaterialTheme.colorScheme.background, CircleShape)
                        .clip(CircleShape)
                        .background(Color.LightGray)
                        .clickable { 
                            profilePicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                        },
                    contentAlignment = Alignment.Center
                ) {
                    val profileSource = newProfileImageUri ?: currentProfile.profileImageUri
                    if (profileSource != null) {
                        AsyncImage(
                            model = profileSource,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }
                    
                    Box(
                        modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.CameraAlt, contentDescription = null, tint = Color.White)
                    }
                }
            }

            Spacer(modifier = Modifier.height(60.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedTextField(
                    value = firstName,
                    onValueChange = { firstName = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Prénom") },
                    shape = RoundedCornerShape(12.dp)
                )

                OutlinedTextField(
                    value = lastName,
                    onValueChange = { lastName = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Nom") },
                    shape = RoundedCornerShape(12.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = alias,
                        onValueChange = {
                            alias = it
                            aliasManuallyEdited = true
                        },
                        modifier = Modifier.weight(1f),
                        label = { Text("Alias") },
                        placeholder = { Text("@votre_alias") },
                        shape = RoundedCornerShape(12.dp),
                        supportingText = { Text("Généré automatiquement depuis prénom + nom", style = MaterialTheme.typography.labelSmall) }
                    )
                    
                    Button(
                        onClick = {
                            // Générer un alias automatiquement avec le nouveau système
                            val firstName = firstName
                            val lastName = lastName
                            val generatedAlias = "${firstName.take(2).uppercase()}${lastName.take(2).uppercase()}${System.currentTimeMillis() % 10000}"
                            alias = "@$generatedAlias"
                            aliasManuallyEdited = false
                        },
                        modifier = Modifier
                            .height(OutlinedTextFieldDefaults.MinHeight)
                            .defaultMinSize(minWidth = 80.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = GreenGabo)
                    ) {
                        Text("Générer", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }
    }
}

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
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.example.ogoula.ui.UserViewModel
import com.example.ogoula.ui.theme.BlueGabo
import com.example.ogoula.ui.theme.GreenGabo
import com.example.ogoula.ui.theme.YellowGabo

@Composable
fun ProfileCreationScreen(
    userViewModel: UserViewModel,
    onNavigateToMain: () -> Unit
) {
    var firstName by remember { mutableStateOf("") }
    var lastName by remember { mutableStateOf("") }
    var profileImageUri by remember { mutableStateOf<Uri?>(null) }
    var bannerImageUri by remember { mutableStateOf<Uri?>(null) }

    val isUploading = userViewModel.isUploading
    val uploadError = userViewModel.uploadError

    val profilePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri -> profileImageUri = uri }
    )

    val bannerPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri -> bannerImageUri = uri }
    )

    val alias = remember(firstName, lastName) {
        if (firstName.isNotEmpty() || lastName.isNotEmpty()) {
            "@${lastName.lowercase().replace(" ", "_")}_${firstName.lowercase().replace(" ", "_")}"
        } else ""
    }

    // Dialogue d'erreur d'upload
    if (uploadError != null) {
        AlertDialog(
            onDismissRequest = { userViewModel.clearUploadError() },
            title = { Text("Avertissement") },
            text = { Text(uploadError!!) },
            confirmButton = {
                TextButton(onClick = { userViewModel.clearUploadError() }) { Text("OK") }
            }
        )
    }

    // Spinner pendant l'upload
    if (isUploading) {
        Dialog(onDismissRequest = {}) {
            Card(shape = RoundedCornerShape(16.dp)) {
                Row(
                    modifier = Modifier.padding(24.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    CircularProgressIndicator(color = GreenGabo)
                    Text("Création du profil en cours...")
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Header avec bannière
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .background(brush = Brush.horizontalGradient(listOf(GreenGabo, YellowGabo, BlueGabo)))
                .clickable {
                    bannerPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                },
            contentAlignment = Alignment.Center
        ) {
            if (bannerImageUri != null) {
                AsyncImage(
                    model = bannerImageUri,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.CameraAlt, contentDescription = null, tint = Color.White)
                    Text("Appuyer pour ajouter une bannière", color = Color.White, style = MaterialTheme.typography.labelSmall)
                }
            }

            // Photo de profil
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
                if (profileImageUri != null) {
                    AsyncImage(
                        model = profileImageUri,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.CameraAlt, contentDescription = null, tint = Color.Gray)
                    }
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
            Text(
                text = "Créez votre identité",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )

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

            if (alias.isNotEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("Votre alias : ", style = MaterialTheme.typography.bodyMedium)
                        Text(alias, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, color = GreenGabo)
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    userViewModel.updateProfile(
                        firstName = firstName,
                        lastName = lastName,
                        alias = alias,
                        profileUri = profileImageUri,
                        bannerUri = bannerImageUri,
                        onDone = { onNavigateToMain() }
                    )
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                enabled = firstName.isNotEmpty() && lastName.isNotEmpty() && !isUploading,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = GreenGabo)
            ) {
                Text("C'est parti !")
            }
        }
    }
}

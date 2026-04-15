package com.example.ogoula.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.ogoula.ui.components.PostPublicationImagePreview
import com.example.ogoula.ui.UserViewModel
import com.example.ogoula.ui.theme.GreenGabo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreatePostScreen(
    userViewModel: UserViewModel,
    onBack: () -> Unit,
    onPostCreated: (String, List<ByteArray>) -> Unit
) {
    var postText by remember { mutableStateOf("") }
    val selectedImages = remember { mutableStateListOf<Uri>() }
    val profile = userViewModel.userProfile
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isPublishing by remember { mutableStateOf(false) }

    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(),
        onResult = { uris -> selectedImages.addAll(uris) }
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Quoi de neuf au pays ?", style = MaterialTheme.typography.titleMedium) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour")
                    }
                },
                actions = {
                    Button(
                        onClick = {
                            scope.launch {
                                isPublishing = true
                                // Lire les bytes AVANT navigation pour garder les permissions URI
                                val byteArrays = withContext(Dispatchers.IO) {
                                    selectedImages.mapNotNull { uri ->
                                        try {
                                            context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                                        } catch (e: Exception) {
                                            android.util.Log.e("CreatePost", "Lecture URI échouée", e)
                                            null
                                        }
                                    }
                                }
                                onPostCreated(postText, byteArrays)
                            }
                        },
                        enabled = (postText.isNotBlank() || selectedImages.isNotEmpty()) && !isPublishing,
                        colors = ButtonDefaults.buttonColors(containerColor = GreenGabo),
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        if (isPublishing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = Color.White
                            )
                        } else {
                            Text("Publier")
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color.LightGray),
                    contentAlignment = Alignment.Center
                ) {
                    if (profile.profileImageUri != null) {
                        AsyncImage(
                            model = profile.profileImageUri,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                val displayName = if (profile.firstName.isNotEmpty()) "${profile.firstName} ${profile.lastName}" else "Moi"
                Text(text = displayName, fontWeight = FontWeight.Bold)
            }

            TextField(
                value = postText,
                onValueChange = { postText = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                placeholder = { Text("Parle-nous des choses de notre bled...") },
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                )
            )

            if (selectedImages.isNotEmpty()) {
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(selectedImages) { uri ->
                        Box(
                            modifier = if (selectedImages.size == 1) {
                                Modifier.fillMaxWidth()
                            } else {
                                Modifier.widthIn(max = 180.dp)
                            }
                        ) {
                            PostPublicationImagePreview(
                                imageModel = uri,
                                modifier = Modifier.fillMaxWidth(),
                            )
                            IconButton(
                                onClick = { selectedImages.remove(uri) },
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .size(24.dp)
                                    .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                            ) {
                                Icon(Icons.Default.Close, contentDescription = "Supprimer", tint = Color.White, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = {
                    imagePicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                }) {
                    Icon(Icons.Default.AddPhotoAlternate, contentDescription = "Ajouter des images", tint = GreenGabo)
                }
                Text("Ajouter des images ou memes", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
            }
        }
    }
}

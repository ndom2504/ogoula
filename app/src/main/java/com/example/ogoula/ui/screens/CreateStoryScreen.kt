package com.example.ogoula.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddPhotoAlternate
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
import com.example.ogoula.data.StorageRepository
import com.example.ogoula.ui.theme.GreenGabo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateStoryScreen(
    onBack: () -> Unit,
    onStoryCreated: (String?, String?) -> Unit   // (texte, imageUrl Supabase)
) {
    var storyText by remember { mutableStateOf("") }
    var selectedImageUri by remember { mutableStateOf<android.net.Uri?>(null) }
    var isSharing by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val storageRepository = remember { StorageRepository() }

    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri -> selectedImageUri = uri }
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Ajouter au Quartier") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour")
                    }
                },
                actions = {
                    Button(
                        onClick = {
                            if (isSharing) return@Button
                            isSharing = true
                            scope.launch {
                                var imageUrl: String? = null
                                // Upload l'image vers Supabase pour avoir une URL persistante
                                if (selectedImageUri != null) {
                                    try {
                                        val bytes = withContext(Dispatchers.IO) {
                                            context.contentResolver.openInputStream(selectedImageUri!!)?.use { it.readBytes() }
                                        }
                                        if (bytes != null) {
                                            imageUrl = try {
                                                storageRepository.uploadStoryImage(UUID.randomUUID().toString(), bytes)
                                            } catch (e: Exception) {
                                                android.util.Log.e("CreateStory", "Upload image story échoué", e)
                                                null
                                            }
                                        }
                                    } catch (e: Exception) {
                                        android.util.Log.e("CreateStory", "Lecture image story échouée", e)
                                    }
                                }
                                onStoryCreated(storyText.ifBlank { null }, imageUrl)
                            }
                        },
                        enabled = (storyText.isNotBlank() || selectedImageUri != null) && !isSharing,
                        colors = ButtonDefaults.buttonColors(containerColor = GreenGabo)
                    ) {
                        if (isSharing) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = Color.White)
                        } else {
                            Text("Partager")
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
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .width(200.dp)
                    .height(320.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(if (selectedImageUri == null) GreenGabo else Color.Black),
                contentAlignment = Alignment.Center
            ) {
                if (selectedImageUri != null) {
                    AsyncImage(
                        model = selectedImageUri,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }

                TextField(
                    value = storyText,
                    onValueChange = { storyText = it },
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    placeholder = { Text("Tape ton texte...", color = Color.White.copy(alpha = 0.7f)) },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    textStyle = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.Bold,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = {
                    imagePicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.AddPhotoAlternate, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Choisir une image")
            }
        }
    }
}

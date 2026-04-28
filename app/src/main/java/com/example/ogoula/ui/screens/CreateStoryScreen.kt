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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ogoula.data.StorageRepository
import com.example.ogoula.ui.components.StoryPublicationPreview
import com.example.ogoula.ui.theme.XBlue
import com.example.ogoula.ui.theme.XDarkGray
import com.example.ogoula.ui.theme.XWhite
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateStoryScreen(
    onBack: () -> Unit,
    /** Premier booléen : succès ; second : message d’erreur si échec (affiché dans la boîte de dialogue). */
    onStoryCreated: suspend (String?, String?) -> Pair<Boolean, String?>,
) {
    var storyText by remember { mutableStateOf("") }
    var selectedImageUri by remember { mutableStateOf<android.net.Uri?>(null) }
    var isSharing by remember { mutableStateOf(false) }
    var publishError by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val storageRepository = remember { StorageRepository() }

    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri -> selectedImageUri = uri }
    )

    publishError?.let { msg ->
        AlertDialog(
            onDismissRequest = { publishError = null },
            title = { Text("Publication impossible") },
            text = { Text(msg) },
            confirmButton = {
                TextButton(onClick = { publishError = null }) { Text("OK") }
            },
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Ajouter au quartier") },
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
                                try {
                                    var imageUrl: String? = null
                                    if (selectedImageUri != null) {
                                        try {
                                            val bytes = withContext(Dispatchers.IO) {
                                                context.contentResolver.openInputStream(selectedImageUri!!)
                                                    ?.use { it.readBytes() }
                                            }
                                            if (bytes != null) {
                                                imageUrl = try {
                                                    storageRepository.uploadStoryImage(
                                                        UUID.randomUUID().toString(),
                                                        bytes
                                                    )
                                                } catch (e: Exception) {
                                                    android.util.Log.e("CreateStory", "Upload image story échoué", e)
                                                    null
                                                }
                                            }
                                        } catch (e: Exception) {
                                            android.util.Log.e("CreateStory", "Lecture image story échouée", e)
                                        }
                                    }
                                    if (selectedImageUri != null && imageUrl.isNullOrBlank()) {
                                        publishError =
                                            "L’image n’a pas pu être envoyée. Vérifie ta connexion, que tu es connecté, " +
                                                "et la configuration du bucket « posts » sur Supabase."
                                        return@launch
                                    }
                                    val (ok, err) = onStoryCreated(storyText.ifBlank { null }, imageUrl)
                                    if (ok) {
                                        onBack()
                                    } else {
                                        publishError = err
                                            ?: "La story n’a pas été enregistrée. Vérifie ta connexion et que tu es bien connecté. " +
                                                "Si le problème continue, la configuration Supabase doit être vérifiée (table stories, stockage « posts »)."
                                    }
                                } finally {
                                    isSharing = false
                                }
                            }
                        },
                        enabled = (storyText.isNotBlank() || selectedImageUri != null) && !isSharing,
                        colors = ButtonDefaults.buttonColors(containerColor = XBlue, contentColor = XWhite)
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
            StoryPublicationPreview(
                imageModel = selectedImageUri,
                placeholderColor = if (selectedImageUri == null) XDarkGray else Color.Black,
                modifier = Modifier.fillMaxWidth(),
            ) {
                TextField(
                    value = storyText,
                    onValueChange = { storyText = it },
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    placeholder = { Text("Partage tes ogoulas ici...", color = Color.White.copy(alpha = 0.7f)) },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        cursorColor = XBlue,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedPlaceholderColor = XWhite.copy(alpha = 0.72f),
                        unfocusedPlaceholderColor = XWhite.copy(alpha = 0.72f)
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
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = XBlue, contentColor = XWhite)
            ) {
                Icon(Icons.Default.AddPhotoAlternate, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Choisir une image")
            }
        }
    }
}

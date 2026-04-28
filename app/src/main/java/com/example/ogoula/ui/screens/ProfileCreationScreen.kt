package com.example.ogoula.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.ui.window.Dialog
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.ogoula.ui.UserViewModel
import com.example.ogoula.ui.ProfileSyncManager
import com.example.ogoula.data.UserRepository
import com.example.ogoula.ui.onboarding.AfricanCountryMenuRow
import com.example.ogoula.ui.onboarding.CONTRIBUTION_MAX_LEN
import com.example.ogoula.ui.onboarding.CONTRIBUTION_MIN_LEN
import com.example.ogoula.ui.onboarding.MAX_INTENTIONS
import com.example.ogoula.ui.onboarding.africanCountryMenuRows
import com.example.ogoula.ui.onboarding.culturalIntentionOptions
import com.example.ogoula.ui.onboarding.PRO_CONTRIBUTION_PRINCIPLES
import com.example.ogoula.ui.onboarding.PRO_CONTRIBUTION_TITLE
import com.example.ogoula.ui.onboarding.selfRoleOptions
import com.example.ogoula.ui.theme.XBlack
import com.example.ogoula.ui.theme.XBlue
import com.example.ogoula.ui.theme.XDarkGray

@Composable
private fun FormSectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurface,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileCreationScreen(
    userViewModel: UserViewModel,
    /** `true` après ouverture de la charte communautaire depuis ce parcours (obligatoire pour valider). */
    communityCharterRead: Boolean = false,
    onNavigateToMain: () -> Unit,
    onReadCharter: () -> Unit = {},
) {
    val context = LocalContext.current
    val userRepository = UserRepository()
    val profileSyncManager = remember { ProfileSyncManager(userViewModel, userRepository, context) }
    
    // rememberSaveable : survit au retour depuis l’écran « Charte » (évite de tout perdre).
    var firstName by rememberSaveable { mutableStateOf("") }
    var lastName by rememberSaveable { mutableStateOf("") }
    var profileImageUriStr by rememberSaveable { mutableStateOf("") }
    var bannerImageUriStr by rememberSaveable { mutableStateOf("") }
    val profileImageUri = profileImageUriStr.takeIf { it.isNotBlank() }?.let { Uri.parse(it) }
    val bannerImageUri = bannerImageUriStr.takeIf { it.isNotBlank() }?.let { Uri.parse(it) }

    var selectedCountry by rememberSaveable { mutableStateOf("") }
    var countryMenuExpanded by remember { mutableStateOf(false) }
    var selectedIntentionIdsCsv by rememberSaveable { mutableStateOf("") }
    val selectedIntentionIds = selectedIntentionIdsCsv.split(',').filter { it.isNotEmpty() }.toSet()
    var selectedRoleId by rememberSaveable { mutableStateOf("") }
    var roleMenuExpanded by remember { mutableStateOf(false) }
    var contribution by rememberSaveable { mutableStateOf("") }
    var ackReadCharter by rememberSaveable { mutableStateOf(false) }
    var ackProContributionRules by rememberSaveable { mutableStateOf(false) }

    val isUploading = userViewModel.isUploading
    val uploadError = userViewModel.uploadError

    val profilePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri -> profileImageUriStr = uri?.toString().orEmpty() }
    )

    val bannerPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri -> bannerImageUriStr = uri?.toString().orEmpty() }
    )

    val alias = remember(firstName, lastName) {
        if (firstName.isNotEmpty() || lastName.isNotEmpty()) {
            "@${lastName.lowercase().replace(" ", "_")}_${firstName.lowercase().replace(" ", "_")}"
        } else ""
    }

    val contributionTrimmed = contribution.trim()
    val contributionOk =
        contributionTrimmed.length in CONTRIBUTION_MIN_LEN..CONTRIBUTION_MAX_LEN
    val intentionsOk = selectedIntentionIds.isNotEmpty() && selectedIntentionIds.size <= MAX_INTENTIONS
    val culturalOk =
        selectedCountry.isNotBlank() && intentionsOk && selectedRoleId.isNotBlank() && contributionOk

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

    if (isUploading) {
        Dialog(onDismissRequest = {}) {
            Card(shape = RoundedCornerShape(16.dp)) {
                Row(
                    modifier = Modifier.padding(24.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    CircularProgressIndicator(color = XBlue)
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
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .background(
                    brush = Brush.horizontalGradient(
                        listOf(XBlack, XBlue.copy(alpha = 0.72f), XDarkGray),
                    ),
                )
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
                    Text(
                        "Appuyer pour ajouter une bannière",
                        color = Color.White,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }

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
                    Icon(Icons.Default.CameraAlt, contentDescription = null, tint = Color.Gray)
                }
            }
        }

        Spacer(modifier = Modifier.height(60.dp))

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = "Bienvenue sur Ogoula",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = "Ogoula est un outil d’influence au service de notre patrimoine et de nos valeurs locales dans le monde. Ton inscription est aussi un engagement.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Card(
                colors = CardDefaults.cardColors(
                    containerColor = XBlue.copy(alpha = 0.22f),
                    contentColor = Color.White,
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = "Déclare ton intention : d’où tu viens culturellement, pourquoi tu es là, et comment tu peux contribuer.",
                    modifier = Modifier.padding(12.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                )
            }

            FormSectionTitle("Identité")

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
                        Text(
                            "Votre alias : ",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(alias, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, color = XBlue)
                    }
                }
            }

            FormSectionTitle("Lien culturel")
            Text(
                text = "Quel pays ou lien représente le mieux ton ancrage ?",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            ExposedDropdownMenuBox(
                expanded = countryMenuExpanded,
                onExpandedChange = { countryMenuExpanded = it }
            ) {
                OutlinedTextField(
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(),
                    readOnly = true,
                    value = selectedCountry,
                    onValueChange = {},
                    label = { Text("Pays / option de référence") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = countryMenuExpanded) },
                    placeholder = { Text("Choisir dans la liste") },
                    shape = RoundedCornerShape(12.dp)
                )
                ExposedDropdownMenu(
                    expanded = countryMenuExpanded,
                    onDismissRequest = { countryMenuExpanded = false },
                    modifier = Modifier.heightIn(max = 360.dp),
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    shape = RoundedCornerShape(12.dp),
                ) {
                    africanCountryMenuRows.forEach { row ->
                        when (row) {
                            is AfricanCountryMenuRow.Region -> {
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            row.title,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    },
                                    onClick = {},
                                    enabled = false,
                                    colors = MenuDefaults.itemColors(
                                        textColor = MaterialTheme.colorScheme.primary,
                                        disabledTextColor = MaterialTheme.colorScheme.primary,
                                    ),
                                )
                            }
                            is AfricanCountryMenuRow.Country -> {
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            row.name,
                                            color = MaterialTheme.colorScheme.onSurface,
                                        )
                                    },
                                    onClick = {
                                        selectedCountry = row.name
                                        countryMenuExpanded = false
                                    },
                                    colors = MenuDefaults.itemColors(
                                        textColor = MaterialTheme.colorScheme.onSurface,
                                    ),
                                )
                            }
                        }
                    }
                }
            }

            FormSectionTitle("Pourquoi rejoindre Ogoula ?")
            Text(
                text = "Choisis 1 ou 2 intentions principales.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            culturalIntentionOptions.forEach { opt ->
                val selected = opt.id in selectedIntentionIds
                FilterChip(
                    selected = selected,
                    onClick = {
                        val ids = selectedIntentionIdsCsv.split(',').filter { it.isNotEmpty() }.toMutableSet()
                        when {
                            selected -> ids.remove(opt.id)
                            ids.size >= MAX_INTENTIONS -> { /* ne pas ajouter au-delà du max */ }
                            else -> ids.add(opt.id)
                        }
                        selectedIntentionIdsCsv = ids.joinToString(",")
                    },
                    label = {
                        Text(
                            opt.label,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                            color = if (selected) Color.White else MaterialTheme.colorScheme.onSurface,
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = XBlue.copy(alpha = 0.22f),
                        selectedLabelColor = Color.White,
                        labelColor = MaterialTheme.colorScheme.onSurface,
                    )
                )
            }

            FormSectionTitle("Comment te définis-tu ?")
            ExposedDropdownMenuBox(
                expanded = roleMenuExpanded,
                onExpandedChange = { roleMenuExpanded = it }
            ) {
                val roleLabel = selfRoleOptions.find { it.id == selectedRoleId }?.label.orEmpty()
                OutlinedTextField(
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(),
                    readOnly = true,
                    value = roleLabel,
                    onValueChange = {},
                    label = { Text("Profil / rôle") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = roleMenuExpanded) },
                    placeholder = { Text("Sélectionner") },
                    shape = RoundedCornerShape(12.dp)
                )
                ExposedDropdownMenu(
                    expanded = roleMenuExpanded,
                    onDismissRequest = { roleMenuExpanded = false },
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    shape = RoundedCornerShape(12.dp),
                ) {
                    selfRoleOptions.forEach { opt ->
                        DropdownMenuItem(
                            text = {
                                Text(
                                    opt.label,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                            },
                            onClick = {
                                selectedRoleId = opt.id
                                roleMenuExpanded = false
                            },
                            colors = MenuDefaults.itemColors(
                                textColor = MaterialTheme.colorScheme.onSurface,
                            ),
                        )
                    }
                }
            }

            FormSectionTitle("Ta contribution")
            Text(
                text = "En une phrase : comment peux-tu contribuer à ton environnement ou à la valorisation de ta culture et de ton patrimoine ?",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            OutlinedTextField(
                value = contribution,
                onValueChange = { if (it.length <= CONTRIBUTION_MAX_LEN) contribution = it },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
                maxLines = 5,
                label = { Text("Ta phrase d’engagement") },
                placeholder = {
                    Text(
                        "Ex. : Je souhaite partager les traditions orales de mon village à travers de courtes vidéos.",
                        style = MaterialTheme.typography.bodySmall
                    )
                },
                supportingText = {
                    Column {
                        Text(
                            "${contributionTrimmed.length} / $CONTRIBUTION_MAX_LEN caractères (minimum $CONTRIBUTION_MIN_LEN)",
                            style = MaterialTheme.typography.labelSmall
                        )
                        Text(
                            "L’algorithme Ogoula valorisera surtout la pertinence, le respect de la charte et les contributions positives.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Medium,
                        )
                    }
                },
                isError = contributionTrimmed.isNotEmpty() && !contributionOk,
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = PRO_CONTRIBUTION_TITLE,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = "Dès l’inscription, tu acceptes le cadre dans lequel Ogoula évalue la qualité des contributions (phases de test incluses). Les seules prises en compte « positives » reposent sur ces règles.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = XBlue.copy(alpha = 0.22f),
                    contentColor = Color.White,
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    PRO_CONTRIBUTION_PRINCIPLES.forEach { line ->
                        Text(
                            text = "• $line",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium,
                            color = Color.White,
                        )
                    }
                }
            }
            Button(
                onClick = onReadCharter,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = XBlue,
                    contentColor = Color.White,
                ),
            ) {
                Text(
                    if (communityCharterRead) {
                        "Revoir la charte communautaire"
                    } else {
                        "Ouvrir la charte communautaire (obligatoire)"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            if (!communityCharterRead) {
                Text(
                    text = "Tu dois ouvrir la charte au moins une fois, puis revenir ici pour cocher les cases ci-dessous.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.error,
                    fontWeight = FontWeight.Medium,
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = ackReadCharter,
                    onCheckedChange = { if (communityCharterRead) ackReadCharter = it },
                    enabled = communityCharterRead,
                )
                Text(
                    text = "J’ai pris connaissance de la charte Ogoula et je m’engage à la respecter.",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.weight(1f),
                    color = if (communityCharterRead) {
                        MaterialTheme.colorScheme.onSurface
                    } else {
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f)
                    },
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = ackProContributionRules,
                    onCheckedChange = { if (communityCharterRead) ackProContributionRules = it },
                    enabled = communityCharterRead,
                )
                Text(
                    text = "Je n’utiliserai pas Ogoula pour injures, violence ou irrespect ; je privilégierai des contributions positives, le fun et la sociabilité bienveillante.",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.weight(1f),
                    color = if (communityCharterRead) {
                        MaterialTheme.colorScheme.onSurface
                    } else {
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f)
                    },
                )
            }

            val canSubmit =
                firstName.isNotBlank() && lastName.isNotBlank() && culturalOk &&
                    communityCharterRead && ackReadCharter && ackProContributionRules && !isUploading

            Button(
                onClick = {
                    val intentionsCsv = selectedIntentionIds.joinToString(",")
                    profileSyncManager.syncProfileComplete(
                        firstName = firstName,
                        lastName = lastName,
                        alias = alias,
                        profileUri = profileImageUri,
                        bannerUri = bannerImageUri,
                        culturalReferenceCountry = selectedCountry.trim(),
                        culturalIntentionsCsv = intentionsCsv,
                        selfRole = selectedRoleId,
                        contributionSentence = contributionTrimmed,
                        signProContributionCharter = true,
                        onSyncComplete = { success, error ->
                            if (success) {
                                onNavigateToMain()
                            }
                        }
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                enabled = canSubmit,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = XBlue,
                    contentColor = Color.White,
                )
            ) {
                Text(
                    "Créer mon compte Ogoula",
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

package com.example.ogoula.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ogoula.ui.UserProfile
import com.example.ogoula.ui.UserViewModel
import com.example.ogoula.ui.onboarding.AfricanCountryMenuRow
import com.example.ogoula.ui.onboarding.CONTRIBUTION_MAX_LEN
import com.example.ogoula.ui.onboarding.CONTRIBUTION_MIN_LEN
import com.example.ogoula.ui.onboarding.MAX_INTENTIONS
import com.example.ogoula.ui.onboarding.africanCountryMenuRows
import com.example.ogoula.ui.onboarding.culturalIntentionOptions
import com.example.ogoula.ui.onboarding.parseIntentionsCsv
import com.example.ogoula.ui.onboarding.selfRoleOptions
import com.example.ogoula.ui.theme.XBlue

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EngagementEditBottomSheet(
    visible: Boolean,
    profile: UserProfile,
    userViewModel: UserViewModel,
    onDismiss: () -> Unit,
) {
    if (!visible) return

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var selectedIntentionIds by remember { mutableStateOf(setOf<String>()) }
    var roleId by remember { mutableStateOf("") }
    var contribution by remember { mutableStateOf("") }
    var selectedCountry by remember { mutableStateOf("") }
    var countryMenuExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(visible, profile.culturalIntentions, profile.selfRole, profile.contributionSentence, profile.culturalReferenceCountry) {
        if (visible) {
            selectedIntentionIds = parseIntentionsCsv(profile.culturalIntentions).toSet()
            roleId = profile.selfRole.orEmpty()
            contribution = profile.contributionSentence.orEmpty()
            selectedCountry = profile.culturalReferenceCountry?.trim().orEmpty()
        }
    }

    val contributionTrimmed = contribution.trim()
    val contributionOk =
        contributionTrimmed.length in CONTRIBUTION_MIN_LEN..CONTRIBUTION_MAX_LEN
    val intentionsOk =
        selectedIntentionIds.isNotEmpty() && selectedIntentionIds.size <= MAX_INTENTIONS
    val canSave = intentionsOk && contributionOk && roleId.isNotBlank() &&
        selectedCountry.isNotBlank() && !userViewModel.isUploading

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        dragHandle = { BottomSheetDefaults.DragHandle() },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                text = "Mon engagement sur Ogoula",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Sur Ogoula, tu contribues à la valorisation collective des marques, produits et personnalités. Définis ton pays de référence, ton orientation et tes motivations d'engagement. Ces informations permettent à la communauté de comprendre ta vision et tes contributions.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp, bottom = 16.dp)
            )

            Text(
                text = "Pays / option de référence",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold
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
                    value = selectedCountry.ifBlank { "Choisir dans la liste" },
                    onValueChange = {},
                    label = { Text("Ancrage culturel") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = countryMenuExpanded) },
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

            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Ton rôle sur Ogoula",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Comment tu te définis : marque, produit, talent, influenceur ou communauté ?",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            Text(
                text = "Ton rôle / profil",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(top = 4.dp, bottom = 4.dp)
            )
            selfRoleOptions.forEach { opt ->
                val selected = opt.id == roleId
                FilterChip(
                    selected = selected,
                    onClick = { roleId = opt.id },
                    label = { Text(opt.label, style = MaterialTheme.typography.bodySmall) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = XBlue.copy(alpha = 0.22f),
                        selectedLabelColor = MaterialTheme.colorScheme.onSurface
                    )
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Tes motivations (1 à 2)",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Choisis ce qui te motive le plus à contribuer et interagir sur Ogoula",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            culturalIntentionOptions.forEach { opt ->
                val selected = opt.id in selectedIntentionIds
                FilterChip(
                    selected = selected,
                    onClick = {
                        selectedIntentionIds = when {
                            selected -> selectedIntentionIds - opt.id
                            selectedIntentionIds.size >= MAX_INTENTIONS -> selectedIntentionIds
                            else -> selectedIntentionIds + opt.id
                        }
                    },
                    label = { Text(opt.label, style = MaterialTheme.typography.bodySmall) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = XBlue.copy(alpha = 0.22f),
                        selectedLabelColor = MaterialTheme.colorScheme.onSurface
                    )
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Mon engagement",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Partage comment tu vas contribuer à la valorisation sur Ogoula. Engagement : authenticité, interactions respectueuses, contenus pertinents et fun.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            OutlinedTextField(
                value = contribution,
                onValueChange = { if (it.length <= CONTRIBUTION_MAX_LEN) contribution = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                minLines = 3,
                maxLines = 5,
                label = { Text("Ma charte d'engagement") },
                supportingText = {
                    Text(
                        "${contributionTrimmed.length} / $CONTRIBUTION_MAX_LEN (min. $CONTRIBUTION_MIN_LEN)",
                        style = MaterialTheme.typography.labelSmall
                    )
                },
                isError = contributionTrimmed.isNotEmpty() && !contributionOk,
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(20.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f)
                ) { Text("Annuler") }
                Button(
                    onClick = {
                        userViewModel.updateProfile(
                            firstName = profile.firstName,
                            lastName = profile.lastName,
                            alias = profile.alias,
                            profileUri = null,
                            bannerUri = null,
                            culturalReferenceCountry = selectedCountry.trim(),
                            culturalIntentionsCsv = selectedIntentionIds.joinToString(","),
                            selfRole = roleId.ifBlank { null },
                            contributionSentence = contributionTrimmed,
                            onDone = { onDismiss() }
                        )
                    },
                    enabled = canSave,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = XBlue)
                ) {
                    Text("Enregistrer")
                }
            }
        }
    }
}

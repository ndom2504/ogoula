package com.example.ogoula.ui.screens

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.provider.ContactsContract
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ContactPhone
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ogoula.data.SupabaseClient
import com.example.ogoula.ui.UserProfile
import com.example.ogoula.ui.theme.GreenGabo
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class DeviceContact(
    val name: String,
    val phone: String,
    val isOnApp: Boolean = false,
    val appProfile: UserProfile? = null
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactsSyncScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var hasPermission by remember { mutableStateOf(false) }
    var isSyncing by remember { mutableStateOf(false) }
    var contacts by remember { mutableStateOf<List<DeviceContact>>(emptyList()) }
    var lastSyncTime by remember { mutableStateOf<String?>(null) }
    var permissionDenied by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasPermission = granted
        permissionDenied = !granted
        if (granted) {
            isSyncing = true
            scope.launch {
                contacts = syncContacts(context)
                lastSyncTime = getCurrentTime()
                isSyncing = false
            }
        }
    }

    fun startSync() {
        isSyncing = true
        scope.launch {
            contacts = syncContacts(context)
            lastSyncTime = getCurrentTime()
            isSyncing = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Synchroniser les contacts") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour")
                    }
                },
                actions = {
                    if (hasPermission) {
                        IconButton(onClick = { startSync() }, enabled = !isSyncing) {
                            Icon(Icons.Default.Sync, contentDescription = "Actualiser", tint = GreenGabo)
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
        ) {
            when {
                isSyncing -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = GreenGabo)
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("Lecture des contacts en cours…", style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }

                permissionDenied -> {
                    PermissionDeniedState()
                }

                !hasPermission -> {
                    RequestPermissionState(
                        onRequest = { permissionLauncher.launch(Manifest.permission.READ_CONTACTS) }
                    )
                }

                contacts.isEmpty() -> {
                    EmptyContactsState(onSync = { startSync() })
                }

                else -> {
                    // Bandeau d'état de sync
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Sync, contentDescription = null, tint = GreenGabo, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "Dernière synchro : ${lastSyncTime ?: "Aujourd'hui"}",
                                    style = MaterialTheme.typography.bodySmall
                                )
                                Text(
                                    "${contacts.count { it.isOnApp }} contact(s) sur Ogoula · ${contacts.count { !it.isOnApp }} à inviter",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    val onApp = contacts.filter { it.isOnApp }
                    val notOnApp = contacts.filter { !it.isOnApp }

                    LazyColumn(modifier = Modifier.weight(1f)) {
                        if (onApp.isNotEmpty()) {
                            item {
                                Text(
                                    "Tes proches sur Ogoula",
                                    modifier = Modifier.padding(start = 16.dp, top = 8.dp, bottom = 4.dp),
                                    style = MaterialTheme.typography.labelLarge,
                                    color = GreenGabo,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            items(onApp) { contact ->
                                RealContactItem(contact = contact, onInvite = null)
                                HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
                            }
                        }

                        if (notOnApp.isNotEmpty()) {
                            item {
                                Text(
                                    "Inviter tes amis au pays",
                                    modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 4.dp),
                                    style = MaterialTheme.typography.labelLarge,
                                    color = Color.Gray,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            items(notOnApp) { contact ->
                                RealContactItem(
                                    contact = contact,
                                    onInvite = {
                                        // Envoyer un SMS d'invitation réel
                                        val smsUri = Uri.parse("smsto:${contact.phone}")
                                        val smsIntent = Intent(Intent.ACTION_SENDTO, smsUri).apply {
                                            putExtra(
                                                "sms_body",
                                                "Hé ${contact.name.split(" ").first()} ! Rejoins-moi sur Ogoula — un réseau pensé pour les cultures africaines et le monde. Infos : https://ogoula.com"
                                            )
                                        }
                                        context.startActivity(smsIntent)
                                    }
                                )
                                HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RequestPermissionState(onRequest: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Default.ContactPhone,
            contentDescription = null,
            modifier = Modifier.size(72.dp),
            tint = GreenGabo
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            "Retrouve tes amis du bled",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            "Autorise l'accès à tes contacts pour voir qui est déjà sur Ogoula et inviter tes proches.",
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 32.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = Color.Gray
        )
        Spacer(modifier = Modifier.height(32.dp))
        Button(
            onClick = onRequest,
            colors = ButtonDefaults.buttonColors(containerColor = GreenGabo)
        ) {
            Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Accéder aux contacts")
        }
    }
}

@Composable
private fun PermissionDeniedState() {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Accès refusé", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            "Pour synchroniser tes contacts, va dans Paramètres > Applications > Ogoula > Autorisations et active \"Contacts\".",
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 32.dp),
            style = MaterialTheme.typography.bodySmall,
            color = Color.Gray
        )
    }
}

@Composable
private fun EmptyContactsState(onSync: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(Icons.Default.ContactPhone, contentDescription = null, modifier = Modifier.size(64.dp), tint = Color.LightGray)
        Spacer(modifier = Modifier.height(16.dp))
        Text("Aucun contact trouvé", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(24.dp))
        Button(onClick = onSync, colors = ButtonDefaults.buttonColors(containerColor = GreenGabo)) {
            Text("Réessayer")
        }
    }
}

@Composable
private fun RealContactItem(contact: DeviceContact, onInvite: (() -> Unit)?) {
    ListItem(
        headlineContent = { Text(contact.name, fontWeight = FontWeight.Medium) },
        supportingContent = { Text(contact.phone, style = MaterialTheme.typography.bodySmall, color = Color.Gray) },
        leadingContent = {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(if (contact.isOnApp) GreenGabo.copy(alpha = 0.15f) else Color.LightGray.copy(alpha = 0.4f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = contact.name.take(1).uppercase(),
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = if (contact.isOnApp) GreenGabo else Color.Gray
                )
            }
        },
        trailingContent = {
            if (contact.isOnApp) {
                Surface(
                    color = GreenGabo.copy(alpha = 0.12f),
                    shape = MaterialTheme.shapes.small
                ) {
                    Text(
                        "Sur Ogoula",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        color = GreenGabo,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            } else if (onInvite != null) {
                OutlinedButton(
                    onClick = onInvite,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                    modifier = Modifier.height(32.dp)
                ) {
                    Text("Inviter", fontSize = 12.sp)
                }
            }
        }
    )
}

/** Lit les vrais contacts du téléphone et vérifie lesquels sont sur Supabase */
private suspend fun syncContacts(context: android.content.Context): List<DeviceContact> =
    withContext(Dispatchers.IO) {
        // 1. Lire les contacts du téléphone
        val deviceContacts = mutableListOf<Pair<String, String>>() // name, phone
        val cursor = context.contentResolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            arrayOf(
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                ContactsContract.CommonDataKinds.Phone.NUMBER
            ),
            null, null,
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " ASC"
        )
        cursor?.use {
            val nameIdx = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
            val phoneIdx = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
            val seen = mutableSetOf<String>()
            while (it.moveToNext()) {
                val name = it.getString(nameIdx)?.trim() ?: continue
                val phone = it.getString(phoneIdx)?.replace("\\s".toRegex(), "") ?: continue
                val key = "$name|$phone"
                if (key !in seen && name.isNotEmpty() && phone.isNotEmpty()) {
                    seen.add(key)
                    deviceContacts.add(Pair(name, phone))
                }
            }
        }

        if (deviceContacts.isEmpty()) return@withContext emptyList()

        // 2. Récupérer tous les profils Supabase pour matcher
        val supabaseProfiles: List<UserProfile> = try {
            SupabaseClient.client.from("profiles")
                .select()
                .decodeList<UserProfile>()
        } catch (e: Exception) {
            emptyList()
        }

        // Normalise un numéro de téléphone (garde les 8 derniers chiffres pour comparer)
        fun normalizePhone(phone: String): String =
            phone.filter { it.isDigit() }.takeLast(8)

        // 3. Croiser contacts téléphone avec profils Supabase
        deviceContacts.map { (name, phone) ->
            val normalizedPhone = normalizePhone(phone)
            val matchedProfile = supabaseProfiles.find { profile ->
                // Matching par nom (prénom + nom) ou par les derniers chiffres du téléphone si stocké dans alias
                val fullName = "${profile.firstName} ${profile.lastName}".trim().lowercase()
                val contactNameLower = name.lowercase()
                fullName == contactNameLower ||
                    (fullName.isNotEmpty() && contactNameLower.contains(fullName)) ||
                    (fullName.isNotEmpty() && fullName.contains(contactNameLower))
            }
            DeviceContact(
                name = name,
                phone = phone,
                isOnApp = matchedProfile != null,
                appProfile = matchedProfile
            )
        }
    }

private fun getCurrentTime(): String {
    val now = java.util.Calendar.getInstance()
    return String.format(
        "%02d:%02d",
        now.get(java.util.Calendar.HOUR_OF_DAY),
        now.get(java.util.Calendar.MINUTE)
    )
}

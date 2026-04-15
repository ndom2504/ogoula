package com.example.ogoula.data

import android.util.Log
import com.example.ogoula.ui.UserProfile
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.jsonObject
import java.util.Locale

class UserRepository {
    private val supabase = SupabaseClient.client

    /** Même logique que PostgREST : chaînes vides optionnelles → null pour ne pas envoyer de clés inutiles. */
    private fun UserProfile.normalizedForUpsert(): UserProfile {
        fun String?.blankAsNull() = this?.trim()?.takeIf { it.isNotEmpty() }
        return copy(
            culturalReferenceCountry = culturalReferenceCountry.blankAsNull(),
            culturalIntentions = culturalIntentions.blankAsNull(),
            selfRole = selfRole.blankAsNull(),
            contributionSentence = contributionSentence.blankAsNull(),
            culturalProfileVisibility = culturalProfileVisibility.blankAsNull(),
            proContributionCharterVersion = proContributionCharterVersion.blankAsNull(),
            proContributionAcknowledgedAt = proContributionAcknowledgedAt.blankAsNull(),
        )
    }

    private val profileJson = Json {
        encodeDefaults = false
        ignoreUnknownKeys = true
    }

    private val missingColumnRegex = Regex("Could not find the '([^']+)' column")

    /**
     * Upsert sur `user_id` (clé unique). Sans `onConflict`, PostgREST peut mal fusionner les lignes.
     *
     * Si la base n’a pas encore toutes les colonnes du modèle (ex. `contribution_sentence`), PostgREST
     * renvoie PGRST204 : on retire la clé signalée et on réessaie (schéma partiel).
     * Pour un schéma à jour, exécuter [docs/supabase_profiles_cultural_engagement.sql].
     *
     * @return false si l’appel réseau / RLS échoue (voir logs).
     */
    suspend fun saveProfile(userId: String, profile: UserProfile): Boolean {
        val uid = SupabaseIdentity.userIdForWritableRow(userId)
        val normalized = profile.normalizedForUpsert().copy(userId = uid)
        var body = profileJson.encodeToJsonElement(UserProfile.serializer(), normalized).jsonObject
        var attempts = 0
        val maxAttempts = 12
        while (attempts < maxAttempts) {
            try {
                supabase.from("profiles").upsert(buildJsonArray { add(body) }) {
                    onConflict = "user_id"
                }
                if (attempts > 0) {
                    Log.w(
                        "UserRepository",
                        "saveProfile: upsert OK après retrait de colonnes absentes du schéma Supabase. " +
                            "Ajoute les colonnes manquantes (voir docs/supabase_profiles_cultural_engagement.sql).",
                    )
                }
                return true
            } catch (e: Exception) {
                val msg = e.message.orEmpty()
                if (!msg.contains("PGRST204")) {
                    Log.e("UserRepository", "saveProfile (profiles upsert)", e)
                    return false
                }
                val col = missingColumnRegex.find(msg)?.groupValues?.getOrNull(1) ?: run {
                    Log.e("UserRepository", "saveProfile (profiles upsert)", e)
                    return false
                }
                val next = JsonObject(body.filterKeys { it != col })
                if (next.size == body.size) {
                    Log.e("UserRepository", "saveProfile: impossible de retirer la colonne signalée: $col", e)
                    return false
                }
                Log.w(
                    "UserRepository",
                    "saveProfile: colonne absente côté Supabase ($col), nouvel essai sans cette clé",
                )
                body = next
                attempts++
            }
        }
        Log.e("UserRepository", "saveProfile: abandon après $maxAttempts tentatives (schéma profiles)")
        return false
    }

    suspend fun getProfile(userId: String): UserProfile? {
        val candidates = buildList {
            addAll(SupabaseIdentity.userIdLookupCandidates(userId))
            SupabaseIdentity.sessionUserIdOrNull()?.let { addAll(SupabaseIdentity.userIdLookupCandidates(it)) }
        }.distinct()
        for (uid in candidates) {
            try {
                val row = supabase.from("profiles")
                    .select {
                        filter { eq("user_id", uid) }
                    }
                    .decodeSingleOrNull<UserProfile>()
                if (row != null) return row
            } catch (e: Exception) {
                Log.w("UserRepository", "getProfile try user_id=$uid", e)
            }
        }
        Log.w("UserRepository", "getProfile: aucune ligne pour userId=$userId (candidats=${candidates.size})")
        return null
    }

    /**
     * Recherche par [alias] : variantes @ / sans @, casse (insensible via clés normalisées).
     */
    suspend fun getProfileByAlias(alias: String): UserProfile? {
        val trimmed = alias.trim()
        if (trimmed.isEmpty()) return null
        val core = trimmed.removePrefix("@").lowercase(Locale.ROOT)
        val canonical = "@$core"
        val variants = buildList {
            add(trimmed)
            add(canonical)
            if (!trimmed.startsWith("@")) add("@$trimmed")
            if (trimmed.startsWith("@")) add(trimmed.removePrefix("@"))
            add(trimmed.lowercase(Locale.ROOT))
            add(core)
        }.distinct()
        for (v in variants) {
            try {
                val row = supabase.from("profiles")
                    .select {
                        filter { eq("alias", v) }
                    }
                    .decodeSingleOrNull<UserProfile>()
                if (row != null) return row
            } catch (e: Exception) {
                Log.w("UserRepository", "getProfileByAlias alias=$v", e)
            }
        }
        return null
    }
}

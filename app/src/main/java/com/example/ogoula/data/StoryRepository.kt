package com.example.ogoula.data

import android.util.Log
import com.example.ogoula.ui.Story
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.time.Instant
import java.time.OffsetDateTime

private const val STORY_VISIBLE_MS = 24L * 60L * 60L * 1000L

@Serializable
private data class StoryRow(
    val id: String,
    @SerialName("user_id") val userId: String,
    @SerialName("author_display") val authorDisplay: String,
    @SerialName("author_handle") val authorHandle: String? = null,
    @SerialName("content_text") val contentText: String? = null,
    @SerialName("image_url") val imageUrl: String? = null,
    val color: Int = 0xFF1C745E.toInt(),
    val status: String = "active",
    @SerialName("moderation_note") val moderationNote: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
    val views: Int = 0,
    val validates: Int = 0,
)

/** Insert seul : ne pas envoyer `created_at` (sinon JSON null casse le default `now()` côté Postgres). */
@Serializable
private data class StoryInsertRow(
    val id: String,
    @SerialName("user_id") val userId: String,
    @SerialName("author_display") val authorDisplay: String,
    @SerialName("author_handle") val authorHandle: String? = null,
    @SerialName("content_text") val contentText: String? = null,
    @SerialName("image_url") val imageUrl: String? = null,
    /** Pas de valeur par défaut ici : avec `encodeDefaults = false`, une valeur par défaut identique au paramètre d’insert n’est pas envoyée → NOT NULL sur `color` côté Postgres. */
    val color: Int,
    val status: String = "active",
)

private fun Throwable.storyInsertUserMessage(): String {
    val raw = (message ?: cause?.message ?: "").trim()
    val lower = raw.lowercase()
    return when {
        lower.contains("row-level security") ||
            lower.contains("new row violates") ||
            lower.contains("42501") ->
            "Insertion refusée par Supabase (règles RLS). Réexécute docs/supabase_stories.sql et vérifie que user_id correspond à ta session."
        lower.contains("does not exist") && lower.contains("column") ->
            "Colonne manquante : exécute docs/supabase_stories.sql sur ton projet Supabase."
        lower.contains("23502") || (lower.contains("null value") && lower.contains("not-null")) ->
            "Donnée manquante pour la story (ex. couleur). Mets à jour l’app ou contacte le support."
        lower.contains("jwt") || lower.contains("expired") || lower.contains("401") ->
            "Session expirée : déconnecte-toi et reconnecte-toi, puis réessaie."
        raw.isNotEmpty() -> raw.take(400)
        else -> "Erreur inconnue lors de l’enregistrement."
    }
}

internal fun parseStoryCreatedAtMs(iso: String?): Long {
    if (iso.isNullOrBlank()) return 0L
    return try {
        Instant.parse(iso).toEpochMilli()
    } catch (_: Exception) {
        try {
            OffsetDateTime.parse(iso).toInstant().toEpochMilli()
        } catch (_: Exception) {
            0L
        }
    }
}

class StoryRepository {
    private val supabase = SupabaseClient.client

    suspend fun getActiveStories(): List<Story> {
        val cutoff = System.currentTimeMillis() - STORY_VISIBLE_MS
        return try {
            supabase.from("stories")
                .select {
                    filter { eq("status", "active") }
                    order("created_at", order = Order.DESCENDING)
                }
                .decodeList<StoryRow>()
                .mapNotNull { row ->
                    val createdMs = parseStoryCreatedAtMs(row.createdAt)
                    if (createdMs > 0L && createdMs < cutoff) null
                    else row.toStory(createdMs)
                }
        } catch (e: Exception) {
            Log.w("StoryRepository", "getActiveStories", e)
            emptyList()
        }
    }

    /**
     * @return `null` si succès, sinon un message exploitable pour l’UI (erreur PostgREST / RLS / réseau).
     */
    suspend fun insertStory(
        id: String,
        authorDisplay: String,
        authorHandle: String,
        contentText: String?,
        imageUrl: String?,
        color: Int = 0xFF1C745E.toInt(),
    ): String? {
        return try {
            // RLS stories_insert_own : auth.uid() doit correspondre à user_id (session obligatoire).
            val sessionUid = SupabaseIdentity.sessionUserIdOrNull()?.trim()?.lowercase()
            if (sessionUid.isNullOrEmpty()) {
                Log.e("StoryRepository", "insertStory: pas de session Supabase (connecte-toi avant de publier)")
                return "Tu n’es pas connecté. Connecte-toi puis réessaie."
            }
            try {
                supabase.auth.refreshCurrentSession()
            } catch (e: Exception) {
                Log.w("StoryRepository", "refreshCurrentSession avant insert (ignoré si déjà valide)", e)
            }
            val row = StoryInsertRow(
                id = id,
                userId = SupabaseIdentity.canonicalUserId(sessionUid),
                authorDisplay = authorDisplay,
                authorHandle = authorHandle.ifBlank { null },
                contentText = contentText,
                imageUrl = imageUrl,
                color = color,
                status = "active",
            )
            // Pas de decode après insert : évite les échecs si la réponse PostgREST diffère du modèle
            // (ex. colonnes optionnelles, préférences) alors que l’INSERT a réussi.
            supabase.from("stories").insert(row)
            null
        } catch (e: Exception) {
            Log.e(
                "StoryRepository",
                "insertStory — vérifie RLS stories_insert_own, colonnes user_id/author_handle, bucket Storage « posts » : ${e.message}",
                e,
            )
            e.storyInsertUserMessage()
        }
    }

    suspend fun updateStoryViews(id: String, views: Int): Boolean {
        return try {
            supabase.from("stories").update({
                set("views", views)
            }) {
                filter { eq("id", id) }
            }
            true
        } catch (e: Exception) {
            Log.e("StoryRepository", "updateStoryViews", e)
            false
        }
    }

    suspend fun updateStoryValidates(id: String, validates: Int): Boolean {
        return try {
            supabase.from("stories").update({
                set("validates", validates)
            }) {
                filter { eq("id", id) }
            }
            true
        } catch (e: Exception) {
            Log.e("StoryRepository", "updateStoryValidates", e)
            false
        }
    }

    private fun StoryRow.toStory(createdAtMs: Long) = Story(
        id = id,
        userId = userId,
        author = authorDisplay,
        authorHandle = authorHandle.orEmpty(),
        contentText = contentText,
        contentImageUrl = imageUrl,
        color = color,
        createdAtMs = createdAtMs,
        views = views,
        validates = validates,
    )
}

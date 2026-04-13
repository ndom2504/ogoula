package com.example.ogoula.data

import android.util.Log
import com.example.ogoula.ui.Story
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
private data class StoryRow(
    val id: String,
    @SerialName("user_id") val userId: String,
    @SerialName("author_display") val authorDisplay: String,
    @SerialName("content_text") val contentText: String? = null,
    @SerialName("image_url") val imageUrl: String? = null,
    val color: Int = 0xFF1C745E.toInt(),
    val status: String = "active",
    @SerialName("moderation_note") val moderationNote: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
)

class StoryRepository {
    private val supabase = SupabaseClient.client

    suspend fun getActiveStories(): List<Story> {
        return try {
            supabase.from("stories")
                .select {
                    filter { eq("status", "active") }
                    order("created_at", order = Order.DESCENDING)
                }
                .decodeList<StoryRow>()
                .map { it.toStory() }
        } catch (e: Exception) {
            Log.w("StoryRepository", "getActiveStories", e)
            emptyList()
        }
    }

    suspend fun insertStory(
        id: String,
        userId: String,
        authorDisplay: String,
        contentText: String?,
        imageUrl: String?,
        color: Int = 0xFF1C745E.toInt(),
    ): Boolean {
        return try {
            val row = StoryRow(
                id = id,
                userId = userId,
                authorDisplay = authorDisplay,
                contentText = contentText,
                imageUrl = imageUrl,
                color = color,
                status = "active",
                moderationNote = null,
            )
            supabase.from("stories").insert(row)
            true
        } catch (e: Exception) {
            Log.e("StoryRepository", "insertStory", e)
            false
        }
    }

    private fun StoryRow.toStory() = Story(
        id = id,
        author = authorDisplay,
        contentText = contentText,
        contentImageUrl = imageUrl,
        color = color,
    )
}

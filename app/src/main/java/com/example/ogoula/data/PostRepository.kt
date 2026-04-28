package com.example.ogoula.data

import com.example.ogoula.ui.components.Comment
import com.example.ogoula.ui.components.Post
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order

class PostRepository {
    private val supabase = SupabaseClient.client

    suspend fun getPosts(limit: Int = 20, offset: Int = 0): List<Post> {
        return try {
            supabase.from("posts")
                .select {
                    order("time", order = Order.DESCENDING)
                    range(offset.toLong(), (offset + limit - 1).toLong())
                }
                .decodeList<Post>()
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    suspend fun createPost(post: Post): String? {
        // Tentative 1 : insertion complète (avec image_urls + video_url)
        return try {
            supabase.from("posts").insert(post) { select() }.decodeSingle<Post>().id
        } catch (e1: Exception) {
            android.util.Log.w("PostRepository", "Insert complet échoué, retry sans video_url", e1)
            // Tentative 2 : sans video_url (colonne peut-être absente)
            try {
                supabase.from("posts").insert(post.copy(videoUrl = null)) { select() }.decodeSingle<Post>().id
            } catch (e2: Exception) {
                android.util.Log.w("PostRepository", "Retry sans video_url échoué, retry minimal", e2)
                // Tentative 3 : sans image_urls ni video_url
                try {
                    supabase.from("posts").insert(post.copy(imageUrls = emptyList(), videoUrl = null)) { select() }.decodeSingle<Post>().id
                } catch (e3: Exception) {
                    android.util.Log.e("PostRepository", "Toutes les tentatives ont échoué", e3)
                    null
                }
            }
        }
    }

    suspend fun updatePostCounts(postId: String, validates: Int, loves: Int) {
        try {
            supabase.from("posts").update({
                set("validates", validates)
                set("loves", loves)
            }) {
                filter { eq("id", postId) }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun updateComments(postId: String, comments: List<Comment>) {
        try {
            supabase.from("posts").update({
                set("comments", comments)
            }) {
                filter { eq("id", postId) }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun editPost(postId: String, newContent: String) {
        try {
            supabase.from("posts").update({
                set("content", newContent)
            }) {
                filter { eq("id", postId) }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun deletePost(postId: String) {
        try {
            supabase.from("posts").delete {
                filter { eq("id", postId) }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /** Recherche distante sur le contenu, l’auteur et le handle (ILIKE). */
    suspend fun searchPostsByText(rawQuery: String, limit: Int = 40): List<Post> {
        val p = SearchQuerySanitizer.forIlike(rawQuery)
        if (p.isEmpty()) return emptyList()
        val pattern = "%$p%"
        return try {
            val byContent = supabase.from("posts").select {
                filter { ilike("content", pattern) }
                order("time", order = Order.DESCENDING)
                limit(limit.toLong())
            }.decodeList<Post>()
            val byAuthor = supabase.from("posts").select {
                filter { ilike("author", pattern) }
                order("time", order = Order.DESCENDING)
                limit(limit.toLong())
            }.decodeList<Post>()
            val byHandle = supabase.from("posts").select {
                filter { ilike("handle", pattern) }
                order("time", order = Order.DESCENDING)
                limit(limit.toLong())
            }.decodeList<Post>()
            (byContent + byAuthor + byHandle)
                .distinctBy { it.id }
                .sortedByDescending { it.time }
                .take(limit)
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }
    
    @kotlinx.serialization.Serializable
    private data class PollVoteRow(
        @kotlinx.serialization.SerialName("post_id") val postId: String,
        @kotlinx.serialization.SerialName("user_id") val userId: String,
        @kotlinx.serialization.SerialName("option_index") val optionIndex: Int,
    )

    suspend fun castPollVote(postId: String, userId: String, optionIndex: Int) {
        try {
            supabase.from("post_poll_votes").upsert(PollVoteRow(postId, userId, optionIndex))
            // Recompute counts and persist them
            val allVotes = supabase.from("post_poll_votes")
                .select { filter { eq("post_id", postId) } }
                .decodeList<PollVoteRow>()
            val maxIdx = allVotes.maxOfOrNull { it.optionIndex } ?: -1
            if (maxIdx >= 0) {
                val counts = MutableList(maxIdx + 1) { 0 }
                allVotes.forEach { counts[it.optionIndex]++ }
                supabase.from("posts").update({
                    set("poll_vote_counts", counts)
                }) { filter { eq("id", postId) } }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun updatePostProfileInfo(postId: String, newAuthorName: String, newAlias: String, newProfileImage: String?) {
        try {
            supabase.from("posts").update({
                set("author", newAuthorName)
                set("handle", newAlias)
                set("author_image_uri", newProfileImage)
            }) {
                filter { eq("id", postId) }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}

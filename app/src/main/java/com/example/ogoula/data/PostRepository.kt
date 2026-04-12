package com.example.ogoula.data

import com.example.ogoula.ui.components.Comment
import com.example.ogoula.ui.components.Post
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order

class PostRepository {
    private val supabase = SupabaseClient.client

    suspend fun getPosts(): List<Post> {
        return try {
            supabase.from("posts")
                .select {
                    order("time", order = Order.DESCENDING)
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
}

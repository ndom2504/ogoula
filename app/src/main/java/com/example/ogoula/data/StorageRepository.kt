package com.example.ogoula.data

import android.content.ContentResolver
import android.net.Uri
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream

class StorageRepository {
    private val supabase = SupabaseClient.client

    private val profilesBucket = "profiles"
    private val postsBucket    = "posts"

    suspend fun uploadProfileImage(userId: String, bytes: ByteArray): String {
        val url = uploadAndGetUrl(profilesBucket, "$userId/profile.jpg", bytes, "image/jpeg")
        // Timestamp en paramètre = cache-busting : force Coil à re-télécharger à chaque changement
        return "$url?t=${System.currentTimeMillis()}"
    }

    suspend fun uploadBannerImage(userId: String, bytes: ByteArray): String {
        val url = uploadAndGetUrl(profilesBucket, "$userId/banner.jpg", bytes, "image/jpeg")
        return "$url?t=${System.currentTimeMillis()}"
    }

    suspend fun uploadPostImage(postId: String, bytes: ByteArray): String =
        uploadAndGetUrl(postsBucket, "$postId/${System.currentTimeMillis()}.jpg", bytes, "image/jpeg")

    suspend fun uploadStoryImage(storyId: String, bytes: ByteArray): String =
        uploadAndGetUrl(postsBucket, "stories/$storyId/${System.currentTimeMillis()}.jpg", bytes, "image/jpeg")

    suspend fun uploadPostVideo(videoId: String, bytes: ByteArray): String =
        uploadAndGetUrl(postsBucket, "videos/$videoId/${System.currentTimeMillis()}.mp4", bytes, "video/mp4")

    /**
     * Upload vidéo depuis un Uri Android (ContentResolver) pour éviter de lire
     * tout le fichier en mémoire (OOM sur grands fichiers).
     * Retourne l'URL publique ou null en cas d'erreur.
     */
    suspend fun uploadPostVideoFromUri(
        contentResolver: ContentResolver,
        uri: Uri,
        videoId: String
    ): String? = withContext(Dispatchers.IO) {
        val path = "videos/$videoId/${System.currentTimeMillis()}.mp4"
        android.util.Log.d("SupaStorage", "Video upload start → $postsBucket/$path")
        try {
            // Lire par chunks de 8 Mo pour éviter OOM
            val bytes = contentResolver.openInputStream(uri)?.use { stream ->
                val buffer = ByteArrayOutputStream()
                val chunk = ByteArray(8 * 1024 * 1024) // 8 MB
                var n: Int
                while (stream.read(chunk).also { n = it } != -1) {
                    buffer.write(chunk, 0, n)
                }
                buffer.toByteArray()
            }
            if (bytes == null) {
                android.util.Log.e("SupaStorage", "Impossible d'ouvrir le stream vidéo")
                return@withContext null
            }
            supabase.storage.from(postsBucket).upload(path, bytes) { upsert = true }
            val url = supabase.storage.from(postsBucket).publicUrl(path)
            android.util.Log.d("SupaStorage", "Video upload OK → $url")
            url
        } catch (e: Exception) {
            android.util.Log.e("SupaStorage", "Video upload failed", e)
            null
        }
    }

    private suspend fun uploadAndGetUrl(
        bucket: String,
        path: String,
        bytes: ByteArray,
        contentType: String = "image/jpeg"
    ): String {
        android.util.Log.d("SupaStorage", "Upload start → $bucket/$path  (${bytes.size} bytes, $contentType)")
        return withContext(Dispatchers.IO) {
            supabase.storage.from(bucket).upload(path, bytes) {
                upsert = true
                this.contentType = io.ktor.http.ContentType.parse(contentType)
            }
            val url = supabase.storage.from(bucket).publicUrl(path)
            android.util.Log.d("SupaStorage", "Upload OK → $url")
            url
        }
    }
}

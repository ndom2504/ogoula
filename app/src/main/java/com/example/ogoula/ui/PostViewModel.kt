package com.example.ogoula.ui

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.ogoula.data.CommunityRepository
import com.example.ogoula.data.SupabaseIdentity
import com.example.ogoula.data.PostRepository
import com.example.ogoula.data.StorageRepository
import com.example.ogoula.ui.components.Comment
import com.example.ogoula.ui.components.Post
import com.example.ogoula.ui.components.handlesEqual
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

data class Notification(
    val id: Int,
    val title: String,
    val description: String,
    val time: String,
    val type: NotificationType,
    var isRead: Boolean = false
)

enum class NotificationType {
    POST, KONGOSSA, COMMUNITY, SYSTEM
}

data class Community(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "",
    val description: String = "",
    val coverImageUri: String? = null,
    val memberCount: Int = 1,
    /** Aligné sur `public.communities.user_id` (créateur). */
    val creatorUserId: String = "",
)

class PostViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = PostRepository()
    private val communityRepository = CommunityRepository()
    private val storageRepository = StorageRepository()
    private val communityPrefs = application.getSharedPreferences("ogoula_communities", Context.MODE_PRIVATE)
    private val followPrefs    = application.getSharedPreferences("ogoula_follows", Context.MODE_PRIVATE)

    private val _posts = MutableStateFlow<List<Post>>(emptyList())
    val posts: StateFlow<List<Post>> = _posts.asStateFlow()

    private val validatedPostIds = mutableSetOf<String>()
    private val lovedPostIds = mutableSetOf<String>()

    /** Clés `postId|commentId` pour savoir si l'utilisateur courant a réagi sur un commentaire. */
    private val validatedCommentKeys = mutableSetOf<String>()
    private val lovedCommentKeys = mutableSetOf<String>()
    private val commentReactionPrefs =
        application.getSharedPreferences("ogoula_comment_reactions", Context.MODE_PRIVATE)

    private val _communities = mutableStateListOf<Community>()
    val communities: List<Community> get() = _communities

    private val _followedUsers = mutableStateListOf<String>()
    val followedUsers: List<String> get() = _followedUsers

    // Données enrichies des utilisateurs suivis (nom + handle + photo)
    data class FollowedUserInfo(val handle: String, val name: String, val imageUri: String? = null)

    private val _communityNotifications = mutableStateListOf<String>()
    val communityNotifications: List<String> get() = _communityNotifications

    private val _notifications = mutableStateListOf<Notification>()
    val notifications: List<Notification> get() = _notifications
    val unreadNotificationsCount: Int get() = _notifications.count { !it.isRead }

    init {
        loadCommentReactionKeys()
        loadCommunities()
        loadFollowedUsers()
        refresh()
        viewModelScope.launch {
            while (true) {
                delay(15_000)
                refresh()
            }
        }
    }

    // ──────────────────────────────────────────────
    // Communities – persistance SharedPreferences
    // ──────────────────────────────────────────────

    private fun saveCommunities() {
        try {
            val array = JSONArray()
            _communities.forEach { c ->
                val obj = JSONObject()
                obj.put("id", c.id)
                obj.put("name", c.name)
                obj.put("description", c.description)
                c.coverImageUri?.let { obj.put("cover", it) }
                obj.put("memberCount", c.memberCount)
                if (c.creatorUserId.isNotBlank()) obj.put("creatorUserId", c.creatorUserId)
                array.put(obj)
            }
            communityPrefs.edit().putString("communities_json", array.toString()).apply()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun loadCommunities() {
        var needsSave = false
        try {
            val json = communityPrefs.getString("communities_json", null)
            if (json != null) {
                val array = JSONArray(json)
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    val id = obj.getString("id")
                    // Migration : ignorer les 2 communautés fictives par défaut
                    if (id == "1" || id == "2") { needsSave = true; continue }
                    _communities.add(
                        Community(
                            id = id,
                            name = obj.getString("name"),
                            description = obj.getString("description"),
                            coverImageUri = if (obj.has("cover")) obj.getString("cover") else null,
                            memberCount = if (obj.has("memberCount")) obj.getInt("memberCount") else 1,
                            creatorUserId = if (obj.has("creatorUserId")) obj.getString("creatorUserId") else "",
                        )
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        // Réécrire si des entrées par défaut ont été filtrées
        if (needsSave) saveCommunities()
    }

    fun addCommunity(name: String, description: String, coverImageBytes: ByteArray? = null) {
        viewModelScope.launch {
            val coverUrl = coverImageBytes?.let {
                try {
                    storageRepository.uploadPostImage("community_${System.currentTimeMillis()}", it)
                } catch (e: Exception) {
                    android.util.Log.e("PostViewModel", "Échec upload cover communauté", e)
                    null
                }
            }
            val uid = SupabaseIdentity.sessionUserIdOrNull()?.trim()?.lowercase().orEmpty()
            val community = Community(
                name = name,
                description = description,
                coverImageUri = coverUrl,
                memberCount = 1,
                creatorUserId = uid,
            )
            _communities.add(0, community)
            _communityNotifications.add(0, "Nouvelle communauté créée : $name")
            saveCommunities()
            try {
                communityRepository.upsert(community, uid)
                communityPrefs.edit().putBoolean("communities_cloud_synced", true).apply()
            } catch (e: Exception) {
                android.util.Log.e("PostViewModel", "Sync communauté Supabase", e)
            }
        }
    }

    fun deleteCommunity(communityId: String) {
        viewModelScope.launch {
            try {
                communityRepository.delete(communityId)
            } catch (e: Exception) {
                android.util.Log.e("PostViewModel", "Suppression communauté distante", e)
            }
            _communities.removeAll { it.id == communityId }
            saveCommunities()
        }
    }

    // ──────────────────────────────────────────────
    // Posts
    // ──────────────────────────────────────────────

    fun refresh() {
        viewModelScope.launch {
            try {
                val rawPosts = repository.getPosts()
                _posts.value = rawPosts.map { post ->
                    mergeCommentReactionFlags(
                        post.copy(
                            isValidatedByMe = post.id in validatedPostIds,
                            isLovedByMe = post.id in lovedPostIds
                        )
                    )
                }
                refreshCommunitiesFromRemote()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    /** Synchronise le Bled avec Supabase : uniquement les communautés créées par l’utilisateur connecté. */
    private suspend fun refreshCommunitiesFromRemote() {
        try {
            val uid = SupabaseIdentity.sessionUserIdOrNull()?.trim()?.lowercase()
            if (uid.isNullOrEmpty()) return

            val remoteMine = communityRepository.getMine(uid)
            if (remoteMine.isNotEmpty()) {
                _communities.clear()
                _communities.addAll(remoteMine)
                saveCommunities()
                return
            }
            if (_communities.isNotEmpty() && !communityPrefs.getBoolean("communities_cloud_synced", false)) {
                _communities.forEach { communityRepository.upsert(it, uid) }
                communityPrefs.edit().putBoolean("communities_cloud_synced", true).apply()
                val again = communityRepository.getMine(uid)
                if (again.isNotEmpty()) {
                    _communities.clear()
                    _communities.addAll(again)
                    saveCommunities()
                }
                return
            }

            _communities.clear()
            saveCommunities()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun addPost(
        content: String,
        author: String = "Moi",
        handle: String = "@anonyme",
        authorImageUri: String? = null,
        imageBytes: List<ByteArray> = emptyList(),
        videoBytes: ByteArray? = null,
        videoUri: Uri? = null          // ← upload direct depuis URI (évite OOM)
    ) {
        viewModelScope.launch {
            val uploadedUrls = imageBytes.mapNotNull { bytes ->
                try {
                    storageRepository.uploadPostImage(UUID.randomUUID().toString(), bytes)
                } catch (e: Exception) {
                    android.util.Log.e("PostViewModel", "Échec upload image post", e)
                    null
                }
            }

            // Priorité : URI direct (streaming, évite OOM) → sinon bytes déjà lus
            val videoUrl: String? = when {
                videoUri != null -> storageRepository.uploadPostVideoFromUri(
                    getApplication<Application>().contentResolver,
                    videoUri,
                    UUID.randomUUID().toString()
                )
                videoBytes != null -> try {
                    storageRepository.uploadPostVideo(UUID.randomUUID().toString(), videoBytes)
                } catch (e: Exception) {
                    android.util.Log.e("PostViewModel", "Échec upload vidéo bytes", e)
                    null
                }
                else -> null
            }

            // Encoder l'URL vidéo dans image_urls (préfixe "video:") comme fallback
            // au cas où la colonne video_url n'existerait pas encore dans Supabase.
            val allImageUrls = buildList {
                addAll(uploadedUrls)
                videoUrl?.let { add("video:$it") }
            }
            val newPost = Post(
                id = UUID.randomUUID().toString(),
                author = author,
                handle = handle,
                content = content,
                time = System.currentTimeMillis(),
                validates = 0,
                loves = 0,
                imageUrls = allImageUrls,
                authorImageUri = authorImageUri,
                videoUrl = videoUrl
            )
            repository.createPost(newPost)
            refresh()
        }
    }

    fun deletePost(postId: String) {
        _posts.value = _posts.value.filter { it.id != postId }
        viewModelScope.launch {
            repository.deletePost(postId)
        }
    }

    fun editPost(postId: String, newContent: String) {
        _posts.value = _posts.value.map {
            if (it.id == postId) it.copy(content = newContent) else it
        }
        viewModelScope.launch {
            repository.editPost(postId, newContent)
        }
    }

    fun toggleValidate(postId: String) {
        val post = _posts.value.find { it.id == postId } ?: return
        val isAlreadyValidated = post.id in validatedPostIds
        if (isAlreadyValidated) validatedPostIds.remove(postId) else validatedPostIds.add(postId)
        val newValids = if (isAlreadyValidated) post.validates - 1 else post.validates + 1
        _posts.value = _posts.value.map {
            if (it.id == postId) it.copy(validates = newValids, isValidatedByMe = !isAlreadyValidated) else it
        }
        viewModelScope.launch {
            repository.updatePostCounts(postId, newValids, post.loves)
        }
    }

    fun toggleLove(postId: String) {
        val post = _posts.value.find { it.id == postId } ?: return
        val isAlreadyLoved = post.id in lovedPostIds
        if (isAlreadyLoved) lovedPostIds.remove(postId) else lovedPostIds.add(postId)
        val newLoves = if (isAlreadyLoved) post.loves - 1 else post.loves + 1
        _posts.value = _posts.value.map {
            if (it.id == postId) it.copy(loves = newLoves, isLovedByMe = !isAlreadyLoved) else it
        }
        viewModelScope.launch {
            repository.updatePostCounts(postId, post.validates, newLoves)
        }
    }

    private fun commentReactionKey(postId: String, commentId: String) = "$postId|$commentId"

    private fun loadCommentReactionKeys() {
        validatedCommentKeys.addAll(commentReactionPrefs.getStringSet("validated", emptySet()) ?: emptySet())
        lovedCommentKeys.addAll(commentReactionPrefs.getStringSet("loved", emptySet()) ?: emptySet())
    }

    private fun saveCommentReactionKeys() {
        commentReactionPrefs.edit()
            .putStringSet("validated", validatedCommentKeys.toSet())
            .putStringSet("loved", lovedCommentKeys.toSet())
            .apply()
    }

    private fun mergeCommentReactionFlags(post: Post): Post {
        return post.copy(
            comments = post.comments.map { c ->
                val key = commentReactionKey(post.id, c.id)
                c.copy(
                    isValidatedByMe = key in validatedCommentKeys,
                    isLovedByMe = key in lovedCommentKeys
                )
            }
        )
    }

    fun addComment(
        postId: String,
        author: String,
        text: String,
        authorImageUri: String? = null,
        authorHandle: String = ""
    ) {
        val post = _posts.value.find { it.id == postId } ?: return
        val newComment = Comment(
            id = UUID.randomUUID().toString(),
            author = author,
            text = text,
            time = System.currentTimeMillis(),
            authorImageUri = authorImageUri,
            authorHandle = authorHandle.trim()
        )
        val updatedComments = post.comments + newComment
        _posts.value = _posts.value.map {
            if (it.id == postId) mergeCommentReactionFlags(it.copy(comments = updatedComments)) else it
        }
        viewModelScope.launch {
            repository.updateComments(postId, updatedComments)
        }
    }

    fun toggleCommentValidate(postId: String, commentId: String, actorHandle: String) {
        val actor = actorHandle.trim()
        if (actor.isEmpty()) return
        val post = _posts.value.find { it.id == postId } ?: return
        val comment = post.comments.find { it.id == commentId } ?: return
        if (handlesEqual(comment.authorHandle, actor)) return
        val key = commentReactionKey(postId, commentId)
        val was = key in validatedCommentKeys
        if (was) validatedCommentKeys.remove(key) else validatedCommentKeys.add(key)
        saveCommentReactionKeys()
        val newCount = if (was) (comment.validates - 1).coerceAtLeast(0) else comment.validates + 1
        val updatedComments = post.comments.map {
            if (it.id == commentId) it.copy(validates = newCount, isValidatedByMe = !was) else it
        }
        _posts.value = _posts.value.map {
            if (it.id == postId) mergeCommentReactionFlags(it.copy(comments = updatedComments)) else it
        }
        viewModelScope.launch {
            repository.updateComments(postId, updatedComments.map { c -> c.copy(isValidatedByMe = false, isLovedByMe = false) })
        }
    }

    fun toggleCommentLove(postId: String, commentId: String, actorHandle: String) {
        val actor = actorHandle.trim()
        if (actor.isEmpty()) return
        val post = _posts.value.find { it.id == postId } ?: return
        val comment = post.comments.find { it.id == commentId } ?: return
        if (handlesEqual(comment.authorHandle, actor)) return
        val key = commentReactionKey(postId, commentId)
        val was = key in lovedCommentKeys
        if (was) lovedCommentKeys.remove(key) else lovedCommentKeys.add(key)
        saveCommentReactionKeys()
        val newCount = if (was) (comment.loves - 1).coerceAtLeast(0) else comment.loves + 1
        val updatedComments = post.comments.map {
            if (it.id == commentId) it.copy(loves = newCount, isLovedByMe = !was) else it
        }
        _posts.value = _posts.value.map {
            if (it.id == postId) mergeCommentReactionFlags(it.copy(comments = updatedComments)) else it
        }
        viewModelScope.launch {
            repository.updateComments(postId, updatedComments.map { c -> c.copy(isValidatedByMe = false, isLovedByMe = false) })
        }
    }

    fun markNotificationsAsRead() {
        _notifications.forEach { it.isRead = true }
    }

    fun toggleFollow(userHandle: String) {
        if (_followedUsers.contains(userHandle)) _followedUsers.remove(userHandle)
        else _followedUsers.add(userHandle)
        saveFollowedUsers()
    }

    private fun saveFollowedUsers() {
        followPrefs.edit().putStringSet("followed", _followedUsers.toSet()).apply()
    }

    private fun loadFollowedUsers() {
        val saved = followPrefs.getStringSet("followed", emptySet()) ?: emptySet()
        _followedUsers.addAll(saved)
    }

    /** Retourne les infos des utilisateurs suivis à partir des posts chargés */
    fun getFollowedUsersInfo(): List<FollowedUserInfo> {
        return _followedUsers.map { handle ->
            val post = _posts.value.firstOrNull { it.handle == handle }
            FollowedUserInfo(handle = handle, name = post?.author ?: handle, imageUri = post?.authorImageUri)
        }
    }

    fun getPopularityScore(userAlias: String): Int {
        var total = 0
        _posts.value.forEach { post ->
            if (handlesEqual(post.handle, userAlias)) {
                total += (post.validates * 10) + (post.loves * 5) + (post.comments.size * 2)
            }
            post.comments.forEach { c ->
                if (c.authorHandle.isNotBlank() && handlesEqual(c.authorHandle, userAlias)) {
                    total += (c.validates * 10) + (c.loves * 5)
                }
            }
        }
        return total
    }
}

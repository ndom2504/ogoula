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
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.example.ogoula.workers.DeadlineNotificationWorker
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID
import java.util.concurrent.TimeUnit
import kotlinx.serialization.Serializable
import org.json.JSONArray
import org.json.JSONObject

data class Notification(
    val id: Int,
    val title: String,
    val description: String,
    val time: String,
    val type: NotificationType,
    var isRead: Boolean = false
)

@Serializable
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
    
    // Pagination support
    private var currentOffset = 0
    private val pageSize = 20
    private var hasMorePosts = true
    private var isLoadingMore = false

    private val validatedPostIds = mutableSetOf<String>()
    private val lovedPostIds = mutableSetOf<String>()
    private val favoritedPostIds = mutableSetOf<String>()

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
        currentOffset = 0
        hasMorePosts = true
        viewModelScope.launch {
            try {
                val rawPosts = repository.getPosts(limit = pageSize, offset = 0)
                _posts.value = rawPosts.map { post ->
                    mergeCommentReactionFlags(
                        post.copy(
                            isValidatedByMe = post.id in validatedPostIds,
                            isLovedByMe = post.id in lovedPostIds
                        )
                    )
                }
                currentOffset = pageSize
                hasMorePosts = rawPosts.size == pageSize
                refreshCommunitiesFromRemote()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun loadMorePosts() {
        if (isLoadingMore || !hasMorePosts) return
        isLoadingMore = true
        viewModelScope.launch {
            try {
                val newPosts = repository.getPosts(limit = pageSize, offset = currentOffset)
                if (newPosts.isNotEmpty()) {
                    val processedPosts = newPosts.map { post ->
                        mergeCommentReactionFlags(
                            post.copy(
                                isValidatedByMe = post.id in validatedPostIds,
                                isLovedByMe = post.id in lovedPostIds
                            )
                        )
                    }
                    _posts.value = _posts.value + processedPosts
                    currentOffset += pageSize
                    hasMorePosts = newPosts.size == pageSize
                } else {
                    hasMorePosts = false
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                isLoadingMore = false
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

    // ── État votes sondages et pétitions ──────────────────────────────────────
    private val _pollVotes = MutableStateFlow<Map<String, Int>>(emptyMap()) // postId → optionIndex
    val pollVotes: StateFlow<Map<String, Int>> = _pollVotes.asStateFlow()

    fun voteOnPoll(postId: String, optionIndex: Int) {
        viewModelScope.launch {
            val userId = SupabaseIdentity.sessionUserIdOrNull() ?: return@launch
            if (_pollVotes.value.containsKey(postId)) return@launch // un seul vote
            _pollVotes.value = _pollVotes.value + (postId to optionIndex)
            _posts.value = _posts.value.map { p ->
                if (p.id != postId) return@map p
                val counts = p.pollVoteCounts.toMutableList()
                while (counts.size < p.pollOptions.size) counts.add(0)
                if (optionIndex < counts.size) counts[optionIndex]++
                p.copy(pollVoteCounts = counts)
            }
            repository.castPollVote(postId, userId, optionIndex)
        }
    }

    fun addPost(
        content: String,
        author: String = "Moi",
        handle: String = "@anonyme",
        authorImageUri: String? = null,
        imageBytes: List<ByteArray> = emptyList(),
        videoBytes: ByteArray? = null,
        videoUri: Uri? = null,          // ← upload direct depuis URI (évite OOM)
        postType: String = "classique",
        pollOptions: List<String> = emptyList(),
        voteOptionImageUris: List<Uri?> = emptyList(),
        goalCount: Int = 0,
        deadlineDays: Int = 0,
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
            // Upload images illustratives par option de vote
            val pollOptionImages: List<String> = voteOptionImageUris.map { uri ->
                if (uri != null) {
                    try {
                        getApplication<Application>().contentResolver.openInputStream(uri)?.use { stream ->
                            storageRepository.uploadPostImage(UUID.randomUUID().toString(), stream.readBytes()) ?: ""
                        } ?: ""
                    } catch (e: Exception) {
                        android.util.Log.e("PostViewModel", "Echec upload image option vote", e)
                        ""
                    }
                } else ""
            }

            val initialCounts = if (pollOptions.isNotEmpty()) List(pollOptions.size) { 0 } else emptyList()
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
                videoUrl = videoUrl,
                postType = postType,
                pollOptions = pollOptions,
                pollVoteCounts = initialCounts,
                pollOptionImages = pollOptionImages,
                goalCount = goalCount,
                deadlineAt = if (deadlineDays > 0) System.currentTimeMillis() + deadlineDays.toLong() * 86_400_000L else null,
            )
            repository.createPost(newPost)
            refresh()
            if (deadlineDays > 0) {
                val deadlineMs = newPost.deadlineAt ?: return@launch
                scheduleDeadlineNotification(newPost.id, content, postType, deadlineMs)
            }
        }
    }

    private fun scheduleDeadlineNotification(postId: String, content: String, postType: String, deadlineAt: Long) {
        val app = getApplication<Application>()
        val delay = deadlineAt - System.currentTimeMillis()
        if (delay <= 0) return
        val data = workDataOf("post_id" to postId, "content" to content, "type" to postType)
        WorkManager.getInstance(app)
            .enqueue(OneTimeWorkRequestBuilder<DeadlineNotificationWorker>()
                .setInitialDelay(delay, TimeUnit.MILLISECONDS)
                .setInputData(data)
                .build())
        if (delay > 86_400_000L) {
            val reminderData = workDataOf("post_id" to postId, "content" to content, "type" to postType, "is_reminder" to true)
            WorkManager.getInstance(app)
                .enqueue(OneTimeWorkRequestBuilder<DeadlineNotificationWorker>()
                    .setInitialDelay(delay - 86_400_000L, TimeUnit.MILLISECONDS)
                    .setInputData(reminderData)
                    .build())
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

    fun createProductPost(
        productUrl: String,
        productTitle: String,
        productPrice: String = "",
        productImage: String = "",
    ) {
        viewModelScope.launch {
            try {
                val post = Post(
                    id = UUID.randomUUID().toString(),
                    author = "Ogoula Admin",
                    handle = "@admin",
                    content = productTitle,
                    time = System.currentTimeMillis(),
                    postType = "classique",
                    productUrl = productUrl,
                    productTitle = productTitle,
                    productPrice = productPrice,
                    productImage = productImage
                )
                repository.createPost(post)
                // Refresh the posts list
                refresh()
            } catch (e: Exception) {
                e.printStackTrace()
            }
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

    fun toggleFavorite(postId: String) {
        val post = _posts.value.find { it.id == postId } ?: return
        val isAlreadyFavorited = post.id in favoritedPostIds
        if (isAlreadyFavorited) favoritedPostIds.remove(postId) else favoritedPostIds.add(postId)
        val newFavorites = if (isAlreadyFavorited) post.favorites - 1 else post.favorites + 1
        _posts.value = _posts.value.map {
            if (it.id == postId) it.copy(favorites = newFavorites, isFavoritedByMe = !isAlreadyFavorited) else it
        }
        // TODO: Sauvegarder dans la base de données si nécessaire
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

    /**
     * Calcule le score d'influence intelligent (Phase 1 Forte).
     * Basé sur :
     * 1. La pertinence des votes (Oracle factor) : a voté pour le gagnant d'un duel/sondage.
     * 2. La qualité du contenu : validations et partages reçus.
     * 3. La qualité du débat : validations reçues sur ses commentaires.
     */
    fun getInfluenceScore(userAlias: String): Int {
        var score = 0
        val allPosts = _posts.value
        
        // 1. Pertinence des Votes (The "Sage" Factor)
        _pollVotes.value.forEach { (postId, votedIndex) ->
            val post = allPosts.find { it.id == postId }
            if (post != null && post.pollVoteCounts.isNotEmpty()) {
                val maxVotes = post.pollVoteCounts.maxOrNull() ?: 0
                val totalVotes = post.pollVoteCounts.sum()
                val winnerIndex = post.pollVoteCounts.indexOf(maxVotes)
                
                // Si l'utilisateur a voté pour l'option majoritaire dans un scrutin significatif (>10 votes)
                if (votedIndex == winnerIndex && totalVotes > 10) {
                    score += 25 
                }
            }
        }
        
        allPosts.forEach { post ->
            // 2. Qualité du Contenu (Auteur)
            if (handlesEqual(post.handle, userAlias)) {
                score += (post.validates * 15)  // Validations par les pairs
                score += (post.shares * 30)     // Viralité / Utilité
                score += (post.favorites * 10)  // Curation
                score += (post.comments.size * 2) // Engagement généré
            }
            
            // 3. Qualité du Débat (Commentateur)
            post.comments.forEach { comment ->
                if (handlesEqual(comment.authorHandle, userAlias)) {
                    score += (comment.validates * 10) // Apport de valeur à la discussion
                    score += (comment.loves * 5)
                }
            }
        }
        return score
    }

    /** Détermine si un utilisateur mérite le badge "Expert" (Phase 1 Forte) */
    fun isExpert(userAlias: String): Boolean {
        // Seuil d'expertise : par exemple 300 points d'influence
        return getInfluenceScore(userAlias) >= 300
    }

    fun getPopularityScore(userAlias: String): Int = getInfluenceScore(userAlias)
    
    /**
     * Met à jour les informations de profil dans un post spécifique
     * Utilisé pour la synchronisation automatique des profils
     */
    fun updatePostProfileInfo(postId: String, newAuthorName: String, newAlias: String, newProfileImage: String?) {
        viewModelScope.launch {
            try {
                // Mettre à jour le post localement
                val updatedPosts = _posts.value.map { post ->
                    if (post.id == postId) {
                        post.copy(
                            author = newAuthorName,
                            handle = newAlias,
                            authorImageUri = newProfileImage
                        )
                    } else {
                        post
                    }
                }
                _posts.value = updatedPosts
                
                // Mettre à jour dans la base de données
                repository.updatePostProfileInfo(postId, newAuthorName, newAlias, newProfileImage)
                
                android.util.Log.d("PostViewModel", "Profil mis à jour pour le post $postId")
            } catch (e: Exception) {
                android.util.Log.e("PostViewModel", "Erreur mise à jour profil post", e)
            }
        }
    }
    
    /**
     * Rafraîchit tous les posts depuis la base de données
     * Utilisé pour la synchronisation automatique
     */
    fun refreshPosts() {
        viewModelScope.launch {
            try {
                refresh() // Recharger tous les posts
                android.util.Log.d("PostViewModel", "Posts rafraîchis pour synchronisation")
            } catch (e: Exception) {
                android.util.Log.e("PostViewModel", "Erreur rafraîchissement posts", e)
            }
        }
    }
}

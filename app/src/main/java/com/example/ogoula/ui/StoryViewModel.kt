package com.example.ogoula.ui

import android.app.Application
import android.content.Context
import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.ogoula.data.StoryRepository
import com.example.ogoula.ui.components.handlesEqual
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID
import kotlin.math.max

private const val STORY_VISIBLE_MS = 24L * 60L * 60L * 1000L

data class Story(
    val id: String,
    val userId: String = "",
    val author: String,
    /** Handle du créateur (ex. @alias) — pour le score et « pas valider sa propre story ». */
    val authorHandle: String = "",
    val contentText: String? = null,
    val contentImageUrl: String? = null,
    val color: Int = 0xFF1C745E.toInt(),
    val createdAtMs: Long = 0L,
    val views: Int = 0,
    val validates: Int = 0,
    val isValidatedByMe: Boolean = false,
) {
    val expiresAtMs: Long
        get() = if (createdAtMs > 0L) createdAtMs + STORY_VISIBLE_MS else Long.MAX_VALUE
}

class StoryViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = StoryRepository()
    private val storyReactionPrefs =
        application.getSharedPreferences("ogoula_story_reactions", Context.MODE_PRIVATE)
    private val viewCountPrefs =
        application.getSharedPreferences("ogoula_story_views_recorded", Context.MODE_PRIVATE)

    private val _stories = mutableStateListOf<Story>()
    val stories: List<Story> get() = _stories

    private val validatedStoryIds = mutableSetOf<String>()

    init {
        loadStoryReactionState()
        // Pas de refresh ici : la session JWT n’est pas toujours prête au démarrage du ViewModel.
        // Rafraîchissement après auth (MainActivity) et depuis l’accueil (HomeScreen).
    }

    private fun loadStoryReactionState() {
        validatedStoryIds.addAll(storyReactionPrefs.getStringSet("validated", emptySet()) ?: emptySet())
    }

    private fun saveStoryReactionState() {
        storyReactionPrefs.edit()
            .putStringSet("validated", validatedStoryIds.toSet())
            .apply()
    }

    private fun mergeFlags(story: Story) = story.copy(isValidatedByMe = story.id in validatedStoryIds)

    fun refresh() {
        viewModelScope.launch {
            val list = repository.getActiveStories().map { mergeFlags(it) }
            _stories.clear()
            _stories.addAll(list)
        }
    }

    /**
     * Publie une story et met à jour la liste locale une fois l’insertion Supabase réussie.
     * À appeler depuis une coroutine (ex. après upload image).
     */
    suspend fun publishStory(
        author: String,
        authorHandle: String,
        text: String? = null,
        imageUrl: String? = null,
    ): Pair<Boolean, String?> {
        val textTrim = text?.trim().orEmpty()
        val image = imageUrl?.trim().orEmpty()
        if (textTrim.isEmpty() && image.isEmpty()) {
            android.util.Log.w("StoryViewModel", "publishStory: ni texte ni image")
            return false to "Ajoute un texte ou une image."
        }
        val id = UUID.randomUUID().toString()
        val insertErr = repository.insertStory(
            id = id,
            authorDisplay = author.ifBlank { "Moi" },
            authorHandle = authorHandle.trim(),
            contentText = textTrim.ifEmpty { null },
            imageUrl = image.ifEmpty { null },
        )
        if (insertErr != null) return false to insertErr
        val list = repository.getActiveStories().map { mergeFlags(it) }
        withContext(Dispatchers.Main.immediate) {
            _stories.clear()
            _stories.addAll(list)
        }
        return true to null
    }

    /**
     * Une vue comptée par couple (spectateur, story). N’incrémente pas si l’auteur ouvre sa propre story.
     */
    fun recordStoryViewIfNeeded(viewerUserId: String, story: Story) {
        if (viewerUserId.isBlank() || story.userId == viewerUserId) return
        val key = "$viewerUserId|${story.id}"
        if (viewCountPrefs.getBoolean(key, false)) return
        viewModelScope.launch {
            val current = _stories.find { it.id == story.id } ?: return@launch
            val next = current.views + 1
            if (repository.updateStoryViews(story.id, next)) {
                replaceStoryInList(story.id) { it.copy(views = next) }
                viewCountPrefs.edit().putBoolean(key, true).apply()
            }
        }
    }

    fun toggleStoryValidate(storyId: String, actorHandle: String) {
        if (actorHandle.isBlank()) return
        val story = _stories.find { it.id == storyId } ?: return
        if (handlesEqual(story.authorHandle, actorHandle)) return
        val was = storyId in validatedStoryIds
        if (was) validatedStoryIds.remove(storyId) else validatedStoryIds.add(storyId)
        saveStoryReactionState()
        val newCount = if (was) max(0, story.validates - 1) else story.validates + 1
        replaceStoryInList(storyId) {
            it.copy(validates = newCount, isValidatedByMe = !was)
        }
        viewModelScope.launch {
            repository.updateStoryValidates(storyId, newCount)
        }
    }

    private fun replaceStoryInList(id: String, transform: (Story) -> Story) {
        val idx = _stories.indexOfFirst { it.id == id }
        if (idx >= 0) {
            val base = _stories[idx]
            _stories[idx] = mergeFlags(transform(base))
        }
    }

    /** Points issus des stories visibles (&lt; 24h) : vues et « Je valide ». */
    fun getStoryPopularityContribution(userAlias: String): Int {
        return _stories
            .filter { it.authorHandle.isNotBlank() && handlesEqual(it.authorHandle, userAlias) }
            .sumOf { it.views * 1 + it.validates * 10 }
    }
}

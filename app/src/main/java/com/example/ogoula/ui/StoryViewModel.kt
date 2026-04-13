package com.example.ogoula.ui

import android.app.Application
import android.content.Context
import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.ogoula.data.StoryRepository
import kotlinx.coroutines.launch
import java.util.UUID

data class Story(
    val id: String,
    val author: String,
    val contentText: String? = null,
    val contentImageUrl: String? = null,
    val color: Int = 0xFF1C745E.toInt(),
)

class StoryViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = StoryRepository()

    private val _stories = mutableStateListOf<Story>()
    val stories: List<Story> get() = _stories

    init {
        // Ancien stockage local (avant Supabase) — on vide pour ne plus mélanger avec les nouvelles stories.
        application.getSharedPreferences("ogoula_stories", Context.MODE_PRIVATE).edit().clear().apply()
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            val list = repository.getActiveStories()
            _stories.clear()
            _stories.addAll(list)
        }
    }

    fun addStory(userId: String, author: String, text: String? = null, imageUrl: String? = null) {
        if (userId.isBlank()) return
        viewModelScope.launch {
            val id = UUID.randomUUID().toString()
            val ok = repository.insertStory(
                id = id,
                userId = userId,
                authorDisplay = author.ifBlank { "Moi" },
                contentText = text,
                imageUrl = imageUrl,
            )
            if (ok) refresh()
        }
    }
}

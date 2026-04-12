package com.example.ogoula.ui

import android.app.Application
import android.content.Context
import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.AndroidViewModel
import org.json.JSONArray
import org.json.JSONObject

data class Story(
    val id: Int,
    val author: String,
    val contentText: String? = null,
    val contentImageUrl: String? = null,   // URL Supabase publique (persistante)
    val color: Int = 0xFF009E60.toInt()
)

class StoryViewModel(application: Application) : AndroidViewModel(application) {
    private val prefs = application.getSharedPreferences("ogoula_stories", Context.MODE_PRIVATE)

    private val _stories = mutableStateListOf<Story>()
    val stories: List<Story> get() = _stories

    init {
        loadStories()
        if (_stories.isEmpty()) {
            _stories.add(Story(1, "Jean", "Direct du PK12 !"))
            _stories.add(Story(2, "Sarah", "Coucher de soleil magnifique"))
        }
    }

    fun addStory(author: String, text: String? = null, imageUrl: String? = null) {
        val newStory = Story(
            id = System.currentTimeMillis().toInt(),
            author = author,
            contentText = text,
            contentImageUrl = imageUrl
        )
        _stories.add(0, newStory)
        saveStories()
    }

    private fun saveStories() {
        try {
            val array = JSONArray()
            _stories.forEach { story ->
                val obj = JSONObject()
                obj.put("id", story.id)
                obj.put("author", story.author)
                story.contentText?.let { obj.put("text", it) }
                story.contentImageUrl?.let { obj.put("imageUrl", it) }
                obj.put("color", story.color)
                array.put(obj)
            }
            prefs.edit().putString("stories_json", array.toString()).apply()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun loadStories() {
        try {
            val json = prefs.getString("stories_json", null) ?: return
            val array = JSONArray(json)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                _stories.add(
                    Story(
                        id = obj.getInt("id"),
                        author = obj.getString("author"),
                        contentText = if (obj.has("text")) obj.getString("text") else null,
                        contentImageUrl = if (obj.has("imageUrl")) obj.getString("imageUrl") else null,
                        color = if (obj.has("color")) obj.getInt("color") else 0xFF009E60.toInt()
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}

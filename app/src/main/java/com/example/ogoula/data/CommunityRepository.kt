package com.example.ogoula.data

import com.example.ogoula.ui.Community
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
private data class CommunityRow(
    val id: String,
    val name: String,
    val description: String = "",
    @SerialName("cover_url") val coverUrl: String? = null,
    @SerialName("member_count") val memberCount: Int = 1,
    @SerialName("user_id") val userId: String? = null,
)

class CommunityRepository {
    private val supabase = SupabaseClient.client

    private fun CommunityRow.toCommunity() = Community(
        id = id,
        name = name,
        description = description,
        coverImageUri = coverUrl,
        memberCount = memberCount,
        creatorUserId = userId.orEmpty(),
    )

    /** Toutes les communautés (ex. admin / migration). */
    suspend fun getAll(): List<Community> {
        return try {
            supabase.from("communities")
                .select {
                    order("created_at", order = Order.DESCENDING)
                }
                .decodeList<CommunityRow>()
                .map { it.toCommunity() }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    /** Communautés créées par l’utilisateur connecté (Bled : « mes » communautés uniquement). */
    suspend fun getMine(creatorUserId: String): List<Community> {
        val uid = creatorUserId.trim().lowercase()
        if (uid.isEmpty()) return emptyList()
        return try {
            supabase.from("communities")
                .select {
                    filter { eq("user_id", uid) }
                    order("created_at", order = Order.DESCENDING)
                }
                .decodeList<CommunityRow>()
                .map { it.toCommunity() }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    suspend fun upsert(community: Community, creatorUserId: String? = null) {
        try {
            val uid = creatorUserId?.trim()?.lowercase().orEmpty().ifEmpty {
                community.creatorUserId.trim().lowercase()
            }
            val row = CommunityRow(
                id = community.id,
                name = community.name,
                description = community.description,
                coverUrl = community.coverImageUri,
                memberCount = community.memberCount,
                userId = uid.ifEmpty { null },
            )
            supabase.from("communities").upsert(row)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun delete(id: String) {
        try {
            supabase.from("communities").delete {
                filter { eq("id", id) }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun searchCommunitiesByText(rawQuery: String, limit: Int = 25): List<Community> {
        val p = SearchQuerySanitizer.forIlike(rawQuery)
        if (p.isEmpty()) return emptyList()
        val pattern = "%$p%"
        return try {
            val byName = supabase.from("communities").select {
                filter { ilike("name", pattern) }
                order("created_at", order = Order.DESCENDING)
                limit(limit.toLong())
            }.decodeList<CommunityRow>().map { it.toCommunity() }
            val byDesc = supabase.from("communities").select {
                filter { ilike("description", pattern) }
                order("created_at", order = Order.DESCENDING)
                limit(limit.toLong())
            }.decodeList<CommunityRow>().map { it.toCommunity() }
            (byName + byDesc)
                .distinctBy { it.id }
                .take(limit)
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }
}

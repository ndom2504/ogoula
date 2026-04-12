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
)

class CommunityRepository {
    private val supabase = SupabaseClient.client

    suspend fun getAll(): List<Community> {
        return try {
            supabase.from("communities")
                .select {
                    order("created_at", order = Order.DESCENDING)
                }
                .decodeList<CommunityRow>()
                .map { row ->
                    Community(
                        id = row.id,
                        name = row.name,
                        description = row.description,
                        coverImageUri = row.coverUrl,
                        memberCount = row.memberCount,
                    )
                }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    suspend fun upsert(community: Community) {
        try {
            val row = CommunityRow(
                id = community.id,
                name = community.name,
                description = community.description,
                coverUrl = community.coverImageUri,
                memberCount = community.memberCount,
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
}

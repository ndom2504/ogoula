package com.example.ogoula.data

import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.postgrest.rpc
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// ── Modèles ──────────────────────────────────────────────────────────────────

@Serializable
data class Leader(
    val id: String = "",
    val name: String = "",
    val country: String = "",
    val role: String = "",
    @SerialName("photo_url") val photoUrl: String? = null,
    val bio: String? = null,
    @SerialName("created_at") val createdAt: String = "",
)

@Serializable
data class Promise(
    val id: String = "",
    @SerialName("leader_id") val leaderId: String = "",
    val title: String = "",
    val description: String? = null,
    val category: String = "Général",
    val status: String = "promis",   // promis | en_cours | tenu | rompu
    @SerialName("source_url") val sourceUrl: String? = null,
    val year: Int? = null,
    @SerialName("votes_kept") val votesKept: Int = 0,
    @SerialName("votes_broken") val votesBroken: Int = 0,
    @SerialName("created_at") val createdAt: String = "",
)

@Serializable
data class PromiseVote(
    val id: String = "",
    @SerialName("promise_id") val promiseId: String = "",
    @SerialName("user_id") val userId: String = "",
    val vote: String = "",   // tenu | rompu
)

// ── Repository ────────────────────────────────────────────────────────────────

class PromiseRepository {
    private val supabase = SupabaseClient.client

    suspend fun getLeaders(): List<Leader> = try {
        supabase.from("leaders")
            .select { order("country", order = Order.ASCENDING) }
            .decodeList<Leader>()
    } catch (e: Exception) {
        e.printStackTrace()
        emptyList()
    }

    suspend fun getLeadersByCountry(country: String): List<Leader> = try {
        supabase.from("leaders")
            .select { filter { eq("country", country) } }
            .decodeList<Leader>()
    } catch (e: Exception) {
        e.printStackTrace()
        emptyList()
    }

    suspend fun getPromisesForLeader(leaderId: String): List<Promise> = try {
        supabase.from("promises")
            .select {
                filter { eq("leader_id", leaderId) }
                order("created_at", order = Order.DESCENDING)
            }
            .decodeList<Promise>()
    } catch (e: Exception) {
        e.printStackTrace()
        emptyList()
    }

    suspend fun getMyVotes(userId: String): List<PromiseVote> = try {
        supabase.from("promise_votes")
            .select { filter { eq("user_id", userId) } }
            .decodeList<PromiseVote>()
    } catch (e: Exception) {
        e.printStackTrace()
        emptyList()
    }

    @Serializable
    private data class VoteParams(
        @SerialName("p_promise_id") val pPromiseId: String,
        @SerialName("p_user_id") val pUserId: String,
        @SerialName("p_vote") val pVote: String,
    )

    /** Vote ou annule si déjà voté dans le même sens (toggle). */
    suspend fun castVote(promiseId: String, userId: String, vote: String): Boolean = try {
        supabase.postgrest.rpc("upsert_promise_vote", VoteParams(promiseId, userId, vote))
        true
    } catch (e: Exception) {
        e.printStackTrace()
        false
    }

    suspend fun addLeader(leader: Leader): Boolean = try {
        supabase.from("leaders").insert(leader)
        true
    } catch (e: Exception) {
        e.printStackTrace()
        false
    }

    suspend fun addPromise(promise: Promise): Boolean = try {
        supabase.from("promises").insert(promise)
        true
    } catch (e: Exception) {
        e.printStackTrace()
        false
    }
}

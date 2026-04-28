package com.example.ogoula.data

import kotlinx.serialization.Serializable
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order
import com.example.ogoula.data.SupabaseClient

/**
 * Représente une demande de suivi/envoi d'amis
 */
@Serializable
data class FollowRequest(
    val id: String = "",
    val senderId: String = "",
    val senderAlias: String = "",
    val senderName: String = "",
    val senderProfileImage: String? = null,
    val receiverId: String = "",
    val receiverAlias: String = "",
    val status: FollowRequestStatus = FollowRequestStatus.PENDING,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

/**
 * Statuts possibles pour une demande de suivi
 */
@Serializable
enum class FollowRequestStatus {
    PENDING,    // En attente de réponse
    ACCEPTED,   // Acceptée - conversation créée
    REJECTED,   // Refusée
    CANCELLED   // Annulée par l'expéditeur
}

/**
 * Repository pour la gestion des demandes de suivi
 */
class FollowRequestRepository {
    private val supabase = SupabaseClient.client
    
    /**
     * Envoyer une demande de suivi
     */
    suspend fun sendFollowRequest(
        senderId: String,
        senderAlias: String,
        senderName: String,
        senderProfileImage: String?,
        receiverId: String,
        receiverAlias: String
    ): Result<FollowRequest> {
        return try {
            // Vérifier si une demande existe déjà
            val existingRequest = getExistingRequest(senderId, receiverId)
            if (existingRequest != null) {
                return Result.failure(Exception("Une demande existe déjà"))
            }
            
            val request = FollowRequest(
                id = java.util.UUID.randomUUID().toString(),
                senderId = senderId,
                senderAlias = senderAlias,
                senderName = senderName,
                senderProfileImage = senderProfileImage,
                receiverId = receiverId,
                receiverAlias = receiverAlias,
                status = FollowRequestStatus.PENDING
            )
            
            supabase.from("follow_requests").insert(request)
            Result.success(request)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Récupérer les demandes reçues (pour l'utilisateur connecté)
     */
    suspend fun getReceivedRequests(receiverId: String): List<FollowRequest> {
        return try {
            supabase.from("follow_requests")
                .select {
                    order("created_at", order = Order.DESCENDING)
                }
                .decodeList<FollowRequest>()
                .filter { it.receiverId == receiverId }
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    /**
     * Récupérer les demandes envoyées (pour l'utilisateur connecté)
     */
    suspend fun getSentRequests(senderId: String): List<FollowRequest> {
        return try {
            supabase.from("follow_requests")
                .select {
                    order("created_at", order = Order.DESCENDING)
                }
                .decodeList<FollowRequest>()
                .filter { it.senderId == senderId }
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    /**
     * Accepter une demande de suivi
     */
    suspend fun acceptRequest(requestId: String): Result<FollowRequest> {
        return try {
            supabase.from("follow_requests").update(mapOf(
                "status" to FollowRequestStatus.ACCEPTED.name,
                "updated_at" to System.currentTimeMillis()
            )) {
                filter { eq("id", requestId) }
            }
            
            // Récupérer la demande mise à jour
            val request = getRequestById(requestId)
            if (request != null) {
                // Créer automatiquement une conversation dans Kongossa
                createKongossaConversation(request)
                Result.success(request.copy(status = FollowRequestStatus.ACCEPTED))
            } else {
                Result.failure(Exception("Demande non trouvée"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Refuser une demande de suivi
     */
    suspend fun rejectRequest(requestId: String): Result<FollowRequest> {
        return try {
            supabase.from("follow_requests").update(mapOf(
                "status" to FollowRequestStatus.REJECTED.name,
                "updated_at" to System.currentTimeMillis()
            )) {
                filter { eq("id", requestId) }
            }
            
            val request = getRequestById(requestId)
            if (request != null) {
                Result.success(request.copy(status = FollowRequestStatus.REJECTED))
            } else {
                Result.failure(Exception("Demande non trouvée"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Vérifier si une demande existe déjà
     */
    private suspend fun getExistingRequest(senderId: String, receiverId: String): FollowRequest? {
        return try {
            supabase.from("follow_requests")
                .select()
                .decodeList<FollowRequest>()
                .find { 
                    it.senderId == senderId && 
                    it.receiverId == receiverId && 
                    it.status == FollowRequestStatus.PENDING 
                }
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Récupérer une demande par son ID
     */
    private suspend fun getRequestById(requestId: String): FollowRequest? {
        return try {
            supabase.from("follow_requests")
                .select()
                .decodeList<FollowRequest>()
                .find { it.id == requestId }
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Créer automatiquement une conversation Kongossa après acceptation
     */
    private suspend fun createKongossaConversation(request: FollowRequest) {
        try {
            val conversation = mapOf(
                "id" to java.util.UUID.randomUUID().toString(),
                "participant1_id" to request.senderId,
                "participant1_alias" to request.senderAlias,
                "participant2_id" to request.receiverId,
                "participant2_alias" to request.receiverAlias,
                "created_at" to System.currentTimeMillis(),
                "last_message_at" to System.currentTimeMillis(),
                "created_from_follow_request" to true,
                "follow_request_id" to request.id
            )
            
            supabase.from("kongossa_conversations").insert(conversation)
        } catch (e: Exception) {
            // Logger l'erreur mais ne pas faire échouer l'acceptation
            println("Erreur création conversation Kongossa: ${e.message}")
        }
    }
}

package com.example.ogoula.data

import android.util.Log
import io.github.jan.supabase.auth.auth

/**
 * Identifiants utilisateur alignés sur la session Supabase Auth (RLS : `auth.uid()::text = user_id`).
 */
object SupabaseIdentity {

    /** `null` si pas de session (ex. avant connexion). */
    fun sessionUserIdOrNull(): String? =
        try {
            SupabaseClient.client.auth.currentSessionOrNull()?.user?.id?.trim()?.takeIf { it.isNotEmpty() }
        } catch (e: Exception) {
            Log.w("SupabaseIdentity", "sessionUserIdOrNull", e)
            null
        }

    /**
     * Variantes de `user_id` pour la table `profiles` (texte vs UUID, tirets).
     * Utilisé en lecture pour retrouver une ligne même si le format diffère légèrement.
     */
    fun userIdLookupCandidates(raw: String?): List<String> {
        val base = raw?.trim()?.lowercase().orEmpty()
        if (base.isEmpty()) return emptyList()
        val out = LinkedHashSet<String>()
        out.add(base)
        val hex = base.replace("-", "")
        if (hex.length == 32 && hex.all { it in '0'..'9' || it in 'a'..'f' }) {
            val dashed =
                "${hex.take(8)}-${hex.substring(8, 12)}-${hex.substring(12, 16)}-${hex.substring(16, 20)}-${hex.substring(20)}"
            out.add(dashed)
            out.add(hex)
        }
        return out.toList()
    }

    /** Pour INSERT/RLS stories & profil : toujours l’UID de la session si disponible. */
    fun userIdForWritableRow(fallbackFromCaller: String): String {
        val s = sessionUserIdOrNull()
        if (!s.isNullOrBlank()) return canonicalUserId(s)
        return canonicalUserId(fallbackFromCaller)
    }

    /** Même format que `auth.uid()::text` côté Postgres (comparaisons RLS stables). */
    fun canonicalUserId(raw: String): String = raw.trim().lowercase()
}

package com.example.ogoula.data

/**
 * Messages lisibles (sans détails techniques HTTP) pour l’utilisateur.
 * Typographie : phrase en français, majuscule en tête de phrase uniquement ; noms propres (Ogoula, Supabase) conservés.
 */
internal fun userFacingAuthError(throwable: Throwable, isSignUp: Boolean): String {
    val raw = throwable.message.orEmpty()
    val low = raw.lowercase()
    return when {
        "user_already_exists" in low ||
            "already registered" in low ||
            "user already registered" in low ->
            "Cette adresse e-mail est déjà enregistrée. La suppression depuis l’admin Ogoula enlève surtout le profil et les données applicatives, pas le compte d’authentification. Connecte-toi avec cet e-mail, ou supprime l’utilisateur dans le tableau Supabase : Authentication → Users."
        isSignUp && (raw.length > 120 || "http" in low || "headers" in low || "x-" in low) ->
            "Impossible de créer le compte. Réessaie ou vérifie que l’e-mail n’est pas déjà utilisé."
        !isSignUp && (raw.length > 120 || "http" in low || "headers" in low) ->
            "Connexion impossible pour le moment. Réessaie."
        else -> {
            val t = raw.trim()
            when {
                t.length > 160 || "http" in low ->
                    if (isSignUp) "Échec de l’inscription. Réessaie." else "Échec de la connexion. Réessaie."
                else ->
                    t.ifEmpty { if (isSignUp) "Échec de l’inscription." else "Échec de la connexion." }
            }
        }
    }
}

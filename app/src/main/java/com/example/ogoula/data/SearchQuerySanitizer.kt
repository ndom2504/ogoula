package com.example.ogoula.data

/**
 * Évite que les caractères spéciaux des motifs SQL LIKE (`%`, `_`) dans la saisie
 * ne se comportent comme des jokers côté PostgREST.
 */
object SearchQuerySanitizer {
    fun forIlike(raw: String): String =
        raw.trim().take(80).replace("%", "").replace("_", "").replace("\\", "")
}

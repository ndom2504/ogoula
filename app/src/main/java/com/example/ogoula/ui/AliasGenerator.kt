package com.example.ogoula.ui

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.Normalizer
import java.util.Locale

/**
 * Générateur d'alias automatique basé sur les premières lettres du nom + numérotation
 */
object AliasGenerator {
    
    /**
     * Génère un alias à partir du nom complet avec numérotation automatique
     * Format : XX#### où XX = 2 premières lettres du nom et #### = numéro séquentiel
     */
    suspend fun generateAlias(
        firstName: String,
        lastName: String,
        existingAliases: List<String> = emptyList()
    ): String {
        return withContext(Dispatchers.IO) {
            val normalizedFirstName = normalizeString(firstName)
            val normalizedLastName = normalizeString(lastName)
            
            // Prendre les 2 premières lettres du nom complet
            val fullName = "$normalizedFirstName$normalizedLastName"
            val prefix = if (fullName.length >= 2) {
                fullName.take(2).uppercase(Locale.FRENCH)
            } else {
                fullName.uppercase(Locale.FRENCH).padEnd(2, 'X')
            }
            
            // Trouver le prochain numéro disponible
            val existingNumbers = existingAliases
                .filter { it.startsWith(prefix, ignoreCase = true) }
                .mapNotNull { alias ->
                    val numberPart = alias.drop(prefix.length)
                    numberPart.toIntOrNull()
                }
                .sorted()
            
            val nextNumber = findNextAvailableNumber(existingNumbers)
            
            // Format : XX#### (ex: MD0001, AB0123)
            String.format("%s%04d", prefix, nextNumber)
        }
    }
    
    /**
     * Trouve le prochain numéro disponible dans la séquence
     */
    private fun findNextAvailableNumber(existingNumbers: List<Int>): Int {
        if (existingNumbers.isEmpty()) return 1
        
        for (i in 1 until existingNumbers.last() + 2) {
            if (i !in existingNumbers) {
                return i
            }
        }
        return existingNumbers.last() + 1
    }
    
    /**
     * Normalise une chaîne de caractères (supprime accents, espaces, etc.)
     */
    private fun normalizeString(input: String): String {
        return Normalizer.normalize(input, Normalizer.Form.NFD)
            .replace("[\\p{InCombiningDiacriticalMarks}]".toRegex(), "")
            .replace("[^a-zA-Z]".toRegex(), "")
            .lowercase(Locale.FRENCH)
    }
    
    /**
     * Valide si un alias suit le format attendu
     */
    fun isValidAlias(alias: String): Boolean {
        return alias.matches(Regex("^[A-Z]{2}\\d{4}$"))
    }
    
    /**
     * Extrait le préfixe (2 lettres) d'un alias
     */
    fun extractPrefix(alias: String): String? {
        return if (isValidAlias(alias)) alias.take(2) else null
    }
    
    /**
     * Extrait le numéro d'un alias
     */
    fun extractNumber(alias: String): Int? {
        return if (isValidAlias(alias)) {
            alias.drop(2).toIntOrNull()
        } else null
    }
}

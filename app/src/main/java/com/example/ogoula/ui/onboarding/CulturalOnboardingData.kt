package com.example.ogoula.ui.onboarding

/** Ligne de menu : région (non sélectionnable) ou pays. */
sealed class AfricanCountryMenuRow {
    data class Region(val title: String) : AfricanCountryMenuRow()
    data class Country(val name: String) : AfricanCountryMenuRow()
}

/** Liste déroulante : pays et options d’appartenance culturelle. */
val africanCountryMenuRows: List<AfricanCountryMenuRow> = buildList {
    fun r(title: String) {
        add(AfricanCountryMenuRow.Region(title))
    }
    fun c(name: String) {
        add(AfricanCountryMenuRow.Country(name))
    }
    r("Afrique de l'Ouest")
    c("Bénin")
    c("Burkina Faso")
    c("Cap-Vert")
    c("Côte d'Ivoire")
    c("Gambie")
    c("Ghana")
    c("Guinée")
    c("Guinée-Bissau")
    c("Libéria")
    c("Mali")
    c("Niger")
    c("Nigéria")
    c("Sénégal")
    c("Sierra Leone")
    c("Togo")
    r("Afrique centrale")
    c("Cameroun")
    c("Centrafrique")
    c("Congo")
    c("RDC")
    c("Gabon")
    c("Guinée équatoriale")
    c("Tchad")
    c("São Tomé-et-Príncipe")
    r("Afrique de l'Est")
    c("Burundi")
    c("Comores")
    c("Djibouti")
    c("Érythrée")
    c("Éthiopie")
    c("Kenya")
    c("Ouganda")
    c("Rwanda")
    c("Somalie")
    c("Soudan")
    c("Soudan du Sud")
    c("Tanzanie")
    r("Afrique australe")
    c("Afrique du Sud")
    c("Angola")
    c("Botswana")
    c("Eswatini (Swaziland)")
    c("Lesotho")
    c("Malawi")
    c("Mozambique")
    c("Namibie")
    c("Zambie")
    c("Zimbabwe")
    r("Afrique du Nord")
    c("Algérie")
    c("Égypte")
    c("Libye")
    c("Maroc")
    c("Mauritanie")
    c("Tunisie")
    r("Autres liens")
    c("Diaspora")
    c("Afro-descendant")
    c("Ami des cultures et du patrimoine")
}

data class CulturalIntentionOption(val id: String, val label: String)

val culturalIntentionOptions: List<CulturalIntentionOption> = listOf(
    CulturalIntentionOption("gain_visibility", "Gagner en visibilité"),
    CulturalIntentionOption("build_influence", "Construire mon influence"),
    CulturalIntentionOption("community_feedback", "Recevoir des retours de la communauté"),
    CulturalIntentionOption("support_brands", "Soutenir marques et produits locaux"),
    CulturalIntentionOption("network_brands", "Réseauter avec d'autres marques"),
    CulturalIntentionOption("talent_promotion", "Valoriser les talents"),
    CulturalIntentionOption("quality_products", "Promouvoir produits de qualité"),
    CulturalIntentionOption("authentic_interactions", "Créer des interactions authentiques"),
    CulturalIntentionOption("competitive_votes", "Participer aux votes et comparaisons"),
    CulturalIntentionOption("share_expertise", "Partager mon expertise et passion"),
)

data class SelfRoleOption(val id: String, val label: String)

val selfRoleOptions: List<SelfRoleOption> = listOf(
    SelfRoleOption("brand", "Marque"),
    SelfRoleOption("product", "Produit"),
    SelfRoleOption("talent", "Talent"),
    SelfRoleOption("influencer", "Influenceur"),
    SelfRoleOption("community", "Communauté"),
    SelfRoleOption("creative", "Créatif"),
    SelfRoleOption("entrepreneur", "Entrepreneur"),
    SelfRoleOption("fan_advocate", "Fan / Ambassadeur"),
    SelfRoleOption("other", "Autre"),
)

fun intentionLabelForId(id: String): String =
    culturalIntentionOptions.find { it.id == id.trim() }?.label ?: id.trim()

fun roleLabelForId(id: String?): String =
    if (id.isNullOrBlank()) "—" else (selfRoleOptions.find { it.id == id }?.label ?: id)

fun parseIntentionsCsv(csv: String?): List<String> =
    csv?.split(",")?.map { it.trim() }?.filter { it.isNotEmpty() }.orEmpty()

const val CONTRIBUTION_MIN_LEN = 15
const val CONTRIBUTION_MAX_LEN = 200
const val MAX_INTENTIONS = 2

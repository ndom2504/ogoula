package com.example.ogoula.data

import com.ogoula.app.BuildConfig
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.realtime.Realtime
import io.github.jan.supabase.storage.Storage

/** URL et clé anon : `supabase.url` et `supabase.anon.key` dans `local.properties` (voir `local.properties.example`). */
object SupabaseClient {
    val client by lazy {
        require(
            BuildConfig.SUPABASE_URL.isNotBlank() &&
                BuildConfig.SUPABASE_ANON_KEY.isNotBlank(),
        ) {
            "Définis supabase.url et supabase.anon.key dans local.properties (voir local.properties.example)."
        }
        createSupabaseClient(
            supabaseUrl = BuildConfig.SUPABASE_URL,
            supabaseKey = BuildConfig.SUPABASE_ANON_KEY,
        ) {
            install(Auth)
            install(Postgrest)
            install(Storage)
            install(Realtime)
        }
    }
}

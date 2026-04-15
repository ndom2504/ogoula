package com.example.ogoula.data

import android.content.Context
import com.ogoula.app.BuildConfig
import com.russhwolf.settings.Settings
import com.russhwolf.settings.SharedPreferencesSettings
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.SettingsSessionManager
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.realtime.Realtime
import io.github.jan.supabase.storage.Storage

/** URL et clé anon : `supabase.url` et `supabase.anon.key` dans `local.properties` (voir `local.properties.example`). */
object SupabaseClient {

    @Volatile
    private var appContext: Context? = null

    /**
     * À appeler depuis [com.example.ogoula.OgoulaApplication] (ou équivalent) avant le premier accès à [client].
     * Configure une session Auth persistante (SharedPreferences), alignée sur la clé utilisée par supabase-kt.
     */
    fun initAndroidContext(context: Context) {
        if (appContext != null) return
        appContext = context.applicationContext
    }

    private fun authSettings(): Settings {
        val ctx = appContext
            ?: error("SupabaseClient.initAndroidContext(Application) doit être appelé au démarrage.")
        return SharedPreferencesSettings.Factory(ctx).create("ogoula_supabase_auth")
    }

    /** Même logique que `createDefaultSettingsKey` dans supabase-kt pour réutiliser / migrer la session. */
    private fun sessionStorageKey(supabaseUrl: String): String {
        val slug = supabaseUrl.removeSuffix("/").replace('/', '-').replace('.', '-')
        return "sb-$slug-${SettingsSessionManager.SETTINGS_KEY}"
    }

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
            install(Auth) {
                sessionManager = SettingsSessionManager(
                    settings = authSettings(),
                    key = sessionStorageKey(BuildConfig.SUPABASE_URL),
                )
            }
            install(Postgrest)
            install(Storage)
            install(Realtime)
        }
    }
}

package com.example.ogoula

import android.app.Application
import com.example.ogoula.data.SupabaseClient

/**
 * Initialise le stockage de session Supabase avant tout accès au client.
 * Sans [SharedPreferencesSettings], la session peut ne pas survivre aux redémarrages.
 */
class OgoulaApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        SupabaseClient.initAndroidContext(this)
    }
}

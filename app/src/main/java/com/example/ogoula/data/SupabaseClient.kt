package com.example.ogoula.data

import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.realtime.Realtime
import io.github.jan.supabase.storage.Storage

// ⚠️  REMPLIR CES VALEURS depuis ton projet Supabase :
//     Dashboard → Settings → API
const val SUPABASE_URL = "https://vqthzwhuvcglwjwmqeil.supabase.co"
const val SUPABASE_ANON_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InZxdGh6d2h1dmNnbHdqd21xZWlsIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NzU5MzU4MTUsImV4cCI6MjA5MTUxMTgxNX0.aLfvun2C2zxw9IZGS6qI2_QUGw0pZSNlYmcbzqUY7DY"

object SupabaseClient {
    val client = createSupabaseClient(
        supabaseUrl = SUPABASE_URL,
        supabaseKey = SUPABASE_ANON_KEY
    ) {
        install(Auth)
        install(Postgrest)
        install(Storage)
        install(Realtime)
    }
}

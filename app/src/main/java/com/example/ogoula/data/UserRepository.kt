package com.example.ogoula.data

import com.example.ogoula.ui.UserProfile
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest

class UserRepository {
    private val supabase = SupabaseClient.client

    suspend fun saveProfile(userId: String, profile: UserProfile) {
        try {
            supabase.from("profiles").upsert(
                UserProfile(
                    userId = userId,
                    firstName = profile.firstName,
                    lastName = profile.lastName,
                    alias = profile.alias,
                    profileImageUri = profile.profileImageUri,
                    bannerImageUri = profile.bannerImageUri
                )
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun getProfile(userId: String): UserProfile? {
        return try {
            supabase.from("profiles")
                .select {
                    filter { eq("user_id", userId) }
                }
                .decodeSingleOrNull<UserProfile>()
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}

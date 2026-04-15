package com.example.ogoula.data

import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.auth.status.SessionStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AuthRepository {
    private val supabase = SupabaseClient.client

    private val _authState = MutableStateFlow<AuthState>(AuthState.Loading)
    val authState = _authState.asStateFlow()

    sealed class AuthState {
        object LoggedOut  : AuthState()
        object Loading    : AuthState()
        data class Error(val message: String) : AuthState()
        data class Authenticated(val userId: String) : AuthState()
    }

    init {
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            supabase.auth.sessionStatus.collect { status ->
                _authState.value = when (status) {
                    is SessionStatus.Authenticated  -> AuthState.Authenticated(status.session.user?.id ?: "")
                    is SessionStatus.NotAuthenticated -> AuthState.LoggedOut
                    is SessionStatus.Initializing   -> AuthState.Loading
                    is SessionStatus.RefreshFailure -> AuthState.LoggedOut
                }
            }
        }
    }

    suspend fun signIn(email: String, password: String) {
        _authState.value = AuthState.Loading
        try {
            supabase.auth.signInWith(Email) {
                this.email    = email.trim()
                this.password = password
            }
        } catch (e: Exception) {
            e.printStackTrace()
            _authState.value = AuthState.Error("Email ou mot de passe incorrect")
        }
    }

    suspend fun signUp(email: String, password: String) {
        _authState.value = AuthState.Loading
        try {
            supabase.auth.signUpWith(Email) {
                this.email    = email.trim()
                this.password = password
            }
            // Si la confirmation email est désactivée dans Supabase → connecté directement
            // Sinon l'utilisateur doit confirmer son email
        } catch (e: Exception) {
            e.printStackTrace()
            _authState.value = AuthState.Error(userFacingAuthError(e, isSignUp = true))
        }
    }

    suspend fun logout() {
        try { supabase.auth.signOut() } catch (e: Exception) { e.printStackTrace() }
        _authState.value = AuthState.LoggedOut
    }
}

package com.example.ogoula.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ogoula.data.AuthRepository
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class AuthViewModel : ViewModel() {
    private val repository = AuthRepository()

    val authState: StateFlow<AuthRepository.AuthState> = repository.authState

    fun signIn(email: String, password: String) {
        viewModelScope.launch { repository.signIn(email, password) }
    }

    fun signUp(email: String, password: String) {
        viewModelScope.launch { repository.signUp(email, password) }
    }

    fun logout() {
        viewModelScope.launch { repository.logout() }
    }
}

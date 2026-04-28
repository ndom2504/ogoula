package com.example.ogoula.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ogoula.data.Leader
import com.example.ogoula.data.Promise
import com.example.ogoula.data.PromiseRepository
import com.example.ogoula.data.PromiseVote
import com.example.ogoula.data.SupabaseIdentity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class PromiseViewModel : ViewModel() {

    // ── Leader sélectionné (navigation) ──────────────────────────────────────
    var selectedLeader by mutableStateOf<Leader?>(null)
        private set

    fun selectLeader(leader: Leader) {
        selectedLeader = leader
        loadPromisesForLeader(leader.id)
    }

    private val repo = PromiseRepository()

    // ── Leaders ───────────────────────────────────────────────────────────────
    private val _leaders = MutableStateFlow<List<Leader>>(emptyList())
    val leaders: StateFlow<List<Leader>> = _leaders.asStateFlow()

    // ── Promesses du leader sélectionné ──────────────────────────────────────
    private val _promises = MutableStateFlow<List<Promise>>(emptyList())
    val promises: StateFlow<List<Promise>> = _promises.asStateFlow()

    // ── Votes de l'utilisateur courant ────────────────────────────────────────
    private val _myVotes = MutableStateFlow<Map<String, String>>(emptyMap()) // promiseId → vote
    val myVotes: StateFlow<Map<String, String>> = _myVotes.asStateFlow()

    // ── Filtre pays ───────────────────────────────────────────────────────────
    private val _selectedCountry = MutableStateFlow<String?>(null)
    val selectedCountry: StateFlow<String?> = _selectedCountry.asStateFlow()

    // ── Chargement ────────────────────────────────────────────────────────────
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        loadLeaders()
        loadMyVotes()
    }

    fun loadLeaders() {
        viewModelScope.launch {
            _isLoading.value = true
            _leaders.value = repo.getLeaders()
            _isLoading.value = false
        }
    }

    fun loadLeadersByCountry(country: String?) {
        _selectedCountry.value = country
        viewModelScope.launch {
            _isLoading.value = true
            _leaders.value = if (country == null) repo.getLeaders()
                             else repo.getLeadersByCountry(country)
            _isLoading.value = false
        }
    }

    fun loadPromisesForLeader(leaderId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _promises.value = repo.getPromisesForLeader(leaderId)
            _isLoading.value = false
        }
    }

    private fun loadMyVotes() {
        viewModelScope.launch {
            val userId = SupabaseIdentity.sessionUserIdOrNull() ?: return@launch
            val votes = repo.getMyVotes(userId)
            _myVotes.value = votes.associate { it.promiseId to it.vote }
        }
    }

    fun castVote(promiseId: String, vote: String) {
        viewModelScope.launch {
            val userId = SupabaseIdentity.sessionUserIdOrNull() ?: return@launch

            val current = _myVotes.value[promiseId]

            // Toggle optimiste localement
            val newVotes = _myVotes.value.toMutableMap()
            if (current == vote) newVotes.remove(promiseId)   // annule le vote
            else newVotes[promiseId] = vote
            _myVotes.value = newVotes

            // Mise à jour locale du compteur de la promesse
            _promises.value = _promises.value.map { p ->
                if (p.id != promiseId) return@map p
                var vk = p.votesKept
                var vb = p.votesBroken
                if (current == "tenu")  vk = maxOf(vk - 1, 0)
                if (current == "rompu") vb = maxOf(vb - 1, 0)
                if (current != vote) {
                    if (vote == "tenu")  vk++
                    else                 vb++
                }
                p.copy(votesKept = vk, votesBroken = vb)
            }

            // Appel Supabase (RPC atomique)
            repo.castVote(promiseId, userId, vote)
        }
    }

    /** Liste dédupliquée des pays disponibles. */
    val countries: List<String>
        get() = _leaders.value.map { it.country }.distinct().sorted()
}

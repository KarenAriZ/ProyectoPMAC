package com.karenarias.proyectopmac.ui.home

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.karenarias.proyectopmac.data.remote.UserCard
import com.karenarias.proyectopmac.data.repository.*
import com.karenarias.proyectopmac.ui.auth.RechargeUiModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.io.IOException
import java.util.Locale

/** Tarjetas de la sesión activa. Sin datos de ejemplo ni caché compartida entre usuarios. */
class HomeViewModel(
    private val repository: UserCardsRepository = UserCardsRepository(),
    private val operationScope: CoroutineScope? = null
) : ViewModel() {
    private val workScope: CoroutineScope get() = operationScope ?: viewModelScope
    private var session: UserSession? = null
    private var generation = 0
    private var refreshVersion = 0
    private var refreshJob: Job? = null
    private var mutationJob: Job? = null
    var sessionUserId by mutableStateOf<String?>(null)
        private set
    var cards by mutableStateOf<List<UserCard>>(emptyList())
        private set
    var selectedNumber by mutableStateOf<String?>(null)
        private set
    var isLoading by mutableStateOf(false)
        private set
    var isSaving by mutableStateOf(false)
        private set
    var errorMessage by mutableStateOf<String?>(null)
        private set
    var requiresLogin by mutableStateOf(false)
        private set
    var selectedTab by mutableStateOf(0)
        private set
    val recharges: List<RechargeUiModel> = emptyList()
    val selectedCard: UserCard? get() = cards.firstOrNull { it.numeroTarjeta == selectedNumber && it.isActive }
        ?: cards.firstOrNull { it.isActive }
    val cardNumber: String get() = selectedCard?.numeroTarjeta.orEmpty()
    val cardType: String get() = selectedCard?.tipo.orEmpty()
    val cardBalance: String get() = selectedCard?.saldoCentimos?.let { String.format(Locale.US, "S/. %.2f", it / 100.0) } ?: "Sin consultar"
    val isLowBalance: Boolean get() = selectedCard?.let {
        it.saldoCentimos != null && it.saldoCentimos < it.umbralSaldoBajoCentimos &&
            it.alertasActivas && it.estadoTarjeta == "ACTIVA"
    } ?: false

    fun bindSession(value: UserSession?) {
        if (session?.accessToken == value?.accessToken && session?.user?.id == value?.user?.id) return
        generation++
        refreshVersion++
        refreshJob?.cancel()
        mutationJob?.cancel()
        session = value
        sessionUserId = value?.user?.id
        cards = emptyList()
        selectedNumber = null
        selectedTab = 0
        isLoading = false
        isSaving = false
        requiresLogin = false
        errorMessage = null
        if (value != null) refreshData()
    }

    fun onTabSelected(index: Int) { selectedTab = index }
    fun selectCard(number: String) {
        if (cards.any { it.numeroTarjeta == number && it.isActive }) selectedNumber = number
    }

    private fun handleError(error: Exception) {
        errorMessage = when (error) {
            is CardsException -> error.message
            is IOException -> "No pudimos confirmar la operación. Revisa tu conexión y actualiza tus tarjetas antes de reintentar."
            else -> "No se pudo interpretar la respuesta del servicio de tarjetas."
        }
        if (error is CardsException && error.status == 401) {
            cards = emptyList()
            selectedNumber = null
            requiresLogin = true
        }
    }

    fun refreshData() {
        val current = session ?: return
        if (isSaving) return
        val version = generation
        val request = ++refreshVersion
        refreshJob?.cancel()
        isLoading = true
        errorMessage = null
        refreshJob = workScope.launch {
            try {
                val result = repository.list(current)
                if (version == generation && request == refreshVersion) {
                    cards = result
                    selectedNumber = selectedCard?.numeroTarjeta
                }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                if (version == generation && request == refreshVersion) handleError(error)
            } finally {
                if (version == generation && request == refreshVersion) isLoading = false
            }
        }
    }

    private fun mutate(operation: suspend (UserSession) -> UserCard, onSuccess: (UserCard) -> Unit) {
        val current = session ?: return
        if (isSaving) return
        val version = generation
        refreshVersion++
        refreshJob?.cancel()
        isLoading = false
        isSaving = true
        errorMessage = null
        mutationJob = workScope.launch {
            try {
                val result = operation(current)
                if (version == generation) {
                    cards = (cards.filterNot { it.numeroTarjeta == result.numeroTarjeta } + result)
                    if (result.isActive) selectedNumber = result.numeroTarjeta
                    onSuccess(result)
                }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                if (version == generation) handleError(error)
            } finally {
                if (version == generation) isSaving = false
            }
        }
    }

    fun addCard(number: String, alias: String, type: String, onSuccess: () -> Unit) =
        mutate({ repository.add(it, number, alias, type) }, { onSuccess() })

    fun changeCardState(number: String, active: Boolean, onSuccess: (UserCard) -> Unit) =
        mutate({ repository.changeState(it, number, active) }, onSuccess)
}

package com.karenarias.proyectopmac.ui.recharge

import androidx.compose.runtime.*
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.karenarias.proyectopmac.data.remote.*
import com.karenarias.proyectopmac.data.repository.*
import com.karenarias.proyectopmac.ui.claims.*
import kotlinx.coroutines.*
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.UUID

fun UserRecharge.historyRecord(): RechargeRecord {
    val local = Instant.parse(fecha).atZone(ZoneId.of("America/Lima"))
    return RechargeRecord(id, "S/ %.2f".format(java.util.Locale.US, montoCentimos / 100.0),
        local.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")),
        local.format(DateTimeFormatter.ofPattern("HH:mm")), "Recarga exitosa",
        ClaimCard(numeroTarjeta, numeroTarjeta.takeLast(4), tipoTarjeta), metodoPago, aliasTarjeta)
}

class UserRechargesViewModel @JvmOverloads constructor(
    private val savedState: SavedStateHandle,
    private val repository: UserRechargesRepository = UserRechargesRepository(),
    private val operationScope: CoroutineScope? = null
) : ViewModel() {
    private val scope get() = operationScope ?: viewModelScope
    private var session: UserSession? = null
    private var generation = 0
    private var loadVersion = 0
    private var loadJob: Job? = null
    private var saveJob: Job? = null
    var items by mutableStateOf<List<UserRecharge>>(emptyList()); private set
    var isLoading by mutableStateOf(false); private set
    var isSaving by mutableStateOf(false); private set
    var historyError by mutableStateOf<String?>(null); private set
    var saveError by mutableStateOf<String?>(null); private set
    var requiresLogin by mutableStateOf(false); private set
    var pending by mutableStateOf<SimulatedRechargeRequest?>(null); private set
    var receipt by mutableStateOf<UserRecharge?>(null); private set

    fun bindSession(value: UserSession?) {
        if (session?.accessToken == value?.accessToken && session?.user?.id == value?.user?.id) return
        generation++; loadVersion++
        loadJob?.cancel(); saveJob?.cancel()
        session = value
        items = emptyList(); receipt = null; isSaving = false; isLoading = false
        requiresLogin = false; historyError = null; saveError = null
        pending = value?.user?.id?.let { id ->
            runCatching { Gson().fromJson(savedState.get<String>("recharge_$id"), SimulatedRechargeRequest::class.java) }.getOrNull()
        }
        if (value != null) refresh()
    }

    private fun error(error: Exception): String {
        if (error is CardsException && error.status == 401) { requiresLogin = true; items = emptyList(); receipt = null }
        return if (error is CardsException) error.message.orEmpty()
            else "No pudimos confirmar la operación. Revisa tu conexión y reintenta con el mismo código."
    }

    fun refresh() {
        val current = session ?: return
        if (isSaving) return
        val version = generation
        val load = ++loadVersion
        loadJob?.cancel(); isLoading = true; historyError = null
        loadJob = scope.launch {
            try {
                val result = repository.list(current)
                if (version == generation && load == loadVersion) items = result
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) { if (version == generation && load == loadVersion) historyError = error(e) }
            finally { if (version == generation && load == loadVersion) isLoading = false }
        }
    }

    fun submit(number: String, cents: Long, method: String, onConfirmed: () -> Unit) {
        val current = session ?: return
        if (isSaving) return
        // Un intento incierto conserva sus datos y UUID, incluso al recrear la pantalla.
        val request = pending ?: SimulatedRechargeRequest(UUID.randomUUID().toString(), number, cents, method)
        pending = request
        savedState["recharge_${current.user.id}"] = Gson().toJson(request)
        val version = generation
        loadVersion++; loadJob?.cancel(); isLoading = false
        isSaving = true; saveError = null; receipt = null
        saveJob = scope.launch {
            try {
                val result = repository.create(current, request)
                if (version == generation) {
                    receipt = result
                    items = (items.filterNot { it.id == result.id } + result).sortedByDescending { Instant.parse(it.fecha) }
                    pending = null
                    savedState.remove<String>("recharge_${current.user.id}")
                    onConfirmed()
                }
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) {
                if (version == generation) {
                    saveError = error(e)
                    // 400/404 certifican que el servidor rechazó antes de escribir.
                    if (e is CardsException && e.status in listOf(400, 404)) {
                        pending = null; savedState.remove<String>("recharge_${current.user.id}")
                    }
                }
            } finally { if (version == generation) isSaving = false }
        }
    }
}

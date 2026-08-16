package com.example.regibazarapp.ui.screens.routes

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.regibazarapp.data.NetworkMonitor
import com.example.regibazarapp.data.SyncManager
import com.example.regibazarapp.data.TokenManager
import com.example.regibazarapp.data.local.AdminRoutesEntity
import com.example.regibazarapp.data.local.RouteDatabase
import com.example.regibazarapp.data.models.CamiAlertDto
import com.example.regibazarapp.data.models.CommonProductDto
import com.example.regibazarapp.data.models.RouteListDto
import com.example.regibazarapp.data.network.NetworkModule
import com.example.regibazarapp.data.repository.AdminRoutesRepository
import com.example.regibazarapp.data.repository.RepositoryResult
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class RoutesListUiState(
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val routes: List<RouteListDto> = emptyList(),
    val error: String? = null,
    val notice: String? = null,
    val isOffline: Boolean = false,
    val camiAlerts: List<CamiAlertDto> = emptyList(),
    val commonProducts: List<CommonProductDto> = emptyList()
)

class RoutesListViewModel(application: Application) : AndroidViewModel(application) {
    private val tokenManager = TokenManager(application)
    private val repository = AdminRoutesRepository()
    private val routeDao = RouteDatabase.getInstance(application).routeDao()
    private val networkMonitor = NetworkMonitor(application)
    private val gson = Gson()

    private val _uiState = MutableStateFlow(RoutesListUiState())
    val uiState: StateFlow<RoutesListUiState> = _uiState.asStateFlow()

    init {
        ensureAuthToken()
        loadRoutes()
        loadCamiAlerts()
        loadCommonProducts()
        monitorNetwork()
        viewModelScope.launch {
            SyncManager.refreshFlow.collect {
                loadRoutes(showLoading = false)
            }
        }
    }

    private fun ensureAuthToken() {
        if (NetworkModule.jwtToken.isNullOrBlank()) {
            NetworkModule.jwtToken = tokenManager.getJwtTokenSync()
        }
    }

    fun loadRoutes(showLoading: Boolean = true) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoading = showLoading && it.routes.isEmpty(),
                    isRefreshing = !showLoading || it.routes.isNotEmpty(),
                    error = null,
                    notice = null
                )
            }

            when (val result = repository.getRoutes()) {
                is RepositoryResult.Success -> {
                    withContext(Dispatchers.IO) {
                        routeDao.upsertAdminRoutes(
                            AdminRoutesEntity(routesJson = gson.toJson(result.value))
                        )
                    }
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isRefreshing = false,
                            routes = result.value,
                            error = null,
                            notice = null
                        )
                    }
                }

                is RepositoryResult.Error -> loadCachedRoutes(result.message)
            }
        }
    }

    private suspend fun loadCachedRoutes(networkError: String) {
        val cached = withContext(Dispatchers.IO) {
            routeDao.getAdminRoutes()
        }

        if (cached == null) {
            _uiState.update {
                it.copy(
                    isLoading = false,
                    isRefreshing = false,
                    error = networkError
                )
            }
            return
        }

        val listType = object : TypeToken<List<RouteListDto>>() {}.type
        val cachedRoutes = runCatching {
            gson.fromJson<List<RouteListDto>>(cached.routesJson, listType)
        }.getOrDefault(emptyList())

        _uiState.update {
            it.copy(
                isLoading = false,
                isRefreshing = false,
                routes = cachedRoutes,
                notice = "Modo offline: mostrando la última lista guardada"
            )
        }
    }

    private fun monitorNetwork() {
        viewModelScope.launch {
            var wasOffline = false
            networkMonitor.isOnline.collect { isOnline ->
                _uiState.update { it.copy(isOffline = !isOnline) }
                if (isOnline && wasOffline) {
                    loadRoutes(showLoading = false)
                }
                wasOffline = !isOnline
            }
        }
    }

    private fun loadCamiAlerts() {
        viewModelScope.launch {
            runCatching { NetworkModule.adminApi.getCamiAlerts() }
                .getOrNull()
                ?.takeIf { it.isSuccessful }
                ?.body()
                ?.let { alerts ->
                    _uiState.update { it.copy(camiAlerts = alerts) }
                }
        }
    }

    private fun loadCommonProducts() {
        viewModelScope.launch {
            runCatching { NetworkModule.adminApi.getCommonProducts() }
                .getOrNull()
                ?.takeIf { it.isSuccessful }
                ?.body()
                ?.let { products ->
                    _uiState.update { it.copy(commonProducts = products) }
                }
        }
    }

    fun logout() {
        viewModelScope.launch {
            tokenManager.clearJwtToken()
            NetworkModule.jwtToken = null
        }
    }
}

package com.ml.shubham0204.facenet_android.presentation.screens.home

import androidx.lifecycle.ViewModel
import com.ml.shubham0204.facenet_android.domain.PersonUseCase
import com.ml.shubham0204.facenet_android.data.SettingsStore
import androidx.compose.runtime.mutableStateOf
import org.koin.android.annotation.KoinViewModel

@KoinViewModel
class HomeScreenViewModel(
    private val personUseCase: PersonUseCase,
    private val settingsStore: SettingsStore,
) : ViewModel() {
    val personFlow = personUseCase.getAll()

    val startupRouteState = mutableStateOf(
        settingsStore.get(StartupDestination.PREF_STARTUP_ROUTE) ?: StartupDestination.HOME,
    )

    fun setStartupRoute(route: String) {
        startupRouteState.value = route
        settingsStore.save(StartupDestination.PREF_STARTUP_ROUTE, route)
    }
}
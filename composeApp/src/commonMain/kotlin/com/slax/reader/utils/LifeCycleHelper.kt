package com.slax.reader.utils

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class AppLifecycleState {
    ON_STOP,
    ON_PAUSE,
    ON_RESUME,
    ON_CREATE,
    ON_DESTROY
}

object LifeCycleHelper : DefaultLifecycleObserver {

    private val _lifecycleState = MutableStateFlow(AppLifecycleState.ON_CREATE)
    val lifecycleState: StateFlow<AppLifecycleState> = _lifecycleState.asStateFlow()

    override fun onStart(owner: LifecycleOwner) {
        updateState(AppLifecycleState.ON_CREATE)
    }

    override fun onStop(owner: LifecycleOwner) {
        updateState(AppLifecycleState.ON_STOP)
    }

    override fun onPause(owner: LifecycleOwner) {
        updateState(AppLifecycleState.ON_PAUSE)
    }

    override fun onDestroy(owner: LifecycleOwner) {
        updateState(AppLifecycleState.ON_DESTROY)
    }

    override fun onResume(owner: LifecycleOwner) {
        updateState(AppLifecycleState.ON_RESUME)
    }

    override fun onCreate(owner: LifecycleOwner) {
        updateState(AppLifecycleState.ON_CREATE)
    }

    private fun updateState(newState: AppLifecycleState) {
        if (_lifecycleState.value != newState) {
            _lifecycleState.value = newState
        }
    }
}
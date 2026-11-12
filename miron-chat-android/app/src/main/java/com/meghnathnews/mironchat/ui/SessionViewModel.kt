package com.meghnathnews.mironchat.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.meghnathnews.mironchat.MironApp
import com.meghnathnews.mironchat.alerts.MessageAlerts
import com.meghnathnews.mironchat.data.ApiError
import com.meghnathnews.mironchat.data.User
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed interface SessionState {
    data object Loading : SessionState
    data object LoggedOut : SessionState
    data object Offline : SessionState
    data class LoggedIn(val user: User) : SessionState
}

class SessionViewModel(application: Application) : AndroidViewModel(application) {
    private val auth = (application as MironApp).auth
    private val current = MutableStateFlow<SessionState>(SessionState.Loading)
    val state: StateFlow<SessionState> = current

    init {
        restore()
    }

    fun restore() {
        if (!auth.hasSession) {
            MessageAlerts.stop(getApplication())
            current.value = SessionState.LoggedOut
            return
        }
        current.value = SessionState.Loading
        viewModelScope.launch {
            current.value = try {
                SessionState.LoggedIn(auth.currentUser())
            } catch (error: ApiError) {
                if (auth.hasSession) SessionState.Offline else SessionState.LoggedOut
            }
        }
    }

    fun loggedIn(user: User) {
        current.value = SessionState.LoggedIn(user)
    }

    fun accountGone() {
        MessageAlerts.stop(getApplication())
        auth.forget()
        current.value = SessionState.LoggedOut
    }

    fun logout() {
        MessageAlerts.stop(getApplication())
        current.value = SessionState.Loading
        viewModelScope.launch {
            auth.logout()
            current.value = SessionState.LoggedOut
        }
    }
}

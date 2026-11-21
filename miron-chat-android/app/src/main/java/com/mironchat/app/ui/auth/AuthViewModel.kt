package com.mironchat.app.ui.auth

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.mironchat.app.MironApp
import com.mironchat.app.data.ApiError
import com.mironchat.app.data.User
import kotlinx.coroutines.launch

enum class AuthMode { Login, Register }

private const val MIN_PASSWORD = 6

class AuthViewModel(application: Application) : AndroidViewModel(application) {
    private val auth = (application as MironApp).auth

    var mode by mutableStateOf(AuthMode.Login)
        private set
    var displayName by mutableStateOf("")
    var username by mutableStateOf("")
    var password by mutableStateOf("")
    var confirmPassword by mutableStateOf("")
    var error by mutableStateOf<String?>(null)
        private set
    var notice by mutableStateOf<String?>(null)
        private set
    var submitting by mutableStateOf(false)
        private set

    fun switchMode(next: AuthMode) {
        if (next == mode) return
        mode = next
        error = null
        notice = null
    }

    fun submit(onLogin: (User) -> Unit) {
        if (submitting) return
        val problem = validate()
        if (problem != null) {
            notice = null
            error = problem
            return
        }
        error = null
        notice = null
        submitting = true
        viewModelScope.launch {
            try {
                if (mode == AuthMode.Register) {
                    auth.register(displayName.trim(), username.trim(), password)
                    mode = AuthMode.Login
                    displayName = ""
                    password = ""
                    confirmPassword = ""
                    notice = "Account created. Please log in."
                } else {
                    onLogin(auth.login(username.trim(), password))
                }
            } catch (failure: ApiError) {
                error = failure.message
            } finally {
                submitting = false
            }
        }
    }

    private fun validate(): String? {
        if (username.isBlank() || password.isEmpty()) return "Please fill all fields"
        if (mode == AuthMode.Login) return null
        if (displayName.isBlank()) return "Please fill all fields"
        if (password.length < MIN_PASSWORD) return "Password must be at least 6 characters"
        if (password != confirmPassword) return "Passwords do not match"
        return null
    }
}

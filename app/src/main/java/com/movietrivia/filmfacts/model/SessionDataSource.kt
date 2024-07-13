package com.movietrivia.filmfacts.model

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

class SessionDataSource @Inject constructor(
    context: Context
) {

    private val encryptedSharedPreferences = EncryptedSharedPreferencesFactory.createSharedPrefs(context, ENCRYPTED_SHARED_PREFS_NAME)

    private val _sessionId = MutableStateFlow(encryptedSharedPreferences.getString(SESSION_ID_KEY, null))
    val sessionId: StateFlow<String?> = _sessionId

    fun setSessionId(sessionId: String) {
        _sessionId.value = sessionId
        with(encryptedSharedPreferences.edit()) {
            putString(SESSION_ID_KEY, sessionId)
            apply()
        }
    }

    fun clearSessionId() {
        _sessionId.value = null
        with(encryptedSharedPreferences.edit()) {
            clear()
            apply()
        }
    }

    private companion object {
        const val ENCRYPTED_SHARED_PREFS_NAME = "INFO"
        const val SESSION_ID_KEY = "KEY"
    }
}

internal object EncryptedSharedPreferencesFactory {
    fun createSharedPrefs(context: Context, name: String) =
        EncryptedSharedPreferences.create(
            name,
            MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC),
            context,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
}
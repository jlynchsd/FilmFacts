package com.movietrivia.filmfacts.model

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject


class UserSettingsDataSource @Inject constructor(
    private val context: Context,
    key: String
) {
    private val preferencesKey = stringPreferencesKey(key)

    val userSettings: Flow<UserSettings> = context.dataStore.data.map {
        UserSettings.deserialize(it[preferencesKey] ?: UserSettings().serialize())
    }

    suspend fun updateUserSettings(settings: UserSettings) {
        context.dataStore.edit {
            it[preferencesKey] = settings.serialize()
        }
    }

    suspend fun reset() {
        context.dataStore.edit {
            it.clear()
        }
    }

    private companion object {
        val Context.dataStore by preferencesDataStore(name = "user_settings")
    }
}
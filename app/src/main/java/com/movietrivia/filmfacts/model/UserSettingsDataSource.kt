package com.movietrivia.filmfacts.model

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject


class UserSettingsDataSource @Inject constructor(
    private val context: Context
) {
    val userSettings: Flow<UserSettings> = context.dataStore.data.map {
        UserSettings.deserialize(it[SETTINGS_KEY] ?: UserSettings().serialize())
    }

    suspend fun updateUserSettings(settings: UserSettings) {
        context.dataStore.edit {
            it[SETTINGS_KEY] = settings.serialize()
        }
    }

    suspend fun reset() {
        context.dataStore.edit {
            it.clear()
        }
    }

    private companion object {
        val SETTINGS_KEY = stringPreferencesKey("SETTINGS_KEY")

        val Context.dataStore by preferencesDataStore(name = "user_settings")
    }
}
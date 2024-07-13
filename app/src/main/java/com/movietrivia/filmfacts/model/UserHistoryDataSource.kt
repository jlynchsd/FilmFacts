package com.movietrivia.filmfacts.model

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class UserHistoryDataSource @Inject constructor(
    private val context: Context,
    private val calendarProvider: CalendarProvider
) {
    val userHistory: Flow<UserHistory> = context.dataStore.data.map {
        UserHistory.deserialize(it[USER_HISTORY_KEY] ?: UserHistory(calendarProvider.instance().timeInMillis).serialize())
    }

    suspend fun updateUserHistory(userHistory: UserHistory) {
        context.dataStore.edit {
            it[USER_HISTORY_KEY] = userHistory.serialize()
        }
    }

    suspend fun reset() {
        context.dataStore.edit {
            it.clear()
        }
    }

    private companion object {
        val USER_HISTORY_KEY = stringPreferencesKey("USER_HISTORY_KEY")

        val Context.dataStore by preferencesDataStore(name = "userHistory")
    }
}
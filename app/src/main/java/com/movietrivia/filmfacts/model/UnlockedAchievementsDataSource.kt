package com.movietrivia.filmfacts.model

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class UnlockedAchievementsDataSource @Inject constructor(
    private val context: Context
) {
    val unlockedAchievements: Flow<UnlockedAchievements> = context.dataStore.data.map {
        UnlockedAchievements.deserialize(it[UNLOCKED_ACHIEVEMENTS_KEY] ?: UnlockedAchievements().serialize())
    }

    suspend fun updateUnlockedAchievements(unlockedAchievements: UnlockedAchievements) {
        context.dataStore.edit {
            it[UNLOCKED_ACHIEVEMENTS_KEY] = unlockedAchievements.serialize()
        }
    }

    suspend fun reset() {
        context.dataStore.edit {
            it.clear()
        }
    }

    private companion object {
        val UNLOCKED_ACHIEVEMENTS_KEY = stringPreferencesKey("UNLOCKED_ACHIEVEMENTS_KEY")

        val Context.dataStore by preferencesDataStore(name = "unlockedAchievements")
    }
}
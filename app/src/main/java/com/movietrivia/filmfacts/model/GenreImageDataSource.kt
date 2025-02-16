package com.movietrivia.filmfacts.model

import android.content.Context
import androidx.annotation.VisibleForTesting
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import javax.inject.Inject

class GenreImageDataSource @Inject constructor(
    private val context: Context,
    keyName:  String,
    private val default: List<UiGenre>
) {
    private val key = stringPreferencesKey(keyName)

    val supportedGenres = default.map { it.genreId }

    suspend fun getGenreImages() = context.dataStore.data.firstOrNull()?.let { preferences ->
        preferences[key]?.let { serialized ->
            GenreImageSerializer.deserialize(serialized).genreImages
        }
    } ?: default

    suspend fun saveGenreImages(images: List<UiGenre>) {
        context.dataStore.edit {
            it[key] = GenreImageSerializer(images).serialize()
        }
    }

    suspend fun clearGenreImages() {
        context.dataStore.edit {
            it.clear()
        }
    }

    @VisibleForTesting
    internal companion object {
        private val Context.dataStore by preferencesDataStore(name = "genre_images")
    }
}

@Serializable
private data class GenreImageSerializer(
    val genreImages: List<UiGenre>
) {
    fun serialize() = kotlinx.serialization.json.Json.encodeToString(this)

    companion object {
        fun deserialize(serialized: String) = kotlinx.serialization.json.Json.decodeFromString<GenreImageSerializer>(serialized)
    }
}
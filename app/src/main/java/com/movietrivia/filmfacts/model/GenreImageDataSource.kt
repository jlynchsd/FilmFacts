package com.movietrivia.filmfacts.model

import android.content.Context
import androidx.annotation.VisibleForTesting
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.movietrivia.filmfacts.api.MovieGenre
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import javax.inject.Inject

class GenreImageDataSource @Inject constructor(
    private val context: Context
) {

    suspend fun getGenreImages() = context.dataStore.data.firstOrNull()?.let { preferences ->
        preferences[genreImagesKey]?.let { serialized ->
            GenreImageSerializer.deserialize(serialized).genreImages
        }
    } ?: defaults

    suspend fun saveGenreImages(images: List<UiGenre>) {
        context.dataStore.edit {
            it[genreImagesKey] = GenreImageSerializer(images).serialize()
        }
    }

    suspend fun clearGenreImages() {
        context.dataStore.edit {
            it.clear()
        }
    }

    @VisibleForTesting
    internal companion object {
        private val genreImagesKey = stringPreferencesKey("GENRE_IMAGES_KEY")
        private val Context.dataStore by preferencesDataStore(name = "genre_images")

        val defaults = listOf(
            UiGenre(
                "https://www.themoviedb.org/t/p/original/crLfhuTYadl39DucKK8HcEJctIf.jpg",
                -1
            ),
            UiGenre(
                "https://www.themoviedb.org/t/p/original/ncEsesgOJDNrTUED89hYbA117wo.jpg",
                MovieGenre.ACTION.key
            ),
            UiGenre(
                "https://www.themoviedb.org/t/p/original/wXsQvli6tWqja51pYxXNG1LFIGV.jpg",
                MovieGenre.ANIMATION.key
            ),
            UiGenre(
                "https://www.themoviedb.org/t/p/original/r0p9IeVzVP4OULWWw0UsDSCWtKb.jpg",
                MovieGenre.FAMILY.key
            ),
            UiGenre(
                "https://www.themoviedb.org/t/p/original/9xOmYwIKLX8pTlDaLKdrvkao8Ju.jpg",
                MovieGenre.FANTASY.key
            ),
            UiGenre(
                "https://www.themoviedb.org/t/p/original/3N9UcnCJRyLE2B4ZTbxIpWqL4aQ.jpg",
                MovieGenre.HORROR.key
            ),
            UiGenre(
                "https://www.themoviedb.org/t/p/original/qJeU7KM4nT2C1WpOrwPcSDGFUWE.jpg",
                MovieGenre.ROMANCE.key
            ),
            UiGenre(
                "https://www.themoviedb.org/t/p/original/rAiYTfKGqDCRIIqo664sY9XZIvQ.jpg",
                MovieGenre.SCI_FI.key
            ),
            UiGenre(
                "https://www.themoviedb.org/t/p/original/x4biAVdPVCghBlsVIzB6NmbghIz.jpg",
                MovieGenre.WESTERN.key
            ),
        )
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
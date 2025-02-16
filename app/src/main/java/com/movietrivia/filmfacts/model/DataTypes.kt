package com.movietrivia.filmfacts.model

import android.net.Uri
import androidx.browser.customtabs.CustomTabsSession
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.transformWhile
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString

data class Actor(
    val id: Int,
    val name: String,
    val gender: Int?
)

sealed class PendingData<T> {
    class None<T>: PendingData<T>()
    class Loading<T>: PendingData<T>()
    class Success<T>(val result: T): PendingData<T>()
    class Error<T>(val error: String): PendingData<T>()
}

fun <T> Flow<PendingData<T>>.firstResponse(): Flow<PendingData<T>> =
    transformWhile { value ->
        when (value) {
            is PendingData.Success<T>, is PendingData.Error<T> -> { emit(value) }
            else -> {}
        }

        (value is PendingData.None<T> || value is PendingData.Loading<T>)
    }

sealed class RemoteData<T> {
    class Success<T>(val result: T): RemoteData<T>()
    class Error<T>(val errorType: RemoteDataError): RemoteData<T>()
}

enum class RemoteDataError {
    UNAVAILABLE,
    DENIED,
    OTHER
}

data class AccountDetails(
    val id: Int,
    val name: String,
    val userName: String,
    val favoriteMoviesMetaData: PagedMetaData,
    val ratedMoviesMetaData: PagedMetaData,
    val watchlistMoviesMetaData: PagedMetaData,
    val favoriteTvShowsMetaData: PagedMetaData,
    val ratedTvShowsMetaData: PagedMetaData,
    val watchlistTvShowsMetaData: PagedMetaData,
    val avatarPath: String
)

data class PagedMetaData(
    val totalPages: Int,
    val totalEntries: Int
)

data class CustomTabMetaData(
    val session: CustomTabsSession?,
    val uri: Uri,
    val expirationTime: Long
)

data class UiImageEntry(
    val imagePath: String,
    val isAnswer: Boolean,
    val title: String? = null,
    val data: String? = null,
) : java.io.Serializable

data class UiTextEntry(
    val isAnswer: Boolean,
    val topContent: String,
    val subContent: String? = null
) : java.io.Serializable

@Serializable
data class UiGenre(
    val imagePath: String,
    val genreId: Int
)

sealed interface UiPrompt

data class UiImagePrompt(
    val entries: List<UiImageEntry>,
    val titleId: Int,
    val titleData: List<String> = emptyList()
): UiPrompt

data class UiTextPrompt(
    val entries: List<UiTextEntry>,
    val images: List<UiImageEntry>,
    val wideImage: Boolean,
    val titleId: Int,
    val titleData: List<String> = emptyList()
): UiPrompt

sealed class PromptState {
    object None: PromptState()

    object Finished: PromptState()

    object Error: PromptState()

    class Ready(val prompt: UiPrompt): PromptState()
}

@Serializable
data class UserSettings(
    val language: String = "en",
    val excludedGenres: List<Int> = emptyList(),
    val releasedAfterOffset: Int? = null,
    val releasedBeforeOffset: Int? = null
) {
    fun serialize() = kotlinx.serialization.json.Json.encodeToString(this)

    companion object {
        fun deserialize(serialized: String) = kotlinx.serialization.json.Json.decodeFromString<UserSettings>(serialized)
    }
}

data class TriviaQuestionResult(
    val correct: Boolean,
    val duration: Long
)

enum class Achievement {
    PERFECT_SCORE,
    SIGN_IN,
    COMPLETE_QUIZZES,
    FINISH_ACHIEVEMENTS,
    COMPLETE_FIRST_QUIZ,
    DELAYED_QUIZ,
    FAST_RESPONSES
}

@Serializable
data class UnlockedAchievements(
    val achievements: Set<Achievement> = emptySet(),
    val newAchievements: Boolean = false
) {
    fun serialize() = kotlinx.serialization.json.Json.encodeToString(this)

    companion object {
        fun deserialize(serialized: String) = kotlinx.serialization.json.Json.decodeFromString<UnlockedAchievements>(serialized)
    }
}

@Serializable
data class UserHistory(
    val startDate: Long,
    val completedQuizzes: Int = 0,
    val correctAnswers: Int = 0,
    val incorrectAnswers: Int = 0,
    val totalQuizDuration: Long = 0,
    val fastResponseCount: Int = 0
) {
    fun serialize() = kotlinx.serialization.json.Json.encodeToString(this)

    companion object {
        fun deserialize(serialized: String) = kotlinx.serialization.json.Json.decodeFromString<UserHistory>(serialized)
    }
}
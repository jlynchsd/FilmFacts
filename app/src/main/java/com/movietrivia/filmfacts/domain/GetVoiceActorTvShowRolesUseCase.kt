package com.movietrivia.filmfacts.domain

import android.content.Context
import com.movietrivia.filmfacts.R
import com.movietrivia.filmfacts.api.ActorTvShowCredits
import com.movietrivia.filmfacts.api.Logger
import com.movietrivia.filmfacts.api.TvGenre
import com.movietrivia.filmfacts.model.*
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class GetVoiceActorTvShowRolesUseCase(
    private val applicationContext: Context,
    private val filmFactsRepository: FilmFactsRepository,
    private val recentPromptsRepository: RecentPromptsRepository,
    private val userDataRepository: UserDataRepository,
    private val calendarProvider: CalendarProvider,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO
) : UseCase {

    override suspend fun invoke(includeGenres: List<Int>?) =
        withContext(dispatcher) {
            getPrompt(includeGenres)
        }

    private suspend fun getPrompt(includeGenres: List<Int>?): UiPrompt? {
        val userSettings = userDataRepository.tvShowUserSettings.firstOrNullCatching() ?: return null
        if (userSettings.excludedGenres.contains(TvGenre.ANIMATION.key)) {
            Logger.info(LOG_TAG, "Skipping voice actor use case since animation is excluded")
            return null
        }
        val genres = if (includeGenres != null) {
            (listOf(TvGenre.ANIMATION.key) + includeGenres).distinct()
        } else {
            listOf(TvGenre.ANIMATION.key)
        }
        val popularActors = getTvShowActors(
            filmFactsRepository,
            recentPromptsRepository,
            getDateRange(userSettings, calendarProvider, LOG_TAG, DateStrategies.MAX),
            genres,
            logTag = LOG_TAG
        ).toMutableList()

        Logger.debug(LOG_TAG, "Popular Actors: ${popularActors.size}")

        val targetCreditsCount = 4
        val strategy = VoiceActorStrategy.values().random()
        val minCredits = when (strategy) {
            VoiceActorStrategy.WAS_VOICED -> 1
            VoiceActorStrategy.WAS_NOT_VOICED -> 3
        }

        val creditFilter = { actorCredit: ActorTvShowCredits ->
            !userSettings.excludedGenres.containsAny(actorCredit.genreIds) &&
                    actorCredit.genreIds.containsAll(genres) &&
                    userSettings.language == actorCredit.originalLanguage &&
                    dateWithinRange(actorCredit.firstAirDate, userSettings.releasedAfterOffset, userSettings.releasedBeforeOffset) &&
                    actorCredit.characterName.isNotBlank() &&
                    actorCredit.showName.isNotBlank() &&
                    !actorCredit.characterName.lowercase().contains("self") && // only actual roles
                    !actorCredit.characterName.lowercase().contains("additional") && // only actual roles
                    !actorCredit.characterName.lowercase().contains("#") && // numbered characters are usually bad
                    !actorCredit.characterName.lowercase().contains("/") && // multiple characters arent great
                    !actorCredit.characterName.lowercase().contains(",") && // multiple characters arent great
                    actorCredit.characterName.filter { it == ' ' }.length <= 2 && // lots of words are usually unnamed characters
                    actorCredit.voteCount > 20 && // avoid obscure roles
                    actorCredit.episodeCount >= 10 // recurring enough character to actually matter
        }

        if (popularActors.size >= 2) {
            val actorCredits = getActorTvShowCredits(
                filmFactsRepository,
                recentPromptsRepository,
                popularActors,
                (minCredits until targetCreditsCount),
                creditFilter,
                logTag = LOG_TAG
            )

            Logger.debug(LOG_TAG, "Actor Credits: ${actorCredits?.third?.size}")

            if (actorCredits != null) {
                val requiredCredits = targetCreditsCount - actorCredits.third.size
                val otherPopularActors = getTvShowActors(
                    filmFactsRepository,
                    recentPromptsRepository,
                    getDateRange(userSettings, calendarProvider, LOG_TAG, DateStrategies.MAX),
                    includeGenres,
                    filter = { it.gender == actorCredits.first.gender },
                    logTag = LOG_TAG
                ).toMutableList()

                val otherActorCredits = getActorTvShowCredits(
                    filmFactsRepository,
                    recentPromptsRepository,
                    otherPopularActors,
                    (requiredCredits..requiredCredits),
                    creditFilter,
                    actorCredits.first,
                    logTag = LOG_TAG
                )

                Logger.debug(LOG_TAG, "Other Actor Credits: ${otherActorCredits?.third?.size}")

                if (otherActorCredits != null) {
                    recentPromptsRepository.addRecentActor(actorCredits.first.id)

                    val textEntries = (actorCredits.third.map {
                        UiTextEntry(
                            strategy == VoiceActorStrategy.WAS_VOICED,
                            it.characterName,
                            it.showName
                        )
                    } + otherActorCredits.third.map {
                        UiTextEntry(
                            strategy == VoiceActorStrategy.WAS_NOT_VOICED,
                            it.characterName,
                            it.showName
                        )
                    }).shuffled()

                    val imageEntry = UiImageEntry(
                        filmFactsRepository.getImageUrl(actorCredits.second, ImageType.PROFILE) ?: "",
                        false
                    )

                    val success = preloadImages(applicationContext, imageEntry.imagePath)

                    Logger.debug(LOG_TAG, "Preloaded Images: $success")

                    val promptTitle = when (strategy) {
                        VoiceActorStrategy.WAS_VOICED -> R.string.actor_voice_roles_title
                        VoiceActorStrategy.WAS_NOT_VOICED -> R.string.actor_voice_roles_reversed_title
                    }

                    if (success) {
                        return UiTextPrompt(
                            textEntries,
                            listOf(imageEntry),
                            false,
                            promptTitle,
                            listOf(actorCredits.first.name)
                        )
                    }
                }
            }
        }

        Logger.info(LOG_TAG, "Unable to generate prompt")
        return null
    }

    private enum class VoiceActorStrategy {
        WAS_VOICED,
        WAS_NOT_VOICED
    }

    private companion object {
        const val LOG_TAG = "GetVoiceActorTvShowRolesUseCase"
    }
}


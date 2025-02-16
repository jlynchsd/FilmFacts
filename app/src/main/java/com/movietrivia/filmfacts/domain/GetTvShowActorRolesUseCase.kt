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

class GetTvShowActorRolesUseCase(
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
        val popularActors = getTvShowActors(
            filmFactsRepository,
            recentPromptsRepository,
            getDateRange(userSettings, calendarProvider, LOG_TAG, DateStrategies.MAX),
            includeGenres,
            logTag = LOG_TAG
        ).toMutableList()

        Logger.debug(LOG_TAG, "Popular Actors: ${popularActors.size}")

        val targetCreditsCount = 4
        val creditFilter = { actorCredit: ActorTvShowCredits ->
            !userSettings.excludedGenres.containsAny(actorCredit.genreIds) &&
                    hasGenreId(actorCredit.genreIds, includeGenres) &&
                    userSettings.language == actorCredit.originalLanguage &&
                    dateWithinRange(actorCredit.firstAirDate, userSettings.releasedAfterOffset, userSettings.releasedBeforeOffset) &&
                    actorCredit.characterName.isNotBlank() &&
                    actorCredit.showName.isNotBlank() &&
                    (includeGenres?.contains(TvGenre.REALITY.key) == true || !actorCredit.characterName.lowercase().contains("self")) && // only actual roles
                    !actorCredit.characterName.lowercase().contains("#") && // numbered characters are usually bad
                    !actorCredit.characterName.lowercase().contains("/") && // multiple characters arent great
                    actorCredit.voteCount > 20 && // avoid obscure roles
                    actorCredit.episodeCount >= 10 // recurring enough character to actually matter
        }

        if (popularActors.size >= 2) {
            val actorCredits = getActorTvShowCredits(
                filmFactsRepository,
                recentPromptsRepository,
                popularActors,
                (1 until targetCreditsCount),
                creditFilter,
                logTag = LOG_TAG
            )

            Logger.debug(LOG_TAG, "Actor Use case Credits: ${actorCredits?.third?.size}")

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

                Logger.debug(LOG_TAG, "Other Actor Use case Credits: ${otherActorCredits?.third?.size}")

                if (otherActorCredits != null) {
                    recentPromptsRepository.addRecentActor(actorCredits.first.id)

                    val textEntries = (actorCredits.third.map {
                        UiTextEntry(
                            true,
                            it.characterName,
                            it.showName
                        )
                    } + otherActorCredits.third.map {
                        UiTextEntry(
                            false,
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

                    if (success) {
                        return UiTextPrompt(
                            textEntries,
                            listOf(imageEntry),
                            false,
                            R.string.actor_roles_title,
                            listOf(actorCredits.first.name)
                        )
                    }
                }
            }
        }

        Logger.info(LOG_TAG, "Unable to generate prompt")
        return null
    }

    private fun hasGenreId(actorGenreIds: List<Int>, requiredGenreIds: List<Int>?) =
        requiredGenreIds?.let {
            actorGenreIds.containsAll(it)
        } ?: true

    private companion object {
        const val LOG_TAG = "GetTvShowActorRolesUseCase"
    }
}
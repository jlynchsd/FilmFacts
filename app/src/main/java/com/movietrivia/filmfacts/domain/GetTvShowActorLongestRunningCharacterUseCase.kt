package com.movietrivia.filmfacts.domain

import android.content.Context
import com.movietrivia.filmfacts.R
import com.movietrivia.filmfacts.api.ActorTvShowCredits
import com.movietrivia.filmfacts.api.Logger
import com.movietrivia.filmfacts.api.PersonDetails
import com.movietrivia.filmfacts.api.TvGenre
import com.movietrivia.filmfacts.model.CalendarProvider
import com.movietrivia.filmfacts.model.FilmFactsRepository
import com.movietrivia.filmfacts.model.ImageType
import com.movietrivia.filmfacts.model.RecentPromptsRepository
import com.movietrivia.filmfacts.model.UiImageEntry
import com.movietrivia.filmfacts.model.UiImagePrompt
import com.movietrivia.filmfacts.model.UiPrompt
import com.movietrivia.filmfacts.model.UserDataRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class GetTvShowActorLongestRunningCharacterUseCase(
    private val applicationContext: Context,
    private val filmFactsRepository: FilmFactsRepository,
    private val recentPromptsRepository: RecentPromptsRepository,
    private val userDataRepository: UserDataRepository,
    private val calendarProvider: CalendarProvider,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO
): UseCase {

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
                    actorCredit.characterName.lowercase().replace("(voice)", "").isNotBlank() && // some credits are just "(voice)"
                    (includeGenres?.contains(TvGenre.REALITY.key) == true || actorCredit.voteCount > 20) && // avoid obscure roles
                    (includeGenres?.contains(TvGenre.REALITY.key) == true || actorCredit.episodeCount >= 10) // recurring enough character to actually matter
        }

        if (popularActors.size >= 4) {
            val actorCredits = mutableListOf<Pair<PersonDetails, ActorTvShowCredits>>()
            while (actorCredits.size < 4 && popularActors.isNotEmpty()) {
                val currentActor = popularActors.random()
                popularActors.remove(currentActor)
                val currentCredit = filmFactsRepository.getActorTvShowCredits(currentActor.id)?.filter(creditFilter)?.maxByOrNull { it.episodeCount }
                Logger.debug(LOG_TAG, "Actor Filtered Tv Show Credit: $currentCredit")
                if (currentCredit != null &&
                    actorCredits.none {
                        actorCredit -> actorCredit.second.episodeCount == currentCredit.episodeCount
                    }) {
                    val actorDetails = filmFactsRepository.getActorDetails(currentActor.id)
                    if (actorDetails != null && actorDetails.profilePath.isNotEmpty()) {
                        actorCredits.add(Pair(actorDetails, currentCredit))
                    }

                }
            }

            Logger.debug(LOG_TAG, "Selected Actor Credits: ${actorCredits.size}")
            if (actorCredits.size == 4) {
                actorCredits.forEach {
                    recentPromptsRepository.addRecentActor(it.first.id)
                }
                val entries = actorCredits.sortedByDescending { it.second.episodeCount }.mapIndexed { index, entry ->
                    UiImageEntry(
                        filmFactsRepository.getImageUrl(entry.first.profilePath, ImageType.PROFILE) ?: "",
                        index == 0,
                        entry.second.characterName,
                        "${entry.second.episodeCount} ${applicationContext.resources.getQuantityString(R.plurals.episode_counter, entry.second.episodeCount)}"
                    )
                }.shuffled()

                val success = preloadImages(applicationContext, *entries.map { it.imagePath }.toTypedArray())
                Logger.debug(LOG_TAG, "Preloaded Images: $success")

                if (success) {
                    return UiImagePrompt(
                        entries,
                        R.string.tv_show_actor_longest_running_character
                    )
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
        const val LOG_TAG = "GetTvShowActorLongestRunningCharacterUseCase"
    }
}
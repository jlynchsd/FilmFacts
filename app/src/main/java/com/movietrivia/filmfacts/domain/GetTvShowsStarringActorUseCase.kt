package com.movietrivia.filmfacts.domain

import android.content.Context
import com.movietrivia.filmfacts.R
import com.movietrivia.filmfacts.api.ActorTvShowCredits
import com.movietrivia.filmfacts.api.Logger
import com.movietrivia.filmfacts.model.*
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.lang.Integer.min

class GetTvShowsStarringActorUseCase(
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

        Logger.debug(LOG_TAG, "Selected Popular Actors: ${popularActors.size}")

        if (popularActors.isNotEmpty()) {
            val prompt = getActorTvShows(popularActors, userSettings, 1, includeGenres)
            Logger.debug(LOG_TAG, "Correct Answer Shows: ${prompt?.second?.size}")

            if (prompt != null) {
                val promptTvShowCount = (1..min(3, prompt.second.size)).random()
                val fillerTvShowCount = 4 - promptTvShowCount
                val filler = getFillerTvShows(popularActors, userSettings, fillerTvShowCount, prompt.first.id, includeGenres)
                Logger.debug(LOG_TAG, "Filler Answer Shows: ${filler?.size}")

                if (filler != null) {
                    val promptStartIndex = (0..prompt.second.size - promptTvShowCount).random()
                    val fillerStartIndex = (0 .. filler.size - fillerTvShowCount).random()
                    val promptTvShows = prompt.second.subList(promptStartIndex, promptStartIndex + promptTvShowCount).map {
                        UiImageEntry(
                            filmFactsRepository.getImageUrl(it.posterPath, ImageType.POSTER) ?: "",
                            true
                        )
                    }
                    val fillerTvShows = filler.subList(fillerStartIndex, fillerStartIndex + fillerTvShowCount).map {
                        UiImageEntry(
                            filmFactsRepository.getImageUrl(it.posterPath, ImageType.POSTER) ?: "",
                            false
                        )
                    }

                    val uiTvShows = (promptTvShows + fillerTvShows).shuffled()
                    val success = preloadImages(applicationContext, *uiTvShows.map { it.imagePath }.toTypedArray())

                    Logger.debug(LOG_TAG, "Preloaded Images: $success")

                    if (success) {
                        return UiImagePrompt(
                            uiTvShows,
                            R.string.tv_show_actor_starred_title,
                            listOf(prompt.first.name)
                        )
                    }

                }
            }
        }

        Logger.info(LOG_TAG, "Unable to generate prompt")
        return null
    }

    private suspend fun getActorTvShows(
        popularActors: MutableList<Actor>,
        userSettings: UserSettings,
        minTvShows: Int,
        includeGenres: List<Int>?
    ): Pair<Actor, List<ActorTvShowCredits>>? {
        val actors = prepActors(popularActors, null, userSettings, includeGenres) ?: return null

        var tvShows: List<ActorTvShowCredits>? = null
        var currentActor = actors.randomOrNull()
        var remainingActorAttempts = 5
        while (currentActor != null && actors.isNotEmpty() && remainingActorAttempts > 0) {
            tvShows = filmFactsRepository.getActorTvShowCredits(currentActor.id)?.distinctBy { it.showName }?.filter { it.posterPath.isNotEmpty() }
            if (includeGenres != null) {
                tvShows = tvShows?.filter { it.genreIds.containsAll(includeGenres) }
            }
            Logger.debug(LOG_TAG, "Filtered Tv Shows for Actor ${currentActor.id}: ${tvShows?.size}")

            if (tvShows != null && tvShows.size >= minTvShows) {
                break
            } else {
                currentActor = getNextActor(currentActor, userSettings, null, actors, includeGenres)
                tvShows = null
                --remainingActorAttempts
            }
        }

        return tvShows?.let { tvShowsList ->
            currentActor?.let { currentActor ->
                Pair(currentActor, tvShowsList)
            }
        }
    }

    private suspend fun getFillerTvShows(
        popularActors: MutableList<Actor>,
        userSettings: UserSettings,
        minTvShows: Int,
        excludeActorId: Int,
        includeGenres: List<Int>?
    ): List<ActorTvShowCredits>? {
        val actors = prepActors(popularActors, excludeActorId, userSettings, includeGenres) ?: return null

        var tvShows: MutableList<ActorTvShowCredits> = mutableListOf()
        var currentActor = actors.randomOrNull()
        var remainingActorAttempts = 5
        while (tvShows.size < minTvShows && currentActor != null && actors.isNotEmpty() && remainingActorAttempts > 0) {
            var currentTvShows = filmFactsRepository.getActorTvShowCredits(currentActor.id)?.filter { it.posterPath.isNotEmpty() }
            if (includeGenres != null) {
                currentTvShows = currentTvShows?.filter { it.genreIds.containsAll(includeGenres) }
            }
            Logger.debug(LOG_TAG, "Filtered Tv Shows for Filler Actor ${currentActor.id}: ${currentTvShows?.size}")

            currentTvShows?.let { shows ->
                tvShows = (tvShows + shows).distinctBy { it.showName }.toMutableList()
            }

            currentActor = getNextActor(currentActor, userSettings, excludeActorId, actors, includeGenres)
            --remainingActorAttempts
        }

        return if (tvShows.size >= minTvShows) {
            tvShows
        } else  {
            null
        }
    }

    private suspend fun prepActors(
        popularActors: MutableList<Actor>,
        excludeActorId: Int?,
        userSettings: UserSettings,
        includeGenres: List<Int>?
    ): MutableList<Actor>? {
        popularActors.apply {
            // filter popular actors in place
            val filtered = popularActors.filter { it.id != excludeActorId }
            popularActors.clear()
            popularActors.addAll(filtered)

            Logger.debug(LOG_TAG, "Filtered Popular Actors: ${filtered.size}")

            // if no remaining actors, fetch new ones
            if (popularActors.isEmpty()) {
                Logger.debug(LOG_TAG, "Loading additional actors from remote")
                popularActors.addAll(
                    getTvShowActors(
                        filmFactsRepository,
                        recentPromptsRepository,
                        getDateRange(userSettings, calendarProvider, LOG_TAG),
                        includeGenres,
                        logTag = LOG_TAG
                    ).filter { it.id != excludeActorId }
                )

                // if no new actors, return null
                if (popularActors.isEmpty()) {
                    Logger.debug(LOG_TAG, "Unable to get Actor Tv Shows")
                    return null
                }
            }
        }

        return popularActors
    }

    private suspend fun getNextActor(
        currentActor: Actor,
        userSettings: UserSettings,
        excludeActorId: Int?,
        popularActors: MutableList<Actor>,
        includeGenres: List<Int>?
    ): Actor {
        popularActors.remove(currentActor)
        if (popularActors.isEmpty()) {
            Logger.debug(LOG_TAG, "Loading next actor from remote")
            popularActors.addAll(
                getTvShowActors(
                    filmFactsRepository,
                    recentPromptsRepository,
                    getDateRange(userSettings, calendarProvider, LOG_TAG),
                    includeGenres,
                    logTag = LOG_TAG
                ).filter { it.id != excludeActorId })
        }
        return if (popularActors.isNotEmpty()) {
            popularActors.random()
        } else {
            Logger.debug(LOG_TAG, "Failed to load next actor from remote")
            currentActor
        }
    }

    private companion object {
        const val LOG_TAG = "GetTvShowsStarringActorUseCase"
    }
}
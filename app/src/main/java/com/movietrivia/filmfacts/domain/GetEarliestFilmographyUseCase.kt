package com.movietrivia.filmfacts.domain

import android.annotation.SuppressLint
import android.content.Context
import android.text.format.DateFormat
import com.movietrivia.filmfacts.R
import com.movietrivia.filmfacts.api.DiscoverService
import com.movietrivia.filmfacts.api.Logger
import com.movietrivia.filmfacts.model.*
import kotlinx.coroutines.*

class GetEarliestFilmographyUseCase(
    private val applicationContext: Context,
    private val filmFactsRepository: FilmFactsRepository,
    private val recentPromptsRepository: RecentPromptsRepository,
    private val calendarProvider: CalendarProvider,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO
) : UseCase {

    override suspend fun invoke(includeGenres: List<Int>?) =
        withContext(dispatcher) {
            getPrompt(includeGenres)
        }

    @SuppressLint("SimpleDateFormat")
    private suspend fun getPrompt(includeGenres: List<Int>?): UiPrompt? {
        val popularActors = getMovieActors(
            filmFactsRepository,
            recentPromptsRepository,
            includeGenres
        ).toMutableList()

        Logger.debug(LOG_TAG, "Popular Actors: ${popularActors.size}")

        val targetActorCount = 4

        if (popularActors.size >= targetActorCount) {
            val actorResults = getActorDetails(
                filmFactsRepository,
                popularActors,
                targetActorCount,
            ) { it.profilePath.isNotEmpty() }

            Logger.debug(LOG_TAG, "Actor Results: ${actorResults.size}")

            if (actorResults.size >= targetActorCount) {
                val actorMovieResults = coroutineScope {
                    actorResults.map {
                        async {
                            filmFactsRepository.getMovies(
                                forceSettings = UserSettings(language = ""),
                                minimumVotes = null,
                                cast = listOf(it.id),
                                movieOrder = DiscoverService.Builder.MovieOrder.RELEASE_DATE_ASC
                            )
                        }
                    }.awaitAll().filterNotNull().toMutableList()
                }

                Logger.debug(LOG_TAG, "Actor Movie Results: ${actorMovieResults.size}")

                if (actorMovieResults.size == actorResults.size) {
                    val actorInfo = actorResults.zip(
                        actorMovieResults.map {
                            dateToTimestamp(it.results.firstOrNull()?.releaseDate, calendarProvider)
                        }
                    ).mapNotNull {
                        it.second?.let { second ->
                            Pair(it.first, second)
                        }
                    }.sortedBy { it.second }.distinctBy { it.second }
                    Logger.debug(LOG_TAG, "Actor Info: ${actorInfo.size}")

                    if (actorInfo.size >= targetActorCount) {
                        actorInfo.forEach { recentPromptsRepository.addRecentActor(it.first.id) }

                        val entries = actorInfo.mapIndexed { index, entry ->
                            UiImageEntry(
                                filmFactsRepository.getImageUrl(entry.first.profilePath, ImageType.PROFILE) ?: "",
                                index == 0,
                                entry.first.name,
                                DateFormat.format("MMM dd, yyyy", entry.second).toString()
                            )
                        }.shuffled()

                        val success = preloadImages(applicationContext, *entries.map { it.imagePath }.toTypedArray())

                        Logger.debug(LOG_TAG, "Preloaded Images: $success")

                        if (success) {
                            return UiImagePrompt(
                                entries,
                                R.string.actor_earliest_filmography_title
                            )
                        }
                    }
                }
            }
        }

        Logger.info(LOG_TAG, "Unable to generate prompt")
        return null
    }

    private companion object {
        const val LOG_TAG = "GetEarliestFilmographyUseCase"
    }
}
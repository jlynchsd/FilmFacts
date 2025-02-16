package com.movietrivia.filmfacts.domain

import android.content.Context
import com.movietrivia.filmfacts.R
import com.movietrivia.filmfacts.api.Logger
import com.movietrivia.filmfacts.model.*
import kotlinx.coroutines.*

class GetBiggestFilmographyUseCase(
    private val applicationContext: Context,
    private val filmFactsRepository: FilmFactsRepository,
    private val recentPromptsRepository: RecentPromptsRepository,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO
) : UseCase {

    override suspend fun invoke(includeGenres: List<Int>?) =
        withContext(dispatcher) {
            getPrompt(includeGenres)
        }

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
                                cast = listOf(it.id)
                            )
                        }
                    }.awaitAll().filterNotNull().distinctBy { it.totalResultCount }.toMutableList()
                }

                Logger.debug(LOG_TAG, "Actor Movie Results: ${actorMovieResults.size}")

                if (actorMovieResults.size == actorResults.size) {
                    val actorInfo = actorResults.zip(actorMovieResults).sortedByDescending { it.second.totalResultCount }
                    actorInfo.forEach { recentPromptsRepository.addRecentActor(it.first.id) }

                    val entries = actorInfo.mapIndexed { index, entry ->
                        UiImageEntry(
                            filmFactsRepository.getImageUrl(entry.first.profilePath, ImageType.PROFILE) ?: "",
                            index == 0,
                            entry.first.name,
                            "${entry.second.totalResultCount} ${applicationContext.resources.getQuantityString(R.plurals.credit_counter, entry.second.totalResultCount)}"
                        )
                    }.shuffled()

                    val success = preloadImages(applicationContext, *entries.map { it.imagePath }.toTypedArray())

                    Logger.debug(LOG_TAG, "Preloaded Images: $success")

                    if (success) {
                        return UiImagePrompt(
                            entries,
                            R.string.movie_actor_longest_filmography_title
                        )
                    }
                }
            }
        }

        Logger.info(LOG_TAG, "Unable to generate prompt")
        return null
    }

    private companion object {
        const val LOG_TAG = "GetBiggestFilmographyUseCase"
    }
}
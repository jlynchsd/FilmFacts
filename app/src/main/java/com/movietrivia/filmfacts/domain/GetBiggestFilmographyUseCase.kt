package com.movietrivia.filmfacts.domain

import android.content.Context
import com.movietrivia.filmfacts.R
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
        val popularActors = getActors(
            filmFactsRepository,
            recentPromptsRepository,
            includeGenres
        ).toMutableList()

        val targetActorCount = 4

        if (popularActors.size >= targetActorCount) {
            val actorResults = getActorDetails(
                filmFactsRepository,
                popularActors,
                targetActorCount,
            ) { it.profilePath.isNotEmpty() }

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

                if (actorMovieResults.size == actorResults.size) {
                    val actorInfo = actorResults.zip(actorMovieResults).sortedByDescending { it.second.totalResultCount }
                    actorInfo.forEach { recentPromptsRepository.addRecentActor(it.first.id) }

                    val entries = actorInfo.mapIndexed { index, entry ->
                        val suffix = if (entry.second.totalResultCount == 1) {
                            R.string.suffix_filmography
                        } else {
                            R.string.suffix_filmography_plural
                        }
                        UiImageEntry(
                            filmFactsRepository.getImageUrl(entry.first.profilePath, ImageType.PROFILE) ?: "",
                            index == 0,
                            entry.first.name,
                            "${entry.second.totalResultCount} ${applicationContext.getString(suffix)}"
                        )
                    }.shuffled()

                    val success = preloadImages(applicationContext, *entries.map { it.imagePath }.toTypedArray())

                    if (success) {
                        return UiImagePrompt(
                            entries,
                            R.string.actor_longest_filmography_title
                        )
                    }
                }
            }
        }
        return null
    }
}
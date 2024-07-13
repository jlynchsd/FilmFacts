package com.movietrivia.filmfacts.domain

import android.annotation.SuppressLint
import android.content.Context
import android.text.format.DateFormat
import com.movietrivia.filmfacts.R
import com.movietrivia.filmfacts.api.DiscoverService
import com.movietrivia.filmfacts.model.*
import kotlinx.coroutines.*
import java.text.SimpleDateFormat
import java.util.*

class GetEarliestFilmographyUseCase(
    private val applicationContext: Context,
    private val filmFactsRepository: FilmFactsRepository,
    private val recentPromptsRepository: RecentPromptsRepository,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO
) : UseCase {

    override suspend fun invoke(includeGenres: List<Int>?) =
        withContext(dispatcher) {
            getPrompt(includeGenres)
        }

    @SuppressLint("SimpleDateFormat")
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
                                cast = listOf(it.id),
                                order = DiscoverService.Builder.Order.RELEASE_DATE_ASC
                            )
                        }
                    }.awaitAll().filterNotNull().toMutableList()
                }

                if (actorMovieResults.size == actorResults.size) {
                    val actorInfo = actorResults.zip(
                        actorMovieResults.map {
                            kotlin.runCatching { SimpleDateFormat("yyyy-MM-dd").parse(it.results.first().releaseDate) }
                                .getOrNull()?.let {
                                    with(Calendar.getInstance()) {
                                        time = it
                                        timeInMillis
                                    }
                                }
                        }
                    ).mapNotNull {
                        it.second?.let { second ->
                            Pair(it.first, second)
                        }
                    }.sortedBy { it.second }.distinctBy { it.second }
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
        return null
    }
}
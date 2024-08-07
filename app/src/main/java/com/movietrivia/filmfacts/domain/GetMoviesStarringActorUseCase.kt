package com.movietrivia.filmfacts.domain

import android.content.Context
import com.movietrivia.filmfacts.R
import com.movietrivia.filmfacts.api.DiscoverMovie
import com.movietrivia.filmfacts.model.*
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.lang.Integer.min

class GetMoviesStarringActorUseCase(
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

        if (popularActors.isNotEmpty()) {
            val prompt = getActorMovies(popularActors, 1, null, includeGenres)
            if (prompt != null) {
                val promptMovieCount = (1..min(3, prompt.second.size)).random()
                val fillerMovieCount = 4 - promptMovieCount
                val filler = getActorMovies(popularActors, fillerMovieCount, prompt.first.id, includeGenres)
                if (filler != null) {
                    val promptStartIndex = (0..prompt.second.size - promptMovieCount).random()
                    val fillerStartIndex = (0 .. filler.second.size - fillerMovieCount).random()
                    val promptMovies = prompt.second.subList(promptStartIndex, promptStartIndex + promptMovieCount).map {
                        UiImageEntry(
                            filmFactsRepository.getImageUrl(it.posterPath, ImageType.POSTER) ?: "",
                            true
                        )
                    }
                    val fillerMovies = filler.second.subList(fillerStartIndex, fillerStartIndex + fillerMovieCount).map {
                        UiImageEntry(
                            filmFactsRepository.getImageUrl(it.posterPath, ImageType.POSTER) ?: "",
                            false
                        )
                    }

                    val uiMovies = (promptMovies + fillerMovies).shuffled()
                    val success = preloadImages(applicationContext, *uiMovies.map { it.imagePath }.toTypedArray())

                    if (success) {
                        return UiImagePrompt(
                            uiMovies,
                            R.string.actor_starred_title,
                            listOf(prompt.first.name)
                        )
                    }

                }
            }
        }
        return null
    }

    private suspend fun getActorMovies(
        popularActors: MutableList<Actor>,
        minMovies: Int,
        excludeActorId: Int?,
        includeGenres: List<Int>?
    ): Pair<Actor, List<DiscoverMovie>>? {
        popularActors.apply {
            // filter popular actors in place
            val filtered = popularActors.filter { it.id != excludeActorId }
            popularActors.clear()
            popularActors.addAll(filtered)

            // if no remaining actors, fetch new ones
            if (popularActors.isEmpty()) {
                popularActors.addAll(
                    getActors(
                        filmFactsRepository,
                        recentPromptsRepository,
                        includeGenres
                    ).filter { it.id != excludeActorId }
                )

                // if no new actors, return null
                if (popularActors.isEmpty()) {
                    return null
                }
            }
        }

        var movies: List<DiscoverMovie>? = null
        var currentActor = popularActors.random()
        var remainingActorAttempts = 5
        while (popularActors.isNotEmpty() && remainingActorAttempts > 0) {
            movies = filmFactsRepository.getMovies(cast = listOf(currentActor.id), includeGenres = includeGenres)?.results
            // only filter out primary actors, filler actors can be used in different prompts
            if (excludeActorId == null) {
                recentPromptsRepository.addRecentActor(currentActor.id)
            }

            if (movies != null && movies.size >= minMovies) {
                if (excludeActorId != null) { // there are initially enough movies, but we may need to exclude some
                    val filteredMovies = mutableListOf<DiscoverMovie>()
                    var index = 0
                    movies.forEach { movie ->
                        ++index
                        if (filteredMovies.size < minMovies) { // keep finding movies that don't have the primary actor
                            val currentMovieCredits = filmFactsRepository.getMovieCredits(movie.id)
                            if (currentMovieCredits?.cast?.count { it.id == excludeActorId } == 0) {
                                filteredMovies.add(movie)
                            }
                        } else { // found enough filler movies, time to exit
                            return@forEach
                        }
                    }
                    // if there are enough filler movies we can move on, otherwise we need to pick a different filler actor
                    if (filteredMovies.size >= minMovies) {
                        movies = filteredMovies
                        break
                    } else {
                        currentActor = getNextActor(currentActor, excludeActorId, popularActors, includeGenres)
                        movies = null
                        --remainingActorAttempts
                    }
                } else { // there are enough movies and no need to exclude any, just finish
                    break
                }
            } else {
                currentActor = getNextActor(currentActor, excludeActorId, popularActors, includeGenres)
                movies = null
                --remainingActorAttempts
            }
        }

        return movies?.let {
            Pair(currentActor, it)
        }
    }

    private suspend fun getNextActor(
        currentActor: Actor,
        excludeActorId: Int?,
        popularActors: MutableList<Actor>,
        includeGenres: List<Int>?
    ): Actor {
        popularActors.remove(currentActor)
        if (popularActors.isEmpty()) {
            popularActors.addAll(
                getActors(
                    filmFactsRepository,
                    recentPromptsRepository,
                    includeGenres
                ).filter { it.id != excludeActorId })
        }
        return if (popularActors.isNotEmpty()) {
            popularActors.random()
        } else {
            currentActor
        }
    }
}
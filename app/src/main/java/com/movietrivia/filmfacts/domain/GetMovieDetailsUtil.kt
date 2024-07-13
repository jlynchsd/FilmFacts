package com.movietrivia.filmfacts.domain

import com.movietrivia.filmfacts.api.MovieDetails
import com.movietrivia.filmfacts.api.MovieImage
import com.movietrivia.filmfacts.api.PersonDetails
import com.movietrivia.filmfacts.model.Actor
import com.movietrivia.filmfacts.model.DiscoverMovie
import com.movietrivia.filmfacts.model.FilmFactsRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

suspend fun getMovieDetails(
    filmFactsRepository: FilmFactsRepository,
    movies: List<DiscoverMovie>,
    target: Int,
    filter: (details: MovieDetails) -> Boolean = { true }
): List<MovieDetails> {
    val remainingMovies = movies.toMutableList()
    val filteredMovies = mutableListOf<MovieDetails>()
    while (filteredMovies.size < target && remainingMovies.size >= (target - filteredMovies.size)) {
        val tempList = mutableListOf<DiscoverMovie>()
        repeat(target - filteredMovies.size) {
            remainingMovies.random().let { randomMovie ->
                remainingMovies.remove(randomMovie)
                tempList.add(randomMovie)
            }
        }
        val tempMovieEntries = coroutineScope {
            tempList.map {
                async {
                    filmFactsRepository.getMovieDetails(it.id)
                }
            }.awaitAll().filterNotNull()
        }
        filteredMovies.addAll(tempMovieEntries.filter(filter))
    }

    return filteredMovies
}

suspend fun getMovieImage(
    filmFactsRepository: FilmFactsRepository,
    movie: DiscoverMovie
): MovieImage? {
    filmFactsRepository.getMovieImages(movie.id)?.let { movieResponse ->
        val movieImages = movieResponse.backdrops.filter { it.language == null }
        if (movieImages.isNotEmpty()) {
            return movieImages.maxBy { it.voteCount }
        }
    }

    return null
}

suspend fun getActorDetails(
    filmFactsRepository: FilmFactsRepository,
    actors: List<Actor>,
    target: Int,
    filter: (details: PersonDetails) -> Boolean = { true }
): List<PersonDetails> {
    val remainingActors = actors.toMutableList()
    val filteredActors = mutableListOf<PersonDetails>()
    while (filteredActors.size < target && remainingActors.size >= (target - filteredActors.size)) {
        val tempList = mutableListOf<Actor>()
        repeat(target - filteredActors.size) {
            remainingActors.random().let { randomActor ->
                remainingActors.remove(randomActor)
                tempList.add(randomActor)
            }
        }
        val tempActorEntries = coroutineScope {
            tempList.map {
                async {
                    filmFactsRepository.getActorDetails(it.id)
                }
            }.awaitAll().filterNotNull()
        }
        filteredActors.addAll(tempActorEntries.filter(filter))
    }

    return filteredActors
}
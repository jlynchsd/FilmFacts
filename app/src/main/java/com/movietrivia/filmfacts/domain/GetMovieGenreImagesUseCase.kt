package com.movietrivia.filmfacts.domain

import android.content.Context
import com.bumptech.glide.Priority
import com.movietrivia.filmfacts.api.DiscoverMovie
import com.movietrivia.filmfacts.api.MovieImage
import com.movietrivia.filmfacts.model.*
import kotlinx.coroutines.*

class GetMovieGenreImagesUseCase(
    private val applicationContext: Context,
    private val filmFactsRepository: FilmFactsRepository,
    private val genreImageRepository: GenreImageRepository,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO
) {

    suspend fun invoke() =
        withContext(dispatcher) {
            val result = genreImageRepository.getGenreImages()

            val hardLoadEndIndex = 1.coerceAtMost(result.size)
            result.subList(0, hardLoadEndIndex).map { preloadImageAsync(applicationContext, it.imagePath, Priority.IMMEDIATE) }.awaitAll()
            if (result.size > hardLoadEndIndex) {
                result.subList(hardLoadEndIndex, result.size).forEach { preloadImage(applicationContext, it.imagePath, Priority.HIGH) }
            }

            result
        }

    suspend fun loadNextGenreImages(userSettings: UserSettings) =
        withContext(dispatcher) {
            coroutineScope {
                val movies = genreImageRepository.supportedGenres.map { async { getGenreMovies(userSettings, it) } }.awaitAll().flatten().distinctBy { it.id }.toMutableList()
                val result = mutableListOf<UiGenre>()
                genreImageRepository.supportedGenres.forEach { genreId ->
                    val availableMovies = if (genreId != -1) {
                        movies.filter { it.genreIds.contains(genreId) }
                    } else {
                        movies
                    }.toMutableList()

                    if (availableMovies.isNotEmpty()) {
                        var currentMovie: DiscoverMovie
                        var currentImage: MovieImage?
                        do {
                            currentMovie = availableMovies.random()
                            availableMovies.remove(currentMovie)
                            movies.remove(currentMovie)
                            currentImage = getMovieImage(filmFactsRepository, currentMovie)
                        } while (availableMovies.isNotEmpty() && currentImage == null)

                        if (currentImage != null) {
                            result.add(
                                UiGenre(
                                    filmFactsRepository.getImageUrl(currentImage.filePath, ImageType.BACKDROP) ?: "",
                                    genreId
                                )
                            )
                        }
                    }
                }

                if (result.isNotEmpty()) {
                    genreImageRepository.saveGenreImages(result)
                }
            }
        }

    private suspend fun getGenreMovies(userSettings: UserSettings, genreId: Int): List<DiscoverMovie> {
        val genreList = if (genreId != -1) {
            listOf(genreId)
        } else {
            null
        }
        return filmFactsRepository.getMovies(
            forceSettings = userSettings.copy(
                excludedGenres = emptyList(),
                releasedAfterOffset = null,
                releasedBeforeOffset = null
            ),
            includeGenres = genreList
        )?.results ?: emptyList()
    }
}
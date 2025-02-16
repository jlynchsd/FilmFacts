package com.movietrivia.filmfacts.domain

import android.content.Context
import com.bumptech.glide.Priority
import com.movietrivia.filmfacts.api.DiscoverTvShow
import com.movietrivia.filmfacts.api.TvShowImage
import com.movietrivia.filmfacts.model.*
import kotlinx.coroutines.*

class GetTvShowGenreImagesUseCase(
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
                val tvShows = genreImageRepository.supportedGenres.map { async { getGenreTvShow(userSettings, it) } }.awaitAll().flatten().distinctBy { it.id }.toMutableList()
                val result = mutableListOf<UiGenre>()
                genreImageRepository.supportedGenres.forEach { genreId ->
                    val availableTvShows = if (genreId != -1) {
                        tvShows.filter { it.genreIds.contains(genreId) }
                    } else {
                        tvShows
                    }.toMutableList()

                    if (availableTvShows.isNotEmpty()) {
                        var currentTvShow: DiscoverTvShow
                        var currentImage: TvShowImage?
                        do {
                            currentTvShow = availableTvShows.random()
                            availableTvShows.remove(currentTvShow)
                            tvShows.remove(currentTvShow)
                            currentImage = getTvShowImage(filmFactsRepository, currentTvShow, LOG_TAG)
                        } while (availableTvShows.isNotEmpty() && currentImage == null)

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

    private suspend fun getGenreTvShow(userSettings: UserSettings, genreId: Int): List<DiscoverTvShow> {
        val genreList = if (genreId != -1) {
            listOf(genreId)
        } else {
            null
        }
        return filmFactsRepository.getTvShows(
            forceSettings = userSettings.copy(
                excludedGenres = emptyList(),
                releasedAfterOffset = null,
                releasedBeforeOffset = null
            ),
            includeGenres = genreList
        )?.results ?: emptyList()
    }

    private companion object {
        const val LOG_TAG = "GetTvShowGenreImagesUseCase"
    }
}
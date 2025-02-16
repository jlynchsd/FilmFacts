package com.movietrivia.filmfacts.domain

import android.content.Context
import com.movietrivia.filmfacts.R
import com.movietrivia.filmfacts.api.DiscoverMovie
import com.movietrivia.filmfacts.api.DiscoverService
import com.movietrivia.filmfacts.api.Logger
import com.movietrivia.filmfacts.api.MovieImage
import com.movietrivia.filmfacts.model.*
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class GetMovieImageUseCase(
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
        val userSettings = userDataRepository.movieUserSettings.firstOrNullCatching() ?: return null
        val dateRange = getDateRange(userSettings, calendarProvider, LOG_TAG)
        val movies = filmFactsRepository.getMovies(
            dateRange = dateRange,
            movieOrder = DiscoverService.Builder.MovieOrder.POPULARITY_DESC,
            includeGenres = includeGenres
        )?.results?.filter { !recentPromptsRepository.isRecentMovie(it.id) }?.toMutableList()

        Logger.debug(LOG_TAG, "Movies: ${movies?.size}")

        val imagelessMovies = mutableListOf<DiscoverMovie>()

        if (!movies.isNullOrEmpty()) {
            var currentMovie: DiscoverMovie
            var currentImage: MovieImage?
            do {
                currentMovie = movies.random()
                movies.remove(currentMovie)
                currentImage = getMovieImage(filmFactsRepository, currentMovie)
                if (currentImage == null) {
                    imagelessMovies.add(currentMovie)
                }
            } while (movies.isNotEmpty() && currentImage == null)

            currentImage?.let { movieImage ->
                val remainingMovies =  (movies + imagelessMovies).toMutableList()
                if (remainingMovies.size >= 3) {
                    val otherMovies = List(3) {
                        remainingMovies.random().also {
                            remainingMovies.remove(it)
                        }
                    }

                    recentPromptsRepository.addRecentMovie(currentMovie.id)

                    val textEntries = (listOf(
                        UiTextEntry(
                            true,
                            currentMovie.title
                        )
                    ) + otherMovies.map {
                        UiTextEntry(
                            false,
                            it.title
                        )
                    }).shuffled()

                    val imageEntry = UiImageEntry(
                        filmFactsRepository.getImageUrl(movieImage.filePath, ImageType.BACKDROP) ?: "",
                        false
                    )

                    val success = preloadImages(applicationContext, imageEntry.imagePath)

                    Logger.debug(LOG_TAG, "Preloaded Images: $success")

                    if (success) {
                        return UiTextPrompt(
                            textEntries,
                            listOf(imageEntry),
                            true,
                            R.string.movie_image_title
                        )
                    }
                }
            }
        }

        Logger.info(LOG_TAG, "Unable to generate prompt")
        return null
    }

    private companion object {
        const val LOG_TAG = "GetMovieImageUseCase"
    }
}
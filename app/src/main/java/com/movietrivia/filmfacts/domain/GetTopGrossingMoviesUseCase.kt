package com.movietrivia.filmfacts.domain

import android.content.Context
import com.movietrivia.filmfacts.R
import com.movietrivia.filmfacts.api.DiscoverService
import com.movietrivia.filmfacts.api.Logger
import com.movietrivia.filmfacts.model.*
import kotlinx.coroutines.*

class GetTopGrossingMoviesUseCase(
    private val applicationContext: Context,
    private val filmFactsRepository: FilmFactsRepository,
    private val recentPromptsRepository: RecentPromptsRepository,
    private val userDataRepository: UserDataRepository,
    private val calendarProvider: CalendarProvider,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO
): UseCase {

    override suspend fun invoke(includeGenres: List<Int>?) =
        withContext(dispatcher) {
            getPrompt(includeGenres)
        }

    private suspend fun getPrompt(includeGenres: List<Int>?): UiPrompt? {
        val userSettings = userDataRepository.movieUserSettings.firstOrNullCatching() ?: return null
        val movies = filmFactsRepository.getMovies(
            dateRange = getDateRange(userSettings, calendarProvider, LOG_TAG),
            movieOrder = DiscoverService.Builder.MovieOrder.REVENUE_DESC,
            includeGenres = includeGenres
        )?.results?.filter { !recentPromptsRepository.isRecentMovie(it.id) }?.toMutableList()

        Logger.debug(LOG_TAG, "Movies: ${movies?.size}")

        if (movies != null && movies.size >= 4) {
            val filteredMovies = getMovieDetails(
                filmFactsRepository,
                movies,
                4
            ) { it.revenue > 0 && it.posterPath.isNotEmpty() }.distinctBy { it.revenue }.toMutableList()

            Logger.debug(LOG_TAG, "Filtered Movies: ${filteredMovies.size}")

            if (filteredMovies.size >= 4) {
                filteredMovies.forEach { recentPromptsRepository.addRecentMovie(it.id) }
                filteredMovies.sortByDescending { it.revenue }
                val uiImageEntries = filteredMovies.mapIndexed { index, filteredMovie ->
                    UiImageEntry(
                        filmFactsRepository.getImageUrl(filteredMovie.posterPath, ImageType.POSTER) ?: "",
                        index == 0,
                        data = formatRevenue(filteredMovie.revenue, applicationContext)
                    )
                }.shuffled()

                val success = preloadImages(applicationContext, *uiImageEntries.map { it.imagePath }.toTypedArray())

                Logger.debug(LOG_TAG, "Preloaded Images: $success")

                if (success) {
                    return UiImagePrompt(
                        uiImageEntries,
                        R.string.top_grossing_movie_title
                    )
                }
            }
        }

        Logger.info(LOG_TAG, "Unable to generate prompt")
        return null
    }

    private companion object {
        const val LOG_TAG = "GetTopGrossingMoviesUseCase"
    }
}
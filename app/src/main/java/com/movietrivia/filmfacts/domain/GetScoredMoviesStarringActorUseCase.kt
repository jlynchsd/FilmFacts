package com.movietrivia.filmfacts.domain

import android.content.Context
import androidx.annotation.VisibleForTesting
import com.movietrivia.filmfacts.R
import com.movietrivia.filmfacts.api.DiscoverMovie
import com.movietrivia.filmfacts.api.DiscoverMovieResponse
import com.movietrivia.filmfacts.api.DiscoverService
import com.movietrivia.filmfacts.api.Logger
import com.movietrivia.filmfacts.api.MovieDetails
import com.movietrivia.filmfacts.model.*
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class GetScoredMoviesStarringActorUseCase (
    private val applicationContext: Context,
    private val filmFactsRepository: FilmFactsRepository,
    private val recentPromptsRepository: RecentPromptsRepository,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO
) : UseCase {

    override suspend fun invoke(includeGenres: List<Int>?) =
        withContext(dispatcher) {
            getPrompt(includeGenres)
        }

    @VisibleForTesting
    internal suspend fun getPrompt(includeGenres: List<Int>?, forceStrategy: ScoreStrategies? = null): UiPrompt? {
        val popularActors = getMovieActors(
            filmFactsRepository,
            recentPromptsRepository,
            includeGenres
        ).toMutableList()

        Logger.debug(LOG_TAG, "Popular Actors: ${popularActors.size}")

        if (popularActors.isNotEmpty()) {
            val strategy = forceStrategy ?: ScoreStrategies.values().random()
            Logger.debug(LOG_TAG, "Score Strategy: $strategy")
            val (order, title) = getScoreMetaData(strategy)
            val movieCount = 4

            var actor: Actor
            var movieResponse: DiscoverMovieResponse?
            var movies: List<DiscoverMovie>?
            do {
                actor = popularActors.random()
                popularActors.remove(actor)
                movieResponse = filmFactsRepository.getMovies(
                    cast = listOf(actor.id),
                    movieOrder = order,
                    includeGenres = includeGenres
                )
                movies = movieResponse?.results
            }
            while (popularActors.isNotEmpty() && (movies != null && movies.size < movieCount))

            Logger.debug(LOG_TAG, "Initial Movies: ${movies?.size}")

            if (movies != null && movies.size >= movieCount) {
                val filter: (MovieDetails) -> Boolean = if (strategy == ScoreStrategies.HIGHEST_GROSSING) {
                    { details: MovieDetails -> details.revenue > 0 && details.posterPath.isNotEmpty() }
                } else {
                    { details: MovieDetails -> details.posterPath.isNotEmpty() }
                }
                val selectedMovies = getMovieDetails(
                    filmFactsRepository,
                    movies,
                    movieCount,
                    filter
                ).toMutableList()

                Logger.debug(LOG_TAG, "Selected Movies: ${selectedMovies.size}")

                if (selectedMovies.size >= movieCount) {
                    recentPromptsRepository.addRecentActor(actor.id)
                    when (strategy) {
                        ScoreStrategies.TOP_RATED -> selectedMovies.sortByDescending { it.voteAverage }
                        ScoreStrategies.WORST_RATED -> selectedMovies.sortBy { it.voteAverage }
                        ScoreStrategies.HIGHEST_GROSSING -> selectedMovies.sortByDescending { it.revenue }
                    }

                    val uiImageEntries = selectedMovies.mapIndexed { index, filteredMovie ->
                        UiImageEntry(
                            filmFactsRepository.getImageUrl(filteredMovie.posterPath, ImageType.POSTER) ?: "",
                            index == 0,
                            data = when (strategy) {
                                ScoreStrategies.TOP_RATED, ScoreStrategies.WORST_RATED -> filteredMovie.voteAverage.toString()
                                ScoreStrategies.HIGHEST_GROSSING -> formatRevenue(filteredMovie.revenue, applicationContext)
                            }
                        )
                    }.shuffled()

                    val success = preloadImages(applicationContext, *uiImageEntries.map { it.imagePath }.toTypedArray())

                    Logger.debug(LOG_TAG, "Preloaded Images: $success")

                    if (success) {
                        return UiImagePrompt(
                            uiImageEntries,
                            title,
                            listOf(actor.name)
                        )
                    }
                }
            }
        }

        Logger.info(LOG_TAG, "Unable to generate prompt")
        return null
    }

    private fun getScoreMetaData(strategy: ScoreStrategies) =
        when (strategy) {
            ScoreStrategies.TOP_RATED -> {
                Pair(DiscoverService.Builder.MovieOrder.POPULARITY_DESC, R.string.movie_actor_highest_rated_title)
            }

            ScoreStrategies.WORST_RATED -> {
                Pair(DiscoverService.Builder.MovieOrder.POPULARITY_ASC, R.string.movie_actor_worst_rated_title)
            }

            ScoreStrategies.HIGHEST_GROSSING -> {
                Pair(DiscoverService.Builder.MovieOrder.REVENUE_DESC, R.string.movie_actor_highest_grossing_title)
            }
        }

    @VisibleForTesting
    internal enum class ScoreStrategies {
        TOP_RATED,
        WORST_RATED,
        HIGHEST_GROSSING
    }

    private companion object {
        const val LOG_TAG = "GetScoredMoviesStarringActorUseCase"
    }
}
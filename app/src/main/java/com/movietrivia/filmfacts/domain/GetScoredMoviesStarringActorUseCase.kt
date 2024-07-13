package com.movietrivia.filmfacts.domain

import android.content.Context
import androidx.annotation.VisibleForTesting
import com.movietrivia.filmfacts.R
import com.movietrivia.filmfacts.api.DiscoverMovieResponse
import com.movietrivia.filmfacts.api.DiscoverService
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
        val popularActors = getActors(
            filmFactsRepository,
            recentPromptsRepository,
            includeGenres
        ).toMutableList()

        if (popularActors.isNotEmpty()) {
            val strategy = forceStrategy ?: ScoreStrategies.values().random()
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
                    order = order,
                    includeGenres = includeGenres
                )
                movies = movieResponse?.results
            }
            while (popularActors.isNotEmpty() && (movies != null && movies.size < movieCount))


            if (movies != null && movies.size >= movieCount) {
                val filter: (MovieDetails) -> Boolean = if (strategy == ScoreStrategies.HIGHEST_GROSSING) {
                    { details: MovieDetails -> details.revenue > 0 }
                } else {
                    { true }
                }
                val selectedMovies = getMovieDetails(
                    filmFactsRepository,
                    movies,
                    movieCount,
                    filter
                ).toMutableList()

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
        return null
    }

    private fun getScoreMetaData(strategy: ScoreStrategies) =
        when (strategy) {
            ScoreStrategies.TOP_RATED -> {
                Pair(DiscoverService.Builder.Order.POPULARITY_DESC, R.string.actor_highest_rated_title)
            }

            ScoreStrategies.WORST_RATED -> {
                Pair(DiscoverService.Builder.Order.POPULARITY_ASC, R.string.actor_worst_rated_title)
            }

            ScoreStrategies.HIGHEST_GROSSING -> {
                Pair(DiscoverService.Builder.Order.REVENUE_DESC, R.string.actor_highest_grossing_title)
            }
        }

    @VisibleForTesting
    internal enum class ScoreStrategies {
        TOP_RATED,
        WORST_RATED,
        HIGHEST_GROSSING
    }
}
package com.movietrivia.filmfacts.domain

import com.movietrivia.filmfacts.api.DiscoverMovie
import com.movietrivia.filmfacts.api.DiscoverService
import com.movietrivia.filmfacts.api.DiscoverTvShow
import com.movietrivia.filmfacts.api.Logger
import com.movietrivia.filmfacts.api.MovieDetails
import com.movietrivia.filmfacts.api.MovieImage
import com.movietrivia.filmfacts.api.PersonDetails
import com.movietrivia.filmfacts.api.TvShowDetails
import com.movietrivia.filmfacts.api.TvShowImage
import com.movietrivia.filmfacts.model.Actor
import com.movietrivia.filmfacts.model.FilmFactsRepository
import com.movietrivia.filmfacts.model.RecentPromptsRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import java.util.Date

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

suspend fun getTvShows(
    filmFactsRepository: FilmFactsRepository,
    recentPromptsRepository: RecentPromptsRepository,
    dateRange: Pair<Date?, Date?>?,
    tvShowOrder: DiscoverService.Builder.TvShowOrder,
    includeGenres: List<Int>?,
    filterRecent: Boolean,
    minimumVotes: Int? = null,
    logTag: String
): MutableList<DiscoverTvShow>? {
    val tvShowResponse = filmFactsRepository.getTvShows(
        dateRange = dateRange,
        tvShowOrder = tvShowOrder,
        includeGenres = includeGenres,
        minimumVotes = minimumVotes
    )
    var tvShows = tvShowResponse?.results?.toMutableList()
    if (filterRecent) {
        tvShows = tvShows?.filter { !recentPromptsRepository.isRecentTvShow(it.id) }?.toMutableList()
    }
    if (tvShowResponse != null && tvShows != null && tvShows.size < 15) {
        val page = when  {
            tvShowResponse.totalPageCount > 2 -> (2 until tvShowResponse.totalPageCount)
            tvShowResponse.totalPageCount == 2 -> (2 .. tvShowResponse.totalPageCount)
            else -> IntRange.EMPTY
        }.randomOrNull()
        Logger.debug(logTag, "Failed to get more than ${tvShows.size} from page ${tvShowResponse.page}, trying page $page of ${tvShowResponse.totalPageCount} with total results of ${tvShowResponse.totalResultCount}")
        if (page != null) {
            var additionalShows = filmFactsRepository.getTvShows(
                dateRange = dateRange,
                tvShowOrder = tvShowOrder,
                includeGenres = includeGenres,
                minimumVotes = minimumVotes,
                page = page
            )?.results
            if (filterRecent) {
                additionalShows = additionalShows?.filter { !recentPromptsRepository.isRecentTvShow(it.id) }
            }
            additionalShows?.let {
                tvShows.addAll(it)
            }
        } else {
            Logger.debug(logTag, "Unable to get page from total page count: ${tvShowResponse.totalPageCount}, with current page ${tvShowResponse.page}, and total results: ${tvShowResponse.totalResultCount}")
        }
    }

    return tvShows
}

suspend fun getTvShowDetails(
    filmFactsRepository: FilmFactsRepository,
    tvShows: List<DiscoverTvShow>,
    target: Int,
    filter: (details: TvShowDetails) -> Boolean = { true },
    postFilter: (result: MutableList<TvShowDetails>) -> MutableList<TvShowDetails> = { it }
): List<TvShowDetails> {
    val remainingTvShows = tvShows.toMutableList()
    var filteredTvShows = mutableListOf<TvShowDetails>()
    while (filteredTvShows.size < target && remainingTvShows.size >= (target - filteredTvShows.size)) {
        val tempList = mutableListOf<DiscoverTvShow>()
        repeat(target - filteredTvShows.size) {
            remainingTvShows.random().let { randomTvShow ->
                remainingTvShows.remove(randomTvShow)
                tempList.add(randomTvShow)
            }
        }
        val tempTvShowEntries = coroutineScope {
            tempList.map {
                async {
                    filmFactsRepository.getTvShowDetails(it.id)
                }
            }.awaitAll().filterNotNull()
        }
        filteredTvShows.addAll(tempTvShowEntries.filter(filter))
        filteredTvShows = postFilter(filteredTvShows)
    }

    return filteredTvShows
}

suspend fun getTvShowImage(
    filmFactsRepository: FilmFactsRepository,
    tvShow: DiscoverTvShow,
    logTag: String
): TvShowImage? {
    filmFactsRepository.getTvShowImages(tvShow.id)?.let { tvShowResponse ->
        Logger.debug(logTag, "Total TV Show Images: ${tvShowResponse.backdrops.size}")
        val tvImages = tvShowResponse.backdrops.filter { it.language == null }
        Logger.debug(logTag, "Filtered TV Show Images: ${tvImages.size}")
        if (tvImages.isNotEmpty()) {
            return tvImages.maxBy { it.voteCount }
        }
    }

    Logger.debug(logTag, "Unable to get TV show images for $tvShow")

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
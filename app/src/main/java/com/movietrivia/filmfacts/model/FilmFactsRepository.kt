package com.movietrivia.filmfacts.model

import android.annotation.SuppressLint
import com.movietrivia.filmfacts.api.DiscoverMovie
import com.movietrivia.filmfacts.api.DiscoverMovieResponse
import com.movietrivia.filmfacts.api.DiscoverService
import com.movietrivia.filmfacts.api.DiscoverTvShow
import com.movietrivia.filmfacts.api.DiscoverTvShowResponse
import com.movietrivia.filmfacts.api.ImageConfiguration
import com.movietrivia.filmfacts.domain.containsAny
import com.movietrivia.filmfacts.domain.dateWithinRange
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import kotlin.coroutines.coroutineContext

class FilmFactsRepository @Inject constructor(
    private val remoteDataSource: TmdbDataSource,
    private val userDataRepository: UserDataRepository
) {
    private var imageConfiguration: ImageConfiguration? = null

    suspend fun getImageUrl(path: String, type: ImageType): String? {
        if (imageConfiguration == null) {
            imageConfiguration = getImageConfiguration()
        }

        imageConfiguration?.let {
            val subUrl = when (type) {
                ImageType.POSTER -> it.posterSizes.last()
                ImageType.PROFILE -> it.profileSizes.last()
                ImageType.BACKDROP -> it.backdropSizes.last()
            }
            return it.secureBaseUrl + subUrl + path
        }

        return null
    }

    private suspend fun getImageConfiguration() = remoteDataSource.getImageConfiguration()

    suspend fun getMovies(forceSettings: UserSettings? = null,
                          dateRange: Pair<Date?, Date?>? = null,
                          movieOrder: DiscoverService.Builder.MovieOrder? = null,
                          releaseType: DiscoverService.Builder.ReleaseType? = null,
                          includeGenres: List<Int>? = null,
                          excludeGenres: List<Int>? = null,
                          cast: List<Int>? = null,
                          minimumVotes: Int? = 20,
                          page: Int = 1
    ): DiscoverMovieResponse? {
        val userSettings = forceSettings ?: userDataRepository.movieUserSettings.first()
        val accountDetails = userDataRepository.accountDetails.value
        val scope = CoroutineScope(coroutineContext)
        val requestedMovies = scope.async {
            remoteDataSource.getMovies(
                userSettings,
                dateRange,
                movieOrder,
                releaseType,
                includeGenres,
                excludeGenres,
                cast,
                minimumVotes,
                page
            )
        }
        if (accountDetails is PendingData.Success &&
            releaseType == null &&
            forceSettings == null &&
            cast == null &&
            (movieOrder == null || !unsupportedOrdering.contains(movieOrder))
        ) {
            val accountMovies = scope.async { getAccountMovies(accountDetails.result) }
            val requestedResponse = requestedMovies.await()
            val accountResponse = accountMovies.await()
            val filteredResponse = accountResponse?.let {
                filterMovies(
                    it,
                    dateRange,
                    includeGenres,
                    userSettings.excludedGenres + (excludeGenres ?: emptyList()),
                    userSettings.language
                )
            } ?: emptyList()
            return requestedResponse?.let { response ->
                val updatedResults = (response.results + filteredResponse).distinctBy { it.id }.toMutableList()
                if (movieOrder != null && filteredResponse.isNotEmpty()) {
                    when (movieOrder) {
                        DiscoverService.Builder.MovieOrder.POPULARITY_ASC -> updatedResults.sortBy { it.popularity }
                        DiscoverService.Builder.MovieOrder.POPULARITY_DESC -> updatedResults.sortByDescending { it.popularity }
                        DiscoverService.Builder.MovieOrder.VOTE_AVERAGE_ASC -> updatedResults.sortBy { it.voteAverage }
                        DiscoverService.Builder.MovieOrder.VOTE_AVERAGE_DESC -> updatedResults.sortByDescending { it.voteAverage }
                        DiscoverService.Builder.MovieOrder.VOTE_COUNT_ASC -> updatedResults.sortBy { it.voteCount }
                        DiscoverService.Builder.MovieOrder.VOTE_COUNT_DESC -> updatedResults.sortByDescending { it.voteCount }
                        DiscoverService.Builder.MovieOrder.RELEASE_DATE_ASC -> updatedResults.sortBy { parseDate(it.releaseDate) }
                        DiscoverService.Builder.MovieOrder.RELEASE_DATE_DESC -> updatedResults.sortByDescending { parseDate(it.releaseDate) }
                        DiscoverService.Builder.MovieOrder.REVENUE_ASC, DiscoverService.Builder.MovieOrder.REVENUE_DESC -> {}
                    }
                }
                response.copy(
                    results = updatedResults,
                    totalResultCount = response.totalResultCount + (updatedResults.size - response.results.size)
                )
            }
        } else {
            return requestedMovies.await()
        }
    }

    suspend fun getTvShows(forceSettings: UserSettings? = null,
                          dateRange: Pair<Date?, Date?>? = null,
                          tvShowOrder: DiscoverService.Builder.TvShowOrder? = null,
                          includeGenres: List<Int>? = null,
                          excludeGenres: List<Int>? = null,
                          minimumVotes: Int? = 20,
                          page: Int = 1
    ): DiscoverTvShowResponse? {
        val userSettings = forceSettings ?: userDataRepository.tvShowUserSettings.first()
        val accountDetails = userDataRepository.accountDetails.value
        val scope = CoroutineScope(coroutineContext)
        val requestedTvShows = scope.async {
            remoteDataSource.getTvShows(
                userSettings,
                dateRange,
                tvShowOrder,
                includeGenres,
                excludeGenres,
                minimumVotes,
                page
            )
        }
        if (accountDetails is PendingData.Success &&
            forceSettings == null
        ) {
            val accountTvShows = scope.async { getAccountTvShows(accountDetails.result) }
            val requestedResponse = requestedTvShows.await()
            val accountResponse = accountTvShows.await()
            val filteredResponse = accountResponse?.let {
                filterTvShows(
                    it,
                    dateRange,
                    includeGenres,
                    userSettings.excludedGenres + (excludeGenres ?: emptyList()),
                    userSettings.language
                )
            } ?: emptyList()
            return requestedResponse?.let { response ->
                val updatedResults = (response.results + filteredResponse).distinctBy { it.id }.toMutableList()
                if (tvShowOrder != null && filteredResponse.isNotEmpty()) {
                    when (tvShowOrder) {
                        DiscoverService.Builder.TvShowOrder.POPULARITY_ASC -> updatedResults.sortBy { it.popularity }
                        DiscoverService.Builder.TvShowOrder.POPULARITY_DESC -> updatedResults.sortByDescending { it.popularity }
                        DiscoverService.Builder.TvShowOrder.VOTE_AVERAGE_ASC -> updatedResults.sortBy { it.voteAverage }
                        DiscoverService.Builder.TvShowOrder.VOTE_AVERAGE_DESC -> updatedResults.sortByDescending { it.voteAverage }
                        DiscoverService.Builder.TvShowOrder.VOTE_COUNT_ASC -> updatedResults.sortBy { it.voteCount }
                        DiscoverService.Builder.TvShowOrder.VOTE_COUNT_DESC -> updatedResults.sortByDescending { it.voteCount }
                        DiscoverService.Builder.TvShowOrder.FIRST_AIR_DATE_ASC -> updatedResults.sortBy { parseDate(it.firstAirDate) }
                        DiscoverService.Builder.TvShowOrder.FIRST_AIR_DATE_DESC -> updatedResults.sortByDescending { parseDate(it.firstAirDate) }
                    }
                }
                response.copy(
                    results = updatedResults,
                    totalResultCount = response.totalResultCount + (updatedResults.size - response.results.size)
                )
            }
        } else {
            return requestedTvShows.await()
        }
    }

    suspend fun getMovieCredits(movieId: Int) = remoteDataSource.getMovieCredits(movieId)

    suspend fun getMovieDetails(movieId: Int) = remoteDataSource.getMovieDetails(movieId)

    suspend fun getMovieImages(movieId: Int) = remoteDataSource.getMovieImages(movieId)

    suspend fun getTvShowCredits(showId: Int) = remoteDataSource.getTvShowCredits(showId)

    suspend fun getTvShowDetails(showId: Int) = remoteDataSource.getTvShowDetails(showId)

    suspend fun getTvShowImages(showId: Int) = remoteDataSource.getTvShowImages(showId)

    suspend fun getActorDetails(actorId: Int) = remoteDataSource.getActorDetails(actorId)

    suspend fun getActorMovieCredits(actorId: Int) = remoteDataSource.getActorMovieCredits(actorId)

    suspend fun getActorTvShowCredits(actorId: Int) = remoteDataSource.getActorTvShowCredits(actorId)

    private suspend fun getAccountMovies(accountDetails: AccountDetails): List<DiscoverMovie>? {
        val availableStrategies = mutableListOf<AccountDataStrategies>()
        if (accountDetails.favoriteMoviesMetaData.totalEntries > 0) {
            availableStrategies.add(AccountDataStrategies.FAVORITE)
        }
        if (accountDetails.ratedMoviesMetaData.totalEntries > 0) {
            availableStrategies.add(AccountDataStrategies.RATED)
        }
        if (accountDetails.watchlistMoviesMetaData.totalEntries > 0) {
            availableStrategies.add(AccountDataStrategies.WATCHLIST)
        }

        if (availableStrategies.isEmpty()) {
            return null
        }

        return when (availableStrategies.random()) {
            AccountDataStrategies.FAVORITE -> {
                userDataRepository.getAccountFavoriteMovies(getPage(accountDetails.favoriteMoviesMetaData.totalPages))?.results
            }

            AccountDataStrategies.RATED -> {
                userDataRepository.getAccountRatedMovies(
                    getPage(accountDetails.ratedMoviesMetaData.totalPages)
                )?.results?.filter { it.rating > 5 }?.map {
                    DiscoverMovie(
                        id = it.id,
                        title = it.title,
                        posterPath = it.posterPath,
                        genreIds = it.genreIds,
                        releaseDate = it.releaseDate,
                        originalLanguage = it.originalLanguage,
                        popularity = it.popularity,
                        voteCount = it.voteCount,
                        voteAverage = it.voteAverage
                    )
                }
            }

            AccountDataStrategies.WATCHLIST -> {
                userDataRepository.getAccountWatchlistMovies(getPage(accountDetails.watchlistMoviesMetaData.totalPages))?.results
            }
        }
    }

    private suspend fun getAccountTvShows(accountDetails: AccountDetails): List<DiscoverTvShow>? {
        val availableStrategies = mutableListOf<AccountDataStrategies>()
        if (accountDetails.favoriteTvShowsMetaData.totalEntries > 0) {
            availableStrategies.add(AccountDataStrategies.FAVORITE)
        }
        if (accountDetails.ratedTvShowsMetaData.totalEntries > 0) {
            availableStrategies.add(AccountDataStrategies.RATED)
        }
        if (accountDetails.watchlistTvShowsMetaData.totalEntries > 0) {
            availableStrategies.add(AccountDataStrategies.WATCHLIST)
        }

        if (availableStrategies.isEmpty()) {
            return null
        }

        return when (availableStrategies.random()) {
            AccountDataStrategies.FAVORITE -> {
                userDataRepository.getAccountFavoriteTvShows(getPage(accountDetails.favoriteTvShowsMetaData.totalPages))?.results
            }

            AccountDataStrategies.RATED -> {
                userDataRepository.getAccountRatedTvShows(
                    getPage(accountDetails.ratedTvShowsMetaData.totalPages)
                )?.results?.filter { it.rating > 5 }?.map {
                    DiscoverTvShow(
                        id = it.id,
                        name = it.name,
                        posterPath = it.posterPath,
                        genreIds = it.genreIds,
                        firstAirDate = it.firstAirDate,
                        originCountry = it.originCountry,
                        originalLanguage = it.originalLanguage,
                        originalName = it.originalName,
                        overview = it.overview,
                        popularity = it.popularity,
                        voteCount = it.voteCount,
                        voteAverage = it.voteAverage
                    )
                }
            }

            AccountDataStrategies.WATCHLIST -> {
                userDataRepository.getAccountWatchlistTvShows(getPage(accountDetails.watchlistTvShowsMetaData.totalPages))?.results
            }
        }
    }

    private fun getPage(maxPages: Int) = (1 .. maxPages).random()

    private fun filterMovies(
        movies: List<DiscoverMovie>,
        dateRange: Pair<Date?, Date?>? = null,
        includeGenres: List<Int>? = null,
        excludeGenres: List<Int>? = null,
        language: String
    ) = movies.filter { movie ->
        dateRange?.let { dateWithinRange(movie.releaseDate,  dateToOffset(it.first), dateToOffset(it.second)) } ?: true &&
                includeGenres?.let { movie.genreIds.containsAny(it) } ?: true &&
                excludeGenres?.let { !movie.genreIds.containsAny(it) } ?: true &&
                movie.originalLanguage == language
    }

    private fun filterTvShows(
        tvShows: List<DiscoverTvShow>,
        dateRange: Pair<Date?, Date?>? = null,
        includeGenres: List<Int>? = null,
        excludeGenres: List<Int>? = null,
        language: String
    ) = tvShows.filter { tvShow ->
        dateRange?.let { dateWithinRange(tvShow.firstAirDate,  dateToOffset(it.first), dateToOffset(it.second)) } ?: true &&
                includeGenres?.let { tvShow.genreIds.containsAny(it) } ?: true &&
                excludeGenres?.let { !tvShow.genreIds.containsAny(it) } ?: true &&
                tvShow.originalLanguage == language
    }

    private fun dateToOffset(date: Date?) =
        date?.let {
            val current = Calendar.getInstance()
            val other = Calendar.getInstance()
            other.time = it
            current.get(Calendar.YEAR) - other.get(Calendar.YEAR)
        }

    @SuppressLint("SimpleDateFormat")
    private fun parseDate(date: String) = kotlin.runCatching { SimpleDateFormat("yyyy-MM-dd").parse(date) }.getOrNull()?.time ?: 0L

    private companion object {
        val unsupportedOrdering = listOf(DiscoverService.Builder.MovieOrder.REVENUE_ASC, DiscoverService.Builder.MovieOrder.REVENUE_DESC)
    }
}

enum class ImageType {
    POSTER,
    PROFILE,
    BACKDROP
}

private enum class AccountDataStrategies {
    FAVORITE,
    RATED,
    WATCHLIST
}
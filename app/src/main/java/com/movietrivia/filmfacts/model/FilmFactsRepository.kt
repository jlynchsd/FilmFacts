package com.movietrivia.filmfacts.model

import android.annotation.SuppressLint
import com.movietrivia.filmfacts.api.DiscoverMovieResponse
import com.movietrivia.filmfacts.api.DiscoverService
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
                          dateRange: Pair<Date, Date>? = null,
                          order: DiscoverService.Builder.Order? = null,
                          releaseType: DiscoverService.Builder.ReleaseType? = null,
                          includeGenres: List<Int>? = null,
                          excludeGenres: List<Int>? = null,
                          cast: List<Int>? = null,
                          minimumVotes: Int? = 20,
                          page: Int = 1
    ): DiscoverMovieResponse? {
        val userSettings = forceSettings ?: userDataRepository.userSettings.first()
        val accountDetails = userDataRepository.accountDetails.value
        val scope = CoroutineScope(coroutineContext)
        val requestedMovies = scope.async {
            remoteDataSource.getMovies(
                userSettings,
                dateRange,
                order,
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
            (order == null || !unsupportedOrdering.contains(order))
        ) {
            val accountMovies = scope.async { getAccountMovies(accountDetails.result) }
            val requestedResponse = requestedMovies.await()
            val accountResponse = accountMovies.await()
            val filteredResponse = accountResponse?.let {
                filterMovies(
                    it,
                    dateRange,
                    includeGenres,
                    userSettings.excludedFilmGenres + (excludeGenres ?: emptyList()),
                    userSettings.language
                )
            } ?: emptyList()
            return requestedResponse?.let { response ->
                val updatedResults = (response.results + filteredResponse).distinctBy { it.id }.toMutableList()
                if (order != null && filteredResponse.isNotEmpty()) {
                    when (order) {
                        DiscoverService.Builder.Order.POPULARITY_ASC -> updatedResults.sortBy { it.popularity }
                        DiscoverService.Builder.Order.POPULARITY_DESC -> updatedResults.sortByDescending { it.popularity }
                        DiscoverService.Builder.Order.VOTE_AVERAGE_ASC -> updatedResults.sortBy { it.voteAverage }
                        DiscoverService.Builder.Order.VOTE_AVERAGE_DESC -> updatedResults.sortByDescending { it.voteAverage }
                        DiscoverService.Builder.Order.VOTE_COUNT_ASC -> updatedResults.sortBy { it.voteCount }
                        DiscoverService.Builder.Order.VOTE_COUNT_DESC -> updatedResults.sortByDescending { it.voteCount }
                        DiscoverService.Builder.Order.RELEASE_DATE_ASC -> updatedResults.sortBy { parseDate(it.releaseDate) }
                        DiscoverService.Builder.Order.RELEASE_DATE_DESC -> updatedResults.sortByDescending { parseDate(it.releaseDate) }
                        DiscoverService.Builder.Order.REVENUE_ASC, DiscoverService.Builder.Order.REVENUE_DESC -> {}
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

    suspend fun getMovieCredits(movieId: Int) = remoteDataSource.getMovieCredits(movieId)

    suspend fun getMovieDetails(movieId: Int) = remoteDataSource.getMovieDetails(movieId)

    suspend fun getMovieImages(movieId: Int) = remoteDataSource.getMovieImages(movieId)

    suspend fun getActorDetails(actorId: Int) = remoteDataSource.getActorDetails(actorId)

    suspend fun getActorCredits(actorId: Int) = remoteDataSource.getActorCredits(actorId)

    private suspend fun getAccountMovies(accountDetails: AccountDetails): List<DiscoverMovie>? {
        val availableStrategies = mutableListOf<AccountDataStrategies>()
        if (accountDetails.favoriteMetaData.totalEntries > 0) {
            availableStrategies.add(AccountDataStrategies.FAVORITE)
        }
        if (accountDetails.ratedMetaData.totalEntries > 0) {
            availableStrategies.add(AccountDataStrategies.RATED)
        }
        if (accountDetails.watchlistMetaData.totalEntries > 0) {
            availableStrategies.add(AccountDataStrategies.WATCHLIST)
        }

        if (availableStrategies.isEmpty()) {
            return null
        }

        return when (availableStrategies.random()) {
            AccountDataStrategies.FAVORITE -> {
                userDataRepository.getAccountFavoriteMovies(getPage(accountDetails.favoriteMetaData.totalPages))?.results
            }

            AccountDataStrategies.RATED -> {
                userDataRepository.getAccountRatedMovies(
                    getPage(accountDetails.ratedMetaData.totalPages)
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
                userDataRepository.getAccountWatchlistMovies(getPage(accountDetails.watchlistMetaData.totalPages))?.results
            }
        }
    }

    private fun getPage(maxPages: Int) = (1 .. maxPages).random()

    private fun filterMovies(
        movies: List<DiscoverMovie>,
        dateRange: Pair<Date, Date>? = null,
        includeGenres: List<Int>? = null,
        excludeGenres: List<Int>? = null,
        language: String
    ) = movies.filter { movie ->
        dateRange?.let { dateWithinRange(movie.releaseDate,  dateToOffset(it.first), dateToOffset(it.second)) } ?: true &&
                includeGenres?.let { movie.genreIds.containsAny(it) } ?: true &&
                excludeGenres?.let { !movie.genreIds.containsAny(it) } ?: true &&
                movie.originalLanguage == language
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
        val unsupportedOrdering = listOf(DiscoverService.Builder.Order.REVENUE_ASC, DiscoverService.Builder.Order.REVENUE_DESC)
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
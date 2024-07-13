package com.movietrivia.filmfacts.model

import com.movietrivia.filmfacts.api.*
import java.util.*
import javax.inject.Inject

class TmdbDataSource @Inject constructor(
    private val discoverService: DiscoverService,
    private val configurationService: ConfigurationService,
    private val personService: PersonService,
    private val movieService: MovieService,
    private val tooManyRequestsDataSource: TooManyRequestsDataSource
) {
    suspend fun getImageConfiguration() = makeNetworkCall(tooManyRequestsDataSource) {
        configurationService.getConfiguration(ConfigurationService.options).body()?.images
    }

    suspend fun getMovieCredits(movieId: Int) = makeNetworkCall(tooManyRequestsDataSource) {
        movieService.getMovieCredits(movieId, MovieService.options).body()
    }

    suspend fun getMovieDetails(movieId: Int) = makeNetworkCall(tooManyRequestsDataSource) {
        movieService.getMovieDetails(movieId, MovieService.options).body()
    }

    suspend fun getMovieImages(movieId: Int) = makeNetworkCall(tooManyRequestsDataSource) {
        movieService.getMovieImages(movieId, MovieService.options).body()
    }

    suspend fun getActorDetails(actorId: Int) = makeNetworkCall(tooManyRequestsDataSource) {
        personService.getActorDetails(actorId, PersonService.Builder().build()).body()
    }

    suspend fun getActorCredits(actorId: Int) = makeNetworkCall(tooManyRequestsDataSource) {
        personService.getActorCredits(actorId, PersonService.Builder().build()).body()?.credits
    }

    suspend fun getMovies(
        userSettings: UserSettings,
        dateRange: Pair<Date, Date>?,
        order: DiscoverService.Builder.Order?,
        releaseType: DiscoverService.Builder.ReleaseType?,
        includeGenres: List<Int>?,
        excludeGenres: List<Int>?,
        cast: List<Int>?,
        minimumVotes: Int?,
        page: Int
    ):  DiscoverMovieResponse? {
        val builder = DiscoverService.Builder(userSettings, page)
        dateRange?.let {
            builder.releasedInRange(it.first, it.second)
        }
        order?.let {
            builder.orderBy(it)
        }
        releaseType?.let {
            builder.withReleaseType(it)
        }
        includeGenres?.let {
            builder.withGenres(*it.toIntArray())
        }
        excludeGenres?.let {
            builder.withoutGenres(*it.toIntArray())
        }
        cast?.let {
            builder.withCast(*it.toIntArray())
        }
        minimumVotes?.let {
            builder.withVotesGreaterThan(it)
        }
        return makeNetworkCall(tooManyRequestsDataSource) {
            discoverService.getMovies(builder.build()).body()?.let { response ->
                response.copy(
                    results = response.results.distinctBy { it.title }.filter { it.posterPath.isNotEmpty() && it.genreIds.isNotEmpty() && it.releaseDate.isNotEmpty()}
                )
            }
        }
    }
}
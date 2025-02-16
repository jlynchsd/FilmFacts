package com.movietrivia.filmfacts.model

import com.movietrivia.filmfacts.api.*
import java.util.*
import javax.inject.Inject

class TmdbDataSource @Inject constructor(
    private val discoverService: DiscoverService,
    private val configurationService: ConfigurationService,
    private val personService: PersonService,
    private val movieService: MovieService,
    private val tvShowService: TvShowService,
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

    suspend fun getTvShowCredits(showId: Int) = makeNetworkCall(tooManyRequestsDataSource) {
        tvShowService.getTvShowCredits(showId, TvShowService.options).body()
    }

    suspend fun getTvShowDetails(showId: Int) = makeNetworkCall(tooManyRequestsDataSource) {
        tvShowService.getTvShowDetails(showId, TvShowService.options).body()
    }

    suspend fun getTvShowImages(showId: Int) = makeNetworkCall(tooManyRequestsDataSource) {
        tvShowService.getTvShowImages(showId, TvShowService.options).body()
    }

    suspend fun getActorDetails(actorId: Int) = makeNetworkCall(tooManyRequestsDataSource) {
        personService.getActorDetails(actorId, PersonService.Builder().build()).body()
    }

    suspend fun getActorMovieCredits(actorId: Int) = makeNetworkCall(tooManyRequestsDataSource) {
        personService.getActorMovieCredits(actorId, PersonService.Builder().build()).body()?.credits
    }

    suspend fun getActorTvShowCredits(actorId: Int) = makeNetworkCall(tooManyRequestsDataSource) {
        personService.getActorTvShowCredits(actorId, PersonService.Builder().build()).body()?.credits
    }

    suspend fun getMovies(
        userSettings: UserSettings,
        dateRange: Pair<Date?, Date?>?,
        movieOrder: DiscoverService.Builder.MovieOrder?,
        releaseType: DiscoverService.Builder.ReleaseType?,
        includeGenres: List<Int>?,
        excludeGenres: List<Int>?,
        cast: List<Int>?,
        minimumVotes: Int?,
        page: Int
    ):  DiscoverMovieResponse? {
        val builder = DiscoverService.Builder(userSettings, page)
        dateRange?.let {
            builder.movieReleasedInRange(it.first, it.second)
        }
        movieOrder?.let {
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
        builder.withCertification("US", "G", "R").withAdultContent(false)
        return makeNetworkCall(tooManyRequestsDataSource) {
            discoverService.getMovies(builder.build()).body()?.let { response ->
                response.copy(
                    results = response.results.distinctBy { it.title }.filter { it.posterPath.isNotEmpty() && it.genreIds.isNotEmpty() && it.releaseDate.isNotEmpty()}
                )
            }
        }
    }

    suspend fun getTvShows(
        userSettings: UserSettings,
        dateRange: Pair<Date?, Date?>?,
        tvShowOrder: DiscoverService.Builder.TvShowOrder?,
        includeGenres: List<Int>?,
        excludeGenres: List<Int>?,
        minimumVotes: Int?,
        page: Int
    ):  DiscoverTvShowResponse? {
        val builder = DiscoverService.Builder(userSettings, page)
        dateRange?.let {
            builder.tvShowReleasedInRange(it.first, it.second)
        }
        tvShowOrder?.let {
            builder.orderBy(it)
        }
        includeGenres?.let {
            builder.withGenres(*it.toIntArray())
        }
        excludeGenres?.let {
            builder.withoutGenres(*it.toIntArray())
        }
        minimumVotes?.let {
            builder.withVotesGreaterThan(it)
        }
        builder.withCertification("US", "TV-Y", "TV-14").withAdultContent(false)
        return makeNetworkCall(tooManyRequestsDataSource) {
            discoverService.getTvShows(builder.build()).body()?.let { response ->
                response.copy(
                    results = response.results.distinctBy { it.name }.filter { it.posterPath.isNotEmpty() && it.genreIds.isNotEmpty() && it.firstAirDate.isNotEmpty()}
                )
            }
        }
    }
}
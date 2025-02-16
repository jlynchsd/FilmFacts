package com.movietrivia.filmfacts.api

import android.annotation.SuppressLint
import androidx.annotation.VisibleForTesting
import com.movietrivia.filmfacts.model.UserSettings
import com.squareup.moshi.Json
import retrofit2.Response
import retrofit2.http.*
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.HashMap

interface DiscoverService {
    @GET("discover/movie")
    suspend fun getMovies(@QueryMap options: Map<String, String>): Response<DiscoverMovieResponse>

    @GET("discover/tv")
    suspend fun getTvShows(@QueryMap options: Map<String, String>): Response<DiscoverTvShowResponse>

    class Builder(settings: UserSettings, page: Int) {
        private val options = HashMap<String, String>().apply {
            put(LANGUAGE, settings.language)
            put(PAGE_KEY, page.toString())
        }

        @SuppressLint("SimpleDateFormat")
        private val formatter = SimpleDateFormat("yyyy-MM-dd")

        init {
            if (settings.excludedGenres.isNotEmpty()) {
                withoutGenres(*settings.excludedGenres.toIntArray())
            }
            settings.releasedAfterOffset?.let {
                options[PRIMARY_RELEASE_DATE_LESS_THAN] = formatter.format(getOffsetDate(it))
                options[FIRST_AIR_DATE_LESS_THAN] = formatter.format(getOffsetDate(it))
            }
            settings.releasedBeforeOffset?.let {
                options[PRIMARY_RELEASE_DATE_GREATER_THAN] = formatter.format(getOffsetDate(it))
                options[FIRST_AIR_DATE_GREATER_THAN] = formatter.format(getOffsetDate(it))
            }
        }

        fun movieReleasedInRange(startDate: Date?, endDate: Date?): Builder {
            options.apply {
                startDate?.let {
                    put(PRIMARY_RELEASE_DATE_GREATER_THAN, formatter.format(it))
                }
                endDate?.let {
                    put(PRIMARY_RELEASE_DATE_LESS_THAN, formatter.format(it))
                }
            }

            return this
        }

        fun tvShowReleasedInRange(startDate: Date?, endDate: Date?): Builder {
            options.apply {
                startDate?.let {
                    put(FIRST_AIR_DATE_GREATER_THAN, formatter.format(it))
                }
                endDate?.let {
                    put(FIRST_AIR_DATE_LESS_THAN, formatter.format(it))
                }
            }

            return this
        }

        fun orderBy(movieOrder: MovieOrder): Builder {
            options.apply {
                put(SORT_BY, movieOrder.key)
            }

            return this
        }

        fun orderBy(tvShowOrder: TvShowOrder): Builder {
            options.apply {
                put(SORT_BY, tvShowOrder.key)
            }

            return this
        }

        fun withGenres(vararg genre: Int): Builder {
            options.apply {
                put(INCLUDE_GENRES, genre.joinToString(","))
            }

            return this
        }

        fun withoutGenres(vararg genre: Int): Builder {
            options.apply {
                put(EXCLUDE_GENRES, genre.joinToString(","))
            }

            return this
        }

        fun withCast(vararg cast: Int): Builder {
            options.apply {
                put(WITH_CAST, cast.joinToString(","))
            }

            return this
        }

        fun withVotesGreaterThan(count: Int): Builder {
            options.apply {
                put(VOTE_COUNT_GTE, count.toString())
            }

            return this
        }

        fun withReleaseType(type: ReleaseType): Builder {
            options.apply {
                put(WITH_RELEASE_TYPE, type.key.toString())
            }

            return this
        }

        fun withCertification(certificationCountry: String, minimumCertification: String? = null, maximumCertification: String? = null): Builder {
            options.apply {
                options[CERTIFICATION_COUNTRY] = certificationCountry
                minimumCertification?.let {
                    options[CERTIFICATION_GTE] = it
                }
                maximumCertification?.let {
                    options[CERTIFICATION_LTE] = it
                }
            }

            return this
        }

        fun withAdultContent(enabled: Boolean) {
            options[INCLUDE_ADULT] = enabled.toString()
        }

        fun build() = options

        private fun getOffsetDate(yearOffset: Int) =
            with(Calendar.getInstance()) {
                add(Calendar.YEAR, -yearOffset)
                time
            }

        enum class MovieOrder(val key: String) {
            POPULARITY_ASC("popularity.asc"),
            POPULARITY_DESC("popularity.desc"),
            REVENUE_ASC("revenue.asc"),
            REVENUE_DESC("revenue.desc"),
            VOTE_AVERAGE_ASC("vote_average.asc"),
            VOTE_AVERAGE_DESC("vote_average.desc"),
            VOTE_COUNT_ASC("vote_count.asc"),
            VOTE_COUNT_DESC("vote_count.desc"),
            RELEASE_DATE_ASC("release_date.asc"),
            RELEASE_DATE_DESC("release_date.desc")
        }

        enum class TvShowOrder(val key: String) {
            POPULARITY_ASC("popularity.asc"),
            POPULARITY_DESC("popularity.desc"),
            VOTE_AVERAGE_ASC("vote_average.asc"),
            VOTE_AVERAGE_DESC("vote_average.desc"),
            VOTE_COUNT_ASC("vote_count.asc"),
            VOTE_COUNT_DESC("vote_count.desc"),
            FIRST_AIR_DATE_ASC("first_air_date.asc"),
            FIRST_AIR_DATE_DESC("first_air_date.desc")
        }

        enum class ReleaseType(val key: Int) {
            PREMIERE(1),
            LIMITED_THEATRICAL(2),
            THEATRICAL(3),
            DIGITAL(4),
            PHYSICAL(5),
            TV(6)
        }

        @VisibleForTesting
        internal companion object {
            @VisibleForTesting
            const val PRIMARY_RELEASE_DATE_GREATER_THAN = "primary_release_date.gte"
            @VisibleForTesting
            const val PRIMARY_RELEASE_DATE_LESS_THAN = "primary_release_date.lte"

            @VisibleForTesting
            const val FIRST_AIR_DATE_GREATER_THAN = "first_air_date.gte"
            @VisibleForTesting
            const val FIRST_AIR_DATE_LESS_THAN = "first_air_date.lte"

            @VisibleForTesting
            const val LANGUAGE = "with_original_language"
            @VisibleForTesting
            const val INCLUDE_GENRES = "with_genres"
            @VisibleForTesting
            const val EXCLUDE_GENRES = "without_genres"
            @VisibleForTesting
            const val WITH_CAST = "with_cast"
            @VisibleForTesting
            const val SORT_BY = "sort_by"
            @VisibleForTesting
            const val VOTE_COUNT_GTE = "vote_count.gte"
            @VisibleForTesting
            const val WITH_RELEASE_TYPE = "with_release_type"
            @VisibleForTesting
            const val PAGE_KEY = "page"

            @VisibleForTesting
            const val CERTIFICATION_COUNTRY = "certification_country"
            @VisibleForTesting
            const val CERTIFICATION_LTE = "certification.lte"
            @VisibleForTesting
            const val CERTIFICATION_GTE = "certification.gte"
            @VisibleForTesting
            const val INCLUDE_ADULT = "include_adult"
        }
    }
}

interface ConfigurationService {
    @GET("configuration")
    suspend fun getConfiguration(@QueryMap options: Map<String, String>): Response<ConfigurationResponse>

    companion object {
        val options = HashMap<String, String>()
    }
}

interface PersonService {
    @GET("person/{person_id}")
    suspend fun getActorDetails(@Path(value = "person_id") personId: Int, @QueryMap options: Map<String, String>): Response<PersonDetails>

    @GET("person/{person_id}/movie_credits")
    suspend fun getActorMovieCredits(@Path(value = "person_id") personId: Int, @QueryMap options: Map<String, String>): Response<ActorMovieCreditsResponse>

    @GET("person/{person_id}/tv_credits")
    suspend fun getActorTvShowCredits(@Path(value = "person_id") personId: Int, @QueryMap options: Map<String, String>): Response<ActorTvShowCreditsResponse>

    class Builder {
        private val options = HashMap<String, String>()

        fun page(page: Int): Builder {
            options.apply {
                put(PAGE_KEY, page.toString())
            }

            return this
        }

        fun build() = options

        @VisibleForTesting
        internal companion object {
            @VisibleForTesting
            const val PAGE_KEY = "page"
        }
    }
}

interface MovieService {
    @GET("movie/{movie_id}/credits")
    suspend fun getMovieCredits(@Path(value = "movie_id") movieId: Int, @QueryMap options: Map<String, String>): Response<MovieCredits>

    @GET("movie/{movie_id}")
    suspend fun getMovieDetails(@Path(value = "movie_id") movieId: Int, @QueryMap options: Map<String, String>): Response<MovieDetails>

    @GET("movie/{movie_id}/images")
    suspend fun getMovieImages(@Path(value = "movie_id") movieId: Int, @QueryMap options: Map<String, String>): Response<MovieImageResponse>

    companion object {
        val options = HashMap<String, String>()
    }
}

interface TvShowService {
    @GET("tv/{show_id}/credits")
    suspend fun getTvShowCredits(@Path(value = "show_id") showId: Int, @QueryMap options: Map<String, String>): Response<TvShowCredits>

    @GET("tv/{show_id}")
    suspend fun getTvShowDetails(@Path(value = "show_id") showId: Int, @QueryMap options: Map<String, String>): Response<TvShowDetails>

    @GET("tv/{show_id}/images")
    suspend fun getTvShowImages(@Path(value = "show_id") showId: Int, @QueryMap options: Map<String, String>): Response<TvShowImageResponse>

    companion object {
        val options = HashMap<String, String>()
    }
}

interface AuthenticationService {
    @GET("authentication/token/new")
    suspend fun getNewAuthenticationToken(@QueryMap options: Map<String, String>): Response<AuthenticationTokenResponse>

    @POST("authentication/session/new")
    suspend fun createSession(@Body body: NewSessionBody, @QueryMap options: Map<String, String>): Response<NewSessionResponse>

    @HTTP(method = "DELETE", path = "authentication/session", hasBody = true)
    suspend fun deleteSession(@Body body: DeleteSessionBody, @QueryMap options: Map<String, String>): Response<DeleteSessionResponse>

    companion object {
        val options = HashMap<String, String>()
    }
}

interface AccountService {
    @GET("account")
    suspend fun getAccountDetails(@QueryMap options: Map<String, String>): Response<AccountDetailsResponse>

    @GET("account/{account_id}/favorite/movies")
    suspend fun getAccountFavoriteMovies(@Path(value = "account_id") accountId: Int, @QueryMap options: Map<String, String>): Response<AccountMoviesResponse>

    @GET("account/{account_id}/rated/movies")
    suspend fun getAccountRatedMovies(@Path(value = "account_id") accountId: Int, @QueryMap options: Map<String, String>): Response<AccountRatedMoviesResponse>

    @GET("account/{account_id}/watchlist/movies")
    suspend fun getAccountWatchlistMovies(@Path(value = "account_id") accountId: Int, @QueryMap options: Map<String, String>): Response<AccountMoviesResponse>

    @GET("account/{account_id}/favorite/tv")
    suspend fun getAccountFavoriteTvShows(@Path(value = "account_id") accountId: Int, @QueryMap options: Map<String, String>): Response<AccountTvShowsResponse>

    @GET("account/{account_id}/rated/tv")
    suspend fun getAccountRatedTvShows(@Path(value = "account_id") accountId: Int, @QueryMap options: Map<String, String>): Response<AccountRatedTvShowsResponse>

    @GET("account/{account_id}/watchlist/tv")
    suspend fun getAccountWatchlistTvShows(@Path(value = "account_id") accountId: Int, @QueryMap options: Map<String, String>): Response<AccountTvShowsResponse>

    class Builder {
        private val options = HashMap<String, String>()

        fun sessionId(sessionId: String): Builder {
            options.apply {
                put(SESSION_ID_KEY, sessionId)
            }

            return this
        }

        fun page(page: Int): Builder {
            options.apply {
                put(PAGE_KEY, page.toString())
            }

            return this
        }

        fun build() = options

        @VisibleForTesting
        internal companion object {
            @VisibleForTesting
            const val SESSION_ID_KEY = "session_id"
            @VisibleForTesting
            const val PAGE_KEY = "page"
        }
    }
}

data class DiscoverMovie(
    @Json(name = "id") val id: Int,
    @Json(name = "title") val title: String,
    @Json(name = "poster_path") @NullableString val posterPath: String,
    @Json(name = "genre_ids") val genreIds: List<Int>,
    @Json(name = "release_date") @NullableString val releaseDate: String,
    @Json(name = "original_language") val originalLanguage: String,
    @Json(name = "popularity") val popularity: Float,
    @Json(name = "vote_count") val voteCount: Int,
    @Json(name = "vote_average") val voteAverage: Float
)

data class DiscoverMovieResponse(
    @Json(name = "page") val page: Int,
    @Json(name = "results") val results: List<DiscoverMovie>,
    @Json(name = "total_pages") val totalPageCount: Int,
    @Json(name = "total_results") val totalResultCount: Int
)

data class ConfigurationResponse(
    @Json(name = "images") val images: ImageConfiguration,
    @Json(name = "change_keys") val changeKeys: List<String>
)

data class ImageConfiguration(
    @Json(name = "base_url") val baseUrl: String,
    @Json(name = "secure_base_url") val secureBaseUrl: String,
    @Json(name = "backdrop_sizes") val backdropSizes: List<String>,
    @Json(name = "logo_sizes") val logoSizes: List<String>,
    @Json(name = "poster_sizes") val posterSizes: List<String>,
    @Json(name = "profile_sizes") val profileSizes: List<String>,
    @Json(name = "still_sizes") val stillSizes: List<String>
)

data class MovieCredits(
    @Json(name = "id") val id: Int,
    @Json(name = "cast") val cast: List<MovieCreditEntry>
)

data class MovieCreditEntry(
    @Json(name = "id") val id: Int,
    @Json(name = "name") val name: String,
    @Json(name = "gender") val gender: Int?,
    @Json(name = "order") val order: Int,
    @Json(name = "character") val characterName: String
)

data class TvShowCredits(
    @Json(name = "id") val id: Int,
    @Json(name = "cast") val cast: List<TvShowCreditEntry>
)

data class TvShowCreditEntry(
    @Json(name = "id") val id: Int,
    @Json(name = "name") val name: String,
    @Json(name = "gender") val gender: Int?,
    @Json(name = "order") val order: Int,
    @Json(name = "character") val characterName: String
)

data class MovieDetails(
    @Json(name = "id") val id: Int,
    @Json(name = "title") val title: String,
    @Json(name = "budget") val budget: Int,
    @Json(name = "revenue") val revenue: Long,
    @Json(name = "vote_average") val voteAverage: Float,
    @Json(name = "poster_path") @NullableString val posterPath: String
)

data class TvShowDetails(
    @Json(name = "id") val id: Int,
    @Json(name = "name") val name: String,
    @Json(name = "first_air_date") val firstAirDate: String?,
    @Json(name = "number_of_episodes") val numberOfEpisodes: Int,
    @Json(name = "number_of_seasons") val numberOfSeasons: Int,
    @Json(name = "seasons") val seasons: List<TvShowSeasonDetails>,
    @Json(name = "vote_average") val voteAverage: Float,
    @Json(name = "vote_count") val voteCount: Int,
    @Json(name = "poster_path") @NullableString val posterPath: String
)

data class TvShowSeasonDetails(
    @Json(name = "id") val id: Int,
    @Json(name = "name") val name: String,
    @Json(name = "air_date") val airDate: String?,
    @Json(name = "episode_count") val episodeCount: Int,
    @Json(name = "poster_path") @NullableString val posterPath: String,
    @Json(name = "season_number") val seasonNumber: Int,
    @Json(name = "vote_average") val voteAverage: Float
)

data class DiscoverTvShow(
    @Json(name = "id") val id: Int,
    @Json(name = "name") val name: String,
    @Json(name = "first_air_date") @NullableString val firstAirDate: String,
    @Json(name = "genre_ids") val genreIds: List<Int>,
    @Json(name = "origin_country") val originCountry: List<String>,
    @Json(name = "original_language") val originalLanguage: String,
    @Json(name = "original_name") val originalName: String,
    @Json(name = "overview") val overview: String,
    @Json(name = "popularity") val popularity: Float,
    @Json(name = "poster_path") @NullableString val posterPath: String,
    @Json(name = "vote_count") val voteCount: Int,
    @Json(name = "vote_average") val voteAverage: Float
)

data class DiscoverTvShowResponse(
    @Json(name = "page") val page: Int,
    @Json(name = "results") val results: List<DiscoverTvShow>,
    @Json(name = "total_pages") val totalPageCount: Int,
    @Json(name = "total_results") val totalResultCount: Int
)

data class PersonDetails(
    @Json(name = "id") val id: Int,
    @Json(name = "name") val name: String,
    @Json(name = "gender") val gender: Int,
    @Json(name = "profile_path") @NullableString val profilePath: String
)

data class ActorMovieCreditsResponse(
    @Json(name = "cast") val credits: List<ActorMovieCredits>
)

data class ActorMovieCredits(
    @Json(name = "id") val id: Int,
    @Json(name = "character") val characterName: String,
    @Json(name = "title") val movieTitle: String,
    @Json(name = "genre_ids") val genreIds: List<Int>,
    @Json(name = "order") val order: Int,
    @Json(name = "vote_count") val voteCount: Int,
    @Json(name = "release_date") @NullableString val releaseDate: String,
    @Json(name= "original_language") val originalLanguage: String
)

data class ActorTvShowCreditsResponse(
    @Json(name = "cast") val credits: List<ActorTvShowCredits>
)

data class ActorTvShowCredits(
    @Json(name = "id") val id: Int,
    @Json(name = "character") val characterName: String,
    @Json(name = "name") val showName: String,
    @Json(name = "genre_ids") val genreIds: List<Int>,
    @Json(name = "vote_count") val voteCount: Int,
    @Json(name = "episode_count") val episodeCount: Int,
    @Json(name = "first_air_date") @NullableString val firstAirDate: String,
    @Json(name = "poster_path") @NullableString val posterPath: String,
    @Json(name= "original_language") val originalLanguage: String
)

data class MovieImageResponse(
    @Json(name = "id") val id: Int,
    @Json(name = "backdrops") val backdrops: List<MovieImage>,
)

data class MovieImage(
    @Json(name = "iso_639_1") val language: String?,
    @Json(name = "file_path") val filePath: String,
    @Json(name = "vote_count") val voteCount: Int
)

data class TvShowImageResponse(
    @Json(name = "id") val id: Int,
    @Json(name = "backdrops") val backdrops: List<TvShowImage>,
)

data class TvShowImage(
    @Json(name = "iso_639_1") val language: String?,
    @Json(name = "file_path") val filePath: String,
    @Json(name = "vote_count") val voteCount: Int
)

data class AuthenticationTokenResponse(
    @Json(name = "success") val success: Boolean,
    @Json(name = "expires_at") val expires: String,
    @Json(name = "request_token") val requestToken: String,
)

data class NewSessionBody(
    @Json(name = "request_token") val requestToken: String
)

data class NewSessionResponse(
    @Json(name = "success") val success: Boolean,
    @Json(name = "session_id") val sessionId: String
)

data class DeleteSessionBody(
    @Json(name = "session_id") val sessionId: String
)

data class DeleteSessionResponse(
    @Json(name = "success") val success: Boolean
)

data class AccountDetailsResponse(
    @Json(name = "id") val id: Int,
    @Json(name = "name") val name: String,
    @Json(name = "username") val userName: String,
    @Json(name = "avatar") val avatar: AccountAvatar
)

data class AccountAvatar(
    @Json(name = "gravatar") val gravatar: Gravatar
)

data class Gravatar(
    @Json(name = "hash") val hash: String
)

data class AccountMoviesResponse(
    @Json(name = "page") val page: Int,
    @Json(name = "results") val results: List<DiscoverMovie>,
    @Json(name = "total_pages") val totalPageCount: Int,
    @Json(name = "total_results") val totalResultCount: Int
)

data class AccountRatedMoviesResponse(
    @Json(name = "page") val page: Int,
    @Json(name = "results") val results: List<RatedMovie>,
    @Json(name = "total_pages") val totalPageCount: Int,
    @Json(name = "total_results") val totalResultCount: Int
)

data class RatedMovie(
    @Json(name = "id") val id: Int,
    @Json(name = "title") val title: String,
    @Json(name = "poster_path") @NullableString val posterPath: String,
    @Json(name = "genre_ids") val genreIds: List<Int>,
    @Json(name = "release_date") @NullableString val releaseDate: String,
    @Json(name = "original_language") val originalLanguage: String,
    @Json(name = "popularity") val popularity: Float,
    @Json(name = "vote_count") val voteCount: Int,
    @Json(name = "vote_average") val voteAverage: Float,
    @Json(name = "rating") val rating: Int
)

data class AccountTvShowsResponse(
    @Json(name = "page") val page: Int,
    @Json(name = "results") val results: List<DiscoverTvShow>,
    @Json(name = "total_pages") val totalPageCount: Int,
    @Json(name = "total_results") val totalResultCount: Int
)

data class AccountRatedTvShowsResponse(
    @Json(name = "page") val page: Int,
    @Json(name = "results") val results: List<RatedTvShow>,
    @Json(name = "total_pages") val totalPageCount: Int,
    @Json(name = "total_results") val totalResultCount: Int
)

data class RatedTvShow(
    @Json(name = "id") val id: Int,
    @Json(name = "name") val name: String,
    @Json(name = "poster_path") @NullableString val posterPath: String,
    @Json(name = "genre_ids") val genreIds: List<Int>,
    @Json(name = "first_air_date") @NullableString val firstAirDate: String,
    @Json(name = "origin_country") val originCountry: List<String>,
    @Json(name = "original_language") val originalLanguage: String,
    @Json(name = "original_name") val originalName: String,
    @Json(name = "overview") val overview: String,
    @Json(name = "popularity") val popularity: Float,
    @Json(name = "vote_count") val voteCount: Int,
    @Json(name = "vote_average") val voteAverage: Float,
    @Json(name = "rating") val rating: Int
)

enum class MovieGenre(val key: Int) {
    ACTION(28),
    ADVENTURE(12),
    ANIMATION(16),
    COMEDY(35),
    CRIME(80),
    DOCUMENTARY(99),
    DRAMA(18),
    FAMILY(10751),
    FANTASY(14),
    HISTORY(36),
    HORROR(27),
    MUSIC(10402),
    MYSTERY(9648),
    ROMANCE(10749),
    SCI_FI(878),
    THRILLER(53),
    WAR(10752),
    WESTERN(37)
}

enum class TvGenre(val key: Int) {
    ACTION_AND_ADVENTURE(10759),
    ANIMATION(16),
    COMEDY(35),
    CRIME(80),
    DOCUMENTARY(99),
    DRAMA(18),
    FAMILY(10751),
    KIDS(10762),
    MYSTERY(9648),
    NEWS(10763),
    REALITY(10764),
    SCI_FI_AND_FANTASY(10765),
    SOAP(10766),
    TALK(10767),
    WAR_AND_POLITICS(10768),
    WESTERN(37)
}
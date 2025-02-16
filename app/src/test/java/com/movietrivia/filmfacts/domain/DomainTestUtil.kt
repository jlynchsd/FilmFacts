package com.movietrivia.filmfacts.domain

import com.movietrivia.filmfacts.api.ActorMovieCredits
import com.movietrivia.filmfacts.api.ActorTvShowCredits
import com.movietrivia.filmfacts.api.DiscoverTvShow
import com.movietrivia.filmfacts.api.MovieGenre
import com.movietrivia.filmfacts.api.PersonDetails
import com.movietrivia.filmfacts.api.TvShowDetails
import com.movietrivia.filmfacts.api.TvShowSeasonDetails
import com.movietrivia.filmfacts.model.Actor
import com.movietrivia.filmfacts.model.FilmFactsRepository
import com.movietrivia.filmfacts.model.RecentPromptsRepository
import com.movietrivia.filmfacts.model.UserSettings
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import kotlinx.coroutines.flow.MutableSharedFlow

fun stubForMovieCreditFilter(
    userSettingsFlow: MutableSharedFlow<UserSettings>,
    recentPromptsRepository: RecentPromptsRepository,
    filmFactsRepository: FilmFactsRepository,
    settings: UserSettings = UserSettings(),
    dateWithinRange: Boolean = true
) {
    unmockkStatic(::getActorMovieCredits)
    userSettingsFlow.tryEmit(settings)

    every {
        recentPromptsRepository.isRecentActor(any())
    } returns false

    coEvery {
        filmFactsRepository.getActorDetails(any())
    } returns PersonDetails(0, "", 0, "foo")

    mockkStatic(::getMovieActors)
    coEvery {
        getMovieActors(any(), any(), any())
    } returns listOf(Actor(0, "foo", 0), Actor(1, "foo", 0))

    every {
        dateWithinRange(any(), any(), any())
    } returns dateWithinRange
}

fun stubForTvShowCreditFilter(
    userSettingsFlow: MutableSharedFlow<UserSettings>,
    recentPromptsRepository: RecentPromptsRepository,
    filmFactsRepository: FilmFactsRepository,
    settings: UserSettings = UserSettings(),
    dateWithinRange: Boolean = true
) {
    unmockkStatic(::getActorTvShowCredits)
    userSettingsFlow.tryEmit(settings)

    every {
        recentPromptsRepository.isRecentActor(any())
    } returns false

    coEvery {
        filmFactsRepository.getActorDetails(any())
    } returns PersonDetails(0, "", 0, "foo")

    mockkStatic(::getTvShowActors)
    coEvery {
        getTvShowActors(any(), any(), any(), any(), logTag = any())
    } returns listOf(Actor(0, "foo", 0), Actor(1, "foo", 0))

    coEvery {
        getTvShowActors(any(), any(), any(), any(), filter = any(), logTag = any())
    } returns listOf(Actor(0, "foo", 0), Actor(1, "foo", 0))

    mockkStatic(::dateWithinRange)
    every {
        dateWithinRange(any(), any(), any())
    } returns dateWithinRange
}

object CreditFilterConstants {
    const val MIN_VOTE_COUNT = 21
    const val MIN_ORDER = 5
    const val MIN_EPISODES = 10
    val LANGUAGE = UserSettings().language
}

fun stubActorCredits(
    movieTitle: String,
    characterName: String? = null,
    id: Int = 0,
    genres: List<Int> = listOf(0, MovieGenre.ANIMATION.key),
    order: Int = CreditFilterConstants.MIN_ORDER,
    votes: Int = CreditFilterConstants.MIN_VOTE_COUNT,
    releaseDate: String = "",
    language: String = CreditFilterConstants.LANGUAGE
) = ActorMovieCredits(id, characterName ?: movieTitle, movieTitle, genres, order, votes, releaseDate, language)

fun stubTvShowDetails(
    id: Int = 0,
    name: String = "fooShow",
    firstAirDate: String = "2000-01-01",
    numberOfEpisodes: Int = 0,
    numberOfSeasons: Int = 0,
    seasons: List<TvShowSeasonDetails> = listOf(),
    voteAverage: Float = 0f,
    voteCount: Int = 0,
    posterPath: String = "fooPath",
) = TvShowDetails(
    id,
    name,
    firstAirDate,
    numberOfEpisodes,
    numberOfSeasons,
    seasons,
    voteAverage,
    voteCount,
    posterPath
)

fun stubTvShowSeasonDetails(
    id: Int = 0,
    name: String = "",
    airDate: String? = null,
    episodeCount: Int = 0,
    posterPath: String = "fooPath",
    seasonNumber: Int = 0,
    voteAverage: Float = 0f
) = TvShowSeasonDetails(
    id, name, airDate, episodeCount, posterPath, seasonNumber, voteAverage
)

fun stubActorTvShowCredits(
    id: Int = 0,
    characterName: String = "fooCharacter",
    showName: String = "fooShow",
    genres: List<Int> = listOf(0, MovieGenre.ANIMATION.key),
    voteCount: Int = 30,
    episodeCount: Int = 10,
    firstAirDate: String = "2000-01-01",
    posterPath: String = "fooPath",
    language: String = UserSettings().language
) = ActorTvShowCredits(
    id,
    characterName,
    showName,
    genres,
    voteCount,
    episodeCount,
    firstAirDate,
    posterPath,
    language
)

fun stubDiscoverTvShow(
    id: Int = 0,
    name: String = "",
    firstAirDate: String = "2000-01-01",
    genreIds: List<Int> = emptyList(),
    originCountry: List<String> = emptyList(),
    originalLanguage: String = "en",
    originalName: String = "",
    overview: String = "",
    popularity: Float = 0f,
    posterPath: String = "fooPath",
    voteCount: Int = 0,
    voteAverage: Float = 0f
) = DiscoverTvShow(
    id,
    name,
    firstAirDate,
    genreIds,
    originCountry,
    originalLanguage,
    originalName,
    overview,
    popularity,
    posterPath,
    voteCount,
    voteAverage
)

val stubActorTvShowCredits = listOf(
    listOf(
        stubActorTvShowCredits(characterName = "other1", showName = "other1", episodeCount = 10)
    ),
    listOf(
        stubActorTvShowCredits(characterName = "other2", showName = "other2", episodeCount = 11)
    ),
    listOf(
        stubActorTvShowCredits(characterName = "other3", showName = "other3", episodeCount = 12)
    ),
    listOf(
        stubActorTvShowCredits(characterName = "other4", showName = "other4", episodeCount = 13)
    )
)
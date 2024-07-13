package com.interviewsample.filmfacts.domain

import com.movietrivia.filmfacts.api.ActorCredits
import com.movietrivia.filmfacts.api.MovieGenre
import com.movietrivia.filmfacts.api.PersonDetails
import com.movietrivia.filmfacts.domain.getActorCredits
import com.movietrivia.filmfacts.domain.getActors
import com.movietrivia.filmfacts.model.Actor
import com.movietrivia.filmfacts.model.FilmFactsRepository
import com.movietrivia.filmfacts.model.RecentPromptsRepository
import com.movietrivia.filmfacts.model.UserSettings
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import kotlinx.coroutines.flow.MutableSharedFlow

fun stubForCreditFilter(
    userSettingsFlow: MutableSharedFlow<UserSettings>,
    recentPromptsRepository: RecentPromptsRepository,
    filmFactsRepository: FilmFactsRepository,
    settings: UserSettings = UserSettings(),
    dateWithinRange: Boolean = true
) {
    unmockkStatic(::getActorCredits)
    userSettingsFlow.tryEmit(settings)

    every {
        recentPromptsRepository.isRecentActor(any())
    } returns false

    coEvery {
        filmFactsRepository.getActorDetails(any())
    } returns PersonDetails(0, "", 0, "foo")

    mockkStatic(::getActors)
    coEvery {
        getActors(any(), any(), any())
    } returns listOf(Actor(0, "foo", 0), Actor(1, "foo", 0))

    every {
        com.movietrivia.filmfacts.domain.dateWithinRange(any(), any(), any())
    } returns dateWithinRange
}

object CreditFilterConstants {
    const val MIN_VOTE_COUNT = 21
    const val MIN_ORDER = 5
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
) =
    ActorCredits(id, characterName ?: movieTitle, movieTitle, genres, order, votes, releaseDate, language)
package com.interviewsample.filmfacts.domain

import com.movietrivia.filmfacts.api.DiscoverMovieResponse
import com.movietrivia.filmfacts.api.MovieDetails
import com.movietrivia.filmfacts.domain.GetScoredMoviesStarringActorUseCase
import com.movietrivia.filmfacts.domain.formatRevenue
import com.movietrivia.filmfacts.domain.getActors
import com.movietrivia.filmfacts.domain.preloadImages
import com.movietrivia.filmfacts.model.Actor
import com.movietrivia.filmfacts.model.DiscoverMovie
import com.movietrivia.filmfacts.model.FilmFactsRepository
import com.movietrivia.filmfacts.model.RecentPromptsRepository
import com.movietrivia.filmfacts.model.UiImagePrompt
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test

class GetScoredMoviesStarringActorUseCaseTest {

    private lateinit var filmFactsRepository: FilmFactsRepository
    private lateinit var recentPromptsRepository: RecentPromptsRepository

    @Before
    fun setup() {
        filmFactsRepository = mockk(relaxed = true)
        recentPromptsRepository = mockk(relaxed = true)

        mockkStatic(::getActors)
        coEvery {
            getActors(any(), any(), any())
        } returns listOf(
            Actor(0, "foo", null),
            Actor(1, "bar", null),
            Actor(2, "fizz", null),
            Actor(3, "buzz", null)
        )

        coEvery {
            filmFactsRepository.getMovies(cast = any(), order = any(), includeGenres = any())
        } returns stubDiscoverMovieResponse

        coEvery {
            filmFactsRepository.getMovieDetails(any())
        } returns MovieDetails(0, "foo", 100, 2000, 5f, "")

        coEvery {
            filmFactsRepository.getImageUrl(any(), any())
        } returns "fooUrl"

        mockkStatic(::formatRevenue)
        every {
            formatRevenue(any(), any())
        } returns ""

        mockkStatic(::preloadImages)
        coEvery {
            preloadImages(any(), *anyVararg())
        } returns true
    }

    @After
    fun teardown() {
        clearAllMocks()
    }

    @Test
    fun `When unable to get actors returns null`() = runTest {
        val useCase = getUseCase(testScheduler)
        coEvery {
            getActors(any(), any(), any())
        } returns emptyList()

        Assert.assertNull(useCase.invoke(null))
    }

    @Test
    fun `When unable to get movies returns null`() = runTest {
        val useCase = getUseCase(testScheduler)
        coEvery {
            filmFactsRepository.getMovies(cast = any(), order = any(), includeGenres = any())
        } returns null

        Assert.assertNull(useCase.invoke(null))
    }

    @Test
    fun `When unable to get enough movies returns null`() = runTest {
        val useCase = getUseCase(testScheduler)
        coEvery {
            filmFactsRepository.getMovies(cast = any(), order = any(), includeGenres = any())
        } returns stubDiscoverMovieResponse.copy(results = listOf(stubDiscoverMovieResponse.results.first()))

        Assert.assertNull(useCase.invoke(null))
    }

    @Test
    fun `When strategy is HIGHEST_GROSSING and movies have negative gross returns null`() = runTest {
        val useCase = getUseCase(testScheduler)
        coEvery {
            filmFactsRepository.getMovieDetails(any())
        } returns MovieDetails(0, "foo", 0, 0, 0f, "")

        Assert.assertNull(useCase.getPrompt(null, GetScoredMoviesStarringActorUseCase.ScoreStrategies.HIGHEST_GROSSING))
    }

    @Test
    fun `When strategy is HIGHEST_GROSSING and movies have positive gross returns prompt`() = runTest {
        val useCase = getUseCase(testScheduler)
        coEvery {
            filmFactsRepository.getMovieDetails(any())
        } returns MovieDetails(0, "foo", 0, 42, 0f, "")

        Assert.assertTrue(
            useCase.getPrompt(
                null,
                GetScoredMoviesStarringActorUseCase.ScoreStrategies.HIGHEST_GROSSING
            ) is UiImagePrompt
        )
    }

    @Test
    fun `When strategy is WORST_RATED and movies have negative gross returns prompt`() = runTest {
        val useCase = getUseCase(testScheduler)
        coEvery {
            filmFactsRepository.getMovieDetails(any())
        } returns MovieDetails(0, "foo", 0, 0, 0f, "")

        Assert.assertTrue(
            useCase.getPrompt(
                null,
                GetScoredMoviesStarringActorUseCase.ScoreStrategies.WORST_RATED
            ) is UiImagePrompt
        )
    }

    @Test
    fun `When strategy is TOP_RATED and movies have negative gross returns prompt`() = runTest {
        val useCase = getUseCase(testScheduler)
        coEvery {
            filmFactsRepository.getMovieDetails(any())
        } returns MovieDetails(0, "foo", 0, 0, 0f, "")

        Assert.assertTrue(
            useCase.getPrompt(
                null,
                GetScoredMoviesStarringActorUseCase.ScoreStrategies.TOP_RATED
            ) is UiImagePrompt
        )
    }

    @Test
    fun `When unable to load image urls returns empty urls`() = runTest {
        val useCase = getUseCase(testScheduler)
        coEvery {
            filmFactsRepository.getImageUrl(any(), any())
        } returns null

        val prompt = useCase.invoke(null) as UiImagePrompt
        prompt.entries.forEach {
            Assert.assertTrue(it.imagePath.isEmpty())
        }
    }

    @Test
    fun `When unable to preload images returns null`() = runTest {
        val useCase = getUseCase(testScheduler)
        coEvery {
            preloadImages(any(), *anyVararg())
        } returns false

        Assert.assertNull(useCase.invoke(null))
    }

    private fun getUseCase(testScheduler: TestCoroutineScheduler) =
        GetScoredMoviesStarringActorUseCase(
            mockk(),
            filmFactsRepository,
            recentPromptsRepository,
            StandardTestDispatcher(testScheduler)
        )

    private companion object {
        val stubDiscoverMovieResponse = DiscoverMovieResponse(
            1,
            listOf(
                DiscoverMovie(0, "foo", "fooPath", emptyList(), "", "en", 5f, 10, 5f),
                DiscoverMovie(1, "bar", "barPath", emptyList(), "", "en", 5f, 10, 5f),
                DiscoverMovie(2, "fizz", "fizzPath", emptyList(), "", "en", 5f, 10, 5f),
                DiscoverMovie(3, "buzz", "buzzPath", emptyList(), "", "en", 5f, 10, 5f)
            ),
            1,
            4
        )
    }
}
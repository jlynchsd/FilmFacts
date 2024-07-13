package com.interviewsample.filmfacts.domain

import com.movietrivia.filmfacts.api.DiscoverMovieResponse
import com.movietrivia.filmfacts.api.MovieCreditEntry
import com.movietrivia.filmfacts.api.MovieCredits
import com.movietrivia.filmfacts.domain.GetMoviesStarringActorUseCase
import com.movietrivia.filmfacts.domain.getActors
import com.movietrivia.filmfacts.domain.preloadImages
import com.movietrivia.filmfacts.model.Actor
import com.movietrivia.filmfacts.model.DiscoverMovie
import com.movietrivia.filmfacts.model.FilmFactsRepository
import com.movietrivia.filmfacts.model.RecentPromptsRepository
import com.movietrivia.filmfacts.model.UiImagePrompt
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.mockkStatic
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test

class GetMoviesStarringActorUseCaseTest {

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
            filmFactsRepository.getMovies(cast = any(), includeGenres = any())
        } returns stubDiscoverMovieResponse

        coEvery {
            filmFactsRepository.getMovieCredits(any())
        } returns MovieCredits(
            0,
            listOf(
                MovieCreditEntry(4, "foo", null, "foo"),
                MovieCreditEntry(5, "bar", null, "bar"),
                MovieCreditEntry(6, "fizz", null, "fizz"),
                MovieCreditEntry(7, "buzz", null, "buzz")
            )
        )

        coEvery {
            filmFactsRepository.getImageUrl(any(), any())
        } returns "fooUrl"

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
    fun `When unable to get movie response and no additional actors returns null`() = runTest {
        val useCase = getUseCase(testScheduler)
        coEvery {
            getActors(any(), any(), any())
        } returnsMany listOf(
            listOf(
                Actor(0, "foo", null),
                Actor(1, "bar", null),
                Actor(2, "fizz", null),
                Actor(3, "buzz", null)
            ),
            emptyList()
        )
        coEvery {
            filmFactsRepository.getMovies(cast = any(), includeGenres = any())
        } returns null

        Assert.assertNull(useCase.invoke(null))
    }

    @Test
    fun `When unable to get movie response but there are additional actors returns prompt`() = runTest {
        val useCase = getUseCase(testScheduler)
        coEvery {
            getActors(any(), any(), any())
        } returnsMany listOf(
            listOf(
                Actor(0, "foo", null),
                Actor(1, "bar", null),
                Actor(2, "fizz", null),
                Actor(3, "buzz", null)
            ),
            emptyList()
        )
        coEvery {
            filmFactsRepository.getMovies(cast = any(), includeGenres = any())
        } returnsMany listOf(null, stubDiscoverMovieResponse, stubDiscoverMovieResponse)

        Assert.assertTrue(useCase.invoke(null) is UiImagePrompt)
    }

    @Test
    fun `When unable to get movies returns null`() = runTest {
        val useCase = getUseCase(testScheduler)
        coEvery {
            filmFactsRepository.getMovies(cast = any(), includeGenres = any())
        } returns null

        Assert.assertNull(useCase.invoke(null))
    }

    @Test
    fun `When filtering actors runs out of actors and successfully queries new actors returns prompt`() = runTest {
        val useCase = getUseCase(testScheduler)
        coEvery {
            getActors(any(), any(), any())
        } returnsMany listOf(
            listOf(
                Actor(0, "foo", null),
                Actor(0, "bar", null),
                Actor(0, "fizz", null),
                Actor(0, "buzz", null)
            ),
            listOf(
                Actor(0, "foo", null),
                Actor(1, "bar", null),
                Actor(2, "fizz", null),
                Actor(3, "buzz", null)
            )
        )

        Assert.assertTrue(useCase.invoke(null) is UiImagePrompt)
    }

    @Test
    fun `When unable to get filler actors returns null`() = runTest {
        val useCase = getUseCase(testScheduler)
        coEvery {
            getActors(any(), any(), any())
        } returns listOf(
            Actor(0, "foo", null),
            Actor(0, "bar", null),
            Actor(0, "fizz", null),
            Actor(0, "buzz", null)
        )

        Assert.assertNull(useCase.invoke(null))
    }

    @Test
    fun `When unable to get movie credits returns null`() = runTest {
        val useCase = getUseCase(testScheduler)
        coEvery {
            filmFactsRepository.getMovieCredits(any())
        } returns null

        Assert.assertNull(useCase.invoke(null))
    }

    @Test
    fun `When unable to get enough movies returns null`() = runTest {
        val useCase = getUseCase(testScheduler)
        coEvery {
            filmFactsRepository.getMovies(cast = any(), includeGenres = any())
        } returns stubDiscoverMovieResponse.copy(results = listOf(stubDiscoverMovieResponse.results.first()))

        Assert.assertNull(useCase.invoke(null))
    }

    @Test
    fun `When unable to get image url returns prompt with empty urls`() = runTest {
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
        GetMoviesStarringActorUseCase(
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
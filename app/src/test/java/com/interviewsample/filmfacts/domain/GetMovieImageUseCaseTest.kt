package com.interviewsample.filmfacts.domain

import com.movietrivia.filmfacts.api.DiscoverMovieResponse
import com.movietrivia.filmfacts.domain.GetMovieImageUseCase
import com.movietrivia.filmfacts.domain.getMovieDateRange
import com.movietrivia.filmfacts.domain.getMovieImage
import com.movietrivia.filmfacts.domain.preloadImages
import com.movietrivia.filmfacts.model.CalendarProvider
import com.movietrivia.filmfacts.model.DiscoverMovie
import com.movietrivia.filmfacts.model.FilmFactsRepository
import com.movietrivia.filmfacts.model.RecentPromptsRepository
import com.movietrivia.filmfacts.model.UiTextPrompt
import com.movietrivia.filmfacts.model.UserDataRepository
import com.movietrivia.filmfacts.model.UserSettings
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.runTest
import okio.IOException
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test

class GetMovieImageUseCaseTest {

    private lateinit var filmFactsRepository: FilmFactsRepository
    private lateinit var recentPromptsRepository: RecentPromptsRepository
    private lateinit var userDataRepository: UserDataRepository

    private lateinit var userSettingsFlow: MutableSharedFlow<UserSettings>

    @Before
    fun setup() {
        filmFactsRepository = mockk(relaxed = true)
        recentPromptsRepository = mockk(relaxed = true)
        userDataRepository = mockk(relaxed = true)

        mockkStatic(::preloadImages)
        coEvery {
            preloadImages(any(), *anyVararg())
        } returns true

        userSettingsFlow = MutableSharedFlow(replay = 1)
        userSettingsFlow.tryEmit(UserSettings())
        every {
            userDataRepository.userSettings
        } returns userSettingsFlow

        mockkStatic(::getMovieDateRange)
        every {
            getMovieDateRange(any(), any())
        } returns mockk()

        coEvery {
            filmFactsRepository.getMovies(dateRange = any(), order = any(), includeGenres = any())
        } returns DiscoverMovieResponse(
            0,
            listOf(
                DiscoverMovie(0, "foo", "fooPath", emptyList(), "", "en", 5f, 10, 5f),
                DiscoverMovie(0, "bar", "barPath", emptyList(), "", "en", 5f, 10, 5f),
                DiscoverMovie(0, "fizz", "fizzPath", emptyList(), "", "en", 5f, 10, 5f),
                DiscoverMovie(0, "buzz", "buzzPath", emptyList(), "", "en", 5f, 10, 5f)
            ),
            1,
            4
        )

        mockkStatic(::getMovieImage)
        coEvery {
            getMovieImage(any(), any())
        } returns mockk(relaxed = true)

        coEvery {
            filmFactsRepository.getImageUrl(any(), any())
        } returns "fooPath"
    }

    @After
    fun teardown() {
        clearAllMocks()
    }

    @Test
    fun `When unable to get user settings returns null`() = runTest {
        val useCase = getUseCase(testScheduler)
        every {
            userDataRepository.userSettings
        } returns emptyFlow()

        Assert.assertNull(useCase.invoke(null))
    }

    @Test
    fun `When getting user settings throws exception returns null`() = runTest {
        val useCase = getUseCase(testScheduler)
        every {
            userDataRepository.userSettings
        } returns kotlinx.coroutines.flow.flow {
            throw IOException()
        }

        Assert.assertNull(useCase.invoke(null))
    }

    @Test
    fun `When unable to get movies response returns null`() = runTest {
        val useCase = getUseCase(testScheduler)
        coEvery {
            filmFactsRepository.getMovies(dateRange = any(), order = any(), includeGenres = any())
        } returns null

        Assert.assertNull(useCase.invoke(null))
    }

    @Test
    fun `When movie results are empty returns null`() = runTest {
        val useCase = getUseCase(testScheduler)
        coEvery {
            filmFactsRepository.getMovies(dateRange = any(), order = any(), includeGenres = any())
        } returns DiscoverMovieResponse(0, emptyList(), 1, 0)

        Assert.assertNull(useCase.invoke(null))
    }

    @Test
    fun `When too few movie results returns null`() = runTest {
        val useCase = getUseCase(testScheduler)
        coEvery {
            filmFactsRepository.getMovies(dateRange = any(), order = any(), includeGenres = any())
        } returns DiscoverMovieResponse(
            0,
            listOf(mockk(relaxed = true), mockk(relaxed = true), mockk(relaxed = true)),
            1,
            3
        )

        Assert.assertNull(useCase.invoke(null))
    }

    @Test
    fun `When too few movie results after filtering returns null`() = runTest {
        val useCase = getUseCase(testScheduler)
        every {
            recentPromptsRepository.isRecentMovie(any())
        } returns true

        Assert.assertNull(useCase.invoke(null))
    }

    @Test
    fun `When unable to get movie images returns null`() = runTest {
        val useCase = getUseCase(testScheduler)
        coEvery {
            getMovieImage(any(), any())
        } returns null

        Assert.assertNull(useCase.invoke(null))
    }

    @Test
    fun `When unable to preload images returns null`() = runTest {
        val useCase = getUseCase(testScheduler)
        coEvery {
            preloadImages(any(), *anyVararg())
        } returns false

        Assert.assertNull(useCase.invoke(null))
    }

    @Test
    fun `When able to load movie image and data returns prompt`() = runTest {
        val useCase = getUseCase(testScheduler)

        Assert.assertTrue(useCase.invoke(null) is UiTextPrompt)
    }

    @Test
    fun `When unable to load image url returns prompt with empty path`() = runTest {
        val useCase = getUseCase(testScheduler)

        coEvery {
            filmFactsRepository.getImageUrl(any(), any())
        } returns null

        val prompt = useCase.invoke(null) as UiTextPrompt
        Assert.assertEquals("", prompt.images.first().imagePath)
    }


    private fun getUseCase(testScheduler: TestCoroutineScheduler) =
        GetMovieImageUseCase(
            mockk(),
            filmFactsRepository,
            recentPromptsRepository,
            userDataRepository,
            CalendarProvider(),
            StandardTestDispatcher(testScheduler)
        )
}
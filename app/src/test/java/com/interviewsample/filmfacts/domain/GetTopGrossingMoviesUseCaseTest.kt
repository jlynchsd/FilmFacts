package com.interviewsample.filmfacts.domain

import com.movietrivia.filmfacts.api.DiscoverMovieResponse
import com.movietrivia.filmfacts.api.MovieDetails
import com.movietrivia.filmfacts.domain.GetTopGrossingMoviesUseCase
import com.movietrivia.filmfacts.domain.formatRevenue
import com.movietrivia.filmfacts.domain.getMovieDateRange
import com.movietrivia.filmfacts.domain.preloadImages
import com.movietrivia.filmfacts.model.CalendarProvider
import com.movietrivia.filmfacts.model.FilmFactsRepository
import com.movietrivia.filmfacts.model.RecentPromptsRepository
import com.movietrivia.filmfacts.model.UiImagePrompt
import com.movietrivia.filmfacts.model.UserDataRepository
import com.movietrivia.filmfacts.model.UserSettings
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.verify
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

class GetTopGrossingMoviesUseCaseTest {

    private lateinit var filmFactsRepository: FilmFactsRepository
    private lateinit var recentPromptsRepository: RecentPromptsRepository
    private lateinit var userDataRepository: UserDataRepository

    private lateinit var userSettingsFlow: MutableSharedFlow<UserSettings>

    @Before
    fun setup() {
        filmFactsRepository = mockk(relaxed = true)
        recentPromptsRepository = mockk(relaxed = true)
        userDataRepository = mockk(relaxed = true)

        userSettingsFlow = MutableSharedFlow(replay = 1)
        userSettingsFlow.tryEmit(UserSettings())
        every {
            userDataRepository.userSettings
        } returns userSettingsFlow

        coEvery {
            filmFactsRepository.getMovies(dateRange = any(), order = any(), includeGenres = any())
        } returns DiscoverMovieResponse(
            0,
            listOf(mockk(relaxed = true), mockk(relaxed = true), mockk(relaxed = true), mockk(relaxed = true)),
            1,
            4
        )

        var revenue = 10L
        coEvery {
            filmFactsRepository.getMovieDetails(any())
        } answers {
            MovieDetails(0, "", 0, ++revenue, 0f, "")
        }

        coEvery {
            filmFactsRepository.getImageUrl(any(), any())
        } returns "fooPath"

        mockkStatic(::getMovieDateRange)
        every {
            getMovieDateRange(any(), any(), any())
        } returns mockk()

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
    fun `When unable to get movies returns null`() = runTest {
        val useCase = getUseCase(testScheduler)
        coEvery {
            filmFactsRepository.getMovies(dateRange = any(), order = any(), includeGenres = any())
        } returns null

        Assert.assertNull(useCase.invoke(null))
    }

    @Test
    fun `When not enough movies are new returns null`() = runTest {
        val useCase = getUseCase(testScheduler)
        every {
            recentPromptsRepository.isRecentMovie(any())
        } returns true

        Assert.assertNull(useCase.invoke(null))
    }

    @Test
    fun `When not enough movies have positive revenue returns null`() = runTest {
        val useCase = getUseCase(testScheduler)
        coEvery {
            filmFactsRepository.getMovieDetails(any())
        } returns MovieDetails(0, "", 0, 0, 0f, "")

        Assert.assertNull(useCase.invoke(null))
    }

    @Test
    fun `When able to get movies returns prompt and stores recent movies`() = runTest {
        val useCase = getUseCase(testScheduler)

        Assert.assertTrue(useCase.invoke(null) is UiImagePrompt)
        verify(exactly = 4) { recentPromptsRepository.addRecentMovie(any()) }
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
    fun `When unable to get image urls returns empty urls`() = runTest {
        val useCase = getUseCase(testScheduler)
        coEvery {
            filmFactsRepository.getImageUrl(any(), any())
        } returns null

        val prompt = useCase.invoke(null) as UiImagePrompt
        prompt.entries.forEach {
            Assert.assertTrue(it.imagePath.isEmpty())
        }
    }

    private fun getUseCase(testScheduler: TestCoroutineScheduler) =
        GetTopGrossingMoviesUseCase(
            mockk(),
            filmFactsRepository,
            recentPromptsRepository,
            userDataRepository,
            CalendarProvider(),
            StandardTestDispatcher(testScheduler)
        )
}
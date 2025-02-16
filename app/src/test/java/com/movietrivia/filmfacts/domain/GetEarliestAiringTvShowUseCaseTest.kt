package com.movietrivia.filmfacts.domain

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
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class GetEarliestAiringTvShowUseCaseTest {

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
            userDataRepository.tvShowUserSettings
        } returns userSettingsFlow

        mockkStatic(::getDateRange)
        every {
            getDateRange(any(), any(), any(), any())
        } returns mockk()

        coEvery {
            filmFactsRepository.getTvShows(any(), any(), any(), any(), any(), any(), any())
        } returns mockk(relaxed = true)

        mockkStatic(::getTvShowDetails)
        coEvery {
            getTvShowDetails(any(), any(), any(), any(), any())
        } returns listOf(
            stubTvShowDetails(firstAirDate = "2000-01-01"),
            stubTvShowDetails(firstAirDate = "2000-01-02"),
            stubTvShowDetails(firstAirDate = "2000-01-03"),
            stubTvShowDetails(firstAirDate = "2000-01-04")
        )

        coEvery {
            filmFactsRepository.getImageUrl(any(), any())
        } returns "fooPath"

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
    fun `When unable to get user setting then returns null`() = runTest {
        every {
            userDataRepository.tvShowUserSettings
        } returns emptyFlow()

        val useCase = getUseCase(testScheduler)

        Assert.assertNull(useCase.invoke(null))
    }

    @Test
    fun `When unable to get tv shows then returns null`() = runTest {
        coEvery {
            filmFactsRepository.getTvShows(any(), any(), any(), any(), any(), any(), any())
        } returns null

        val useCase = getUseCase(testScheduler)

        Assert.assertNull(useCase.invoke(null))
    }

    @Test
    fun `When unable to get tv show details then returns null`() = runTest {
        coEvery {
            getTvShowDetails(any(), any(), any(), any(), any())
        } returns emptyList()

        val useCase = getUseCase(testScheduler)

        Assert.assertNull(useCase.invoke(null))
    }

    @Test
    fun `When unable to get four tv shows then returns null`() = runTest {
        coEvery {
            getTvShowDetails(any(), any(), any(), any(), any())
        } returns listOf(
            stubTvShowDetails(firstAirDate = "2000-01-01"),
            stubTvShowDetails(firstAirDate = "2000-01-02"),
            stubTvShowDetails(firstAirDate = "2000-01-03"),
        )

        val useCase = getUseCase(testScheduler)

        Assert.assertNull(useCase.invoke(null))
    }

    @Test
    fun `When unable to load images then returns null`() = runTest {
        mockkStatic(::preloadImages)
        coEvery {
            preloadImages(any(), *anyVararg())
        } returns false

        val useCase = getUseCase(testScheduler)

        Assert.assertNull(useCase.invoke(null))
    }

    @Test
    fun `When valid data is provided then returns result with correct answer`() = runTest {
        val useCase = getUseCase(testScheduler)

        val result = useCase.invoke(null) as UiImagePrompt

        Assert.assertEquals("Jan 01, 2000", result.entries.first { it.isAnswer }.data)
    }

    private fun getUseCase(testScheduler: TestCoroutineScheduler) =
        GetEarliestAiringTvShowUseCase(
            mockk(),
            filmFactsRepository,
            recentPromptsRepository,
            userDataRepository,
            CalendarProvider(),
            StandardTestDispatcher(testScheduler)
        )
}
package com.movietrivia.filmfacts.domain

import com.movietrivia.filmfacts.api.DiscoverTvShowResponse
import com.movietrivia.filmfacts.model.CalendarProvider
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
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.IOException

@RunWith(RobolectricTestRunner::class)
class GetTvShowImageUseCaseTest {

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
            userDataRepository.tvShowUserSettings
        } returns userSettingsFlow

        mockkStatic(::getDateRange)
        every {
            getDateRange(any(), any(), logTag = any())
        } returns mockk()

        coEvery {
            filmFactsRepository.getTvShows(dateRange = any(), tvShowOrder = any(), includeGenres = any(), minimumVotes = any())
        } returns DiscoverTvShowResponse(
            0,
            listOf(
                stubDiscoverTvShow(),
                stubDiscoverTvShow(),
                stubDiscoverTvShow(),
                stubDiscoverTvShow()
            ),
            1,
            4
        )

        mockkStatic(::getTvShowImage)
        coEvery {
            getTvShowImage(any(), any(), logTag = any())
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
            userDataRepository.tvShowUserSettings
        } returns emptyFlow()

        Assert.assertNull(useCase.invoke(null))
    }

    @Test
    fun `When getting user settings throws exception returns null`() = runTest {
        val useCase = getUseCase(testScheduler)
        every {
            userDataRepository.tvShowUserSettings
        } returns kotlinx.coroutines.flow.flow {
            throw IOException()
        }

        Assert.assertNull(useCase.invoke(null))
    }

    @Test
    fun `When unable to get tv shows response returns null`() = runTest {
        val useCase = getUseCase(testScheduler)
        coEvery {
            filmFactsRepository.getTvShows(dateRange = any(), tvShowOrder = any(), includeGenres = any(), minimumVotes = any())
        } returns null

        Assert.assertNull(useCase.invoke(null))
    }

    @Test
    fun `When tv show results are empty returns null`() = runTest {
        val useCase = getUseCase(testScheduler)
        coEvery {
            filmFactsRepository.getTvShows(dateRange = any(), tvShowOrder = any(), includeGenres = any(), minimumVotes = any())
        } returns DiscoverTvShowResponse(0, emptyList(), 1, 0)

        Assert.assertNull(useCase.invoke(null))
    }

    @Test
    fun `When too few tv show results returns null`() = runTest {
        val useCase = getUseCase(testScheduler)
        coEvery {
            filmFactsRepository.getTvShows(dateRange = any(), tvShowOrder = any(), includeGenres = any(), minimumVotes = any())
        } returns DiscoverTvShowResponse(
            0,
            listOf(mockk(relaxed = true), mockk(relaxed = true), mockk(relaxed = true)),
            1,
            3
        )

        Assert.assertNull(useCase.invoke(null))
    }

    @Test
    fun `When too few tv show results after filtering returns null`() = runTest {
        val useCase = getUseCase(testScheduler)
        every {
            recentPromptsRepository.isRecentTvShow(any())
        } returns true

        Assert.assertNull(useCase.invoke(null))
    }

    @Test
    fun `When unable to get tv show images returns null`() = runTest {
        val useCase = getUseCase(testScheduler)
        coEvery {
            getTvShowImage(any(), any(), logTag = any())
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
    fun `When able to load tv show image and data returns prompt`() = runTest {
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
        GetTvShowImageUseCase(
            mockk(),
            filmFactsRepository,
            recentPromptsRepository,
            userDataRepository,
            CalendarProvider(),
            StandardTestDispatcher(testScheduler)
        )
}
package com.movietrivia.filmfacts.domain

import com.movietrivia.filmfacts.api.DiscoverMovie
import com.movietrivia.filmfacts.api.DiscoverMovieResponse
import com.movietrivia.filmfacts.model.CalendarProvider
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
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class GetEarliestFilmographyUseCaseTest {

    private lateinit var filmFactsRepository: FilmFactsRepository
    private lateinit var recentPromptsRepository: RecentPromptsRepository
    private lateinit var calendarProvider: CalendarProvider

    @Before
    fun setup() {
        filmFactsRepository = mockk(relaxed = true)
        recentPromptsRepository = mockk(relaxed = true)
        calendarProvider = CalendarProvider()

        mockkStatic(::preloadImages)
        coEvery {
            preloadImages(any(), *anyVararg())
        } returns true

        mockkStatic(::getMovieActors)
        coEvery {
            getMovieActors(any(), any(), any())
        } returns listOf(mockk(), mockk(), mockk(), mockk())

        mockkStatic(::getActorDetails)
        coEvery {
            getActorDetails(any(), any(), any(), any())
        } returns listOf(mockk(relaxed = true), mockk(relaxed = true), mockk(relaxed = true), mockk(relaxed = true))

        var day = 1
        coEvery {
            filmFactsRepository.getMovies(forceSettings = any(), minimumVotes = any(), cast = any(), movieOrder = any())
        } answers {
            DiscoverMovieResponse(
                0,
                listOf(
                    DiscoverMovie(
                        0,
                        "foo",
                        "fooPath",
                        listOf(0,1,2),
                        "2000-01-0${++day}",
                        "en",
                        10f,
                        20,
                        7f
                    )
                ),
                1,
                1
            )
        }

        coEvery {
            filmFactsRepository.getImageUrl(any(), any())
        } returns "fooUrl"
    }

    @After
    fun teardown() {
        clearAllMocks()
    }

    @Test
    fun `When unable to get actors returns null`() = runTest {
        val useCase = getUseCase(testScheduler)
        coEvery {
            getMovieActors(any(), any(), any())
        } returns emptyList()

        Assert.assertNull(useCase.invoke(null))
    }

    @Test
    fun `When unable to get actor details returns null`() = runTest {
        val useCase = getUseCase(testScheduler)
        coEvery {
            getActorDetails(any(), any(), any(), any())
        } returns emptyList()

        Assert.assertNull(useCase.invoke(null))
    }

    @Test
    fun `When unable to get movies for all actors returns null`() = runTest {
        val useCase = getUseCase(testScheduler)
        coEvery {
            filmFactsRepository.getMovies(forceSettings = any(), minimumVotes = any(), cast = any(), movieOrder = any())
        } returnsMany listOf(
            DiscoverMovieResponse(0, emptyList(), 1, 1),
            null,
            DiscoverMovieResponse(0, emptyList(), 1, 1),
            DiscoverMovieResponse(0, emptyList(), 1, 1)
        )

        Assert.assertNull(useCase.invoke(null))
    }

    @Test
    fun `When unable to parse release dates returns null`() = runTest {
        val useCase = getUseCase(testScheduler)
        coEvery {
            filmFactsRepository.getMovies(forceSettings = any(), minimumVotes = any(), cast = any(), movieOrder = any())
        } returns DiscoverMovieResponse(0, emptyList(), 1, 1)

        Assert.assertNull(useCase.invoke(null))
    }

    @Test
    fun `When unable to preload movie images returns null`() = runTest {
        val useCase = getUseCase(testScheduler)
        coEvery {
            preloadImages(any(), *anyVararg())
        } returns false

        Assert.assertNull(useCase.invoke(null))
    }

    @Test
    fun `When able to load actor movies and preload images returns prompt`() = runTest {
        val useCase = getUseCase(testScheduler)

        Assert.assertNotNull(useCase.invoke(null))
    }

    @Test
    fun `When unable to get image url loads empty path`() = runTest {
        val useCase = getUseCase(testScheduler)
        coEvery {
            filmFactsRepository.getImageUrl(any(), any())
        } returns null

        val prompt = useCase.invoke(null) as UiImagePrompt
        prompt.entries.forEach {
            Assert.assertEquals("", it.imagePath)
        }
    }

    private fun getUseCase(testScheduler: TestCoroutineScheduler) =
        GetEarliestFilmographyUseCase(
            mockk(),
            filmFactsRepository,
            recentPromptsRepository,
            calendarProvider,
            StandardTestDispatcher(testScheduler)
        )
}
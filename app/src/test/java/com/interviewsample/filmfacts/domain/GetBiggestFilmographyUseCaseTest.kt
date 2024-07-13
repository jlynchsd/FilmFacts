package com.interviewsample.filmfacts.domain

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.movietrivia.filmfacts.R
import com.movietrivia.filmfacts.api.DiscoverMovieResponse
import com.movietrivia.filmfacts.domain.GetBiggestFilmographyUseCase
import com.movietrivia.filmfacts.domain.getActorDetails
import com.movietrivia.filmfacts.domain.getActors
import com.movietrivia.filmfacts.domain.preloadImages
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
class GetBiggestFilmographyUseCaseTest {

    private lateinit var filmFactsRepository: FilmFactsRepository
    private lateinit var recentPromptsRepository: RecentPromptsRepository

    @Before
    fun setup() {
        filmFactsRepository = mockk(relaxed = true)
        recentPromptsRepository = mockk(relaxed = true)

        mockkStatic(::preloadImages)
        coEvery {
            preloadImages(any(), *anyVararg())
        } returns true

        mockkStatic(::getActors)
        coEvery {
            getActors(any(), any(), any())
        } returns listOf(mockk(), mockk(), mockk(), mockk())

        mockkStatic(::getActorDetails)
        coEvery {
            getActorDetails(any(), any(), any(), any())
        } returns listOf(mockk(relaxed = true), mockk(relaxed = true), mockk(relaxed = true), mockk(relaxed = true))

        var totalResultCount = 1
        coEvery {
            filmFactsRepository.getMovies(forceSettings = any(), minimumVotes = any(), cast = any())
        } answers {
            DiscoverMovieResponse(0, emptyList(), 1, ++totalResultCount)
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
            getActors(any(), any(), any())
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
            filmFactsRepository.getMovies(forceSettings = any(), minimumVotes = any(), cast = any())
        } returnsMany listOf(
            DiscoverMovieResponse(0, emptyList(), 1, 1),
            null,
            DiscoverMovieResponse(0, emptyList(), 1, 1),
            DiscoverMovieResponse(0, emptyList(), 1, 1)
        )

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

    @Test
    fun `When actors filmography has one entry uses singular tense`() = runTest {
        val useCase = getUseCase(testScheduler)
        var totalResultCount = 1
        coEvery {
            filmFactsRepository.getMovies(forceSettings = any(), minimumVotes = any(), cast = any())
        } answers {
            DiscoverMovieResponse(0, emptyList(), 1, ++totalResultCount)
        }

        val prompt = useCase.invoke(null) as UiImagePrompt
        prompt.entries.forEach {
            Assert.assertTrue(it.data!!.contains(getContext().getString(R.string.suffix_filmography)))
        }
    }

    @Test
    fun `When actors filmography has multiple entries uses plural tense`() = runTest {
        val useCase = getUseCase(testScheduler)
        var totalResultsCount = 2
        coEvery {
            filmFactsRepository.getMovies(forceSettings = any(), minimumVotes = any(), cast = any())
        } answers {
            DiscoverMovieResponse(0, emptyList(), 1, ++totalResultsCount)
        }

        val prompt = useCase.invoke(null) as UiImagePrompt
        prompt.entries.forEach {
            Assert.assertTrue(it.data!!.contains(getContext().getString(R.string.suffix_filmography_plural)))
        }
    }

    private fun getUseCase(testScheduler: TestCoroutineScheduler) =
        GetBiggestFilmographyUseCase(
            getContext(),
            filmFactsRepository,
            recentPromptsRepository,
            StandardTestDispatcher(testScheduler)
        )

    private fun getContext(): Context = ApplicationProvider.getApplicationContext()
}
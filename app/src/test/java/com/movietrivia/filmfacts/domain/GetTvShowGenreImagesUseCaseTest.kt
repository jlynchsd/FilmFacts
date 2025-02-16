package com.movietrivia.filmfacts.domain

import com.movietrivia.filmfacts.api.DiscoverTvShowResponse
import com.movietrivia.filmfacts.api.TvGenre
import com.movietrivia.filmfacts.model.FilmFactsRepository
import com.movietrivia.filmfacts.model.GenreImageRepository
import com.movietrivia.filmfacts.model.UiGenre
import com.movietrivia.filmfacts.model.UserSettings
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.runs
import io.mockk.slot
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test

class GetTvShowGenreImagesUseCaseTest {

    private lateinit var filmFactsRepository: FilmFactsRepository
    private lateinit var genreImageRepository: GenreImageRepository

    @Before
    fun setup() {
        filmFactsRepository = mockk(relaxed = true)
        genreImageRepository = mockk(relaxed = true)

        mockkStatic(::preloadImageAsync)
        coEvery {
            preloadImageAsync(any(), any(), any())
        } returns CompletableDeferred(true)

        coEvery {
            preloadImage(any(), any(), any())
        } just runs

        coEvery {
            filmFactsRepository.getTvShows(forceSettings = any(), includeGenres = any())
        } returns DiscoverTvShowResponse(
            0,
            listOf(
                stubDiscoverTvShow(id = 0, genreIds = listOf(TvGenre.values()[0].key)),
                stubDiscoverTvShow(id = 1, genreIds = listOf(TvGenre.values()[1].key)),
                stubDiscoverTvShow(id = 2, genreIds = listOf(TvGenre.values()[2].key)),
                stubDiscoverTvShow(id = 3, genreIds = listOf(TvGenre.values()[3].key))
            ),
            1,
            1
        )

        mockkStatic(::getTvShowImage)
        coEvery {
            getTvShowImage(any(), any(), logTag = any())
        } returns mockk(relaxed = true)

        coEvery {
            filmFactsRepository.getImageUrl(any(), any())
        } returns "foo"
    }

    @After
    fun teardown() {
        clearAllMocks()
    }

    @Test
    fun `When no genre images available does not load images`() = runTest {
        val useCase = getUseCase(testScheduler)
        coEvery {
            genreImageRepository.getGenreImages()
        } returns emptyList()

        useCase.invoke()

        coVerify(exactly = 0) { preloadImageAsync(any(), any(), any()) }
        coVerify(exactly = 0) { preloadImage(any(), any(), any()) }
    }

    @Test
    fun `When one genre images available only hard loads image`() = runTest {
        val useCase = getUseCase(testScheduler)
        coEvery {
            genreImageRepository.getGenreImages()
        } returns listOf(mockk(relaxed = true))

        useCase.invoke()

        coVerify(exactly = 1) { preloadImageAsync(any(), any(), any()) }
        coVerify(exactly = 0) { preloadImage(any(), any(), any()) }
    }

    @Test
    fun `When multiple genre images available only hard loads first image and soft loads remaining images`() = runTest {
        val useCase = getUseCase(testScheduler)
        coEvery {
            genreImageRepository.getGenreImages()
        } returns listOf(mockk(relaxed = true), mockk(relaxed = true), mockk(relaxed = true))

        useCase.invoke()

        coVerify(exactly = 1) { preloadImageAsync(any(), any(), any()) }
        coVerify(exactly = 2) { preloadImage(any(), any(), any()) }
    }

    // region loadNextGenreImages

    @Test
    fun `When loading genre images and tv shows result is null does not save any images`() = runTest {
        val useCase = getUseCase(testScheduler)
        coEvery {
            filmFactsRepository.getTvShows(forceSettings = any(), includeGenres = any())
        } returns null

        useCase.loadNextGenreImages(UserSettings())

        coVerify(exactly = 0) { genreImageRepository.saveGenreImages(any()) }
    }

    @Test
    fun `When loading genre images and unable to get any tv shows images does not save any images`() = runTest {
        val useCase = getUseCase(testScheduler)
        coEvery {
            getTvShowImage(any(), any(), logTag = any())
        } returns null

        useCase.loadNextGenreImages(UserSettings())

        coVerify(exactly = 0) { genreImageRepository.saveGenreImages(any()) }
    }

    @Test
    fun `When loading genre images and loads tv shows images saves images`() = runTest {
        every {
            genreImageRepository.supportedGenres
        } returns TvGenre.values().map { it.key } + listOf(-1)

        val useCase = getUseCase(testScheduler)

        useCase.loadNextGenreImages(UserSettings())

        coVerify(exactly = 1) { genreImageRepository.saveGenreImages(any()) }
    }

    @Test
    fun `When loading genre images and unable to load image url uses empty string`() = runTest {
        every {
            genreImageRepository.supportedGenres
        } returns TvGenre.values().map { it.key } + listOf(-1)

        val useCase = getUseCase(testScheduler)
        coEvery {
            filmFactsRepository.getImageUrl(any(), any())
        } returns null

        val slot = slot<List<UiGenre>>()

        coEvery {
            genreImageRepository.saveGenreImages(capture(slot))
        } just runs

        useCase.loadNextGenreImages(UserSettings())

        Assert.assertTrue(slot.captured.isNotEmpty())
        slot.captured.forEach {
            Assert.assertEquals("", it.imagePath)
        }
    }

    // endregion

    private fun getUseCase(testScheduler: TestCoroutineScheduler) =
        GetTvShowGenreImagesUseCase(
            mockk(),
            filmFactsRepository,
            genreImageRepository,
            StandardTestDispatcher(testScheduler)
        )
}
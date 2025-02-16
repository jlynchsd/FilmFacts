package com.movietrivia.filmfacts.domain

import com.movietrivia.filmfacts.api.DiscoverMovie
import com.movietrivia.filmfacts.api.DiscoverMovieResponse
import com.movietrivia.filmfacts.api.MovieGenre
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

class GetMovieGenreImagesUseCaseTest {

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
            filmFactsRepository.getMovies(forceSettings = any(), includeGenres = any())
        } returns DiscoverMovieResponse(
            0,
            listOf(
                DiscoverMovie(0, "foo", "fooPath", listOf(MovieGenre.ACTION.key), "", "en", 10f, 20, 10f),
                DiscoverMovie(0, "bar", "barPath", listOf(MovieGenre.ADVENTURE.key), "", "en", 10f, 20, 10f),
                DiscoverMovie(0, "fizz", "fizzPath", listOf(MovieGenre.ANIMATION.key), "", "en", 10f, 20, 10f),
                DiscoverMovie(0, "buzz", "buzzPath", listOf(MovieGenre.FAMILY.key), "", "en", 10f, 20, 10f)
            ),
            1,
            1
        )

        mockkStatic(::getMovieImage)
        coEvery {
            getMovieImage(any(), any())
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
    fun `When loading genre images and movie result is null does not save any images`() = runTest {
        val useCase = getUseCase(testScheduler)
        coEvery {
            filmFactsRepository.getMovies(forceSettings = any(), includeGenres = any())
        } returns null

        useCase.loadNextGenreImages(UserSettings())

        coVerify(exactly = 0) { genreImageRepository.saveGenreImages(any()) }
    }

    @Test
    fun `When loading genre images and unable to get any movie images does not save any images`() = runTest {
        val useCase = getUseCase(testScheduler)
        coEvery {
            getMovieImage(any(), any())
        } returns null

        useCase.loadNextGenreImages(UserSettings())

        coVerify(exactly = 0) { genreImageRepository.saveGenreImages(any()) }
    }

    @Test
    fun `When loading genre images and loads movie images saves images`() = runTest {
        every {
            genreImageRepository.supportedGenres
        } returns MovieGenre.values().map { it.key }

        val useCase = getUseCase(testScheduler)

        useCase.loadNextGenreImages(UserSettings())

        coVerify(exactly = 1) { genreImageRepository.saveGenreImages(any()) }
    }

    @Test
    fun `When loading genre images and unable to load image url uses empty string`() = runTest {
        every {
            genreImageRepository.supportedGenres
        } returns MovieGenre.values().map { it.key }

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
        GetMovieGenreImagesUseCase(
            mockk(),
            filmFactsRepository,
            genreImageRepository,
            StandardTestDispatcher(testScheduler)
        )
}
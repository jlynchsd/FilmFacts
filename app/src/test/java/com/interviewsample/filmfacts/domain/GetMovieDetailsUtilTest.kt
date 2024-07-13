package com.interviewsample.filmfacts.domain

import com.movietrivia.filmfacts.api.MovieDetails
import com.movietrivia.filmfacts.api.MovieImage
import com.movietrivia.filmfacts.api.MovieImageResponse
import com.movietrivia.filmfacts.api.PersonDetails
import com.movietrivia.filmfacts.domain.getActorDetails
import com.movietrivia.filmfacts.domain.getMovieDetails
import com.movietrivia.filmfacts.domain.getMovieImage
import com.movietrivia.filmfacts.model.FilmFactsRepository
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert
import org.junit.Before
import org.junit.Test

class GetMovieDetailsUtilTest {

    private lateinit var filmFactsRepository: FilmFactsRepository

    @Before
    fun setup() {
        filmFactsRepository = mockk(relaxed = true)
    }

    // region getMovieDetails

    @Test
    fun `When getting movie details but not enough movies to reach target returns empty result`() = runTest {
        val details = getMovieDetails(filmFactsRepository, emptyList(), 3)
        Assert.assertTrue(details.isEmpty())
    }

    @Test
    fun `When getting movie details but details are null returns empty result`() = runTest {
        coEvery {
            filmFactsRepository.getMovieDetails(any())
        } returns null

        val details = getMovieDetails(
            filmFactsRepository,
            listOf(mockk(relaxed = true), mockk(relaxed = true), mockk(relaxed = true)),
            3
        )
        Assert.assertTrue(details.isEmpty())
    }

    @Test
    fun `When getting movie details returns list of movie details matching target length`() = runTest {
        coEvery {
            filmFactsRepository.getMovieDetails(any())
        } returns mockk()

        val details = getMovieDetails(
            filmFactsRepository,
            listOf(mockk(relaxed = true), mockk(relaxed = true), mockk(relaxed = true)),
            3
        )
        Assert.assertEquals(3, details.size)
    }

    @Test
    fun `When getting movie details and more details than targets size returns list of movie details matching target length`() = runTest {
        coEvery {
            filmFactsRepository.getMovieDetails(any())
        } returns mockk()

        val details = getMovieDetails(
            filmFactsRepository,
            listOf(mockk(relaxed = true), mockk(relaxed = true), mockk(relaxed = true), mockk(relaxed = true), mockk(relaxed = true)),
            3
        )
        Assert.assertEquals(3, details.size)
    }

    @Test
    fun `When getting movie details with filter applies filter to results`() = runTest {
        coEvery {
            filmFactsRepository.getMovieDetails(any())
        } returnsMany listOf(
            MovieDetails(0, "foo", 0, 0, 0f, ""),
            MovieDetails(1, "bar", 0, 0, 0f, ""),
            MovieDetails(2, "fizz", 0, 0, 0f, ""),
            MovieDetails(3, "buzz", 0, 0, 0f, "")
        )

        val details = getMovieDetails(
            filmFactsRepository,
            listOf(mockk(relaxed = true), mockk(relaxed = true), mockk(relaxed = true), mockk(relaxed = true), mockk(relaxed = true)),
            3
        ) {
            it.id != 0
        }
        Assert.assertEquals(3, details.size)
        details.forEach {
            Assert.assertNotEquals(0, it.id)
        }
    }

    @Test
    fun `When getting movie details with filter has fewer matches than target length returns empty result`() = runTest {
        coEvery {
            filmFactsRepository.getMovieDetails(any())
        } returnsMany listOf(
            MovieDetails(0, "foo", 0, 0, 0f, ""),
            MovieDetails(1, "bar", 0, 0, 0f, ""),
            MovieDetails(2, "fizz", 0, 0, 0f, ""),
            MovieDetails(3, "buzz", 0, 0, 0f, "")
        )

        val details = getMovieDetails(
            filmFactsRepository,
            listOf(mockk(relaxed = true), mockk(relaxed = true), mockk(relaxed = true), mockk(relaxed = true)),
            3
        ) {
            it.id >= 3
        }
        Assert.assertTrue(details.isEmpty())
    }

    // endregion

    // region getMovieImage

    @Test
    fun `When getting movie image but unable to get images returns null`() = runTest {
        coEvery {
            filmFactsRepository.getMovieImages(any())
        } returns null

        val image = getMovieImage(filmFactsRepository, mockk(relaxed = true))

        Assert.assertNull(image)
    }

    @Test
    fun `When getting movie image but no image backdrops returns null`() = runTest {
        coEvery {
            filmFactsRepository.getMovieImages(any())
        } returns MovieImageResponse(
            0,
            emptyList()
        )

        val image = getMovieImage(filmFactsRepository, mockk(relaxed = true))

        Assert.assertNull(image)
    }

    @Test
    fun `When getting movie image but no image backdrops without language returns null`() = runTest {
        coEvery {
            filmFactsRepository.getMovieImages(any())
        } returns MovieImageResponse(
            0,
            listOf(
                MovieImage("en", "foo", 0),
                MovieImage("en", "bar", 0)
            )
        )

        val image = getMovieImage(filmFactsRepository, mockk(relaxed = true))

        Assert.assertNull(image)
    }

    @Test
    fun `When getting movie image picks highest voted matching image`() = runTest {
        coEvery {
            filmFactsRepository.getMovieImages(any())
        } returns MovieImageResponse(
            0,
            listOf(
                MovieImage("en", "foo", 10),
                MovieImage("fr", "bar", 5),
                MovieImage(null, "fizz", 0),
                MovieImage(null, "buzz", 3),
            )
        )

        val image = getMovieImage(filmFactsRepository, mockk(relaxed = true))

        Assert.assertEquals("buzz", image!!.filePath)
    }

    // endregion

    // region getActorDetails

    @Test
    fun `When getting actor details but not enough actors to reach target returns empty result`() = runTest {
        val details = getActorDetails(filmFactsRepository, emptyList(), 3)
        Assert.assertTrue(details.isEmpty())
    }

    @Test
    fun `When getting actor details but details are null returns empty result`() = runTest {
        coEvery {
            filmFactsRepository.getActorDetails(any())
        } returns null

        val details = getActorDetails(
            filmFactsRepository,
            listOf(mockk(relaxed = true), mockk(relaxed = true), mockk(relaxed = true)),
            3
        )
        Assert.assertTrue(details.isEmpty())
    }

    @Test
    fun `When getting actor details returns list of actor details matching target length`() = runTest {
        coEvery {
            filmFactsRepository.getActorDetails(any())
        } returns mockk()

        val details = getActorDetails(
            filmFactsRepository,
            listOf(mockk(relaxed = true), mockk(relaxed = true), mockk(relaxed = true)),
            3
        )
        Assert.assertEquals(3, details.size)
    }

    @Test
    fun `When getting actor details and more details than targets size returns list of actor details matching target length`() = runTest {
        coEvery {
            filmFactsRepository.getActorDetails(any())
        } returns mockk()

        val details = getActorDetails(
            filmFactsRepository,
            listOf(mockk(relaxed = true), mockk(relaxed = true), mockk(relaxed = true), mockk(relaxed = true), mockk(relaxed = true)),
            3
        )
        Assert.assertEquals(3, details.size)
    }

    @Test
    fun `When getting actor details with filter applies filter to results`() = runTest {
        coEvery {
            filmFactsRepository.getActorDetails(any())
        } returnsMany listOf(
            PersonDetails(0, "foo", 0, ""),
            PersonDetails(1, "bar", 0, ""),
            PersonDetails(2, "fizz", 0, ""),
            PersonDetails(3, "buzz", 0, "")
        )

        val details = getActorDetails(
            filmFactsRepository,
            listOf(mockk(relaxed = true), mockk(relaxed = true), mockk(relaxed = true), mockk(relaxed = true), mockk(relaxed = true)),
            3
        ) {
            it.id != 0
        }
        Assert.assertEquals(3, details.size)
        details.forEach {
            Assert.assertNotEquals(0, it.id)
        }
    }

    @Test
    fun `When getting actor details with filter has fewer matches than target length returns empty result`() = runTest {
        coEvery {
            filmFactsRepository.getActorDetails(any())
        } returnsMany listOf(
            PersonDetails(0, "foo", 0, ""),
            PersonDetails(1, "bar", 0, ""),
            PersonDetails(2, "fizz", 0, ""),
            PersonDetails(3, "buzz", 0, "")
        )

        val details = getActorDetails(
            filmFactsRepository,
            listOf(mockk(relaxed = true), mockk(relaxed = true), mockk(relaxed = true), mockk(relaxed = true)),
            3
        ) {
            it.id >= 3
        }
        Assert.assertTrue(details.isEmpty())
    }

    // endregion
}
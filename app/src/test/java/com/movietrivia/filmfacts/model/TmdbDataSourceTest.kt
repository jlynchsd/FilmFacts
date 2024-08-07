package com.movietrivia.filmfacts.model

import com.movietrivia.filmfacts.api.ActorCreditsResponse
import com.movietrivia.filmfacts.api.ConfigurationResponse
import com.movietrivia.filmfacts.api.ConfigurationService
import com.movietrivia.filmfacts.api.DiscoverMovie
import com.movietrivia.filmfacts.api.DiscoverMovieResponse
import com.movietrivia.filmfacts.api.DiscoverService
import com.movietrivia.filmfacts.api.MovieService
import com.movietrivia.filmfacts.api.PersonService
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import retrofit2.Response
import java.time.Instant
import java.util.Date

@RunWith(RobolectricTestRunner::class)
class TmdbDataSourceTest {

    private lateinit var discoverService: DiscoverService
    private lateinit var configurationService: ConfigurationService
    private lateinit var personService: PersonService
    private lateinit var movieService: MovieService
    private lateinit var tooManyRequestsDataSource: TooManyRequestsDataSource
    private lateinit var dataSource: TmdbDataSource

    @Before
    fun setup() {
        discoverService = mockk(relaxed = true)
        configurationService = mockk(relaxed = true)
        personService = mockk(relaxed = true)
        movieService = mockk(relaxed = true)
        tooManyRequestsDataSource = mockk(relaxed = true)
        dataSource = TmdbDataSource(discoverService, configurationService, personService, movieService, tooManyRequestsDataSource)

        every {
            tooManyRequestsDataSource.requestsAllowed()
        } returns true
    }

    @Test
    fun `When getting image configuration delegates to configuration service`() = runTest {
        dataSource.getImageConfiguration()

        coVerify { configurationService.getConfiguration(any()) }
    }

    @Test
    fun `When getting image configuration throws exception returns null`() = runTest {
        coEvery {
            configurationService.getConfiguration(any())
        } throws Exception()

        Assert.assertNull(dataSource.getImageConfiguration())
    }

    @Test
    fun `When getting image configuration but body is null returns null`() = runTest {
        val mockResponse = mockk<Response<ConfigurationResponse>>()
        every {
            mockResponse.body()
        } returns null

        coEvery {
            configurationService.getConfiguration(any())
        } returns mockResponse

        Assert.assertNull(dataSource.getImageConfiguration())
    }

    @Test
    fun `When getting movie credits delegates to movie service`() = runTest {
        dataSource.getMovieCredits(0)

        coVerify { movieService.getMovieCredits(any(), any()) }
    }

    @Test
    fun `When getting movie credits throws exception returns null`() = runTest {
        coEvery {
            movieService.getMovieCredits(any(), any())
        } throws Exception()

        Assert.assertNull(dataSource.getMovieCredits(0))
    }

    @Test
    fun `When getting movie details delegates to movie service`() = runTest {
        dataSource.getMovieDetails(0)

        coVerify { movieService.getMovieDetails(any(), any()) }
    }

    @Test
    fun `When getting movie details throws exception returns null`() = runTest {
        coEvery {
            movieService.getMovieDetails(any(), any())
        } throws Exception()

        Assert.assertNull(dataSource.getMovieDetails(0))
    }

    @Test
    fun `When getting movie images delegates to movie service`() = runTest {
        dataSource.getMovieImages(0)

        coVerify { movieService.getMovieImages(any(), any()) }
    }

    @Test
    fun `When getting movie images throws exception returns null`() = runTest {
        coEvery {
            movieService.getMovieImages(any(), any())
        } throws Exception()

        Assert.assertNull(dataSource.getMovieImages(0))
    }

    @Test
    fun `When getting actor details delegates to person service`() = runTest {
        dataSource.getActorDetails(0)

        coVerify { personService.getActorDetails(any(), any()) }
    }

    @Test
    fun `When getting actor details throws exception returns null`() = runTest {
        coEvery {
            personService.getActorDetails(any(), any())
        } throws Exception()

        Assert.assertNull(dataSource.getActorDetails(0))
    }

    @Test
    fun `When getting actor credits delegates to person service`() = runTest {
        dataSource.getActorCredits(0)

        coVerify { personService.getActorCredits(any(), any()) }
    }

    @Test
    fun `When getting actor credits throws exception returns null`() = runTest {
        coEvery {
            personService.getActorCredits(any(), any())
        } throws Exception()

        Assert.assertNull(dataSource.getActorCredits(0))
    }

    @Test
    fun `When getting actor credits but body is null returns null`() = runTest {
        val mockResponse = mockk<Response<ActorCreditsResponse>>()
        every {
            mockResponse.body()
        } returns null

        coEvery {
            personService.getActorCredits(any(), any())
        } returns mockResponse

        Assert.assertNull(dataSource.getActorCredits(0))
    }

    @Test
    fun `When getting movies with no filters does not pass filters to discover service`() = runTest {
        val filterSlot = slot<Map<String, String>>()
        coEvery {
            discoverService.getMovies(capture(filterSlot))
        } returns mockk(relaxed = true)

        dataSource.getMovies(
            UserSettings(), null, null, null,
            null, null, null, null, 0
        )

        Assert.assertEquals(2, filterSlot.captured.keys.size)
    }

    @Test
    fun `When getting movies with all filters passes all filters to discover service`() = runTest {
        val filterSlot = slot<Map<String, String>>()
        coEvery {
            discoverService.getMovies(capture(filterSlot))
        } returns mockk(relaxed = true)

        dataSource.getMovies(
            UserSettings(),
            Pair(Date.from(Instant.now()), Date.from(Instant.now())),
            DiscoverService.Builder.Order.REVENUE_DESC,
            DiscoverService.Builder.ReleaseType.PREMIERE,
            listOf(1,2,3),
            listOf(4,5,6),
            listOf(7,8,9),
            7,
            0
        )

        Assert.assertEquals(10, filterSlot.captured.keys.size)
    }

    @Test
    fun `When getting movies throws exception returns null`() = runTest {
        coEvery {
            discoverService.getMovies(any())
        } throws Exception()

        Assert.assertNull(
            dataSource.getMovies(
                UserSettings(), null, null, null,
                null, null, null, null, 0
            )
        )
    }

    @Test
    fun `When getting movies with invalid entries filters them out`() = runTest {
        val entries = listOf(
            DiscoverMovie(
                0,
                "title",
                "poster",
                listOf(0),
                "date",
                "language",
                0F,
                0,
                0F
            ),
            DiscoverMovie(
                0,
                "title",
                "poster",
                listOf(0),
                "date",
                "language",
                0F,
                0,
                0F
            ),
            DiscoverMovie(
                0,
                "title1",
                "",
                listOf(0),
                "date",
                "language",
                0F,
                0,
                0F
            ),
            DiscoverMovie(
                0,
                "title2",
                "poster",
                listOf(),
                "date",
                "language",
                0F,
                0,
                0F
            ),
            DiscoverMovie(
                0,
                "title3",
                "poster",
                listOf(0),
                "",
                "language",
                0F,
                0,
                0F
            )
        )

        val mockResponse = mockk<Response<DiscoverMovieResponse>>()
        every {
            mockResponse.body()
        } returns DiscoverMovieResponse(0, entries, 0, 0)

        coEvery {
            discoverService.getMovies(any())
        } returns mockResponse

        Assert.assertEquals(1,
            dataSource.getMovies(
                UserSettings(), null, null, null,
                null, null, null, null, 0
            )?.results?.size
        )
    }

    @Test
    fun `When movie response body is null returns null`() = runTest {
        val mockResponse = mockk<Response<DiscoverMovieResponse>>()
        every {
            mockResponse.body()
        } returns null
        coEvery {
            discoverService.getMovies(any())
        } returns mockResponse

        Assert.assertNull(
            dataSource.getMovies(
                UserSettings(), null, null, null,
                null, null, null, null, 0
            )
        )
    }
}
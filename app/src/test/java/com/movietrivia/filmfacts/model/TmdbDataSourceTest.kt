package com.movietrivia.filmfacts.model

import com.movietrivia.filmfacts.api.ActorMovieCreditsResponse
import com.movietrivia.filmfacts.api.ActorTvShowCreditsResponse
import com.movietrivia.filmfacts.api.ConfigurationResponse
import com.movietrivia.filmfacts.api.ConfigurationService
import com.movietrivia.filmfacts.api.DiscoverMovie
import com.movietrivia.filmfacts.api.DiscoverMovieResponse
import com.movietrivia.filmfacts.api.DiscoverService
import com.movietrivia.filmfacts.api.DiscoverTvShowResponse
import com.movietrivia.filmfacts.api.MovieService
import com.movietrivia.filmfacts.api.PersonService
import com.movietrivia.filmfacts.api.TvShowService
import com.movietrivia.filmfacts.domain.stubDiscoverTvShow
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
    private lateinit var tvShowService: TvShowService
    private lateinit var tooManyRequestsDataSource: TooManyRequestsDataSource
    private lateinit var dataSource: TmdbDataSource

    @Before
    fun setup() {
        discoverService = mockk(relaxed = true)
        configurationService = mockk(relaxed = true)
        personService = mockk(relaxed = true)
        movieService = mockk(relaxed = true)
        tvShowService = mockk(relaxed = true)
        tooManyRequestsDataSource = mockk(relaxed = true)
        dataSource = TmdbDataSource(discoverService, configurationService, personService, movieService, tvShowService, tooManyRequestsDataSource)

        every {
            tooManyRequestsDataSource.requestsAllowed()
        } returns true
    }


    // region Image Configuration

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

    // endregion


    // region Movies

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
    fun `When getting movies with no filters does not pass filters to discover service`() = runTest {
        val filterSlot = slot<Map<String, String>>()
        coEvery {
            discoverService.getMovies(capture(filterSlot))
        } returns mockk(relaxed = true)

        dataSource.getMovies(
            UserSettings(), null, null, null,
            null, null, null, null, 0
        )

        Assert.assertEquals(6, filterSlot.captured.keys.size)
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
            DiscoverService.Builder.MovieOrder.REVENUE_DESC,
            DiscoverService.Builder.ReleaseType.PREMIERE,
            listOf(1,2,3),
            listOf(4,5,6),
            listOf(7,8,9),
            7,
            0
        )

        Assert.assertEquals(14, filterSlot.captured.keys.size)
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

    // endregion


    // region Tv Shows

    @Test
    fun `When getting tv show credits delegates to tv show service`() = runTest {
        dataSource.getTvShowCredits(0)

        coVerify { tvShowService.getTvShowCredits(any(), any()) }
    }

    @Test
    fun `When getting tv show credits throws exception returns null`() = runTest {
        coEvery {
            tvShowService.getTvShowCredits(any(), any())
        } throws Exception()

        Assert.assertNull(dataSource.getTvShowCredits(0))
    }

    @Test
    fun `When getting tv show details delegates to tv show service`() = runTest {
        dataSource.getTvShowDetails(0)

        coVerify { tvShowService.getTvShowDetails(any(), any()) }
    }

    @Test
    fun `When getting tv show details throws exception returns null`() = runTest {
        coEvery {
            tvShowService.getTvShowDetails(any(), any())
        } throws Exception()

        Assert.assertNull(dataSource.getTvShowDetails(0))
    }

    @Test
    fun `When getting tv show images delegates to tv show service`() = runTest {
        dataSource.getTvShowImages(0)

        coVerify { tvShowService.getTvShowImages(any(), any()) }
    }

    @Test
    fun `When getting tv show images throws exception returns null`() = runTest {
        coEvery {
            tvShowService.getTvShowImages(any(), any())
        } throws Exception()

        Assert.assertNull(dataSource.getTvShowImages(0))
    }

    @Test
    fun `When getting tv shows with no filters does not pass filters to discover service`() = runTest {
        val filterSlot = slot<Map<String, String>>()
        coEvery {
            discoverService.getTvShows(capture(filterSlot))
        } returns mockk(relaxed = true)

        dataSource.getTvShows(
            UserSettings(), null, null, null,
            null, null,0
        )

        Assert.assertEquals(6, filterSlot.captured.keys.size)
    }

    @Test
    fun `When getting tv shows with all filters passes all filters to discover service`() = runTest {
        val filterSlot = slot<Map<String, String>>()
        coEvery {
            discoverService.getTvShows(capture(filterSlot))
        } returns mockk(relaxed = true)

        dataSource.getTvShows(
            UserSettings(),
            Pair(Date.from(Instant.now()), Date.from(Instant.now())),
            DiscoverService.Builder.TvShowOrder.VOTE_AVERAGE_ASC,
            listOf(1,2,3),
            listOf(4,5,6),
            7,
            0
        )

        Assert.assertEquals(12, filterSlot.captured.keys.size)
    }

    @Test
    fun `When getting tv shows throws exception returns null`() = runTest {
        coEvery {
            discoverService.getTvShows(any())
        } throws Exception()

        Assert.assertNull(
            dataSource.getTvShows(
                UserSettings(), null, null, null,
                null, null, 0
            )
        )
    }

    @Test
    fun `When getting tv shows with invalid entries filters them out`() = runTest {
        val entries = listOf(
            stubDiscoverTvShow(name = "title", genreIds = listOf(1)),
            stubDiscoverTvShow(name = "title1", posterPath = "", firstAirDate = "date"),
            stubDiscoverTvShow(name = "title2", firstAirDate = "date"),
            stubDiscoverTvShow(name = "title3")
        )

        val mockResponse = mockk<Response<DiscoverTvShowResponse>>()
        every {
            mockResponse.body()
        } returns DiscoverTvShowResponse(0, entries, 0, 0)

        coEvery {
            discoverService.getTvShows(any())
        } returns mockResponse

        Assert.assertEquals(1,
            dataSource.getTvShows(
                UserSettings(), null, null, null,
                null, null, 0
            )?.results?.size
        )
    }

    @Test
    fun `When tv show response body is null returns null`() = runTest {
        val mockResponse = mockk<Response<DiscoverTvShowResponse>>()
        every {
            mockResponse.body()
        } returns null
        coEvery {
            discoverService.getTvShows(any())
        } returns mockResponse

        Assert.assertNull(
            dataSource.getTvShows(
                UserSettings(), null, null, null,
                null, null, 0
            )
        )
    }

    // endregion


    // region Actors

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
    fun `When getting movie actor credits delegates to person service`() = runTest {
        dataSource.getActorMovieCredits(0)

        coVerify { personService.getActorMovieCredits(any(), any()) }
    }

    @Test
    fun `When getting movie actor credits throws exception returns null`() = runTest {
        coEvery {
            personService.getActorMovieCredits(any(), any())
        } throws Exception()

        Assert.assertNull(dataSource.getActorMovieCredits(0))
    }

    @Test
    fun `When getting movie actor credits but body is null returns null`() = runTest {
        val mockResponse = mockk<Response<ActorMovieCreditsResponse>>()
        every {
            mockResponse.body()
        } returns null

        coEvery {
            personService.getActorMovieCredits(any(), any())
        } returns mockResponse

        Assert.assertNull(dataSource.getActorMovieCredits(0))
    }

    @Test
    fun `When getting tv show actor credits delegates to person service`() = runTest {
        dataSource.getActorTvShowCredits(0)

        coVerify { personService.getActorTvShowCredits(any(), any()) }
    }

    @Test
    fun `When getting actor tv show credits throws exception returns null`() = runTest {
        coEvery {
            personService.getActorTvShowCredits(any(), any())
        } throws Exception()

        Assert.assertNull(dataSource.getActorTvShowCredits(0))
    }

    @Test
    fun `When getting actor tv show credits but body is null returns null`() = runTest {
        val mockResponse = mockk<Response<ActorTvShowCreditsResponse>>()
        every {
            mockResponse.body()
        } returns null

        coEvery {
            personService.getActorTvShowCredits(any(), any())
        } returns mockResponse

        Assert.assertNull(dataSource.getActorTvShowCredits(0))
    }

    // endregion
}
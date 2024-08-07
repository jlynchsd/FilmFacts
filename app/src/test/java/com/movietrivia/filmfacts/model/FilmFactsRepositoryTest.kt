package com.movietrivia.filmfacts.model

import com.movietrivia.filmfacts.api.AccountMoviesResponse
import com.movietrivia.filmfacts.api.AccountRatedMoviesResponse
import com.movietrivia.filmfacts.api.DiscoverMovie
import com.movietrivia.filmfacts.api.DiscoverMovieResponse
import com.movietrivia.filmfacts.api.DiscoverService
import com.movietrivia.filmfacts.api.ImageConfiguration
import com.movietrivia.filmfacts.api.RatedMovie
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert
import org.junit.Before
import org.junit.Test

class FilmFactsRepositoryTest {

    private lateinit var remoteDataSource: TmdbDataSource
    private lateinit var userDataRepository: UserDataRepository
    private lateinit var filmFactsRepository: FilmFactsRepository

    @Before
    fun setup() {
        remoteDataSource = mockk(relaxed = true)
        userDataRepository = mockk(relaxed = true)
        filmFactsRepository = FilmFactsRepository(remoteDataSource, userDataRepository)
    }

    // region Image Urls

    @Test
    fun `When getting poster url returns correctly formatted url`() = runTest {
        val path = "foo"
        val expected = "secureBaseUrl/posterSizes/$path"
        coEvery {
            remoteDataSource.getImageConfiguration()
        } returns stubImageConfiguration

        Assert.assertEquals(expected, filmFactsRepository.getImageUrl(path, ImageType.POSTER))
    }

    @Test
    fun `When getting backdrop url returns correctly formatted url`() = runTest {
        val path = "foo"
        val expected = "secureBaseUrl/backdropSizes/$path"
        coEvery {
            remoteDataSource.getImageConfiguration()
        } returns stubImageConfiguration

        Assert.assertEquals(expected, filmFactsRepository.getImageUrl(path, ImageType.BACKDROP))
    }

    @Test
    fun `When getting profile url returns correctly formatted url`() = runTest {
        val path = "foo"
        val expected = "secureBaseUrl/profileSizes/$path"
        coEvery {
            remoteDataSource.getImageConfiguration()
        } returns stubImageConfiguration

        Assert.assertEquals(expected, filmFactsRepository.getImageUrl(path, ImageType.PROFILE))
    }

    @Test
    fun `When image configuration already loaded does not reload`() = runTest {
        val path = "foo"
        coEvery {
            remoteDataSource.getImageConfiguration()
        } returns stubImageConfiguration

        filmFactsRepository.getImageUrl(path, ImageType.PROFILE)
        filmFactsRepository.getImageUrl(path, ImageType.PROFILE)

        coVerify(exactly = 1) { remoteDataSource.getImageConfiguration() }
    }

    @Test
    fun `When getting unable to get image url returns null`() = runTest {
        val path = "foo"
        coEvery {
            remoteDataSource.getImageConfiguration()
        } returns null

        Assert.assertNull(filmFactsRepository.getImageUrl(path, ImageType.PROFILE))
    }

    // endregion


    // region Get Movies

    @Test
    fun `When account details are unavailable only returns remote movies`() = runTest {
        mockSettingsFlow()
        mockRemoteDataSource()

        val detailsFlow = MutableStateFlow(PendingData.None<AccountDetails>())
        every {
            userDataRepository.accountDetails
        } returns detailsFlow

        Assert.assertEquals(stubRemoteMovies, filmFactsRepository.getMovies())
    }

    @Test
    fun `When specifying release type only returns remote movies`() = runTest {
        mockSettingsFlow()
        mockAccountDetails()
        mockRemoteDataSource()

        Assert.assertEquals(stubRemoteMovies, filmFactsRepository.getMovies(releaseType = DiscoverService.Builder.ReleaseType.PREMIERE))
    }

    @Test
    fun `When forcing settings only returns remote movies`() = runTest {
        val settings = UserSettings()
        mockSettingsFlow(settings)
        mockAccountDetails()
        mockRemoteDataSource()

        Assert.assertEquals(stubRemoteMovies, filmFactsRepository.getMovies(forceSettings = settings))
    }

    @Test
    fun `When specifying cast type only returns remote movies`() = runTest {
        mockSettingsFlow()
        mockAccountDetails()
        mockRemoteDataSource()

        Assert.assertEquals(stubRemoteMovies, filmFactsRepository.getMovies(cast = listOf()))
    }

    @Test
    fun `When specifying unsupported ordering only returns remote movies`() = runTest {
        mockSettingsFlow()
        mockAccountDetails()
        mockRemoteDataSource()

        Assert.assertEquals(stubRemoteMovies, filmFactsRepository.getMovies(order = DiscoverService.Builder.Order.REVENUE_ASC))
    }

    @Test
    fun `When account info available but no user movies only returns remote data`() = runTest {
        mockSettingsFlow()
        mockAccountDetails()
        mockRemoteDataSource()

        Assert.assertEquals(stubRemoteMovies, filmFactsRepository.getMovies())
    }

    @Test
    fun `When account info available but account favorite fails to get data only returns remote data`() = runTest {
        mockSettingsFlow()
        mockAccountDetails(stubAccountDetails(favorite = availablePagedMetaData))
        mockRemoteDataSource()

        coEvery {
            userDataRepository.getAccountFavoriteMovies(any())
        } returns null

        Assert.assertEquals(stubRemoteMovies, filmFactsRepository.getMovies())
    }

    @Test
    fun `When account favorites available returns remote movies and account movies`() = runTest {
        mockSettingsFlow()
        mockAccountDetails(stubAccountDetails(favorite = availablePagedMetaData))
        mockRemoteDataSource()

        coEvery {
            userDataRepository.getAccountFavoriteMovies(any())
        } returns stubAccountMovies

        Assert.assertEquals(
            (stubRemoteMovies.results + stubAccountMovies.results).toSet(),
            filmFactsRepository.getMovies()!!.results.toSet()
        )
    }

    @Test
    fun `When account info available but account rated fails to get data only returns remote data`() = runTest {
        mockSettingsFlow()
        mockAccountDetails(stubAccountDetails(rated = availablePagedMetaData))
        mockRemoteDataSource()

        coEvery {
            userDataRepository.getAccountRatedMovies(any())
        } returns null

        Assert.assertEquals(stubRemoteMovies, filmFactsRepository.getMovies())
    }

    @Test
    fun `When account rated available returns remote movies and account movies`() = runTest {
        mockSettingsFlow()
        mockAccountDetails(stubAccountDetails(rated = availablePagedMetaData))
        mockRemoteDataSource()

        coEvery {
            userDataRepository.getAccountRatedMovies(any())
        } returns stubAccountRatedMovies

        Assert.assertEquals(
            (stubRemoteMovies.results + stubAccountMovies.results).toSet(),
            filmFactsRepository.getMovies()!!.results.toSet()
        )
    }

    @Test
    fun `When account rated movies are rated poorly does not include them in the response`() = runTest {
        mockSettingsFlow()
        mockAccountDetails(stubAccountDetails(rated = availablePagedMetaData))
        mockRemoteDataSource()

        coEvery {
            userDataRepository.getAccountRatedMovies(any())
        } returns stubAccountPoorlyRatedMovies

        Assert.assertEquals(
            (stubRemoteMovies.results).toSet(),
            filmFactsRepository.getMovies()!!.results.toSet()
        )
    }

    @Test
    fun `When account info available but account watchlist fails to get data only returns remote data`() = runTest {
        mockSettingsFlow()
        mockAccountDetails(stubAccountDetails(watchlist = availablePagedMetaData))
        mockRemoteDataSource()

        coEvery {
            userDataRepository.getAccountWatchlistMovies(any())
        } returns null

        Assert.assertEquals(stubRemoteMovies, filmFactsRepository.getMovies())
    }

    @Test
    fun `When account watchlist available returns remote movies and account movies`() = runTest {
        mockSettingsFlow()
        mockAccountDetails(stubAccountDetails(watchlist = availablePagedMetaData))
        mockRemoteDataSource()

        coEvery {
            userDataRepository.getAccountWatchlistMovies(any())
        } returns stubAccountMovies

        Assert.assertEquals(
            (stubRemoteMovies.results + stubAccountMovies.results).toSet(),
            filmFactsRepository.getMovies()!!.results.toSet()
        )
    }

    @Test
    fun `When ordering by ascending popularity with account data orders correctly`() = runTest {
        mockSettingsFlow()
        mockAccountDetails(stubAccountDetails(watchlist = availablePagedMetaData))
        mockRemoteDataSource()

        coEvery {
            userDataRepository.getAccountWatchlistMovies(any())
        } returns stubAccountMovies

        Assert.assertEquals(
            (stubRemoteMovies.results + stubAccountMovies.results).sortedBy { it.popularity },
            filmFactsRepository.getMovies(order = DiscoverService.Builder.Order.POPULARITY_ASC)!!.results
        )
    }

    @Test
    fun `When ordering by descending popularity with account data orders correctly`() = runTest {
        mockSettingsFlow()
        mockAccountDetails(stubAccountDetails(watchlist = availablePagedMetaData))
        mockRemoteDataSource()

        coEvery {
            userDataRepository.getAccountWatchlistMovies(any())
        } returns stubAccountMovies

        Assert.assertEquals(
            (stubRemoteMovies.results + stubAccountMovies.results).sortedByDescending { it.popularity },
            filmFactsRepository.getMovies(order = DiscoverService.Builder.Order.POPULARITY_DESC)!!.results
        )
    }

    @Test
    fun `When ordering by ascending vote average with account data orders correctly`() = runTest {
        mockSettingsFlow()
        mockAccountDetails(stubAccountDetails(watchlist = availablePagedMetaData))
        mockRemoteDataSource()

        coEvery {
            userDataRepository.getAccountWatchlistMovies(any())
        } returns stubAccountMovies

        Assert.assertEquals(
            (stubRemoteMovies.results + stubAccountMovies.results).sortedBy { it.voteAverage },
            filmFactsRepository.getMovies(order = DiscoverService.Builder.Order.VOTE_AVERAGE_ASC)!!.results
        )
    }

    @Test
    fun `When ordering by descending vote average with account data orders correctly`() = runTest {
        mockSettingsFlow()
        mockAccountDetails(stubAccountDetails(watchlist = availablePagedMetaData))
        mockRemoteDataSource()

        coEvery {
            userDataRepository.getAccountWatchlistMovies(any())
        } returns stubAccountMovies

        Assert.assertEquals(
            (stubRemoteMovies.results + stubAccountMovies.results).sortedByDescending { it.voteAverage },
            filmFactsRepository.getMovies(order = DiscoverService.Builder.Order.VOTE_AVERAGE_DESC)!!.results
        )
    }

    @Test
    fun `When ordering by ascending vote count with account data orders correctly`() = runTest {
        mockSettingsFlow()
        mockAccountDetails(stubAccountDetails(watchlist = availablePagedMetaData))
        mockRemoteDataSource()

        coEvery {
            userDataRepository.getAccountWatchlistMovies(any())
        } returns stubAccountMovies

        Assert.assertEquals(
            (stubRemoteMovies.results + stubAccountMovies.results).sortedBy { it.voteCount },
            filmFactsRepository.getMovies(order = DiscoverService.Builder.Order.VOTE_COUNT_ASC)!!.results
        )
    }

    @Test
    fun `When ordering by descending vote count with account data orders correctly`() = runTest {
        mockSettingsFlow()
        mockAccountDetails(stubAccountDetails(watchlist = availablePagedMetaData))
        mockRemoteDataSource()

        coEvery {
            userDataRepository.getAccountWatchlistMovies(any())
        } returns stubAccountMovies

        Assert.assertEquals(
            (stubRemoteMovies.results + stubAccountMovies.results).sortedByDescending { it.voteCount },
            filmFactsRepository.getMovies(order = DiscoverService.Builder.Order.VOTE_COUNT_DESC)!!.results
        )
    }

    @Test
    fun `When ordering by ascending release date with account data orders correctly`() = runTest {
        mockSettingsFlow()
        mockAccountDetails(stubAccountDetails(watchlist = availablePagedMetaData))
        mockRemoteDataSource()

        coEvery {
            userDataRepository.getAccountWatchlistMovies(any())
        } returns stubAccountMovies

        Assert.assertEquals(
            (stubRemoteMovies.results + stubAccountMovies.results).sortedBy { it.releaseDate },
            filmFactsRepository.getMovies(order = DiscoverService.Builder.Order.RELEASE_DATE_ASC)!!.results
        )
    }

    @Test
    fun `When ordering by descending release date with account data orders correctly`() = runTest {
        mockSettingsFlow()
        mockAccountDetails(stubAccountDetails(watchlist = availablePagedMetaData))
        mockRemoteDataSource()

        coEvery {
            userDataRepository.getAccountWatchlistMovies(any())
        } returns stubAccountMovies

        Assert.assertEquals(
            (stubRemoteMovies.results + stubAccountMovies.results).sortedByDescending { it.releaseDate },
            filmFactsRepository.getMovies(order = DiscoverService.Builder.Order.RELEASE_DATE_DESC)!!.results
        )
    }

    @Test
    fun `When ordering by ascending revenue with account data excludes account data`() = runTest {
        mockSettingsFlow()
        mockAccountDetails(stubAccountDetails(watchlist = availablePagedMetaData))
        mockRemoteDataSource()

        coEvery {
            userDataRepository.getAccountWatchlistMovies(any())
        } returns stubAccountMovies

        Assert.assertEquals(
            stubRemoteMovies.results,
            filmFactsRepository.getMovies(order = DiscoverService.Builder.Order.REVENUE_ASC)!!.results
        )
    }

    @Test
    fun `When ordering by descending revenue with account data excludes account data`() = runTest {
        mockSettingsFlow()
        mockAccountDetails(stubAccountDetails(watchlist = availablePagedMetaData))
        mockRemoteDataSource()

        coEvery {
            userDataRepository.getAccountWatchlistMovies(any())
        } returns stubAccountMovies

        filmFactsRepository.getMovies(order = DiscoverService.Builder.Order.REVENUE_DESC)!!.results

        Assert.assertEquals(
            stubRemoteMovies.results,
            filmFactsRepository.getMovies(order = DiscoverService.Builder.Order.REVENUE_DESC)!!.results
        )
    }

    // endregion


    // region Movie Info

    @Test
    fun `When getting movie credits delegates to remote datasource`() = runTest {
        coEvery {
            remoteDataSource.getMovieCredits(any())
        } returns mockk()

        filmFactsRepository.getMovieCredits(0)

        coVerify { remoteDataSource.getMovieCredits(any()) }
    }

    @Test
    fun `When getting movie details delegates to remote datasource`() = runTest {
        coEvery {
            remoteDataSource.getMovieDetails(any())
        } returns mockk()

        filmFactsRepository.getMovieDetails(0)

        coVerify { remoteDataSource.getMovieDetails(any()) }
    }

    @Test
    fun `When getting movie images delegates to remote datasource`() = runTest {
        coEvery {
            remoteDataSource.getMovieImages(any())
        } returns mockk()

        filmFactsRepository.getMovieImages(0)

        coVerify { remoteDataSource.getMovieImages(any()) }
    }

    // endregion


    // region Actor Info

    @Test
    fun `When getting actor details delegates to remote datasource`() = runTest {
        coEvery {
            remoteDataSource.getActorDetails(any())
        } returns mockk()

        filmFactsRepository.getActorDetails(0)

        coVerify { remoteDataSource.getActorDetails(any()) }
    }

    @Test
    fun `When getting actor credits delegates to remote datasource`() = runTest {
        coEvery {
            remoteDataSource.getActorCredits(any())
        } returns mockk()

        filmFactsRepository.getActorCredits(0)

        coVerify { remoteDataSource.getActorCredits(any()) }
    }

    // endregion


    // region Util methods

    private fun mockSettingsFlow(settings: UserSettings = UserSettings()) {
        val settingsFlow = MutableStateFlow(settings)
        every {
            userDataRepository.userSettings
        } returns settingsFlow
    }

    private fun mockAccountDetails(details: AccountDetails = stubAccountDetails()) {
        val detailsFlow = MutableStateFlow(PendingData.Success(details))
        every {
            userDataRepository.accountDetails
        } returns detailsFlow
    }

    private fun mockRemoteDataSource(response: DiscoverMovieResponse = stubRemoteMovies) {
        coEvery {
            remoteDataSource.getMovies(any(), any(), any(), any(), any(), any(), any(), any(), any())
        } returns response
    }

    private fun stubAccountDetails(
        favorite: PagedMetaData = emptyPagedMetaData,
        rated: PagedMetaData = emptyPagedMetaData,
        watchlist: PagedMetaData = emptyPagedMetaData
    ) = AccountDetails(
        0,
        "",
        "",
        favorite,
        rated,
        watchlist,
        ""
    )

    // endregion

    private companion object {
        val stubImageConfiguration = ImageConfiguration(
            "baseUrl/",
            "secureBaseUrl/",
            listOf("backdropSizes/"),
            listOf("logoSizes/"),
            listOf("posterSizes/"),
            listOf("profileSizes/"),
            listOf("stillSizes/")
        )

        val stubRemoteMovies = DiscoverMovieResponse(
            0,
            listOf(
                stubMovie(0, 0),
                stubMovie(1,1),
                stubMovie(2,2),
                stubMovie(3, 3),
                stubMovie( 4, 4)
            ),
            1,
            5
        )

        val stubAccountMovies = AccountMoviesResponse(
            0,
            listOf(
                stubMovie(5, 5),
                stubMovie(6,6),
                stubMovie(7,7)
            ),
            1,
            3
        )

        val stubAccountRatedMovies = AccountRatedMoviesResponse(
            0,
            listOf(
                stubRatedMovie(5, 5, 7),
                stubRatedMovie(6,6, 7),
                stubRatedMovie(7,7, 7)
            ),
            1,
            3
        )

        val stubAccountPoorlyRatedMovies = AccountRatedMoviesResponse(
            0,
            listOf(
                stubRatedMovie(5, 5, 0),
                stubRatedMovie(6,6, 0),
                stubRatedMovie(7,7, 0)
            ),
            1,
            3
        )

        fun stubMovie(id: Int, genre: Int) =
            DiscoverMovie(
                id,
                "foo",
                "",
                listOf(genre),
                "",
                UserSettings().language,
                0f,
                0,
                0f
            )

        fun stubRatedMovie(id: Int, genre: Int, rating: Int) =
            RatedMovie(
                id,
                "foo",
                "",
                listOf(genre),
                "",
                UserSettings().language,
                0f,
                0,
                0f,
                rating
            )

        val emptyPagedMetaData = PagedMetaData(0, 0)
        val availablePagedMetaData = PagedMetaData(1, 1)
    }
}
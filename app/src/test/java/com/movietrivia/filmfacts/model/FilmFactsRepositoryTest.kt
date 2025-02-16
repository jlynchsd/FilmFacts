package com.movietrivia.filmfacts.model

import com.movietrivia.filmfacts.api.AccountMoviesResponse
import com.movietrivia.filmfacts.api.AccountRatedMoviesResponse
import com.movietrivia.filmfacts.api.AccountRatedTvShowsResponse
import com.movietrivia.filmfacts.api.AccountTvShowsResponse
import com.movietrivia.filmfacts.api.DiscoverMovie
import com.movietrivia.filmfacts.api.DiscoverMovieResponse
import com.movietrivia.filmfacts.api.DiscoverService
import com.movietrivia.filmfacts.api.DiscoverTvShowResponse
import com.movietrivia.filmfacts.api.ImageConfiguration
import com.movietrivia.filmfacts.api.RatedMovie
import com.movietrivia.filmfacts.api.RatedTvShow
import com.movietrivia.filmfacts.domain.stubDiscoverTvShow
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
        mockMovieSettingsFlow()
        mockRemoteMoviesDataSource()

        val detailsFlow = MutableStateFlow(PendingData.None<AccountDetails>())
        every {
            userDataRepository.accountDetails
        } returns detailsFlow

        Assert.assertEquals(stubRemoteMovies, filmFactsRepository.getMovies())
    }

    @Test
    fun `When specifying release type only returns remote movies`() = runTest {
        mockMovieSettingsFlow()
        mockAccountDetails()
        mockRemoteMoviesDataSource()

        Assert.assertEquals(stubRemoteMovies, filmFactsRepository.getMovies(releaseType = DiscoverService.Builder.ReleaseType.PREMIERE))
    }

    @Test
    fun `When forcing settings only returns remote movies`() = runTest {
        val settings = UserSettings()
        mockMovieSettingsFlow(settings)
        mockAccountDetails()
        mockRemoteMoviesDataSource()

        Assert.assertEquals(stubRemoteMovies, filmFactsRepository.getMovies(forceSettings = settings))
    }

    @Test
    fun `When specifying cast type only returns remote movies`() = runTest {
        mockMovieSettingsFlow()
        mockAccountDetails()
        mockRemoteMoviesDataSource()

        Assert.assertEquals(stubRemoteMovies, filmFactsRepository.getMovies(cast = listOf()))
    }

    @Test
    fun `When specifying unsupported ordering only returns remote movies`() = runTest {
        mockMovieSettingsFlow()
        mockAccountDetails()
        mockRemoteMoviesDataSource()

        Assert.assertEquals(stubRemoteMovies, filmFactsRepository.getMovies(movieOrder = DiscoverService.Builder.MovieOrder.REVENUE_ASC))
    }

    @Test
    fun `When account info available but no user movies only returns remote data`() = runTest {
        mockMovieSettingsFlow()
        mockAccountDetails()
        mockRemoteMoviesDataSource()

        Assert.assertEquals(stubRemoteMovies, filmFactsRepository.getMovies())
    }

    @Test
    fun `When account info available but account favorite fails to get data only returns remote data`() = runTest {
        mockMovieSettingsFlow()
        mockAccountDetails(stubAccountDetails(favoriteMovies = availablePagedMetaData))
        mockRemoteMoviesDataSource()

        coEvery {
            userDataRepository.getAccountFavoriteMovies(any())
        } returns null

        Assert.assertEquals(stubRemoteMovies, filmFactsRepository.getMovies())
    }

    @Test
    fun `When account favorites available returns remote movies and account movies`() = runTest {
        mockMovieSettingsFlow()
        mockAccountDetails(stubAccountDetails(favoriteMovies = availablePagedMetaData))
        mockRemoteMoviesDataSource()

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
        mockMovieSettingsFlow()
        mockAccountDetails(stubAccountDetails(ratedMovies = availablePagedMetaData))
        mockRemoteMoviesDataSource()

        coEvery {
            userDataRepository.getAccountRatedMovies(any())
        } returns null

        Assert.assertEquals(stubRemoteMovies, filmFactsRepository.getMovies())
    }

    @Test
    fun `When account rated available returns remote movies and account movies`() = runTest {
        mockMovieSettingsFlow()
        mockAccountDetails(stubAccountDetails(ratedMovies = availablePagedMetaData))
        mockRemoteMoviesDataSource()

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
        mockMovieSettingsFlow()
        mockAccountDetails(stubAccountDetails(ratedMovies = availablePagedMetaData))
        mockRemoteMoviesDataSource()

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
        mockMovieSettingsFlow()
        mockAccountDetails(stubAccountDetails(watchlistMovies = availablePagedMetaData))
        mockRemoteMoviesDataSource()

        coEvery {
            userDataRepository.getAccountWatchlistMovies(any())
        } returns null

        Assert.assertEquals(stubRemoteMovies, filmFactsRepository.getMovies())
    }

    @Test
    fun `When account watchlist available returns remote movies and account movies`() = runTest {
        mockMovieSettingsFlow()
        mockAccountDetails(stubAccountDetails(watchlistMovies = availablePagedMetaData))
        mockRemoteMoviesDataSource()

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
        mockMovieSettingsFlow()
        mockAccountDetails(stubAccountDetails(watchlistMovies = availablePagedMetaData))
        mockRemoteMoviesDataSource()

        coEvery {
            userDataRepository.getAccountWatchlistMovies(any())
        } returns stubAccountMovies

        Assert.assertEquals(
            (stubRemoteMovies.results + stubAccountMovies.results).sortedBy { it.popularity },
            filmFactsRepository.getMovies(movieOrder = DiscoverService.Builder.MovieOrder.POPULARITY_ASC)!!.results
        )
    }

    @Test
    fun `When ordering by descending popularity with account data orders correctly`() = runTest {
        mockMovieSettingsFlow()
        mockAccountDetails(stubAccountDetails(watchlistMovies = availablePagedMetaData))
        mockRemoteMoviesDataSource()

        coEvery {
            userDataRepository.getAccountWatchlistMovies(any())
        } returns stubAccountMovies

        Assert.assertEquals(
            (stubRemoteMovies.results + stubAccountMovies.results).sortedByDescending { it.popularity },
            filmFactsRepository.getMovies(movieOrder = DiscoverService.Builder.MovieOrder.POPULARITY_DESC)!!.results
        )
    }

    @Test
    fun `When ordering by ascending vote average with account data orders correctly`() = runTest {
        mockMovieSettingsFlow()
        mockAccountDetails(stubAccountDetails(watchlistMovies = availablePagedMetaData))
        mockRemoteMoviesDataSource()

        coEvery {
            userDataRepository.getAccountWatchlistMovies(any())
        } returns stubAccountMovies

        Assert.assertEquals(
            (stubRemoteMovies.results + stubAccountMovies.results).sortedBy { it.voteAverage },
            filmFactsRepository.getMovies(movieOrder = DiscoverService.Builder.MovieOrder.VOTE_AVERAGE_ASC)!!.results
        )
    }

    @Test
    fun `When ordering by descending vote average with account data orders correctly`() = runTest {
        mockMovieSettingsFlow()
        mockAccountDetails(stubAccountDetails(watchlistMovies = availablePagedMetaData))
        mockRemoteMoviesDataSource()

        coEvery {
            userDataRepository.getAccountWatchlistMovies(any())
        } returns stubAccountMovies

        Assert.assertEquals(
            (stubRemoteMovies.results + stubAccountMovies.results).sortedByDescending { it.voteAverage },
            filmFactsRepository.getMovies(movieOrder = DiscoverService.Builder.MovieOrder.VOTE_AVERAGE_DESC)!!.results
        )
    }

    @Test
    fun `When ordering by ascending vote count with account data orders correctly`() = runTest {
        mockMovieSettingsFlow()
        mockAccountDetails(stubAccountDetails(watchlistMovies = availablePagedMetaData))
        mockRemoteMoviesDataSource()

        coEvery {
            userDataRepository.getAccountWatchlistMovies(any())
        } returns stubAccountMovies

        Assert.assertEquals(
            (stubRemoteMovies.results + stubAccountMovies.results).sortedBy { it.voteCount },
            filmFactsRepository.getMovies(movieOrder = DiscoverService.Builder.MovieOrder.VOTE_COUNT_ASC)!!.results
        )
    }

    @Test
    fun `When ordering by descending vote count with account data orders correctly`() = runTest {
        mockMovieSettingsFlow()
        mockAccountDetails(stubAccountDetails(watchlistMovies = availablePagedMetaData))
        mockRemoteMoviesDataSource()

        coEvery {
            userDataRepository.getAccountWatchlistMovies(any())
        } returns stubAccountMovies

        Assert.assertEquals(
            (stubRemoteMovies.results + stubAccountMovies.results).sortedByDescending { it.voteCount },
            filmFactsRepository.getMovies(movieOrder = DiscoverService.Builder.MovieOrder.VOTE_COUNT_DESC)!!.results
        )
    }

    @Test
    fun `When ordering by ascending release date with account data orders correctly`() = runTest {
        mockMovieSettingsFlow()
        mockAccountDetails(stubAccountDetails(watchlistMovies = availablePagedMetaData))
        mockRemoteMoviesDataSource()

        coEvery {
            userDataRepository.getAccountWatchlistMovies(any())
        } returns stubAccountMovies

        Assert.assertEquals(
            (stubRemoteMovies.results + stubAccountMovies.results).sortedBy { it.releaseDate },
            filmFactsRepository.getMovies(movieOrder = DiscoverService.Builder.MovieOrder.RELEASE_DATE_ASC)!!.results
        )
    }

    @Test
    fun `When ordering by descending release date with account data orders correctly`() = runTest {
        mockMovieSettingsFlow()
        mockAccountDetails(stubAccountDetails(watchlistMovies = availablePagedMetaData))
        mockRemoteMoviesDataSource()

        coEvery {
            userDataRepository.getAccountWatchlistMovies(any())
        } returns stubAccountMovies

        Assert.assertEquals(
            (stubRemoteMovies.results + stubAccountMovies.results).sortedByDescending { it.releaseDate },
            filmFactsRepository.getMovies(movieOrder = DiscoverService.Builder.MovieOrder.RELEASE_DATE_DESC)!!.results
        )
    }

    @Test
    fun `When ordering by ascending revenue with account data excludes account data`() = runTest {
        mockMovieSettingsFlow()
        mockAccountDetails(stubAccountDetails(watchlistMovies = availablePagedMetaData))
        mockRemoteMoviesDataSource()

        coEvery {
            userDataRepository.getAccountWatchlistMovies(any())
        } returns stubAccountMovies

        Assert.assertEquals(
            stubRemoteMovies.results,
            filmFactsRepository.getMovies(movieOrder = DiscoverService.Builder.MovieOrder.REVENUE_ASC)!!.results
        )
    }

    @Test
    fun `When ordering by descending revenue with account data excludes account data`() = runTest {
        mockMovieSettingsFlow()
        mockAccountDetails(stubAccountDetails(watchlistMovies = availablePagedMetaData))
        mockRemoteMoviesDataSource()

        coEvery {
            userDataRepository.getAccountWatchlistMovies(any())
        } returns stubAccountMovies

        filmFactsRepository.getMovies(movieOrder = DiscoverService.Builder.MovieOrder.REVENUE_DESC)!!.results

        Assert.assertEquals(
            stubRemoteMovies.results,
            filmFactsRepository.getMovies(movieOrder = DiscoverService.Builder.MovieOrder.REVENUE_DESC)!!.results
        )
    }

    // endregion


    // region Get Tv Shows

    @Test
    fun `When account details are unavailable only returns remote tv shows`() = runTest {
        mockTvShowsSettingsFlow()
        mockRemoteTvShowsDataSource()

        val detailsFlow = MutableStateFlow(PendingData.None<AccountDetails>())
        every {
            userDataRepository.accountDetails
        } returns detailsFlow

        Assert.assertEquals(stubRemoteTvShows, filmFactsRepository.getTvShows())
    }

    @Test
    fun `When forcing settings only returns remote tv shows`() = runTest {
        val settings = UserSettings()
        mockTvShowsSettingsFlow(settings)
        mockAccountDetails()
        mockRemoteTvShowsDataSource()

        Assert.assertEquals(stubRemoteTvShows, filmFactsRepository.getTvShows(forceSettings = settings))
    }

    @Test
    fun `When account info available but no user tv shows only returns remote data`() = runTest {
        mockTvShowsSettingsFlow()
        mockAccountDetails()
        mockRemoteTvShowsDataSource()

        Assert.assertEquals(stubRemoteTvShows, filmFactsRepository.getTvShows())
    }

    @Test
    fun `When account info available but account favorite tv shows fails to get data only returns remote data`() = runTest {
        mockTvShowsSettingsFlow()
        mockAccountDetails(stubAccountDetails(favoriteTvShows = availablePagedMetaData))
        mockRemoteTvShowsDataSource()

        coEvery {
            userDataRepository.getAccountFavoriteTvShows(any())
        } returns null

        Assert.assertEquals(stubRemoteTvShows, filmFactsRepository.getTvShows())
    }

    @Test
    fun `When account favorites available returns remote tv shows and account tv shows`() = runTest {
        mockTvShowsSettingsFlow()
        mockAccountDetails(stubAccountDetails(favoriteTvShows = availablePagedMetaData))
        mockRemoteTvShowsDataSource()

        coEvery {
            userDataRepository.getAccountFavoriteTvShows(any())
        } returns stubAccountTvShows

        Assert.assertEquals(
            (stubRemoteTvShows.results + stubAccountTvShows.results).toSet(),
            filmFactsRepository.getTvShows()!!.results.toSet()
        )
    }

    @Test
    fun `When account info available but account rated tv shows fails to get data only returns remote data`() = runTest {
        mockTvShowsSettingsFlow()
        mockAccountDetails(stubAccountDetails(ratedTvShows = availablePagedMetaData))
        mockRemoteTvShowsDataSource()

        coEvery {
            userDataRepository.getAccountRatedTvShows(any())
        } returns null

        Assert.assertEquals(stubRemoteTvShows, filmFactsRepository.getTvShows())
    }

    @Test
    fun `When account rated available returns remote tv shows and account tv shows`() = runTest {
        mockTvShowsSettingsFlow()
        mockAccountDetails(stubAccountDetails(ratedTvShows = availablePagedMetaData))
        mockRemoteTvShowsDataSource()

        coEvery {
            userDataRepository.getAccountRatedTvShows(any())
        } returns stubAccountRatedTvShows

        Assert.assertEquals(
            (stubRemoteTvShows.results + stubAccountTvShows.results).toSet(),
            filmFactsRepository.getTvShows()!!.results.toSet()
        )
    }

    @Test
    fun `When account rated tv shows are rated poorly does not include them in the response`() = runTest {
        mockTvShowsSettingsFlow()
        mockAccountDetails(stubAccountDetails(ratedTvShows = availablePagedMetaData))
        mockRemoteTvShowsDataSource()

        coEvery {
            userDataRepository.getAccountRatedTvShows(any())
        } returns stubAccountPoorlyRatedTvShows

        Assert.assertEquals(
            (stubRemoteTvShows.results).toSet(),
            filmFactsRepository.getTvShows()!!.results.toSet()
        )
    }

    @Test
    fun `When account info available but account watchlist tv shows fails to get data only returns remote data`() = runTest {
        mockTvShowsSettingsFlow()
        mockAccountDetails(stubAccountDetails(watchlistTvShows = availablePagedMetaData))
        mockRemoteTvShowsDataSource()

        coEvery {
            userDataRepository.getAccountWatchlistTvShows(any())
        } returns null

        Assert.assertEquals(stubRemoteTvShows, filmFactsRepository.getTvShows())
    }

    @Test
    fun `When account watchlist available returns remote tv shows and account tv shows`() = runTest {
        mockTvShowsSettingsFlow()
        mockAccountDetails(stubAccountDetails(watchlistTvShows = availablePagedMetaData))
        mockRemoteTvShowsDataSource()

        coEvery {
            userDataRepository.getAccountWatchlistTvShows(any())
        } returns stubAccountTvShows

        Assert.assertEquals(
            (stubRemoteTvShows.results + stubAccountTvShows.results).toSet(),
            filmFactsRepository.getTvShows()!!.results.toSet()
        )
    }

    @Test
    fun `When ordering by ascending popularity with account tv shows data orders correctly`() = runTest {
        mockTvShowsSettingsFlow()
        mockAccountDetails(stubAccountDetails(watchlistTvShows = availablePagedMetaData))
        mockRemoteTvShowsDataSource()

        coEvery {
            userDataRepository.getAccountWatchlistTvShows(any())
        } returns stubAccountTvShows

        Assert.assertEquals(
            (stubRemoteTvShows.results + stubAccountTvShows.results).sortedBy { it.popularity },
            filmFactsRepository.getTvShows(tvShowOrder = DiscoverService.Builder.TvShowOrder.POPULARITY_ASC)!!.results
        )
    }

    @Test
    fun `When ordering by descending popularity with account tv shows data orders correctly`() = runTest {
        mockTvShowsSettingsFlow()
        mockAccountDetails(stubAccountDetails(watchlistTvShows = availablePagedMetaData))
        mockRemoteTvShowsDataSource()

        coEvery {
            userDataRepository.getAccountWatchlistTvShows(any())
        } returns stubAccountTvShows

        Assert.assertEquals(
            (stubRemoteTvShows.results + stubAccountTvShows.results).sortedByDescending { it.popularity },
            filmFactsRepository.getTvShows(tvShowOrder = DiscoverService.Builder.TvShowOrder.POPULARITY_DESC)!!.results
        )
    }

    @Test
    fun `When ordering by ascending vote average with account tv shows data orders correctly`() = runTest {
        mockTvShowsSettingsFlow()
        mockAccountDetails(stubAccountDetails(watchlistTvShows = availablePagedMetaData))
        mockRemoteTvShowsDataSource()

        coEvery {
            userDataRepository.getAccountWatchlistTvShows(any())
        } returns stubAccountTvShows

        Assert.assertEquals(
            (stubRemoteTvShows.results + stubAccountTvShows.results).sortedBy { it.voteAverage },
            filmFactsRepository.getTvShows(tvShowOrder = DiscoverService.Builder.TvShowOrder.VOTE_AVERAGE_ASC)!!.results
        )
    }

    @Test
    fun `When ordering by descending vote average with account tv shows data orders correctly`() = runTest {
        mockTvShowsSettingsFlow()
        mockAccountDetails(stubAccountDetails(watchlistTvShows = availablePagedMetaData))
        mockRemoteTvShowsDataSource()

        coEvery {
            userDataRepository.getAccountWatchlistTvShows(any())
        } returns stubAccountTvShows

        Assert.assertEquals(
            (stubRemoteTvShows.results + stubAccountTvShows.results).sortedByDescending { it.voteAverage },
            filmFactsRepository.getTvShows(tvShowOrder = DiscoverService.Builder.TvShowOrder.VOTE_AVERAGE_DESC)!!.results
        )
    }

    @Test
    fun `When ordering by ascending vote count with account tv shows data orders correctly`() = runTest {
        mockTvShowsSettingsFlow()
        mockAccountDetails(stubAccountDetails(watchlistTvShows = availablePagedMetaData))
        mockRemoteTvShowsDataSource()

        coEvery {
            userDataRepository.getAccountWatchlistTvShows(any())
        } returns stubAccountTvShows

        Assert.assertEquals(
            (stubRemoteTvShows.results + stubAccountTvShows.results).sortedBy { it.voteCount },
            filmFactsRepository.getTvShows(tvShowOrder = DiscoverService.Builder.TvShowOrder.VOTE_COUNT_ASC)!!.results
        )
    }

    @Test
    fun `When ordering by descending vote count with account tv shows data orders correctly`() = runTest {
        mockTvShowsSettingsFlow()
        mockAccountDetails(stubAccountDetails(watchlistTvShows = availablePagedMetaData))
        mockRemoteTvShowsDataSource()

        coEvery {
            userDataRepository.getAccountWatchlistTvShows(any())
        } returns stubAccountTvShows

        Assert.assertEquals(
            (stubRemoteTvShows.results + stubAccountTvShows.results).sortedByDescending { it.voteCount },
            filmFactsRepository.getTvShows(tvShowOrder = DiscoverService.Builder.TvShowOrder.VOTE_COUNT_DESC)!!.results
        )
    }

    @Test
    fun `When ordering by ascending release date with account tv shows data orders correctly`() = runTest {
        mockTvShowsSettingsFlow()
        mockAccountDetails(stubAccountDetails(watchlistTvShows = availablePagedMetaData))
        mockRemoteTvShowsDataSource()

        coEvery {
            userDataRepository.getAccountWatchlistTvShows(any())
        } returns stubAccountTvShows

        Assert.assertEquals(
            (stubRemoteTvShows.results + stubAccountTvShows.results).sortedBy { it.firstAirDate },
            filmFactsRepository.getTvShows(tvShowOrder = DiscoverService.Builder.TvShowOrder.FIRST_AIR_DATE_ASC)!!.results
        )
    }

    @Test
    fun `When ordering by descending release date with account tv shows data orders correctly`() = runTest {
        mockTvShowsSettingsFlow()
        mockAccountDetails(stubAccountDetails(watchlistTvShows = availablePagedMetaData))
        mockRemoteTvShowsDataSource()

        coEvery {
            userDataRepository.getAccountWatchlistTvShows(any())
        } returns stubAccountTvShows

        Assert.assertEquals(
            (stubRemoteTvShows.results + stubAccountTvShows.results).sortedByDescending { it.firstAirDate },
            filmFactsRepository.getTvShows(tvShowOrder = DiscoverService.Builder.TvShowOrder.FIRST_AIR_DATE_DESC)!!.results
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


    // region Tv Show Info

    @Test
    fun `When getting tv show credits delegates to remote datasource`() = runTest {
        coEvery {
            remoteDataSource.getTvShowCredits(any())
        } returns mockk()

        filmFactsRepository.getTvShowCredits(0)

        coVerify { remoteDataSource.getTvShowCredits(any()) }
    }

    @Test
    fun `When getting tv show details delegates to remote datasource`() = runTest {
        coEvery {
            remoteDataSource.getTvShowDetails(any())
        } returns mockk()

        filmFactsRepository.getTvShowDetails(0)

        coVerify { remoteDataSource.getTvShowDetails(any()) }
    }

    @Test
    fun `When getting tv show images delegates to remote datasource`() = runTest {
        coEvery {
            remoteDataSource.getTvShowImages(any())
        } returns mockk()

        filmFactsRepository.getTvShowImages(0)

        coVerify { remoteDataSource.getTvShowImages(any()) }
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
    fun `When getting actor movie credits delegates to remote datasource`() = runTest {
        coEvery {
            remoteDataSource.getActorMovieCredits(any())
        } returns mockk()

        filmFactsRepository.getActorMovieCredits(0)

        coVerify { remoteDataSource.getActorMovieCredits(any()) }
    }

    @Test
    fun `When getting actor tv show credits delegates to remote datasource`() = runTest {
        coEvery {
            remoteDataSource.getActorTvShowCredits(any())
        } returns mockk()

        filmFactsRepository.getActorTvShowCredits(0)

        coVerify { remoteDataSource.getActorTvShowCredits(any()) }
    }

    // endregion


    // region Util methods

    private fun mockMovieSettingsFlow(settings: UserSettings = UserSettings()) {
        val settingsFlow = MutableStateFlow(settings)
        every {
            userDataRepository.movieUserSettings
        } returns settingsFlow
    }

    private fun mockTvShowsSettingsFlow(settings: UserSettings = UserSettings()) {
        val settingsFlow = MutableStateFlow(settings)
        every {
            userDataRepository.tvShowUserSettings
        } returns settingsFlow
    }

    private fun mockAccountDetails(details: AccountDetails = stubAccountDetails()) {
        val detailsFlow = MutableStateFlow(PendingData.Success(details))
        every {
            userDataRepository.accountDetails
        } returns detailsFlow
    }

    private fun mockRemoteMoviesDataSource(response: DiscoverMovieResponse = stubRemoteMovies) {
        coEvery {
            remoteDataSource.getMovies(any(), any(), any(), any(), any(), any(), any(), any(), any())
        } returns response
    }

    private fun mockRemoteTvShowsDataSource(response: DiscoverTvShowResponse = stubRemoteTvShows) {
        coEvery {
            remoteDataSource.getTvShows(any(), any(), any(), any(), any(), any(), any())
        } returns response
    }

    private fun stubAccountDetails(
        favoriteMovies: PagedMetaData = emptyPagedMetaData,
        ratedMovies: PagedMetaData = emptyPagedMetaData,
        watchlistMovies: PagedMetaData = emptyPagedMetaData,
        favoriteTvShows: PagedMetaData = emptyPagedMetaData,
        ratedTvShows: PagedMetaData = emptyPagedMetaData,
        watchlistTvShows: PagedMetaData = emptyPagedMetaData
    ) = AccountDetails(
        0,
        "",
        "",
        favoriteMovies,
        ratedMovies,
        watchlistMovies,
        favoriteTvShows,
        ratedTvShows,
        watchlistTvShows,
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

        val stubRemoteTvShows = DiscoverTvShowResponse(
            0,
            listOf(
                stubDiscoverTvShow(id = 0, genreIds = listOf(0)),
                stubDiscoverTvShow(id = 1, genreIds = listOf(1)),
                stubDiscoverTvShow(id = 2, genreIds = listOf(2)),
                stubDiscoverTvShow(id = 3, genreIds = listOf(3)),
                stubDiscoverTvShow(id = 4, genreIds = listOf(4)),
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

        val stubAccountTvShows = AccountTvShowsResponse(
            0,
            listOf(
                stubDiscoverTvShow(id = 5, genreIds = listOf(5)),
                stubDiscoverTvShow(id = 6, genreIds = listOf(6)),
                stubDiscoverTvShow(id = 7, genreIds = listOf(7))
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

        val stubAccountRatedTvShows = AccountRatedTvShowsResponse(
            0,
            listOf(
                stubRatedTvShow(5, listOf(5), 7),
                stubRatedTvShow( 6, listOf(6), 7),
                stubRatedTvShow(7, listOf(7), 7)
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

        val stubAccountPoorlyRatedTvShows = AccountRatedTvShowsResponse(
            0,
            listOf(
                stubRatedTvShow(5, listOf(5), 0),
                stubRatedTvShow( 6, listOf(6), 0),
                stubRatedTvShow(7, listOf(7), 0)
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

        fun stubRatedTvShow(id: Int, genres: List<Int>, rating: Int) =
            RatedTvShow(
                id,
                "",
                "fooPath",
                genres,
                "2000-01-01",
                emptyList(),
                "en",
                "",
                "",
                0f,
                0,
                0f,
                rating
            )

        val emptyPagedMetaData = PagedMetaData(0, 0)
        val availablePagedMetaData = PagedMetaData(1, 1)
    }
}
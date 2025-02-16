package com.movietrivia.filmfacts.model

import com.movietrivia.filmfacts.api.AccountAvatar
import com.movietrivia.filmfacts.api.AccountDetailsResponse
import com.movietrivia.filmfacts.api.AccountMoviesResponse
import com.movietrivia.filmfacts.api.AccountRatedMoviesResponse
import com.movietrivia.filmfacts.api.AccountRatedTvShowsResponse
import com.movietrivia.filmfacts.api.AccountTvShowsResponse
import com.movietrivia.filmfacts.api.Gravatar
import com.movietrivia.filmfacts.api.NewSessionResponse
import com.movietrivia.filmfacts.domain.preloadImage
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.runs
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test

class UserDataRepositoryTest {

    private lateinit var authenticationRepository: AuthenticationRepository
    private lateinit var movieUserSettingsDataSource: UserSettingsDataSource
    private lateinit var tvShowUserSettingsDataSource: UserSettingsDataSource
    private lateinit var sessionDataSource: SessionDataSource
    private lateinit var accountDataSource: AccountDataSource
    private lateinit var userDataRepository: UserDataRepository

    private lateinit var sessionIdFlow: MutableStateFlow<String?>

    @Before
    fun setup() {
        authenticationRepository = mockk(relaxed = true)
        movieUserSettingsDataSource = mockk(relaxed = true)
        tvShowUserSettingsDataSource = mockk(relaxed = true)
        sessionDataSource = mockk(relaxed = true)
        accountDataSource = mockk(relaxed = true)
        userDataRepository = UserDataRepository(mockk(), authenticationRepository, movieUserSettingsDataSource, tvShowUserSettingsDataSource, sessionDataSource, accountDataSource)

        sessionIdFlow = MutableStateFlow(null)
        every {
            sessionDataSource.sessionId
        } returns sessionIdFlow

        mockkStatic(::preloadImage)
        every {
            preloadImage(any(), any())
        } just runs
    }

    @After
    fun teardown() {
        clearAllMocks()
    }

    // region Account Details

    @Test
    fun `When loading account details but no session loads error`() = runTest {
        userDataRepository.loadAccountDetails()

        Assert.assertTrue(userDataRepository.accountDetails.value is PendingData.Error)
    }

    @Test
    fun `When loading account details but unable to get remote data loads error`() = runTest {
        sessionIdFlow.value = "foo"
        coEvery {
            accountDataSource.getAccountDetails(any())
        } returns RemoteData.Error(RemoteDataError.OTHER)

        userDataRepository.loadAccountDetails()

        Assert.assertTrue(userDataRepository.accountDetails.value is PendingData.Error)
    }

    @Test
    fun `When loading account details but denied remote data loads error and deletes session`() = runTest {
        sessionIdFlow.value = "foo"
        coEvery {
            accountDataSource.getAccountDetails(any())
        } returns RemoteData.Error(RemoteDataError.DENIED)

        userDataRepository.loadAccountDetails()

        Assert.assertTrue(userDataRepository.accountDetails.value is PendingData.Error)

        coVerify { authenticationRepository.deleteSession(any()) }
        coVerify { sessionDataSource.clearSessionId() }
    }

    // endregion


    // region Movies

    @Test
    fun `When loading account movie details but unable to get metadata uses default data`() = runTest {
        sessionIdFlow.value = "foo"
        coEvery {
            accountDataSource.getAccountFavoriteMovies(any(), any(), any())
        } returns null
        coEvery {
            accountDataSource.getAccountRatedMovies(any(), any(), any())
        } returns null
        coEvery {
            accountDataSource.getAccountWatchlistMovies(any(), any(), any())
        } returns null
        coEvery {
            accountDataSource.getAccountDetails(any())
        } returns RemoteData.Success(
            AccountDetailsResponse(0, "", "", AccountAvatar(Gravatar("fizz")))
        )

        userDataRepository.loadAccountDetails()

        Assert.assertTrue(userDataRepository.accountDetails.value is PendingData.Success)

        val result = (userDataRepository.accountDetails.value as PendingData.Success).result
        Assert.assertEquals(0, result.favoriteMoviesMetaData.totalEntries)
        Assert.assertEquals(0, result.favoriteMoviesMetaData.totalPages)
        Assert.assertEquals(0, result.ratedMoviesMetaData.totalEntries)
        Assert.assertEquals(0, result.ratedMoviesMetaData.totalPages)
        Assert.assertEquals(0, result.watchlistMoviesMetaData.totalEntries)
        Assert.assertEquals(0, result.watchlistMoviesMetaData.totalPages)
    }

    @Test
    fun `When loading account movie details with metadata uses supplied data`() = runTest {
        sessionIdFlow.value = "foo"
        coEvery {
            accountDataSource.getAccountFavoriteMovies(any(), any(), any())
        } returns AccountMoviesResponse(1, emptyList(), 1, 1)
        coEvery {
            accountDataSource.getAccountRatedMovies(any(), any(), any())
        } returns AccountRatedMoviesResponse(2, emptyList(), 2, 2)
        coEvery {
            accountDataSource.getAccountWatchlistMovies(any(), any(), any())
        } returns AccountMoviesResponse(3, emptyList(), 3, 3)
        coEvery {
            accountDataSource.getAccountDetails(any())
        } returns RemoteData.Success(
            AccountDetailsResponse(0, "", "", AccountAvatar(Gravatar("fizz")))
        )

        userDataRepository.loadAccountDetails()

        Assert.assertTrue(userDataRepository.accountDetails.value is PendingData.Success)

        val result = (userDataRepository.accountDetails.value as PendingData.Success).result
        Assert.assertEquals(1, result.favoriteMoviesMetaData.totalEntries)
        Assert.assertEquals(1, result.favoriteMoviesMetaData.totalPages)
        Assert.assertEquals(2, result.ratedMoviesMetaData.totalEntries)
        Assert.assertEquals(2, result.ratedMoviesMetaData.totalPages)
        Assert.assertEquals(3, result.watchlistMoviesMetaData.totalEntries)
        Assert.assertEquals(3, result.watchlistMoviesMetaData.totalPages)
    }

    @Test
    fun `When getting favorite movies but no session returns null`() = runTest {

        Assert.assertNull(userDataRepository.getAccountFavoriteMovies(0))
    }

    @Test
    fun `When getting favorite movies but no account details returns null`() = runTest {
        sessionIdFlow.value = "foo"

        Assert.assertNull(userDataRepository.getAccountFavoriteMovies(0))
    }

    @Test
    fun `When getting favorite movies with account loaded returns data`() = runTest {
        loadAccountData()

        Assert.assertNotNull(userDataRepository.getAccountFavoriteMovies(0))
    }

    @Test
    fun `When getting rated movies but no session returns null`() = runTest {

        Assert.assertNull(userDataRepository.getAccountRatedMovies(0))
    }

    @Test
    fun `When getting rated movies but no account details returns null`() = runTest {
        sessionIdFlow.value = "foo"

        Assert.assertNull(userDataRepository.getAccountRatedMovies(0))
    }

    @Test
    fun `When getting rated movies with account loaded returns data`() = runTest {
        loadAccountData()

        Assert.assertNotNull(userDataRepository.getAccountRatedMovies(0))
    }

    @Test
    fun `When getting watchlist movies but no session returns null`() = runTest {

        Assert.assertNull(userDataRepository.getAccountWatchlistMovies(0))
    }

    @Test
    fun `When getting watchlist movies but no account details returns null`() = runTest {
        sessionIdFlow.value = "foo"

        Assert.assertNull(userDataRepository.getAccountWatchlistMovies(0))
    }

    @Test
    fun `When getting watchlist movies with account loaded returns data`() = runTest {
        loadAccountData()

        Assert.assertNotNull(userDataRepository.getAccountWatchlistMovies(0))
    }

    @Test
    fun `When updated movie user settings delegates to user settings datasource`() = runTest {
        userDataRepository.updateMovieUserSettings(UserSettings())

        coVerify { movieUserSettingsDataSource.updateUserSettings(any()) }
    }

    // endregion


    // region Tv Shows

    @Test
    fun `When loading account tv show details but unable to get metadata uses default data`() = runTest {
        sessionIdFlow.value = "foo"
        coEvery {
            accountDataSource.getAccountFavoriteTvShows(any(), any(), any())
        } returns null
        coEvery {
            accountDataSource.getAccountRatedTvShows(any(), any(), any())
        } returns null
        coEvery {
            accountDataSource.getAccountWatchlistTvShows(any(), any(), any())
        } returns null
        coEvery {
            accountDataSource.getAccountDetails(any())
        } returns RemoteData.Success(
            AccountDetailsResponse(0, "", "", AccountAvatar(Gravatar("fizz")))
        )

        userDataRepository.loadAccountDetails()

        Assert.assertTrue(userDataRepository.accountDetails.value is PendingData.Success)

        val result = (userDataRepository.accountDetails.value as PendingData.Success).result
        Assert.assertEquals(0, result.favoriteTvShowsMetaData.totalEntries)
        Assert.assertEquals(0, result.favoriteTvShowsMetaData.totalPages)
        Assert.assertEquals(0, result.ratedTvShowsMetaData.totalEntries)
        Assert.assertEquals(0, result.ratedTvShowsMetaData.totalPages)
        Assert.assertEquals(0, result.watchlistTvShowsMetaData.totalEntries)
        Assert.assertEquals(0, result.watchlistTvShowsMetaData.totalPages)
    }

    @Test
    fun `When loading account tv show details with metadata uses supplied data`() = runTest {
        sessionIdFlow.value = "foo"
        coEvery {
            accountDataSource.getAccountFavoriteTvShows(any(), any(), any())
        } returns AccountTvShowsResponse(1, emptyList(), 1, 1)
        coEvery {
            accountDataSource.getAccountRatedTvShows(any(), any(), any())
        } returns AccountRatedTvShowsResponse(2, emptyList(), 2, 2)
        coEvery {
            accountDataSource.getAccountWatchlistTvShows(any(), any(), any())
        } returns AccountTvShowsResponse(3, emptyList(), 3, 3)
        coEvery {
            accountDataSource.getAccountDetails(any())
        } returns RemoteData.Success(
            AccountDetailsResponse(0, "", "", AccountAvatar(Gravatar("fizz")))
        )

        userDataRepository.loadAccountDetails()

        Assert.assertTrue(userDataRepository.accountDetails.value is PendingData.Success)

        val result = (userDataRepository.accountDetails.value as PendingData.Success).result
        Assert.assertEquals(1, result.favoriteTvShowsMetaData.totalEntries)
        Assert.assertEquals(1, result.favoriteTvShowsMetaData.totalPages)
        Assert.assertEquals(2, result.ratedTvShowsMetaData.totalEntries)
        Assert.assertEquals(2, result.ratedTvShowsMetaData.totalPages)
        Assert.assertEquals(3, result.watchlistTvShowsMetaData.totalEntries)
        Assert.assertEquals(3, result.watchlistTvShowsMetaData.totalPages)
    }

    @Test
    fun `When getting favorite tv shows but no session returns null`() = runTest {

        Assert.assertNull(userDataRepository.getAccountFavoriteTvShows(0))
    }

    @Test
    fun `When getting favorite tv shows but no account details returns null`() = runTest {
        sessionIdFlow.value = "foo"

        Assert.assertNull(userDataRepository.getAccountFavoriteTvShows(0))
    }

    @Test
    fun `When getting favorite tv shows with account loaded returns data`() = runTest {
        loadAccountData()

        Assert.assertNotNull(userDataRepository.getAccountFavoriteTvShows(0))
    }

    @Test
    fun `When getting rated tv shows but no session returns null`() = runTest {

        Assert.assertNull(userDataRepository.getAccountRatedTvShows(0))
    }

    @Test
    fun `When getting rated tv shows but no account details returns null`() = runTest {
        sessionIdFlow.value = "foo"

        Assert.assertNull(userDataRepository.getAccountRatedTvShows(0))
    }

    @Test
    fun `When getting rated tv shows with account loaded returns data`() = runTest {
        loadAccountData()

        Assert.assertNotNull(userDataRepository.getAccountRatedTvShows(0))
    }

    @Test
    fun `When getting watchlist tv shows but no session returns null`() = runTest {

        Assert.assertNull(userDataRepository.getAccountWatchlistTvShows(0))
    }

    @Test
    fun `When getting watchlist tv shows but no account details returns null`() = runTest {
        sessionIdFlow.value = "foo"

        Assert.assertNull(userDataRepository.getAccountWatchlistTvShows(0))
    }

    @Test
    fun `When getting watchlist tv shows with account loaded returns data`() = runTest {
        loadAccountData()

        Assert.assertNotNull(userDataRepository.getAccountWatchlistTvShows(0))
    }

    @Test
    fun `When updated tv show user settings delegates to user settings datasource`() = runTest {
        userDataRepository.updateTvShowUserSettings(UserSettings())

        coVerify { tvShowUserSettingsDataSource.updateUserSettings(any()) }
    }

    // endregion


    // region Auth Session

    @Test
    fun `When getting auth token delegates to auth repository`() = runTest {
        userDataRepository.getNewAuthenticationToken()

        coVerify { authenticationRepository.getNewAuthenticationToken() }
    }

    @Test
    fun `When checking if has session returns correct data`() = runTest {
        Assert.assertFalse(userDataRepository.hasSession())

        sessionIdFlow.value = "foo"

        Assert.assertTrue(userDataRepository.hasSession())
    }

    @Test
    fun `When setting pending session delegates to auth repository`() = runTest {
        userDataRepository.setPendingSession("")

        coVerify { authenticationRepository.setPendingSession(any()) }
    }

    @Test
    fun `When creating pending session returns null does not save it in datasource`() = runTest {
        coEvery {
            authenticationRepository.createPendingSession()
        } returns null

        userDataRepository.createPendingSession()

        verify(exactly = 0) { sessionDataSource.setSessionId(any()) }
    }

    @Test
    fun `When creating pending session fails does not save it in datasource`() = runTest {
        coEvery {
            authenticationRepository.createPendingSession()
        } returns NewSessionResponse(false, "")

        userDataRepository.createPendingSession()

        verify(exactly = 0) { sessionDataSource.setSessionId(any()) }
    }

    @Test
    fun `When creating pending session succeeds saves it in datasource`() = runTest {
        coEvery {
            authenticationRepository.createPendingSession()
        } returns NewSessionResponse(true, "")

        userDataRepository.createPendingSession()

        verify(exactly = 1) { sessionDataSource.setSessionId(any()) }
    }

    @Test
    fun `When deleting session removes session id and resets account details`() = runTest {
        loadAccountData()

        Assert.assertTrue(userDataRepository.accountDetails.value is PendingData.Success)

        userDataRepository.deleteSession()

        Assert.assertTrue(userDataRepository.accountDetails.value is PendingData.None)

        coVerify { authenticationRepository.deleteSession(any()) }
        coVerify { sessionDataSource.clearSessionId() }
    }

    @Test
    fun `When deleting session without current session active does not delete from repository`() = runTest {
        loadAccountData()
        sessionIdFlow.value = null

        Assert.assertTrue(userDataRepository.accountDetails.value is PendingData.Success)

        userDataRepository.deleteSession()

        Assert.assertTrue(userDataRepository.accountDetails.value is PendingData.None)

        coVerify(exactly = 0) { authenticationRepository.deleteSession(any()) }
        coVerify { sessionDataSource.clearSessionId() }
    }


    // endregion

    private suspend fun loadAccountData() {
        sessionIdFlow.value = "foo"
        coEvery {
            accountDataSource.getAccountFavoriteMovies(any(), any(), any())
        } returns AccountMoviesResponse(1, emptyList(), 1, 1)
        coEvery {
            accountDataSource.getAccountRatedMovies(any(), any(), any())
        } returns AccountRatedMoviesResponse(2, emptyList(), 2, 2)
        coEvery {
            accountDataSource.getAccountWatchlistMovies(any(), any(), any())
        } returns AccountMoviesResponse(3, emptyList(), 3, 3)
        coEvery {
            accountDataSource.getAccountDetails(any())
        } returns RemoteData.Success(
            AccountDetailsResponse(0, "", "", AccountAvatar(Gravatar("fizz")))
        )

        userDataRepository.loadAccountDetails()
    }
}
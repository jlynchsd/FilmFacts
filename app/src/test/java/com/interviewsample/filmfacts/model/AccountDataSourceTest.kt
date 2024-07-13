package com.interviewsample.filmfacts.model

import com.interviewsample.filmfacts.mockRetrofitResponse
import com.movietrivia.filmfacts.api.AccountAvatar
import com.movietrivia.filmfacts.api.AccountDetailsResponse
import com.movietrivia.filmfacts.api.AccountMoviesResponse
import com.movietrivia.filmfacts.api.AccountRatedMoviesResponse
import com.movietrivia.filmfacts.api.AccountService
import com.movietrivia.filmfacts.api.Gravatar
import com.movietrivia.filmfacts.model.AccountDataSource
import com.movietrivia.filmfacts.model.RemoteData
import com.movietrivia.filmfacts.model.RemoteDataError
import com.movietrivia.filmfacts.model.TooManyRequestsDataSource
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class AccountDataSourceTest {

    private lateinit var accountService: AccountService
    private lateinit var tooManyRequestsDataSource: TooManyRequestsDataSource
    private lateinit var dataSource: AccountDataSource

    @Before
    fun setup() {
        accountService = mockk(relaxed = true)
        tooManyRequestsDataSource = mockk(relaxed = true)
        dataSource = AccountDataSource(accountService, tooManyRequestsDataSource)

        every {
            tooManyRequestsDataSource.requestsAllowed()
        } returns true
    }

    // region Account Details

    @Test
    fun `When getting account details returns details`() = runTest {
        val accountResponse = AccountDetailsResponse(
            0, "foo", "bar", AccountAvatar(Gravatar("fizz"))
        )

        coEvery {
            accountService.getAccountDetails(any())
        } returns mockRetrofitResponse(accountResponse)

        Assert.assertEquals(
            RemoteData.Success(accountResponse).result,
            (dataSource.getAccountDetails("") as RemoteData.Success).result
        )
    }

    @Test
    fun `When getting account details throws exception returns unavailable error`() = runTest {
        coEvery {
            accountService.getAccountDetails(any())
        } throws Exception()

        Assert.assertEquals(
            RemoteData.Error<AccountDetailsResponse>(RemoteDataError.UNAVAILABLE).errorType,
            (dataSource.getAccountDetails("") as RemoteData.Error).errorType
        )
    }

    @Test
    fun `When getting account details are unauthorized returns denied error`() = runTest {
        coEvery {
            accountService.getAccountDetails(any())
        } returns mockRetrofitResponse(mockk(), false, 401)

        Assert.assertEquals(
            RemoteData.Error<AccountDetailsResponse>(RemoteDataError.DENIED).errorType,
            (dataSource.getAccountDetails("") as RemoteData.Error).errorType
        )
    }

    @Test
    fun `When getting account details are forbidden returns denied error`() = runTest {
        coEvery {
            accountService.getAccountDetails(any())
        } returns mockRetrofitResponse(mockk(), false, 403)

        Assert.assertEquals(
            RemoteData.Error<AccountDetailsResponse>(RemoteDataError.DENIED).errorType,
            (dataSource.getAccountDetails("") as RemoteData.Error).errorType
        )
    }

    @Test
    fun `When getting account details fails returns other error`() = runTest {
        coEvery {
            accountService.getAccountDetails(any())
        } returns mockRetrofitResponse(mockk(), false, 500)

        Assert.assertEquals(
            RemoteData.Error<AccountDetailsResponse>(RemoteDataError.OTHER).errorType,
            (dataSource.getAccountDetails("") as RemoteData.Error).errorType
        )
    }

    // endregion



    // region Account Favorites

    @Test
    fun `When getting account favorite movies returns favorites`() = runTest {
        val favoriteResponse = AccountMoviesResponse(
            0, listOf(), 1, 0
        )

        coEvery {
            accountService.getAccountFavoriteMovies(any(), any())
        } returns mockRetrofitResponse(favoriteResponse)

        Assert.assertEquals(favoriteResponse, dataSource.getAccountFavoriteMovies(0, "", 0))
    }

    @Test
    fun `When getting account favorite movies throws exception returns null`() = runTest {
        coEvery {
            accountService.getAccountFavoriteMovies(any(), any())
        } throws Exception()

        Assert.assertNull(dataSource.getAccountFavoriteMovies(0, "", 0))
    }

    // endregion



    // region Account Rated

    @Test
    fun `When getting account rated movies returns rated`() = runTest {
        val ratedResponse = AccountRatedMoviesResponse(
            0, listOf(), 1, 0
        )

        coEvery {
            accountService.getAccountRatedMovies(any(), any())
        } returns mockRetrofitResponse(ratedResponse)

        Assert.assertEquals(ratedResponse, dataSource.getAccountRatedMovies(0, "", 0))
    }

    @Test
    fun `When getting account rated movies throws exception returns null`() = runTest {
        coEvery {
            accountService.getAccountRatedMovies(any(), any())
        } throws Exception()

        Assert.assertNull(dataSource.getAccountRatedMovies(0, "", 0))
    }

    // endregion



    // region Account Watchlist

    @Test
    fun `When getting account watchlist movies returns watchlist`() = runTest {
        val watchlistResponse = AccountMoviesResponse(
            0, listOf(), 1, 0
        )

        coEvery {
            accountService.getAccountWatchlistMovies(any(), any())
        } returns mockRetrofitResponse(watchlistResponse)

        Assert.assertEquals(watchlistResponse, dataSource.getAccountWatchlistMovies(0, "", 0))
    }

    @Test
    fun `When getting account watchlist movies throws exception returns null`() = runTest {
        coEvery {
            accountService.getAccountWatchlistMovies(any(), any())
        } throws Exception()

        Assert.assertNull(dataSource.getAccountWatchlistMovies(0, "", 0))
    }

    // endregion
}
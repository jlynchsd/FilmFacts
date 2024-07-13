package com.movietrivia.filmfacts.model

import android.util.Log
import com.movietrivia.filmfacts.api.AccountService
import javax.inject.Inject

class AccountDataSource @Inject constructor(
    private val accountService: AccountService,
    private val tooManyRequestsDataSource: TooManyRequestsDataSource
) {

    suspend fun getAccountDetails(sessionId: String) = try {
        val result = accountService.getAccountDetails(AccountService.Builder().sessionId(sessionId).build())
        val resultBody = result.body()
        if (result.isSuccessful && resultBody != null) {
            RemoteData.Success(resultBody)
        } else {
            when (result.code()) {
                401, 403 -> RemoteData.Error(RemoteDataError.DENIED)
                TooManyRequestsDataSource.TOO_MANY_REQUESTS_RESPONSE_CODE -> {
                    tooManyRequestsDataSource.processHeaders(result.headers())
                    RemoteData.Error(RemoteDataError.OTHER)
                }
                else -> RemoteData.Error(RemoteDataError.OTHER)
            }
        }
    } catch (e: Exception) {
        Log.e(TAG, e.toString())
        tooManyRequestsDataSource.processException(e)
        RemoteData.Error(RemoteDataError.UNAVAILABLE)
    }

    suspend fun getAccountFavoriteMovies(accountId: Int, sessionId: String, page: Int) = makeNetworkCall(tooManyRequestsDataSource) {
        accountService.getAccountFavoriteMovies(
            accountId,
            AccountService.Builder().sessionId(sessionId).page(page).build()
        ).body()
    }

    suspend fun getAccountRatedMovies(accountId: Int, sessionId: String, page: Int) = makeNetworkCall(tooManyRequestsDataSource) {
        accountService.getAccountRatedMovies(
            accountId,
            AccountService.Builder().sessionId(sessionId).page(page).build()
        ).body()
    }

    suspend fun getAccountWatchlistMovies(accountId: Int, sessionId: String, page: Int) = makeNetworkCall(tooManyRequestsDataSource) {
        accountService.getAccountWatchlistMovies(
            accountId, AccountService.Builder().sessionId(sessionId).page(page).build()
        ).body()
    }

    private companion object {
        const val TAG = "AccountDataSource"
    }
}
package com.movietrivia.filmfacts.model

import android.content.Context
import com.movietrivia.filmfacts.api.AccountMoviesResponse
import com.movietrivia.filmfacts.api.AccountRatedMoviesResponse
import com.movietrivia.filmfacts.api.AccountRatedTvShowsResponse
import com.movietrivia.filmfacts.api.AccountTvShowsResponse
import com.movietrivia.filmfacts.domain.preloadImage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import kotlin.coroutines.coroutineContext

class UserDataRepository @Inject constructor(
    private val context: Context,
    private val authenticationRepository: AuthenticationRepository,
    private val movieUserSettingsDataSource: UserSettingsDataSource,
    private val tvShowUserSettingsDataSource: UserSettingsDataSource,
    private val sessionDataSource: SessionDataSource,
    private val accountDataSource: AccountDataSource
) {
    val movieUserSettings = movieUserSettingsDataSource.userSettings
    val tvShowUserSettings = tvShowUserSettingsDataSource.userSettings
    private val _accountDetails = MutableStateFlow<PendingData<AccountDetails>>(PendingData.None())
    val accountDetails: StateFlow<PendingData<AccountDetails>> = _accountDetails

    suspend fun loadAccountDetails() {
        _accountDetails.value = PendingData.Loading()
        sessionDataSource.sessionId.value?.let { sessionId ->
            when (val remoteData = accountDataSource.getAccountDetails(sessionId)) {
                is RemoteData.Success -> {
                    val response = remoteData.result
                    val scope = CoroutineScope(coroutineContext)
                    val favoriteMoviesResponse = scope.async {
                        accountDataSource.getAccountFavoriteMovies(response.id, sessionId, 1)
                    }
                    val ratedMoviesResponse = scope.async {
                        accountDataSource.getAccountRatedMovies(response.id, sessionId, 1)
                    }
                    val watchlistMoviesResponse = scope.async {
                        accountDataSource.getAccountWatchlistMovies(response.id, sessionId, 1)
                    }

                    val favoriteTvShowsResponse = scope.async {
                        accountDataSource.getAccountFavoriteTvShows(response.id, sessionId, 1)
                    }
                    val ratedTvShowsResponse = scope.async {
                        accountDataSource.getAccountRatedTvShows(response.id, sessionId, 1)
                    }
                    val watchlistTvShowsResponse = scope.async {
                        accountDataSource.getAccountWatchlistTvShows(response.id, sessionId, 1)
                    }

                    val favoriteMoviesMetaData = favoriteMoviesResponse.await()?.let {
                        PagedMetaData(it.totalPageCount, it.totalResultCount)
                    } ?: PagedMetaData(0, 0)

                    val ratedMoviesMetaData = ratedMoviesResponse.await()?.let {
                        PagedMetaData(it.totalPageCount, it.totalResultCount)
                    } ?: PagedMetaData(0, 0)

                    val watchlistMoviesMetaData = watchlistMoviesResponse.await()?.let {
                        PagedMetaData(it.totalPageCount, it.totalResultCount)
                    } ?: PagedMetaData(0, 0)

                    val favoriteTvShowsMetaData = favoriteTvShowsResponse.await()?.let {
                        PagedMetaData(it.totalPageCount, it.totalResultCount)
                    } ?: PagedMetaData(0, 0)

                    val ratedTvShowsMetaData = ratedTvShowsResponse.await()?.let {
                        PagedMetaData(it.totalPageCount, it.totalResultCount)
                    } ?: PagedMetaData(0, 0)

                    val watchlistTvShowsMetaData = watchlistTvShowsResponse.await()?.let {
                        PagedMetaData(it.totalPageCount, it.totalResultCount)
                    } ?: PagedMetaData(0, 0)

                    _accountDetails.value = PendingData.Success(
                        AccountDetails(
                            id = response.id,
                            name = response.name,
                            userName = response.userName,
                            avatarPath = "https://secure.gravatar.com/avatar/${response.avatar.gravatar.hash}.jpg?s=400",
                            favoriteMoviesMetaData = favoriteMoviesMetaData,
                            ratedMoviesMetaData = ratedMoviesMetaData,
                            watchlistMoviesMetaData = watchlistMoviesMetaData,
                            favoriteTvShowsMetaData = favoriteTvShowsMetaData,
                            ratedTvShowsMetaData = ratedTvShowsMetaData,
                            watchlistTvShowsMetaData = watchlistTvShowsMetaData
                        ).also { details ->
                            preloadImage(context, details.avatarPath)
                        }
                    )
                }

                is RemoteData.Error -> {
                    _accountDetails.value = PendingData.Error(LOAD_ACCOUNT_DATA_ERROR_REQUEST_FAILED)
                    if (remoteData.errorType == RemoteDataError.DENIED) {
                        clearSession()
                    }
                }
            }
        } ?: run {
            _accountDetails.value = PendingData.Error(LOAD_ACCOUNT_DATA_ERROR_NO_SESSION)
        }
    }

    suspend fun getAccountFavoriteMovies(page: Int): AccountMoviesResponse? {
        sessionDataSource.sessionId.value?.let { sessionId ->
            accountDetails.value.let { accountDetails ->
                if (accountDetails is PendingData.Success) {
                    return accountDataSource.getAccountFavoriteMovies(accountDetails.result.id, sessionId, page)
                }
            }
        }
        return null
    }

    suspend fun getAccountRatedMovies(page: Int): AccountRatedMoviesResponse? {
        sessionDataSource.sessionId.value?.let { sessionId ->
            accountDetails.value.let { accountDetails ->
                if (accountDetails is PendingData.Success) {
                    return accountDataSource.getAccountRatedMovies(accountDetails.result.id, sessionId, page)
                }
            }
        }
        return null
    }

    suspend fun getAccountWatchlistMovies(page: Int): AccountMoviesResponse? {
        sessionDataSource.sessionId.value?.let { sessionId ->
            accountDetails.value.let { accountDetails ->
                if (accountDetails is PendingData.Success) {
                    return accountDataSource.getAccountWatchlistMovies(accountDetails.result.id, sessionId, page)
                }
            }
        }
        return null
    }

    suspend fun getAccountFavoriteTvShows(page: Int): AccountTvShowsResponse? {
        sessionDataSource.sessionId.value?.let { sessionId ->
            accountDetails.value.let { accountDetails ->
                if (accountDetails is PendingData.Success) {
                    return accountDataSource.getAccountFavoriteTvShows(accountDetails.result.id, sessionId, page)
                }
            }
        }
        return null
    }

    suspend fun getAccountRatedTvShows(page: Int): AccountRatedTvShowsResponse? {
        sessionDataSource.sessionId.value?.let { sessionId ->
            accountDetails.value.let { accountDetails ->
                if (accountDetails is PendingData.Success) {
                    return accountDataSource.getAccountRatedTvShows(accountDetails.result.id, sessionId, page)
                }
            }
        }
        return null
    }

    suspend fun getAccountWatchlistTvShows(page: Int): AccountTvShowsResponse? {
        sessionDataSource.sessionId.value?.let { sessionId ->
            accountDetails.value.let { accountDetails ->
                if (accountDetails is PendingData.Success) {
                    return accountDataSource.getAccountWatchlistTvShows(accountDetails.result.id, sessionId, page)
                }
            }
        }
        return null
    }

    suspend fun updateMovieUserSettings(settings: UserSettings) = movieUserSettingsDataSource.updateUserSettings(settings)

    suspend fun updateTvShowUserSettings(settings: UserSettings) = tvShowUserSettingsDataSource.updateUserSettings(settings)

    suspend fun getNewAuthenticationToken() = authenticationRepository.getNewAuthenticationToken()

    fun hasSession() = sessionDataSource.sessionId.value != null

    fun setPendingSession(requestToken: String) = authenticationRepository.setPendingSession(requestToken)

    suspend fun createPendingSession() {
        authenticationRepository.createPendingSession()?.let {
            if (it.success) {
                sessionDataSource.setSessionId(it.sessionId)
            }
        }
    }

    suspend fun deleteSession() {
        clearSession()
        _accountDetails.value = PendingData.None()
    }

    private suspend fun clearSession() {
        sessionDataSource.sessionId.value?.let {
            authenticationRepository.deleteSession(it)
        }
        sessionDataSource.clearSessionId()
    }

    private companion object {
        const val LOAD_ACCOUNT_DATA_ERROR_NO_SESSION = "LOAD_ACCOUNT_DATA_ERROR_NO_SESSION"
        const val LOAD_ACCOUNT_DATA_ERROR_REQUEST_FAILED = "LOAD_ACCOUNT_DATA_ERROR_REQUEST_FAILED"
    }
}
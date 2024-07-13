package com.movietrivia.filmfacts.model

import android.annotation.SuppressLint
import com.movietrivia.filmfacts.api.AuthenticationService
import com.movietrivia.filmfacts.api.DeleteSessionBody
import com.movietrivia.filmfacts.api.NewSessionBody
import com.movietrivia.filmfacts.api.getAuthUri
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.transformWhile
import java.text.SimpleDateFormat
import javax.inject.Inject

class AuthenticationDataSource @Inject constructor(
    private val authenticationService: AuthenticationService,
    private val customTabsDataSource: CustomTabsDataSource,
    private val tooManyRequestsDataSource: TooManyRequestsDataSource
) {

    private var currentMetaData: CustomTabMetaData? = null
    private val _hasAuthRequest = MutableStateFlow(false)
    val hasAuthRequest: StateFlow<Boolean> = _hasAuthRequest

    private var pendingRequestToken: String? = null
    private val _hasPendingRequestToken = MutableStateFlow(false)
    val hasPendingRequestToken: StateFlow<Boolean> = _hasPendingRequestToken

    suspend fun getNewAuthenticationToken() = makeNetworkCall(tooManyRequestsDataSource) {
        authenticationService.getNewAuthenticationToken(AuthenticationService.options).body()
    }

    suspend fun deleteSession(sessionId: String) = makeNetworkCall(tooManyRequestsDataSource) {
        authenticationService.deleteSession(DeleteSessionBody(sessionId), AuthenticationService.options).body()
    }

    fun setPendingSession(requestToken: String) {
        _hasPendingRequestToken.value = true
        pendingRequestToken = requestToken
    }

    suspend fun createPendingSession() = makeNetworkCall(tooManyRequestsDataSource) {
        pendingRequestToken?.let {
            pendingRequestToken = null
            _hasPendingRequestToken.value = false
            authenticationService.createSession(NewSessionBody(it), AuthenticationService.options).body()
        }
    }

    fun hasAuthRequestExpired() = currentMetaData?.let {
        it.expirationTime < CurrentTime.currentTimeInMillis()
    } ?: false

    fun consumeAuthRequest(): CustomTabMetaData? {
        _hasAuthRequest.value = false
        val temp = currentMetaData
        currentMetaData = null
        return temp
    }

    @SuppressLint("SimpleDateFormat")
    suspend fun prepareAuthenticationSession() {
        val tabsSupported = customTabsDataSource.startSession()
        getNewAuthenticationToken()?.let { tokenResponse ->
            if (tokenResponse.success) {
                val currentSession = customTabsDataSource.tabSession.value
                val authUri = getAuthUri(tokenResponse.requestToken)
                val expiration = kotlin.runCatching { SimpleDateFormat("yyyy-MM-dd HH:mm:ss zzz").parse(tokenResponse.expires) }.getOrNull()?.time ?: 0L
                if (tabsSupported) {
                    if (currentSession != null) {
                        currentSession.mayLaunchUrl(authUri, null, null)
                        currentMetaData = CustomTabMetaData(
                            session = currentSession,
                            uri = authUri,
                            expirationTime = expiration
                        )
                        _hasAuthRequest.value = true
                    } else {
                        customTabsDataSource.tabSession.firstNonNull().collect {
                            it.mayLaunchUrl(authUri, null, null)
                            currentMetaData = CustomTabMetaData(
                                session = it,
                                uri = authUri,
                                expirationTime = expiration
                            )
                            _hasAuthRequest.value = true
                        }
                    }
                } else {
                    currentMetaData = CustomTabMetaData(
                        session = null,
                        uri = authUri,
                        expirationTime = expiration
                    )
                    _hasAuthRequest.value = true
                }
            }
        }
    }
}

internal object CurrentTime {
    fun currentTimeInMillis() = System.currentTimeMillis()
}

private fun <T> Flow<T?>.firstNonNull() =
    transformWhile { value ->
        if (value != null) {
            emit(value)
        }
        value == null
    }
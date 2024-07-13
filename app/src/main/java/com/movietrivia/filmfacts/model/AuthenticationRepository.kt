package com.movietrivia.filmfacts.model

import javax.inject.Inject

class AuthenticationRepository @Inject constructor(
    private val authenticationDataSource: AuthenticationDataSource,
) {
    val hasAuthRequest = authenticationDataSource.hasAuthRequest
    val hasPendingRequestToken = authenticationDataSource.hasPendingRequestToken

    fun consumeAuthRequest() = authenticationDataSource.consumeAuthRequest()

    fun hasAuthRequestExpired() = authenticationDataSource.hasAuthRequestExpired()

    suspend fun getNewAuthenticationToken() = authenticationDataSource.getNewAuthenticationToken()

    fun setPendingSession(requestToken: String) = authenticationDataSource.setPendingSession(requestToken)

    suspend fun createPendingSession() = authenticationDataSource.createPendingSession()

    suspend fun deleteSession(sessionId: String) = authenticationDataSource.deleteSession(sessionId)

    suspend fun prepareAuthenticationSession() = authenticationDataSource.prepareAuthenticationSession()
}
package com.movietrivia.filmfacts.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.movietrivia.filmfacts.model.AuthenticationRepository
import com.movietrivia.filmfacts.model.CustomTabMetaData
import com.movietrivia.filmfacts.model.UserDataRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class AuthenticationViewModel @Inject internal constructor(
    private val authenticationRepository: AuthenticationRepository,
    private val userDataRepository: UserDataRepository
): ViewModel() {

    val hasAuthRequest = authenticationRepository.hasAuthRequest
    val hasPendingRequestToken = authenticationRepository.hasPendingRequestToken

    fun consumeAuthRequest(): CustomTabMetaData? {
        return if (!authenticationRepository.hasAuthRequestExpired()) {
            authenticationRepository.consumeAuthRequest()
        } else {
            null
        }
    }

    fun prepareAuthenticationSession() {
        viewModelScope.launch {
            authenticationRepository.prepareAuthenticationSession()
        }
    }

    suspend fun getNewAuthenticationToken() = userDataRepository.getNewAuthenticationToken()

    fun setPendingSession(requestToken: String) = userDataRepository.setPendingSession(requestToken)

    suspend fun createPendingSession() = withContext(Dispatchers.IO) {
        userDataRepository.createPendingSession()
    }

    fun hasSession() = userDataRepository.hasSession()

    fun deleteSession() {
        viewModelScope.launch(Dispatchers.IO) {
            userDataRepository.deleteSession()
        }
    }
}
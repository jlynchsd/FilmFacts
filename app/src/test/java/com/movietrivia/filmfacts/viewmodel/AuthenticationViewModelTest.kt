package com.movietrivia.filmfacts.viewmodel

import com.movietrivia.filmfacts.model.AuthenticationRepository
import com.movietrivia.filmfacts.model.UserDataRepository
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.Assert
import org.junit.Before
import org.junit.Test

class AuthenticationViewModelTest {

    private lateinit var authenticationRepository: AuthenticationRepository
    private lateinit var userDataRepository: UserDataRepository
    private lateinit var viewModel: AuthenticationViewModel

    @Before
    fun setup() {
        authenticationRepository = mockk(relaxed = true)
        userDataRepository = mockk(relaxed = true)
        viewModel = AuthenticationViewModel(authenticationRepository, userDataRepository)
    }

    @Test
    fun `When consuming expired auth request returns null`() {
        every {
            authenticationRepository.hasAuthRequestExpired()
        } returns true

        Assert.assertNull(viewModel.consumeAuthRequest())
    }

    @Test
    fun `When consuming valid auth request delegates to the auth repository`() {
        every {
            authenticationRepository.hasAuthRequestExpired()
        } returns false
        every {
            authenticationRepository.consumeAuthRequest()
        } returns null

        viewModel.consumeAuthRequest()

        verify { authenticationRepository.consumeAuthRequest() }
    }

    @Test
    fun `When preparing auth session delegates to auth repository`() = runViewModelScope {
        viewModel.prepareAuthenticationSession()

        coVerify { authenticationRepository.prepareAuthenticationSession() }
    }

    @Test
    fun `When getting new authentication token delegates to user repository`() = runTest {
        viewModel.getNewAuthenticationToken()

        coVerify { userDataRepository.getNewAuthenticationToken() }
    }

    @Test
    fun `When setting pending session delegates to user repository`() {
        val token = "foo"
        viewModel.setPendingSession(token)

        verify { userDataRepository.setPendingSession(token) }
    }

    @Test
    fun `When creating pending session delegates to user repository`() = runTest {
        viewModel.createPendingSession()

        coVerify { userDataRepository.createPendingSession() }
    }

    @Test
    fun `When getting if there is an active session delegates to user repository`() {
        viewModel.hasSession()

        verify { userDataRepository.hasSession() }
    }

    @Test
    fun `When deleting session delegates to user repository`() = runViewModelScope {
        viewModel.deleteSession()

        coVerify { userDataRepository.deleteSession() }
    }
}
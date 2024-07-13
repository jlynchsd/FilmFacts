package com.interviewsample.filmfacts.model

import com.movietrivia.filmfacts.api.AuthenticationTokenResponse
import com.movietrivia.filmfacts.api.DeleteSessionResponse
import com.movietrivia.filmfacts.api.NewSessionResponse
import com.movietrivia.filmfacts.model.AuthenticationDataSource
import com.movietrivia.filmfacts.model.AuthenticationRepository
import com.movietrivia.filmfacts.model.CustomTabMetaData
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.Assert
import org.junit.Before
import org.junit.Test

class AuthenticationRepositoryTest {

    private lateinit var dataSource: AuthenticationDataSource
    private lateinit var repository: AuthenticationRepository

    @Before
    fun setup() {
        dataSource = mockk(relaxed = true)
        repository = AuthenticationRepository(dataSource)
    }

    @Test
    fun `When consuming auth request delegates to datasource`() {
        val authRequest = mockk<CustomTabMetaData>()
        every {
            dataSource.consumeAuthRequest()
        } returns authRequest

        Assert.assertEquals(authRequest, repository.consumeAuthRequest())
    }

    @Test
    fun `When checking if auth request expired delegates to datasource`() {
        repository.hasAuthRequestExpired()

        verify { dataSource.hasAuthRequestExpired() }
    }

    @Test
    fun `When getting new auth token delegates to datasource`() = runTest {
        val response = AuthenticationTokenResponse(true, "", "")
        coEvery {
            dataSource.getNewAuthenticationToken()
        } returns response

        Assert.assertEquals(response, repository.getNewAuthenticationToken())
    }

    @Test
    fun `When setting pending session delegates to datasource`() {
        repository.setPendingSession("")

        verify { dataSource.setPendingSession("") }
    }


    @Test
    fun `When creating pending session delegates to datasource`() = runTest {
        val response = NewSessionResponse(true, "")
        coEvery {
            dataSource.createPendingSession()
        } returns response

        Assert.assertEquals(response, repository.createPendingSession())
    }

    @Test
    fun `When deleting auth session delegates to datasource`() = runTest {
        val response = DeleteSessionResponse(true)
        coEvery {
            dataSource.deleteSession(any())
        } returns response

        Assert.assertEquals(response, repository.deleteSession(""))
    }

    @Test
    fun `When preparing auth session delegates to datasource`() = runTest {
        repository.prepareAuthenticationSession()

        coVerify { dataSource.prepareAuthenticationSession() }
    }
}
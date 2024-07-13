package com.interviewsample.filmfacts.model

import androidx.browser.customtabs.CustomTabsSession
import com.interviewsample.filmfacts.mockRetrofitResponse
import com.movietrivia.filmfacts.api.AuthenticationService
import com.movietrivia.filmfacts.api.AuthenticationTokenResponse
import com.movietrivia.filmfacts.api.DeleteSessionResponse
import com.movietrivia.filmfacts.api.NewSessionResponse
import com.movietrivia.filmfacts.model.AuthenticationDataSource
import com.movietrivia.filmfacts.model.CurrentTime
import com.movietrivia.filmfacts.model.CustomTabsDataSource
import com.movietrivia.filmfacts.model.TooManyRequestsDataSource
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.text.SimpleDateFormat
import java.util.Date

@RunWith(RobolectricTestRunner::class)
class AuthenticationDataSourceTest {

    private lateinit var authService: AuthenticationService
    private lateinit var tabDataSource: CustomTabsDataSource
    private lateinit var tooManyRequestsDataSource: TooManyRequestsDataSource
    private lateinit var authDataSource: AuthenticationDataSource

    @Before
    fun setup() {
        authService = mockk(relaxed = true)
        tabDataSource = mockk(relaxed = true)
        tooManyRequestsDataSource = mockk(relaxed = true)
        authDataSource = AuthenticationDataSource(authService, tabDataSource, tooManyRequestsDataSource)

        every {
            tooManyRequestsDataSource.requestsAllowed()
        } returns true
    }

    // region New Auth Token

    @Test
    fun `When getting new authentication token returns auth token`() = runTest {
        val authToken = AuthenticationTokenResponse(true, "", "foo")
        coEvery {
            authService.getNewAuthenticationToken(any())
        } returns mockRetrofitResponse(authToken)

        Assert.assertEquals(authToken, authDataSource.getNewAuthenticationToken())
    }

    @Test
    fun `When getting new authentication token throws exception returns null`() = runTest {
        coEvery {
            authService.getNewAuthenticationToken(any())
        } throws Exception()

        Assert.assertNull(authDataSource.getNewAuthenticationToken())
    }

    // endregion

    // region Delete Auth Session

    @Test
    fun `When deleting authentication session returns successful deletion`() = runTest {
        val deleteToken = DeleteSessionResponse(true)
        coEvery {
            authService.deleteSession(any(), any())
        } returns mockRetrofitResponse(deleteToken)

        Assert.assertEquals(deleteToken, authDataSource.deleteSession(""))
    }

    @Test
    fun `When deleting authentication session throws exception returns null`() = runTest {
        coEvery {
            authService.deleteSession(any(), any())
        } throws Exception()

        Assert.assertNull(authDataSource.deleteSession(""))
    }

    // endregion

    // region Pending Session

    @Test
    fun `When setting pending session flags pending request`() = runTest {
        authDataSource.setPendingSession("")

        Assert.assertTrue(authDataSource.hasPendingRequestToken.value)
    }

    @Test
    fun `When creating pending session removes pending flag and returns session`() = runTest {
        val session = NewSessionResponse(true, "foo")

        coEvery {
            authService.createSession(any(), any())
        } returns mockRetrofitResponse(session)

        authDataSource.setPendingSession("")

        Assert.assertTrue(authDataSource.hasPendingRequestToken.value)

        val result = authDataSource.createPendingSession()

        Assert.assertEquals(session, result)
        Assert.assertFalse(authDataSource.hasPendingRequestToken.value)
    }

    @Test
    fun `When creating pending session but no pending token returns null`() = runTest {
        Assert.assertNull(authDataSource.createPendingSession())
    }

    @Test
    fun `When creating pending session throws exception returns null`() = runTest {
        coEvery {
            authService.createSession(any(), any())
        } throws Exception()
        authDataSource.setPendingSession("")

        Assert.assertNull(authDataSource.createPendingSession())
    }

    // endregion

    // region Auth Request

    @Test
    fun `When preparing auth session and custom tabs not supported creates tab-less session`() = runTest {
        every {
            tabDataSource.startSession()
        } returns false
        every {
            tabDataSource.tabSession
        } returns MutableStateFlow(null)
        val authToken = AuthenticationTokenResponse(true, "", "foo")
        coEvery {
            authService.getNewAuthenticationToken(any())
        } returns mockRetrofitResponse(authToken)

        authDataSource.prepareAuthenticationSession()

        Assert.assertTrue(authDataSource.hasAuthRequest.value)

        val request = authDataSource.consumeAuthRequest()

        Assert.assertFalse(authDataSource.hasAuthRequest.value)
        Assert.assertNull(request!!.session)
    }

    @Test
    fun `When preparing auth session and custom tabs has not started waits until custom tab is ready`() = runTest {
        every {
            tabDataSource.startSession()
        } returns true
        val mockSession: CustomTabsSession = mockk(relaxed = true)
        every {
            tabDataSource.tabSession
        } returnsMany listOf(MutableStateFlow(null), MutableStateFlow(mockSession))
        val authToken = AuthenticationTokenResponse(true, "", "foo")
        coEvery {
            authService.getNewAuthenticationToken(any())
        } returns mockRetrofitResponse(authToken)

        authDataSource.prepareAuthenticationSession()

        Assert.assertTrue(authDataSource.hasAuthRequest.value)

        val request = authDataSource.consumeAuthRequest()

        Assert.assertFalse(authDataSource.hasAuthRequest.value)
        Assert.assertEquals(mockSession, request!!.session)
    }

    @Test
    fun `When preparing auth session and custom tabs have already started uses existing custom tabs`() = runTest {
        every {
            tabDataSource.startSession()
        } returns true
        val mockSession: CustomTabsSession = mockk(relaxed = true)
        every {
            tabDataSource.tabSession
        } returns MutableStateFlow(mockSession)
        val authToken = AuthenticationTokenResponse(true, "", "foo")
        coEvery {
            authService.getNewAuthenticationToken(any())
        } returns mockRetrofitResponse(authToken)

        authDataSource.prepareAuthenticationSession()

        Assert.assertTrue(authDataSource.hasAuthRequest.value)

        val request = authDataSource.consumeAuthRequest()

        Assert.assertFalse(authDataSource.hasAuthRequest.value)
        Assert.assertEquals(mockSession, request!!.session)
    }

    @Test
    fun `When preparing auth session and no auth token available does not create request`() = runTest {
        every {
            tabDataSource.startSession()
        } returns false
        coEvery {
            authService.getNewAuthenticationToken(any())
        } throws Exception()

        authDataSource.prepareAuthenticationSession()

        Assert.assertFalse(authDataSource.hasAuthRequest.value)
        Assert.assertNull(authDataSource.consumeAuthRequest())
    }

    @Test
    fun `When preparing auth session and auth token failed does not create request`() = runTest {
        every {
            tabDataSource.startSession()
        } returns false
        val authToken = AuthenticationTokenResponse(false, "", "foo")
        coEvery {
            authService.getNewAuthenticationToken(any())
        } returns mockRetrofitResponse(authToken)

        authDataSource.prepareAuthenticationSession()

        Assert.assertFalse(authDataSource.hasAuthRequest.value)
        Assert.assertNull(authDataSource.consumeAuthRequest())
    }

    @Test
    fun `When preparing auth session and expiration date in past is expired`() = runTest {
        mockkObject(CurrentTime)
        every { CurrentTime.currentTimeInMillis() } returns 100
        every {
            tabDataSource.startSession()
        } returns false
        every {
            tabDataSource.tabSession
        } returns MutableStateFlow(null)
        val authToken = AuthenticationTokenResponse(true, "", "foo")
        coEvery {
            authService.getNewAuthenticationToken(any())
        } returns mockRetrofitResponse(authToken)

        authDataSource.prepareAuthenticationSession()

        Assert.assertTrue(authDataSource.hasAuthRequestExpired())

        clearAllMocks()
    }

    @Test
    fun `When preparing auth session and expiration date in future is not expired`() = runTest {
        mockkObject(CurrentTime)
        every { CurrentTime.currentTimeInMillis() } returns 0
        every {
            tabDataSource.startSession()
        } returns false
        every {
            tabDataSource.tabSession
        } returns MutableStateFlow(null)
        val date = SimpleDateFormat("yyyy-MM-dd HH:mm:ss zzz").format(Date(100))
        val authToken = AuthenticationTokenResponse(true, date, "foo")
        coEvery {
            authService.getNewAuthenticationToken(any())
        } returns mockRetrofitResponse(authToken)

        authDataSource.prepareAuthenticationSession()

        Assert.assertFalse(authDataSource.hasAuthRequestExpired())

        clearAllMocks()
    }

    @Test
    fun `When checking auth session expiration before preparing returns false`() = runTest {
        Assert.assertFalse(authDataSource.hasAuthRequestExpired())
    }

    // endregion
}
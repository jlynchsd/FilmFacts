package com.interviewsample.filmfacts.model

import com.movietrivia.filmfacts.model.TooManyRequestsDataSource
import com.movietrivia.filmfacts.model.makeNetworkCall
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.Assert
import org.junit.Before
import org.junit.Test

class NetworkUtilTest {

    private lateinit var tooManyRequestsDataSource: TooManyRequestsDataSource

    @Before
    fun setup() {
        tooManyRequestsDataSource = mockk(relaxed = true)
    }

    @Test
    fun `When making network call and not too many requests then makes call and returns result`() = runTest {
        var networkCalled = false
        val responseValue = "foo"

        every {
            tooManyRequestsDataSource.requestsAllowed()
        } returns true

        val response: String? = makeNetworkCall(tooManyRequestsDataSource) {
            networkCalled = true
            responseValue
        }

        Assert.assertTrue(networkCalled)
        Assert.assertEquals(responseValue, response)
    }

    @Test
    fun `When making network call and too many requests then does makes call and returns null`() = runTest {
        var networkCalled = false
        val responseValue = "foo"

        every {
            tooManyRequestsDataSource.requestsAllowed()
        } returns false

        val response: String? = makeNetworkCall(tooManyRequestsDataSource) {
            networkCalled = true
            responseValue
        }

        Assert.assertFalse(networkCalled)
        Assert.assertNull(response)
    }

    @Test
    fun `When making network call throws exception then processes exception and returns null`() = runTest {

        every {
            tooManyRequestsDataSource.requestsAllowed()
        } returns true

        val response = makeNetworkCall<String>(tooManyRequestsDataSource) {
            throw Exception()
        }

        verify {
            tooManyRequestsDataSource.processException(any())
        }
        Assert.assertNull(response)
    }
}
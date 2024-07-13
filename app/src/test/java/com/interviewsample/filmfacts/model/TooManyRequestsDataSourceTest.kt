package com.interviewsample.filmfacts.model

import com.movietrivia.filmfacts.model.CalendarProvider
import com.movietrivia.filmfacts.model.TooManyRequestsDataSource
import io.mockk.every
import io.mockk.mockk
import okhttp3.Headers
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response
import java.util.Calendar
import java.util.Date

class TooManyRequestsDataSourceTest {

    private lateinit var calendar: Calendar
    private lateinit var calendarProvider: CalendarProvider
    private lateinit var tooManyRequestsDataSource: TooManyRequestsDataSource

    @Before
    fun setup() {
        calendar = mockk(relaxed = true)
        calendarProvider = mockk(relaxed = true)
        tooManyRequestsDataSource = TooManyRequestsDataSource(calendarProvider)

        every {
            calendarProvider.instance()
        } returns calendar
    }

    @Test
    fun `When processing non http exception then does not set retry time`() {
        every {
            calendar.timeInMillis
        } returns 1

        tooManyRequestsDataSource.processException(Exception())

        Assert.assertTrue(tooManyRequestsDataSource.requestsAllowed())
    }

    @Test
    fun `When processing unrelated http exception then does not set retry time`() {
        every {
            calendar.timeInMillis
        } returns 1

        val mockException: HttpException = mockk(relaxed = true)

        tooManyRequestsDataSource.processException(mockException)

        Assert.assertTrue(tooManyRequestsDataSource.requestsAllowed())
    }

    @Test
    fun `When processing too many requests exception and no time present uses default timeout`() {
        val mockException: HttpException = mockk(relaxed = true)
        every {
            mockException.code()
        } returns TooManyRequestsDataSource.TOO_MANY_REQUESTS_RESPONSE_CODE

        every {
            mockException.response()
        } returns null

        every {
            calendar.timeInMillis
        } returnsMany listOf(
            0L,
            TooManyRequestsDataSource.FIXED_RETRY_DELAY,
            TooManyRequestsDataSource.FIXED_RETRY_DELAY + 1
        )

        tooManyRequestsDataSource.processException(mockException)

        Assert.assertFalse(tooManyRequestsDataSource.requestsAllowed())
        Assert.assertTrue(tooManyRequestsDataSource.requestsAllowed())
    }

    @Test
    fun `When processing too many requests exception and time present uses provided timeout`() {
        val mockOffset = 1234L
        val mockException: HttpException = mockk(relaxed = true)
        every {
            mockException.code()
        } returns TooManyRequestsDataSource.TOO_MANY_REQUESTS_RESPONSE_CODE

        val mockHeaders: Headers = mockk()
        every {
            mockHeaders.getDate(TooManyRequestsDataSource.RETRY_AFTER_HEADER)
        } returns Date(mockOffset)

        val mockResponse: Response<String> = mockk()
        every {
            mockResponse.headers()
        } returns mockHeaders

        every {
            mockException.response()
        } returns mockResponse

        every {
            calendar.timeInMillis
        } returnsMany listOf(
            mockOffset,
            mockOffset + 1
        )

        tooManyRequestsDataSource.processException(mockException)

        Assert.assertFalse(tooManyRequestsDataSource.requestsAllowed())
        Assert.assertTrue(tooManyRequestsDataSource.requestsAllowed())
    }
}
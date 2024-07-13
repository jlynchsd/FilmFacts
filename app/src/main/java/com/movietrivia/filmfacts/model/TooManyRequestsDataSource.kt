package com.movietrivia.filmfacts.model

import okhttp3.Headers
import retrofit2.HttpException

class TooManyRequestsDataSource(
    private val calendarProvider: CalendarProvider
) {

    private var allowRequestsAfterTime = 0L

    fun requestsAllowed() = calendarProvider.instance().timeInMillis > allowRequestsAfterTime

    fun processException(exception: Exception) {
        if (exception is HttpException && exception.code() == TOO_MANY_REQUESTS_RESPONSE_CODE) {
            processHeaders(exception.response()?.headers())
        }
    }

    fun processHeaders(headers: Headers?) {
        val retryDate = headers?.getDate(RETRY_AFTER_HEADER)
        allowRequestsAfterTime =
            retryDate?.time ?: (calendarProvider.instance().timeInMillis + FIXED_RETRY_DELAY)
    }

    companion object {
        const val TOO_MANY_REQUESTS_RESPONSE_CODE = 429
        const val RETRY_AFTER_HEADER = "Retry-After"
        const val FIXED_RETRY_DELAY: Long = 2 * 60 * 1000
    }
}
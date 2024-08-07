package com.movietrivia.filmfacts

import io.mockk.every
import io.mockk.mockk
import retrofit2.Response

fun <T> mockRetrofitResponse(content: T, successful: Boolean = true, responseCode: Int = 200): Response<T> =
    mockk<Response<T>>().also {
        every {
            it.body()
        } returns content

        every {
            it.isSuccessful
        } returns successful

        every {
            it.code()
        } returns responseCode
    }
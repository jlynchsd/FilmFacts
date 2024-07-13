package com.interviewsample.filmfacts.api

import com.movietrivia.filmfacts.api.AccountService
import org.junit.Assert
import org.junit.Test

class AccountServiceTest {

    @Test
    fun `When building default adds no values`() {
        val result = AccountService.Builder().build()

        Assert.assertEquals(0, result.keys.size)
    }

    @Test
    fun `When building with sessionId applies sessionId`() {
        val sessionId = "foo"
        val result = AccountService.Builder().sessionId(sessionId).build()

        Assert.assertEquals(sessionId, result[AccountService.Builder.SESSION_ID_KEY])
    }

    @Test
    fun `When building with page applies page`() {
        val page = 3
        val result = AccountService.Builder().page(page).build()

        Assert.assertEquals(page.toString(), result[AccountService.Builder.PAGE_KEY])
    }
}
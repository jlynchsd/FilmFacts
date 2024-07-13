package com.interviewsample.filmfacts.api

import com.movietrivia.filmfacts.api.PersonService
import org.junit.Assert
import org.junit.Test

class PersonServiceTest {

    @Test
    fun `When building default has no page`() {
        val result = PersonService.Builder().build()

        Assert.assertEquals(0, result.keys.size)
    }

    @Test
    fun `When building with page applies page`() {
        val page = 3
        val result = PersonService.Builder().page(page).build()

        Assert.assertEquals(page.toString(), result[PersonService.Builder.PAGE_KEY])
    }
}
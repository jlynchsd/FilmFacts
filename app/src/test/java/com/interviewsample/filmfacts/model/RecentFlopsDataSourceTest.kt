package com.interviewsample.filmfacts.model

import com.movietrivia.filmfacts.model.RecentFlopsDataSource
import org.junit.Assert
import org.junit.Before
import org.junit.Test

class RecentFlopsDataSourceTest {

    private lateinit var dataSource: RecentFlopsDataSource

    @Before
    fun setup() {
        dataSource = RecentFlopsDataSource()
    }

    @Test
    fun `When movie is added is considered a recent movie`() {
        val elements = IntRange(1, 4).toList()

        elements.forEach {
            dataSource.addMovie(it, true)
        }

        elements.forEach {
            Assert.assertTrue(dataSource.isRecentMovie(it))
        }
    }

    @Test
    fun `When adding elements then resetting flops then only removes flop movies`() {
        val flops = IntRange(1, 4).toList()
        val nonFlops = IntRange(5, 8).toList()

        flops.forEach {
            dataSource.addMovie(it, true)
        }
        nonFlops.forEach {
            dataSource.addMovie(it, false)
        }

        dataSource.resetFlops()

        flops.forEach {
            Assert.assertFalse(dataSource.isRecentMovie(it))
        }
        nonFlops.forEach {
            Assert.assertTrue(dataSource.isRecentMovie(it))
        }
    }

    @Test
    fun `When adding elements then resetting flops then sets page back to 1`() {
        val flops = IntRange(1, 4).toList()
        val nonFlops = IntRange(5, 8).toList()

        flops.forEach {
            dataSource.addMovie(it, true)
        }
        nonFlops.forEach {
            dataSource.addMovie(it, false)
        }
        dataSource.page = 5

        dataSource.resetFlops()

        Assert.assertEquals(1, dataSource.page)
    }

    @Test
    fun `When adding elements exceeds cache only removes earliest elements`() {
        val elements = IntRange(0, 40).toList()

        elements.forEach {
            dataSource.addMovie(it, false)
        }

        Assert.assertFalse(dataSource.isRecentMovie(elements[0]))

        elements.subList(1, elements.size).forEach {
            Assert.assertTrue(dataSource.isRecentMovie(it))
        }
    }

    @Test
    fun `When setting page to a lower value than the current page ignores update`() {
        dataSource.page = 5
        dataSource.page = 3

        Assert.assertEquals(5, dataSource.page)
    }
}
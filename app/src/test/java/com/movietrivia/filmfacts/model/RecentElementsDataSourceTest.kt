package com.movietrivia.filmfacts.model

import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class RecentElementsDataSourceTest {

    private lateinit var dataSource: RecentElementsDataSource

    @Before
    fun setup() = runBlocking {
        dataSource = RecentElementsDataSource(ApplicationProvider.getApplicationContext(), "foo").apply {
            resetElements()
        }
    }

    @Test
    fun `When adding elements then they become recent elements`() {
        val elements = IntRange(1, 4).toList()

        elements.forEach {
            dataSource.addElement(it)
        }

        elements.forEach {
            Assert.assertTrue(dataSource.isRecentElement(it))
        }
    }

    @Test
    fun `When adding elements then resetting recent elements then none are recent`() {
        val elements = IntRange(1, 4).toList()

        elements.forEach {
            dataSource.addElement(it)
        }

        dataSource.resetRecentElements()

        elements.forEach {
            Assert.assertFalse(dataSource.isRecentElement(it))
        }
    }

    @Test
    fun `When adding elements exceeds cache only removes earliest elements`() {
        val elements = IntRange(0, 40).toList()

        elements.forEach {
            dataSource.addElement(it)
        }

        Assert.assertFalse(dataSource.isRecentElement(elements[0]))

        elements.subList(1, elements.size).forEach {
            Assert.assertTrue(dataSource.isRecentElement(it))
        }
    }

    @Test
    fun `When elements are added and then datasource is saved and loaded restores saved state`() = runTest {
        val elements = IntRange(1, 4).toList()

        elements.forEach {
            dataSource.addElement(it)
        }

        dataSource.saveElements()

        dataSource.resetRecentElements()

        elements.forEach {
            Assert.assertFalse(dataSource.isRecentElement(it))
        }

        dataSource.loadElements()

        elements.forEach {
            Assert.assertTrue(dataSource.isRecentElement(it))
        }
    }
}
package com.movietrivia.filmfacts.model

import androidx.test.core.app.ApplicationProvider
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class UserHistoryDataSourceTest {

    private lateinit var dataSource: UserHistoryDataSource

    @Before
    fun setup() = runBlocking {
        dataSource = UserHistoryDataSource(ApplicationProvider.getApplicationContext(), mockk(relaxed = true)).also {
            it.reset()
        }
    }

    @Test
    fun `When created with no value uses default value`() = runTest {
        Assert.assertEquals(UserHistory(0), dataSource.userHistory.first())
    }

    @Test
    fun `When updating user history exposes updated value`() = runTest {
        val updatedUserHistory = UserHistory(
            1,
            correctAnswers = 1
        )

        dataSource.updateUserHistory(updatedUserHistory)

        Assert.assertEquals(updatedUserHistory, dataSource.userHistory.first())
    }

    @Test
    fun `When resetting user history exposes default value`() = runTest {
        val updatedUserHistory = UserHistory(
            1,
            correctAnswers = 1
        )

        dataSource.updateUserHistory(updatedUserHistory)

        Assert.assertEquals(updatedUserHistory, dataSource.userHistory.first())

        dataSource.reset()

        Assert.assertEquals(UserHistory(0), dataSource.userHistory.first())
    }
}
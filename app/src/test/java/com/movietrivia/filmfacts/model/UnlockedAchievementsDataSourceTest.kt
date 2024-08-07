package com.movietrivia.filmfacts.model

import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class UnlockedAchievementsDataSourceTest {

    private lateinit var dataSource: UnlockedAchievementsDataSource

    @Before
    fun setup() = runBlocking {
        dataSource = UnlockedAchievementsDataSource(ApplicationProvider.getApplicationContext()).also {
            it.reset()
        }
    }

    @Test
    fun `When created with no value uses default value`() = runTest {
        Assert.assertEquals(UnlockedAchievements(), dataSource.unlockedAchievements.first())
    }

    @Test
    fun `When updating unlocked achievements exposes updated value`() = runTest {
        val updatedAchievements = UnlockedAchievements(
            achievements = setOf(Achievement.FINISH_ACHIEVEMENTS),
            newAchievements = true
        )

        dataSource.updateUnlockedAchievements(updatedAchievements)

        Assert.assertEquals(updatedAchievements, dataSource.unlockedAchievements.first())
    }

    @Test
    fun `When resetting unlocked achievements exposes default value`() = runTest {
        val updatedAchievements = UnlockedAchievements(
            achievements = setOf(Achievement.FINISH_ACHIEVEMENTS),
            newAchievements = true
        )

        dataSource.updateUnlockedAchievements(updatedAchievements)

        Assert.assertEquals(updatedAchievements, dataSource.unlockedAchievements.first())

        dataSource.reset()

        Assert.assertEquals(UnlockedAchievements(), dataSource.unlockedAchievements.first())
    }
}
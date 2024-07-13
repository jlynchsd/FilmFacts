package com.interviewsample.filmfacts.model

import androidx.test.core.app.ApplicationProvider
import com.movietrivia.filmfacts.model.UserSettings
import com.movietrivia.filmfacts.model.UserSettingsDataSource
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class UserSettingsDataSourceTest {

    private lateinit var dataSource: UserSettingsDataSource

    @Before
    fun setup() = runBlocking {
        dataSource = UserSettingsDataSource(ApplicationProvider.getApplicationContext()).also {
            it.reset()
        }
    }

    @Test
    fun `When created with no value uses default value`() = runTest {
        Assert.assertEquals(UserSettings(), dataSource.userSettings.first())
    }

    @Test
    fun `When updating user settings exposes updated value`() = runTest {
        val updatedSettings = UserSettings(language = "es")

        dataSource.updateUserSettings(updatedSettings)

        Assert.assertEquals(updatedSettings, dataSource.userSettings.first())
    }

    @Test
    fun `When resetting user settings exposes default value`() = runTest {
        val updatedSettings = UserSettings(language = "es")

        dataSource.updateUserSettings(updatedSettings)

        Assert.assertEquals(updatedSettings, dataSource.userSettings.first())

        dataSource.reset()

        Assert.assertEquals(UserSettings(), dataSource.userSettings.first())
    }
}
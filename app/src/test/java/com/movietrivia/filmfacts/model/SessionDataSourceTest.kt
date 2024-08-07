package com.movietrivia.filmfacts.model

import android.content.SharedPreferences
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.verify
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SessionDataSourceTest {

    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var datsSource: SessionDataSource

    @Before
    fun setup() {
        mockkObject(EncryptedSharedPreferencesFactory)
        sharedPreferences = mockk(relaxed = true)
        every {
            EncryptedSharedPreferencesFactory.createSharedPrefs(any(), any())
        } returns sharedPreferences

        datsSource = SessionDataSource(mockk())
    }

    @After
    fun teardown() {
        clearAllMocks()
    }

    @Test
    fun `When initially created has no session`() {
        every {
            sharedPreferences.getString(any(), any())
        } returns null

        Assert.assertNull(SessionDataSource(mockk()).sessionId.value)
    }

    @Test
    fun `When created with existing session loads session`() {
        val session = "foo"
        every {
            sharedPreferences.getString(any(), any())
        } returns session

        val newDataSource = SessionDataSource(mockk())

        Assert.assertEquals(session, newDataSource.sessionId.value)
    }

    @Test
    fun `When setting a session updates flow and shared preferences`() {
        val session = "foo"

        datsSource.setSessionId(session)

        Assert.assertEquals(session, datsSource.sessionId.value)
        verify(exactly = 1) { sharedPreferences.edit() }
    }

    @Test
    fun `When clearing a session updates flow and shared preferences`() {
        val session = "foo"

        datsSource.setSessionId(session)

        Assert.assertEquals(session, datsSource.sessionId.value)

        datsSource.clearSessionId()

        Assert.assertNull(datsSource.sessionId.value)
        verify(exactly = 2) { sharedPreferences.edit() }
    }
}
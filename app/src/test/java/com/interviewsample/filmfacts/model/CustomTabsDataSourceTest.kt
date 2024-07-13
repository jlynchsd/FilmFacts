package com.interviewsample.filmfacts.model

import androidx.browser.customtabs.CustomTabsClient
import androidx.browser.customtabs.CustomTabsServiceConnection
import com.movietrivia.filmfacts.model.CustomTabsDataSource
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.verify
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test

class CustomTabsDataSourceTest {

    private lateinit var dataSource: CustomTabsDataSource

    @Before
    fun setup() {
        dataSource = CustomTabsDataSource(mockk())
        mockkStatic(CustomTabsClient::class)
    }

    @After
    fun teardown() {
        clearAllMocks()
    }

    @Test
    fun `When starting session but no available package does not start tabs`() {
        every {
            CustomTabsClient.getPackageName(any(), any())
        } returns null

        Assert.assertFalse(dataSource.startSession())
    }

    @Test
    fun `When starting session with available package binds custom tab service`() {
        every {
            CustomTabsClient.getPackageName(any(), any())
        } returns "foo"
        every {
            CustomTabsClient.bindCustomTabsService(any(), any(), any())
        } returns true

        dataSource.startSession()

        verify { CustomTabsClient.bindCustomTabsService(any(), any(), any()) }
    }

    @Test
    fun `When custom tab service connected warms up custom tab client and exposes session`() {
        every {
            CustomTabsClient.getPackageName(any(), any())
        } returns "foo"
        val connectionSlot = slot<CustomTabsServiceConnection>()
        every {
            CustomTabsClient.bindCustomTabsService(any(), any(), capture(connectionSlot))
        } returns true
        val mockClient = mockk<CustomTabsClient>(relaxed = true)
        every {
            mockClient.newSession(any())
        } returns mockk()

        dataSource.startSession()

        connectionSlot.captured.onCustomTabsServiceConnected(mockk(), mockClient)
        verify { mockClient.warmup(0) }
        verify { mockClient.newSession(null) }
        Assert.assertNotNull(dataSource.tabSession.value)
    }

    @Test
    fun `When custom tab service disconnected removes exposed session`() {
        every {
            CustomTabsClient.getPackageName(any(), any())
        } returns "foo"
        val connectionSlot = slot<CustomTabsServiceConnection>()
        every {
            CustomTabsClient.bindCustomTabsService(any(), any(), capture(connectionSlot))
        } returns true
        val mockClient = mockk<CustomTabsClient>(relaxed = true)
        every {
            mockClient.newSession(any())
        } returns mockk()

        dataSource.startSession()

        connectionSlot.captured.onCustomTabsServiceConnected(mockk(), mockClient)
        Assert.assertNotNull(dataSource.tabSession.value)

        connectionSlot.captured.onServiceDisconnected(mockk())
        Assert.assertNull(dataSource.tabSession.value)
    }

    @Test
    fun `When started with active session does not rebind session`() {
        every {
            CustomTabsClient.getPackageName(any(), any())
        } returns "foo"
        val connectionSlot = slot<CustomTabsServiceConnection>()
        every {
            CustomTabsClient.bindCustomTabsService(any(), any(), capture(connectionSlot))
        } returns true
        val mockClient = mockk<CustomTabsClient>(relaxed = true)
        every {
            mockClient.newSession(any())
        } returns mockk()

        dataSource.startSession()

        connectionSlot.captured.onCustomTabsServiceConnected(mockk(), mockClient)

        dataSource.startSession()

        verify(exactly = 1) { CustomTabsClient.bindCustomTabsService(any(), any(), any()) }
    }

    @Test
    fun `When custom tabs package is available then custom tabs are supported`() {
        every {
            CustomTabsClient.getPackageName(any(), any())
        } returns "foo"

        Assert.assertTrue(CustomTabsDataSource.customTabsSupported(mockk()))
    }

    @Test
    fun `When custom tabs package is not available then custom tabs are not supported`() {
        every {
            CustomTabsClient.getPackageName(any(), any())
        } returns null

        Assert.assertFalse(CustomTabsDataSource.customTabsSupported(mockk()))
    }
}
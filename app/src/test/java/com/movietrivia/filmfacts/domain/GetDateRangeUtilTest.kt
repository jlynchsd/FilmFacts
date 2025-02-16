package com.movietrivia.filmfacts.domain

import com.movietrivia.filmfacts.model.CalendarProvider
import com.movietrivia.filmfacts.model.UserSettings
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.util.Calendar
import java.util.Date

@RunWith(RobolectricTestRunner::class)
class GetDateRangeUtilTest {

    // region MULTI_YEAR strategy
    @Test
    fun `When using multi year strategy picks multi year range`() {
        val mockProvider = mockProvider()
        val mockCalendar = mockProvider.instance()
        getDateRange(UserSettings(releasedAfterOffset = 5, releasedBeforeOffset = 7), mockProvider, "", DateStrategies.MULTI_YEAR)

        verify {
            mockCalendar.add(Calendar.YEAR, any())
            mockCalendar.set(Calendar.DAY_OF_MONTH, 1)
            mockCalendar.set(Calendar.MONTH, 0)

            mockCalendar.add(Calendar.YEAR, any())
            mockCalendar.set(Calendar.DAY_OF_MONTH, 31)
            mockCalendar.set(Calendar.MONTH, 11)
        }
    }

    @Test
    fun `When using multi year strategy with no date range the picks multi year range`() {
        val mockProvider = mockProvider()
        val mockCalendar = mockProvider.instance()
        getDateRange(UserSettings(), mockProvider, "", DateStrategies.MULTI_YEAR)

        verify {
            mockCalendar.add(Calendar.YEAR, any())
            mockCalendar.set(Calendar.DAY_OF_MONTH, 1)
            mockCalendar.set(Calendar.MONTH, 0)

            mockCalendar.add(Calendar.YEAR, any())
            mockCalendar.set(Calendar.DAY_OF_MONTH, 31)
            mockCalendar.set(Calendar.MONTH, 11)
        }
    }

    // endregion

    // region MAX strategy

    @Test
    fun `When using max strategy with no start and end offsets then no date range is set`() {
        val mockProvider = mockProvider()
        val result = getDateRange(UserSettings(), mockProvider, "", DateStrategies.MAX)

        Assert.assertNull(result.first)
        Assert.assertNull(result.second)
    }

    @Test
    fun `When using max strategy with start and end offsets uses provided range`() {
        val mockProvider = mockProvider()
        val mockCalendar = mockProvider.instance()
        getDateRange(UserSettings(releasedAfterOffset = 5, releasedBeforeOffset = 7), mockProvider, "", DateStrategies.MAX)

        verify {
            mockCalendar.add(Calendar.YEAR, -7)
            mockCalendar.set(Calendar.DAY_OF_MONTH, 1)
            mockCalendar.set(Calendar.MONTH, 0)

            mockCalendar.add(Calendar.YEAR, -5)
            mockCalendar.set(Calendar.DAY_OF_MONTH, 31)
            mockCalendar.set(Calendar.MONTH, 11)
        }
    }

    @Test
    fun `When using max strategy with start and end offsets equal uses 1 year range`() {
        val mockProvider = mockProvider()
        val mockCalendar = mockProvider.instance()
        getDateRange(UserSettings(releasedAfterOffset = 5, releasedBeforeOffset = 5), mockProvider, "", DateStrategies.MAX)

        verify {
            mockCalendar.add(Calendar.YEAR, -5)
            mockCalendar.set(Calendar.DAY_OF_MONTH, 1)
            mockCalendar.set(Calendar.MONTH, 0)

            mockCalendar.add(Calendar.YEAR, -5)
            mockCalendar.set(Calendar.DAY_OF_MONTH, 31)
            mockCalendar.set(Calendar.MONTH, 11)
        }
    }

    @Test
    fun `When using max strategy with start time after end time uses default range`() {
        val mockProvider = mockProvider()
        val mockCalendar = mockProvider.instance()
        getDateRange(UserSettings(releasedAfterOffset = 7, releasedBeforeOffset = 5), mockProvider, "", DateStrategies.MAX)

        verify {
            mockCalendar.add(Calendar.YEAR, -49)
            mockCalendar.set(Calendar.DAY_OF_MONTH, 1)
            mockCalendar.set(Calendar.MONTH, 0)

            mockCalendar.add(Calendar.YEAR, 0)
            mockCalendar.set(Calendar.DAY_OF_MONTH, 31)
            mockCalendar.set(Calendar.MONTH, 11)
        }
    }

    private fun mockProvider(): CalendarProvider {
        val mockCalendar = mockk<Calendar>(relaxed = true)

        every {
            mockCalendar.time
        } returns Date(0)

        val mockProvider = mockk<CalendarProvider>()
        every {
            mockProvider.instance()
        } returns mockCalendar

        return mockProvider
    }

    // endregion
}
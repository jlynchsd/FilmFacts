package com.movietrivia.filmfacts.domain

import com.movietrivia.filmfacts.model.CalendarProvider
import com.movietrivia.filmfacts.model.UserSettings
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Test
import java.util.Calendar
import java.util.Date

class GetMovieFiltersUtilTest {

    // region RECENT strategy

    @Test
    fun `When using recent strategy picks 90 day date range`() {
        val mockProvider = mockProvider()
        val mockCalendar = mockProvider.instance()
        getMovieDateRange(UserSettings(), mockProvider, DateStrategies.RECENT)

        verify {
            mockCalendar.add(Calendar.DAY_OF_MONTH, -90)
        }
    }

    // endregion

    // region SINGLE_YEAR strategy

    @Test
    fun `When using single year strategy picks 1 year range`() {
        val mockProvider = mockProvider()
        val mockCalendar = mockProvider.instance()
        getMovieDateRange(UserSettings(), mockProvider, DateStrategies.SINGLE_YEAR)

        verify {
            mockCalendar.add(Calendar.YEAR, any())
            mockCalendar.set(Calendar.DAY_OF_MONTH, 1)
            mockCalendar.set(Calendar.MONTH, 0)

            mockCalendar.set(Calendar.DAY_OF_MONTH, 31)
            mockCalendar.set(Calendar.MONTH, 11)
        }
    }

    // endregion

    // region MULTI_YEAR strategy
    @Test
    fun `When using multi year strategy picks multi year range`() {
        val mockProvider = mockProvider()
        val mockCalendar = mockProvider.instance()
        getMovieDateRange(UserSettings(), mockProvider, DateStrategies.MULTI_YEAR)

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
    fun `When using max strategy with no start and end offsets defaults to 50 year range`() {
        val mockProvider = mockProvider()
        val mockCalendar = mockProvider.instance()
        getMovieDateRange(UserSettings(), mockProvider, DateStrategies.MAX)

        verify {
            mockCalendar.add(Calendar.YEAR, -49)
            mockCalendar.set(Calendar.DAY_OF_MONTH, 1)
            mockCalendar.set(Calendar.MONTH, 0)

            mockCalendar.add(Calendar.YEAR, 0)
            mockCalendar.set(Calendar.DAY_OF_MONTH, 31)
            mockCalendar.set(Calendar.MONTH, 11)
        }
    }

    @Test
    fun `When using max strategy with start and end offsets uses provided range`() {
        val mockProvider = mockProvider()
        val mockCalendar = mockProvider.instance()
        getMovieDateRange(UserSettings(releasedAfterOffset = 5, releasedBeforeOffset = 7), mockProvider, DateStrategies.MAX)

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
        getMovieDateRange(UserSettings(releasedAfterOffset = 5, releasedBeforeOffset = 5), mockProvider, DateStrategies.MAX)

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
        getMovieDateRange(UserSettings(releasedAfterOffset = 7, releasedBeforeOffset = 5), mockProvider, DateStrategies.MAX)

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
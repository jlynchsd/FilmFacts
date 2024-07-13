package com.movietrivia.filmfacts.domain

import com.movietrivia.filmfacts.model.CalendarProvider
import com.movietrivia.filmfacts.model.UserSettings
import java.util.*

fun getMovieDateRange(
    userSettings: UserSettings,
    calendarProvider: CalendarProvider,
    forceStrategy: DateStrategies? = null,
): Pair<Date, Date> {
    val strategies = DateStrategies.values().toMutableList()
    if ((userSettings.releasedAfterOffset ?: 0) >= 1) {
        strategies.remove(DateStrategies.RECENT)
    }

    return when (forceStrategy ?: strategies.random()) {
        DateStrategies.RECENT -> {
            with (calendarProvider.instance()) {
                val endDate = time
                add(Calendar.DAY_OF_MONTH, -90)
                val startDate = time
                Pair(startDate, endDate)
            }
        }

        DateStrategies.SINGLE_YEAR -> {
            val endOffset = userSettings.releasedAfterOffset ?: 0
            val startOffset = userSettings.releasedBeforeOffset ?: 50
            val yearOffset = (endOffset..startOffset).random()
            with (calendarProvider.instance()) {
                add(Calendar.YEAR, -yearOffset)
                set(Calendar.DAY_OF_MONTH, 1)
                set(Calendar.MONTH, 0)
                val startDate = time
                set(Calendar.DAY_OF_MONTH, 31)
                set(Calendar.MONTH, 11)
                val endDate = time
                Pair(startDate, endDate)
            }
        }

        DateStrategies.MULTI_YEAR -> {
            val endOffset = userSettings.releasedAfterOffset ?: 0
            val startOffset = userSettings.releasedBeforeOffset ?: 50
            val startYearOffset = (endOffset + 1 .. startOffset).random()
            val endYearOffset = (endOffset ..startYearOffset).random()

            val startDate = with (calendarProvider.instance()) {
                add(Calendar.YEAR, -startYearOffset)
                set(Calendar.DAY_OF_MONTH, 1)
                set(Calendar.MONTH, 0)
                time
            }

            val endDate = with (calendarProvider.instance()) {
                add(Calendar.YEAR, -endYearOffset)
                set(Calendar.DAY_OF_MONTH, 31)
                set(Calendar.MONTH, 11)
                time
            }

            Pair(startDate, endDate)
        }

        DateStrategies.MAX -> {
            var endOffset = userSettings.releasedAfterOffset ?: 0
            var startOffset = userSettings.releasedBeforeOffset ?: 49
            if (endOffset > startOffset) {
                endOffset = 0
                startOffset = 49
            }
            val startDate = with (calendarProvider.instance()) {
                add(Calendar.YEAR, -startOffset)
                set(Calendar.DAY_OF_MONTH, 1)
                set(Calendar.MONTH, 0)
                time
            }

            val endDate = with (calendarProvider.instance()) {
                add(Calendar.YEAR, -endOffset)
                set(Calendar.DAY_OF_MONTH, 31)
                set(Calendar.MONTH, 11)
                time
            }

            Pair(startDate, endDate)
        }
    }
}

enum class DateStrategies {
    RECENT,
    SINGLE_YEAR,
    MULTI_YEAR,
    MAX
}
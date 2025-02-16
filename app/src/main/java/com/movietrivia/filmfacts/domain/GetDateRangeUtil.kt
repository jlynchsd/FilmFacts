package com.movietrivia.filmfacts.domain

import android.annotation.SuppressLint
import com.movietrivia.filmfacts.api.Logger
import com.movietrivia.filmfacts.model.CalendarProvider
import com.movietrivia.filmfacts.model.UserSettings
import java.text.SimpleDateFormat
import java.util.*

fun getDateRange(
    userSettings: UserSettings,
    calendarProvider: CalendarProvider,
    logTag: String,
    forceStrategy: DateStrategies? = null
): Pair<Date?, Date?> {
    val strategies = DateStrategies.values()

    return when (forceStrategy ?: strategies.random()) {
        DateStrategies.MULTI_YEAR -> {
            val endOffset = userSettings.releasedAfterOffset ?: 0
            val startOffset = userSettings.releasedBeforeOffset ?: 100
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

            Logger.debug(logTag, "Selected Multi Year Date Range from $startDate to $endDate")
            Pair(startDate, endDate)
        }

        DateStrategies.MAX -> {
            var endOffset = userSettings.releasedAfterOffset ?: 0
            var startOffset = userSettings.releasedBeforeOffset ?: 49
            if (endOffset > startOffset) {
                endOffset = 0
                startOffset = 49
            }
            val startDate = if (userSettings.releasedBeforeOffset != null) {
                with (calendarProvider.instance()) {
                    add(Calendar.YEAR, -startOffset)
                    set(Calendar.DAY_OF_MONTH, 1)
                    set(Calendar.MONTH, 0)
                    time
                }
            } else {
                null
            }

            val endDate = if (userSettings.releasedAfterOffset != null) {
                with (calendarProvider.instance()) {
                    add(Calendar.YEAR, -endOffset)
                    set(Calendar.DAY_OF_MONTH, 31)
                    set(Calendar.MONTH, 11)
                    time
                }
            } else  {
                null
            }

            Logger.debug(logTag, "Selected Max Date Range from $startDate to $endDate")
            Pair(startDate, endDate)
        }
    }
}

enum class DateStrategies {
    MULTI_YEAR,
    MAX
}

@SuppressLint("SimpleDateFormat")
fun dateToTimestamp(date: String?, calendarProvider: CalendarProvider) =
    date?.let {
        runCatching { SimpleDateFormat("yyyy-MM-dd").parse(it) }
            .getOrNull()?.let {
                with(calendarProvider.instance()) {
                    time = it
                    timeInMillis
                }
            }
    }

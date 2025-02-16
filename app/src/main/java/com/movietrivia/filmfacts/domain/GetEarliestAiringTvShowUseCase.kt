package com.movietrivia.filmfacts.domain

import android.content.Context
import android.text.format.DateFormat
import com.movietrivia.filmfacts.R
import com.movietrivia.filmfacts.api.DiscoverService
import com.movietrivia.filmfacts.api.Logger
import com.movietrivia.filmfacts.model.CalendarProvider
import com.movietrivia.filmfacts.model.FilmFactsRepository
import com.movietrivia.filmfacts.model.ImageType
import com.movietrivia.filmfacts.model.RecentPromptsRepository
import com.movietrivia.filmfacts.model.UiImageEntry
import com.movietrivia.filmfacts.model.UiImagePrompt
import com.movietrivia.filmfacts.model.UiPrompt
import com.movietrivia.filmfacts.model.UserDataRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class GetEarliestAiringTvShowUseCase(
    private val applicationContext: Context,
    private val filmFactsRepository: FilmFactsRepository,
    private val recentPromptsRepository: RecentPromptsRepository,
    private val userDataRepository: UserDataRepository,
    private val calendarProvider: CalendarProvider,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO
): UseCase {

    override suspend fun invoke(includeGenres: List<Int>?) =
        withContext(dispatcher) {
            getPrompt(includeGenres)
        }

    private suspend fun getPrompt(includeGenres: List<Int>?): UiPrompt? {
        val userSettings = userDataRepository.tvShowUserSettings.firstOrNullCatching() ?: return null
        val tvShows = getTvShows(
            filmFactsRepository,
            recentPromptsRepository,
            getDateRange(userSettings, calendarProvider, LOG_TAG, DateStrategies.MAX),
            DiscoverService.Builder.TvShowOrder.POPULARITY_DESC,
            includeGenres,
            true,
            logTag = LOG_TAG
        )
        Logger.debug(LOG_TAG, "Tv Shows: ${tvShows?.size}")

        if (tvShows != null) {
            val filteredTvShows = getTvShowDetails(
                filmFactsRepository,
                tvShows,
                4,
                { it.firstAirDate != null && dateToTimestamp(it.firstAirDate, calendarProvider) != null && it.posterPath.isNotEmpty() }
            ) { shows -> shows.distinctBy { it.firstAirDate }.toMutableList() }.toMutableList()

            Logger.debug(LOG_TAG, "Filtered Tv Shows: ${filteredTvShows.size}")

            if (filteredTvShows.size == 4) {
                val uiImageEntries = filteredTvShows.sortedBy { dateToTimestamp(it.firstAirDate, calendarProvider) }.mapIndexed { index, filteredTvShow ->
                    UiImageEntry(
                        filmFactsRepository.getImageUrl(filteredTvShow.posterPath, ImageType.POSTER) ?: "",
                        index == 0,
                        data = DateFormat.format("MMM dd, yyyy", dateToTimestamp(filteredTvShow.firstAirDate, calendarProvider) ?: 0).toString()
                    )
                }.shuffled()

                val success = preloadImages(applicationContext, *uiImageEntries.map { it.imagePath }.toTypedArray())

                Logger.debug(LOG_TAG, "Preloaded Images: $success")

                if (success) {
                    return UiImagePrompt(
                        uiImageEntries,
                        R.string.earliest_airing_tv_show_title
                    )
                }
            }
        }

        Logger.info(LOG_TAG, "Unable to generate prompt")
        return null
    }

    private companion object {
        const val LOG_TAG = "GetEarliestAiringTvShowUseCase"
    }
}
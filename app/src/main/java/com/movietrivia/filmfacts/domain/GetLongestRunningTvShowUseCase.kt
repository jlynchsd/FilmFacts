package com.movietrivia.filmfacts.domain

import android.content.Context
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

class GetLongestRunningTvShowUseCase(
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
                { it.numberOfEpisodes > 0 && it.posterPath.isNotEmpty() }
            ) { shows -> shows.distinctBy { it.numberOfEpisodes }.toMutableList() }.toMutableList()
            Logger.debug(LOG_TAG, "Filtered Tv Shows: ${filteredTvShows.size}")

            if (filteredTvShows.size >= 4) {
                filteredTvShows.forEach { recentPromptsRepository.addRecentTvShow(it.id) }
                filteredTvShows.sortByDescending { it.numberOfEpisodes }
                val uiImageEntries = filteredTvShows.mapIndexed { index, filteredTvShow ->
                    UiImageEntry(
                        filmFactsRepository.getImageUrl(filteredTvShow.posterPath, ImageType.POSTER) ?: "",
                        index == 0,
                        data = "${filteredTvShow.numberOfEpisodes} ${applicationContext.resources.getQuantityString(R.plurals.episode_counter, filteredTvShow.numberOfEpisodes) }"
                    )
                }.shuffled()

                val success = preloadImages(applicationContext, *uiImageEntries.map { it.imagePath }.toTypedArray())

                Logger.debug(LOG_TAG, "Preloaded Images: $success")

                if (success) {
                    return UiImagePrompt(
                        uiImageEntries,
                        R.string.longest_running_tv_show_title
                    )
                }
            }
        }

        Logger.info(LOG_TAG, "Unable to generate prompt")
        return null
    }

    private companion object {
        const val LOG_TAG = "GetLongestRunningTvShowUseCase"
    }
}
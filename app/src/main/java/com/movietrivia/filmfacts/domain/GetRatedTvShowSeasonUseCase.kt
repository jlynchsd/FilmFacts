package com.movietrivia.filmfacts.domain

import android.content.Context
import androidx.annotation.VisibleForTesting
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

class GetRatedTvShowSeasonUseCase(
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

    @VisibleForTesting
    internal suspend fun getPrompt(includeGenres: List<Int>?, forceStrategy: RatedStrategies? = null): UiPrompt? {
        val userSettings = userDataRepository.tvShowUserSettings.firstOrNullCatching() ?: return null
        val dateRange = getDateRange(userSettings, calendarProvider, LOG_TAG, DateStrategies.MAX)
        val tvShows = getTvShows(
            filmFactsRepository,
            recentPromptsRepository,
            dateRange,
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
                1,
                { show ->
                    show.seasons.map { it.voteAverage }.distinct().filter { it != 0f }.size >= 4 &&
                    show.seasons.distinctBy { it.voteAverage }.filter { it.voteAverage != 0f && it.posterPath.isNotEmpty() }.size >= 4
                }
            ).toMutableList()

            Logger.debug(LOG_TAG, "Filtered Tv Shows: ${filteredTvShows.size}")

            if (filteredTvShows.isNotEmpty()) {
                val tvShow = filteredTvShows.first()
                val strategy = forceStrategy ?: RatedStrategies.values().random()
                recentPromptsRepository.addRecentTvShow(tvShow.id)
                val filteredSeasons = tvShow.seasons.distinctBy { it.voteAverage }.filter { it.voteAverage != 0f && it.posterPath.isNotEmpty() }.shuffled()
                Logger.debug(LOG_TAG, "Filtered Seasons: ${filteredSeasons.size}")
                if (filteredSeasons.size >= 4) {
                    val selectedSeasons = filteredSeasons.subList(0, 4)
                    val ratedSeasons = if (strategy == RatedStrategies.TOP_RATED) {
                        selectedSeasons.sortedByDescending { it.voteAverage }
                    } else {
                        selectedSeasons.sortedBy { it.voteAverage }
                    }

                    val title = if (strategy == RatedStrategies.TOP_RATED) {
                        R.string.highest_rated_tv_show_season_title
                    } else {
                        R.string.worst_rated_tv_show_season_title
                    }

                    val uiImageEntries = ratedSeasons.mapIndexed { index, filteredTvShowSeason ->
                        UiImageEntry(
                            filmFactsRepository.getImageUrl(filteredTvShowSeason.posterPath, ImageType.POSTER) ?: "",
                            index == 0,
                            "${applicationContext.getString(R.string.tv_show_season)} ${filteredTvShowSeason.seasonNumber}",
                            "${filteredTvShowSeason.voteAverage}"
                        )
                    }.shuffled()

                    val success = preloadImages(applicationContext, *uiImageEntries.map { it.imagePath }.toTypedArray())

                    Logger.debug(LOG_TAG, "Preloaded Images: $success")

                    if (success) {
                        return UiImagePrompt(
                            uiImageEntries,
                            title,
                            listOf(tvShow.name)
                        )
                    }
                }
            }
        }

        Logger.info(LOG_TAG, "Unable to generate prompt")
        return null
    }

    @VisibleForTesting
    internal enum class RatedStrategies {
        TOP_RATED,
        WORST_RATED
    }

    private companion object {
        const val LOG_TAG = "GetRatedTvShowSeasonUseCase"
    }
}
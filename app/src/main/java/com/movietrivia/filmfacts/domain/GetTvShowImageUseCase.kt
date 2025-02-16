package com.movietrivia.filmfacts.domain

import android.content.Context
import com.movietrivia.filmfacts.R
import com.movietrivia.filmfacts.api.DiscoverService
import com.movietrivia.filmfacts.api.DiscoverTvShow
import com.movietrivia.filmfacts.api.Logger
import com.movietrivia.filmfacts.api.TvShowImage
import com.movietrivia.filmfacts.model.CalendarProvider
import com.movietrivia.filmfacts.model.FilmFactsRepository
import com.movietrivia.filmfacts.model.ImageType
import com.movietrivia.filmfacts.model.RecentPromptsRepository
import com.movietrivia.filmfacts.model.UiImageEntry
import com.movietrivia.filmfacts.model.UiPrompt
import com.movietrivia.filmfacts.model.UiTextEntry
import com.movietrivia.filmfacts.model.UiTextPrompt
import com.movietrivia.filmfacts.model.UserDataRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class GetTvShowImageUseCase(
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

        val imagelessTvShows = mutableListOf<DiscoverTvShow>()

        if (!tvShows.isNullOrEmpty()) {
            var currentTvShow: DiscoverTvShow
            var currentImage: TvShowImage?
            do {
                currentTvShow = tvShows.random()
                tvShows.remove(currentTvShow)
                currentImage = getTvShowImage(filmFactsRepository, currentTvShow, LOG_TAG)
                if (currentImage == null) {
                    imagelessTvShows.add(currentTvShow)
                }
            } while (tvShows.isNotEmpty() && currentImage == null)

            currentImage?.let { tvShowImage ->
                val remainingTvShows =  (tvShows + imagelessTvShows).toMutableList()
                Logger.debug(LOG_TAG, "Remaining Tv Shows: ${remainingTvShows.size}")

                if (remainingTvShows.size >= 3) {
                    val otherTvShows = List(3) {
                        remainingTvShows.random().also {
                            remainingTvShows.remove(it)
                        }
                    }

                    recentPromptsRepository.addRecentMovie(currentTvShow.id)

                    val textEntries = (listOf(
                        UiTextEntry(
                            true,
                            currentTvShow.name
                        )
                    ) + otherTvShows.map {
                        UiTextEntry(
                            false,
                            it.name
                        )
                    }).shuffled()

                    val imageEntry = UiImageEntry(
                        filmFactsRepository.getImageUrl(tvShowImage.filePath, ImageType.BACKDROP) ?: "",
                        false
                    )

                    val success = preloadImages(applicationContext, imageEntry.imagePath)

                    Logger.debug(LOG_TAG, "Preloaded Images: $success")

                    if (success) {
                        return UiTextPrompt(
                            textEntries,
                            listOf(imageEntry),
                            true,
                            R.string.tv_show_image_title
                        )
                    }
                }
            }
        }

        Logger.info(LOG_TAG, "Unable to generate prompt")
        return null
    }

    private companion object {
        const val LOG_TAG = "GetTvShowImageUseCase"
    }
}
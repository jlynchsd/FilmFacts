package com.movietrivia.filmfacts.domain

import android.content.Context
import com.movietrivia.filmfacts.R
import com.movietrivia.filmfacts.api.ActorMovieCredits
import com.movietrivia.filmfacts.api.Logger
import com.movietrivia.filmfacts.model.*
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class GetMovieActorRolesUseCase(
    private val applicationContext: Context,
    private val filmFactsRepository: FilmFactsRepository,
    private val recentPromptsRepository: RecentPromptsRepository,
    private val userDataRepository: UserDataRepository,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO
) : UseCase {

    override suspend fun invoke(includeGenres: List<Int>?) =
        withContext(dispatcher) {
            getPrompt(includeGenres)
        }

    private suspend fun getPrompt(includeGenres: List<Int>?): UiPrompt? {
        val userSettings = userDataRepository.movieUserSettings.firstOrNullCatching() ?: return null
        val popularActors = getMovieActors(
            filmFactsRepository,
            recentPromptsRepository,
            includeGenres
        ).toMutableList()

        Logger.debug(LOG_TAG, "Popular Actors: ${popularActors.size}")

        val targetCreditsCount = 4
        val creditFilter = { actorCredit: ActorMovieCredits ->
            !userSettings.excludedGenres.containsAny(actorCredit.genreIds) &&
                    hasGenreId(actorCredit.genreIds, includeGenres) &&
                    userSettings.language == actorCredit.originalLanguage &&
                    dateWithinRange(actorCredit.releaseDate, userSettings.releasedAfterOffset, userSettings.releasedBeforeOffset) &&
                    actorCredit.characterName.isNotBlank() &&
                    actorCredit.movieTitle.isNotBlank() &&
                    !actorCredit.characterName.lowercase().contains("self") && // only actual roles
                    !actorCredit.characterName.lowercase().contains("#") && // numbered characters are usually bad
                    !actorCredit.characterName.lowercase().contains("/") && // multiple characters arent great
                    actorCredit.voteCount > 20 && // avoid obscure roles
                    actorCredit.order <= 5 // high enough billing to actually matter
        }

        if (popularActors.size >= 2) {
            val actorCredits = getActorMovieCredits(
                filmFactsRepository,
                recentPromptsRepository,
                popularActors,
                (1 until targetCreditsCount),
                creditFilter
            )

            Logger.debug(LOG_TAG, "Actor Credits: $actorCredits")

            if (actorCredits != null) {
                val requiredCredits = targetCreditsCount - actorCredits.third.size
                val otherActorCredits = getActorMovieCredits(
                    filmFactsRepository,
                    recentPromptsRepository,
                    popularActors,
                    (requiredCredits..requiredCredits),
                    creditFilter,
                    actorCredits.first
                )

                Logger.debug(LOG_TAG, "Other Actor Credits: $otherActorCredits")

                if (otherActorCredits != null) {
                    recentPromptsRepository.addRecentActor(actorCredits.first.id)

                    val textEntries = (actorCredits.third.map {
                        UiTextEntry(
                            true,
                            it.characterName,
                            it.movieTitle
                        )
                    } + otherActorCredits.third.map {
                        UiTextEntry(
                            false,
                            it.characterName,
                            it.movieTitle
                        )
                    }).shuffled()

                    val imageEntry = UiImageEntry(
                        filmFactsRepository.getImageUrl(actorCredits.second, ImageType.PROFILE) ?: "",
                        false
                    )

                    val success = preloadImages(applicationContext, imageEntry.imagePath)

                    Logger.debug(LOG_TAG, "Preloaded Images: $success")

                    if (success) {
                        return UiTextPrompt(
                            textEntries,
                            listOf(imageEntry),
                            false,
                            R.string.actor_roles_title,
                            listOf(actorCredits.first.name)
                        )
                    }
                }
            }
        }

        Logger.info(LOG_TAG, "Unable to generate prompt")
        return null
    }

    private fun hasGenreId(actorGenreIds: List<Int>, requiredGenreIds: List<Int>?) =
        requiredGenreIds?.let {
            actorGenreIds.containsAll(it)
        } ?: true

    private companion object {
        const val LOG_TAG = "GetMovieActorRolesUseCase"
    }
}
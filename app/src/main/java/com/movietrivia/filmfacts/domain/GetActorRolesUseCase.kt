package com.movietrivia.filmfacts.domain

import android.content.Context
import com.movietrivia.filmfacts.R
import com.movietrivia.filmfacts.api.ActorCredits
import com.movietrivia.filmfacts.model.*
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class GetActorRolesUseCase(
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
        val userSettings = userDataRepository.userSettings.firstOrNullCatching() ?: return null
        val popularActors = getActors(
            filmFactsRepository,
            recentPromptsRepository,
            includeGenres
        ).toMutableList()

        val targetCreditsCount = 4
        val creditFilter = { actorCredit: ActorCredits ->
            !userSettings.excludedFilmGenres.containsAny(actorCredit.genreIds) &&
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
            val actorCredits = getActorCredits(
                filmFactsRepository,
                recentPromptsRepository,
                popularActors,
                (1 until targetCreditsCount),
                creditFilter
            )

            if (actorCredits != null) {
                val requiredCredits = targetCreditsCount - actorCredits.third.size
                val otherActorCredits = getActorCredits(
                    filmFactsRepository,
                    recentPromptsRepository,
                    popularActors,
                    (requiredCredits..requiredCredits),
                    creditFilter,
                    actorCredits.first
                )

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

        return null
    }

    private fun hasGenreId(actorGenreIds: List<Int>, requiredGenreIds: List<Int>?) =
        requiredGenreIds?.let {
            actorGenreIds.containsAll(it)
        } ?: true
}
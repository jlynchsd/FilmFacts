package com.movietrivia.filmfacts.domain

import android.content.Context
import com.movietrivia.filmfacts.R
import com.movietrivia.filmfacts.api.ActorCredits
import com.movietrivia.filmfacts.api.MovieGenre
import com.movietrivia.filmfacts.model.*
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class GetVoiceActorRolesUseCase(
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
        if (userSettings.excludedFilmGenres.contains(MovieGenre.ANIMATION.key)) {
            return null
        }
        val genres = if (includeGenres != null) {
            (listOf(MovieGenre.ANIMATION.key) + includeGenres).distinct()
        } else {
            listOf(MovieGenre.ANIMATION.key)
        }
        val popularActors = getActors(
            filmFactsRepository,
            recentPromptsRepository,
            genres
        ).toMutableList()

        val targetCreditsCount = 4
        val strategy = VoiceActorStrategy.values().random()
        val minCredits = when (strategy) {
            VoiceActorStrategy.WAS_VOICED -> 1
            VoiceActorStrategy.WAS_NOT_VOICED -> 3
        }

        val creditFilter = { actorCredit: ActorCredits ->
            !userSettings.excludedFilmGenres.containsAny(actorCredit.genreIds) &&
                    actorCredit.genreIds.containsAll(genres) &&
                    userSettings.language == actorCredit.originalLanguage &&
                    dateWithinRange(actorCredit.releaseDate, userSettings.releasedAfterOffset, userSettings.releasedBeforeOffset) &&
                    actorCredit.characterName.isNotBlank() &&
                    actorCredit.movieTitle.isNotBlank() &&
                    !actorCredit.characterName.lowercase().contains("self") && // only actual roles
                    !actorCredit.characterName.lowercase().contains("additional") && // only actual roles
                    !actorCredit.characterName.lowercase().contains("#") && // numbered characters are usually bad
                    !actorCredit.characterName.lowercase().contains("/") && // multiple characters arent great
                    !actorCredit.characterName.lowercase().contains(",") && // multiple characters arent great
                    actorCredit.characterName.filter { it == ' ' }.length <= 2 && // lots of words are usually unnamed characters
                    actorCredit.voteCount > 20 && // avoid obscure roles
                    actorCredit.order <= 5 // high enough billing to actually matter
        }

        if (popularActors.size >= 2) {
            val actorCredits = getActorCredits(
                filmFactsRepository,
                recentPromptsRepository,
                popularActors,
                (minCredits until targetCreditsCount),
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
                            strategy == VoiceActorStrategy.WAS_VOICED,
                            it.characterName,
                            it.movieTitle
                        )
                    } + otherActorCredits.third.map {
                        UiTextEntry(
                            strategy == VoiceActorStrategy.WAS_NOT_VOICED,
                            it.characterName,
                            it.movieTitle
                        )
                    }).shuffled()

                    val imageEntry = UiImageEntry(
                        filmFactsRepository.getImageUrl(actorCredits.second, ImageType.PROFILE) ?: "",
                        false
                    )

                    val success = preloadImages(applicationContext, imageEntry.imagePath)

                    val promptTitle = when (strategy) {
                        VoiceActorStrategy.WAS_VOICED -> R.string.actor_voice_roles_title
                        VoiceActorStrategy.WAS_NOT_VOICED -> R.string.actor_voice_roles_reversed_title
                    }

                    if (success) {
                        return UiTextPrompt(
                            textEntries,
                            listOf(imageEntry),
                            false,
                            promptTitle,
                            listOf(actorCredits.first.name)
                        )
                    }
                }
            }
        }

        return null
    }
}

private enum class VoiceActorStrategy {
    WAS_VOICED,
    WAS_NOT_VOICED
}
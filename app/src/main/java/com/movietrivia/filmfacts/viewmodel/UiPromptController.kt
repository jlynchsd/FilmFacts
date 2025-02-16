package com.movietrivia.filmfacts.viewmodel

import com.movietrivia.filmfacts.domain.*
import com.movietrivia.filmfacts.model.PromptState
import com.movietrivia.filmfacts.model.RecentPromptsRepository
import com.movietrivia.filmfacts.model.UiPrompt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import javax.inject.Inject

class UiPromptController @Inject internal constructor(
    private val recentPromptsRepository: RecentPromptsRepository,
    voiceActorRolesUseCase: GetVoiceActorMovieRolesUseCase,
    topGrossingUseCase: GetTopGrossingMoviesUseCase,
    moviesStarringActorUseCase: GetMoviesStarringActorUseCase,
    scoredMoviesStarringActorUseCase: GetScoredMoviesStarringActorUseCase,
    biggestFilmographyUseCase: GetBiggestFilmographyUseCase,
    earliestFilmographyUseCase: GetEarliestFilmographyUseCase,
    actorRolesUseCase: GetMovieActorRolesUseCase,
    movieImageUseCase: GetMovieImageUseCase,
    longestRunningTvShowUseCase: GetLongestRunningTvShowUseCase,
    tvShowImageUseCase: GetTvShowImageUseCase,
    ratedTvShowSeasonUseCase: GetRatedTvShowSeasonUseCase,
    earliestAiringTvShowUseCase: GetEarliestAiringTvShowUseCase,
    voiceActorTvShowRolesUseCase: GetVoiceActorTvShowRolesUseCase,
    tvShowsStarringActorUseCase: GetTvShowsStarringActorUseCase,
    tvShowActorRolesUseCase: GetTvShowActorRolesUseCase,
    tvShowActorLongestRunningCharacterUseCase: GetTvShowActorLongestRunningCharacterUseCase
) {
    private val _prompt = MutableStateFlow<PromptState>(PromptState.None)
    val prompt: StateFlow<PromptState> = _prompt
    private var currentPromptGroup = 0

    var remainingPrompts = 0
        private set

    private val prompts = listOf(
        PromptGroup(
            topGrossingUseCase,
            moviesStarringActorUseCase,
            scoredMoviesStarringActorUseCase,
            biggestFilmographyUseCase,
            earliestFilmographyUseCase,
            actorRolesUseCase,
            voiceActorRolesUseCase,
            movieImageUseCase
        ),
        PromptGroup(
            longestRunningTvShowUseCase,
            ratedTvShowSeasonUseCase,
            earliestAiringTvShowUseCase,
            voiceActorTvShowRolesUseCase,
            tvShowImageUseCase,
            tvShowsStarringActorUseCase,
            tvShowActorRolesUseCase,
            tvShowActorLongestRunningCharacterUseCase
        )
    )

    fun nextPrompt() {
        _prompt.value = if (!prompts[currentPromptGroup].cachedPrompts.isEmpty()) {
             PromptState.Ready(prompts[currentPromptGroup].cachedPrompts.removeFirst())
        } else {
            if (remainingPrompts > 0) {
                PromptState.None
            } else {
                PromptState.Finished
            }
        }
    }

    fun resetPrompts(group: Int = currentPromptGroup, resetFailureCounts: Boolean = true) {
        _prompt.value = PromptState.None
        remainingPrompts = 0
        prompts[group].cachedPrompts.clear()

        if (resetFailureCounts) {
            prompts[group].prompts.reset()
        }
    }

    suspend fun loadPrompts(requestedGenre: Int?, loadCount: Int) {
        remainingPrompts = loadCount
        coroutineScope {
            var attempts = 0
            var loadedPrompts = 0
            do {
                val loadChunk = if (prompt.value == PromptState.None) {
                    1
                } else {
                    2.coerceAtMost(remainingPrompts)
                }
                val results = List(loadChunk) {
                    async {
                        kotlin.runCatching { prompts[currentPromptGroup].prompts.pickUseCase() }.getOrNull()?.let { prompt ->
                            val includeGenres = if (requestedGenre != null) {
                                listOf(requestedGenre)
                            } else {
                                null
                            }
                            prompt.invoke(includeGenres).also {
                                if (it == null) {
                                    prompts[currentPromptGroup].prompts.failed(prompt)
                                } else {
                                    prompts[currentPromptGroup].prompts.succeeded(prompt)
                                }
                            }
                        }
                    }
                }.awaitAll().filterNotNull().toMutableList()
                attempts += loadChunk
                remainingPrompts -= results.size
                loadedPrompts += results.size
                results.forEach { prompts[currentPromptGroup].cachedPrompts.add(it) }
                if (prompt.value == PromptState.None && results.isNotEmpty()) {
                    nextPrompt()
                }
            } while (remainingPrompts > 0 && attempts < 2 * loadCount)

            remainingPrompts = 0

            if (attempts >= 2 * loadCount && loadedPrompts == 0) {
                recentPromptsRepository.reset()
                withContext(Dispatchers.Main) {
                    _prompt.value = PromptState.Error
                }
            }
        }
    }

    fun updatePromptGroup(group: Int) {
        currentPromptGroup = group
    }
}

private class PromptGroup(
    vararg useCases: UseCase
) {
    val prompts = UseCasePicker(useCases.asList())
    val cachedPrompts = SynchronizedDequeue<UiPrompt>(PROMPT_CACHE_SIZE)

    private companion object {
        const val PROMPT_CACHE_SIZE = 7
    }
}

private class SynchronizedDequeue<T>(initialSize: Int) {
    private val lock = Any()
    private val cachedPrompts = ArrayDeque<T>(initialSize)

    fun removeFirst() = synchronized(lock) {
        cachedPrompts.removeFirst()
    }

    fun add(element: T) = synchronized(lock) {
        cachedPrompts.add(element)
    }

    fun isEmpty() = synchronized(lock) {
        cachedPrompts.isEmpty()
    }

    fun clear() = synchronized(lock) {
        cachedPrompts.clear()
    }
}
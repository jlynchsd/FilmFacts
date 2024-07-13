package com.movietrivia.filmfacts.viewmodel

import com.movietrivia.filmfacts.api.MovieGenre
import com.movietrivia.filmfacts.domain.*
import com.movietrivia.filmfacts.model.PromptState
import com.movietrivia.filmfacts.model.RecentPromptsRepository
import com.movietrivia.filmfacts.model.UiPrompt
import com.movietrivia.filmfacts.model.UserSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import java.util.Collections
import javax.inject.Inject

class UiPromptController @Inject internal constructor(
    private val recentPromptsRepository: RecentPromptsRepository,
    private val voiceActorRolesUseCase: GetVoiceActorRolesUseCase,
    topGrossingUseCase: GetTopGrossingMoviesUseCase,
    moviesStarringActorUseCase: GetMoviesStarringActorUseCase,
    scoredMoviesStarringActorUseCase: GetScoredMoviesStarringActorUseCase,
    biggestFilmographyUseCase: GetBiggestFilmographyUseCase,
    earliestFilmographyUseCase: GetEarliestFilmographyUseCase,
    actorRolesUseCase: GetActorRolesUseCase,
    movieImageUseCase: GetMovieImageUseCase
) {
    private val _prompt = MutableStateFlow<PromptState>(PromptState.None)
    val prompt: StateFlow<PromptState> = _prompt

    var remainingPrompts = 0
        private set

    private val prompts = EvenRandom(
        listOf(
            topGrossingUseCase,
            moviesStarringActorUseCase,
            scoredMoviesStarringActorUseCase,
            biggestFilmographyUseCase,
            earliestFilmographyUseCase,
            actorRolesUseCase,
            voiceActorRolesUseCase,
            movieImageUseCase
        )
    )
    private val blacklistedUseCases = Collections.synchronizedList(mutableListOf<UseCase>())

    private val cachedPrompts = SynchronizedDequeue<UiPrompt>(PROMPT_CACHE_SIZE)

    fun nextPrompt() {
        _prompt.value = if (!cachedPrompts.isEmpty()) {
             PromptState.Ready(cachedPrompts.removeFirst())
        } else {
            if (remainingPrompts > 0) {
                PromptState.None
            } else {
                PromptState.Finished
            }
        }
    }

    fun resetPrompts(userSettings: UserSettings) {
        _prompt.value = PromptState.None
        remainingPrompts = 0
        cachedPrompts.clear()
        blacklistedUseCases.forEach {
            prompts.addElement(it)
        }
        blacklistedUseCases.clear()

        if (MovieGenre.ANIMATION.key in userSettings.excludedFilmGenres) {
            blacklistUseCase(voiceActorRolesUseCase)
        }
    }

    suspend fun loadPrompts(requestedGenre: Int?, loadCount: Int) {
        remainingPrompts = loadCount
        coroutineScope {
            var attempts = 0
            do {
                val loadChunk = if (prompt.value == PromptState.None) {
                    1
                } else {
                    2.coerceAtMost(remainingPrompts)
                }
                val results = List(loadChunk) {
                    async {
                        kotlin.runCatching { prompts.random() }.getOrNull()?.let { prompt ->
                            val includeGenres = if (requestedGenre != null) {
                                listOf(requestedGenre)
                            } else {
                                null
                            }
                            prompt.invoke(includeGenres).also {
                                if (it == null) {
                                    blacklistUseCase(prompt)
                                }
                            }
                        }
                    }
                }.awaitAll().filterNotNull().toMutableList()
                attempts += loadChunk
                remainingPrompts -= results.size
                results.forEach { cachedPrompts.add(it) }
                if (prompt.value == PromptState.None && results.isNotEmpty()) {
                    nextPrompt()
                }
            } while (remainingPrompts > 0 && attempts < 2 * loadCount)

            remainingPrompts = 0

            if (attempts >= 2 * loadCount) {
                withContext(Dispatchers.Main) {
                    _prompt.value = if (cachedPrompts.isEmpty()) {
                        PromptState.Error
                    } else {
                        PromptState.Finished
                    }
                }
            }
        }
    }

    private fun blacklistUseCase(prompt: UseCase) {
        blacklistedUseCases.add(prompt)
        prompts.removeElement(prompt)
        if (prompts.isEmpty()) {
            blacklistedUseCases.forEach {
                prompts.addElement(it)
            }
            blacklistedUseCases.clear()
            recentPromptsRepository.reset()
        }
    }

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
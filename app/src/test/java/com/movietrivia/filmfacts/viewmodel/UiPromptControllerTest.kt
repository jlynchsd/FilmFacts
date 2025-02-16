package com.movietrivia.filmfacts.viewmodel

import com.movietrivia.filmfacts.domain.GetMovieActorRolesUseCase
import com.movietrivia.filmfacts.domain.GetBiggestFilmographyUseCase
import com.movietrivia.filmfacts.domain.GetEarliestAiringTvShowUseCase
import com.movietrivia.filmfacts.domain.GetEarliestFilmographyUseCase
import com.movietrivia.filmfacts.domain.GetLongestRunningTvShowUseCase
import com.movietrivia.filmfacts.domain.GetMovieImageUseCase
import com.movietrivia.filmfacts.domain.GetMoviesStarringActorUseCase
import com.movietrivia.filmfacts.domain.GetRatedTvShowSeasonUseCase
import com.movietrivia.filmfacts.domain.GetScoredMoviesStarringActorUseCase
import com.movietrivia.filmfacts.domain.GetTopGrossingMoviesUseCase
import com.movietrivia.filmfacts.domain.GetTvShowActorLongestRunningCharacterUseCase
import com.movietrivia.filmfacts.domain.GetTvShowActorRolesUseCase
import com.movietrivia.filmfacts.domain.GetTvShowImageUseCase
import com.movietrivia.filmfacts.domain.GetTvShowsStarringActorUseCase
import com.movietrivia.filmfacts.domain.GetVoiceActorMovieRolesUseCase
import com.movietrivia.filmfacts.domain.GetVoiceActorTvShowRolesUseCase
import com.movietrivia.filmfacts.domain.UseCase
import com.movietrivia.filmfacts.model.PromptState
import com.movietrivia.filmfacts.model.RecentPromptsRepository
import io.mockk.coEvery
import io.mockk.coJustAwait
import io.mockk.mockk
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.test.runTest
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class UiPromptControllerTest {

    private lateinit var recentPromptsRepository: RecentPromptsRepository
    private lateinit var voiceActorRolesUseCase: GetVoiceActorMovieRolesUseCase
    private lateinit var topGrossingUseCase: GetTopGrossingMoviesUseCase
    private lateinit var moviesStarringActorUseCase: GetMoviesStarringActorUseCase
    private lateinit var scoredMoviesStarringActorUseCase: GetScoredMoviesStarringActorUseCase
    private lateinit var biggestFilmographyUseCase: GetBiggestFilmographyUseCase
    private lateinit var earliestFilmographyUseCase: GetEarliestFilmographyUseCase
    private lateinit var actorRolesUseCase: GetMovieActorRolesUseCase
    private lateinit var movieImageUseCase: GetMovieImageUseCase
    private lateinit var getLongestRunningTvShowUseCase: GetLongestRunningTvShowUseCase
    private lateinit var tvShowImageUseCase: GetTvShowImageUseCase
    private lateinit var ratedTvShowSeasonUseCase: GetRatedTvShowSeasonUseCase
    private lateinit var earliestAiringTvShowUseCase: GetEarliestAiringTvShowUseCase
    private lateinit var voiceActorTvShowRolesUseCase: GetVoiceActorTvShowRolesUseCase
    private lateinit var tvShowsStarringActorUseCase: GetTvShowsStarringActorUseCase
    private lateinit var tvShowActorRolesUseCase: GetTvShowActorRolesUseCase
    private lateinit var tvShowActorLongestRunningCharacterUseCase: GetTvShowActorLongestRunningCharacterUseCase
    private lateinit var useCases: List<UseCase>

    private lateinit var controller: UiPromptController

    @Before
    fun setup() {
        recentPromptsRepository = mockk(relaxed = true)
        voiceActorRolesUseCase = mockk(relaxed = true)
        topGrossingUseCase = mockk(relaxed = true)
        moviesStarringActorUseCase = mockk(relaxed = true)
        scoredMoviesStarringActorUseCase = mockk(relaxed = true)
        biggestFilmographyUseCase = mockk(relaxed = true)
        earliestFilmographyUseCase = mockk(relaxed = true)
        actorRolesUseCase = mockk(relaxed = true)
        movieImageUseCase = mockk(relaxed = true)
        getLongestRunningTvShowUseCase = mockk(relaxed = true)
        tvShowImageUseCase = mockk(relaxed = true)
        ratedTvShowSeasonUseCase = mockk(relaxed = true)
        earliestAiringTvShowUseCase = mockk(relaxed = true)
        voiceActorTvShowRolesUseCase = mockk(relaxed = true)
        tvShowsStarringActorUseCase = mockk(relaxed = true)
        tvShowActorRolesUseCase = mockk(relaxed = true)
        tvShowActorLongestRunningCharacterUseCase = mockk(relaxed = true)

        useCases = listOf(
            voiceActorRolesUseCase,
            topGrossingUseCase,
            moviesStarringActorUseCase,
            scoredMoviesStarringActorUseCase,
            biggestFilmographyUseCase,
            earliestFilmographyUseCase,
            actorRolesUseCase,
            movieImageUseCase,
            getLongestRunningTvShowUseCase,
            tvShowImageUseCase,
            ratedTvShowSeasonUseCase,
            earliestAiringTvShowUseCase,
            voiceActorTvShowRolesUseCase,
            tvShowsStarringActorUseCase,
            tvShowActorRolesUseCase,
            tvShowActorLongestRunningCharacterUseCase
        )
        useCases.forEach {
            coEvery {
                it.invoke(any())
            } returns mockk()
        }

        controller = UiPromptController(
            recentPromptsRepository,
            voiceActorRolesUseCase,
            topGrossingUseCase,
            moviesStarringActorUseCase,
            scoredMoviesStarringActorUseCase,
            biggestFilmographyUseCase,
            earliestFilmographyUseCase,
            actorRolesUseCase,
            movieImageUseCase,
            getLongestRunningTvShowUseCase,
            tvShowImageUseCase,
            ratedTvShowSeasonUseCase,
            earliestAiringTvShowUseCase,
            voiceActorTvShowRolesUseCase,
            tvShowsStarringActorUseCase,
            tvShowActorRolesUseCase,
            tvShowActorLongestRunningCharacterUseCase
        )
    }

    @Test
    fun `When successfully loading prompts posts results`() = runTest {
        Assert.assertEquals(PromptState.None, controller.prompt.value)

        controller.loadPrompts(0, 7)


        Assert.assertTrue(controller.prompt.value is PromptState.Ready)
    }

    @Test
    fun `When successfully loading all prompts and then getting all prompts posts finished`() = runTest {
        val loadCount = 3
        controller.loadPrompts(null, loadCount)
        repeat(loadCount) {
            Assert.assertTrue(controller.prompt.value is PromptState.Ready)
            controller.nextPrompt()
        }

        Assert.assertEquals(PromptState.Finished, controller.prompt.value)
    }

    @Test
    fun `When unable to load use cases posts error`() = runViewModelScope {
        useCases.forEach {
            coEvery {
                it.invoke(any())
            } returns null
        }

        controller.loadPrompts(null, 5)

        Assert.assertEquals(PromptState.Error, controller.prompt.value)
    }

    @Test
    fun `When resetting prompts posts none`() = runTest {
        controller.loadPrompts(null, 3)

        Assert.assertTrue(controller.prompt.value is PromptState.Ready)

        controller.resetPrompts()

        Assert.assertEquals(PromptState.None, controller.prompt.value)
    }

    @Test
    fun `Given remaining but no cached prompts when getting next prompt then prompts posts none`() = runTest {
        useCases.forEach {
            coJustAwait {
                it.invoke(any())
            }
        }
        val mutex = Mutex(true)

        val job = launch {
            mutex.unlock()
            controller.loadPrompts(null, 5)
        }
        mutex.withLock {
            controller.nextPrompt()
            Assert.assertEquals(PromptState.None, controller.prompt.value)
            job.cancel()
        }
    }
}
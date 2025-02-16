package com.movietrivia.filmfacts.viewmodel

import androidx.lifecycle.Lifecycle
import com.movietrivia.filmfacts.domain.AwardAchievementsUseCase
import com.movietrivia.filmfacts.domain.GetMovieGenreImagesUseCase
import com.movietrivia.filmfacts.domain.GetTvShowGenreImagesUseCase
import com.movietrivia.filmfacts.model.CalendarProvider
import com.movietrivia.filmfacts.model.RecentPromptsRepository
import com.movietrivia.filmfacts.model.UiGenre
import com.movietrivia.filmfacts.model.UnlockedAchievements
import com.movietrivia.filmfacts.model.UserDataRepository
import com.movietrivia.filmfacts.model.UserProgressRepository
import com.movietrivia.filmfacts.model.UserSettings
import io.mockk.CapturingSlot
import io.mockk.coEvery
import io.mockk.coJustAwait
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import okio.IOException
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class FilmFactsViewModelTest {

    private lateinit var userDataRepository: UserDataRepository
    private lateinit var userProgressRepository: UserProgressRepository
    private lateinit var recentPromptsRepository: RecentPromptsRepository
    private lateinit var uiPromptController: UiPromptController
    private lateinit var awardAchievementsUseCase: AwardAchievementsUseCase
    private lateinit var movieGenreImagesUseCase: GetMovieGenreImagesUseCase
    private lateinit var tvShowGenreImagesUseCase: GetTvShowGenreImagesUseCase
    private lateinit var calendarProvider: CalendarProvider
    private lateinit var viewModel: FilmFactsViewModel

    @Before
    fun setup() {
        userDataRepository = mockk(relaxed = true)
        userProgressRepository = mockk(relaxed = true)
        recentPromptsRepository = mockk(relaxed = true)
        uiPromptController = mockk(relaxed = true)
        awardAchievementsUseCase = mockk(relaxed = true)
        movieGenreImagesUseCase = mockk(relaxed = true)
        tvShowGenreImagesUseCase = mockk(relaxed = true)
        calendarProvider = mockk(relaxed = true)

        val movieUserSettingsFlow = MutableSharedFlow<UserSettings>(replay = 1)
        movieUserSettingsFlow.tryEmit(UserSettings())
        every {
            userDataRepository.movieUserSettings
        } returns movieUserSettingsFlow

        val tvShowUserSettingsFlow = MutableSharedFlow<UserSettings>(replay = 1)
        tvShowUserSettingsFlow.tryEmit(UserSettings())
        every {
            userDataRepository.tvShowUserSettings
        } returns tvShowUserSettingsFlow

        viewModel = FilmFactsViewModel(
            userDataRepository,
            userProgressRepository,
            recentPromptsRepository,
            uiPromptController,
            awardAchievementsUseCase,
            movieGenreImagesUseCase,
            tvShowGenreImagesUseCase,
            calendarProvider
        )
    }

    @Test
    fun `When created loads user data`() {
        coVerify { userDataRepository.loadAccountDetails() }
    }

    @Test
    fun `When created loads recent prompts`() {
        coVerify { recentPromptsRepository.loadData() }
    }

    @Test
    fun `When created loads user's movie genre images and fetches next set of movie genre images`() = runViewModelScope {
        coEvery {
            movieGenreImagesUseCase.invoke()
        } returns listOf(
            UiGenre("", 0),
            UiGenre("", 1),
            UiGenre("", 2)
        )

        val userSettingsFlow = MutableSharedFlow<UserSettings>(replay = 1)
        userSettingsFlow.tryEmit(UserSettings(excludedGenres = listOf(1)))
        every {
            userDataRepository.movieUserSettings
        } returns userSettingsFlow

        viewModel = FilmFactsViewModel(
            userDataRepository,
            userProgressRepository,
            recentPromptsRepository,
            uiPromptController,
            awardAchievementsUseCase,
            movieGenreImagesUseCase,
            tvShowGenreImagesUseCase,
            calendarProvider
        ).apply {
            setActivePromptGroup(UserSettings(excludedGenres = listOf(1)), FilmFactsViewModel.PromptGroup.MOVIES)
        }

        Assert.assertEquals(2, viewModel.genreImages.value.size)
        viewModel.genreImages.value.forEach {
            Assert.assertNotEquals(1, it.genreId)
        }

        coVerify { movieGenreImagesUseCase.loadNextGenreImages(any()) }
    }

    @Test
    fun `When created loads user's tv show genre images and fetches next set of tv show genre images`() = runViewModelScope {
        coEvery {
            tvShowGenreImagesUseCase.invoke()
        } returns listOf(
            UiGenre("", 0),
            UiGenre("", 1),
            UiGenre("", 2)
        )

        val userSettingsFlow = MutableSharedFlow<UserSettings>(replay = 1)
        userSettingsFlow.tryEmit(UserSettings(excludedGenres = listOf(1)))
        every {
            userDataRepository.tvShowUserSettings
        } returns userSettingsFlow

        viewModel = FilmFactsViewModel(
            userDataRepository,
            userProgressRepository,
            recentPromptsRepository,
            uiPromptController,
            awardAchievementsUseCase,
            movieGenreImagesUseCase,
            tvShowGenreImagesUseCase,
            calendarProvider
        ).apply {
            setActivePromptGroup(UserSettings(excludedGenres = listOf(1)), FilmFactsViewModel.PromptGroup.TV_SHOWS)
        }

        Assert.assertEquals(2, viewModel.genreImages.value.size)
        viewModel.genreImages.value.forEach {
            Assert.assertNotEquals(1, it.genreId)
        }

        coVerify { tvShowGenreImagesUseCase.loadNextGenreImages(any()) }
    }

    @Test
    fun `When unable to get movie user settings uses default settings`() = runViewModelScope {
        every {
            userDataRepository.movieUserSettings
        } returns kotlinx.coroutines.flow.flow {
            throw IOException()
        }

        movieGenreImagesUseCase = mockk(relaxed = true)
        viewModel = FilmFactsViewModel(
            userDataRepository,
            userProgressRepository,
            recentPromptsRepository,
            uiPromptController,
            awardAchievementsUseCase,
            movieGenreImagesUseCase,
            tvShowGenreImagesUseCase,
            calendarProvider
        )

        coVerify { movieGenreImagesUseCase.loadNextGenreImages(UserSettings()) }
    }

    @Test
    fun `When unable to get tv show user settings uses default settings`() = runViewModelScope {
        every {
            userDataRepository.tvShowUserSettings
        } returns kotlinx.coroutines.flow.flow {
            throw IOException()
        }

        tvShowGenreImagesUseCase = mockk(relaxed = true)
        viewModel = FilmFactsViewModel(
            userDataRepository,
            userProgressRepository,
            recentPromptsRepository,
            uiPromptController,
            awardAchievementsUseCase,
            movieGenreImagesUseCase,
            tvShowGenreImagesUseCase,
            calendarProvider
        )

        coVerify { tvShowGenreImagesUseCase.loadNextGenreImages(UserSettings()) }
    }

    @Test
    fun `When getting next prompt delegates to prompt controller`() = runViewModelScope {
        viewModel.nextPrompt()

        verify { uiPromptController.nextPrompt() }
    }

    @Test
    fun `When requesting a genre resets prompts`() = runViewModelScope {
        viewModel.requestGenre(1)

        verify { uiPromptController.resetPrompts() }
    }

    @Test
    fun `When loading prompts delegates to prompt controller`() = runViewModelScope {
        val loadCount = 3
        viewModel.loadPrompts(loadCount)

        coVerify { uiPromptController.loadPrompts(any(), loadCount) }
    }

    @Test
    fun `When requesting genre and then loading prompts loads prompts with requested genre`() = runViewModelScope {
        val genre = 7
        viewModel.requestGenre(genre)
        viewModel.loadPrompts(3)

        coVerify { uiPromptController.loadPrompts(genre, any()) }
    }

    @Test
    fun `When cancelling prompts resets prompts`() = runViewModelScope {
        coJustAwait {
            uiPromptController.loadPrompts(any(), any())
        }

        viewModel.loadPrompts(0)
        viewModel.cancelPrompts()

        verify { uiPromptController.resetPrompts(resetFailureCounts = false) }
    }

    @Test
    fun `When updating movie user settings filters genres, persists data, and resets prompts`() = runViewModelScope {
        coEvery {
            movieGenreImagesUseCase.invoke()
        } returns listOf(
            UiGenre("", 0),
            UiGenre("", 1),
            UiGenre("", 2)
        )
        viewModel = FilmFactsViewModel(
            userDataRepository,
            userProgressRepository,
            recentPromptsRepository,
            uiPromptController,
            awardAchievementsUseCase,
            movieGenreImagesUseCase,
            tvShowGenreImagesUseCase,
            calendarProvider
        ).apply {
            setActivePromptGroup(UserSettings(), FilmFactsViewModel.PromptGroup.MOVIES)
        }

        val settings = UserSettings(excludedGenres = listOf(0, 2))
        viewModel.updateMovieUserSettings(settings)

        Assert.assertEquals(1, viewModel.genreImages.value.size)
        Assert.assertEquals(1, viewModel.genreImages.value.first().genreId)

        coVerify { userDataRepository.updateMovieUserSettings(settings) }
        coVerify { uiPromptController.resetPrompts(FilmFactsViewModel.PromptGroup.MOVIES.ordinal) }
    }

    @Test
    fun `When updating tv show user settings filters genres, persists data, and resets prompts`() = runViewModelScope {
        coEvery {
            tvShowGenreImagesUseCase.invoke()
        } returns listOf(
            UiGenre("", 0),
            UiGenre("", 1),
            UiGenre("", 2)
        )
        viewModel = FilmFactsViewModel(
            userDataRepository,
            userProgressRepository,
            recentPromptsRepository,
            uiPromptController,
            awardAchievementsUseCase,
            movieGenreImagesUseCase,
            tvShowGenreImagesUseCase,
            calendarProvider
        ).apply {
            setActivePromptGroup(UserSettings(), FilmFactsViewModel.PromptGroup.TV_SHOWS)
        }

        val settings = UserSettings(excludedGenres = listOf(0, 2))
        viewModel.updateTvShowUserSettings(settings)

        Assert.assertEquals(1, viewModel.genreImages.value.size)
        Assert.assertEquals(1, viewModel.genreImages.value.first().genreId)

        coVerify { userDataRepository.updateTvShowUserSettings(settings) }
        coVerify { uiPromptController.resetPrompts(FilmFactsViewModel.PromptGroup.TV_SHOWS.ordinal) }
    }

    @Test
    fun `When loading account details delegates to repository`() = runViewModelScope {
        viewModel.loadAccountDetails()

        coVerify { userDataRepository.loadAccountDetails() }
    }

    @Test
    fun `When pausing saves recent prompts data`() = runViewModelScope {
        viewModel.onStateChanged(mockk(), Lifecycle.Event.ON_PAUSE)

        coVerify { recentPromptsRepository.saveData() }
    }

    @Test
    fun `When resuming and remaining prompts loads remaining prompts`() = runViewModelScope {
        val remainingPrompts = 4
        every {
            uiPromptController.remainingPrompts
        } returns remainingPrompts

        viewModel.onStateChanged(mockk(), Lifecycle.Event.ON_RESUME)

        coVerify { uiPromptController.loadPrompts(any(), remainingPrompts) }
    }

    @Test
    fun `When resuming and no remaining prompts loads nothing`() = runViewModelScope {
        val remainingPrompts = 0
        every {
            uiPromptController.remainingPrompts
        } returns remainingPrompts

        viewModel.onStateChanged(mockk(), Lifecycle.Event.ON_RESUME)

        coVerify(exactly = 0) { uiPromptController.loadPrompts(any(), remainingPrompts) }
    }

    @Test
    fun `When unsupported lifecycle change does nothing`() = runViewModelScope {
        viewModel.onStateChanged(mockk(), Lifecycle.Event.ON_CREATE)

        coVerify(exactly = 0) { recentPromptsRepository.saveData() }
        coVerify(exactly = 0) { uiPromptController.loadPrompts(any(), any()) }
    }

    @Test
    fun `When awarding achievements delegates to use case`() = runViewModelScope {
        viewModel.awardAchievements()

        coVerify {
            awardAchievementsUseCase.invoke(null)
        }
    }

    @Test
    fun `When disabling new achievement sets new achievement to false`() = runViewModelScope {
        val unlockedAchievementsFlow = MutableStateFlow(UnlockedAchievements(newAchievements = true))
        every {
            userProgressRepository.unlockedAchievements
        } returns unlockedAchievementsFlow

        val captureSlot = CapturingSlot<UnlockedAchievements>()

        coEvery {
            userProgressRepository.updateUnlockedAchievements(capture(captureSlot))
        } just runs

        viewModel.disableNewAchievements()

        Assert.assertFalse(captureSlot.captured.newAchievements)
    }

}
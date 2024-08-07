package com.movietrivia.filmfacts.viewmodel

import androidx.lifecycle.Lifecycle
import com.movietrivia.filmfacts.domain.AwardAchievementsUseCase
import com.movietrivia.filmfacts.domain.GetGenreImagesUseCase
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
    private lateinit var genreImagesUseCase: GetGenreImagesUseCase
    private lateinit var calendarProvider: CalendarProvider
    private lateinit var viewModel: FilmFactsViewModel

    @Before
    fun setup() {
        userDataRepository = mockk(relaxed = true)
        userProgressRepository = mockk(relaxed = true)
        recentPromptsRepository = mockk(relaxed = true)
        uiPromptController = mockk(relaxed = true)
        awardAchievementsUseCase = mockk(relaxed = true)
        genreImagesUseCase = mockk(relaxed = true)
        calendarProvider = mockk(relaxed = true)

        val userSettingsFlow = MutableSharedFlow<UserSettings>(replay = 1)
        userSettingsFlow.tryEmit(UserSettings())
        every {
            userDataRepository.userSettings
        } returns userSettingsFlow

        viewModel = FilmFactsViewModel(
            userDataRepository,
            userProgressRepository,
            recentPromptsRepository,
            uiPromptController,
            awardAchievementsUseCase,
            genreImagesUseCase,
            calendarProvider
        )
    }

    @Test
    fun `When created loads user data`() {
        coVerify { userDataRepository.loadAccountDetails() }
    }

    @Test
    fun `When created loads recent prompts and clears ui prompts`() {
        coVerify { recentPromptsRepository.loadData() }
        coVerify { uiPromptController.resetPrompts(any()) }
    }

    @Test
    fun `When created loads user's genre images and fetches next set of genre images`() = runViewModelScope {
        coEvery {
            genreImagesUseCase.invoke()
        } returns listOf(
            UiGenre("", 0),
            UiGenre("", 1),
            UiGenre("", 2)
        )

        val userSettingsFlow = MutableSharedFlow<UserSettings>(replay = 1)
        userSettingsFlow.tryEmit(UserSettings(excludedFilmGenres = listOf(1)))
        every {
            userDataRepository.userSettings
        } returns userSettingsFlow

        viewModel = FilmFactsViewModel(
            userDataRepository,
            userProgressRepository,
            recentPromptsRepository,
            uiPromptController,
            awardAchievementsUseCase,
            genreImagesUseCase,
            calendarProvider
        )

        Assert.assertEquals(2, viewModel.genreImages.value.size)
        viewModel.genreImages.value.forEach {
            Assert.assertNotEquals(1, it.genreId)
        }

        coVerify { genreImagesUseCase.loadNextGenreImages(any()) }
    }

    @Test
    fun `When unable to get user settings uses default settings`() = runViewModelScope {
        every {
            userDataRepository.userSettings
        } returns kotlinx.coroutines.flow.flow {
            throw IOException()
        }

        genreImagesUseCase = mockk(relaxed = true)
        viewModel = FilmFactsViewModel(
            userDataRepository,
            userProgressRepository,
            recentPromptsRepository,
            uiPromptController,
            awardAchievementsUseCase,
            genreImagesUseCase,
            calendarProvider
        )

        coVerify { genreImagesUseCase.loadNextGenreImages(UserSettings()) }
    }

    @Test
    fun `When getting next prompt delegates to prompt controller`() = runViewModelScope {
        viewModel.nextPrompt()

        verify { uiPromptController.nextPrompt() }
    }

    @Test
    fun `When requesting a genre resets prompts`() = runViewModelScope {
        viewModel.requestGenre(1)

        verify { uiPromptController.resetPrompts(any()) }
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

        verify { uiPromptController.resetPrompts(any()) }
    }

    @Test
    fun `When updating user settings filters genres, persists data, and resets prompts`() = runViewModelScope {
        coEvery {
            genreImagesUseCase.invoke()
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
            genreImagesUseCase,
            calendarProvider
        )

        val settings = UserSettings(excludedFilmGenres = listOf(0, 2))
        viewModel.updateUserSettings(settings)

        Assert.assertEquals(1, viewModel.genreImages.value.size)
        Assert.assertEquals(1, viewModel.genreImages.value.first().genreId)

        coVerify { userDataRepository.updateUserSettings(settings) }
        coVerify { uiPromptController.resetPrompts(settings) }
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
package com.movietrivia.filmfacts.viewmodel

import androidx.lifecycle.*
import com.movietrivia.filmfacts.domain.AwardAchievementsUseCase
import com.movietrivia.filmfacts.domain.GetGenreImagesUseCase
import com.movietrivia.filmfacts.domain.firstOrNullCatching
import com.movietrivia.filmfacts.model.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import javax.inject.Inject

@HiltViewModel
class FilmFactsViewModel @Inject internal constructor(
    private val userDataRepository: UserDataRepository,
    private val userProgressRepository: UserProgressRepository,
    private val recentPromptsRepository: RecentPromptsRepository,
    private val uiPromptController: UiPromptController,
    private val awardAchievementsUseCase: AwardAchievementsUseCase,
    genreImagesUseCase: GetGenreImagesUseCase,
    calendarProvider: CalendarProvider
    ): ViewModel(), LifecycleEventObserver {

    val prompt = uiPromptController.prompt

    val userSettings = userDataRepository.userSettings.catch {}.stateIn(viewModelScope, SharingStarted.Eagerly, UserSettings())
    val userHistory = userProgressRepository.userHistory.stateIn(viewModelScope, SharingStarted.Eagerly, UserHistory(calendarProvider.instance().timeInMillis))
    val unlockedAchievements = userProgressRepository.unlockedAchievements.stateIn(viewModelScope, SharingStarted.Eagerly, UnlockedAchievements())
    val accountDetails = userDataRepository.accountDetails

    private var totalGenreImages: List<UiGenre> = emptyList()
    private val _genreImages = MutableStateFlow<List<UiGenre>>(emptyList())
    val genreImages: StateFlow<List<UiGenre>> = _genreImages

    private var requestedGenre: Int? = null
    private var promptsJob: Job? = null

    init {
        viewModelScope.launch {
            userDataRepository.loadAccountDetails()
            totalGenreImages = genreImagesUseCase.invoke()
            val userSettings = userDataRepository.userSettings.firstOrNullCatching() ?: UserSettings()
            updateGenreSelection(userSettings)
            recentPromptsRepository.loadData()
            uiPromptController.resetPrompts(userSettings)
            genreImagesUseCase.loadNextGenreImages(userSettings)
        }
    }

    fun nextPrompt() = uiPromptController.nextPrompt()

    fun loadPrompts(count: Int) {
        promptsJob?.cancel()
        promptsJob = viewModelScope.launch {
            uiPromptController.loadPrompts(requestedGenre, count)
        }
    }

    fun cancelPrompts() {
        promptsJob?.let {
            it.cancel()
            promptsJob = null
        }
        uiPromptController.resetPrompts(userSettings.value)
    }

    fun updateUserSettings(userSettings: UserSettings) {
        viewModelScope.launch {
            updateGenreSelection(userSettings)
            userDataRepository.updateUserSettings(userSettings)
            uiPromptController.resetPrompts(userSettings)
        }
    }

    fun requestGenre(genreId: Int?) {
        requestedGenre = genreId
        uiPromptController.resetPrompts(userSettings.value)
    }

    suspend fun loadAccountDetails() = userDataRepository.loadAccountDetails()

    override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
        when (event) {
            Lifecycle.Event.ON_PAUSE -> {
                viewModelScope.launch {
                    recentPromptsRepository.saveData()
                }
            }
            Lifecycle.Event.ON_RESUME -> {
                if (uiPromptController.remainingPrompts > 0) {
                    loadPrompts(uiPromptController.remainingPrompts)
                }
            }
            else -> {}
        }
    }

    suspend fun awardAchievements() = awardAchievementsUseCase(null)

    suspend fun disableNewAchievements() {
        userProgressRepository.updateUnlockedAchievements(unlockedAchievements.value.copy(newAchievements = false))
    }

    private fun updateGenreSelection(userSettings: UserSettings) {
        _genreImages.value = totalGenreImages.filter { !userSettings.excludedFilmGenres.contains(it.genreId) }
    }
}
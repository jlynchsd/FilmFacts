package com.movietrivia.filmfacts.viewmodel

import androidx.lifecycle.*
import com.movietrivia.filmfacts.domain.AwardAchievementsUseCase
import com.movietrivia.filmfacts.domain.GetMovieGenreImagesUseCase
import com.movietrivia.filmfacts.domain.GetTvShowGenreImagesUseCase
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
    movieGenreImagesUseCase: GetMovieGenreImagesUseCase,
    tvShowGenreImagesUseCase: GetTvShowGenreImagesUseCase,
    calendarProvider: CalendarProvider
    ): ViewModel(), LifecycleEventObserver {

    val prompt = uiPromptController.prompt

    val movieUserSettings = userDataRepository.movieUserSettings.catch {}.stateIn(viewModelScope, SharingStarted.Eagerly, UserSettings())
    val tvShowUserSettings = userDataRepository.tvShowUserSettings.catch {}.stateIn(viewModelScope, SharingStarted.Eagerly, UserSettings())
    val userHistory = userProgressRepository.userHistory.stateIn(viewModelScope, SharingStarted.Eagerly, UserHistory(calendarProvider.instance().timeInMillis))
    val unlockedAchievements = userProgressRepository.unlockedAchievements.stateIn(viewModelScope, SharingStarted.Eagerly, UnlockedAchievements())
    val accountDetails = userDataRepository.accountDetails

    private var totalMovieGenreImages: List<UiGenre> = emptyList()
    private var totalTvShowGenreImages: List<UiGenre> = emptyList()
    private val _genreImages = MutableStateFlow<List<UiGenre>>(emptyList())
    val genreImages: StateFlow<List<UiGenre>> = _genreImages

    var promptGroup = PromptGroup.MOVIES
        private set
    private var requestedGenre: Int? = null
    private var promptsJob: Job? = null

    init {
        viewModelScope.launch {
            userDataRepository.loadAccountDetails()
            totalMovieGenreImages = movieGenreImagesUseCase.invoke()
            totalTvShowGenreImages = tvShowGenreImagesUseCase.invoke()
            val movieUserSettings = userDataRepository.movieUserSettings.firstOrNullCatching() ?: UserSettings()
            val tvShowUserSettings = userDataRepository.tvShowUserSettings.firstOrNullCatching() ?: UserSettings()
            setActivePromptGroup(movieUserSettings, promptGroup)
            recentPromptsRepository.loadData()
            movieGenreImagesUseCase.loadNextGenreImages(movieUserSettings)
            tvShowGenreImagesUseCase.loadNextGenreImages(tvShowUserSettings)
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
        uiPromptController.resetPrompts(resetFailureCounts = false)
    }

    fun setActivePromptGroup(userSettings: UserSettings, group: PromptGroup) {
        promptGroup = group
        updateGenreSelection(userSettings)
        uiPromptController.updatePromptGroup(promptGroup.ordinal)
    }

    fun updateMovieUserSettings(userSettings: UserSettings) {
        viewModelScope.launch {
            if (promptGroup == PromptGroup.MOVIES) {
                updateGenreSelection(userSettings)
            }

            uiPromptController.resetPrompts(PromptGroup.MOVIES.ordinal)
            userDataRepository.updateMovieUserSettings(userSettings)
        }
    }

    fun updateTvShowUserSettings(userSettings: UserSettings) {
        viewModelScope.launch {
            if (promptGroup == PromptGroup.TV_SHOWS) {
                updateGenreSelection(userSettings)
            }

            uiPromptController.resetPrompts(PromptGroup.TV_SHOWS.ordinal)
            userDataRepository.updateTvShowUserSettings(userSettings)
        }
    }

    fun requestGenre(genreId: Int?) {
        uiPromptController.resetPrompts(resetFailureCounts = genreId != requestedGenre)
        requestedGenre = genreId
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
        val genreList = when (promptGroup) {
            PromptGroup.MOVIES -> totalMovieGenreImages
            PromptGroup.TV_SHOWS -> totalTvShowGenreImages
        }

        _genreImages.value = genreList.filter { !userSettings.excludedGenres.contains(it.genreId) }
    }

    enum class PromptGroup {
        MOVIES,
        TV_SHOWS
    }
}
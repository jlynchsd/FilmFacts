package com.movietrivia.filmfacts.viewmodel

import androidx.lifecycle.ViewModel
import com.movietrivia.filmfacts.domain.AwardAchievementsUseCase
import com.movietrivia.filmfacts.model.Achievement
import com.movietrivia.filmfacts.model.TriviaQuestionResult
import com.movietrivia.filmfacts.model.UserProgressRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject


@HiltViewModel
class TriviaSessionsViewModel @Inject internal constructor(
    private val userProgressRepository: UserProgressRepository,
    private val awardAchievementsUseCase: AwardAchievementsUseCase
): ViewModel() {

    private var _triviaResults = mutableListOf<TriviaQuestionResult>()
    val triviaResults: List<TriviaQuestionResult> get() = _triviaResults

    fun startTriviaSession() {
        _triviaResults = mutableListOf()
    }

    fun storeTriviaAnswer(result: TriviaQuestionResult) {
        _triviaResults.add(result)
    }

    suspend fun awardAchievements(): Set<Achievement> {
        userProgressRepository.saveTriviaSessionResults(triviaResults)
        return awardAchievementsUseCase(triviaResults)
    }
}
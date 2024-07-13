package com.movietrivia.filmfacts.model

import kotlinx.coroutines.flow.first
import javax.inject.Inject

class UserProgressRepository @Inject constructor(
    private val unlockedAchievementsDataSource: UnlockedAchievementsDataSource,
    private val userHistoryDataSource: UserHistoryDataSource
) {

    val userHistory = userHistoryDataSource.userHistory
    val unlockedAchievements = unlockedAchievementsDataSource.unlockedAchievements

    suspend fun saveTriviaSessionResults(results: List<TriviaQuestionResult>) {
        val correctResults = results.filter { it.correct }.size
        val incorrectResults = results.filter { !it.correct }.size
        val totalDuration = results.sumOf { it.duration }
        val fastResponses = results.filter { it.correct && it.duration <= 1000 }.size

        userHistoryDataSource.userHistory.first().let {
            userHistoryDataSource.updateUserHistory(
                it.copy(
                    completedQuizzes = it.completedQuizzes + 1,
                    correctAnswers = it.correctAnswers + correctResults,
                    incorrectAnswers = it.incorrectAnswers + incorrectResults,
                    totalQuizDuration = it.totalQuizDuration + totalDuration,
                    fastResponseCount = it.fastResponseCount + fastResponses
                )
            )
        }
    }

    suspend fun updateUnlockedAchievements(unlockedAchievements: UnlockedAchievements) {
        unlockedAchievementsDataSource.updateUnlockedAchievements(unlockedAchievements)
    }
}
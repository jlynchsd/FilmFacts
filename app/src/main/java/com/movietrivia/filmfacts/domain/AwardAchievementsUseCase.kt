package com.movietrivia.filmfacts.domain

import com.movietrivia.filmfacts.model.Achievement
import com.movietrivia.filmfacts.model.CalendarProvider
import com.movietrivia.filmfacts.model.PendingData
import com.movietrivia.filmfacts.model.TriviaQuestionResult
import com.movietrivia.filmfacts.model.UnlockedAchievements
import com.movietrivia.filmfacts.model.UserDataRepository
import com.movietrivia.filmfacts.model.UserHistory
import com.movietrivia.filmfacts.model.UserProgressRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

class AwardAchievementsUseCase(
    private val calendarProvider: CalendarProvider,
    private val userProgressRepository: UserProgressRepository,
    private val userDataRepository: UserDataRepository,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO
) {

    suspend operator fun invoke(results: List<TriviaQuestionResult>?) =
        withContext(dispatcher) {
            awardAchievements(results)
        }

    private suspend fun awardAchievements(results: List<TriviaQuestionResult>?): Set<Achievement> {
        val userHistory = userProgressRepository.userHistory.first()
        val unlockedAchievements = userProgressRepository.unlockedAchievements.first()

        val updatedUnlockedAchievements = unlockedAchievements.achievements.toMutableSet()
        val awardedAchievements = mutableSetOf<Achievement>()

        Achievement.values().filter { !unlockedAchievements.achievements.contains(it) }.forEach {
            if (isAwarded(it, userHistory, unlockedAchievements, results)) {
                updatedUnlockedAchievements.add(it)
                awardedAchievements.add(it)
            }
        }

        if (isAwarded(Achievement.FINISH_ACHIEVEMENTS, userHistory, UnlockedAchievements(updatedUnlockedAchievements), results)) {
            updatedUnlockedAchievements.add(Achievement.FINISH_ACHIEVEMENTS)
            awardedAchievements.add(Achievement.FINISH_ACHIEVEMENTS)
        }

        userProgressRepository.updateUnlockedAchievements(
            UnlockedAchievements(
                updatedUnlockedAchievements,
                awardedAchievements.isNotEmpty()
            )
        )
        return awardedAchievements
    }

    private fun isAwarded(
        achievement: Achievement,
        userHistory: UserHistory,
        unlockedAchievements: UnlockedAchievements,
        results: List<TriviaQuestionResult>?
    ) =
        when(achievement) {
            Achievement.PERFECT_SCORE ->
                if (results != null) {
                    awardPerfectScore(results)
                } else {
                    false
                }
            Achievement.SIGN_IN -> awardSignIn()
            Achievement.COMPLETE_QUIZZES -> awardCompleteQuizzes(userHistory)
            Achievement.FINISH_ACHIEVEMENTS -> awardFinishAchievements(unlockedAchievements)
            Achievement.COMPLETE_FIRST_QUIZ -> awardCompleteFirstQuiz(userHistory)
            Achievement.DELAYED_QUIZ -> awardDelayedQuiz(userHistory)
            Achievement.FAST_RESPONSES -> awardFastResponses(userHistory)
        }

    private fun awardPerfectScore(
        results: List<TriviaQuestionResult>
    ): Boolean {
        return results.filter { it.correct }.size == results.size
    }

    private fun awardSignIn(): Boolean {
        return userDataRepository.accountDetails.value is PendingData.Success
    }

    private fun awardCompleteQuizzes(userHistory: UserHistory): Boolean {
        return userHistory.completedQuizzes >= 100
    }

    private fun awardFinishAchievements(unlockedAchievements: UnlockedAchievements): Boolean {
        return unlockedAchievements.achievements.containsAll(
            Achievement.values().filter { it != Achievement.FINISH_ACHIEVEMENTS }
        )
    }

    private fun awardCompleteFirstQuiz(userHistory: UserHistory): Boolean {
        return userHistory.completedQuizzes >= 1
    }

    private fun awardDelayedQuiz(userHistory: UserHistory): Boolean {
        return calendarProvider.instance().timeInMillis - userHistory.startDate >= 7*24*60*60*1000
    }

    private fun awardFastResponses(userHistory: UserHistory): Boolean {
        return userHistory.fastResponseCount >= 25
    }
}
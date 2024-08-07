package com.movietrivia.filmfacts.model

import io.mockk.CapturingSlot
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert
import org.junit.Before
import org.junit.Test

class UserProgressRepositoryTest {

    private lateinit var unlockedAchievementsDataSource: UnlockedAchievementsDataSource
    private lateinit var userHistoryDataSource: UserHistoryDataSource
    private lateinit var userProgressRepository: UserProgressRepository

    private lateinit var userHistoryFlow: MutableStateFlow<UserHistory>
    private lateinit var userHistorySlot: CapturingSlot<UserHistory>

    @Before
    fun setup() {
        unlockedAchievementsDataSource = mockk(relaxed = true)
        userHistoryDataSource = mockk(relaxed = true)
        userProgressRepository = UserProgressRepository(unlockedAchievementsDataSource, userHistoryDataSource)

        userHistoryFlow = MutableStateFlow(UserHistory(0))
        every {
            userHistoryDataSource.userHistory
        } returns userHistoryFlow

        userHistorySlot = CapturingSlot()
        coEvery {
            userHistoryDataSource.updateUserHistory(capture(userHistorySlot))
        } just runs
    }

    @Test
    fun `Given new trivia results when saving results then increments quiz count`() = runTest {
        userProgressRepository.saveTriviaSessionResults(TRIVIA_RESULTS)

        Assert.assertEquals(1, userHistorySlot.captured.completedQuizzes)
    }

    @Test
    fun `Given new trivia results when saving results then correctly sorts correct and incorrect answers`() = runTest {
        userProgressRepository.saveTriviaSessionResults(TRIVIA_RESULTS)

        Assert.assertEquals(4, userHistorySlot.captured.correctAnswers)
        Assert.assertEquals(3, userHistorySlot.captured.incorrectAnswers)
    }

    @Test
    fun `Given new trivia results when saving results then increases total duration`() = runTest {
        userProgressRepository.saveTriviaSessionResults(TRIVIA_RESULTS)

        Assert.assertEquals(7100, userHistorySlot.captured.totalQuizDuration)
    }

    @Test
    fun `Given new trivia results when saving results then correctly increases fast responses`() = runTest {
        userProgressRepository.saveTriviaSessionResults(TRIVIA_RESULTS)

        Assert.assertEquals(2, userHistorySlot.captured.fastResponseCount)
    }

    @Test
    fun `When updating unlocked achievements delegates to data source`() = runTest {
        userProgressRepository.updateUnlockedAchievements(mockk())

        coVerify {
            unlockedAchievementsDataSource.updateUnlockedAchievements(any())
        }
    }

    private companion object {
        val TRIVIA_RESULTS = listOf(
            TriviaQuestionResult(true, 500),
            TriviaQuestionResult(false, 700),
            TriviaQuestionResult(true, 900),
            TriviaQuestionResult(false, 1000),
            TriviaQuestionResult(true, 1200),
            TriviaQuestionResult(false, 1300),
            TriviaQuestionResult(true, 1500)
        )
    }
}
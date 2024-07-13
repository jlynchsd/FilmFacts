package com.interviewsample.filmfacts.domain

import com.movietrivia.filmfacts.domain.AwardAchievementsUseCase
import com.movietrivia.filmfacts.model.AccountDetails
import com.movietrivia.filmfacts.model.Achievement
import com.movietrivia.filmfacts.model.CalendarProvider
import com.movietrivia.filmfacts.model.PendingData
import com.movietrivia.filmfacts.model.TriviaQuestionResult
import com.movietrivia.filmfacts.model.UnlockedAchievements
import com.movietrivia.filmfacts.model.UserDataRepository
import com.movietrivia.filmfacts.model.UserHistory
import com.movietrivia.filmfacts.model.UserProgressRepository
import io.mockk.CapturingSlot
import io.mockk.coEvery
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.runTest
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import java.util.Calendar


class AwardAchievementsUseCaseTest {

    private lateinit var calendarProvider: CalendarProvider
    private lateinit var userProgressRepository: UserProgressRepository
    private lateinit var userDataRepository: UserDataRepository

    private lateinit var userHistoryFlow: MutableStateFlow<UserHistory>
    private lateinit var unlockedAchievementsFlow: MutableStateFlow<UnlockedAchievements>
    private lateinit var accountDetailsFlow: MutableStateFlow<PendingData<AccountDetails>>
    private lateinit var unlockedAchievementsSlot: CapturingSlot<UnlockedAchievements>

    @Before
    fun setup() {
        calendarProvider = mockk(relaxed = true)
        userProgressRepository = mockk(relaxed = true)
        userDataRepository = mockk(relaxed = true)

        userHistoryFlow = MutableStateFlow(UserHistory(0))
        unlockedAchievementsFlow = MutableStateFlow(UnlockedAchievements())
        accountDetailsFlow = MutableStateFlow(PendingData.None())
        unlockedAchievementsSlot = CapturingSlot()

        every {
            userProgressRepository.userHistory
        } returns userHistoryFlow

        every {
            userProgressRepository.unlockedAchievements
        } returns unlockedAchievementsFlow

        every {
            userDataRepository.accountDetails
        } returns accountDetailsFlow

        coEvery {
            userProgressRepository.updateUnlockedAchievements(capture(unlockedAchievementsSlot))
        } just runs
    }

    @Test
    fun `When no achievements are awarded then no achievements saved or returned`() = runTest {
        val awarded = getUseCase(testScheduler).invoke(listOf(TriviaQuestionResult(false, 0)))

        Assert.assertTrue(unlockedAchievementsSlot.captured.achievements.isEmpty())
        Assert.assertFalse(unlockedAchievementsSlot.captured.newAchievements)
        Assert.assertTrue(awarded.isEmpty())
    }

    @Test
    fun `Given perfect score when awarding achievements then awards perfect score achievement`() = runTest {
        val awarded = getUseCase(testScheduler).invoke(listOf(TriviaQuestionResult(true, 0)))

        Assert.assertEquals(Achievement.PERFECT_SCORE, unlockedAchievementsSlot.captured.achievements.first())
        Assert.assertTrue(unlockedAchievementsSlot.captured.newAchievements)
        Assert.assertEquals(Achievement.PERFECT_SCORE, awarded.first())
    }

    @Test
    fun `Given signed in when awarding achievements then awards signed in achievement`() = runTest {
        accountDetailsFlow.value = PendingData.Success(AccountDetails(0, "", "", mockk(), mockk(), mockk(), ""))
        val awarded = getUseCase(testScheduler).invoke(null)

        Assert.assertEquals(Achievement.SIGN_IN, unlockedAchievementsSlot.captured.achievements.first())
        Assert.assertTrue(unlockedAchievementsSlot.captured.newAchievements)
        Assert.assertEquals(Achievement.SIGN_IN, awarded.first())
    }

    @Test
    fun `Given completed quizzes when awarding achievements then awards completed quizzes achievement`() = runTest {
        userHistoryFlow.value = UserHistory(0, completedQuizzes = 100)
        val awarded = getUseCase(testScheduler).invoke(null)

        Assert.assertEquals(Achievement.COMPLETE_QUIZZES, unlockedAchievementsSlot.captured.achievements.first())
        Assert.assertTrue(unlockedAchievementsSlot.captured.newAchievements)
        Assert.assertEquals(Achievement.COMPLETE_QUIZZES, awarded.first())
    }

    @Test
    fun `Given finished achievements when awarding achievements then awards finished achievements achievement`() = runTest {
        unlockedAchievementsFlow.value = UnlockedAchievements(
            achievements = setOf(
                Achievement.PERFECT_SCORE,
                Achievement.SIGN_IN,
                Achievement.COMPLETE_QUIZZES,
                Achievement.COMPLETE_FIRST_QUIZ,
                Achievement.DELAYED_QUIZ,
                Achievement.FAST_RESPONSES
            )
        )
        val awarded = getUseCase(testScheduler).invoke(null)

        Assert.assertTrue(unlockedAchievementsSlot.captured.achievements.contains(Achievement.FINISH_ACHIEVEMENTS))
        Assert.assertTrue(unlockedAchievementsSlot.captured.newAchievements)
        Assert.assertEquals(Achievement.FINISH_ACHIEVEMENTS, awarded.first())
    }

    @Test
    fun `Given all but last achievement when awarding last achievement then awards finished achievements achievement`() = runTest {
        unlockedAchievementsFlow.value = UnlockedAchievements(
            achievements = setOf(
                Achievement.SIGN_IN,
                Achievement.COMPLETE_QUIZZES,
                Achievement.COMPLETE_FIRST_QUIZ,
                Achievement.DELAYED_QUIZ,
                Achievement.FAST_RESPONSES
            )
        )
        val awarded = getUseCase(testScheduler).invoke(listOf(TriviaQuestionResult(true, 0)))

        Assert.assertTrue(unlockedAchievementsSlot.captured.achievements.contains(Achievement.FINISH_ACHIEVEMENTS))
        Assert.assertTrue(unlockedAchievementsSlot.captured.newAchievements)
        Assert.assertTrue(awarded.contains(Achievement.FINISH_ACHIEVEMENTS))
    }

    @Test
    fun `Given completed first quiz when awarding achievements then awards completed first quiz achievement`() = runTest {
        userHistoryFlow.value = UserHistory(0, completedQuizzes = 1)
        val awarded = getUseCase(testScheduler).invoke(null)

        Assert.assertEquals(Achievement.COMPLETE_FIRST_QUIZ, unlockedAchievementsSlot.captured.achievements.first())
        Assert.assertTrue(unlockedAchievementsSlot.captured.newAchievements)
        Assert.assertEquals(Achievement.COMPLETE_FIRST_QUIZ, awarded.first())
    }

    @Test
    fun `Given completed delayed quiz when awarding achievements then awards completed delayed quiz achievement`() = runTest {
        val mockCalendar = mockk<Calendar>(relaxed = true)
        every {
            mockCalendar.timeInMillis
        } returns 7*24*60*60*1000
        every {
            calendarProvider.instance()
        } returns mockCalendar
        val awarded = getUseCase(testScheduler).invoke(null)

        Assert.assertEquals(Achievement.DELAYED_QUIZ, unlockedAchievementsSlot.captured.achievements.first())
        Assert.assertTrue(unlockedAchievementsSlot.captured.newAchievements)
        Assert.assertEquals(Achievement.DELAYED_QUIZ, awarded.first())
    }

    @Test
    fun `Given completed fast responses when awarding achievements then awards completed fast responses achievement`() = runTest {
        userHistoryFlow.value = UserHistory(0, fastResponseCount = 25)
        val awarded = getUseCase(testScheduler).invoke(null)

        Assert.assertEquals(Achievement.FAST_RESPONSES, unlockedAchievementsSlot.captured.achievements.first())
        Assert.assertTrue(unlockedAchievementsSlot.captured.newAchievements)
        Assert.assertEquals(Achievement.FAST_RESPONSES, awarded.first())
    }

    private fun getUseCase(testScheduler: TestCoroutineScheduler) =
        AwardAchievementsUseCase(
            calendarProvider,
            userProgressRepository,
            userDataRepository,
            StandardTestDispatcher(testScheduler)
        )
}
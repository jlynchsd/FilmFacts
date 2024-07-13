package com.interviewsample.filmfacts.viewmodel

import com.movietrivia.filmfacts.domain.AwardAchievementsUseCase
import com.movietrivia.filmfacts.model.TriviaQuestionResult
import com.movietrivia.filmfacts.model.UserProgressRepository
import com.movietrivia.filmfacts.viewmodel.TriviaSessionsViewModel
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert
import org.junit.Before
import org.junit.Test

class TriviaSessionsViewModelTest {

    private lateinit var userProgressRepository: UserProgressRepository
    private lateinit var awardAchievementsUseCase: AwardAchievementsUseCase
    private lateinit var viewModel: TriviaSessionsViewModel

    @Before
    fun setup() {
        userProgressRepository = mockk(relaxed = true)
        awardAchievementsUseCase = mockk(relaxed = true)
        viewModel = TriviaSessionsViewModel(userProgressRepository, awardAchievementsUseCase)
    }

    @Test
    fun `When created defaults to empty session`() {
        Assert.assertEquals(emptyList<TriviaQuestionResult>(), viewModel.triviaResults)
    }

    @Test
    fun `When answer is correct stores data`() {
        viewModel.storeTriviaAnswer(TriviaQuestionResult(true, 0))

        Assert.assertEquals(TriviaQuestionResult(true, 0), viewModel.triviaResults.first())
    }

    @Test
    fun `When answer is incorrect stores data`() {
        viewModel.storeTriviaAnswer(TriviaQuestionResult(false, 0))

        Assert.assertEquals(TriviaQuestionResult(false, 0), viewModel.triviaResults.first())
    }

    @Test
    fun `When starting session replaces existing scores with empty session`() {
        viewModel.storeTriviaAnswer(TriviaQuestionResult(false, 0))
        viewModel.storeTriviaAnswer(TriviaQuestionResult(true, 0))
        viewModel.storeTriviaAnswer(TriviaQuestionResult(false, 0))

        Assert.assertEquals(3, viewModel.triviaResults.size)

        viewModel.startTriviaSession()

        Assert.assertEquals(emptyList<TriviaQuestionResult>(), viewModel.triviaResults)
    }

    @Test
    fun `When awarding achievements saves session results and delegates to use case`() = runTest {
        viewModel.awardAchievements()

        coVerify {
            userProgressRepository.saveTriviaSessionResults(any())
        }

        coVerify {
            awardAchievementsUseCase.invoke(any())
        }
    }
}
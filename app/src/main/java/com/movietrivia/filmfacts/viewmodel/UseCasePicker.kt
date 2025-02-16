package com.movietrivia.filmfacts.viewmodel

import com.movietrivia.filmfacts.api.Logger
import com.movietrivia.filmfacts.domain.UseCase
import kotlin.math.max

class UseCasePicker(useCases: List<UseCase>) {

    private val randomUseCases = EvenRandom(useCases)
    private val failureCount = HashMap<UseCase, Int>()

    fun pickUseCase(): UseCase? {
        var result = kotlin.runCatching { randomUseCases.random() }.getOrNull()
        result?.let { useCase ->
            failureCount[useCase]?.let {
                // If result has lots of failures, increase chance that we skip it
                if ((0 .. it).randomOrNull() != 0) {
                    Logger.debug(LOG_TAG, "UseCase $useCase skipped")
                    result = null
                }
            }
        }

        return result
    }

    fun failed(useCase: UseCase) {
        val useCaseFailureCount = failureCount[useCase]
        failureCount[useCase] = if (useCaseFailureCount != null) {
            useCaseFailureCount + 1
        } else {
            1
        }

        Logger.debug(LOG_TAG, "UseCase $useCase failure count: ${failureCount[useCase]}")
    }

    fun succeeded(useCase: UseCase) {
        val useCaseFailureCount = failureCount[useCase]
        failureCount[useCase] = if (useCaseFailureCount != null) {
            if (useCaseFailureCount != 0) {
                Logger.debug(LOG_TAG, "UseCase $useCase succeeded; count: ${max(0, useCaseFailureCount - 2)}")
            }
            max(0, useCaseFailureCount - 2)
        } else {
            0
        }
    }

    fun reset() {
        failureCount.clear()
    }

    private companion object {
        const val LOG_TAG = "UseCasePicker"
    }
}
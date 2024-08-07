package com.movietrivia.filmfacts.viewmodel

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain

@OptIn(ExperimentalCoroutinesApi::class)
fun runViewModelScope(body: suspend () -> Unit) = runTest {
    Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
    try {
        body.invoke()
    } finally {
        Dispatchers.resetMain()
    }
}
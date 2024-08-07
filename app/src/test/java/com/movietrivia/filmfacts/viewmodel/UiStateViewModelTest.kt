package com.movietrivia.filmfacts.viewmodel

import org.junit.Assert
import org.junit.Test

class UiStateViewModelTest {

    @Test
    fun `When updating home screen offset stores value`() {
        val offset = 3
        val viewModel = UiStateViewModel()

        viewModel.homeScreenScrollOffset = offset

        Assert.assertEquals(offset, viewModel.homeScreenScrollOffset)
    }
}
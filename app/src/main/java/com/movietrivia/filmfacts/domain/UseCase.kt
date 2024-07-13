package com.movietrivia.filmfacts.domain

import com.movietrivia.filmfacts.model.UiPrompt

interface UseCase {

    suspend operator fun invoke(includeGenres: List<Int>?): UiPrompt?
}
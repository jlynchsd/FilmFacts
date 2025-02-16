package com.movietrivia.filmfacts.model

import javax.inject.Inject

class GenreImageRepository @Inject constructor(
    private val genreImageDataSource: GenreImageDataSource
) {
    val supportedGenres = genreImageDataSource.supportedGenres

    suspend fun getGenreImages() = genreImageDataSource.getGenreImages()

    suspend fun saveGenreImages(images: List<UiGenre>) = genreImageDataSource.saveGenreImages(images)
}
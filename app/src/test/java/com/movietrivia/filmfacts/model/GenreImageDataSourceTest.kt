package com.movietrivia.filmfacts.model

import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class GenreImageDataSourceTest {

    private lateinit var dataSource: GenreImageDataSource

    @Before
    fun setup() = runBlocking {
        dataSource = GenreImageDataSource(ApplicationProvider.getApplicationContext()).also {
            it.clearGenreImages()
        }
    }

    @Test
    fun `When getting genre images before any have been set returns defaults`() = runTest {
        Assert.assertEquals(GenreImageDataSource.defaults, dataSource.getGenreImages())
    }

    @Test
    fun `When new genre images are saved they replace existing genre images`() = runTest {
        val genreImages = listOf(
            UiGenre("foo", 0),
            UiGenre("bar", 1)
        )

        Assert.assertEquals(GenreImageDataSource.defaults, dataSource.getGenreImages())

        dataSource.saveGenreImages(genreImages)

        Assert.assertEquals(genreImages, dataSource.getGenreImages())
    }
}
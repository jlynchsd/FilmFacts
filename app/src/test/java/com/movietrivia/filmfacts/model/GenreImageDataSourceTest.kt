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
    private val defaults = listOf(
        UiGenre("fizz", 2),
        UiGenre("buzz", 3)
    )

    @Before
    fun setup() = runBlocking {
        dataSource = GenreImageDataSource(ApplicationProvider.getApplicationContext(), "foo", defaults).also {
            it.clearGenreImages()
        }
    }

    @Test
    fun `When getting genre images before any have been set returns defaults`() = runTest {
        Assert.assertEquals(defaults, dataSource.getGenreImages())
    }

    @Test
    fun `When new genre images are saved they replace existing genre images`() = runTest {
        val genreImages = listOf(
            UiGenre("foo", 0),
            UiGenre("bar", 1)
        )

        Assert.assertEquals(defaults, dataSource.getGenreImages())

        dataSource.saveGenreImages(genreImages)

        Assert.assertEquals(genreImages, dataSource.getGenreImages())
    }
}
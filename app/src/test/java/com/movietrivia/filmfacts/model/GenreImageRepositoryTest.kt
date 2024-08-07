package com.movietrivia.filmfacts.model

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import kotlinx.coroutines.test.runTest
import org.junit.Assert
import org.junit.Before
import org.junit.Test

class GenreImageRepositoryTest {

    private lateinit var dataSource: GenreImageDataSource
    private lateinit var repository: GenreImageRepository

    @Before
    fun setup() {
        dataSource = mockk(relaxed = true)
        repository = GenreImageRepository(dataSource)
    }

    @Test
    fun `When getting genre images delegates to data source`() = runTest {
        coEvery {
            dataSource.getGenreImages()
        } returns genres

        Assert.assertEquals(genres, repository.getGenreImages())
    }

    @Test
    fun `When setting genre images delegates to data source`() = runTest {
        coEvery {
            dataSource.saveGenreImages(any())
        } just runs

        repository.saveGenreImages(genres)

        coVerify(exactly = 1) { dataSource.saveGenreImages(genres) }
    }

    private companion object {
        val genres = listOf(
            UiGenre("foo", 0),
            UiGenre("bar", 1)
        )
    }
}
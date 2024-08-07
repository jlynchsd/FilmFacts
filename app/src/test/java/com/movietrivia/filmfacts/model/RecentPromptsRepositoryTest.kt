package com.movietrivia.filmfacts.model

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.Assert
import org.junit.Before
import org.junit.Test

class RecentPromptsRepositoryTest {

    private lateinit var actorsDataSource: RecentElementsDataSource
    private lateinit var flopsDataSource: RecentFlopsDataSource
    private lateinit var moviesDataSource: RecentElementsDataSource
    private lateinit var repository: RecentPromptsRepository

    @Before
    fun setup() {
        actorsDataSource = mockk(relaxed = true)
        flopsDataSource = mockk(relaxed = true)
        moviesDataSource = mockk(relaxed = true)
        repository = RecentPromptsRepository(actorsDataSource, flopsDataSource, moviesDataSource)
    }

    @Test
    fun `When loading data delegates to persistent data sources`() = runTest {
        coEvery {
            actorsDataSource.loadElements()
        } just runs
        coEvery {
            moviesDataSource.loadElements()
        } just runs

        repository.loadData()

        coVerify(exactly = 1) { actorsDataSource.loadElements() }
        coVerify(exactly = 1) { moviesDataSource.loadElements() }
    }

    @Test
    fun `When saving data delegates to persistent data sources`() = runTest {
        coEvery {
            actorsDataSource.saveElements()
        } just runs
        coEvery {
            moviesDataSource.saveElements()
        } just runs

        repository.saveData()

        coVerify(exactly = 1) { actorsDataSource.saveElements() }
        coVerify(exactly = 1) { moviesDataSource.saveElements() }
    }

    @Test
    fun `When checking if recent movie delegates to movie data source`() {
        val element = 0

        repository.isRecentMovie(element)

        verify(exactly = 1) { moviesDataSource.isRecentElement(element) }
    }

    @Test
    fun `When adding recent movie delegates to movie data source`() {
        val element = 0

        repository.addRecentMovie(element)

        verify(exactly = 1) { moviesDataSource.addElement(element) }
    }

    @Test
    fun `When checking if recent actor delegates to actor data source`() {
        val element = 0

        repository.isRecentActor(element)

        verify(exactly = 1) { actorsDataSource.isRecentElement(element) }
    }

    @Test
    fun `When adding recent actor delegates to actor data source`() {
        val element = 0

        repository.addRecentActor(element)

        verify(exactly = 1) { actorsDataSource.addElement(element) }
    }

    @Test
    fun `When checking if recent flop delegates to flop data source`() {
        val element = 0

        repository.isRecentFlop(element)

        verify(exactly = 1) { flopsDataSource.isRecentMovie(element) }
    }

    @Test
    fun `When adding recent flop delegates to flop data source`() {
        val element = 0
        val flop = true

        repository.addFlop(element, flop)

        verify(exactly = 1) { flopsDataSource.addMovie(element, flop) }
    }

    @Test
    fun `When resetting delegates to all data sources`() {
        repository.reset()

        verify(exactly = 1) { actorsDataSource.resetRecentElements() }
        verify(exactly = 1) { moviesDataSource.resetRecentElements() }
        verify(exactly = 1) { flopsDataSource.resetFlops() }
    }

    @Test
    fun `When setting and then querying flop page delegates to flop data source`() {
        val page = 5
        every {
            flopsDataSource.page
        } returns page

        Assert.assertEquals(page, repository.flopPage)
    }
}
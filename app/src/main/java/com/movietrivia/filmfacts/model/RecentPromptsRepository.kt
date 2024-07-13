package com.movietrivia.filmfacts.model

import javax.inject.Inject

class RecentPromptsRepository @Inject constructor(
    private val recentActorsDataSource: RecentElementsDataSource,
    private val recentFlopsDataSource: RecentFlopsDataSource,
    private val recentMoviesDataSource: RecentElementsDataSource
) {

    suspend fun loadData() {
        recentActorsDataSource.loadElements()
        recentMoviesDataSource.loadElements()
    }

    suspend fun saveData() {
        recentActorsDataSource.saveElements()
        recentMoviesDataSource.saveElements()
    }

    fun isRecentMovie(movieId: Int) = recentMoviesDataSource.isRecentElement(movieId)

    fun addRecentMovie(movieId: Int) = recentMoviesDataSource.addElement(movieId)

    fun isRecentActor(actorId: Int) = recentActorsDataSource.isRecentElement(actorId)

    fun addRecentActor(actorId: Int) = recentActorsDataSource.addElement(actorId)

    fun isRecentFlop(movieId: Int) = recentFlopsDataSource.isRecentMovie(movieId)

    fun addFlop(movieId: Int, isFlop: Boolean) = recentFlopsDataSource.addMovie(movieId, isFlop)

    fun reset() {
        recentActorsDataSource.resetRecentElements()
        recentMoviesDataSource.resetRecentElements()
        recentFlopsDataSource.resetFlops()
    }

    var flopPage
        get() = recentFlopsDataSource.page
        set(value) {
            recentFlopsDataSource.page = value
        }
}
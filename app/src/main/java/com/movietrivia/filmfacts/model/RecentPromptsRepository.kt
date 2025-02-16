package com.movietrivia.filmfacts.model

import javax.inject.Inject

class RecentPromptsRepository @Inject constructor(
    private val recentActorsDataSource: RecentElementsDataSource,
    private val recentFlopsDataSource: RecentFlopsDataSource,
    private val recentMoviesDataSource: RecentElementsDataSource,
    private val recentTvShowsDataSource: RecentElementsDataSource
) {

    suspend fun loadData() {
        recentActorsDataSource.loadElements()
        recentMoviesDataSource.loadElements()
        recentTvShowsDataSource.loadElements()
    }

    suspend fun saveData() {
        recentActorsDataSource.saveElements()
        recentMoviesDataSource.saveElements()
        recentTvShowsDataSource.saveElements()
    }

    fun isRecentMovie(movieId: Int) = recentMoviesDataSource.isRecentElement(movieId)

    fun addRecentMovie(movieId: Int) = recentMoviesDataSource.addElement(movieId)

    fun isRecentTvShow(showId: Int) = recentTvShowsDataSource.isRecentElement(showId)

    fun addRecentTvShow(showId: Int) = recentTvShowsDataSource.addElement(showId)

    fun isRecentActor(actorId: Int) = recentActorsDataSource.isRecentElement(actorId)

    fun addRecentActor(actorId: Int) = recentActorsDataSource.addElement(actorId)

    fun isRecentFlop(movieId: Int) = recentFlopsDataSource.isRecentMovie(movieId)

    fun addFlop(movieId: Int, isFlop: Boolean) = recentFlopsDataSource.addMovie(movieId, isFlop)

    fun reset() {
        recentActorsDataSource.resetRecentElements()
        recentMoviesDataSource.resetRecentElements()
        recentTvShowsDataSource.resetRecentElements()
        recentFlopsDataSource.resetFlops()
    }

    var flopPage
        get() = recentFlopsDataSource.page
        set(value) {
            recentFlopsDataSource.page = value
        }
}
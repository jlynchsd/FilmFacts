package com.movietrivia.filmfacts.model

import java.util.*
import java.util.concurrent.atomic.AtomicInteger
import kotlin.collections.LinkedHashMap

class RecentFlopsDataSource {

    private val movies = Collections.synchronizedMap(object : LinkedHashMap<Int, Boolean>() {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<Int, Boolean>?): Boolean {
            return size > CACHE_SIZE
        }
    })

    private var _page = AtomicInteger(1)
    var page
        get() = _page.get()
        set(value) {
            if (value > _page.get()) {
                _page.set(value)
            }
        }

    fun isRecentMovie(movieId: Int): Boolean {
        return movies.contains(movieId)
    }

    fun addMovie(movieId: Int, isFlop: Boolean) {
        movies[movieId] = isFlop
    }

    fun resetFlops() {
        movies.values.removeAll { it }
        _page.set(1)
    }

    private companion object {
        const val CACHE_SIZE = 40
    }
}
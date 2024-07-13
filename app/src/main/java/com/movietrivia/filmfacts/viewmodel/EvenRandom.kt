package com.movietrivia.filmfacts.viewmodel

class EvenRandom<T>(data: List<T>) {

    private val lock = Any()
    private val remaining = data.toMutableList()
    private val picked = mutableListOf<T>()

    @Throws(NoSuchElementException::class)
    fun random(): T  = synchronized(lock) {
        if (remaining.isEmpty()) {
            remaining.addAll(picked)
            picked.clear()
        }

        val pick = remaining.random()
        remaining.remove(pick)
        picked.add(pick)

        return pick
    }

    fun removeElement(element: T) = synchronized(lock) {
        remaining.remove(element)
        picked.remove(element)
    }

    fun addElement(element: T) = synchronized(lock) {
        remaining.add(element)
    }

    fun isEmpty() = synchronized(lock) {
        remaining.size + picked.size == 0
    }
}
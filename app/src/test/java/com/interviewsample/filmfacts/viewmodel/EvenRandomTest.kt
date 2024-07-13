package com.interviewsample.filmfacts.viewmodel

import com.movietrivia.filmfacts.viewmodel.EvenRandom
import org.junit.Assert
import org.junit.Test

class EvenRandomTest {

    @Test
    fun `When created with no elements is empty`() {
        Assert.assertTrue(EvenRandom<Int>(emptyList()).isEmpty())
    }

    @Test
    fun `When created with some elements is not empty`() {
        Assert.assertFalse(EvenRandom(listOf(1)).isEmpty())
    }

    @Test
    fun `When getting random elements only gets each element once`() {
        val elements = listOf(1, 2, 3)
        val evenRandom = EvenRandom(elements)

        val picked = mutableListOf<Int>()
        repeat(elements.size) {
            picked.add(evenRandom.random())
        }

        val unique = picked.toSet()

        Assert.assertEquals(elements.toSet(), unique)
    }

    @Test
    fun `When getting random elements multiple times iterates through all elements equally`() {
        val elements = listOf(1, 2, 3)
        val evenRandom = EvenRandom(elements)

        val picked = mutableListOf<Int>()
        repeat(elements.size) {
            picked.add(evenRandom.random())
        }
        val unique = picked.toSet()

        val picked2 = mutableListOf<Int>()
        repeat(elements.size) {
            picked2.add(evenRandom.random())
        }
        val unique2 = picked.toSet()

        val picked3 = mutableListOf<Int>()
        repeat(elements.size) {
            picked3.add(evenRandom.random())
        }
        val unique3 = picked.toSet()

        Assert.assertEquals(elements.toSet(), unique)
        Assert.assertEquals(elements.toSet(), unique2)
        Assert.assertEquals(elements.toSet(), unique3)
    }

    @Test
    fun `When adding new element does not count as picked`() {
        val elements = listOf(1, 2, 3)
        val evenRandom = EvenRandom(elements)

        repeat(elements.size) {
            evenRandom.random()
        }

        evenRandom.addElement(4)

        Assert.assertEquals(4, evenRandom.random())
    }

    @Test
    fun `When adding new element and starting fresh cycle picks all elements`() {
        val elements = listOf(1, 2, 3)
        val evenRandom = EvenRandom(elements)

        repeat(elements.size) {
            evenRandom.random()
        }

        evenRandom.addElement(4)
        evenRandom.random()

        val picked = mutableListOf<Int>()
        repeat(4) {
            picked.add(evenRandom.random())
        }
        val unique = picked.toSet()

        Assert.assertEquals(setOf(1, 2, 3, 4), unique)
    }

    @Test
    fun `When removing element does not get it and does not repeat it`() {
        val elements = listOf(1, 2, 3)
        val evenRandom = EvenRandom(elements)
        evenRandom.removeElement(3)

        val picked = mutableListOf<Int>()
        repeat(10) {
            picked.add(evenRandom.random())
        }
        val unique = picked.toSet()

        Assert.assertEquals(setOf(1, 2), unique)
    }

    @Test
    fun `When adding and removing elements changes whether its empty`() {
        val evenRandom = EvenRandom<Int>(emptyList())

        Assert.assertTrue(evenRandom.isEmpty())

        evenRandom.addElement(1)

        Assert.assertFalse(evenRandom.isEmpty())

        evenRandom.removeElement(1)

        Assert.assertTrue(evenRandom.isEmpty())
    }

    @Test
    fun `When removing non-existent element does nothing`() {
        val evenRandom = EvenRandom(listOf(1))

        evenRandom.removeElement(2)

        Assert.assertFalse(evenRandom.isEmpty())
    }

    @Test(expected = NoSuchElementException::class)
    fun `When no elements and getting value throws exception`() {
        EvenRandom<Int>(emptyList()).random()
    }
}
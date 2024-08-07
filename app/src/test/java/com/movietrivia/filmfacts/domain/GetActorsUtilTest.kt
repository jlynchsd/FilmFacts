package com.movietrivia.filmfacts.domain

import com.movietrivia.filmfacts.api.ActorCredits
import com.movietrivia.filmfacts.api.DiscoverMovieResponse
import com.movietrivia.filmfacts.api.MovieCreditEntry
import com.movietrivia.filmfacts.api.MovieCredits
import com.movietrivia.filmfacts.api.PersonDetails
import com.movietrivia.filmfacts.model.Actor
import com.movietrivia.filmfacts.model.FilmFactsRepository
import com.movietrivia.filmfacts.model.RecentPromptsRepository
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.util.Calendar

@RunWith(RobolectricTestRunner::class)
class GetActorsUtilTest {

    private lateinit var filmFactsRepository: FilmFactsRepository
    private lateinit var recentPromptsRepository: RecentPromptsRepository

    @Before
    fun setup() {
        filmFactsRepository = mockk(relaxed = true)
        recentPromptsRepository = mockk(relaxed = true)

        coEvery {
            filmFactsRepository.getMovies(order = any(), includeGenres = any(), minimumVotes = any())
        } returns DiscoverMovieResponse(
            1,
            listOf(mockk(relaxed = true), mockk(relaxed = true), mockk(relaxed = true), mockk(relaxed = true)),
            1,
            0
        )

        coEvery {
            filmFactsRepository.getMovieCredits(any())
        } returns MovieCredits(
            0,
            listOf(
                MovieCreditEntry(0, "foo", null, "foo"),
                MovieCreditEntry(0, "bar", null, "bar"),
                MovieCreditEntry(0, "fizz", null, "fizz"),
                MovieCreditEntry(0, "buzz", null, "buzz"),
                MovieCreditEntry(0, "fin", null, "fin")
            )
        )

        coEvery {
            filmFactsRepository.getActorDetails(any())
        } returns PersonDetails(0, "foo", 0, "fooPath")

        coEvery {
            filmFactsRepository.getActorCredits(any())
        } returns listOf(
            ActorCredits(0, "foo", "foo", emptyList(), 0, 0, "", ""),
            ActorCredits(1, "bar", "bar", emptyList(), 0, 0, "", ""),
            ActorCredits(2, "fizz", "fizz", emptyList(), 0, 0, "", ""),
            ActorCredits(3, "buzz", "buzz", emptyList(), 0, 0, "", "")
        )

    }

    // region getActors

    @Test
    fun `When getting actors but seed movies are null returns empty result`() = runTest {
        coEvery {
            filmFactsRepository.getMovies(order = any(), includeGenres = any(), minimumVotes = any())
        } returns null

        val actors = getActors(filmFactsRepository, recentPromptsRepository)
        Assert.assertTrue(actors.isEmpty())
    }

    @Test
    fun `When getting actors but seed movies are empty returns empty result`() = runTest {
        coEvery {
            filmFactsRepository.getMovies(order = any(), includeGenres = any(), minimumVotes = any())
        } returns DiscoverMovieResponse(1, emptyList(), 1, 0)

        val actors = getActors(filmFactsRepository, recentPromptsRepository)
        Assert.assertTrue(actors.isEmpty())
    }

    @Test
    fun `When getting actors but movie credits are null returns empty result`() = runTest {
        coEvery {
            filmFactsRepository.getMovieCredits(any())
        } returns null

        val actors = getActors(filmFactsRepository, recentPromptsRepository)
        Assert.assertTrue(actors.isEmpty())
    }

    @Test
    fun `When getting actors but movie credits are empty returns empty result`() = runTest {
        coEvery {
            filmFactsRepository.getMovieCredits(any())
        } returns MovieCredits(0, emptyList())

        val actors = getActors(filmFactsRepository, recentPromptsRepository)
        Assert.assertTrue(actors.isEmpty())
    }

    @Test
    fun `When getting actors but all actors are recent returns empty result`() = runTest {
        coEvery {
            recentPromptsRepository.isRecentActor(any())
        } returns true

        val actors = getActors(filmFactsRepository, recentPromptsRepository)
        Assert.assertTrue(actors.isEmpty())
    }

    @Test
    fun `When getting actors and able to get data returns actors`() = runTest {
        val actors = getActors(filmFactsRepository, recentPromptsRepository)
        Assert.assertTrue(actors.isNotEmpty())
    }

    @Test
    fun `When getting actors and only one seed movie returns actors`() = runTest {
        coEvery {
            filmFactsRepository.getMovies(order = any(), includeGenres = any(), minimumVotes = any())
        } returns DiscoverMovieResponse(
            1,
            listOf(mockk(relaxed = true)),
            1,
            0
        )

        val actors = getActors(filmFactsRepository, recentPromptsRepository)
        Assert.assertTrue(actors.isNotEmpty())
    }

    @Test
    fun `When getting actors and many seed movies returns actors`() = runTest {
        coEvery {
            filmFactsRepository.getMovies(order = any(), includeGenres = any(), minimumVotes = any())
        } returns DiscoverMovieResponse(
            1,
            listOf(mockk(relaxed = true), mockk(relaxed = true), mockk(relaxed = true), mockk(relaxed = true), mockk(relaxed = true), mockk(relaxed = true)),
            1,
            0
        )

        val actors = getActors(filmFactsRepository, recentPromptsRepository)
        Assert.assertTrue(actors.isNotEmpty())
    }

    @Test
    fun `When getting actors and only one credit returns actors`() = runTest {
        coEvery {
            filmFactsRepository.getMovieCredits(any())
        } returns MovieCredits(
            0,
            listOf(
                MovieCreditEntry(0, "foo", null, "foo")
            )
        )

        val actors = getActors(filmFactsRepository, recentPromptsRepository)
        Assert.assertTrue(actors.isNotEmpty())
    }

    @Test
    fun `When getting actors and many credits returns actors`() = runTest {
        coEvery {
            filmFactsRepository.getMovieCredits(any())
        } returns MovieCredits(
            0,
            listOf(
                MovieCreditEntry(0, "foo", null, "foo"),
                MovieCreditEntry(0, "bar", null, "bar"),
                MovieCreditEntry(0, "fizz", null, "fizz"),
                MovieCreditEntry(0, "buzz", null, "buzz"),
                MovieCreditEntry(0, "fin", null, "fin"),
                MovieCreditEntry(0, "alpha", null, "alpha"),
                MovieCreditEntry(0, "bravo", null, "bravo"),
                MovieCreditEntry(0, "charlie", null, "charlie")
            )
        )

        val actors = getActors(filmFactsRepository, recentPromptsRepository)
        Assert.assertTrue(actors.isNotEmpty())
    }

    // endregion

    // region getActorCredits

    @Test
    fun `When getting actor credits and popular actors are recent returns null`() = runTest {
        coEvery {
            recentPromptsRepository.isRecentActor(any())
        } returns true

        val credits = getActorCredits(
            filmFactsRepository, recentPromptsRepository,
            popularActors,
            creditCount,
            creditFilter
        )

        Assert.assertNull(credits)
    }

    @Test
    fun `When getting actor credits with excluded actor and popular actors are recent returns null`() = runTest {
        coEvery {
            recentPromptsRepository.isRecentActor(any())
        } returns true

        val credits = getActorCredits(
            filmFactsRepository, recentPromptsRepository,
            popularActors,
            creditCount,
            creditFilter,
            Actor(0, "fin", 0)
        )

        Assert.assertNull(credits)
    }

    @Test
    fun `When getting actor credits with excluded actor and actor matches popular actors returns null`() = runTest {
        val actor = Actor(0, "fin", 0)
        val credits = getActorCredits(
            filmFactsRepository, recentPromptsRepository,
            listOf(actor, actor, actor, actor),
            creditCount,
            creditFilter,
            actor
        )

        Assert.assertNull(credits)
    }

    @Test
    fun `When getting actor credits with excluded actor and all actors have different gender returns null`() = runTest {
        val actor = Actor(0, "fin", 0)
        val otherActor = Actor(1, "fin", 1)
        val credits = getActorCredits(
            filmFactsRepository, recentPromptsRepository,
            listOf(otherActor, otherActor, otherActor, otherActor),
            creditCount,
            creditFilter,
            actor
        )

        Assert.assertNull(credits)
    }

    @Test
    fun `When getting initial actor if unable to get details returns null`() = runTest {
        coEvery {
            filmFactsRepository.getActorDetails(any())
        } returns null

        val credits = getActorCredits(
            filmFactsRepository, recentPromptsRepository,
            popularActors,
            creditCount,
            creditFilter
        )

        Assert.assertNull(credits)
    }

    @Test
    fun `When getting initial actor if details are empty returns null`() = runTest {
        coEvery {
            filmFactsRepository.getActorDetails(any())
        } returns PersonDetails(0, "foo", 0, "")

        val credits = getActorCredits(
            filmFactsRepository, recentPromptsRepository,
            popularActors,
            creditCount,
            creditFilter
        )

        Assert.assertNull(credits)
    }

    @Test
    fun `When getting initial credits and unable to get credits returns null`() = runTest {
        coEvery {
            filmFactsRepository.getActorCredits(any())
        } returns null

        val credits = getActorCredits(
            filmFactsRepository, recentPromptsRepository,
            popularActors,
            creditCount,
            creditFilter
        )

        Assert.assertNull(credits)
    }

    @Test
    fun `When getting filler credits and unable to get credits returns null`() = runTest {
        coEvery {
            filmFactsRepository.getActorCredits(any())
        } returns null

        val credits = getActorCredits(
            filmFactsRepository, recentPromptsRepository,
            popularActors,
            creditCount,
            creditFilter,
            Actor(0, "foo", 0)
        )

        Assert.assertNull(credits)
    }

    @Test
    fun `When getting initial credits and credits are empty returns null`() = runTest {
        coEvery {
            filmFactsRepository.getActorCredits(any())
        } returns emptyList()

        val credits = getActorCredits(
            filmFactsRepository, recentPromptsRepository,
            popularActors,
            creditCount,
            creditFilter
        )

        Assert.assertNull(credits)
    }

    @Test
    fun `When getting filler credits and credits are empty returns null`() = runTest {
        coEvery {
            filmFactsRepository.getActorCredits(any())
        } returns emptyList()

        val credits = getActorCredits(
            filmFactsRepository, recentPromptsRepository,
            popularActors,
            creditCount,
            creditFilter,
            Actor(0, "foo", 0)
        )

        Assert.assertNull(credits)
    }

    @Test
    fun `When getting credits and too few credits because have duplicate character name returns null`() = runTest {
        coEvery {
            filmFactsRepository.getActorCredits(any())
        } returns listOf(
            ActorCredits(0, "foo", "foo", emptyList(), 0, 0, "", ""),
            ActorCredits(0, "foo", "foo", emptyList(), 0, 0, "", ""),
            ActorCredits(0, "foo", "foo", emptyList(), 0, 0, "", ""),
            ActorCredits(0, "foo", "foo", emptyList(), 0, 0, "", "")
        )

        val credits = getActorCredits(
            filmFactsRepository, recentPromptsRepository,
            popularActors,
            (2 .. 4),
            creditFilter
        )

        Assert.assertNull(credits)
    }

    @Test
    fun `When getting credits and character name has unclosed parentheses removes everything past parenthesis`() = runTest {
        coEvery {
            filmFactsRepository.getActorCredits(any())
        } returns listOf(
            ActorCredits(0, "foo(", "foo", emptyList(), 0, 0, "", ""),
            ActorCredits(1, "bar(some stuff", "bar", emptyList(), 0, 0, "", ""),
            ActorCredits(2, "fizz((", "fizz", emptyList(), 0, 0, "", ""),
            ActorCredits(3, "buzz([{", "buzz", emptyList(), 0, 0, "", "")
        )

        val credits = getActorCredits(
            filmFactsRepository, recentPromptsRepository,
            popularActors,
            creditCount,
            creditFilter
        )
        val truncatedNames = listOf("foo", "bar", "fizz", "buzz")
        credits!!.third.forEach {
            Assert.assertTrue(truncatedNames.contains(it.characterName))
        }
    }

    @Test
    fun `When getting initial credits and data available returns credits`() = runTest {
        val credits = getActorCredits(
            filmFactsRepository, recentPromptsRepository,
            popularActors,
            creditCount,
            creditFilter
        )

        Assert.assertNotNull(credits)
    }

    @Test
    fun `When getting filler credits and data available returns credits`() = runTest {
        val credits = getActorCredits(
            filmFactsRepository, recentPromptsRepository,
            popularActors,
            creditCount,
            creditFilter,
            Actor(0, "", 0)
        )

        Assert.assertNotNull(credits)
    }

    @Test
    fun `When getting filler credits and not enough entries returns null`() = runTest {
        val credits = getActorCredits(
            filmFactsRepository, recentPromptsRepository,
            popularActors,
            (100..200),
            creditFilter,
            Actor(0, "", 0)
        )

        Assert.assertNull(credits)
    }

    // endregion

    // region dateWithinRange

    @Test
    fun `When date is blank return false`() {
        Assert.assertFalse(dateWithinRange("", null, null))
    }

    @Test
    fun `When date is malformed return false`() {
        Assert.assertFalse(dateWithinRange("foo", null, null))
    }

    @Test
    fun `When date has no range return true`() {
        Assert.assertTrue(dateWithinRange("2000-01-01", null, null))
    }

    @Test
    fun `When date is within range returns true`() {
        Assert.assertTrue(dateWithinRange(getTimestamp(5), 3, 7))
    }

    @Test
    fun `When date is before range returns false`() {
        Assert.assertFalse(dateWithinRange(getTimestamp(1), 3, 7))
    }

    @Test
    fun `When date is after range returns false`() {
        Assert.assertFalse(dateWithinRange(getTimestamp(9), 3, 7))
    }

    @Test
    fun `When date is before start with no end time returns false`() {
        Assert.assertFalse(dateWithinRange(getTimestamp(9), null, 7))
    }

    @Test
    fun `When date is after start with no end time returns true`() {
        Assert.assertTrue(dateWithinRange(getTimestamp(0), null, 7))
    }

    @Test
    fun `When date is after end with no start time returns false`() {
        Assert.assertFalse(dateWithinRange(getTimestamp(1), 3, null))
    }

    @Test
    fun `When date is before end with no start time returns true`() {
        Assert.assertTrue(dateWithinRange(getTimestamp(4), 3, null))
    }

    private fun getTimestamp(yearOffset: Int) =
        Calendar.getInstance().let {
            it.add(Calendar.YEAR, -yearOffset)
            "${it.get(Calendar.YEAR)}-${it.get(Calendar.MONTH)}-${it.get(Calendar.DAY_OF_MONTH)}"
        }

    // endregion

    private companion object {
        val popularActors = listOf(
            Actor(0, "foo", 0),
            Actor(1, "bar", 0),
            Actor(2, "fizz", 0),
            Actor(3, "buzz", 0)
        )
        val creditCount = (1 .. 4)
        val creditFilter: (ActorCredits) -> Boolean = { true }
    }
}
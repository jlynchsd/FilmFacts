package com.movietrivia.filmfacts.domain

import com.movietrivia.filmfacts.api.ActorTvShowCredits
import com.movietrivia.filmfacts.api.DiscoverTvShowResponse
import com.movietrivia.filmfacts.api.PersonDetails
import com.movietrivia.filmfacts.api.TvShowCreditEntry
import com.movietrivia.filmfacts.api.TvShowCredits
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

@RunWith(RobolectricTestRunner::class)
class GetTvShowActorsUtilTest {
    private lateinit var filmFactsRepository: FilmFactsRepository
    private lateinit var recentPromptsRepository: RecentPromptsRepository

    @Before
    fun setup() {
        filmFactsRepository = mockk(relaxed = true)
        recentPromptsRepository = mockk(relaxed = true)

        coEvery {
            filmFactsRepository.getTvShows(dateRange = any(), tvShowOrder = any(), includeGenres = any(), minimumVotes = any())
        } returns DiscoverTvShowResponse(
            1,
            listOf(stubDiscoverTvShow(), stubDiscoverTvShow(), stubDiscoverTvShow(), stubDiscoverTvShow()),
            1,
            4
        )

        coEvery {
            filmFactsRepository.getTvShowCredits(any())
        } returns TvShowCredits(
            0,
            listOf(
                TvShowCreditEntry(0, "foo", null, 0, "foo"),
                TvShowCreditEntry(0, "bar", null, 1, "bar"),
                TvShowCreditEntry(0, "fizz", null, 2, "fizz"),
                TvShowCreditEntry(0, "buzz", null, 3, "buzz"),
                TvShowCreditEntry(0, "fin", null, 4, "fin")
            )
        )

        coEvery {
            filmFactsRepository.getActorDetails(any())
        } returns PersonDetails(0, "foo", 0, "fooPath")

        coEvery {
            filmFactsRepository.getActorTvShowCredits(any())
        } returns listOf(
            stubActorTvShowCredits(0, "foo", "foo"),
            stubActorTvShowCredits(1, "bar", "bar"),
            stubActorTvShowCredits(2, "fizz", "fizz"),
            stubActorTvShowCredits(3, "buzz", "buzz")
        )
    }

    // region getActors

    @Test
    fun `When getting actors but seed tv shows are null returns empty result`() = runTest {
        coEvery {
            filmFactsRepository.getTvShows(dateRange = any(), tvShowOrder = any(), includeGenres = any(), minimumVotes = any())
        } returns null

        val actors = getTvShowActors(filmFactsRepository, recentPromptsRepository, logTag = "")
        Assert.assertTrue(actors.isEmpty())
    }

    @Test
    fun `When getting actors but seed tv shows are empty returns empty result`() = runTest {
        coEvery {
            filmFactsRepository.getTvShows(dateRange = any(), tvShowOrder = any(), includeGenres = any(), minimumVotes = any())
        } returns DiscoverTvShowResponse(1, emptyList(), 1, 0)

        val actors = getTvShowActors(filmFactsRepository, recentPromptsRepository, logTag = "")
        Assert.assertTrue(actors.isEmpty())
    }

    @Test
    fun `When getting actors but tv show credits are null returns empty result`() = runTest {
        coEvery {
            filmFactsRepository.getTvShowCredits(any())
        } returns null

        val actors = getTvShowActors(filmFactsRepository, recentPromptsRepository, logTag = "")
        Assert.assertTrue(actors.isEmpty())
    }

    @Test
    fun `When getting actors but tv show credits are empty returns empty result`() = runTest {
        coEvery {
            filmFactsRepository.getTvShowCredits(any())
        } returns TvShowCredits(0, emptyList())

        val actors = getTvShowActors(filmFactsRepository, recentPromptsRepository, logTag = "")
        Assert.assertTrue(actors.isEmpty())
    }

    @Test
    fun `When getting actors but all actors are recent returns empty result`() = runTest {
        coEvery {
            recentPromptsRepository.isRecentActor(any())
        } returns true

        val actors = getTvShowActors(filmFactsRepository, recentPromptsRepository, logTag = "")
        Assert.assertTrue(actors.isEmpty())
    }

    @Test
    fun `When getting actors and able to get data returns actors`() = runTest {
        val actors = getTvShowActors(filmFactsRepository, recentPromptsRepository, logTag = "")
        Assert.assertTrue(actors.isNotEmpty())
    }

    @Test
    fun `When getting actors and only one seed tv show returns actors`() = runTest {
        coEvery {
            filmFactsRepository.getTvShows(dateRange = any(), tvShowOrder = any(), includeGenres = any(), minimumVotes = any())
        } returns DiscoverTvShowResponse(
            1,
            listOf(mockk(relaxed = true)),
            1,
            0
        )

        val actors = getTvShowActors(filmFactsRepository, recentPromptsRepository, logTag = "")
        Assert.assertTrue(actors.isNotEmpty())
    }

    @Test
    fun `When getting actors and many seed tv shows returns actors`() = runTest {
        coEvery {
            filmFactsRepository.getTvShows(dateRange = any(), tvShowOrder = any(), includeGenres = any(), minimumVotes = any())
        } returns DiscoverTvShowResponse(
            1,
            listOf(mockk(relaxed = true), mockk(relaxed = true), mockk(relaxed = true), mockk(relaxed = true), mockk(relaxed = true), mockk(relaxed = true)),
            1,
            0
        )

        val actors = getTvShowActors(filmFactsRepository, recentPromptsRepository, logTag = "")
        Assert.assertTrue(actors.isNotEmpty())
    }

    @Test
    fun `When getting actors and only one credit returns actors`() = runTest {
        coEvery {
            filmFactsRepository.getTvShowCredits(any())
        } returns TvShowCredits(
            0,
            listOf(
                TvShowCreditEntry(0, "foo", null, 0, "foo")
            )
        )

        val actors = getTvShowActors(filmFactsRepository, recentPromptsRepository, logTag = "")
        Assert.assertTrue(actors.isNotEmpty())
    }

    @Test
    fun `When getting actors and many credits returns actors`() = runTest {
        coEvery {
            filmFactsRepository.getTvShowCredits(any())
        } returns TvShowCredits(
            0,
            listOf(
                TvShowCreditEntry(0, "foo", null, 0, "foo"),
                TvShowCreditEntry(0, "bar", null, 1, "bar"),
                TvShowCreditEntry(0, "fizz", null, 2, "fizz"),
                TvShowCreditEntry(0, "buzz", null, 3, "buzz"),
                TvShowCreditEntry(0, "fin", null, 4, "fin"),
                TvShowCreditEntry(0, "alpha", null, 5, "alpha"),
                TvShowCreditEntry(0, "bravo", null, 6, "bravo"),
                TvShowCreditEntry(0, "charlie", null, 7, "charlie")
            )
        )

        val actors = getTvShowActors(filmFactsRepository, recentPromptsRepository, logTag = "")
        Assert.assertTrue(actors.isNotEmpty())
    }

    // endregion


    // region getActorCredits

    @Test
    fun `When getting actor credits and popular actors are recent returns null`() = runTest {
        coEvery {
            recentPromptsRepository.isRecentActor(any())
        } returns true

        val credits = getActorTvShowCredits(
            filmFactsRepository, recentPromptsRepository,
            popularActors,
            creditCount,
            creditFilter,
            logTag = ""
        )

        Assert.assertNull(credits)
    }

    @Test
    fun `When getting actor credits with excluded actor and popular actors are recent returns null`() = runTest {
        coEvery {
            recentPromptsRepository.isRecentActor(any())
        } returns true

        val credits = getActorTvShowCredits(
            filmFactsRepository, recentPromptsRepository,
            popularActors,
            creditCount,
            creditFilter,
            Actor(0, "fin", 0),
            logTag = ""
        )

        Assert.assertNull(credits)
    }

    @Test
    fun `When getting actor credits with excluded actor and actor matches popular actors returns null`() = runTest {
        val actor = Actor(0, "fin", 0)
        val credits = getActorTvShowCredits(
            filmFactsRepository, recentPromptsRepository,
            listOf(actor, actor, actor, actor),
            creditCount,
            creditFilter,
            actor,
            logTag = ""
        )

        Assert.assertNull(credits)
    }

    @Test
    fun `When getting actor credits with excluded actor and all actors have different gender returns null`() = runTest {
        val actor = Actor(0, "fin", 0)
        val otherActor = Actor(1, "fin", 1)
        val credits = getActorTvShowCredits(
            filmFactsRepository, recentPromptsRepository,
            listOf(otherActor, otherActor, otherActor, otherActor),
            creditCount,
            creditFilter,
            actor,
            logTag = ""
        )

        Assert.assertNull(credits)
    }

    @Test
    fun `When getting initial actor if unable to get details returns null`() = runTest {
        coEvery {
            filmFactsRepository.getActorDetails(any())
        } returns null

        val credits = getActorTvShowCredits(
            filmFactsRepository, recentPromptsRepository,
            popularActors,
            creditCount,
            creditFilter,
            logTag = ""
        )

        Assert.assertNull(credits)
    }

    @Test
    fun `When getting initial actor if details are empty returns null`() = runTest {
        coEvery {
            filmFactsRepository.getActorDetails(any())
        } returns PersonDetails(0, "foo", 0, "")

        val credits = getActorTvShowCredits(
            filmFactsRepository, recentPromptsRepository,
            popularActors,
            creditCount,
            creditFilter,
            logTag = ""
        )

        Assert.assertNull(credits)
    }

    @Test
    fun `When getting initial credits and unable to get credits returns null`() = runTest {
        coEvery {
            filmFactsRepository.getActorTvShowCredits(any())
        } returns null

        val credits = getActorTvShowCredits(
            filmFactsRepository, recentPromptsRepository,
            popularActors,
            creditCount,
            creditFilter,
            logTag = ""
        )

        Assert.assertNull(credits)
    }

    @Test
    fun `When getting filler credits and unable to get credits returns null`() = runTest {
        coEvery {
            filmFactsRepository.getActorTvShowCredits(any())
        } returns null

        val credits = getActorTvShowCredits(
            filmFactsRepository, recentPromptsRepository,
            popularActors,
            creditCount,
            creditFilter,
            Actor(0, "foo", 0),
            logTag = ""
        )

        Assert.assertNull(credits)
    }

    @Test
    fun `When getting initial credits and credits are empty returns null`() = runTest {
        coEvery {
            filmFactsRepository.getActorTvShowCredits(any())
        } returns emptyList()

        val credits = getActorTvShowCredits(
            filmFactsRepository, recentPromptsRepository,
            popularActors,
            creditCount,
            creditFilter,
            logTag = ""
        )

        Assert.assertNull(credits)
    }

    @Test
    fun `When getting filler credits and credits are empty returns null`() = runTest {
        coEvery {
            filmFactsRepository.getActorTvShowCredits(any())
        } returns emptyList()

        val credits = getActorTvShowCredits(
            filmFactsRepository, recentPromptsRepository,
            popularActors,
            creditCount,
            creditFilter,
            Actor(0, "foo", 0),
            logTag = ""
        )

        Assert.assertNull(credits)
    }

    @Test
    fun `When getting credits and too few credits because have duplicate character name returns null`() = runTest {
        coEvery {
            filmFactsRepository.getActorTvShowCredits(any())
        } returns listOf(
            stubActorTvShowCredits(0, "foo", "foo"),
            stubActorTvShowCredits(0, "foo", "foo"),
            stubActorTvShowCredits(0, "foo", "foo"),
            stubActorTvShowCredits(0, "foo", "foo")
        )

        val credits = getActorTvShowCredits(
            filmFactsRepository, recentPromptsRepository,
            popularActors,
            (2 .. 4),
            creditFilter,
            logTag = ""
        )

        Assert.assertNull(credits)
    }

    @Test
    fun `When getting credits and character name has unclosed parentheses removes everything past parenthesis`() = runTest {
        coEvery {
            filmFactsRepository.getActorTvShowCredits(any())
        } returns listOf(
            stubActorTvShowCredits(0, "foo(", "foo"),
            stubActorTvShowCredits(0, "bar(some stuff", "foo"),
            stubActorTvShowCredits(0, "fizz((", "foo"),
            stubActorTvShowCredits(0, "buzz([{", "foo")
        )

        val credits = getActorTvShowCredits(
            filmFactsRepository, recentPromptsRepository,
            popularActors,
            creditCount,
            creditFilter,
            logTag = ""
        )
        val truncatedNames = listOf("foo", "bar", "fizz", "buzz")
        credits!!.third.forEach {
            Assert.assertTrue(truncatedNames.contains(it.characterName))
        }
    }

    @Test
    fun `When getting initial credits and data available returns credits`() = runTest {
        val credits = getActorTvShowCredits(
            filmFactsRepository, recentPromptsRepository,
            popularActors,
            creditCount,
            creditFilter,
            logTag = ""
        )

        Assert.assertNotNull(credits)
    }

    @Test
    fun `When getting filler credits and data available returns credits`() = runTest {
        val credits = getActorTvShowCredits(
            filmFactsRepository, recentPromptsRepository,
            popularActors,
            creditCount,
            creditFilter,
            Actor(0, "", 0),
            logTag = ""
        )

        Assert.assertNotNull(credits)
    }

    @Test
    fun `When getting filler credits and not enough entries returns null`() = runTest {
        val credits = getActorTvShowCredits(
            filmFactsRepository, recentPromptsRepository,
            popularActors,
            (100..200),
            creditFilter,
            Actor(0, "", 0),
            logTag = ""
        )

        Assert.assertNull(credits)
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
        val creditFilter: (ActorTvShowCredits) -> Boolean = { true }
    }
}
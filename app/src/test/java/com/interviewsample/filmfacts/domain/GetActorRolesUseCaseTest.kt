package com.interviewsample.filmfacts.domain

import com.movietrivia.filmfacts.domain.GetActorRolesUseCase
import com.movietrivia.filmfacts.domain.dateWithinRange
import com.movietrivia.filmfacts.domain.getActorCredits
import com.movietrivia.filmfacts.domain.getActors
import com.movietrivia.filmfacts.domain.preloadImages
import com.movietrivia.filmfacts.model.Actor
import com.movietrivia.filmfacts.model.FilmFactsRepository
import com.movietrivia.filmfacts.model.RecentPromptsRepository
import com.movietrivia.filmfacts.model.UiTextPrompt
import com.movietrivia.filmfacts.model.UserDataRepository
import com.movietrivia.filmfacts.model.UserSettings
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.runs
import io.mockk.slot
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.runTest
import okio.IOException
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test

class GetActorRolesUseCaseTest {

    private lateinit var filmFactsRepository: FilmFactsRepository
    private lateinit var recentPromptsRepository: RecentPromptsRepository
    private lateinit var userDataRepository: UserDataRepository

    private lateinit var userSettingsFlow: MutableSharedFlow<UserSettings>

    @Before
    fun setup() {
        filmFactsRepository = mockk(relaxed = true)
        recentPromptsRepository = mockk(relaxed = true)
        userDataRepository = mockk(relaxed = true)

        mockkStatic(::preloadImages)
        coEvery {
            preloadImages(any(), any())
        } returns true

        userSettingsFlow = MutableSharedFlow(replay = 1)
        userSettingsFlow.tryEmit(UserSettings())
        every {
            userDataRepository.userSettings
        } returns userSettingsFlow

        mockkStatic(::getActors)
        coEvery {
            getActors(any(), any(), any())
        } returns listOf(mockk(relaxed = true), mockk(relaxed = true), mockk(relaxed = true), mockk(relaxed = true))

        mockkStatic(::getActorCredits)
        coEvery {
            getActorCredits(any(), any(), any(), any(), any(), any())
        } returns Triple(Actor(0, "foo", 1), "bar", emptyList())

        every {
            recentPromptsRepository.addRecentActor(any())
        } just runs
    }

    @After
    fun teardown() {
        clearAllMocks()
    }

    @Test
    fun `When unable to get user settings returns null`() = runTest {
        val useCase = getUseCase(testScheduler)
        every {
            userDataRepository.userSettings
        } returns emptyFlow()

        Assert.assertNull(useCase.invoke(null))
    }

    @Test
    fun `When getting user settings throws exception returns null`() = runTest {
        val useCase = getUseCase(testScheduler)
        every {
            userDataRepository.userSettings
        } returns kotlinx.coroutines.flow.flow {
            throw IOException()
        }

        Assert.assertNull(useCase.invoke(null))
    }

    @Test
    fun `When not enough actors for use case returns null`() = runTest {
        val useCase = getUseCase(testScheduler)
        coEvery {
            getActors(any(), any(), any())
        } returns emptyList()

        Assert.assertNull(useCase.invoke(null))
    }

    @Test
    fun `When no actor credits returns null`() = runTest {
        val useCase = getUseCase(testScheduler)
        coEvery {
            getActors(any(), any(), any())
        } returns listOf(mockk(), mockk(), mockk(), mockk())

        coEvery {
            getActorCredits(any(), any(), any(), any(), any())
        } returns null

        Assert.assertNull(useCase.invoke(null))
    }

    @Test
    fun `When no other actor credits returns null`() = runTest {
        val useCase = getUseCase(testScheduler)
        coEvery {
            getActors(any(), any(), any())
        } returns listOf(mockk(), mockk(), mockk(), mockk())

        coEvery {
            getActorCredits(any(), any(), any(), any(), any(), any())
        } returnsMany listOf(Triple(Actor(0, "foo", 1), "bar", emptyList()), null)

        Assert.assertNull(useCase.invoke(null))
    }

    @Test
    fun `When all actors are available but unable to preload image returns null`() = runTest {
        val useCase = getUseCase(testScheduler)
        coEvery {
            getActors(any(), any(), any())
        } returns listOf(mockk(), mockk(), mockk(), mockk())

        coEvery {
            preloadImages(any(), any())
        } returns false

        Assert.assertNull(useCase.invoke(null))
    }

    @Test
    fun `When all actors are available but unable to get image url loads empty path`() = runTest {
        val useCase = getUseCase(testScheduler)
        coEvery {
            getActors(any(), any(), any())
        } returns listOf(mockk(), mockk(), mockk(), mockk())

        val urlSlot = slot<String>()

        coEvery {
            filmFactsRepository.getImageUrl(any(), any())
        } returns null

        coEvery {
            preloadImages(any(), capture(urlSlot))
        } returns false

        useCase.invoke(null)

        Assert.assertEquals("", urlSlot.captured)
    }

    @Test
    fun `When all actors are available and able to load image returns prompt`() = runTest {
        val useCase = getUseCase(testScheduler)
        coEvery {
            getActors(any(), any(), any())
        } returns listOf(mockk(), mockk(), mockk(), mockk())

        coEvery {
            preloadImages(any(), any())
        } returns true

        Assert.assertNotNull(useCase.invoke(null))
    }

    // region credit filter

    @Test
    fun `When actor's credits include excluded genres excludes the credits from the result`() = runTest {
        val useCase = getUseCase(testScheduler)
        stubForCreditFilter(
            userSettingsFlow, recentPromptsRepository, filmFactsRepository,
            settings = UserSettings(excludedFilmGenres = listOf(0,1,2))
        )

        coEvery {
            filmFactsRepository.getActorCredits(any())
        } returns listOf(
            stubActorCredits("foo", genres = listOf(0)),
            stubActorCredits("bar", genres = listOf(4)),
            stubActorCredits("fizz", genres = listOf(5)),
            stubActorCredits("buzz", genres = listOf(6)),
            stubActorCredits("fin", genres = listOf(7))
        )

        val prompt = useCase.invoke(null) as UiTextPrompt
        prompt.entries.forEach {
            Assert.assertNotEquals("foo", it.topContent)
        }
    }

    @Test
    fun `When excluding credits and not enough match returns no prompt`() = runTest {
        val useCase = getUseCase(testScheduler)
        stubForCreditFilter(
            userSettingsFlow,
            recentPromptsRepository,
            filmFactsRepository,
            settings = UserSettings(excludedFilmGenres = listOf(0,1,2,4,5,6))
        )

        coEvery {
            filmFactsRepository.getActorCredits(any())
        } returns listOf(
            stubActorCredits("foo", genres = listOf(0)),
            stubActorCredits("bar", genres = listOf(4)),
            stubActorCredits("fizz", genres = listOf(5)),
            stubActorCredits("buzz", genres = listOf(6)),
            stubActorCredits("fin", genres = listOf(7))
        )

        Assert.assertNull(useCase.invoke(null))
    }

    @Test
    fun `When specifying genre ids only returns credits that match all genres`() = runTest {
        val useCase = getUseCase(testScheduler)
        stubForCreditFilter(userSettingsFlow, recentPromptsRepository, filmFactsRepository)

        coEvery {
            filmFactsRepository.getActorCredits(any())
        } returns listOf(
            stubActorCredits("foo", genres = listOf(0)),
            stubActorCredits("bar", genres = listOf(1, 2)),
            stubActorCredits("fizz", genres = listOf(1, 2)),
            stubActorCredits("buzz", genres = listOf(1, 2)),
            stubActorCredits("fin", genres = listOf(1, 2))
        )

        val prompt = useCase.invoke(listOf(1, 2)) as UiTextPrompt
        prompt.entries.forEach {
            Assert.assertNotEquals("foo", it.topContent)
        }
    }

    @Test
    fun `When specifying genre ids and only partial matches returns no prompt`() = runTest {
        val useCase = getUseCase(testScheduler)
        stubForCreditFilter(userSettingsFlow, recentPromptsRepository, filmFactsRepository)

        coEvery {
            filmFactsRepository.getActorCredits(any())
        } returns listOf(
            stubActorCredits("foo", genres = listOf(0)),
            stubActorCredits("bar", genres = listOf(1, 2)),
            stubActorCredits("fizz", genres = listOf(1, 2)),
            stubActorCredits("buzz", genres = listOf(1, 2)),
            stubActorCredits("fin", genres = listOf(1, 2))
        )

        Assert.assertNull( useCase.invoke(listOf(1, 2, 3)))
    }

    @Test
    fun `When specifying language only returns credits that match language`() = runTest {
        val useCase = getUseCase(testScheduler)
        stubForCreditFilter(userSettingsFlow, recentPromptsRepository, filmFactsRepository)

        coEvery {
            filmFactsRepository.getActorCredits(any())
        } returns listOf(
            stubActorCredits("foo", language = "foo"),
            stubActorCredits("bar"),
            stubActorCredits("fizz"),
            stubActorCredits("buzz"),
            stubActorCredits("fin")
        )

        val prompt = useCase.invoke(null) as UiTextPrompt
        prompt.entries.forEach {
            Assert.assertNotEquals("foo", it.topContent)
        }
    }

    @Test
    fun `When specifying language but not enough credits match returns no prompt`() = runTest {
        val useCase = getUseCase(testScheduler)
        stubForCreditFilter(userSettingsFlow, recentPromptsRepository, filmFactsRepository)

        coEvery {
            filmFactsRepository.getActorCredits(any())
        } returns listOf(
            stubActorCredits("foo", language = "foo"),
            stubActorCredits("bar", language = "bar"),
            stubActorCredits("fizz", language = "fizz"),
            stubActorCredits("buzz", language = "buzz"),
            stubActorCredits("fin")
        )

        Assert.assertNull(useCase.invoke(null))
    }

    @Test
    fun `When specifying date range only returns credits that match date range`() = runTest {
        val useCase = getUseCase(testScheduler)
        stubForCreditFilter(userSettingsFlow, recentPromptsRepository, filmFactsRepository)
        every {
            dateWithinRange(any(), any(), any())
        } returnsMany listOf(false, true, true, true, true, false, true, true, true, true)

        coEvery {
            filmFactsRepository.getActorCredits(any())
        } returns listOf(
            stubActorCredits("foo"),
            stubActorCredits("bar"),
            stubActorCredits("fizz"),
            stubActorCredits("buzz"),
            stubActorCredits("fin")
        )

        val prompt = useCase.invoke(null) as UiTextPrompt
        prompt.entries.forEach {
            Assert.assertNotEquals("foo", it.topContent)
        }
    }

    @Test
    fun `When specifying date range but not enough credits match returns no prompt`() = runTest {
        val useCase = getUseCase(testScheduler)
        stubForCreditFilter(userSettingsFlow, recentPromptsRepository, filmFactsRepository)
        every {
            dateWithinRange(any(), any(), any())
        } returns false

        coEvery {
            filmFactsRepository.getActorCredits(any())
        } returns listOf(
            stubActorCredits("foo"),
            stubActorCredits("bar"),
            stubActorCredits("fizz"),
            stubActorCredits("buzz"),
            stubActorCredits("fin")
        )

        Assert.assertNull(useCase.invoke(null))
    }

    @Test
    fun `When credit character name is empty does not return credit`() = runTest {
        val useCase = getUseCase(testScheduler)
        stubForCreditFilter(userSettingsFlow, recentPromptsRepository, filmFactsRepository)

        coEvery {
            filmFactsRepository.getActorCredits(any())
        } returns listOf(
            stubActorCredits("foo", characterName = ""),
            stubActorCredits("bar"),
            stubActorCredits("fizz"),
            stubActorCredits("buzz"),
            stubActorCredits("fin")
        )

        val prompt = useCase.invoke(null) as UiTextPrompt
        prompt.entries.forEach {
            Assert.assertNotEquals("foo", it.subContent)
        }
    }

    @Test
    fun `When not enough credits due to empty character names returns no prompt`() = runTest {
        val useCase = getUseCase(testScheduler)
        stubForCreditFilter(userSettingsFlow, recentPromptsRepository, filmFactsRepository)

        coEvery {
            filmFactsRepository.getActorCredits(any())
        } returns listOf(
            stubActorCredits("foo", characterName = ""),
            stubActorCredits("bar", characterName = ""),
            stubActorCredits("fizz", characterName = ""),
            stubActorCredits("buzz", characterName = ""),
            stubActorCredits("fin")
        )

        Assert.assertNull(useCase.invoke(null))
    }

    @Test
    fun `When credit title is empty does not return credit`() = runTest {
        val useCase = getUseCase(testScheduler)
        stubForCreditFilter(userSettingsFlow, recentPromptsRepository, filmFactsRepository)

        coEvery {
            filmFactsRepository.getActorCredits(any())
        } returns listOf(
            stubActorCredits("", characterName = "foo"),
            stubActorCredits("bar"),
            stubActorCredits("fizz"),
            stubActorCredits("buzz"),
            stubActorCredits("fin")
        )

        val prompt = useCase.invoke(null) as UiTextPrompt
        prompt.entries.forEach {
            Assert.assertNotEquals("foo", it.topContent)
        }
    }

    @Test
    fun `When not enough credits due to empty titles returns no prompt`() = runTest {
        val useCase = getUseCase(testScheduler)
        stubForCreditFilter(userSettingsFlow, recentPromptsRepository, filmFactsRepository)

        coEvery {
            filmFactsRepository.getActorCredits(any())
        } returns listOf(
            stubActorCredits("", characterName = "foo"),
            stubActorCredits("", characterName = "bar"),
            stubActorCredits("", characterName = "fizz"),
            stubActorCredits("", characterName = "buzz"),
            stubActorCredits("fin")
        )

        Assert.assertNull(useCase.invoke(null))
    }

    @Test
    fun `When acting credit is for self not included in results`() = runTest {
        val useCase = getUseCase(testScheduler)
        stubForCreditFilter(userSettingsFlow, recentPromptsRepository, filmFactsRepository)

        coEvery {
            filmFactsRepository.getActorCredits(any())
        } returns listOf(
            stubActorCredits("foo", characterName = "foo self"),
            stubActorCredits("bar"),
            stubActorCredits("fizz"),
            stubActorCredits("buzz"),
            stubActorCredits("fin")
        )

        val prompt = useCase.invoke(null) as UiTextPrompt
        prompt.entries.forEach {
            Assert.assertNotEquals("foo", it.subContent)
        }
    }

    @Test
    fun `When not enough credits due to self credits returns no prompt`() = runTest {
        val useCase = getUseCase(testScheduler)
        stubForCreditFilter(userSettingsFlow, recentPromptsRepository, filmFactsRepository)

        coEvery {
            filmFactsRepository.getActorCredits(any())
        } returns listOf(
            stubActorCredits("foo", characterName = "foo self"),
            stubActorCredits("bar", characterName = "bar Self"),
            stubActorCredits("fizz", characterName = "fizz -self-"),
            stubActorCredits("buzz", characterName = "buzz SELF"),
            stubActorCredits("fin")
        )

        Assert.assertNull(useCase.invoke(null))
    }

    @Test
    fun `When acting credit name contains parenthesis removes content in parenthesis`() = runTest {
        val useCase = getUseCase(testScheduler)
        stubForCreditFilter(userSettingsFlow, recentPromptsRepository, filmFactsRepository)

        coEvery {
            filmFactsRepository.getActorCredits(any())
        } returns listOf(
            stubActorCredits("foo", characterName = "foo (1)"),
            stubActorCredits("bar", characterName = "bar (2)"),
            stubActorCredits("fizz", characterName = "fizz (3)"),
            stubActorCredits("buzz", characterName = "buzz (4)"),
            stubActorCredits("fin", characterName = "fin (5)")
        )

        val prompt = useCase.invoke(null) as UiTextPrompt
        prompt.entries.forEach {
            Assert.assertFalse(it.topContent.contains(Regex("\\(\\d\\)")))
        }
    }

    @Test
    fun `When acting credit is for numbered credit not included in results`() = runTest {
        val useCase = getUseCase(testScheduler)
        stubForCreditFilter(userSettingsFlow, recentPromptsRepository, filmFactsRepository)

        coEvery {
            filmFactsRepository.getActorCredits(any())
        } returns listOf(
            stubActorCredits("foo", characterName = "foo #1"),
            stubActorCredits("bar"),
            stubActorCredits("fizz"),
            stubActorCredits("buzz"),
            stubActorCredits("fin")
        )

        val prompt = useCase.invoke(null) as UiTextPrompt
        prompt.entries.forEach {
            Assert.assertNotEquals("foo", it.subContent)
        }
    }

    @Test
    fun `When not enough credits due to numbered credits returns no prompt`() = runTest {
        val useCase = getUseCase(testScheduler)
        stubForCreditFilter(userSettingsFlow, recentPromptsRepository, filmFactsRepository)

        coEvery {
            filmFactsRepository.getActorCredits(any())
        } returns listOf(
            stubActorCredits("foo", characterName = "foo #1"),
            stubActorCredits("bar", characterName = "bar #2"),
            stubActorCredits("fizz", characterName = "fizz #3"),
            stubActorCredits("buzz", characterName = "buzz #4"),
            stubActorCredits("fin")
        )

        Assert.assertNull(useCase.invoke(null))
    }

    @Test
    fun `When acting credit is for combined credit not included in results`() = runTest {
        val useCase = getUseCase(testScheduler)
        stubForCreditFilter(userSettingsFlow, recentPromptsRepository, filmFactsRepository)

        coEvery {
            filmFactsRepository.getActorCredits(any())
        } returns listOf(
            stubActorCredits("foo", characterName = "foo/foo1"),
            stubActorCredits("bar"),
            stubActorCredits("fizz"),
            stubActorCredits("buzz"),
            stubActorCredits("fin")
        )

        val prompt = useCase.invoke(null) as UiTextPrompt
        prompt.entries.forEach {
            Assert.assertNotEquals("foo", it.subContent)
        }
    }

    @Test
    fun `When not enough credits due to combined credits returns no prompt`() = runTest {
        val useCase = getUseCase(testScheduler)
        stubForCreditFilter(userSettingsFlow, recentPromptsRepository, filmFactsRepository)

        coEvery {
            filmFactsRepository.getActorCredits(any())
        } returns listOf(
            stubActorCredits("foo", characterName = "foo/foo1"),
            stubActorCredits("bar", characterName = "bar / barz"),
            stubActorCredits("fizz", characterName = "fizz/ fiiz"),
            stubActorCredits("buzz", characterName = "buzz   /   buzz"),
            stubActorCredits("fin")
        )

        Assert.assertNull(useCase.invoke(null))
    }

    @Test
    fun `When acting credit has low votes not included in results`() = runTest {
        val useCase = getUseCase(testScheduler)
        stubForCreditFilter(userSettingsFlow, recentPromptsRepository, filmFactsRepository)

        coEvery {
            filmFactsRepository.getActorCredits(any())
        } returns listOf(
            stubActorCredits("foo", votes = CreditFilterConstants.MIN_VOTE_COUNT - 1),
            stubActorCredits("bar"),
            stubActorCredits("fizz"),
            stubActorCredits("buzz"),
            stubActorCredits("fin")
        )

        val prompt = useCase.invoke(null) as UiTextPrompt
        prompt.entries.forEach {
            Assert.assertNotEquals("foo", it.subContent)
        }
    }

    @Test
    fun `When not enough credits due to low voted credits returns no prompt`() = runTest {
        val useCase = getUseCase(testScheduler)
        stubForCreditFilter(userSettingsFlow, recentPromptsRepository, filmFactsRepository)

        coEvery {
            filmFactsRepository.getActorCredits(any())
        } returns listOf(
            stubActorCredits("foo", votes = CreditFilterConstants.MIN_VOTE_COUNT - 1),
            stubActorCredits("bar", votes = CreditFilterConstants.MIN_VOTE_COUNT - 1),
            stubActorCredits("fizz", votes = CreditFilterConstants.MIN_VOTE_COUNT - 1),
            stubActorCredits("buzz", votes = CreditFilterConstants.MIN_VOTE_COUNT - 1),
            stubActorCredits("fin")
        )

        Assert.assertNull(useCase.invoke(null))
    }

    @Test
    fun `When acting credit has low billing not included in results`() = runTest {
        val useCase = getUseCase(testScheduler)
        stubForCreditFilter(userSettingsFlow, recentPromptsRepository, filmFactsRepository)

        coEvery {
            filmFactsRepository.getActorCredits(any())
        } returns listOf(
            stubActorCredits("foo", order = CreditFilterConstants.MIN_ORDER + 1),
            stubActorCredits("bar"),
            stubActorCredits("fizz"),
            stubActorCredits("buzz"),
            stubActorCredits("fin")
        )

        val prompt = useCase.invoke(null) as UiTextPrompt
        prompt.entries.forEach {
            Assert.assertNotEquals("foo", it.subContent)
        }
    }

    @Test
    fun `When not enough credits due to low credit billing returns no prompt`() = runTest {
        val useCase = getUseCase(testScheduler)
        stubForCreditFilter(userSettingsFlow, recentPromptsRepository, filmFactsRepository)

        coEvery {
            filmFactsRepository.getActorCredits(any())
        } returns listOf(
            stubActorCredits("foo", order = CreditFilterConstants.MIN_ORDER + 1),
            stubActorCredits("bar", order = CreditFilterConstants.MIN_ORDER + 1),
            stubActorCredits("fizz", order = CreditFilterConstants.MIN_ORDER + 1),
            stubActorCredits("buzz", order = CreditFilterConstants.MIN_ORDER + 1),
            stubActorCredits("fin")
        )

        Assert.assertNull(useCase.invoke(null))
    }

    // endregion

    private fun getUseCase(testScheduler: TestCoroutineScheduler) =
        GetActorRolesUseCase(
            mockk(),
            filmFactsRepository,
            recentPromptsRepository,
            userDataRepository,
            StandardTestDispatcher(testScheduler)
        )
    
}
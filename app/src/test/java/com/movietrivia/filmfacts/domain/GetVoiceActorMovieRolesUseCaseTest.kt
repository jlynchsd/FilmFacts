package com.movietrivia.filmfacts.domain

import com.movietrivia.filmfacts.api.MovieGenre
import com.movietrivia.filmfacts.model.FilmFactsRepository
import com.movietrivia.filmfacts.model.RecentPromptsRepository
import com.movietrivia.filmfacts.model.UiTextPrompt
import com.movietrivia.filmfacts.model.UserDataRepository
import com.movietrivia.filmfacts.model.UserSettings
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.runTest
import okio.IOException
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class GetVoiceActorMovieRolesUseCaseTest {

    private lateinit var filmFactsRepository: FilmFactsRepository
    private lateinit var recentPromptsRepository: RecentPromptsRepository
    private lateinit var userDataRepository: UserDataRepository

    private lateinit var userSettingsFlow: MutableSharedFlow<UserSettings>

    @Before
    fun setup() {
        filmFactsRepository = mockk(relaxed = true)
        recentPromptsRepository = mockk(relaxed = true)
        userDataRepository = mockk(relaxed = true)

        userSettingsFlow = MutableSharedFlow(replay = 1)
        userSettingsFlow.tryEmit(UserSettings())
        every {
            userDataRepository.movieUserSettings
        } returns userSettingsFlow

        mockkStatic(::getMovieActors)
        coEvery {
            getMovieActors(any(), any(), any())
        } returns listOf(mockk(), mockk())

        coEvery {
            getActorMovieCredits(any(), any(), any(), any(), any(), any())
        } returns Triple(mockk(relaxed = true), "", listOf(mockk(relaxed = true), mockk(relaxed = true)))

        coEvery {
            filmFactsRepository.getImageUrl(any(), any())
        } returns "fooPath"

        mockkStatic(::preloadImages)
        coEvery {
            preloadImages(any(), *anyVararg())
        } returns true
    }

    @After
    fun teardown() {
        clearAllMocks()
    }

    @Test
    fun `When unable to get user settings returns null`() = runTest {
        val useCase = getUseCase(testScheduler)
        every {
            userDataRepository.movieUserSettings
        } returns emptyFlow()

        Assert.assertNull(useCase.invoke(null))
    }

    @Test
    fun `When getting user settings throws exception returns null`() = runTest {
        val useCase = getUseCase(testScheduler)
        every {
            userDataRepository.movieUserSettings
        } returns flow {
            throw IOException()
        }

        Assert.assertNull(useCase.invoke(null))
    }

    @Test
    fun `When user settings excludes animation returns null`() = runTest {
        val useCase = getUseCase(testScheduler)
        userSettingsFlow.tryEmit(UserSettings(excludedGenres = listOf(MovieGenre.ANIMATION.key)))

        Assert.assertNull(useCase.invoke(null))
    }

    @Test
    fun `When unable to get actors returns null`() = runTest {
        val useCase = getUseCase(testScheduler)
        coEvery {
            getMovieActors(any(), any(), any())
        } returns emptyList()

        Assert.assertNull(useCase.invoke(null))
    }

    @Test
    fun `When genres are null adds default animation genre`() = runTest {
        val useCase = getUseCase(testScheduler)
        val slot = slot<List<Int>?>()
        coEvery {
            getMovieActors(any(), any(), captureNullable(slot))
        } returns emptyList()

        useCase.invoke(null)

        Assert.assertTrue(slot.captured!!.contains(MovieGenre.ANIMATION.key))
    }

    @Test
    fun `When genres do not include animation adds default animation genre`() = runTest {
        val useCase = getUseCase(testScheduler)
        val slot = slot<List<Int>?>()
        coEvery {
            getMovieActors(any(), any(), captureNullable(slot))
        } returns emptyList()

        useCase.invoke(listOf(1,2,3))

        Assert.assertTrue(slot.captured!!.contains(MovieGenre.ANIMATION.key))
    }

    @Test
    fun `When unable to get actor credits returns null`() = runTest {
        val useCase = getUseCase(testScheduler)
        coEvery {
            getActorMovieCredits(any(), any(), any(), any(), any())
        } returns null

        Assert.assertNull(useCase.invoke(null))
    }

    @Test
    fun `When unable to get filler actor credits returns null`() = runTest {
        val useCase = getUseCase(testScheduler)
        coEvery {
            getActorMovieCredits(any(), any(), any(), any(), any(), any())
        } returnsMany listOf(
            Triple(mockk(relaxed = true), "", listOf(mockk(relaxed = true), mockk(relaxed = true))),
            null
        )

        Assert.assertNull(useCase.invoke(null))
    }

    @Test
    fun `When able to get main actor and filler actors returns prompt and saves recent actor`() = runTest {
        val useCase = getUseCase(testScheduler)

        Assert.assertTrue(useCase.invoke(null) is UiTextPrompt)
        verify(exactly = 1) { recentPromptsRepository.addRecentActor(any()) }
    }

    @Test
    fun `When unable to preload images returns null`() = runTest {
        val useCase = getUseCase(testScheduler)
        coEvery {
            preloadImages(any(), *anyVararg())
        } returns false

        Assert.assertNull(useCase.invoke(null))
    }

    @Test
    fun `When unable to get image paths returns empty paths`() = runTest {
        val useCase = getUseCase(testScheduler)
        coEvery {
            filmFactsRepository.getImageUrl(any(), any())
        } returns null

        val prompt = useCase.invoke(null) as UiTextPrompt
        prompt.images.forEach {
            Assert.assertTrue(it.imagePath.isEmpty())
        }
    }

    // region credit filter

    @Test
    fun `When actor's credits include excluded genres excludes the credits from the result`() = runTest {
        val useCase = getUseCase(testScheduler)
        stubForMovieCreditFilter(
            userSettingsFlow, recentPromptsRepository, filmFactsRepository,
            settings = UserSettings(excludedGenres = listOf(0,1,2))
        )

        coEvery {
            filmFactsRepository.getActorMovieCredits(any())
        } returns listOf(
            stubActorCredits("foo", genres = listOf(0, MovieGenre.ANIMATION.key)),
            stubActorCredits("bar", genres = listOf(4, MovieGenre.ANIMATION.key)),
            stubActorCredits("fizz", genres = listOf(5, MovieGenre.ANIMATION.key)),
            stubActorCredits("buzz", genres = listOf(6, MovieGenre.ANIMATION.key)),
            stubActorCredits("fin", genres = listOf(7, MovieGenre.ANIMATION.key))
        )

        val prompt = useCase.invoke(null) as UiTextPrompt
        prompt.entries.forEach {
            Assert.assertNotEquals("foo", it.topContent)
        }
    }

    @Test
    fun `When excluding credits and not enough match returns no prompt`() = runTest {
        val useCase = getUseCase(testScheduler)
        stubForMovieCreditFilter(
            userSettingsFlow,
            recentPromptsRepository,
            filmFactsRepository,
            settings = UserSettings(excludedGenres = listOf(0,1,2,4,5,6))
        )

        coEvery {
            filmFactsRepository.getActorMovieCredits(any())
        } returns listOf(
            stubActorCredits("foo", genres = listOf(0, MovieGenre.ANIMATION.key)),
            stubActorCredits("bar", genres = listOf(4, MovieGenre.ANIMATION.key)),
            stubActorCredits("fizz", genres = listOf(5, MovieGenre.ANIMATION.key)),
            stubActorCredits("buzz", genres = listOf(6, MovieGenre.ANIMATION.key)),
            stubActorCredits("fin", genres = listOf(7, MovieGenre.ANIMATION.key))
        )

        Assert.assertNull(useCase.invoke(null))
    }

    @Test
    fun `When actor's credits do not include animation excludes the credits from the result`() = runTest {
        val useCase = getUseCase(testScheduler)
        stubForMovieCreditFilter(userSettingsFlow, recentPromptsRepository, filmFactsRepository)

        coEvery {
            filmFactsRepository.getActorMovieCredits(any())
        } returns listOf(
            stubActorCredits("foo", genres = listOf(0)),
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
    fun `When specifying genre ids only returns credits that match all genres`() = runTest {
        val useCase = getUseCase(testScheduler)
        stubForMovieCreditFilter(userSettingsFlow, recentPromptsRepository, filmFactsRepository)

        coEvery {
            filmFactsRepository.getActorMovieCredits(any())
        } returns listOf(
            stubActorCredits("foo", genres = listOf(0, MovieGenre.ANIMATION.key)),
            stubActorCredits("bar", genres = listOf(1, 2, MovieGenre.ANIMATION.key)),
            stubActorCredits("fizz", genres = listOf(1, 2, MovieGenre.ANIMATION.key)),
            stubActorCredits("buzz", genres = listOf(1, 2, MovieGenre.ANIMATION.key)),
            stubActorCredits("fin", genres = listOf(1, 2, MovieGenre.ANIMATION.key))
        )

        val prompt = useCase.invoke(listOf(1, 2)) as UiTextPrompt
        prompt.entries.forEach {
            Assert.assertNotEquals("foo", it.topContent)
        }
    }

    @Test
    fun `When specifying genre ids and only partial matches returns no prompt`() = runTest {
        val useCase = getUseCase(testScheduler)
        stubForMovieCreditFilter(userSettingsFlow, recentPromptsRepository, filmFactsRepository)

        coEvery {
            filmFactsRepository.getActorMovieCredits(any())
        } returns listOf(
            stubActorCredits("foo", genres = listOf(0, MovieGenre.ANIMATION.key)),
            stubActorCredits("bar", genres = listOf(1, 2, MovieGenre.ANIMATION.key)),
            stubActorCredits("fizz", genres = listOf(1, 2, MovieGenre.ANIMATION.key)),
            stubActorCredits("buzz", genres = listOf(1, 2, MovieGenre.ANIMATION.key)),
            stubActorCredits("fin", genres = listOf(1, 2, MovieGenre.ANIMATION.key))
        )

        Assert.assertNull( useCase.invoke(listOf(1, 2, 3)))
    }

    @Test
    fun `When specifying language only returns credits that match language`() = runTest {
        val useCase = getUseCase(testScheduler)
        stubForMovieCreditFilter(userSettingsFlow, recentPromptsRepository, filmFactsRepository)

        coEvery {
            filmFactsRepository.getActorMovieCredits(any())
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
        stubForMovieCreditFilter(userSettingsFlow, recentPromptsRepository, filmFactsRepository)

        coEvery {
            filmFactsRepository.getActorMovieCredits(any())
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
        stubForMovieCreditFilter(userSettingsFlow, recentPromptsRepository, filmFactsRepository)
        every {
            dateWithinRange(any(), any(), any())
        } returnsMany listOf(false, true, true, true, true, false, true, true, true, true)

        coEvery {
            filmFactsRepository.getActorMovieCredits(any())
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
        stubForMovieCreditFilter(userSettingsFlow, recentPromptsRepository, filmFactsRepository)
        every {
            dateWithinRange(any(), any(), any())
        } returns false

        coEvery {
            filmFactsRepository.getActorMovieCredits(any())
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
        stubForMovieCreditFilter(userSettingsFlow, recentPromptsRepository, filmFactsRepository)

        coEvery {
            filmFactsRepository.getActorMovieCredits(any())
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
        stubForMovieCreditFilter(userSettingsFlow, recentPromptsRepository, filmFactsRepository)

        coEvery {
            filmFactsRepository.getActorMovieCredits(any())
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
        stubForMovieCreditFilter(userSettingsFlow, recentPromptsRepository, filmFactsRepository)

        coEvery {
            filmFactsRepository.getActorMovieCredits(any())
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
        stubForMovieCreditFilter(userSettingsFlow, recentPromptsRepository, filmFactsRepository)

        coEvery {
            filmFactsRepository.getActorMovieCredits(any())
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
        stubForMovieCreditFilter(userSettingsFlow, recentPromptsRepository, filmFactsRepository)

        coEvery {
            filmFactsRepository.getActorMovieCredits(any())
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
        stubForMovieCreditFilter(userSettingsFlow, recentPromptsRepository, filmFactsRepository)

        coEvery {
            filmFactsRepository.getActorMovieCredits(any())
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
    fun `When acting credit is for additional role not included in results`() = runTest {
        val useCase = getUseCase(testScheduler)
        stubForMovieCreditFilter(userSettingsFlow, recentPromptsRepository, filmFactsRepository)

        coEvery {
            filmFactsRepository.getActorMovieCredits(any())
        } returns listOf(
            stubActorCredits("foo", characterName = "additional foo"),
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
    fun `When not enough credits due to additional credits returns no prompt`() = runTest {
        val useCase = getUseCase(testScheduler)
        stubForMovieCreditFilter(userSettingsFlow, recentPromptsRepository, filmFactsRepository)

        coEvery {
            filmFactsRepository.getActorMovieCredits(any())
        } returns listOf(
            stubActorCredits("foo", characterName = "additional foo"),
            stubActorCredits("bar", characterName = "Additional bar"),
            stubActorCredits("fizz", characterName = "-additional- fizz"),
            stubActorCredits("buzz", characterName = "ADDITIONAL buzz"),
            stubActorCredits("fin")
        )

        Assert.assertNull(useCase.invoke(null))
    }

    @Test
    fun `When acting credit name contains parenthesis removes content in parenthesis`() = runTest {
        val useCase = getUseCase(testScheduler)
        stubForMovieCreditFilter(userSettingsFlow, recentPromptsRepository, filmFactsRepository)

        coEvery {
            filmFactsRepository.getActorMovieCredits(any())
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
        stubForMovieCreditFilter(userSettingsFlow, recentPromptsRepository, filmFactsRepository)

        coEvery {
            filmFactsRepository.getActorMovieCredits(any())
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
        stubForMovieCreditFilter(userSettingsFlow, recentPromptsRepository, filmFactsRepository)

        coEvery {
            filmFactsRepository.getActorMovieCredits(any())
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
        stubForMovieCreditFilter(userSettingsFlow, recentPromptsRepository, filmFactsRepository)

        coEvery {
            filmFactsRepository.getActorMovieCredits(any())
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
        stubForMovieCreditFilter(userSettingsFlow, recentPromptsRepository, filmFactsRepository)

        coEvery {
            filmFactsRepository.getActorMovieCredits(any())
        } returns listOf(
            stubActorCredits("foo", characterName = "foo/foo1"),
            stubActorCredits("bar", characterName = "bar /barz"),
            stubActorCredits("fizz", characterName = "fizz/ fiiz"),
            stubActorCredits("buzz", characterName = "buzz/buzz"),
            stubActorCredits("fin")
        )

        Assert.assertNull(useCase.invoke(null))
    }

    @Test
    fun `When acting credit is for multiple credits not included in results`() = runTest {
        val useCase = getUseCase(testScheduler)
        stubForMovieCreditFilter(userSettingsFlow, recentPromptsRepository, filmFactsRepository)

        coEvery {
            filmFactsRepository.getActorMovieCredits(any())
        } returns listOf(
            stubActorCredits("foo", characterName = "foo,foo1"),
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
    fun `When not enough credits due to multiple credits returns no prompt`() = runTest {
        val useCase = getUseCase(testScheduler)
        stubForMovieCreditFilter(userSettingsFlow, recentPromptsRepository, filmFactsRepository)

        coEvery {
            filmFactsRepository.getActorMovieCredits(any())
        } returns listOf(
            stubActorCredits("foo", characterName = "foo,foo1"),
            stubActorCredits("bar", characterName = "bar ,barz"),
            stubActorCredits("fizz", characterName = "fizz, fiiz"),
            stubActorCredits("buzz", characterName = "buzz,buzz"),
            stubActorCredits("fin")
        )

        Assert.assertNull(useCase.invoke(null))
    }

    @Test
    fun `When acting credit has long name is not included in results`() = runTest {
        val useCase = getUseCase(testScheduler)
        stubForMovieCreditFilter(userSettingsFlow, recentPromptsRepository, filmFactsRepository)

        coEvery {
            filmFactsRepository.getActorMovieCredits(any())
        } returns listOf(
            stubActorCredits("foo", characterName = "some minor character name"),
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
    fun `When not enough credits due to long names returns no prompt`() = runTest {
        val useCase = getUseCase(testScheduler)
        stubForMovieCreditFilter(userSettingsFlow, recentPromptsRepository, filmFactsRepository)

        coEvery {
            filmFactsRepository.getActorMovieCredits(any())
        } returns listOf(
            stubActorCredits("foo", characterName = "some minor character name"),
            stubActorCredits("bar", characterName = "a characters long description"),
            stubActorCredits("fizz", characterName = "bodyguard of the main villain"),
            stubActorCredits("buzz", characterName = "friendly town shopkeeper person"),
            stubActorCredits("fin")
        )

        Assert.assertNull(useCase.invoke(null))
    }

    @Test
    fun `When acting credit has low votes not included in results`() = runTest {
        val useCase = getUseCase(testScheduler)
        stubForMovieCreditFilter(userSettingsFlow, recentPromptsRepository, filmFactsRepository)

        coEvery {
            filmFactsRepository.getActorMovieCredits(any())
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
        stubForMovieCreditFilter(userSettingsFlow, recentPromptsRepository, filmFactsRepository)

        coEvery {
            filmFactsRepository.getActorMovieCredits(any())
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
        stubForMovieCreditFilter(userSettingsFlow, recentPromptsRepository, filmFactsRepository)

        coEvery {
            filmFactsRepository.getActorMovieCredits(any())
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
        stubForMovieCreditFilter(userSettingsFlow, recentPromptsRepository, filmFactsRepository)

        coEvery {
            filmFactsRepository.getActorMovieCredits(any())
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
        GetVoiceActorMovieRolesUseCase(
            mockk(),
            filmFactsRepository,
            recentPromptsRepository,
            userDataRepository,
            StandardTestDispatcher(testScheduler)
        )
}
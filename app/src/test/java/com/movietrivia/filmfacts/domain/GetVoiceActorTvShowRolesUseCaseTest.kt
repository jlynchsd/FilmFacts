package com.movietrivia.filmfacts.domain

import com.movietrivia.filmfacts.api.TvGenre
import com.movietrivia.filmfacts.model.CalendarProvider
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
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.IOException

@RunWith(RobolectricTestRunner::class)
class GetVoiceActorTvShowRolesUseCaseTest {
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
            userDataRepository.tvShowUserSettings
        } returns userSettingsFlow

        mockkStatic(::getTvShowActors)
        coEvery {
            getTvShowActors(any(), any(), any(), any(), logTag = any())
        } returns listOf(mockk(), mockk())

        coEvery {
            getActorTvShowCredits(any(), any(), any(), any(), any(), any(), logTag = any())
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
            userDataRepository.tvShowUserSettings
        } returns emptyFlow()

        Assert.assertNull(useCase.invoke(null))
    }

    @Test
    fun `When getting user settings throws exception returns null`() = runTest {
        val useCase = getUseCase(testScheduler)
        every {
            userDataRepository.tvShowUserSettings
        } returns flow {
            throw IOException()
        }

        Assert.assertNull(useCase.invoke(null))
    }

    @Test
    fun `When user settings excludes animation returns null`() = runTest {
        val useCase = getUseCase(testScheduler)
        userSettingsFlow.tryEmit(UserSettings(excludedGenres = listOf(TvGenre.ANIMATION.key)))

        Assert.assertNull(useCase.invoke(null))
    }

    @Test
    fun `When unable to get actors returns null`() = runTest {
        val useCase = getUseCase(testScheduler)
        coEvery {
            getTvShowActors(any(), any(), any(), any(), logTag = any())
        } returns emptyList()

        Assert.assertNull(useCase.invoke(null))
    }

    @Test
    fun `When genres are null adds default animation genre`() = runTest {
        val useCase = getUseCase(testScheduler)
        val slot = slot<List<Int>?>()
        coEvery {
            getTvShowActors(any(), any(), any(), captureNullable(slot), logTag = any())
        } returns emptyList()

        useCase.invoke(null)

        Assert.assertTrue(slot.captured!!.contains(TvGenre.ANIMATION.key))
    }

    @Test
    fun `When genres do not include animation adds default animation genre`() = runTest {
        val useCase = getUseCase(testScheduler)
        val slot = slot<List<Int>?>()
        coEvery {
            getTvShowActors(any(), any(), any(), captureNullable(slot), logTag = any())
        } returns emptyList()

        useCase.invoke(listOf(1,2,3))

        Assert.assertTrue(slot.captured!!.contains(TvGenre.ANIMATION.key))
    }

    @Test
    fun `When unable to get actor credits returns null`() = runTest {
        val useCase = getUseCase(testScheduler)
        coEvery {
            getActorTvShowCredits(any(), any(), any(), any(), any(), logTag = any())
        } returns null

        Assert.assertNull(useCase.invoke(null))
    }

    @Test
    fun `When unable to get filler actor credits returns null`() = runTest {
        val useCase = getUseCase(testScheduler)
        coEvery {
            getActorTvShowCredits(any(), any(), any(), any(), any(), any(), logTag = any())
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
        stubForTvShowCreditFilter(
            userSettingsFlow, recentPromptsRepository, filmFactsRepository,
            settings = UserSettings(excludedGenres = listOf(0,1,2))
        )

        coEvery {
            filmFactsRepository.getActorTvShowCredits(any())
        } returns listOf(
            stubActorTvShowCredits(characterName = "foo", genres = listOf(0, TvGenre.ANIMATION.key)),
            stubActorTvShowCredits(characterName = "bar", genres = listOf(4, TvGenre.ANIMATION.key)),
            stubActorTvShowCredits(characterName = "fizz", genres = listOf(5, TvGenre.ANIMATION.key)),
            stubActorTvShowCredits(characterName = "buzz", genres = listOf(6, TvGenre.ANIMATION.key)),
            stubActorTvShowCredits(characterName = "fin", genres = listOf(7, TvGenre.ANIMATION.key))
        )

        val prompt = useCase.invoke(null) as UiTextPrompt
        prompt.entries.forEach {
            Assert.assertNotEquals("foo", it.topContent)
        }
    }

    @Test
    fun `When excluding credits and not enough match returns no prompt`() = runTest {
        val useCase = getUseCase(testScheduler)
        stubForTvShowCreditFilter(
            userSettingsFlow,
            recentPromptsRepository,
            filmFactsRepository,
            settings = UserSettings(excludedGenres = listOf(0,1,2,4,5,6))
        )

        coEvery {
            filmFactsRepository.getActorTvShowCredits(any())
        } returns listOf(
            stubActorTvShowCredits(characterName = "foo", genres = listOf(0, TvGenre.ANIMATION.key)),
            stubActorTvShowCredits(characterName = "bar", genres = listOf(4, TvGenre.ANIMATION.key)),
            stubActorTvShowCredits(characterName = "fizz", genres = listOf(5, TvGenre.ANIMATION.key)),
            stubActorTvShowCredits(characterName = "buzz", genres = listOf(6, TvGenre.ANIMATION.key)),
            stubActorTvShowCredits(characterName = "fin", genres = listOf(7, TvGenre.ANIMATION.key))
        )

        Assert.assertNull(useCase.invoke(null))
    }

    @Test
    fun `When actor's credits do not include animation excludes the credits from the result`() = runTest {
        val useCase = getUseCase(testScheduler)
        stubForTvShowCreditFilter(userSettingsFlow, recentPromptsRepository, filmFactsRepository)

        coEvery {
            filmFactsRepository.getActorTvShowCredits(any())
        } returns listOf(
            stubActorTvShowCredits(characterName = "foo", genres = listOf(0)),
            stubActorTvShowCredits(characterName = "bar"),
            stubActorTvShowCredits(characterName = "fizz"),
            stubActorTvShowCredits(characterName = "buzz"),
            stubActorTvShowCredits(characterName = "fin")
        )

        val prompt = useCase.invoke(null) as UiTextPrompt
        prompt.entries.forEach {
            Assert.assertNotEquals("foo", it.topContent)
        }
    }

    @Test
    fun `When specifying genre ids only returns credits that match all genres`() = runTest {
        val useCase = getUseCase(testScheduler)
        stubForTvShowCreditFilter(userSettingsFlow, recentPromptsRepository, filmFactsRepository)

        coEvery {
            filmFactsRepository.getActorTvShowCredits(any())
        } returns listOf(
            stubActorTvShowCredits(characterName = "foo", genres = listOf(0, TvGenre.ANIMATION.key)),
            stubActorTvShowCredits(characterName = "bar", genres = listOf(1, 2, TvGenre.ANIMATION.key)),
            stubActorTvShowCredits(characterName = "fizz", genres = listOf(1, 2, TvGenre.ANIMATION.key)),
            stubActorTvShowCredits(characterName = "buzz", genres = listOf(1, 2, TvGenre.ANIMATION.key)),
            stubActorTvShowCredits(characterName = "fin", genres = listOf(1, 2, TvGenre.ANIMATION.key))
        )

        val prompt = useCase.invoke(listOf(1, 2)) as UiTextPrompt
        prompt.entries.forEach {
            Assert.assertNotEquals("foo", it.topContent)
        }
    }

    @Test
    fun `When specifying genre ids and only partial matches returns no prompt`() = runTest {
        val useCase = getUseCase(testScheduler)
        stubForTvShowCreditFilter(userSettingsFlow, recentPromptsRepository, filmFactsRepository)

        coEvery {
            filmFactsRepository.getActorTvShowCredits(any())
        } returns listOf(
            stubActorTvShowCredits(characterName = "foo", genres = listOf(0, TvGenre.ANIMATION.key)),
            stubActorTvShowCredits(characterName = "bar", genres = listOf(1, 2, TvGenre.ANIMATION.key)),
            stubActorTvShowCredits(characterName = "fizz", genres = listOf(1, 2, TvGenre.ANIMATION.key)),
            stubActorTvShowCredits(characterName = "buzz", genres = listOf(1, 2, TvGenre.ANIMATION.key)),
            stubActorTvShowCredits(characterName = "fin", genres = listOf(1, 2, TvGenre.ANIMATION.key))
        )

        Assert.assertNull( useCase.invoke(listOf(1, 2, 3)))
    }

    @Test
    fun `When specifying language only returns credits that match language`() = runTest {
        val useCase = getUseCase(testScheduler)
        stubForTvShowCreditFilter(userSettingsFlow, recentPromptsRepository, filmFactsRepository)

        coEvery {
            filmFactsRepository.getActorTvShowCredits(any())
        } returns listOf(
            stubActorTvShowCredits(characterName = "foo", language = "foo"),
            stubActorTvShowCredits(characterName = "bar"),
            stubActorTvShowCredits(characterName = "fizz"),
            stubActorTvShowCredits(characterName = "buzz"),
            stubActorTvShowCredits(characterName = "fin")
        )

        val prompt = useCase.invoke(null) as UiTextPrompt
        prompt.entries.forEach {
            Assert.assertNotEquals("foo", it.topContent)
        }
    }

    @Test
    fun `When specifying language but not enough credits match returns no prompt`() = runTest {
        val useCase = getUseCase(testScheduler)
        stubForTvShowCreditFilter(userSettingsFlow, recentPromptsRepository, filmFactsRepository)

        coEvery {
            filmFactsRepository.getActorTvShowCredits(any())
        } returns listOf(
            stubActorTvShowCredits(characterName = "foo", language = "foo"),
            stubActorTvShowCredits(characterName = "bar", language = "bar"),
            stubActorTvShowCredits(characterName = "fizz", language = "fizz"),
            stubActorTvShowCredits(characterName = "buzz", language = "buzz"),
            stubActorTvShowCredits(characterName = "fin")
        )

        Assert.assertNull(useCase.invoke(null))
    }

    @Test
    fun `When specifying date range only returns credits that match date range`() = runTest {
        val useCase = getUseCase(testScheduler)
        stubForTvShowCreditFilter(userSettingsFlow, recentPromptsRepository, filmFactsRepository)
        every {
            dateWithinRange(any(), any(), any())
        } returnsMany listOf(false, true, true, true, true, false, true, true, true, true)

        coEvery {
            filmFactsRepository.getActorTvShowCredits(any())
        } returns listOf(
            stubActorTvShowCredits(characterName = "foo"),
            stubActorTvShowCredits(characterName = "bar"),
            stubActorTvShowCredits(characterName = "fizz"),
            stubActorTvShowCredits(characterName = "buzz"),
            stubActorTvShowCredits(characterName = "fin")
        )

        val prompt = useCase.invoke(null) as UiTextPrompt
        prompt.entries.forEach {
            Assert.assertNotEquals("foo", it.topContent)
        }
    }

    @Test
    fun `When specifying date range but not enough credits match returns no prompt`() = runTest {
        val useCase = getUseCase(testScheduler)
        stubForTvShowCreditFilter(userSettingsFlow, recentPromptsRepository, filmFactsRepository)
        every {
            dateWithinRange(any(), any(), any())
        } returns false

        coEvery {
            filmFactsRepository.getActorTvShowCredits(any())
        } returns listOf(
            stubActorTvShowCredits(characterName = "foo"),
            stubActorTvShowCredits(characterName = "bar"),
            stubActorTvShowCredits(characterName = "fizz"),
            stubActorTvShowCredits(characterName = "buzz"),
            stubActorTvShowCredits(characterName = "fin")
        )

        Assert.assertNull(useCase.invoke(null))
    }

    @Test
    fun `When credit character name is empty does not return credit`() = runTest {
        val useCase = getUseCase(testScheduler)
        stubForTvShowCreditFilter(userSettingsFlow, recentPromptsRepository, filmFactsRepository)

        coEvery {
            filmFactsRepository.getActorTvShowCredits(any())
        } returns listOf(
            stubActorTvShowCredits(characterName = "", showName = "foo"),
            stubActorTvShowCredits(characterName = "bar"),
            stubActorTvShowCredits(characterName = "fizz"),
            stubActorTvShowCredits(characterName = "buzz"),
            stubActorTvShowCredits(characterName = "fin")
        )

        val prompt = useCase.invoke(null) as UiTextPrompt
        prompt.entries.forEach {
            Assert.assertNotEquals("foo", it.subContent)
        }
    }

    @Test
    fun `When not enough credits due to empty character names returns no prompt`() = runTest {
        val useCase = getUseCase(testScheduler)
        stubForTvShowCreditFilter(userSettingsFlow, recentPromptsRepository, filmFactsRepository)

        coEvery {
            filmFactsRepository.getActorTvShowCredits(any())
        } returns listOf(
            stubActorTvShowCredits(showName = "foo", characterName = ""),
            stubActorTvShowCredits(showName = "bar", characterName = ""),
            stubActorTvShowCredits(showName = "fizz", characterName = ""),
            stubActorTvShowCredits(showName = "buzz", characterName = ""),
            stubActorTvShowCredits(showName = "fin", characterName = "fin")
        )

        Assert.assertNull(useCase.invoke(null))
    }

    @Test
    fun `When credit show name is empty does not return credit`() = runTest {
        val useCase = getUseCase(testScheduler)
        stubForTvShowCreditFilter(userSettingsFlow, recentPromptsRepository, filmFactsRepository)

        coEvery {
            filmFactsRepository.getActorTvShowCredits(any())
        } returns listOf(
            stubActorTvShowCredits(showName = "", characterName = "foo"),
            stubActorTvShowCredits(showName = "bar", characterName = "bar"),
            stubActorTvShowCredits(showName = "fizz", characterName = "fizz"),
            stubActorTvShowCredits(showName = "buzz", characterName = "buzz"),
            stubActorTvShowCredits(showName = "fin", characterName = "fin")
        )

        val prompt = useCase.invoke(null) as UiTextPrompt
        prompt.entries.forEach {
            Assert.assertNotEquals("foo", it.topContent)
        }
    }

    @Test
    fun `When not enough credits due to empty show names returns no prompt`() = runTest {
        val useCase = getUseCase(testScheduler)
        stubForTvShowCreditFilter(userSettingsFlow, recentPromptsRepository, filmFactsRepository)

        coEvery {
            filmFactsRepository.getActorTvShowCredits(any())
        } returns listOf(
            stubActorTvShowCredits(showName = "", characterName = "foo"),
            stubActorTvShowCredits(showName = "", characterName = "bar"),
            stubActorTvShowCredits(showName = "", characterName = "fizz"),
            stubActorTvShowCredits(showName = "", characterName = "buzz"),
            stubActorTvShowCredits(showName = "fin", characterName = "fin")
        )

        Assert.assertNull(useCase.invoke(null))
    }

    @Test
    fun `When acting credit is for self not included in results`() = runTest {
        val useCase = getUseCase(testScheduler)
        stubForTvShowCreditFilter(userSettingsFlow, recentPromptsRepository, filmFactsRepository)

        coEvery {
            filmFactsRepository.getActorTvShowCredits(any())
        } returns listOf(
            stubActorTvShowCredits(showName = "foo", characterName = "foo self"),
            stubActorTvShowCredits(showName = "bar", characterName = "bar"),
            stubActorTvShowCredits(showName = "fizz", characterName = "fizz"),
            stubActorTvShowCredits(showName = "buzz", characterName = "buzz"),
            stubActorTvShowCredits(showName = "fin", characterName = "fin")
        )

        val prompt = useCase.invoke(null) as UiTextPrompt
        prompt.entries.forEach {
            Assert.assertNotEquals("foo", it.subContent)
        }
    }

    @Test
    fun `When not enough credits due to self credits returns no prompt`() = runTest {
        val useCase = getUseCase(testScheduler)
        stubForTvShowCreditFilter(userSettingsFlow, recentPromptsRepository, filmFactsRepository)

        coEvery {
            filmFactsRepository.getActorTvShowCredits(any())
        } returns listOf(
            stubActorTvShowCredits(showName = "foo", characterName = "foo self"),
            stubActorTvShowCredits(showName = "bar", characterName = "bar Self"),
            stubActorTvShowCredits(showName = "fizz", characterName = "fizz -self-"),
            stubActorTvShowCredits(showName = "buzz", characterName = "buzz SELF"),
            stubActorTvShowCredits(showName = "fin", characterName = "fin")
        )

        Assert.assertNull(useCase.invoke(null))
    }

    @Test
    fun `When acting credit is for additional role not included in results`() = runTest {
        val useCase = getUseCase(testScheduler)
        stubForTvShowCreditFilter(userSettingsFlow, recentPromptsRepository, filmFactsRepository)

        coEvery {
            filmFactsRepository.getActorTvShowCredits(any())
        } returns listOf(
            stubActorTvShowCredits(showName = "foo", characterName = "additional foo"),
            stubActorTvShowCredits(showName = "bar", characterName = "bar"),
            stubActorTvShowCredits(showName = "fizz", characterName = "fizz"),
            stubActorTvShowCredits(showName = "buzz", characterName = "buzz"),
            stubActorTvShowCredits(showName = "fin", characterName = "fin")
        )

        val prompt = useCase.invoke(null) as UiTextPrompt
        prompt.entries.forEach {
            Assert.assertNotEquals("foo", it.subContent)
        }
    }

    @Test
    fun `When not enough credits due to additional credits returns no prompt`() = runTest {
        val useCase = getUseCase(testScheduler)
        stubForTvShowCreditFilter(userSettingsFlow, recentPromptsRepository, filmFactsRepository)

        coEvery {
            filmFactsRepository.getActorTvShowCredits(any())
        } returns listOf(
            stubActorTvShowCredits(showName = "foo", characterName = "additional foo"),
            stubActorTvShowCredits(showName = "bar", characterName = "Additional bar"),
            stubActorTvShowCredits(showName = "fizz", characterName = "-additional- fizz"),
            stubActorTvShowCredits(showName = "buzz", characterName = "ADDITIONAL buzz"),
            stubActorTvShowCredits(showName = "fin", characterName = "fin")
        )

        Assert.assertNull(useCase.invoke(null))
    }

    @Test
    fun `When acting credit name contains parenthesis removes content in parenthesis`() = runTest {
        val useCase = getUseCase(testScheduler)
        stubForTvShowCreditFilter(userSettingsFlow, recentPromptsRepository, filmFactsRepository)

        coEvery {
            filmFactsRepository.getActorTvShowCredits(any())
        } returns listOf(
            stubActorTvShowCredits(showName = "foo", characterName = "foo (1)"),
            stubActorTvShowCredits(showName = "bar", characterName = "bar (2)"),
            stubActorTvShowCredits(showName = "fizz", characterName = "fizz (3)"),
            stubActorTvShowCredits(showName = "buzz", characterName = "buzz (4)"),
            stubActorTvShowCredits(showName = "fin", characterName = "fin (5)")
        )

        val prompt = useCase.invoke(null) as UiTextPrompt
        prompt.entries.forEach {
            Assert.assertFalse(it.topContent.contains(Regex("\\(\\d\\)")))
        }
    }

    @Test
    fun `When acting credit is for numbered credit not included in results`() = runTest {
        val useCase = getUseCase(testScheduler)
        stubForTvShowCreditFilter(userSettingsFlow, recentPromptsRepository, filmFactsRepository)

        coEvery {
            filmFactsRepository.getActorTvShowCredits(any())
        } returns listOf(
            stubActorTvShowCredits(showName = "foo", characterName = "foo #1"),
            stubActorTvShowCredits(showName = "bar", characterName = "bar"),
            stubActorTvShowCredits(showName = "fizz", characterName = "fizz"),
            stubActorTvShowCredits(showName = "buzz", characterName = "buzz"),
            stubActorTvShowCredits(showName = "fin", characterName = "fin")
        )

        val prompt = useCase.invoke(null) as UiTextPrompt
        prompt.entries.forEach {
            Assert.assertNotEquals("foo", it.subContent)
        }
    }

    @Test
    fun `When not enough credits due to numbered credits returns no prompt`() = runTest {
        val useCase = getUseCase(testScheduler)
        stubForTvShowCreditFilter(userSettingsFlow, recentPromptsRepository, filmFactsRepository)

        coEvery {
            filmFactsRepository.getActorTvShowCredits(any())
        } returns listOf(
            stubActorTvShowCredits(showName = "foo", characterName = "foo #1"),
            stubActorTvShowCredits(showName = "bar", characterName = "bar #2"),
            stubActorTvShowCredits(showName = "fizz", characterName = "fizz #3"),
            stubActorTvShowCredits(showName = "buzz", characterName = "buzz #4"),
            stubActorTvShowCredits(showName = "fin", characterName = "fin")
        )

        Assert.assertNull(useCase.invoke(null))
    }

    @Test
    fun `When acting credit is for combined credit not included in results`() = runTest {
        val useCase = getUseCase(testScheduler)
        stubForTvShowCreditFilter(userSettingsFlow, recentPromptsRepository, filmFactsRepository)

        coEvery {
            filmFactsRepository.getActorTvShowCredits(any())
        } returns listOf(
            stubActorTvShowCredits(showName = "foo", characterName = "foo/foo1"),
            stubActorTvShowCredits(showName = "bar", characterName = "bar"),
            stubActorTvShowCredits(showName = "fizz", characterName = "fizz"),
            stubActorTvShowCredits(showName = "buzz", characterName = "buzz"),
            stubActorTvShowCredits(showName = "fin", characterName = "fin")
        )

        val prompt = useCase.invoke(null) as UiTextPrompt
        prompt.entries.forEach {
            Assert.assertNotEquals("foo", it.subContent)
        }
    }

    @Test
    fun `When not enough credits due to combined credits returns no prompt`() = runTest {
        val useCase = getUseCase(testScheduler)
        stubForTvShowCreditFilter(userSettingsFlow, recentPromptsRepository, filmFactsRepository)

        coEvery {
            filmFactsRepository.getActorTvShowCredits(any())
        } returns listOf(
            stubActorTvShowCredits(showName = "foo", characterName = "foo/foo1"),
            stubActorTvShowCredits(showName = "bar", characterName = "bar /barz"),
            stubActorTvShowCredits(showName = "fizz", characterName = "fizz/ fiiz"),
            stubActorTvShowCredits(showName = "buzz", characterName = "buzz/buzz"),
            stubActorTvShowCredits(showName = "fin", characterName = "fin")
        )

        Assert.assertNull(useCase.invoke(null))
    }

    @Test
    fun `When acting credit is for multiple credits not included in results`() = runTest {
        val useCase = getUseCase(testScheduler)
        stubForTvShowCreditFilter(userSettingsFlow, recentPromptsRepository, filmFactsRepository)

        coEvery {
            filmFactsRepository.getActorTvShowCredits(any())
        } returns listOf(
            stubActorTvShowCredits(showName = "foo", characterName = "foo,foo1"),
            stubActorTvShowCredits(showName = "bar", characterName = "bar"),
            stubActorTvShowCredits(showName = "fizz", characterName = "fizz"),
            stubActorTvShowCredits(showName = "buzz", characterName = "buzz"),
            stubActorTvShowCredits(showName = "fin", characterName = "fin")
        )

        val prompt = useCase.invoke(null) as UiTextPrompt
        prompt.entries.forEach {
            Assert.assertNotEquals("foo", it.subContent)
        }
    }

    @Test
    fun `When not enough credits due to multiple credits returns no prompt`() = runTest {
        val useCase = getUseCase(testScheduler)
        stubForTvShowCreditFilter(userSettingsFlow, recentPromptsRepository, filmFactsRepository)

        coEvery {
            filmFactsRepository.getActorTvShowCredits(any())
        } returns listOf(
            stubActorTvShowCredits(showName = "foo", characterName = "foo,foo1"),
            stubActorTvShowCredits(showName = "bar", characterName = "bar ,barz"),
            stubActorTvShowCredits(showName = "fizz", characterName = "fizz, fiiz"),
            stubActorTvShowCredits(showName = "buzz", characterName = "buzz,buzz"),
            stubActorTvShowCredits(showName = "fin", characterName = "fin")
        )

        Assert.assertNull(useCase.invoke(null))
    }

    @Test
    fun `When acting credit has long name is not included in results`() = runTest {
        val useCase = getUseCase(testScheduler)
        stubForTvShowCreditFilter(userSettingsFlow, recentPromptsRepository, filmFactsRepository)

        coEvery {
            filmFactsRepository.getActorTvShowCredits(any())
        } returns listOf(
            stubActorTvShowCredits(showName = "foo", characterName = "some minor character name"),
            stubActorTvShowCredits(showName = "bar", characterName = "bar"),
            stubActorTvShowCredits(showName = "fizz", characterName = "fizz"),
            stubActorTvShowCredits(showName = "buzz", characterName = "buzz"),
            stubActorTvShowCredits(showName = "fin", characterName = "fin")
        )

        val prompt = useCase.invoke(null) as UiTextPrompt
        prompt.entries.forEach {
            Assert.assertNotEquals("foo", it.subContent)
        }
    }

    @Test
    fun `When not enough credits due to long names returns no prompt`() = runTest {
        val useCase = getUseCase(testScheduler)
        stubForTvShowCreditFilter(userSettingsFlow, recentPromptsRepository, filmFactsRepository)

        coEvery {
            filmFactsRepository.getActorTvShowCredits(any())
        } returns listOf(
            stubActorTvShowCredits(showName = "foo", characterName = "some minor character name"),
            stubActorTvShowCredits(showName = "bar", characterName = "a characters long description"),
            stubActorTvShowCredits(showName = "fizz", characterName = "bodyguard of the main villain"),
            stubActorTvShowCredits(showName = "buzz", characterName = "friendly town shopkeeper person"),
            stubActorTvShowCredits(showName = "fin", characterName = "fin")
        )

        Assert.assertNull(useCase.invoke(null))
    }

    @Test
    fun `When acting credit has low votes not included in results`() = runTest {
        val useCase = getUseCase(testScheduler)
        stubForTvShowCreditFilter(userSettingsFlow, recentPromptsRepository, filmFactsRepository)

        coEvery {
            filmFactsRepository.getActorTvShowCredits(any())
        } returns listOf(
            stubActorTvShowCredits(showName = "foo", voteCount = CreditFilterConstants.MIN_VOTE_COUNT - 1),
            stubActorTvShowCredits(showName = "bar", characterName = "bar"),
            stubActorTvShowCredits(showName = "fizz", characterName = "fizz"),
            stubActorTvShowCredits(showName = "buzz", characterName = "buzz"),
            stubActorTvShowCredits(showName = "fin", characterName = "fin")
        )

        val prompt = useCase.invoke(null) as UiTextPrompt
        prompt.entries.forEach {
            Assert.assertNotEquals("foo", it.subContent)
        }
    }

    @Test
    fun `When not enough credits due to low voted credits returns no prompt`() = runTest {
        val useCase = getUseCase(testScheduler)
        stubForTvShowCreditFilter(userSettingsFlow, recentPromptsRepository, filmFactsRepository)

        coEvery {
            filmFactsRepository.getActorTvShowCredits(any())
        } returns listOf(
            stubActorTvShowCredits(showName = "foo", characterName = "foo", voteCount = CreditFilterConstants.MIN_VOTE_COUNT - 1),
            stubActorTvShowCredits(showName = "bar", characterName = "bar", voteCount = CreditFilterConstants.MIN_VOTE_COUNT - 1),
            stubActorTvShowCredits(showName = "fizz", characterName = "fizz", voteCount = CreditFilterConstants.MIN_VOTE_COUNT - 1),
            stubActorTvShowCredits(showName = "buzz", characterName = "buzz", voteCount = CreditFilterConstants.MIN_VOTE_COUNT - 1),
            stubActorTvShowCredits(showName = "fin", characterName = "fin")
        )

        Assert.assertNull(useCase.invoke(null))
    }

    @Test
    fun `When acting credit has low episode count not included in results`() = runTest {
        val useCase = getUseCase(testScheduler)
        stubForTvShowCreditFilter(userSettingsFlow, recentPromptsRepository, filmFactsRepository)

        coEvery {
            filmFactsRepository.getActorTvShowCredits(any())
        } returns listOf(
            stubActorTvShowCredits(showName = "foo", characterName = "foo", episodeCount = CreditFilterConstants.MIN_EPISODES - 1),
            stubActorTvShowCredits(showName = "bar", characterName = "bar"),
            stubActorTvShowCredits(showName = "fizz", characterName = "fizz"),
            stubActorTvShowCredits(showName = "buzz", characterName = "buzz"),
            stubActorTvShowCredits(showName = "fin", characterName = "fin")
        )

        val prompt = useCase.invoke(null) as UiTextPrompt
        prompt.entries.forEach {
            Assert.assertNotEquals("foo", it.subContent)
        }
    }

    @Test
    fun `When not enough credits due to low episode count returns no prompt`() = runTest {
        val useCase = getUseCase(testScheduler)
        stubForTvShowCreditFilter(userSettingsFlow, recentPromptsRepository, filmFactsRepository)

        coEvery {
            filmFactsRepository.getActorTvShowCredits(any())
        } returns listOf(
            stubActorTvShowCredits(showName = "foo", characterName = "foo", episodeCount = CreditFilterConstants.MIN_EPISODES - 1),
            stubActorTvShowCredits(showName = "bar", characterName = "bar", episodeCount = CreditFilterConstants.MIN_EPISODES - 1),
            stubActorTvShowCredits(showName = "fizz", characterName = "fizz", episodeCount = CreditFilterConstants.MIN_EPISODES - 1),
            stubActorTvShowCredits(showName = "buzz", characterName = "buzz", episodeCount = CreditFilterConstants.MIN_EPISODES - 1),
            stubActorTvShowCredits(showName = "fin", characterName = "fin")
        )

        Assert.assertNull(useCase.invoke(null))
    }

    // endregion

    private fun getUseCase(testScheduler: TestCoroutineScheduler) =
        GetVoiceActorTvShowRolesUseCase(
            mockk(),
            filmFactsRepository,
            recentPromptsRepository,
            userDataRepository,
            CalendarProvider(),
            StandardTestDispatcher(testScheduler)
        )
}
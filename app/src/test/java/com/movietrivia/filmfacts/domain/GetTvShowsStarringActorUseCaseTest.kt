package com.movietrivia.filmfacts.domain

import com.movietrivia.filmfacts.api.ActorTvShowCredits
import com.movietrivia.filmfacts.api.TvShowCreditEntry
import com.movietrivia.filmfacts.api.TvShowCredits
import com.movietrivia.filmfacts.model.Actor
import com.movietrivia.filmfacts.model.CalendarProvider
import com.movietrivia.filmfacts.model.FilmFactsRepository
import com.movietrivia.filmfacts.model.RecentPromptsRepository
import com.movietrivia.filmfacts.model.UiImagePrompt
import com.movietrivia.filmfacts.model.UserDataRepository
import com.movietrivia.filmfacts.model.UserSettings
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class GetTvShowsStarringActorUseCaseTest {

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
            getTvShowActors(any(), any(), any(), logTag = any())
        } returns listOf(
            Actor(0, "foo", null),
            Actor(1, "bar", null),
            Actor(2, "fizz", null),
            Actor(3, "buzz", null)
        )

        coEvery {
            filmFactsRepository.getActorTvShowCredits(any())
        } returns listOf(mockActorTvShowCredits("foo"), mockActorTvShowCredits("bar"), mockActorTvShowCredits("fizz"))

        coEvery {
            filmFactsRepository.getTvShowCredits(any())
        } returns TvShowCredits(
            0,
            listOf(
                TvShowCreditEntry(4, "foo", null, 0,"foo"),
                TvShowCreditEntry(5, "bar", null, 0, "bar"),
                TvShowCreditEntry(6, "fizz", null, 0, "fizz"),
                TvShowCreditEntry(7, "buzz", null, 0, "buzz")
            )
        )

        coEvery {
            filmFactsRepository.getImageUrl(any(), any())
        } returns "fooUrl"

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
    fun `When unable to get actors returns null`() = runTest {
        coEvery {
            getTvShowActors(any(), any(), any(), logTag = any())
        } returns emptyList()

        val useCase = getUseCase(testScheduler)

        Assert.assertNull(useCase.invoke(null))
    }

    @Test
    fun `When unable to get actor tv show credits then returns null`() = runTest {
        coEvery {
            filmFactsRepository.getActorTvShowCredits(any())
        } returns null

        val useCase = getUseCase(testScheduler)

        Assert.assertNull(useCase.invoke(null))
    }

    @Test
    fun `When actor credit attempts exceeded then returns null`() = runTest {
        coEvery {
            getTvShowActors(any(), any(), any(), logTag = any())
        } returns listOf(
            Actor(0, "foo", null),
            Actor(1, "bar", null),
            Actor(2, "fizz", null),
            Actor(3, "buzz", null),
            Actor(4, "thing1", null),
            Actor(5, "thing2", null),
            Actor(6, "thing3", null)
        )

        coEvery {
            filmFactsRepository.getActorTvShowCredits(any())
        } returns null

        val useCase = getUseCase(testScheduler)

        Assert.assertNull(useCase.invoke(null))
    }

    @Test
    fun `When unable to get enough popular actors then returns null`() = runTest {
        coEvery {
            getTvShowActors(any(), any(), any(), logTag = any())
        } returnsMany listOf(
            listOf(
                Actor(0, "foo", null),
                Actor(1, "bar", null),
                Actor(2, "fizz", null),
                Actor(3, "buzz", null)
            ),
            emptyList()
        )

        coEvery {
            filmFactsRepository.getActorTvShowCredits(any())
        } returns null

        val useCase = getUseCase(testScheduler)

        Assert.assertNull(useCase.invoke(null))
    }

    @Test
    fun `When unable to get enough filler actors then returns null`() = runTest {
        coEvery {
            getTvShowActors(any(), any(), any(), logTag = any())
        } returns listOf(
            Actor(0, "foo", null)
        )

        val useCase = getUseCase(testScheduler)

        Assert.assertNull(useCase.invoke(null))
    }

    @Test
    fun `When unable to get enough filler tv shows then returns null`() = runTest {
        coEvery {
            filmFactsRepository.getActorTvShowCredits(any())
        } returns listOf(mockk(relaxed = true))

        val useCase = getUseCase(testScheduler)

        Assert.assertNull(useCase.invoke(null))
    }

    @Test
    fun `When unable to get enough credits due to multiple credits on same show then returns null`() = runTest {
        coEvery {
            filmFactsRepository.getActorTvShowCredits(any())
        } returns listOf(
            ActorTvShowCredits(
                0,
                "foo",
                "Foo Show",
                emptyList(),
                0,
                0,
                "",
                "",
                ""
            ),
            ActorTvShowCredits(
                1,
                "bar",
                "Foo Show",
                emptyList(),
                0,
                0,
                "",
                "",
                ""
            ),
            ActorTvShowCredits(
                2,
                "fizz",
                "Foo Show",
                emptyList(),
                0,
                0,
                "",
                "",
                ""
            ),
            ActorTvShowCredits(
                3,
                "buzz",
                "Foo Show",
                emptyList(),
                0,
                0,
                "",
                "",
                ""
            )
        )

        val useCase = getUseCase(testScheduler)

        Assert.assertNull(useCase.invoke(null))
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
    fun `When given valid data returns valid prompt`() = runTest {
        val useCase = getUseCase(testScheduler)

        val prompt = useCase.invoke(null)

        Assert.assertTrue(prompt is UiImagePrompt)
    }

    private fun getUseCase(testScheduler: TestCoroutineScheduler) =
        GetTvShowsStarringActorUseCase(
            mockk(),
            filmFactsRepository,
            recentPromptsRepository,
            userDataRepository,
            CalendarProvider(),
            StandardTestDispatcher(testScheduler)
        )

    private fun mockActorTvShowCredits(name: String): ActorTvShowCredits {
        val mock: ActorTvShowCredits = mockk(relaxed = true)
        every {
            mock.posterPath
        } returns "fooPath"

        every {
            mock.showName
        } returns name

        return mock
    }
}
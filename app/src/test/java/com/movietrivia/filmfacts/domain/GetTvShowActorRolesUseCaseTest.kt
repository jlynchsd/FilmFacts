package com.movietrivia.filmfacts.domain

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
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.emptyFlow
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
class GetTvShowActorRolesUseCaseTest {
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

        mockkStatic("com.movietrivia.filmfacts.domain.GetTvShowActorsUtilKt")
        mockkStatic(::getTvShowActors)
        coEvery {
            getTvShowActors(any(), any(), any(), logTag = any())
        } returns listOf(mockk(relaxed = true), mockk(relaxed = true), mockk(relaxed = true), mockk(relaxed = true))

        coEvery {
            filmFactsRepository.getActorTvShowCredits(any())
        } returnsMany stubActorTvShowCredits

        // NOTE: Only need to mockkStatic once per file, subsequent ones will wipe earlier mocks
//        mockkStatic(::getActorTvShowCredits)
        coEvery {
            getActorTvShowCredits(any(), any(), any(), any(), any(), any(), logTag = any())
        } returnsMany listOf(
            Triple(
                mockk(relaxed = true),
                "",
                listOf(stubActorTvShowCredits(characterName = "foo"), stubActorTvShowCredits(characterName = "foo"))
            ),
            Triple(
                mockk(relaxed = true),
                "",
                listOf(stubActorTvShowCredits(characterName = "bar"), stubActorTvShowCredits(characterName = "bar"))
            )
        )

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
    fun `When unable to get user setting then returns null`() = runTest {
        every {
            userDataRepository.tvShowUserSettings
        } returns emptyFlow()

        val useCase = getUseCase(testScheduler)

        Assert.assertNull(useCase.invoke(null))
    }

    @Test
    fun `When unable to get tv show actors then returns null`() = runTest {
        coEvery {
            getTvShowActors(any(), any(), any(), logTag = any())
        } returns emptyList()

        val useCase = getUseCase(testScheduler)

        Assert.assertNull(useCase.invoke(null))
    }

    @Test
    fun `When unable to get tv show credits then returns null`() = runTest {
        coEvery {
            getActorTvShowCredits(any(), any(), any(), any(), any(), any(), logTag = any())
        } returns null

        val useCase = getUseCase(testScheduler)

        Assert.assertNull(useCase.invoke(null))
    }

    @Test
    fun `When unable to get other tv show credits then returns null`() = runTest {
        coEvery {
            getActorTvShowCredits(any(), any(), any(), any(), any(), any(), logTag = any())
        } returnsMany listOf(
            Triple(
                mockk(relaxed = true),
                "",
                listOf(stubActorTvShowCredits(characterName = "foo"), stubActorTvShowCredits(characterName = "foo"))
            ),
            null
        )

        val useCase = getUseCase(testScheduler)

        Assert.assertNull(useCase.invoke(null))
    }

    @Test
    fun `When unable to load images then returns null`() = runTest {
        mockkStatic(::preloadImages)
        coEvery {
            preloadImages(any(), *anyVararg())
        } returns false

        val useCase = getUseCase(testScheduler)

        Assert.assertNull(useCase.invoke(null))
    }

    @Test
    fun `When given valid data picks actor and populates fields`() = runTest {
        val useCase = getUseCase(testScheduler)

        val prompt = useCase.invoke(null) as UiTextPrompt

        prompt.entries.filter { it.isAnswer }.forEach {
            Assert.assertEquals("foo", it.topContent)
        }
    }

    private fun getUseCase(testScheduler: TestCoroutineScheduler) =
        GetTvShowActorRolesUseCase(
            mockk(),
            filmFactsRepository,
            recentPromptsRepository,
            userDataRepository,
            CalendarProvider(),
            StandardTestDispatcher(testScheduler)
        )
}
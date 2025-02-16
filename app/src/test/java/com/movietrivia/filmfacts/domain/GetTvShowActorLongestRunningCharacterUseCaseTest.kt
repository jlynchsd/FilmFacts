package com.movietrivia.filmfacts.domain

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.movietrivia.filmfacts.api.PersonDetails
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
class GetTvShowActorLongestRunningCharacterUseCaseTest {

    private lateinit var filmFactsRepository: FilmFactsRepository
    private lateinit var recentPromptsRepository: RecentPromptsRepository
    private lateinit var userDataRepository: UserDataRepository

    private lateinit var userSettingsFlow: MutableSharedFlow<UserSettings>

    @Before
    fun setup () {
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
        } returns listOf(mockk(relaxed = true), mockk(relaxed = true), mockk(relaxed = true), mockk(relaxed = true))

        coEvery {
            filmFactsRepository.getActorTvShowCredits(any())
        } returnsMany stubActorTvShowCredits

        val mockActorDetails: PersonDetails = mockk(relaxed = true)
        every {
            mockActorDetails.profilePath
        } returns "fooPath"
        coEvery {
            filmFactsRepository.getActorDetails(any())
        } returns mockActorDetails

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
    fun `When unable to get minimum number of actors then returns null`() = runTest {
        coEvery {
            getTvShowActors(any(), any(), any(), logTag = any())
        } returns emptyList()

        val useCase = getUseCase(testScheduler)

        Assert.assertNull(useCase.invoke(null))
    }

    @Test
    fun `When unable to get enough actors credits to pass filters then returns null`() = runTest {
        coEvery {
            filmFactsRepository.getActorTvShowCredits(any())
        } returnsMany listOf(
            listOf(stubActorTvShowCredits(characterName = "")),
            listOf(stubActorTvShowCredits(characterName = "")),
            listOf(stubActorTvShowCredits(characterName = "")),
            listOf(stubActorTvShowCredits(characterName = ""))
        )

        val useCase = getUseCase(testScheduler)

        Assert.assertNull(useCase.invoke(null))
    }

    @Test
    fun `When unable to get enough actors credits with unique episode counts then returns null`() = runTest {
        coEvery {
            filmFactsRepository.getActorTvShowCredits(any())
        } returnsMany listOf(
            listOf(stubActorTvShowCredits()),
            listOf(stubActorTvShowCredits()),
            listOf(stubActorTvShowCredits()),
            listOf(stubActorTvShowCredits())
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
    fun `When actor details are null then returns null`() = runTest {
        coEvery {
            filmFactsRepository.getActorDetails(any())
        } returns null

        val useCase = getUseCase(testScheduler)

        Assert.assertNull(useCase.invoke(null))
    }

    @Test
    fun `When picking longest running character then picks first character with valid name`() = runTest {
        val tvShowCredits = listOf(
            stubActorTvShowCredits(characterName = "foo/foo1", episodeCount = 100),
            stubActorTvShowCredits(characterName = "bar / bar1", episodeCount = 99),
            stubActorTvShowCredits(characterName = "  (voice)  ", episodeCount = 98),
            stubActorTvShowCredits(characterName = "foo #1", episodeCount = 97),
            stubActorTvShowCredits(characterName = "Self", episodeCount = 96),
            stubActorTvShowCredits(showName = "", episodeCount = 95),
            stubActorTvShowCredits(voteCount = 0, episodeCount = 94),
            stubActorTvShowCredits(episodeCount = 1),
            stubActorTvShowCredits(language = "", episodeCount = 93),
            stubActorTvShowCredits(characterName = "buzz", showName = "buzz", episodeCount = 20)
        )

        coEvery {
            filmFactsRepository.getActorTvShowCredits(any())
        } returnsMany listOf(
            tvShowCredits,
        ) + stubActorTvShowCredits

        val useCase = getUseCase(testScheduler)

        val prompt = useCase.invoke(null) as UiImagePrompt
        Assert.assertEquals("buzz", prompt.entries.first { it.isAnswer }.title)
    }

    @Test
    fun `When getting valid data then picks longest running character and populates fields`() = runTest {
        val useCase = getUseCase(testScheduler)

        val prompt = useCase.invoke(null) as UiImagePrompt

        Assert.assertEquals("other4", prompt.entries.first { it.isAnswer }.title)
        Assert.assertEquals("13 Episodes", prompt.entries.first { it.isAnswer }.data)
    }

    private fun getUseCase(testScheduler: TestCoroutineScheduler) =
        GetTvShowActorLongestRunningCharacterUseCase(
            getContext(),
            filmFactsRepository,
            recentPromptsRepository,
            userDataRepository,
            calendarProvider = CalendarProvider(),
            StandardTestDispatcher(testScheduler)
        )

    private fun getContext(): Context = ApplicationProvider.getApplicationContext()
}
package com.interviewsample.filmfacts.api

import com.movietrivia.filmfacts.api.DiscoverService
import com.movietrivia.filmfacts.model.UserSettings
import org.junit.Assert
import org.junit.Test
import java.time.Instant
import java.util.Date

class DiscoverServiceTest {

    @Test
    fun `When creating default builder adds language and page`() {
        val page = 1
        val settings = UserSettings()
        val result = DiscoverService.Builder(settings, page).build()

        Assert.assertEquals(settings.language, result[DiscoverService.Builder.LANGUAGE])
        Assert.assertEquals(page.toString(), result[DiscoverService.Builder.PAGE_KEY])
        Assert.assertEquals(2, result.keys.size)
    }

    @Test
    fun `When creating builder with user settings applies user settings`() {
        val settings = UserSettings(
            excludedFilmGenres = listOf(0,1,2),
            releasedAfterOffset = 2,
            releasedBeforeOffset = 3
        )
        val result = DiscoverService.Builder(settings, 1).build()

        Assert.assertTrue(result.keys.contains(DiscoverService.Builder.EXCLUDE_GENRES))
        Assert.assertTrue(result.keys.contains(DiscoverService.Builder.PRIMARY_RELEASE_DATE_LESS_THAN))
        Assert.assertTrue(result.keys.contains(DiscoverService.Builder.PRIMARY_RELEASE_DATE_GREATER_THAN))
    }

    @Test
    fun `When creating builder with start and end date overrides user settings`() {
        val settings = UserSettings(
            excludedFilmGenres = listOf(0,1,2),
            releasedAfterOffset = 2,
            releasedBeforeOffset = 3
        )
        val date = Date.from(Instant.ofEpochMilli(0))
        val result = DiscoverService.Builder(settings, 1).releasedInRange(date, date).build()
        val expected = "1969-12-31"

        Assert.assertEquals(expected, result[DiscoverService.Builder.PRIMARY_RELEASE_DATE_LESS_THAN])
        Assert.assertEquals(expected, result[DiscoverService.Builder.PRIMARY_RELEASE_DATE_GREATER_THAN])
    }

    @Test
    fun `When creating builder with order applies order`() {
        val order = DiscoverService.Builder.Order.VOTE_AVERAGE_DESC
        val result = DiscoverService.Builder(UserSettings(), 1).orderBy(order).build()

        Assert.assertEquals(order.key, result[DiscoverService.Builder.SORT_BY])
    }

    @Test
    fun `When creating builder with include genres applies genres`() {
        val result = DiscoverService.Builder(UserSettings(), 1).withGenres(0).build()

        Assert.assertTrue(result.keys.contains(DiscoverService.Builder.INCLUDE_GENRES))
    }

    @Test
    fun `When creating builder with exclude genres overwrites user settings`() {
        val result = DiscoverService.Builder(UserSettings(excludedFilmGenres = listOf(0,1,2)), 1).withoutGenres(3,4).build()

        Assert.assertEquals("3,4", result[DiscoverService.Builder.EXCLUDE_GENRES])
    }

    @Test
    fun `When creating builder with cast applies cast`() {
        val result = DiscoverService.Builder(UserSettings(), 1).withCast(0,1).build()

        Assert.assertEquals("0,1", result[DiscoverService.Builder.WITH_CAST])
    }

    @Test
    fun `When creating builder with votes gte applies votes`() {
        val result = DiscoverService.Builder(UserSettings(), 1).withVotesGreaterThan(9).build()

        Assert.assertEquals("9", result[DiscoverService.Builder.VOTE_COUNT_GTE])
    }

    @Test
    fun `When creating builder with release type applies release type`() {
        val releaseType = DiscoverService.Builder.ReleaseType.PHYSICAL
        val result = DiscoverService.Builder(UserSettings(), 1).withReleaseType(releaseType).build()

        Assert.assertEquals(releaseType.key.toString(), result[DiscoverService.Builder.WITH_RELEASE_TYPE])
    }
}
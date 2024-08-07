package com.movietrivia.filmfacts.domain

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.movietrivia.filmfacts.R
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class FormatOutputUtilTest {

    @Test
    fun `When revenue is less than a million uses 'thousand' suffix`() {
        Assert.assertTrue(
            formatRevenue(9001, getContext())
                .endsWith(getContext().getString(R.string.suffix_thousand))
        )
    }

    @Test
    fun `When revenue is less than a negative million uses 'thousand' suffix`() {
        Assert.assertTrue(
            formatRevenue(-5000, getContext())
                .endsWith(getContext().getString(R.string.suffix_thousand))
        )
    }

    @Test
    fun `When revenue is a million uses 'million' suffix`() {
        Assert.assertTrue(
            formatRevenue(1_000_000, getContext())
                .endsWith(getContext().getString(R.string.suffix_million))
        )
    }

    @Test
    fun `When revenue is one less than a billion uses 'million' suffix`() {
        Assert.assertTrue(
            formatRevenue(999_999_999, getContext())
                .endsWith(getContext().getString(R.string.suffix_million))
        )
    }

    @Test
    fun `When revenue is between a million and billion uses 'million' suffix`() {
        Assert.assertTrue(
            formatRevenue(3_000_000, getContext())
                .endsWith(getContext().getString(R.string.suffix_million))
        )
    }

    @Test
    fun `When revenue is between a negative million and negative billion uses 'million' suffix`() {
        Assert.assertTrue(
            formatRevenue(-99_000_000, getContext())
                .endsWith(getContext().getString(R.string.suffix_million))
        )
    }

    @Test
    fun `When revenue is at least a billion uses 'billion' suffix`() {
        Assert.assertTrue(
            formatRevenue(9_000_000_000, getContext())
                .endsWith(getContext().getString(R.string.suffix_billion))
        )
    }

    @Test
    fun `When revenue is at least a negative billion uses 'billion' suffix`() {
        Assert.assertTrue(
            formatRevenue(-1_000_000_000, getContext())
                .endsWith(getContext().getString(R.string.suffix_billion))
        )
    }

    private fun getContext() = ApplicationProvider.getApplicationContext<Context>()
}
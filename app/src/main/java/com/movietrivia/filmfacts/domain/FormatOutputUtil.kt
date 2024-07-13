package com.movietrivia.filmfacts.domain

import android.content.Context
import com.movietrivia.filmfacts.R
import java.text.NumberFormat
import java.util.*
import kotlin.math.abs

fun formatRevenue(revenue: Long, context: Context) =
    when {
        abs(revenue) < 1_000_000 -> {
            NumberFormat.getInstance(Locale.getDefault()).format(revenue) + " " + context.getString(
                R.string.suffix_thousand)
        }
        abs(revenue) in 1_000_000..999_999_999 -> {
            NumberFormat.getInstance(Locale.getDefault()).format(revenue.toFloat() / 1_000_000) + " " + context.getString(
                R.string.suffix_million)
        }
        else -> {
            NumberFormat.getInstance(Locale.getDefault()).format(revenue.toFloat() / 1_000_000_000) + " " + context.getString(
                R.string.suffix_billion)
        }
    }
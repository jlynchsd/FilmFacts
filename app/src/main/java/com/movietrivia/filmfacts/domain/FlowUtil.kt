package com.movietrivia.filmfacts.domain

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

suspend fun <T> Flow<T>.firstOrNullCatching(): T? =
    kotlin.runCatching { this.first() }.getOrNull()
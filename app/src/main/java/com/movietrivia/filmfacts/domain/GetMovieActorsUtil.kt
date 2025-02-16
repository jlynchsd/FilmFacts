package com.movietrivia.filmfacts.domain

import android.annotation.SuppressLint
import com.movietrivia.filmfacts.api.ActorMovieCredits
import com.movietrivia.filmfacts.api.DiscoverService
import com.movietrivia.filmfacts.model.Actor
import com.movietrivia.filmfacts.model.FilmFactsRepository
import com.movietrivia.filmfacts.model.RecentPromptsRepository
import java.lang.Integer.min
import java.text.SimpleDateFormat
import java.util.*


suspend fun getMovieActors(
    filmFactsRepository: FilmFactsRepository,
    recentPromptsRepository: RecentPromptsRepository,
    requiredGenres: List<Int>? = null
): List<Actor> {
    val seedMovies = getSeedMovies(filmFactsRepository, requiredGenres)?.results?.toMutableList()
    val result = mutableSetOf<Actor>()
    if (!seedMovies.isNullOrEmpty()) {
        repeat (min(4, seedMovies.size)) {
            val currentSeedMovie = seedMovies.random()
            seedMovies.remove(currentSeedMovie)
            val movieCase = filmFactsRepository.getMovieCredits(currentSeedMovie.id)
            val movieCast = movieCase?.cast?.filter { !recentPromptsRepository.isRecentActor(it.id) && it.order <= 10 }
            if (!movieCast.isNullOrEmpty()) {
                val currentCast = movieCast.toMutableList()
                repeat (min(5, currentCast.size)) {
                    val currentActor = currentCast.random()
                    currentCast.remove(currentActor)
                    result.add(Actor(currentActor.id, currentActor.name, currentActor.gender))
                }
            }
        }
    }

    return result.toList()
}

private suspend fun getSeedMovies(
    filmFactsRepository: FilmFactsRepository,
    requiredGenres: List<Int>?
) =
    when (MovieStrategies.values().random()) {
        MovieStrategies.POPULARITY -> {
            filmFactsRepository.getMovies(
                movieOrder = DiscoverService.Builder.MovieOrder.POPULARITY_DESC,
                includeGenres = requiredGenres
            )
        }

        MovieStrategies.VOTES -> {
            filmFactsRepository.getMovies(
                movieOrder = DiscoverService.Builder.MovieOrder.VOTE_AVERAGE_DESC,
                includeGenres = requiredGenres,
                minimumVotes = 1000
            )
        }

        MovieStrategies.REVENUE -> {
            filmFactsRepository.getMovies(
                movieOrder = DiscoverService.Builder.MovieOrder.REVENUE_DESC,
                includeGenres = requiredGenres
            )
        }
    }

suspend fun getActorMovieCredits(
    filmFactsRepository: FilmFactsRepository,
    recentPromptsRepository: RecentPromptsRepository,
    popularActors: List<Actor>,
    creditCount: IntRange,
    creditFilter: (ActorMovieCredits) -> Boolean,
    excludeActor: Actor? = null
): Triple<Actor, String, List<ActorMovieCredits>>? {
    val credits = mutableListOf<ActorMovieCredits>()

    val filteredActors = if (excludeActor != null) {
        popularActors.filter {
            !recentPromptsRepository.isRecentActor(it.id) &&
                    it.id != excludeActor.id &&
                    it.gender == excludeActor.gender
        }.toMutableList()
    } else {
        popularActors.filter { !recentPromptsRepository.isRecentActor(it.id) }.toMutableList()
    }

    while (filteredActors.isNotEmpty()) {
        val remainingCount = (creditCount.first - credits.size..creditCount.last - credits.size)
        val currentActor = filteredActors.random()
        filteredActors.remove(currentActor)
        val actorDetails = if (excludeActor == null) {
            filmFactsRepository.getActorDetails(currentActor.id)
        } else {
            null
        }
        if (excludeActor == null && (actorDetails == null || actorDetails.profilePath.isBlank())) {
            continue
        }
        val actorCredits = filmFactsRepository.getActorMovieCredits(currentActor.id)?.map { it.copy(characterName = filterNames(it.characterName)) }
            ?.filter(creditFilter)?.distinctBy { it.characterName }?.toMutableList()

        val hasCreditRequirements = if (excludeActor == null) {
            actorCredits != null && actorCredits.size >= remainingCount.first
        } else {
            !actorCredits.isNullOrEmpty()
        }

        if (actorCredits != null && hasCreditRequirements) {
            val start = remainingCount.first.coerceAtMost(actorCredits.size)
            val end = actorCredits.size.coerceAtMost(remainingCount.last)
            repeat((start..end).random()) {
                actorCredits.random().let {
                    credits.add(it)
                    actorCredits.remove(it)
                }
            }

            if (excludeActor == null) {
                return Triple(currentActor, actorDetails?.profilePath ?: "", credits)
            } else if (credits.size >= creditCount.first) {
                return Triple(currentActor,"", credits)
            }
        }
    }

    return null
}

@SuppressLint("SimpleDateFormat")
fun dateWithinRange(date: String, releasedAfter: Int?, releasedBefore: Int?): Boolean {
    if (date.isBlank()) {
        return false
    }

    val releaseDate = runCatching { SimpleDateFormat("yyyy-MM-dd").parse(date) }.getOrNull()?.time ?: return false

    val startDate = if (releasedBefore != null) {
        with (Calendar.getInstance()) {
            add(Calendar.YEAR, -releasedBefore)
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.MONTH, 0)
            time.time
        }
    } else {
        null
    }

    val endDate = if (releasedAfter != null) {
        with (Calendar.getInstance()) {
            add(Calendar.YEAR, -releasedAfter)
            set(Calendar.DAY_OF_MONTH, 31)
            set(Calendar.MONTH, 11)
            time.time
        }
    } else {
        null
    }

    var withinRange = true
    if (startDate != null) {
        withinRange = releaseDate > startDate
    }
    if (withinRange && endDate != null) {
        withinRange = releaseDate < endDate
    }

    return withinRange
}

fun <T> List<T>.containsAny(other: List<T>): Boolean {
    other.forEach {
        if (contains(it)) {
            return true
        }
    }

    return false
}

private fun filterNames(name: String): String {
    var filteredName = name
    while (filteredName.contains("(")) {
        val startIndex = filteredName.indexOf("(")
        val endIndex = filteredName.indexOf(")")
        filteredName = if (endIndex > startIndex) {
            filteredName.removeRange((startIndex .. endIndex))
        } else {
            filteredName.substring(0, startIndex)
        }

    }
    return filteredName
}

private enum class MovieStrategies {
    POPULARITY,
    VOTES,
    REVENUE
}
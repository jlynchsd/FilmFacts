package com.movietrivia.filmfacts.domain

import com.movietrivia.filmfacts.api.ActorTvShowCredits
import com.movietrivia.filmfacts.api.DiscoverService
import com.movietrivia.filmfacts.api.DiscoverTvShow
import com.movietrivia.filmfacts.api.Logger
import com.movietrivia.filmfacts.api.TvGenre
import com.movietrivia.filmfacts.api.TvShowCreditEntry
import com.movietrivia.filmfacts.model.Actor
import com.movietrivia.filmfacts.model.FilmFactsRepository
import com.movietrivia.filmfacts.model.RecentPromptsRepository
import java.lang.Integer.min
import java.util.Date


suspend fun getTvShowActors(
    filmFactsRepository: FilmFactsRepository,
    recentPromptsRepository: RecentPromptsRepository,
    dateRange: Pair<Date?, Date?>? = null,
    requiredGenres: List<Int>? = null,
    targetActors: Int = 20,
    filter: (TvShowCreditEntry) -> Boolean = { true },
    logTag: String
): List<Actor> {
    val seedTvShows = getSeedTvShows(filmFactsRepository, recentPromptsRepository, dateRange, requiredGenres, logTag = logTag)?.toMutableList()
    Logger.debug(logTag, "Seed Tv Shows: ${seedTvShows?.size}")
    val result = mutableSetOf<Actor>()
    while (!seedTvShows.isNullOrEmpty() && result.size < targetActors) {
        val currentSeedTvShow = seedTvShows.random()
        seedTvShows.remove(currentSeedTvShow)
        val tvShowCase = filmFactsRepository.getTvShowCredits(currentSeedTvShow.id)
        Logger.debug(logTag, "Seed Tv Shows Id: ${currentSeedTvShow.id}")
        Logger.debug(logTag, "Seed Tv Shows Cast: ${tvShowCase?.cast?.size}")
        val tvShowCast = tvShowCase?.cast?.filter { !recentPromptsRepository.isRecentActor(it.id) && it.order <= 10 }
        Logger.debug(logTag, "Seed Tv Shows Filtered Cast: ${tvShowCast?.size}")
        if (!tvShowCast.isNullOrEmpty()) {
            val currentCast = tvShowCast.filter(filter).toMutableList()
            repeat (min(5, currentCast.size)) {
                val currentActor = currentCast.random()
                currentCast.remove(currentActor)
                result.add(Actor(currentActor.id, currentActor.name, currentActor.gender))
            }
        }
    }

    return result.toList()
}

private suspend fun getSeedTvShows(
    filmFactsRepository: FilmFactsRepository,
    recentPromptsRepository: RecentPromptsRepository,
    dateRange: Pair<Date?, Date?>?,
    requiredGenres: List<Int>?,
    forceStrategy: TvShowStrategies? = null,
    logTag: String
): List<DiscoverTvShow>? {
    val strategy = TvShowStrategies.values().random()
    return when (forceStrategy ?: strategy) {
        TvShowStrategies.POPULARITY -> {
            Logger.debug(logTag, "Seed Tv Show by Popularity")
            getTvShows(
                filmFactsRepository,
                recentPromptsRepository,
                dateRange,
                DiscoverService.Builder.TvShowOrder.POPULARITY_DESC,
                requiredGenres,
                false,
                minimumVotes = minimumVoteCountByGenre(requiredGenres),
                logTag = logTag
            )
        }

        TvShowStrategies.VOTES -> {
            Logger.debug(logTag, "Seed Tv Show by Votes")
            getTvShows(
                filmFactsRepository,
                recentPromptsRepository,
                dateRange,
                DiscoverService.Builder.TvShowOrder.VOTE_AVERAGE_DESC,
                requiredGenres,
                false,
                minimumVotes = minimumVoteCountByGenre(requiredGenres),
                logTag = logTag
            )
        }
    }
}

private fun minimumVoteCountByGenre(genres: List<Int>?): Int {
    val mapping = mapOf(
        TvGenre.ACTION_AND_ADVENTURE.key to 20,
        TvGenre.ANIMATION.key to 20,
        TvGenre.COMEDY.key to 20,
        TvGenre.CRIME.key to 20,
        TvGenre.FAMILY.key to 20,
        TvGenre.REALITY.key to 20,
        TvGenre.SCI_FI_AND_FANTASY.key to 20,
        TvGenre.WAR_AND_POLITICS.key to 5,
        TvGenre.WESTERN.key to 1
    )

    return if (genres != null) {
        genres.minOfOrNull {
            mapping[it] ?: 20
        } ?: 20
    } else {
        20
    }
}

suspend fun getActorTvShowCredits(
    filmFactsRepository: FilmFactsRepository,
    recentPromptsRepository: RecentPromptsRepository,
    popularActors: List<Actor>,
    creditCount: IntRange,
    creditFilter: (ActorTvShowCredits) -> Boolean,
    excludeActor: Actor? = null,
    logTag: String
): Triple<Actor, String, List<ActorTvShowCredits>>? {
    val credits = mutableListOf<ActorTvShowCredits>()

    val filteredActors = if (excludeActor != null) {
        popularActors.filter {
            !recentPromptsRepository.isRecentActor(it.id) &&
                    it.id != excludeActor.id &&
                    it.gender == excludeActor.gender
        }.toMutableList()
    } else {
        popularActors.filter { !recentPromptsRepository.isRecentActor(it.id) }.toMutableList()
    }

    Logger.debug(logTag, "Popular Actors: ${popularActors.size}, Filtered Actors: ${filteredActors.size}")

    while (filteredActors.isNotEmpty()) {
        val remainingCount = (creditCount.first - credits.size..creditCount.last - credits.size)
        val currentActor = filteredActors.random()
        filteredActors.remove(currentActor)
        val actorDetails = if (excludeActor == null) {
            filmFactsRepository.getActorDetails(currentActor.id)
        } else {
            null
        }
        Logger.debug(logTag, "$actorDetails")
        if (excludeActor == null && (actorDetails == null || actorDetails.profilePath.isBlank())) {
            Logger.debug(logTag, "Skipped $actorDetails")
            continue
        }
        val actorCredits = filmFactsRepository.getActorTvShowCredits(currentActor.id)?.map { it.copy(characterName = filterNames(it.characterName)) }
            ?.filter(creditFilter)?.distinctBy { it.characterName }?.toMutableList()
        Logger.debug(logTag, "Actor Credits: ${actorCredits?.size}")

        val hasCreditRequirements = if (excludeActor == null) {
            actorCredits != null && actorCredits.size >= remainingCount.first
        } else {
            !actorCredits.isNullOrEmpty()
        }

        Logger.debug(logTag, "Has Credit Requirements: $hasCreditRequirements")

        if (actorCredits != null && hasCreditRequirements) {
            val start = remainingCount.first.coerceAtMost(actorCredits.size)
            val end = actorCredits.size.coerceAtMost(remainingCount.last)
            repeat((start..end).random()) {
                actorCredits.random().let {
                    credits.add(it)
                    actorCredits.remove(it)
                }
            }

            Logger.debug(logTag, "Got actor tv show credits")
            if (excludeActor == null) {
                return Triple(currentActor, actorDetails?.profilePath ?: "", credits)
            } else if (credits.size >= creditCount.first) {
                return Triple(currentActor,"", credits)
            }
        }
    }

    Logger.debug(logTag, "Failed to get actor tv show credits")
    return null
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

private enum class TvShowStrategies {
    POPULARITY,
    VOTES
}
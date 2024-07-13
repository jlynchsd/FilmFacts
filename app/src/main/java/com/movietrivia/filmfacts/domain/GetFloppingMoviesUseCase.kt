package com.movietrivia.filmfacts.domain

/**
 * Disabling this use case because flopping movies are relatively rare, and so it consumes lots of bandwidth
 * trying to find enough flops to make a prompt which taxes the backend and slows down the app.
 */

//import android.content.Context
//import com.movietrivia.filmfacts.R
//import com.movietrivia.filmfacts.api.DiscoverService
//import com.movietrivia.filmfacts.api.MovieDetails
//import com.movietrivia.filmfacts.model.*
//import kotlinx.coroutines.*
//import kotlinx.coroutines.flow.firstOrNull
//import kotlin.math.min
//
//
//class GetFloppingMoviesUseCase(
//    private val applicationContext: Context,
//    private val filmFactsRepository: FilmFactsRepository,
//    private val recentPromptsRepository: RecentPromptsRepository,
//    private val userDataRepository: UserDataRepository,
//    private val dispatcher: CoroutineDispatcher = Dispatchers.IO
//) : UseCase {
//
//    override suspend fun invoke(includeGenres: List<Int>?) =
//        withContext(dispatcher) {
//            getPrompt(includeGenres)
//        }
//
//    private suspend fun getPrompt(includeGenres: List<Int>?): UiPrompt? {
//        val userSettings = userDataRepository.userSettings.firstOrNull() ?: return null
//        var resultPage = recentPromptsRepository.flopPage
//        val dateRange = getMovieDateRange(userSettings, DateStrategies.MAX)
//        val minimumVotes = 300
//        var movieResponse = filmFactsRepository.getMovies(
//            dateRange = dateRange,
//            order = DiscoverService.Builder.Order.REVENUE_ASC,
//            releaseType = DiscoverService.Builder.ReleaseType.THEATRICAL,
//            includeGenres = includeGenres,
//            minimumVotes = minimumVotes,
//            page = resultPage
//        )
//        var movies = movieResponse?.results?.toMutableList()
//
//        val filteredMovies = mutableListOf<MovieDetails>()
//        val targetSize = 4
//
//        while (resultPage < (movieResponse?.totalPageCount ?: 0)) {
//            if (movies != null && filteredMovies.size < targetSize) {
//                filteredMovies.addAll(getFloppingMovies(movies, targetSize - filteredMovies.size))
//            }
//
//            if (filteredMovies.size >= targetSize) {
//                break
//            } else {
//                recentPromptsRepository.flopPage = ++resultPage
//                if (resultPage > (movieResponse?.totalPageCount ?: 0)) {
//                    break
//                }
//                movieResponse = filmFactsRepository.getMovies(
//                    dateRange = dateRange,
//                    order = DiscoverService.Builder.Order.REVENUE_ASC,
//                    releaseType = DiscoverService.Builder.ReleaseType.THEATRICAL,
//                    includeGenres = includeGenres,
//                    minimumVotes = minimumVotes,
//                    page = resultPage
//                )
//                movies = movieResponse?.results?.toMutableList()
//            }
//        }
//
//        if (filteredMovies.size >= targetSize) {
//            filteredMovies.sortByDescending { it.budget - it.revenue }
//            val uiImageEntries = filteredMovies.mapIndexed { index, filteredMovie ->
//                UiImageEntry(
//                    filmFactsRepository.getImageUrl(filteredMovie.posterPath, ImageType.POSTER) ?: "",
//                    index == 0,
//                    data = formatRevenue(filteredMovie.revenue - filteredMovie.budget.toLong(), applicationContext)
//                )
//            }.shuffled()
//
//            val success = !uiImageEntries.map {
//                preloadImageAsync(applicationContext, it.imagePath)
//            }.awaitAll().contains(false)
//
//            if (success) {
//                return UiImagePrompt(
//                    uiImageEntries,
//                    R.string.biggest_flop_movie_title
//                )
//            }
//        }
//
//        return null
//    }
//
//    private suspend fun getFloppingMovies(movies: List<DiscoverMovie>, count: Int): List<MovieDetails> {
//        val remainingMovies = movies.filter { !recentPromptsRepository.isRecentFlop(it.id) }.toMutableList()
//        val filteredMovies = mutableListOf<MovieDetails>()
//        while (filteredMovies.size < count && remainingMovies.size > (count - filteredMovies.size)) {
//            val tempList = mutableListOf<DiscoverMovie>()
//            repeat(min(count - filteredMovies.size, remainingMovies.size)) {
//                remainingMovies.random().let { randomMovie ->
//                    remainingMovies.remove(randomMovie)
//                    tempList.add(randomMovie)
//                }
//            }
//            val tempMovieEntries = coroutineScope {
//                tempList.map {
//                    async {
//                        filmFactsRepository.getMovieDetails(it.id)
//                    }
//                }.awaitAll().filterNotNull()
//            }
//            tempMovieEntries.forEach {
//                recentPromptsRepository.addFlop(it.id, wasFlop(it))
//            }
//
//            filteredMovies.addAll(tempMovieEntries.filter { wasFlop(it) })
//        }
//
//        return filteredMovies
//    }
//
//    private fun wasFlop(movieDetails: MovieDetails) =
//        movieDetails.budget > 0 && movieDetails.revenue > 0 && movieDetails.budget > movieDetails.revenue
//}
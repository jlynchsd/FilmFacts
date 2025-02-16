package com.movietrivia.filmfacts.ui.compose

import android.content.Intent
import android.graphics.drawable.Drawable
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.bumptech.glide.integration.compose.RequestBuilderTransform
import com.movietrivia.filmfacts.AuthenticationActivity
import com.movietrivia.filmfacts.R
import com.movietrivia.filmfacts.model.PromptState
import com.movietrivia.filmfacts.model.UiGenre
import com.movietrivia.filmfacts.model.UiImagePrompt
import com.movietrivia.filmfacts.model.UiTextPrompt
import com.movietrivia.filmfacts.ui.orderedFilmGenres
import com.movietrivia.filmfacts.ui.orderedTvGenres
import com.movietrivia.filmfacts.viewmodel.AuthenticationViewModel
import com.movietrivia.filmfacts.viewmodel.FilmFactsViewModel
import com.movietrivia.filmfacts.viewmodel.TriviaSessionsViewModel
import com.movietrivia.filmfacts.viewmodel.UiStateViewModel

private const val SPLASH_SCREEN = "SPLASH_SCREEN"
private const val MOVIE_SCREEN = "MOVIE_SCREEN"
private const val TV_SHOW_SCREEN = "TV_SHOW_SCREEN"
private const val TRIVIA_SCREEN = "TRIVIA_SCREEN"
private const val SETTINGS_SCREEN = "SETTINGS_SCREEN"
private const val PROGRESS_SCREEN = "PROGRESS_SCREEN"
private const val ABOUT_SCREEN = "ABOUT_SCREEN"
private const val ERROR_SCREEN = "ERROR_SCREEN"

@Composable
fun ScreenNavigation(
    filmFactsViewModel: FilmFactsViewModel,
    uiStateViewModel: UiStateViewModel,
    triviaSessionsViewModel: TriviaSessionsViewModel,
    authenticationViewModel: AuthenticationViewModel,
    imageContent: @Composable (
        modifier: Modifier,
        contentDescription: String,
        url: String,
        builder: RequestBuilderTransform<Drawable>
    ) -> Unit
) {
    val navController = rememberNavController()
    NavHost(navController = navController, SPLASH_SCREEN) {
        composable(SPLASH_SCREEN) {
            SplashScreen {
                navController.navigate(MOVIE_SCREEN) {
                    popUpTo(SPLASH_SCREEN) {
                        inclusive = true
                    }
                }
            }
        }

        composable(MOVIE_SCREEN) {
            Screen(navController, filmFactsViewModel, title = stringResource(id = R.string.movie_screen_title)) {
                GenreScreen(filmFactsViewModel, uiStateViewModel, ::mapMovies, imageContent) {
                    when (it) {
                        is GenreScreenAction.StartGenre -> {
                            filmFactsViewModel.requestGenre(it.genreId)
                            triviaSessionsViewModel.startTriviaSession()
                            filmFactsViewModel.loadPrompts(7)
                            navController.navigate(TRIVIA_SCREEN)
                        }
                    }
                }
            }
        }

        composable(TV_SHOW_SCREEN) {
            Screen(navController, filmFactsViewModel, title = stringResource(id = R.string.tv_show_screen_title)) {
                GenreScreen(filmFactsViewModel, uiStateViewModel, ::mapTvShows, imageContent) {
                    when (it) {
                        is GenreScreenAction.StartGenre -> {
                            filmFactsViewModel.requestGenre(it.genreId)
                            triviaSessionsViewModel.startTriviaSession()
                            filmFactsViewModel.loadPrompts(7)
                            navController.navigate(TRIVIA_SCREEN)
                        }
                    }
                }
            }
        }

        composable(TRIVIA_SCREEN) {
            TriviaScreenContent(
                filmFactsViewModel = filmFactsViewModel,
                triviaSessionsViewModel = triviaSessionsViewModel,
                navController = navController,
                imageContent = imageContent
            )
        }

        composable(SETTINGS_SCREEN) {
            Screen(navController, filmFactsViewModel, title = stringResource(id = R.string.settings_page_title)) {
                SettingsScreenContent(
                    filmFactsViewModel = filmFactsViewModel,
                    authenticationViewModel = authenticationViewModel,
                    imageContent = imageContent
                )
            }
        }

        composable(PROGRESS_SCREEN) {
            Screen(navController, filmFactsViewModel, title = stringResource(id = R.string.user_progress_title)) {
                UserProgressScreen(
                    filmFactsViewModel
                )
            }
        }

        composable(ABOUT_SCREEN) {
            Screen(navController, filmFactsViewModel, title = stringResource(id = R.string.about_page_title)) {
                AboutScreen()
            }
        }

        composable(ERROR_SCREEN) {
            Screen(navController, filmFactsViewModel, title = stringResource(id = R.string.error_page_title)) {
                ErrorScreen {
                    when (filmFactsViewModel.promptGroup) {
                        FilmFactsViewModel.PromptGroup.MOVIES -> {
                            navController.navigate(MOVIE_SCREEN) {
                                popUpTo(MOVIE_SCREEN) {
                                    inclusive = true
                                }
                            }
                        }
                        FilmFactsViewModel.PromptGroup.TV_SHOWS -> {
                            navController.navigate(TV_SHOW_SCREEN) {
                                popUpTo(TV_SHOW_SCREEN) {
                                    inclusive = true
                                }
                            }
                        }
                    }

                }
            }
        }
    }
}

private fun mapMovies(genre: UiGenre): Pair<Int, Int>{
    val genreIndex = orderedFilmGenres.mapIndexed {
            index, movieGenre ->  if (movieGenre.key == genre.genreId) {
        index
    } else {
        null
    }
    }.filterNotNull().first()

    return Pair(genreIndex, R.array.film_genres)
}

private fun mapTvShows(genre: UiGenre): Pair<Int, Int>{
    val genreIndex = orderedTvGenres.mapIndexed {
            index, tvShowGenre ->  if (tvShowGenre.key == genre.genreId) {
        index
    } else {
        null
    }
    }.filterNotNull().first()

    return Pair(genreIndex, R.array.tv_genres)
}

@Composable
private fun Screen(navController: NavController, filmFactsViewModel: FilmFactsViewModel, title: String, content: @Composable () -> Unit) {
    val headerColor = MaterialTheme.colorScheme.primary
    val headerTextColor = MaterialTheme.colorScheme.onPrimary
    val navBarColor = MaterialTheme.colorScheme.secondary
    LayoutSelector(
        landscape = {
            Column(Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(color = headerColor)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth(.92f)
                            .padding(8.dp)
                    ) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.headlineLarge,
                            color = headerTextColor
                        )
                    }
                }

                Row(modifier = Modifier.fillMaxSize()) {
                    Column(modifier = Modifier.weight(.92f)) {
                        content()
                    }

                    Column(
                        verticalArrangement = Arrangement.SpaceEvenly,
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .fillMaxSize()
                            .weight(.08f)
                            .background(color = navBarColor)
                    ) {
                        NavigationBarContent(navController, filmFactsViewModel)
                    }
                }
            }
        },
        portrait = {
            Column(modifier = Modifier.fillMaxSize()) {
                Column(modifier = Modifier.weight(.92f)) {
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(color = headerColor)
                            .padding(8.dp)
                    ) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.headlineLarge,
                            color = headerTextColor
                        )
                    }
                    content()
                }

                Row(
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(.08f)
                        .background(color = navBarColor)
                ) {
                    NavigationBarContent(navController, filmFactsViewModel)
                }
            }
        }
    )
}

@Composable
private fun NavigationBarContent(navController: NavController, filmFactsViewModel: FilmFactsViewModel) {
    val unlockedAchievements by filmFactsViewModel.unlockedAchievements.collectAsStateWithLifecycle()
    val movieUserSettings by filmFactsViewModel.movieUserSettings.collectAsStateWithLifecycle()
    val tvShowUserSettings by filmFactsViewModel.tvShowUserSettings.collectAsStateWithLifecycle()
    NavigationButton(
        onClick = {
            filmFactsViewModel.setActivePromptGroup(movieUserSettings, FilmFactsViewModel.PromptGroup.MOVIES)
            navController.navigate(MOVIE_SCREEN)
        }
    ) {
        Icon(
            painterResource(R.drawable.film_reel_48px),
            contentDescription = stringResource(id = R.string.nav_bar_movie_button)
        )
    }

    NavigationButton(
        onClick = {
            filmFactsViewModel.setActivePromptGroup(tvShowUserSettings, FilmFactsViewModel.PromptGroup.TV_SHOWS)
            navController.navigate(TV_SHOW_SCREEN)
        }
    ) {
        Icon(
            painterResource(R.drawable.television_48px),
            contentDescription = stringResource(id = R.string.nav_bar_tv_show_button)
        )
    }

    NavigationButton(onClick = { navController.navigate(SETTINGS_SCREEN) }) {
        Icon(
            painterResource(R.drawable.person_48px),
            contentDescription = stringResource(id = R.string.nav_bar_settings_button)
        )
    }

    NavigationButton(onClick = { navController.navigate(PROGRESS_SCREEN) }) {
        Icon(
            painterResource(R.drawable.star_48px),
            contentDescription = stringResource(id = R.string.nav_bar_user_progress_button),
            tint = if (unlockedAchievements.newAchievements) MaterialTheme.colorScheme.primary else LocalContentColor.current
        )
    }

    NavigationButton(onClick = { navController.navigate(ABOUT_SCREEN) }) {
        Icon(
            painterResource(R.drawable.info_48px),
            contentDescription = stringResource(id = R.string.nav_bar_about_button)
        )
    }
}

@Composable
fun NavigationButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    content: @Composable () -> Unit
) {
    @Suppress("DEPRECATION_ERROR")
    Box(
        modifier = modifier
            .minimumInteractiveComponentSize()
            .size(40.0.dp)
            .background(color = MaterialTheme.colorScheme.secondary)
            .clickable(
                onClick = onClick,
                role = Role.Button,
                interactionSource = interactionSource,
                indication = androidx.compose.material.ripple.rememberRipple(
                    bounded = false,
                    radius = 20.dp
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        val contentColor = LocalContentColor.current
        CompositionLocalProvider(LocalContentColor provides contentColor, content = content)
    }
}

@Composable
private fun SettingsScreenContent(
    filmFactsViewModel: FilmFactsViewModel,
    authenticationViewModel: AuthenticationViewModel,
    imageContent: @Composable (
        modifier: Modifier,
        contentDescription: String,
        url: String,
        builder: RequestBuilderTransform<Drawable>
    ) -> Unit
) {
    val movieUserSettings by filmFactsViewModel.movieUserSettings.collectAsStateWithLifecycle()
    val tvShowUserSettings by filmFactsViewModel.tvShowUserSettings.collectAsStateWithLifecycle()
    val accountDetails by filmFactsViewModel.accountDetails.collectAsStateWithLifecycle()
    val context = LocalContext.current
    Settings(filmFactsViewModel, movieUserSettings, tvShowUserSettings, accountDetails, imageContent) {
        when (it) {
            is SettingsResult.UpdatedSettings -> {
                if (it.movieUserSettings != movieUserSettings) {
                    filmFactsViewModel.updateMovieUserSettings(it.movieUserSettings)
                }

                if (it.tvShowUserSettings != tvShowUserSettings) {
                    filmFactsViewModel.updateTvShowUserSettings(it.tvShowUserSettings)
                }
            }
            is SettingsResult.SignIn -> {
                context.startActivity(Intent(context, AuthenticationActivity::class.java))
            }
            is SettingsResult.SignOut -> {
                authenticationViewModel.deleteSession()
                authenticationViewModel.prepareAuthenticationSession()
            }
        }
    }
}

@Composable
private fun TriviaScreenContent(
    filmFactsViewModel: FilmFactsViewModel,
    triviaSessionsViewModel: TriviaSessionsViewModel,
    navController: NavController,
    imageContent: @Composable (
        modifier: Modifier,
        contentDescription: String,
        url: String,
        builder: RequestBuilderTransform<Drawable>
    ) -> Unit
) {
    BackHandler {
        filmFactsViewModel.cancelPrompts()
        navController.popBackStack()
    }
    val prompt by filmFactsViewModel.prompt.collectAsStateWithLifecycle()

    LaunchedEffect(prompt) {
        if (prompt is PromptState.Error) {
            navController.navigate(ERROR_SCREEN)
        }
    }
    when (val currentPrompt = prompt) {
        is PromptState.Ready -> {
            when (currentPrompt.prompt) {
                is UiImagePrompt -> {
                    PickImage(
                        prompt = currentPrompt.prompt,
                        imageContent = imageContent
                    ) {
                        triviaSessionsViewModel.storeTriviaAnswer(it)
                        filmFactsViewModel.nextPrompt()
                    }
                }

                is UiTextPrompt -> {
                    PickText(
                        prompt = currentPrompt.prompt,
                        imageContent = imageContent
                    ) {
                        triviaSessionsViewModel.storeTriviaAnswer(it)
                        filmFactsViewModel.nextPrompt()
                    }
                }
            }
        }

        is PromptState.None -> {
            TriviaLoadingScreen()
        }

        is PromptState.Finished -> {
            TriviaScoreScreen(triviaSessionsViewModel = triviaSessionsViewModel) {
                when (filmFactsViewModel.promptGroup) {
                    FilmFactsViewModel.PromptGroup.MOVIES -> {
                        navController.navigate(MOVIE_SCREEN) {
                            popUpTo(MOVIE_SCREEN) {
                                inclusive = true
                            }
                        }
                    }
                    FilmFactsViewModel.PromptGroup.TV_SHOWS -> {
                        navController.navigate(TV_SHOW_SCREEN) {
                            popUpTo(TV_SHOW_SCREEN) {
                                inclusive = true
                            }
                        }
                    }
                }
            }
        }

        is PromptState.Error -> {}
    }
}
package com.movietrivia.filmfacts.ui.compose

import android.content.Intent
import android.graphics.drawable.Drawable
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
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
import com.movietrivia.filmfacts.model.UiImagePrompt
import com.movietrivia.filmfacts.model.UiTextPrompt
import com.movietrivia.filmfacts.viewmodel.AuthenticationViewModel
import com.movietrivia.filmfacts.viewmodel.FilmFactsViewModel
import com.movietrivia.filmfacts.viewmodel.TriviaSessionsViewModel
import com.movietrivia.filmfacts.viewmodel.UiStateViewModel

private const val SPLASH_SCREEN = "SPLASH_SCREEN"
private const val HOME_SCREEN = "HOME_SCREEN"
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
                navController.navigate(HOME_SCREEN) {
                    popUpTo(SPLASH_SCREEN) {
                        inclusive = true
                    }
                }
            }
        }

        composable(HOME_SCREEN) {
            Screen(navController, filmFactsViewModel, title = stringResource(id = R.string.home_screen_title)) {
                HomeScreen(filmFactsViewModel, uiStateViewModel, imageContent) {
                    when (it) {
                        is HomeScreenAction.StartGenre -> {
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
                    navController.navigate(HOME_SCREEN) {
                        popUpTo(HOME_SCREEN) {
                            inclusive = true
                        }
                    }
                }
            }
        }
    }
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
    IconButton(onClick = { navController.navigate(HOME_SCREEN) }) {
        Icon(
            painterResource(R.drawable.home_48px),
            contentDescription = stringResource(id = R.string.nav_bar_home_button)
        )
    }

    IconButton(onClick = { navController.navigate(SETTINGS_SCREEN) }) {
        Icon(
            painterResource(R.drawable.person_48px),
            contentDescription = stringResource(id = R.string.nav_bar_settings_button)
        )
    }

    IconButton(onClick = { navController.navigate(PROGRESS_SCREEN) }) {
        Icon(
            painterResource(R.drawable.star_48px),
            contentDescription = stringResource(id = R.string.nav_bar_user_progress_button),
            tint = if (unlockedAchievements.newAchievements) MaterialTheme.colorScheme.primary else LocalContentColor.current
        )
    }

    IconButton(onClick = { navController.navigate(ABOUT_SCREEN) }) {
        Icon(
            painterResource(R.drawable.info_48px),
            contentDescription = stringResource(id = R.string.nav_bar_about_button)
        )
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
    val userSettings by filmFactsViewModel.userSettings.collectAsStateWithLifecycle()
    val accountDetails by filmFactsViewModel.accountDetails.collectAsStateWithLifecycle()
    val context = LocalContext.current
    Settings(filmFactsViewModel, userSettings, accountDetails, imageContent) {
        when (it) {
            is SettingsResult.UpdatedSettings -> {
                if (it.userSettings != userSettings) {
                    filmFactsViewModel.updateUserSettings(it.userSettings)
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
                navController.navigate(HOME_SCREEN) {
                    popUpTo(HOME_SCREEN) {
                        inclusive = true
                    }
                }
            }
        }

        is PromptState.Error -> {}
    }
}
package com.movietrivia.filmfacts

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import com.movietrivia.filmfacts.model.AccountDetails
import com.movietrivia.filmfacts.model.PendingData
import com.movietrivia.filmfacts.model.firstResponse
import com.movietrivia.filmfacts.ui.compose.GlideWrapper
import com.movietrivia.filmfacts.ui.compose.ScreenNavigation
import com.movietrivia.filmfacts.ui.theme.FilmFactsTheme
import com.movietrivia.filmfacts.viewmodel.AuthenticationViewModel
import com.movietrivia.filmfacts.viewmodel.FilmFactsViewModel
import com.movietrivia.filmfacts.viewmodel.TriviaSessionsViewModel
import com.movietrivia.filmfacts.viewmodel.UiStateViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val filmFactsViewModel: FilmFactsViewModel by viewModels()
    private val uiStateViewModel: UiStateViewModel by viewModels()
    private val triviaSessionsViewModel: TriviaSessionsViewModel by viewModels()
    private val authenticationViewModel: AuthenticationViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        lifecycle.addObserver(filmFactsViewModel)

        setContent {
            FilmFactsTheme {
                ScreenNavigation(
                    filmFactsViewModel = filmFactsViewModel,
                    uiStateViewModel = uiStateViewModel,
                    triviaSessionsViewModel = triviaSessionsViewModel,
                    authenticationViewModel = authenticationViewModel,
                    imageContent = {
                            modifier,
                            contentDescription,
                            url,
                            builder ->
                        GlideWrapper(modifier, contentDescription, url, builder)
                    }
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        lifecycleScope.launch {
            if (authenticationViewModel.hasPendingRequestToken.value) {
                authenticationViewModel.createPendingSession()
            }

            if (authenticationViewModel.hasSession() &&
                (filmFactsViewModel.accountDetails.value is PendingData.Error ||
                        filmFactsViewModel.accountDetails.value is PendingData.None)
            ) {
                filmFactsViewModel.loadAccountDetails()
            }
            filmFactsViewModel.accountDetails.firstResponse().collect { pendingData: PendingData<AccountDetails> ->
                val hasAuthRequest = authenticationViewModel.hasAuthRequest.value
                if (pendingData is PendingData.Error<AccountDetails> && !hasAuthRequest) {
                    authenticationViewModel.prepareAuthenticationSession()
                }
            }
        }
    }
}
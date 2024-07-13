package com.movietrivia.filmfacts.ui.compose

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.movietrivia.filmfacts.R
import com.movietrivia.filmfacts.model.Achievement
import com.movietrivia.filmfacts.viewmodel.TriviaSessionsViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.seconds


@Composable
fun TriviaLoadingScreen() {
    Column(
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxSize()
            .background(color = MaterialTheme.colorScheme.background)
    ) {

        Text(
            text = stringResource(id = R.string.trivia_load_screen_message),
            color = MaterialTheme.colorScheme.onBackground,
            fontSize = 40.sp
        )
        MovieCountdown()
    }
}

@Composable
fun TriviaScoreScreen(triviaSessionsViewModel: TriviaSessionsViewModel, continueAction: () -> Unit) {
    val results = triviaSessionsViewModel.triviaResults
    var continueButtonVisible by rememberSaveable { mutableStateOf(false) }
    var achievements by rememberSaveable { mutableStateOf(setOf<Achievement>()) }
    var achievementsCalculated by rememberSaveable { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    val initialDelay = 0.3.seconds
    val duration = 2.seconds

    Column(
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .background(color = MaterialTheme.colorScheme.background)
    ) {
        Text(
            text = stringResource(id = R.string.trivia_score_screen_title),
            color = MaterialTheme.colorScheme.onBackground,
            fontSize = 60.sp
        )
        Row {
            ScrollingCounter(
                targetValue = results.filter { it.correct }.size,
                color = MaterialTheme.colorScheme.onBackground,
                fontSize = 24.sp,
                initialDelay = initialDelay,
                duration = duration
            )
            Text(
                text = " / ${results.size}",
                color = MaterialTheme.colorScheme.onBackground,
                fontSize = 24.sp
            )
        }

        AnimatedVisibility(visible = continueButtonVisible) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(horizontal = 7.dp)
            ) {
                achievements.forEach {
                    AchievementRibbon(
                        achievement = it,
                        awarded = true,
                        animated = true,
                        progress = null
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                }
                Button(
                    onClick = { continueAction() },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text(
                        text = stringResource(id = R.string.trivia_score_screen_button),
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onPrimary,
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
        }

        LaunchedEffect(Unit) {
            coroutineScope.launch {
                if (!achievementsCalculated) {
                    achievements = triviaSessionsViewModel.awardAchievements()
                    achievementsCalculated = true
                }
            }

            delay(initialDelay + duration + 0.3.seconds)
            continueButtonVisible = true
        }
    }
}
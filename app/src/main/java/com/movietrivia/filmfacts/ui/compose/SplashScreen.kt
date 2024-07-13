package com.movietrivia.filmfacts.ui.compose

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.sp
import com.movietrivia.filmfacts.R

@Composable
fun SplashScreen(finished: () -> Unit) {
    val gradient = listOf(
        MaterialTheme.colorScheme.onBackground,
        MaterialTheme.colorScheme.onBackground,
        MaterialTheme.colorScheme.secondary,
        MaterialTheme.colorScheme.onBackground,
        MaterialTheme.colorScheme.onBackground
    )
    var animating by remember { mutableStateOf(false) }
    val duration = 700
    val initialDelay = 100
    val offset by animateFloatAsState(
        targetValue = if (animating) 900f else -900f,
        animationSpec = tween(
            delayMillis = initialDelay,
            durationMillis = duration,
            easing = LinearEasing
        ),
        finishedListener = { finished() }, label = ""
    )
    val alpha = remember { Animatable(0f) }

    LaunchedEffect(null) {
        animating = true
        alpha.animateTo(.2f,
            animationSpec = tween(
                delayMillis = initialDelay,
                durationMillis = (duration - initialDelay) / 2
            )
        )
        alpha.animateTo(
            targetValue = 0f,
            animationSpec = tween(durationMillis = (duration - initialDelay) / 2)
        )
    }
    Column(
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxSize()
            .background(color = MaterialTheme.colorScheme.background)
    ) {
        Box {
            Text(
                text = stringResource(id = R.string.splash_screen_title),
                style = TextStyle(
                    brush = Brush.horizontalGradient(
                        colors = gradient,
                        startX = offset
                    )
                ),
                fontSize = 70.sp
            )
            Text(
                text = stringResource(id = R.string.splash_screen_title),
                style = TextStyle(
                    color = Color.Black.copy(alpha = alpha.value)
                ),
                fontSize = 70.sp
            )
        }
    }
}
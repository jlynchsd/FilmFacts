package com.movietrivia.filmfacts.ui.compose

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.TextUnit
import kotlin.time.Duration

@Composable
fun ScrollingCounter(
    targetValue: Int,
    color: Color,
    fontSize: TextUnit,
    initialDelay: Duration,
    duration: Duration
) {
    var value by rememberSaveable { mutableIntStateOf(0) }
    val counter by animateIntAsState(
        targetValue = value,
        animationSpec = tween(
            durationMillis = duration.inWholeMilliseconds.toInt(),
            delayMillis = initialDelay.inWholeMilliseconds.toInt(),
            easing = FastOutSlowInEasing
        ),
        label = "ScrollingCounter"
    )
    Text(
        text = "$counter",
        color = color,
        fontSize = fontSize
    )
    LaunchedEffect(Unit) {
        value = targetValue
    }
}
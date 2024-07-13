package com.movietrivia.filmfacts.ui.compose

import android.annotation.SuppressLint
import android.content.res.Configuration
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext

@SuppressLint("UnusedBoxWithConstraintsScope")
@Composable
fun LayoutSelector(
    landscape: @Composable () -> Unit,
    portrait: @Composable () -> Unit,
    modifier: Modifier = Modifier
) {
    val isPortrait = LocalContext.current.resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT
    BoxWithConstraints(modifier) {
        if (isPortrait) {
            portrait()
        } else {
            landscape()
        }
    }
}
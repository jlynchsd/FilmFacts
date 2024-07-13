package com.movietrivia.filmfacts.ui.compose

import android.annotation.SuppressLint
import android.graphics.drawable.Drawable
import androidx.compose.animation.animateColor
import androidx.compose.animation.core.animateDp
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.bumptech.glide.integration.compose.RequestBuilderTransform
import com.movietrivia.filmfacts.model.UiImageEntry

@SuppressLint("UnusedBoxWithConstraintsScope")
@Composable
fun UiImageOption(
    entries: List<UiImageEntryState>,
    index: Int,
    finished: Boolean,
    forcePortrait: Boolean,
    imageContent: @Composable (
        modifier: Modifier,
        contentDescription: String,
        url: String,
        builder: RequestBuilderTransform<Drawable>
    ) -> Unit,
    modifier: Modifier = Modifier,
    onClick : (isAnswer: Boolean, index: Int) -> Unit
) {
    val entry = entries[index].entry
    val picked  = entries[index].picked
    val tweenDuration = if (picked) {
        200
    } else {
        0
    }
    val showData = entry.data != null && finished

    val dataBackgroundColor = if (entry.isAnswer) {
        MaterialTheme.colorScheme.tertiary
    } else {
        if (picked) {
            MaterialTheme.colorScheme.error
        } else {
            MaterialTheme.colorScheme.background.copy(alpha = .8f)
        }
    }

    val dataTextColor = if (entry.isAnswer) {
        MaterialTheme.colorScheme.onTertiary
    } else {
        if (picked) {
            MaterialTheme.colorScheme.onError
        } else {
            MaterialTheme.colorScheme.onBackground
        }
    }

    val dataBottomPadding = if (picked) {
        8.dp
    } else {
        12.dp
    }

    val transition = updateTransition(targetState = picked, label = "entryClickTransition")
    val entryPadding by transition.animateDp( transitionSpec = {
        tween(tweenDuration)
    }, label = "entryPadding") { shouldAnimate ->
        if (shouldAnimate) {
            10.dp
        } else {
            6.dp
        }
    }
    val entryBorderColor by transition.animateColor(transitionSpec = {
        tween(tweenDuration)
    }, label = "entryBorderColor") { shouldAnimate ->
        val targetColor = if (entry.isAnswer) {
            MaterialTheme.colorScheme.tertiary
        } else {
            MaterialTheme.colorScheme.error
        }

        if (shouldAnimate) {
            targetColor
        } else {
            MaterialTheme.colorScheme.background
        }
    }
    Column(
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .padding(entryPadding)
            .clickable {
                if (!picked) {
                    onClick(entry.isAnswer, index)
                }
            }
    ) {
        var boxModifier = Modifier
            .fillMaxHeight()
            .border(7.dp, entryBorderColor)
        if (forcePortrait) {
            boxModifier = boxModifier.aspectRatio(0.681481f)
        }
        BoxWithConstraints(
            modifier = boxModifier
        ) {
            imageContent(
                Modifier.fillMaxSize(),
                "",
                entry.imagePath
            ) { it.thumbnail(it.clone().sizeMultiplier(.25f)) }
            if (entry.title != null) {
                Row(
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .fillMaxWidth()
                        .background(color = MaterialTheme.colorScheme.background.copy(alpha = .8f))
                ) {
                    Text(
                        text = entry.title,
                        maxLines = 1,
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier
                            .fillMaxWidth(.85f)
                            .padding(top = 8.dp, bottom = 2.dp)
                    )
                }
            }
            if (entry.data != null && showData) {
                Row(
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .background(color = dataBackgroundColor)
                ) {
                    Text(
                        text = entry.data,
                        maxLines = 1,
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.bodyLarge,
                        color = dataTextColor,
                        modifier = Modifier
                            .padding(top = 4.dp, bottom = dataBottomPadding)
                    )
                }
            }
        }
    }
}

data class UiImageEntryState(
    val entry: UiImageEntry,
    val picked: Boolean = false,
) : java.io.Serializable
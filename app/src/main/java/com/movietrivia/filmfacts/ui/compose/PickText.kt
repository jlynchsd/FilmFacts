package com.movietrivia.filmfacts.ui.compose

import android.graphics.drawable.Drawable
import androidx.compose.animation.animateColor
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.bumptech.glide.integration.compose.RequestBuilderTransform
import com.movietrivia.filmfacts.model.TriviaQuestionResult
import com.movietrivia.filmfacts.model.UiTextEntry
import com.movietrivia.filmfacts.model.UiTextPrompt

@Composable
fun PickText(
    prompt: UiTextPrompt,
    imageContent: @Composable (
        modifier: Modifier,
        contentDescription: String,
        url: String,
        builder: RequestBuilderTransform<Drawable>
    ) -> Unit,
    continueAction: (result: TriviaQuestionResult) -> Unit
) {
    var finished by rememberSaveable(prompt) { mutableStateOf(false) }
    var correct by rememberSaveable(prompt) { mutableStateOf(false) }
    val startTime by remember(prompt) { mutableLongStateOf(System.currentTimeMillis()) }
    var totalTime by remember(prompt) { mutableLongStateOf(0L) }
    val totalCorrectAnswers = prompt.entries.count { it.isAnswer }
    val images = prompt.images.map { UiImageEntryState(it) }
    var entries by rememberSaveable(prompt) { mutableStateOf(prompt.entries.map { UiTextEntryState(it) }) }
    var correctPicks by rememberSaveable(prompt) { mutableIntStateOf(0) }
    LayoutSelector(
        landscape = {
            PickTextLandscape(
                prompt,
                finished,
                correct,
                totalTime,
                images,
                entries,
                imageContent,
                continueAction
            ) { isAnswer, index ->
                if (!finished) {
                    entries = entries.mapIndexed { mapIndex, entryState -> if (mapIndex == index) entryState.copy(picked = true) else entryState }
                    if (isAnswer) {
                        correctPicks++
                        if (correctPicks == totalCorrectAnswers) {
                            finished = true
                            correct = true
                            totalTime = System.currentTimeMillis() - startTime
                        }
                    } else {
                        entries = entries.map { it.copy(picked = true) }
                        finished = true
                        correct = false
                        totalTime = System.currentTimeMillis() - startTime
                    }
                }
            }
        },
        portrait = {
            PickTextPortrait(
                prompt,
                finished,
                correct,
                totalTime,
                images,
                entries,
                imageContent,
                continueAction
            ) { isAnswer, index ->
                if (!finished) {
                    entries = entries.mapIndexed { mapIndex, entryState -> if (mapIndex == index) entryState.copy(picked = true) else entryState }
                    if (isAnswer) {
                        correctPicks++
                        if (correctPicks == totalCorrectAnswers) {
                            finished = true
                            correct = true
                            totalTime = System.currentTimeMillis() - startTime
                        }
                    } else {
                        entries = entries.map { it.copy(picked = true) }
                        finished = true
                        correct = false
                        totalTime = System.currentTimeMillis() - startTime
                    }
                }
            }
        }
    )
}

@Composable
private fun PickTextLandscape(
    prompt: UiTextPrompt,
    finished: Boolean,
    correct: Boolean,
    totalTime: Long,
    images: List<UiImageEntryState>,
    entries: List<UiTextEntryState>,
    imageContent: @Composable (
        modifier: Modifier,
        contentDescription: String,
        url: String,
        builder: RequestBuilderTransform<Drawable>
    ) -> Unit,
    continueAction: (result: TriviaQuestionResult) -> Unit,
    pickedAction: (isAnswer: Boolean, index: Int) -> Unit
) {
    val imageWeight = if (prompt.wideImage) {
        0.5f
    } else {
        0.4f
    }
    val textWeight = 1 - imageWeight
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxSize()
            .background(color = MaterialTheme.colorScheme.background)
    ) {
        PickHeader(titleId = prompt.titleId, titleData = prompt.titleData)

        Row(
            modifier = Modifier.fillMaxWidth().weight(.9f).padding(top = 8.dp, bottom = 8.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(imageWeight)
            ) {
                images.forEachIndexed { index, _ ->
                    UiImageOption(
                        images,
                        index,
                        false,
                        !prompt.wideImage,
                        imageContent,
                        Modifier.weight(1f)
                    ) { _, _ -> }
                }
            }

            Column(
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(textWeight)
            ) {
                entries.forEachIndexed { index, _ ->
                    UiTextOption(
                        entries,
                        index,
                        true,
                        Modifier.weight(.25f),
                        pickedAction
                    )
                }
            }
        }

        PickFooter(
            finished = finished,
            result = TriviaQuestionResult(correct, totalTime),
            modifier = Modifier.weight(.25f),
            continueAction = continueAction
        )
    }
}

@Composable
private fun PickTextPortrait(
    prompt: UiTextPrompt,
    finished: Boolean,
    correct: Boolean,
    totalTime: Long,
    images: List<UiImageEntryState>,
    entries: List<UiTextEntryState>,
    imageContent: @Composable (
        modifier: Modifier,
        contentDescription: String,
        url: String,
        builder: RequestBuilderTransform<Drawable>
    ) -> Unit,
    continueAction: (result: TriviaQuestionResult) -> Unit,
    pickedAction: (isAnswer: Boolean, index: Int) -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxSize()
            .background(color = MaterialTheme.colorScheme.background)
    ) {
        PickHeader(titleId = prompt.titleId, titleData = prompt.titleData)

        Row(
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .weight(.9f)
        ) {
            images.forEachIndexed { index, _ ->
                UiImageOption(
                    images,
                    index,
                    false,
                    !prompt.wideImage,
                    imageContent,
                    Modifier.weight(1f)
                ) { _, _ -> }
            }
        }

        Column(
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1.1f)
                .padding(top = 8.dp, bottom = 8.dp)
        ) {
            entries.forEachIndexed { index, _ ->
                UiTextOption(
                    entries,
                    index,
                    false,
                    Modifier.weight(.25f),
                    pickedAction
                )
            }
        }

        PickFooter(
            finished = finished,
            result = TriviaQuestionResult(correct, totalTime),
            modifier = Modifier.weight(.25f),
            continueAction = continueAction
        )
    }
}

@Composable
private fun UiTextOption(
    entries: List<UiTextEntryState>,
    index: Int,
    singleLineText: Boolean,
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

    val transition = updateTransition(targetState = picked, label = "entryClickTransition")
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
    val entryBackgroundColor by transition.animateColor(transitionSpec = {
        tween(tweenDuration)
    }, label = "entryBackgroundColor") { shouldAnimate ->
        val targetColor = if (entry.isAnswer) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.errorContainer
        }

        if (shouldAnimate) {
            targetColor
        } else {
            MaterialTheme.colorScheme.surface
        }
    }

    val entryTextColor by transition.animateColor(transitionSpec = {
        tween(tweenDuration)
    }, label = "entryTextColor") { shouldAnimate ->
        val targetColor = if (entry.isAnswer) {
            MaterialTheme.colorScheme.onPrimaryContainer
        } else {
            MaterialTheme.colorScheme.onErrorContainer
        }

        if (shouldAnimate) {
            targetColor
        } else {
            MaterialTheme.colorScheme.onSurface
        }
    }

    Button(
        onClick = {
            if (!picked) {
                onClick(entry.isAnswer, index)
            }
        },
        border = BorderStroke(4.dp, color = entryBorderColor),
        colors = ButtonDefaults.buttonColors(
            containerColor = entryBackgroundColor
        ),
        modifier = modifier
            .fillMaxWidth(.95f)
            .padding(2.dp)
    ) {
        val fontSize = MaterialTheme.typography.bodyLarge.fontSize
        var currentFontSize by remember { mutableStateOf(fontSize) }
        Text(
            text = buildAnnotatedString {
                append(entry.topContent.trim())
                entry.subContent?.let {
                    val delimiter = if (singleLineText) {
                        ", "
                    } else {
                        "\n"
                    }
                    append(delimiter)
                    withStyle(
                        SpanStyle(
                            fontStyle = FontStyle.Italic
                        )
                    ) {
                        append(it)
                    }
                }
            },
            textAlign = TextAlign.Center,
            color = entryTextColor,
            style = MaterialTheme.typography.bodyLarge,
            fontSize = currentFontSize,
            maxLines = if (singleLineText) 1 else 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(4.dp),
            onTextLayout = {
                if (it.isLineEllipsized(it.lineCount - 1)) {
                    currentFontSize = currentFontSize.times(.9f)
                }
            }
        )
    }
}

private data class UiTextEntryState(
    val entry: UiTextEntry,
    val picked: Boolean = false
) : java.io.Serializable
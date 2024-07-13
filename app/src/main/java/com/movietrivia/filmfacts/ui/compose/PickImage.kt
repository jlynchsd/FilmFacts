package com.movietrivia.filmfacts.ui.compose

import android.graphics.drawable.Drawable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.bumptech.glide.integration.compose.RequestBuilderTransform
import com.movietrivia.filmfacts.model.TriviaQuestionResult
import com.movietrivia.filmfacts.model.UiImagePrompt

@Composable
fun PickImage(
    prompt: UiImagePrompt,
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
    var entries by rememberSaveable(prompt) { mutableStateOf(prompt.entries.map { UiImageEntryState(it) }) }
    var correctPicks by rememberSaveable(prompt) { mutableIntStateOf(0) }
    LayoutSelector(
        landscape = {
            PickImageLandscape(
                prompt,
                finished,
                correct,
                totalTime,
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
            PickImagePortrait(
                prompt,
                finished,
                correct,
                totalTime,
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
private fun PickImageLandscape(
    prompt: UiImagePrompt,
    finished: Boolean,
    correct: Boolean,
    totalTime: Long,
    entries: List<UiImageEntryState>,
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
                .weight(1f)) {
            repeat(4) { index ->
                UiImageOption(
                    entries,
                    index,
                    finished,
                    true,
                    imageContent,
                    Modifier
                        .weight(1f),
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
private fun PickImagePortrait(
    prompt: UiImagePrompt,
    finished: Boolean,
    correct: Boolean,
    totalTime: Long,
    entries: List<UiImageEntryState>,
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
                .weight(1f)) {
            repeat(2) { index ->
                UiImageOption(
                    entries,
                    index,
                    finished,
                    true,
                    imageContent,
                    Modifier
                        .weight(1f),
                    pickedAction
                )
            }
        }
        Row(
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)) {
            repeat(2) { index ->
                UiImageOption(
                    entries,
                    index + 2,
                    finished,
                    true,
                    imageContent,
                    Modifier
                        .weight(1f),
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

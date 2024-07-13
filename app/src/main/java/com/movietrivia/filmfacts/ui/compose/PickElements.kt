package com.movietrivia.filmfacts.ui.compose

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.movietrivia.filmfacts.R
import com.movietrivia.filmfacts.model.TriviaQuestionResult

@Composable
fun PickHeader(titleId: Int, titleData: List<String>) {
    Surface(color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.fillMaxWidth()) {
        Row(
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = stringResource(id = titleId, *titleData.toTypedArray()),
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onPrimary,
                style = MaterialTheme.typography.headlineLarge,
                modifier = Modifier.padding(bottom = 4.dp)
            )
        }
    }
}

@Composable
fun PickFooter(
    finished: Boolean,
    result: TriviaQuestionResult,
    modifier: Modifier = Modifier,
    continueAction: (result: TriviaQuestionResult) -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.secondary,
        modifier = modifier.fillMaxWidth()
    ) {
        if (finished) {
            val buttonColor = if (result.correct) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.error
            }

            val textColor = if (result.correct) {
                MaterialTheme.colorScheme.onPrimary
            } else {
                MaterialTheme.colorScheme.onError
            }

            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
            ) {
                Button(
                    onClick = { continueAction(result) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = buttonColor
                    )
                ) {
                    Text(
                        text = stringResource(id = R.string.continue_button_message),
                        textAlign = TextAlign.Center,
                        color = textColor,
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
        }
    }
}
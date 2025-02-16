package com.movietrivia.filmfacts.ui.compose

import android.graphics.drawable.Drawable
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.bumptech.glide.integration.compose.RequestBuilderTransform
import com.movietrivia.filmfacts.R
import com.movietrivia.filmfacts.model.UiGenre

@Composable
fun PickGenre(
    uiGenre: UiGenre,
    genreMap: (UiGenre) -> Pair<Int, Int>,
    imageContent: @Composable (
        modifier: Modifier,
        contentDescription: String,
        url: String,
        builder: RequestBuilderTransform<Drawable>
    ) -> Unit,
    callback: (genreId: Int?) -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val text = if (uiGenre.genreId == -1) {
        stringResource(id = R.string.film_genres_all)
    } else {
        val mappedGenre = genreMap(uiGenre)
        stringArrayResource(id = mappedGenre.second)[mappedGenre.first]
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth(.95f)
    ) {
        Row(
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxWidth()
                .background(color = MaterialTheme.colorScheme.background.copy(alpha = .8f))
        ) {
            Text(
                text = text,
                maxLines = 1,
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier
                    .fillMaxWidth(.85f)
                    .padding(top = 8.dp, bottom = 2.dp)
            )
        }
        imageContent(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(.7f)
                .clickable(interactionSource, indication = null) { callback( if (uiGenre.genreId != -1) uiGenre.genreId else null ) },
            "",
            uiGenre.imagePath
        ) {
            it.circleCrop().thumbnail(it.clone().sizeMultiplier(.5f))
        }
    }
}
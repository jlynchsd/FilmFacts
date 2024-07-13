package com.movietrivia.filmfacts.ui.compose

import android.graphics.drawable.Drawable
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage
import com.bumptech.glide.integration.compose.RequestBuilderTransform

@OptIn(ExperimentalGlideComposeApi::class)
@Composable
fun GlideWrapper(
    modifier: Modifier = Modifier,
    contentDescription: String = "",
    url: String,
    builder: RequestBuilderTransform<Drawable>
) {
    GlideImage(
        model = url,
        contentDescription = contentDescription,
        modifier = modifier,
        requestBuilderTransform = builder
    )
}
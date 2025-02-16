package com.movietrivia.filmfacts.ui.compose

import android.content.res.Configuration
import android.graphics.drawable.Drawable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bumptech.glide.integration.compose.RequestBuilderTransform
import com.movietrivia.filmfacts.model.UiGenre
import com.movietrivia.filmfacts.viewmodel.FilmFactsViewModel
import com.movietrivia.filmfacts.viewmodel.UiStateViewModel

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun GenreScreen(
    filmFactsViewModel: FilmFactsViewModel,
    uiStateViewModel: UiStateViewModel,
    genreMap: (UiGenre) -> Pair<Int, Int>,
    imageContent: @Composable (
        modifier: Modifier,
        contentDescription: String,
        url: String,
        builder: RequestBuilderTransform<Drawable>
    ) -> Unit,
    onClick: (action: GenreScreenAction) -> Unit
) {
    val genreImages by filmFactsViewModel.genreImages.collectAsStateWithLifecycle()
    val isPortrait = LocalContext.current.resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT
    val state = rememberLazyListState()
    LaunchedEffect(isPortrait) {
        if (isPortrait) {
            state.scrollToItem(uiStateViewModel.homeScreenScrollOffset)
        } else {
            state.scrollToItem(index = uiStateViewModel.homeScreenScrollOffset / 2)
        }
    }
    DisposableEffect(isPortrait) {
        onDispose {
            if (isPortrait) {
                uiStateViewModel.homeScreenScrollOffset = state.firstVisibleItemIndex
            } else {
                uiStateViewModel.homeScreenScrollOffset = state.firstVisibleItemIndex * 2
            }
        }
    }
    Column(
        modifier = Modifier.fillMaxSize().background(color = MaterialTheme.colorScheme.background)
    ) {
        LayoutSelector(
            landscape = {
                LazyRow(
                    modifier = Modifier.fillMaxSize(),
                    state = state,
                    flingBehavior = rememberSnapFlingBehavior(lazyListState = state)
                ) {
                    items(pairGenres(genreImages)) { entry ->
                        Row(
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillParentMaxSize()
                        ) {
                            val secondEntry = entry.second
                            if (secondEntry != null) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier.weight(0.5f)
                                ) {
                                    PickGenre(entry.first, genreMap, imageContent) {
                                        onClick(GenreScreenAction.StartGenre(it))
                                    }
                                }
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier.weight(0.5f)
                                ) {
                                    PickGenre(secondEntry, genreMap, imageContent) {
                                        onClick(GenreScreenAction.StartGenre(it))
                                    }
                                }
                            } else {
                                PickGenre(entry.first, genreMap, imageContent) {
                                    onClick(GenreScreenAction.StartGenre(it))
                                }
                            }
                        }
                    }
                }
            },
            portrait = {
                LazyRow(
                    modifier = Modifier.fillMaxSize(),
                    state = state,
                    flingBehavior = rememberSnapFlingBehavior(lazyListState = state)
                ) {
                    items(genreImages) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier.fillParentMaxSize()
                        ) {
                            PickGenre(it, genreMap, imageContent) {
                                onClick(GenreScreenAction.StartGenre(it))
                            }
                        }
                    }
                }
            }
        )
    }
}

private fun pairGenres(genres: List<UiGenre>): List<Pair<UiGenre, UiGenre?>> {
    val entries = genres.toMutableList()
    var trailingEntry: UiGenre? = null
    val result = mutableListOf<Pair<UiGenre, UiGenre?>>()
    if (entries.size % 2 != 0) {
        trailingEntry = entries.removeLast()
    }
    entries.forEachIndexed { index, uiGenre ->
        if (index % 2 != 0) {
            result.add(Pair(entries[index - 1], uiGenre))
        }
    }
    trailingEntry?.let {
        result.add(Pair(it, null))
    }

    return result
}

sealed class GenreScreenAction {
    class StartGenre(val genreId: Int?) : GenreScreenAction()
}
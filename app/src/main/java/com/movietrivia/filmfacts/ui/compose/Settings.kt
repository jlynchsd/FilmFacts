package com.movietrivia.filmfacts.ui.compose

import android.content.Context
import android.graphics.drawable.Drawable
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.bumptech.glide.integration.compose.RequestBuilderTransform
import com.movietrivia.filmfacts.R
import com.movietrivia.filmfacts.model.AccountDetails
import com.movietrivia.filmfacts.model.Achievement
import com.movietrivia.filmfacts.model.PendingData
import com.movietrivia.filmfacts.model.UserSettings
import com.movietrivia.filmfacts.ui.orderedFilmGenres
import com.movietrivia.filmfacts.viewmodel.FilmFactsViewModel
import kotlinx.coroutines.delay
import kotlin.math.roundToInt
import kotlin.time.Duration.Companion.seconds

sealed class SettingsResult {
    object SignIn: SettingsResult()
    object SignOut: SettingsResult()
    data class UpdatedSettings(val userSettings: UserSettings): SettingsResult()
}

@Composable
fun Settings(
    filmFactsViewModel: FilmFactsViewModel,
    userSettings: UserSettings,
    accountDetails: PendingData<AccountDetails>,
    imageContent: @Composable (
        modifier: Modifier,
        contentDescription: String,
        url: String,
        builder: RequestBuilderTransform<Drawable>
    ) -> Unit,
    callback: (result: SettingsResult) -> Unit
) {
    val excludedFilmGenres = remember { mutableListOf(*userSettings.excludedFilmGenres.toTypedArray()) }
    var currentLanguage by remember { mutableIntStateOf(locales.indexOf(userSettings.language)) }
    var openAchievementDialog by remember { mutableStateOf(false) }
    var newAchievements by remember { mutableStateOf(emptyList<Achievement>()) }
    var startIndex = 0f
    var endIndex = (releaseOffsets.size - 1).toFloat()
    if (userSettings.releasedAfterOffset != null) {
        startIndex = releaseOffsets.indexOf(userSettings.releasedAfterOffset).toFloat()
    }
    if (userSettings.releasedBeforeOffset != null) {
        endIndex = releaseOffsets.indexOf(userSettings.releasedBeforeOffset).toFloat()
    }
    var dateRange by remember { mutableStateOf(startIndex .. endIndex) }

    Column(
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .background(color = MaterialTheme.colorScheme.background)

    ) {
        AccountContent(accountDetails, imageContent, callback)
        ExpandableContent(title = stringResource(id = R.string.settings_film_genre_title)) {
            FilmGenresContent(excludedGenres = excludedFilmGenres)
        }
        ExpandableContent(title = stringResource(id = R.string.settings_film_language_title)) {
            LayoutSelector(
                landscape = {
                    LanguagesContentLandscape(
                        currentLanguageIndex = currentLanguage,
                    ) {
                        currentLanguage = it
                    }
                }, portrait = {
                    LanguagesContentPortrait(
                        currentLanguageIndex = currentLanguage,
                    ) {
                        currentLanguage = it
                    }
                }
            )
        }
        ExpandableContent(title = stringResource(id = R.string.settings_film_release_date_title)) {
            ReleasedContent(dateRange) {
                dateRange = it
            }
        }

        if (openAchievementDialog) {
            AchievementDialogue(newAchievements = newAchievements) {
                openAchievementDialog = false
            }
        }

        LaunchedEffect(accountDetails) {
            newAchievements = filmFactsViewModel.awardAchievements().toList()
            if (newAchievements.isNotEmpty()) {
                openAchievementDialog = true
            }
        }

        DisposableEffect(key1 = null) {
            onDispose {
                val selectedStartOffset = dateRange.start.roundToInt()
                val selectedEndOffset = dateRange.endInclusive.roundToInt()
                var startOffset: Int? = null
                var endOffset: Int? = null

                if (selectedStartOffset != 0) {
                    startOffset = releaseOffsets[selectedStartOffset]
                }
                if (selectedEndOffset != (releaseOffsets.size - 1)) {
                    endOffset = releaseOffsets[selectedEndOffset]
                }

                callback(
                    SettingsResult.UpdatedSettings(
                        userSettings.copy(
                            language = locales[currentLanguage],
                            excludedFilmGenres = excludedFilmGenres,
                            releasedAfterOffset = startOffset,
                            releasedBeforeOffset = endOffset
                        )
                    )
                )
            }
        }
    }
}

@Composable
private fun ExpandableContent(title: String, content: @Composable () -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val transition = updateTransition(targetState = expanded, label = "expandSettingsTransition")
    val rotationAngle by transition.animateFloat(transitionSpec = {
        tween(300)
    }, label = "settingsArrowRotation") { isExpanded ->
        if (isExpanded) {
            90f
        } else {
            0f
        }
    }
    val offset by transition.animateOffset(transitionSpec = {
        tween(300)
    }, label = "settingsArrowOffset") { isExpanded ->
        if (isExpanded) {
            Offset(5f, 0f)
        } else {
            Offset(0f, 0f)
        }
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .background(color = MaterialTheme.colorScheme.primary)
                .fillMaxWidth()
                .clickable {
                    expanded = !expanded
                }
        ) {
            Icon(
                painterResource(id = R.drawable.arrow_forward_48px),
                contentDescription = "",
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier
                    .size(30.dp)
                    .rotate(rotationAngle)
                    .offset(offset.x.dp, offset.y.dp)
                    .padding(end = 5.dp)
            )
            Text(
                text = title,
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.padding(vertical = 10.dp)
            )
        }
        AnimatedVisibility(visible = expanded, enter = expandVertically(), exit = shrinkVertically()) {
            Row(modifier = Modifier.padding(8.dp)) {
                content()
            }
        }
    }
}

@Composable
private fun AccountContent(
    accountDetails: PendingData<AccountDetails>,
    imageContent: @Composable (
        modifier: Modifier,
        contentDescription: String,
        url: String,
        builder: RequestBuilderTransform<Drawable>
    ) -> Unit,
    callback: (result: SettingsResult) -> Unit
) {
    Box(
        modifier = Modifier
            .background(color = MaterialTheme.colorScheme.primaryContainer)
            .border(2.dp, color = MaterialTheme.colorScheme.primary)
    ) {
        if (accountDetails is PendingData.Success) {
            AccountData(accountDetails.result, imageContent, callback)
        } else {
            AccountSignIn(callback)
        }
    }
}

@Composable
private fun AccountSignIn(callback: (result: SettingsResult) -> Unit) {
    Column(
        modifier = Modifier
            .padding(20.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Image(
                painter = painterResource(id = R.drawable.tmdb_logo),
                contentDescription = stringResource(
                    id = R.string.about_page_tmdb_logo_description
                ),
                modifier = Modifier.padding(4.dp)
            )
            Column(
                verticalArrangement = Arrangement.SpaceEvenly,
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxSize()
            ) {
                Text(
                    text = stringResource(id = R.string.settings_sign_in_prompt),
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    style = MaterialTheme.typography.bodyLarge,
                    fontStyle = FontStyle.Italic,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(4.dp)
                )

                Button(
                    onClick = { callback(SettingsResult.SignIn) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text(
                        text = stringResource(id = R.string.settings_sign_in_button),
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onPrimary,
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
        }
    }
}

@Composable
private fun AccountData(
    accountDetails: AccountDetails,
    imageContent: @Composable (
        modifier: Modifier,
        contentDescription: String,
        url: String,
        builder: RequestBuilderTransform<Drawable>
    ) -> Unit,
    callback: (result: SettingsResult) -> Unit
) {
    val favoriteResId = if (accountDetails.favoriteMetaData.totalEntries == 1) {
        R.string.settings_singular_favorite_movie
    } else {
        R.string.settings_plural_favorite_movies
    }
    val ratedResId = if (accountDetails.ratedMetaData.totalEntries == 1) {
        R.string.settings_singular_rated_movie
    } else {
        R.string.settings_plural_rated_movies
    }
    val watchlistResId = if (accountDetails.watchlistMetaData.totalEntries == 1) {
        R.string.settings_singular_watchlisted_movie
    } else {
        R.string.settings_plural_watchlisted_movies
    }

    Column(
        modifier = Modifier
            .padding(20.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            imageContent(
                Modifier
                    .fillMaxHeight()
                    .padding(4.dp),
                "",
                accountDetails.avatarPath
            ) { it }
            Column(
                verticalArrangement = Arrangement.SpaceEvenly,
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxSize()
            ) {
                Column {
                    Text(
                        text = accountDetails.name.ifBlank { accountDetails.userName },
                        style = MaterialTheme.typography.titleLarge,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(4.dp)
                    )

                    Text(
                        text = "${accountDetails.favoriteMetaData.totalEntries} ${stringResource(id = favoriteResId)}",
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(2.dp)
                    )

                    Text(
                        text = "${accountDetails.ratedMetaData.totalEntries} ${stringResource(id = ratedResId)}",
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(2.dp)
                    )

                    Text(
                        text = "${accountDetails.watchlistMetaData.totalEntries} ${stringResource(id = watchlistResId)}",
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(2.dp)
                    )
                }

                Button(
                    onClick = { callback(SettingsResult.SignOut) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text(
                        text = stringResource(id = R.string.settings_sign_out_button),
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onPrimary,
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FilmGenresContent(excludedGenres: MutableList<Int>) {
    val toggleStates = remember { mutableStateMapOf<String, Boolean>() }
    FlowRow(
        horizontalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxWidth()
    ) {
        stringArrayResource(id = R.array.film_genres).forEachIndexed { index,  genre ->
            toggleStates[genre] = !excludedGenres.contains(orderedFilmGenres[index].key)
            ToggleToken(
                text = genre,
                enabledState = toggleStates) {
                toggleStates[genre] = it
                if (it) {
                    excludedGenres.remove(orderedFilmGenres[index].key)
                } else {
                    excludedGenres.add(orderedFilmGenres[index].key)
                }
            }
        }
    }
}

@Composable
private fun LanguagesContentPortrait(currentLanguageIndex: Int, callback: (updatedLanguageIndex: Int) -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        stringArrayResource(id = R.array.film_languages).forEachIndexed { index, language ->
            LanguageOption(
                language = language,
                currentLanguageIndex = currentLanguageIndex,
                index = index, callback = callback
            )
        }
    }
}

@Composable
private fun LanguagesContentLandscape(currentLanguageIndex: Int, callback: (updatedLanguageIndex: Int) -> Unit) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.weight(0.5f)) {
            stringArrayResource(id = R.array.film_languages).let {
                it.slice((0 ..  it.size / 2)).forEachIndexed { index, language ->
                    LanguageOption(
                        language = language,
                        currentLanguageIndex = currentLanguageIndex,
                        index = index, callback = callback
                    )
                }
            }
        }

        Column(modifier = Modifier.weight(0.5f)) {
            stringArrayResource(id = R.array.film_languages).let {
                val startIndex = (it.size / 2) + 1
                it.slice((startIndex until it.size)).forEachIndexed { index, language ->
                    LanguageOption(
                        language = language,
                        currentLanguageIndex = currentLanguageIndex,
                        index = startIndex + index, callback = callback
                    )
                }
            }
        }
    }
}

@Composable
fun LanguageOption(
    language: String,
    currentLanguageIndex: Int,
    index: Int,
    callback: (updatedLanguageIndex: Int) -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .selectable(
                selected = (currentLanguageIndex == index),
                onClick = { callback(index) }
            )
    ) {
        RadioButton(
            selected = (currentLanguageIndex == index),
            onClick = { callback(index) },
            colors = RadioButtonDefaults.colors(
                unselectedColor = MaterialTheme.colorScheme.onBackground
            )
        )
        Text(
            text = language,
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(start = 16.dp),
            color = MaterialTheme.colorScheme.onBackground
        )
    }
}

@Composable
private fun ReleasedContent(range: ClosedFloatingPointRange<Float>, onChanged: (range: ClosedFloatingPointRange<Float>) -> Unit) {
    Column {
        Text(
            text = formatRange(range, LocalContext.current),
            color = MaterialTheme.colorScheme.onBackground
        )
        RangeSlider(
            value = range,
            onValueChange = {
                if (it.start.roundToInt() != it.endInclusive.roundToInt()) {
                    onChanged(it)
                }
            },
            valueRange = 0f..(releaseOffsets.size - 1).toFloat(),
            steps = 5
        )
    }
}

@Composable
private fun AchievementDialogue(newAchievements: List<Achievement>, onDismissRequest: () -> Unit) {
    Dialog(onDismissRequest = onDismissRequest) {
        newAchievements.forEach {
            AchievementRibbon(
                achievement = it,
                awarded = true,
                animated = true,
                progress = null
            )
            Spacer(modifier = Modifier.height(4.dp))
        }
    }

    LaunchedEffect(Unit) {
        delay(4.seconds)
        onDismissRequest()
    }
}

private fun formatRange(range: ClosedFloatingPointRange<Float>, context: Context): String {
    val start = range.start.roundToInt()
    val end = range.endInclusive.roundToInt()
    if (start == 0 && end == (releaseOffsets.size - 1)) {
        return context.getString(R.string.settings_release_date_unlimited)
    }

    if (start == (releaseOffsets.size - 2) && end == (releaseOffsets.size - 1)) {
        return "${formatReleaseRangePick(start, context)}+ ${context.getString(R.string.settings_release_date_plural_suffix)}"
    }

    val suffix = if (end == 1) {
        context.getString(R.string.settings_release_date_singular_suffix)
    } else {
        context.getString(R.string.settings_release_date_plural_suffix)
    }
    return "${formatReleaseRangePick(start, context)} - ${formatReleaseRangePick(end, context)} $suffix"
}

private fun formatReleaseRangePick(pick: Int, context: Context) =
    when (pick) {
        0 -> context.getString(R.string.settings_release_date_present)
        else -> {
            val suffix = if (pick == (releaseOffsets.size - 1)) {
                "+"
            } else {
                ""
            }
            releaseOffsets[pick].toString() + suffix
        }
    }


@Composable
private fun ToggleToken(text: String, enabledState: SnapshotStateMap<String, Boolean>, callback: (enabled: Boolean) -> Unit) {
    val enabled = enabledState[text] ?: true
    val color = if (enabled) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.primaryContainer
    }
    val textColor = if (enabled) {
        MaterialTheme.colorScheme.onPrimary
    } else {
        MaterialTheme.colorScheme.onPrimaryContainer
    }
    Surface(modifier = Modifier.padding(4.dp)) {
        Button(
            onClick = { callback(!enabled) },
            colors = ButtonDefaults.buttonColors(
                containerColor = color
            ),
            modifier = Modifier.background(MaterialTheme.colorScheme.background)
        ) {
            Text(
                text = text,
                color = textColor
            )
        }
    }

}

private val locales = listOf("en", "es", "pt", "fr", "it", "de", "ar", "hi", "zh", "ja", "ko")
private val releaseOffsets = listOf(0, 1, 2, 5, 10, 30, 30)


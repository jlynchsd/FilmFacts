package com.movietrivia.filmfacts.ui.compose

import android.annotation.SuppressLint
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseInOutBounce
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.movietrivia.filmfacts.R
import com.movietrivia.filmfacts.model.Achievement
import com.movietrivia.filmfacts.viewmodel.FilmFactsViewModel
import java.text.SimpleDateFormat
import java.util.Date

@Composable
fun UserProgressScreen(
    filmFactsViewModel: FilmFactsViewModel
) {
    val unlockedAchievements by filmFactsViewModel.unlockedAchievements.collectAsStateWithLifecycle()
    val userHistory by filmFactsViewModel.userHistory.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        filmFactsViewModel.disableNewAchievements()
    }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .background(color = MaterialTheme.colorScheme.background)
    ) {
        Spacer(modifier = Modifier.height(7.dp))
        Text(
            stringResource(id = R.string.user_stats_title),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier
                .fillMaxWidth()
                .background(color = MaterialTheme.colorScheme.primaryContainer)
        )
        Spacer(modifier = Modifier.height(7.dp))

        Row(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(2.dp)) {
                StatisticBox(
                    content = formatStartDate(userHistory.startDate),
                    description = stringResource(id = R.string.user_stats_joined_date)
                )
                Spacer(modifier = Modifier.height(4.dp))
                StatisticBox(
                    content = userHistory.completedQuizzes.toString(),
                    description = pluralStringResource(id = R.plurals.user_stats_completed_quizzes, userHistory.completedQuizzes)
                )
                Spacer(modifier = Modifier.height(4.dp))
                StatisticBox(
                    content = userHistory.correctAnswers.toString(),
                    description = pluralStringResource(id = R.plurals.user_stats_correct_answers, userHistory.correctAnswers)
                )
            }
            Column(modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(2.dp)) {
                val totalTimePlayedUnits = calculateDuration(userHistory.totalQuizDuration)
                val totalTimePlayed = if (totalTimePlayedUnits != null) {
                    pluralStringResource(id = totalTimePlayedUnits.unit, count = totalTimePlayedUnits.count, totalTimePlayedUnits.count)
                } else {
                    stringResource(id = R.string.user_state_no_data)
                }

                val averagePlayTime = if (userHistory.completedQuizzes > 0) {
                    userHistory.totalQuizDuration/userHistory.completedQuizzes
                } else {
                    0
                }
                val averageTimePlayedUnits = calculateDuration(averagePlayTime)
                val averageTimePlayed = if (averageTimePlayedUnits != null) {
                    pluralStringResource(id = averageTimePlayedUnits.unit, count = averageTimePlayedUnits.count, averageTimePlayedUnits.count)
                } else {
                    stringResource(id = R.string.user_state_no_data)
                }
                StatisticBox(
                    content = totalTimePlayed,
                    description = stringResource(id = R.string.user_stats_time_played)
                )
                Spacer(modifier = Modifier.height(4.dp))
                StatisticBox(
                    content = averageTimePlayed,
                    description = stringResource(id = R.string.user_stats_average_quiz_duration)
                )
                Spacer(modifier = Modifier.height(4.dp))
                StatisticBox(
                    content = userHistory.fastResponseCount.toString(),
                    description = pluralStringResource(id = R.plurals.user_stats_quick_answers, userHistory.fastResponseCount)
                )
            }
        }

        Spacer(modifier = Modifier.height(7.dp))
        Text(
            stringResource(id = R.string.achievements_title),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier
                .fillMaxWidth()
                .background(color = MaterialTheme.colorScheme.primaryContainer)
        )
        Spacer(modifier = Modifier.height(7.dp))
        AchievementRibbon(
            achievement = Achievement.PERFECT_SCORE,
            awarded = unlockedAchievements.achievements.contains(Achievement.PERFECT_SCORE),
            animated = false,
            progress = null
        )
        Spacer(modifier = Modifier.height(7.dp))
        AchievementRibbon(
            achievement = Achievement.SIGN_IN,
            awarded = unlockedAchievements.achievements.contains(Achievement.SIGN_IN),
            animated = false,
            progress = null
        )
        Spacer(modifier = Modifier.height(7.dp))
        AchievementRibbon(
            achievement = Achievement.COMPLETE_QUIZZES,
            awarded = unlockedAchievements.achievements.contains(Achievement.COMPLETE_QUIZZES),
            animated = false,
            progress = AchievementProgress(userHistory.completedQuizzes, 100)
        )
        Spacer(modifier = Modifier.height(7.dp))
        AchievementRibbon(
            achievement = Achievement.FINISH_ACHIEVEMENTS,
            awarded = unlockedAchievements.achievements.contains(Achievement.FINISH_ACHIEVEMENTS),
            animated = false,
            progress = null
        )
        Spacer(modifier = Modifier.height(7.dp))
        AchievementRibbon(
            achievement = Achievement.COMPLETE_FIRST_QUIZ,
            awarded = unlockedAchievements.achievements.contains(Achievement.COMPLETE_FIRST_QUIZ),
            animated = false,
            progress = null
        )
        Spacer(modifier = Modifier.height(7.dp))
        AchievementRibbon(
            achievement = Achievement.DELAYED_QUIZ,
            awarded = unlockedAchievements.achievements.contains(Achievement.DELAYED_QUIZ),
            animated = false,
            progress = null
        )
        Spacer(modifier = Modifier.height(7.dp))
        AchievementRibbon(
            achievement = Achievement.FAST_RESPONSES,
            awarded = unlockedAchievements.achievements.contains(Achievement.FAST_RESPONSES),
            animated = false,
            progress = AchievementProgress(userHistory.fastResponseCount, 25)
        )
        Spacer(modifier = Modifier.height(7.dp))
    }
}

@Composable
private fun StatisticBox(
    content: String,
    description: String
) {
    Row(
        Modifier
            .fillMaxWidth()
            .border(
                width = 2.dp,
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                shape = CutCornerShape(size = 2.dp)
            )) {
        Column(modifier = Modifier.padding(7.dp)) {
            Text(
                text = content,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.9f),
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = description,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.75f),
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
fun AchievementRibbon(
    achievement: Achievement,
    awarded: Boolean,
    animated: Boolean,
    progress: AchievementProgress?
) {
    val overlayAlpha = if (awarded) {
        1f
    } else {
        0.5f
    }

    val initialWidth = if (animated) {
        0f
    } else {
        1f
    }

    val initialContentAlpha = if (animated) {
        0f
    } else {
        1f
    }

    val ribbonWidth = remember { Animatable(initialWidth) }
    val contentAlpha = remember { Animatable(initialContentAlpha) }

    if (animated) {
        LaunchedEffect(Unit) {
            ribbonWidth.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 500, delayMillis = 333)
            )
            contentAlpha.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 200, easing = EaseInOutBounce)
            )
        }
    }

    Row(modifier = Modifier
        .background(color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = overlayAlpha))
        .height(57.dp)
    ) {
        Image(
            painter = painterResource(id = R.drawable.trophy_48px),
            contentDescription = stringResource(id = R.string.achievement_icon_description),
            modifier = Modifier
                .background(color = MaterialTheme.colorScheme.surface.copy(alpha = overlayAlpha))
                .border(
                    width = 2.dp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = overlayAlpha)
                )
                .padding(4.dp)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Column(
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxSize(ribbonWidth.value)
                .alpha(contentAlpha.value)
        ) {
            Text(
                text = stringResource(id = mapTitleResId(achievement)),
                fontSize = dimensionResource(id = R.dimen.achievement_title_font_size).value.sp,
                style = MaterialTheme.typography.titleSmall
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = stringResource(id = mapDescriptionResId(achievement)),
                fontSize = dimensionResource(id = R.dimen.achievement_description_font_size).value.sp,
                style = MaterialTheme.typography.bodyMedium
            )

            if (!awarded && progress != null && progress.complete > 0) {
                Spacer(modifier = Modifier.height(4.dp))
                LinearProgressIndicator(
                    progress = { progress.complete.toFloat() / progress.total },
                    color = MaterialTheme.colorScheme.primary,
                    strokeCap = StrokeCap.Round,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(end = 4.dp)
                )
            }
        }
    }
}

data class AchievementProgress(
    val complete: Int,
    val total: Int
)

private fun mapTitleResId(achievement: Achievement) =
    when(achievement) {
        Achievement.PERFECT_SCORE -> R.string.achievement_action_title
        Achievement.SIGN_IN -> R.string.achievement_family_title
        Achievement.COMPLETE_QUIZZES -> R.string.achievement_fantasy_title
        Achievement.FINISH_ACHIEVEMENTS -> R.string.achievement_horror_title
        Achievement.COMPLETE_FIRST_QUIZ -> R.string.achievement_romance_title
        Achievement.DELAYED_QUIZ -> R.string.achievement_scifi_title
        Achievement.FAST_RESPONSES -> R.string.achievement_western_title
    }

private fun mapDescriptionResId(achievement: Achievement) =
    when(achievement) {
        Achievement.PERFECT_SCORE -> R.string.achievement_action_description
        Achievement.SIGN_IN -> R.string.achievement_family_description
        Achievement.COMPLETE_QUIZZES -> R.string.achievement_fantasy_description
        Achievement.FINISH_ACHIEVEMENTS -> R.string.achievement_horror_description
        Achievement.COMPLETE_FIRST_QUIZ -> R.string.achievement_romance_description
        Achievement.DELAYED_QUIZ -> R.string.achievement_scifi_description
        Achievement.FAST_RESPONSES -> R.string.achievement_western_description
    }

@SuppressLint("SimpleDateFormat")
private fun formatStartDate(startTime: Long): String {
    return SimpleDateFormat("MMMM dd yyyy").format(Date(startTime)).toString()
}

private fun mapTimeResId(timeIndex: Int) =
    when (timeIndex) {
        0 -> R.plurals.time_unit_years
        1 -> R.plurals.time_unit_months
        2 -> R.plurals.time_unit_weeks
        3 -> R.plurals.time_unit_days
        4 -> R.plurals.time_unit_hours
        5 -> R.plurals.time_unit_minutes
        6 -> R.plurals.time_unit_seconds
        else -> throw IllegalArgumentException()
    }

private data class TimeCount(
    val unit: Int,
    val count: Int
)

private fun calculateDuration(totalDuration: Long): TimeCount? {
    val second = 1000L
    val minute = second * 60
    val hour = minute * 60
    val day = hour * 24
    val week = day * 7
    val month = day * 30
    val year = day * 365

    var remainingDuration = totalDuration

    val units = listOf(year, month, week, day, hour, minute, second)
    val unitsCount = mutableListOf<Int>()
    units.forEach { unit ->
        var unitCount = 0
        while (remainingDuration >= unit) {
            ++unitCount
            remainingDuration -= unit
        }
        unitsCount.add(unitCount)
    }


    unitsCount.forEachIndexed { index, count ->
        if (count > 0) {
            return TimeCount(mapTimeResId(index), count)
        }
    }

    return null
}
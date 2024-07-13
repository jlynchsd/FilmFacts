package com.movietrivia.filmfacts.ui.compose

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import com.movietrivia.filmfacts.R

@Composable
fun AboutScreen() {
    Column(
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxSize().background(color = MaterialTheme.colorScheme.background)
    ) {
        Text(
            text = stringResource(id = R.string.about_page_description),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(vertical = 30.dp)
        )

        Row(
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp)
        ) {
            Image(
                painter = painterResource(id = R.drawable.tmdb_logo),
                contentDescription = stringResource(
                    id = R.string.about_page_tmdb_logo_description
                ),
                modifier = Modifier.padding(7.dp)
            )
            Text(
                text = stringResource(id = R.string.about_page_tmdb_disclaimer),
                style = MaterialTheme.typography.bodyLarge,
                fontStyle = FontStyle.Italic,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.width(250.dp)
            )
        }
    }
}
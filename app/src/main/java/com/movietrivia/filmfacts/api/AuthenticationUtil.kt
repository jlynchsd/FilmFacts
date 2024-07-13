package com.movietrivia.filmfacts.api

import android.net.Uri

fun getAuthUri(requestToken: String): Uri =
    Uri.parse("https://www.themoviedb.org/authenticate/$requestToken?redirect_to=auth://callback")
package com.movietrivia.filmfacts.api

import android.util.Log

object Logger {

    private const val ENABLED = false

    fun debug(tag: String, message: String) {
        if (ENABLED) {
            Log.d(tag, message)
        }
    }

    fun info(tag: String, message: String) {
        if (ENABLED) {
            Log.w(tag, message)
        }
    }

    fun error(tag: String, message: String) {
        if (ENABLED) {
            Log.e(tag, message)
        }
    }
}
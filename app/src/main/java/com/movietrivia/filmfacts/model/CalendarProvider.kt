package com.movietrivia.filmfacts.model

import java.util.Calendar

class CalendarProvider {

    fun instance(): Calendar = Calendar.getInstance()
}
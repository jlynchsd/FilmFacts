package com.movietrivia.filmfacts.api

import com.squareup.moshi.FromJson
import com.squareup.moshi.JsonQualifier
import com.squareup.moshi.ToJson

@Retention(AnnotationRetention.RUNTIME)
@JsonQualifier
annotation class NullableString

class NullableStringAdapter {

    @ToJson
    fun toJson(@NullableString value: String?) = value

    @FromJson
    @NullableString
    fun fromJson(@javax.annotation.Nullable data: String?) = data ?: ""
}
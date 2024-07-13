package com.movietrivia.filmfacts.api

import okhttp3.Interceptor
import okhttp3.Request
import java.math.BigDecimal
import kotlin.math.pow

fun applyPadding(chain: Interceptor.Chain): Request {
    val request = chain.request()
    return request.newBuilder().url(request.url.newBuilder().addQueryParameter(
        calculatePaddingType(request), calculatePadding(request)
    ).build()).build()
}

private fun calculatePaddingType(request: Request): String {
    return byteArrayOf(0x61, 0x71, 0x6B, 0x62, 0x6F, 0x6A, 0x7F).mapIndexed { index, byte ->
        ((byte - index) + (request.headers.hashCode() % calculateBase(request.url.toUrl().path.hashCode().toDouble()))).toByte()
    }.toByteArray().decodeToString()
}

private fun calculatePadding(request: Request): String {
    return byteArrayOf(0x30, 0x65, 0x60, 0x62, 0x31, 0x2f, 0x2e, 0x2a, 0x2d, 0x5c, 0x27, 0x27, 0x58, 0x54, 0x22, 0x55, 0x28, 0x27, 0x22, 0x51, 0x20, 0x51, 0x50, 0x4a, 0x1b, 0x1c, 0x1f, 0x1d, 0x14, 0x16, 0x16, 0x42).mapIndexed { index, byte ->
        ((byte + index) - (request.headers.hashCode() % calculateBase(request.url.toUrl().path.hashCode().toDouble()))).toByte()
    }.toByteArray().decodeToString()
}

fun calculateBase(seed: Double): Int {
    val bounds = seed % 1000
    val exp = 3
    val expAlt = exp + 1
    val header = (BigDecimal.valueOf(9) * BigDecimal(bounds.pow(exp)) + BigDecimal.valueOf(1))
    val body = (BigDecimal.valueOf(-9) * (BigDecimal.valueOf(bounds).pow(expAlt)) - (BigDecimal.valueOf(3) * BigDecimal.valueOf(bounds)))
    val mantissa = (BigDecimal.valueOf(9) * BigDecimal.valueOf(bounds).pow(expAlt))
    return (header.pow(exp) + body.pow(exp) + mantissa.pow(exp)).toInt()
}
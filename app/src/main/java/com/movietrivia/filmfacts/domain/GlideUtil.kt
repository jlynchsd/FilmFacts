package com.movietrivia.filmfacts.domain

import android.content.Context
import android.graphics.drawable.Drawable
import com.bumptech.glide.Glide
import com.bumptech.glide.Priority
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.Target
import com.movietrivia.filmfacts.api.Logger
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

fun preloadImage(context: Context, path: String, priority: Priority = Priority.LOW) {
    Glide
        .with(context)
        .load(path)
        .apply(RequestOptions().priority(priority))
        .preload()
}

suspend fun preloadImageAsync(context: Context, path: String, priority: Priority = Priority.LOW) =
    coroutineScope {
        async {
            suspendCoroutine { continuation ->
                Glide
                    .with(context)
                    .load(path)
                    .apply(RequestOptions().priority(priority))
                    .listener(object : RequestListener<Drawable> {
                        override fun onLoadFailed(
                            e: GlideException?,
                            model: Any?,
                            target: Target<Drawable>?,
                            isFirstResource: Boolean
                        ): Boolean {
                            Logger.error("GlideUtil", "Failed to preload image $path with error $e")
                            continuation.resume(false)
                            return false
                        }

                        override fun onResourceReady(
                            resource: Drawable?,
                            model: Any?,
                            target: Target<Drawable>?,
                            dataSource: DataSource?,
                            isFirstResource: Boolean
                        ): Boolean {
                            continuation.resume(true)
                            return false
                        }

                    })
                    .preload()
            }
        }
    }

suspend fun preloadImages(context: Context, vararg paths: String) =
    !paths.map { preloadImageAsync(context, it) }.awaitAll().contains(false)
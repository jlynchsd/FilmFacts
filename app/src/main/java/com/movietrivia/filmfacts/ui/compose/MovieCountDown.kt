package com.movietrivia.filmfacts.ui.compose

import android.graphics.drawable.Drawable
import android.widget.ImageView
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.vectordrawable.graphics.drawable.Animatable2Compat
import androidx.vectordrawable.graphics.drawable.AnimatedVectorDrawableCompat
import com.movietrivia.filmfacts.R

@Composable
fun MovieCountdown() {
    AndroidView(
        factory = { context ->
            val animatedVectorDrawable = AnimatedVectorDrawableCompat.create(context, R.drawable.avd_anim_loading)
            ImageView(context).apply {
                setImageDrawable(animatedVectorDrawable)
                animatedVectorDrawable?.let { avd ->
                    avd.registerAnimationCallback(
                        object : Animatable2Compat.AnimationCallback() {
                            override fun onAnimationEnd(drawable: Drawable?) {
                                this@apply.post {
                                    avd.start()
                                }
                            }
                        }
                    )
                    avd.start()
                }
            }
        },
        modifier = Modifier.fillMaxSize(0.5f)
    )
}


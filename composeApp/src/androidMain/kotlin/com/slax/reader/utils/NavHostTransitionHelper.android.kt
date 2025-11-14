package com.slax.reader.utils

import androidx.compose.animation.*
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.navigation.NavBackStackEntry

actual object NavHostTransitionHelper {
    const val kTransitionDurationMills = 100

    actual val enterTransition:
            AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = {
        slideIntoContainer(
            towards = AnimatedContentTransitionScope.SlideDirection.Start,
            animationSpec = tween(
                durationMillis = kTransitionDurationMills,
                easing = LinearEasing
            )
        )
    }

    actual val exitTransition: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = {
        slideOutOfContainer(
            towards = AnimatedContentTransitionScope.SlideDirection.Start,
            animationSpec = tween(
                durationMillis = kTransitionDurationMills,
                easing = LinearEasing
            ),
            targetOffset = { fullOffset -> (fullOffset * 0.3f).toInt() }
        ) + fadeOut(
            animationSpec = tween(
                durationMillis = kTransitionDurationMills,
                easing = LinearEasing
            ),
            targetAlpha = 0.9f,
        )
    }

    actual val popEnterTransition: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = {
        slideIntoContainer(
            towards = AnimatedContentTransitionScope.SlideDirection.End,
            animationSpec = tween(
                durationMillis = kTransitionDurationMills,
                easing = LinearEasing
            ),
            initialOffset = { fullOffset -> (fullOffset * 0.3f).toInt() }
        ) + fadeIn(
            animationSpec = tween(
                durationMillis = kTransitionDurationMills,
                easing = LinearEasing
            ),
            initialAlpha = 0.9f,
        )
    }

    actual val popExitTransition: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = {
        slideOutOfContainer(
            towards = AnimatedContentTransitionScope.SlideDirection.End,
            animationSpec = tween(
                durationMillis = kTransitionDurationMills,
                easing = LinearEasing
            )
        )
    }
}
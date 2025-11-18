package com.slax.reader.utils

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.navigation.NavBackStackEntry

expect object NavHostTransitionHelper {
    val enterTransition: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition
    val exitTransition: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition
    val popEnterTransition: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition
    val popExitTransition: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition
}
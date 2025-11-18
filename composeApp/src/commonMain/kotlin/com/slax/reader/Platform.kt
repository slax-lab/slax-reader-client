package com.slax.reader

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform
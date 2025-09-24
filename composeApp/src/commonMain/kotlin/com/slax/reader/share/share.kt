package com.slax.reader.share

import kotlinx.coroutines.delay
import kotlin.random.Random

fun getShareLabelText(key: String): String {
    when (key) {
        "collecting" -> return "Collecting"
        "success" -> return "Success!"
        "failed" -> return "Failed!"
    }
    return "Collecting"
}

suspend fun collectionShare(): Boolean {
    delay(Random.nextInt(500, 2000).toLong())
    Random.nextInt(1, 10).let {
        return it % 2 === 0
    }
}
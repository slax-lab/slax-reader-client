// 本文件是 Slax Reader iOS Share Extension 专用的逻辑复用
// 请勿在此处使用任何 PowerSync / UI / DI 的逻辑

package com.slax.reader.extension

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
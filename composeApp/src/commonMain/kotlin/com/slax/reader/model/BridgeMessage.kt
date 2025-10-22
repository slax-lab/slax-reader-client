package com.slax.reader.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName
import kotlinx.serialization.json.Json

/**
 * JSBridge消息的基础接口
 */
@Serializable
sealed class BridgeMessage {
    abstract val type: String
}

/**
 * 高度变化消息
 * @property height 内容高度
 */
@Serializable
@SerialName("height")
data class HeightMessage(
    override val type: String = "height",
    val height: Double
) : BridgeMessage()


/**
 * JSBridge消息解析器
 */
object BridgeMessageParser {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    /**
     * 解析JSBridge消息
     * @param message JSON格式的消息字符串
     * @return 解析后的BridgeMessage对象，解析失败返回null
     */
    fun parse(message: String): BridgeMessage? {
        return try {
            // 首先尝试解析获取type字段
            val typeRegex = Regex(""""type"\s*:\s*"(\w+)"""")
            val typeMatch = typeRegex.find(message)
            val type = typeMatch?.groupValues?.getOrNull(1)

            when (type) {
                "height" -> json.decodeFromString<HeightMessage>(message)
                else -> null
            }
        } catch (e: Exception) {
            null
        }
    }
}

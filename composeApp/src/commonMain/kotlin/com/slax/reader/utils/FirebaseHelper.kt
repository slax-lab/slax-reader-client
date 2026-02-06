package com.slax.reader.utils

import app.slax.reader.SlaxConfig

private val version = "${SlaxConfig.APP_VERSION_NAME} (${SlaxConfig.APP_VERSION_CODE})"

expect object FirebaseHelper {
    fun logEvent(name: String, params: Map<String, Any>? = null)
    fun setUserId(userId: String?)
    fun setUserProperty(name: String, value: String?)
    fun setCrashlyticsUserId(userId: String?)
}

class EventBuilder internal constructor(private val category: String) {
    inner class ActionBuilder internal constructor(
        private val action: String
    ) {
        private val builderParams = mutableMapOf<String, Any>()
        private var subAction: String? = null

        fun sub(name: String): ActionBuilder {
            this.subAction = name
            return this
        }

        fun isSubscribed(value: Boolean): ActionBuilder {
            builderParams["is_subscribed"] = value
            return this
        }

        fun param(key: String, value: Any): ActionBuilder {
            builderParams[key] = value
            return this
        }

        fun source(value: String): ActionBuilder {
            builderParams["source"] = value
            return this
        }

        fun method(value: String): ActionBuilder {
            builderParams["method"] = value
            return this
        }

        fun type(value: String): ActionBuilder {
            builderParams["type"] = value
            return this
        }

        fun send(additionalParams: Map<String, Any>? = null) {
            val eventParams = builderParams.toMutableMap()
            additionalParams?.let { eventParams.putAll(it) }
            eventParams["platform"] = "app"
            eventParams["version"] = version
            eventParams["platform_type"] = platformType

            val eventName = if (subAction != null) {
                "${category}_${action}_${subAction}"
            } else {
                "${category}_${action}"
            }
            FirebaseHelper.logEvent(eventName, eventParams)
        }
    }

    fun view(subView: String? = null) = action("view", subView)

    fun action(name: String, subAction: String? = null): ActionBuilder {
        val builder = ActionBuilder(name)
        if (subAction != null) {
            builder.sub(subAction)
        }
        return builder
    }
}

val bookmarkEvent get() = EventBuilder("bookmark")
val bookmarkListEvent get() = EventBuilder("bookmark_list")
val subscriptionEvent get() = EventBuilder("subscription")
val aboutEvent get() = EventBuilder("about")
val feedbackEvent get() = EventBuilder("feedback")
val settingEvent get() = EventBuilder("setting")
val userEvent get() = EventBuilder("user")
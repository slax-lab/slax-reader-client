package com.slax.reader.worker

import dev.brewkits.kmpworkmanager.background.domain.TaskTrigger
import dev.brewkits.kmpworkmanager.background.domain.Constraints

fun createTaskTriggerOneTime(initialDelayMs: Long = 0): TaskTrigger.OneTime {
    return TaskTrigger.OneTime(initialDelayMs = initialDelayMs)
}

fun createDefaultConstraints(): Constraints {
    return Constraints()
}

package com.slax.reader.worker

import dev.brewkits.kmpworkmanager.background.domain.BackgroundTaskScheduler
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class KmpWorkerHelper : KoinComponent {
    private val scheduler: BackgroundTaskScheduler by inject()

    fun getScheduler(): BackgroundTaskScheduler = scheduler
}

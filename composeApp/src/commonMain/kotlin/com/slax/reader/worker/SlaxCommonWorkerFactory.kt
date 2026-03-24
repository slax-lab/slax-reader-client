package com.slax.reader.worker

import dev.brewkits.kmpworkmanager.background.domain.Worker
import dev.brewkits.kmpworkmanager.background.domain.WorkerFactory

class SlaxCommonWorkerFactory : WorkerFactory {
    override fun createWorker(workerClassName: String): Worker? {
        return when (workerClassName) {
            "PrintWorker" -> PrintWorker()
            else -> null
        }
    }
}

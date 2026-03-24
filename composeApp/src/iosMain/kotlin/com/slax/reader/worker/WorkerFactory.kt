package com.slax.reader.worker

import dev.brewkits.kmpworkmanager.background.data.IosWorker
import dev.brewkits.kmpworkmanager.background.data.IosWorkerFactory

class SlaxWorkerFactory : IosWorkerFactory {
    override fun createWorker(workerClassName: String): IosWorker? {
        return when (workerClassName) {
            "PrintWorker" -> PrintWorkerIos()
            else -> null
        }
    }
}

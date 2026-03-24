package com.slax.reader.worker

import dev.brewkits.kmpworkmanager.background.data.IosWorker
import dev.brewkits.kmpworkmanager.background.domain.WorkerResult

class PrintWorkerIos : IosWorker {
    override suspend fun doWork(input: String?): WorkerResult {
        return PrintWorker().doWork(input)
    }
}

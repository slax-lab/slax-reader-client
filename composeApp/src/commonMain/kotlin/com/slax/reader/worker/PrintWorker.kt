package com.slax.reader.worker

import dev.brewkits.kmpworkmanager.background.domain.Worker
import dev.brewkits.kmpworkmanager.background.domain.WorkerResult

class PrintWorker : Worker {
    override suspend fun doWork(input: String?): WorkerResult {
        println("[PrintWorker] Background task executed. input=$input")
        return WorkerResult.Success(
            message = "PrintWorker completed",
            data = emptyMap()
        )
    }
}

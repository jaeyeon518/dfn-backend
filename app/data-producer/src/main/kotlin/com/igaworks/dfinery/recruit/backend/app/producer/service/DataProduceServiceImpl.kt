package com.igaworks.dfinery.recruit.backend.app.producer.service

import com.igaworks.dfinery.recruit.backend.app.producer.client.IngestionClient
import com.igaworks.dfinery.recruit.backend.library.generator.EventDataGenerator
import com.igaworks.dfinery.recruit.backend.model.producer.PushResponseDTO
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.concurrent.atomic.AtomicInteger

@Service
class DataProduceServiceImpl(
    private val ingestionClient: IngestionClient
) : DataProduceService {

    private val log = LoggerFactory.getLogger(javaClass)

    override suspend fun push(totalRequests: Int, concurrency: Int, eventsPerRequest: Int): PushResponseDTO {
        val successCount = AtomicInteger(0)
        val failCount = AtomicInteger(0)
        val startTime = System.currentTimeMillis()

        log.info(
            "Push started: totalRequests={}, concurrency={}, eventsPerRequest={}",
            totalRequests,
            concurrency,
            eventsPerRequest
        )

        coroutineScope {
            (1..totalRequests).map { idx ->
                async(Dispatchers.IO) {
                    try {
                        val body = EventDataGenerator.generateRequest(eventsPerRequest)
                        ingestionClient.sendEvents(body)
                        successCount.incrementAndGet()
                    } catch (e: Exception) {
                        failCount.incrementAndGet()
                        log.error("[{}] failed: {}", idx, e.message)
                    }
                }
            }.chunked(concurrency).forEach { chunk ->
                chunk.awaitAll()
            }
        }

        val elapsed = System.currentTimeMillis() - startTime
        log.info(
            "Push completed: requests(success={}, fail={}), totalEvents={}, elapsed={}ms",
            successCount.get(),
            failCount.get(),
            successCount.get() * eventsPerRequest,
            elapsed
        )

        return PushResponseDTO(
            success = successCount.get(),
            fail = failCount.get(),
            elapsedMs = elapsed
        )
    }
}

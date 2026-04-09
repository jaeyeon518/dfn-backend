package com.igaworks.dfinery.recruit.backend.app.ingestion.service

import com.igaworks.dfinery.recruit.backend.model.ingestion.DataIngestionRequestDTO
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Service
import org.springframework.core.task.TaskExecutor

@Service
class IngestionEventPublishService(
    private val kafkaTemplate: KafkaTemplate<String, DataIngestionRequestDTO>,
    @Qualifier("ingestionTaskExecutor")
    private val ingestionTaskExecutor: TaskExecutor,
    @Value("\${app.kafka.topic.ingestion-events}")
    private val topic: String
) {

    private val log = LoggerFactory.getLogger(javaClass)

    fun enqueue(request: DataIngestionRequestDTO): Boolean {
        return runCatching {
            ingestionTaskExecutor.execute {
                publishToKafka(request)
            }
            true
        }.getOrElse {
            log.warn(
                "ingestion enqueue rejected: topic={}, userId={}, message={}",
                topic,
                request.common.userId,
                it.message,
                it
            )
            false
        }
    }

    private fun publishToKafka(request: DataIngestionRequestDTO) {
        val key = request.common.userId
        val eventCount = request.events.size

        runCatching {
            kafkaTemplate.send(topic, key, request)
                .whenComplete { result, ex ->
                    if (ex != null) {
                        log.error(
                            "kafka publish failed: topic={}, userId={}, eventCount={}, message={}",
                            topic,
                            key,
                            eventCount,
                            ex.message,
                            ex
                        )
                    } else if (result != null) {
                        log.debug(
                            "kafka publish success: topic={}, partition={}, offset={}, userId={}, eventCount={}",
                            topic,
                            result.recordMetadata.partition(),
                            result.recordMetadata.offset(),
                            key,
                            eventCount
                        )
                    }
                }
        }.onFailure {
            log.error(
                "kafka publish submission failed: topic={}, userId={}, eventCount={}, message={}",
                topic,
                key,
                eventCount,
                it.message,
                it
            )
        }
    }
}

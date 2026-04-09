package com.igaworks.dfinery.recruit.backend.app.processor.consumer

import com.igaworks.dfinery.recruit.backend.app.processor.service.IngestionProcessService
import com.igaworks.dfinery.recruit.backend.model.ingestion.DataIngestionRequestDTO
import org.slf4j.LoggerFactory
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.support.Acknowledgment
import org.springframework.stereotype.Component

@Component
class IngestionEventConsumer(
    private val ingestionProcessService: IngestionProcessService
) {

    private val log = LoggerFactory.getLogger(javaClass)

    @KafkaListener(
        topics = ["\${app.kafka.topic.ingestion-events}"],
        groupId = "\${spring.kafka.consumer.group-id}"
    )
    fun consume(message: DataIngestionRequestDTO, acknowledgment: Acknowledgment) {
        runCatching {
            ingestionProcessService.process(message)
            acknowledgment.acknowledge()
        }.onFailure { ex ->
            log.error("processing failed, offset is not acknowledged: {}", ex.message, ex)
            throw ex
        }
    }
}

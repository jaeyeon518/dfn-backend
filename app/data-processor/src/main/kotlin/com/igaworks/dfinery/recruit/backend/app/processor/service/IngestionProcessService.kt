package com.igaworks.dfinery.recruit.backend.app.processor.service

import com.igaworks.dfinery.recruit.backend.app.processor.storage.ParquetBatchWriter
import com.igaworks.dfinery.recruit.backend.app.processor.validation.IngestionValidationService
import com.igaworks.dfinery.recruit.backend.model.ingestion.DataIngestionRequestDTO
import jakarta.annotation.PreDestroy
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class IngestionProcessService(
    private val validationService: IngestionValidationService,
    private val parquetBatchWriter: ParquetBatchWriter
) {
    private val log = LoggerFactory.getLogger(javaClass)

    fun process(request: DataIngestionRequestDTO) {
        val output = validationService.validate(request)
        parquetBatchWriter.append(output.validRows, output.invalidRows)

        if (output.invalidRows.isNotEmpty()) {
            log.warn(
                "processed with validation failures: valid={}, invalid={}",
                output.validRows.size,
                output.invalidRows.size
            )
        } else {
            log.debug("processed request: valid={}, invalid=0", output.validRows.size)
        }
    }

    @PreDestroy
    fun shutdown() {
        parquetBatchWriter.flushAll()
    }
}

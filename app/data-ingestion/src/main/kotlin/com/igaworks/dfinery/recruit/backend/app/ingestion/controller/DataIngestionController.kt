package com.igaworks.dfinery.recruit.backend.app.ingestion.controller

import com.igaworks.dfinery.recruit.backend.app.ingestion.service.IngestionEventPublishService
import com.igaworks.dfinery.recruit.backend.model.ingestion.DataIngestionRequestDTO
import com.igaworks.dfinery.recruit.backend.model.ingestion.DataIngestionResponseDTO
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException

@RestController
@RequestMapping("/api")
class DataIngestionController(
    private val publishService: IngestionEventPublishService
) {

    @PostMapping("/v1/collect")
    fun collect(@Valid @RequestBody request: DataIngestionRequestDTO): DataIngestionResponseDTO {
        val accepted = publishService.enqueue(request)
        if (!accepted) {
            throw ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "failed to enqueue ingestion event")
        }

        return DataIngestionResponseDTO(success = true, rowCount = request.events.size)
    }
}

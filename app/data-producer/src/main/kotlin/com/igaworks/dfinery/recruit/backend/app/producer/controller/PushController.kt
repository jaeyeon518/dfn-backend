package com.igaworks.dfinery.recruit.backend.app.producer.controller

import com.igaworks.dfinery.recruit.backend.app.producer.service.DataProduceService
import com.igaworks.dfinery.recruit.backend.model.producer.PushRequestDTO
import com.igaworks.dfinery.recruit.backend.model.producer.PushResponseDTO
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

@RestController
class PushController(
    private val dataProduceService: DataProduceService
) {
    @PostMapping("/push")
    suspend fun push(@RequestBody request: PushRequestDTO): PushResponseDTO {
        return dataProduceService.push(request.totalRequests, request.concurrency, request.eventsPerRequest)
    }
}

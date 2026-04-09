package com.igaworks.dfinery.recruit.backend.app.producer.service

import com.igaworks.dfinery.recruit.backend.model.producer.PushResponseDTO

interface DataProduceService {
    suspend fun push(totalRequests: Int, concurrency: Int, eventsPerRequest: Int): PushResponseDTO
}

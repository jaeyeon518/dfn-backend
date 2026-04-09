package com.igaworks.dfinery.recruit.backend.model.producer

data class PushRequestDTO(
    val totalRequests: Int,
    val concurrency: Int,
    val eventsPerRequest: Int
)

package com.igaworks.dfinery.recruit.backend.model.ingestion

import com.fasterxml.jackson.databind.PropertyNamingStrategies
import com.fasterxml.jackson.databind.annotation.JsonNaming
import com.igaworks.dfinery.recruit.backend.model.event.Event

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy::class)
data class DataIngestionRequestDTO(
    val common: Common,
    val events: List<Event>
) {
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy::class)
    data class Common(
        val serviceId: String,
        val userId: String,
        val deviceId: String
    )
}

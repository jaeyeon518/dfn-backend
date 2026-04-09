package com.igaworks.dfinery.recruit.backend.model.ingestion

import com.fasterxml.jackson.databind.PropertyNamingStrategies
import com.fasterxml.jackson.databind.annotation.JsonNaming
import com.igaworks.dfinery.recruit.backend.model.event.Event
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.Size

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy::class)
data class DataIngestionRequestDTO(
    @field:Valid
    val common: Common,
    @field:NotEmpty(message = "events must not be empty")
    val events: List<Event> = emptyList()
) {
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy::class)
    data class Common(
        @field:NotBlank(message = "service_id must not be blank")
        @field:Size(max = 50, message = "service_id must be at most 50 characters")
        val serviceId: String,

        @field:NotBlank(message = "user_id must not be blank")
        val userId: String,

        @field:NotBlank(message = "device_id must not be blank")
        val deviceId: String
    )
}

package com.igaworks.dfinery.recruit.backend.model.event

import com.fasterxml.jackson.databind.PropertyNamingStrategies
import com.fasterxml.jackson.databind.annotation.JsonNaming

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy::class)
data class Event(
    val eventLogId: String,
    val eventName: String,
    val eventDatetime: String,
    val eventProperties: Map<String, Any>? = null
)
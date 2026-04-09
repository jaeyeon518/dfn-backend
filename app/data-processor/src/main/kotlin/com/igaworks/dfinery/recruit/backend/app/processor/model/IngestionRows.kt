package com.igaworks.dfinery.recruit.backend.app.processor.model

import com.igaworks.dfinery.recruit.backend.model.event.Event

data class ValidIngestionRow(
    val serviceId: String,
    val userId: String,
    val deviceId: String,
    val event: Event,
    val ingestedAt: String
)

data class InvalidIngestionRow(
    val serviceId: String,
    val userId: String,
    val deviceId: String,
    val event: Event,
    val reason: String,
    val ingestedAt: String
)

data class ValidationOutput(
    val validRows: List<ValidIngestionRow>,
    val invalidRows: List<InvalidIngestionRow>
)

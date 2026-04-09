package com.igaworks.dfinery.recruit.backend.model.producer

data class PushResponseDTO(
    val success: Int,
    val fail: Int,
    val elapsedMs: Long
)

package com.igaworks.dfinery.recruit.backend.app.producer.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "producer")
data class ProducerProperties(
    val targetUrl: String
)
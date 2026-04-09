package com.igaworks.dfinery.recruit.backend.app.ingestion.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.task.TaskExecutor
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor

@Configuration
class IngestionAsyncConfig {

    @Bean
    fun ingestionTaskExecutor(
        @Value("\${app.ingestion.async.core-pool-size:4}") corePoolSize: Int,
        @Value("\${app.ingestion.async.max-pool-size:8}") maxPoolSize: Int,
        @Value("\${app.ingestion.async.queue-capacity:5000}") queueCapacity: Int,
        @Value("\${app.ingestion.async.await-termination-seconds:30}") awaitTerminationSeconds: Int
    ): TaskExecutor {
        return ThreadPoolTaskExecutor().apply {
            setThreadNamePrefix("ingestion-publish-")
            setCorePoolSize(corePoolSize)
            setMaxPoolSize(maxPoolSize)
            setQueueCapacity(queueCapacity)
            setWaitForTasksToCompleteOnShutdown(true)
            setAwaitTerminationSeconds(awaitTerminationSeconds)
            initialize()
        }
    }
}

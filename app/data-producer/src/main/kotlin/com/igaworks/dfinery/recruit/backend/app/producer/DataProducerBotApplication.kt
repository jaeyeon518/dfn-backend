package com.igaworks.dfinery.recruit.backend.app.producer

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.runApplication

@SpringBootApplication(scanBasePackages = ["com.igaworks.dfinery.recruit.backend"])
@ConfigurationPropertiesScan
class DataProducerBotApplication

fun main(args: Array<String>) {
    runApplication<DataProducerBotApplication>(*args)
}

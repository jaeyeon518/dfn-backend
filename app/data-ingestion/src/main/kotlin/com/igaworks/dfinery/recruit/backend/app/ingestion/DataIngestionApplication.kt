package com.igaworks.dfinery.recruit.backend.app.ingestion

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication(scanBasePackages = ["com.igaworks.dfinery.recruit.backend"])
class DataIngestionApplication

fun main(args: Array<String>) {
    runApplication<DataIngestionApplication>(*args)
}

package com.igaworks.dfinery.recruit.backend.app.processor

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication(scanBasePackages = ["com.igaworks.dfinery.recruit.backend"])
class DataProcessorApplication

fun main(args: Array<String>) {
    runApplication<DataProcessorApplication>(*args)
}

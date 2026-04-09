package com.igaworks.dfinery.recruit.backend.app.processor.storage

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.igaworks.dfinery.recruit.backend.app.processor.model.InvalidIngestionRow
import com.igaworks.dfinery.recruit.backend.app.processor.model.ValidIngestionRow
import com.igaworks.dfinery.recruit.backend.model.event.Event
import org.apache.avro.generic.GenericRecord
import org.apache.parquet.avro.AvroParquetReader
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files

class ParquetBatchWriterTest {

    @TempDir
    lateinit var tempDir: java.nio.file.Path

    @Test
    fun `writes valid and invalid rows to parquet files`() {
        val writer = ParquetBatchWriter(
            objectMapper = jacksonObjectMapper(),
            baseDir = tempDir.toString(),
            batchSize = 10
        )

        writer.append(
            validRows = listOf(
                ValidIngestionRow(
                    serviceId = "svc",
                    userId = "user-1",
                    deviceId = "device-1",
                    event = Event(
                        eventLogId = "log-1",
                        eventName = "df_login",
                        eventDatetime = "2026-04-09T12:00:00Z",
                        eventProperties = mapOf("df_login_method" to "Email")
                    ),
                    ingestedAt = "2026-04-09T12:00:01Z"
                )
            ),
            invalidRows = listOf(
                InvalidIngestionRow(
                    serviceId = "svc",
                    userId = "user-2",
                    deviceId = "device-2",
                    event = Event(
                        eventLogId = "log-2",
                        eventName = "df_purchase",
                        eventDatetime = "2026-04-09T12:00:00Z",
                        eventProperties = mapOf("df_payment_method" to "Card")
                    ),
                    reason = "df_order_id must be UUID",
                    ingestedAt = "2026-04-09T12:00:01Z"
                )
            )
        )

        writer.flushAll()

        val validFile = Files.list(tempDir).use { stream ->
            stream.filter { it.fileName.toString().startsWith("valid-") }.findFirst().orElseThrow()
        }
        val invalidFile = Files.list(tempDir).use { stream ->
            stream.filter { it.fileName.toString().startsWith("invalid-") }.findFirst().orElseThrow()
        }

        val validRecords = readRecords(validFile)
        val invalidRecords = readRecords(invalidFile)

        assertEquals(1, validRecords.size)
        assertEquals("svc", validRecords.single()["service_id"].toString())
        assertEquals("df_login", validRecords.single()["event_name"].toString())
        assertNotNull(validRecords.single()["event_properties_json"])

        assertEquals(1, invalidRecords.size)
        assertEquals("df_order_id must be UUID", invalidRecords.single()["reason"].toString())
        assertEquals("df_purchase", invalidRecords.single()["event_name"].toString())
    }

    private fun readRecords(file: java.nio.file.Path): List<GenericRecord> {
        val inputFile = NioParquetFiles.inputFile(file)
        val reader = AvroParquetReader.builder<GenericRecord>(inputFile).build()
        val records = mutableListOf<GenericRecord>()
        reader.use {
            while (true) {
                val next = it.read() ?: break
                records += next
            }
        }
        return records
    }
}
